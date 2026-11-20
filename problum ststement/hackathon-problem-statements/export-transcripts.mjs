#!/usr/bin/env node
// Finds your AI tool sessions for this repo and copies them into transcripts/ (gzipped), then commits them.
// Run it from anywhere inside the repo:   node export-transcripts.mjs
// Run it again whenever you like; it replaces the earlier copies. Push afterwards (git push).
//
// Finds: OpenAI Codex (CLI, IDE extension, app), Google Antigravity (editor and agy CLI, version 2 or later),
// Gemini CLI, Claude Code. Using something else? Export its chats yourself into transcripts/ (Markdown or JSON).
//
// Options: --dry-run (list only), --no-commit, --since <ISO date> (default: this repo's first commit),
//          --out <dir> (default: <repo>/transcripts)
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import zlib from "node:zlib";
import { execFileSync } from "node:child_process";

const args = process.argv.slice(2);
const flag = (name) => args.includes(name);
const option = (name) => { const i = args.indexOf(name); return i >= 0 ? args[i + 1] : undefined; };

function git(...a) {
  try { return execFileSync("git", a, { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] }).trim(); }
  catch { return ""; }
}

const repo = path.resolve(git("rev-parse", "--show-toplevel") || process.cwd());
const out = path.resolve(option("--out") || path.join(repo, "transcripts"));
const firstCommit = Number(git("log", "--reverse", "--format=%ct").split("\n")[0]) * 1000;
const since = option("--since") ? Date.parse(option("--since")) : (firstCommit || Date.now() - 12 * 3600e3) - 10 * 60e3;
const home = os.homedir();

// Paths compare case-insensitively with forward slashes, so Windows and macOS spellings both match.
const norm = (p) => String(p).replace(/\\/g, "/").replace(/\/+$/, "").toLowerCase();
const repoNorm = norm(repo);
const inRepo = (p) => p && (norm(p) === repoNorm || norm(p).startsWith(repoNorm + "/"));
// The same path as it may appear inside a file: plain, JSON-escaped (Windows), or as a file:// URI.
const fwd = repo.replace(/\\/g, "/");
const needles = [...new Set([
  fwd, repo, repo.replace(/\\/g, "\\\\"),
  encodeURI(fwd).replace(/:/g, "%3A"), encodeURI(fwd).replace(/:/g, "%3a"),
].map((s) => s.toLowerCase()))];
// A whole-path mention only: ".../slotbook-ann" must not match ".../slotbook-anna".
const mentionsRepo = (file) => {
  const text = fs.readFileSync(file).toString("latin1").toLowerCase();
  return needles.some((n) => {
    for (let i = text.indexOf(n); i >= 0; i = text.indexOf(n, i + 1)) {
      if (!/[a-z0-9_.-]/.test(text[i + n.length] || "")) return true;
    }
    return false;
  });
};

function walk(dir, test, depth = 6, found = []) {
  let entries;
  try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch { return found; }
  for (const e of entries) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) { if (depth > 0) walk(p, test, depth - 1, found); }
    else if (e.isFile() && test(p)) found.push(p);
  }
  return found;
}
const recent = (p) => { try { return fs.statSync(p).mtimeMs >= since; } catch { return false; } };

// First "cwd" in a JSONL session (Codex puts it in session_meta.payload.cwd, Claude Code on each line).
function jsonlCwd(file) {
  const fd = fs.openSync(file, "r");
  try {
    const buf = Buffer.alloc(256 * 1024);
    const text = buf.subarray(0, fs.readSync(fd, buf, 0, buf.length, 0)).toString("utf8");
    for (const line of text.split("\n")) {
      try {
        const j = JSON.parse(line);
        const cwd = j?.payload?.cwd ?? j?.cwd;
        if (cwd) return cwd;
      } catch { /* partial line */ }
    }
  } finally { fs.closeSync(fd); }
  return null;
}

const found = [];   // { tool, file }
const warnings = [];

// OpenAI Codex: CLI, IDE extension and app all write rollout-*.jsonl under CODEX_HOME (default ~/.codex).
const codexHome = process.env.CODEX_HOME || path.join(home, ".codex");
for (const sub of ["sessions", "archived_sessions"]) {
  for (const f of walk(path.join(codexHome, sub), (p) => p.endsWith(".jsonl") && recent(p))) {
    if (inRepo(jsonlCwd(f))) found.push({ tool: "codex", file: f });
  }
}

// Claude Code: ~/.claude/projects/<project>/<session>.jsonl, each line carrying its cwd.
const claudeHome = process.env.CLAUDE_CONFIG_DIR || path.join(home, ".claude");
for (const f of walk(path.join(claudeHome, "projects"), (p) => p.endsWith(".jsonl") && recent(p), 2)) {
  if (inRepo(jsonlCwd(f))) found.push({ tool: "claude", file: f });
}

// Google Antigravity 2+ (editor: ~/.gemini/antigravity, agy CLI: ~/.gemini/antigravity-cli): one SQLite file per
// conversation, plus its -wal file holding the latest steps. Older versions kept encrypted .pb files.
const geminiHome = path.join(home, ".gemini");
let geminiDirs = [];
try { geminiDirs = fs.readdirSync(geminiHome, { withFileTypes: true }).filter((e) => e.isDirectory()).map((e) => e.name); } catch {}
for (const d of geminiDirs.filter((d) => d.startsWith("antigravity"))) {
  const conv = path.join(geminiHome, d, "conversations");
  for (const f of walk(conv, (p) => p.endsWith(".db") && (recent(p) || recent(p + "-wal")), 0)) {
    const wal = f + "-wal";
    if (mentionsRepo(f) || (fs.existsSync(wal) && mentionsRepo(wal))) {
      found.push({ tool: d, file: f });
      if (fs.existsSync(wal)) found.push({ tool: d, file: wal });
    }
  }
  if (walk(conv, (p) => p.endsWith(".pb") && recent(p), 0).length) {
    warnings.push(`An old version of Antigravity (${d}) is in use: its chat history is encrypted and cannot be exported. Update Antigravity.`);
  }
}

// Gemini CLI: ~/.gemini/tmp/<project hash>/ (chats/*.json, logs.json).
for (const f of walk(path.join(geminiHome, "tmp"), (p) => p.endsWith(".json") && recent(p), 3)) {
  if (mentionsRepo(f)) found.push({ tool: "gemini-cli", file: f });
}

const since_ = new Date(since).toISOString().replace("T", " ").slice(0, 16);
console.log(`Repo: ${repo}\nSessions changed since ${since_} UTC that belong to this repo:`);
if (!found.length) console.log("  none found");
let total = 0;
const written = [];
for (const { tool, file } of found) {
  const name = `${tool}-${path.basename(path.dirname(file)) === "conversations" ? "" : path.basename(path.dirname(file)) + "-"}${path.basename(file)}.gz`
    .replace(/[^A-Za-z0-9._-]/g, "_");
  const data = zlib.gzipSync(fs.readFileSync(file), { level: 9 });
  total += data.length;
  console.log(`  ${tool.padEnd(18)} ${file}  (${(data.length / 1e6).toFixed(1)} MB compressed)`);
  if (!flag("--dry-run")) {
    fs.mkdirSync(out, { recursive: true });
    fs.writeFileSync(path.join(out, name), data);
    written.push(path.join(out, name));
  }
}
for (const w of warnings) console.log(`\nWARNING: ${w}`);
console.log(`\nFound: ${[...new Set(found.map((f) => f.tool))].join(" ") || "none"}`);
if (total > 90e6) console.log("\nWARNING: over 90 MB. GitHub refuses files over 100 MB; tell an organiser.");
if (flag("--dry-run") || !written.length) process.exit(found.length ? 0 : 1);

console.log(`\nWrote ${written.length} file(s) to ${path.relative(repo, out) || out}.`);
if (!flag("--no-commit") && out.startsWith(repo)) {
  git("add", "--", ...written);
  if (git("diff", "--cached", "--name-only", "--", ...written)) {
    git("commit", "-m", "Transcripts", "--", ...written);
    console.log("Committed. Now run: git push");
  } else {
    console.log("No change since your last export.");
  }
}
