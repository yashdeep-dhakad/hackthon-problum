// Tiny fetch wrapper. The token from /api/auth/login is kept in localStorage.

export class ApiError extends Error {
  constructor(status, body) {
    super((body && (body.error || body.message)) || `Request failed (${status})`)
    this.status = status
    this.body = body
  }
}

export function getToken() {
  return localStorage.getItem('slotbook.token')
}

export function setToken(token) {
  if (token) localStorage.setItem('slotbook.token', token)
  else localStorage.removeItem('slotbook.token')
}

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) throw new ApiError(res.status, data)
  return data
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body ?? {}),
  put: (path, body) => request('PUT', path, body),
  del: (path) => request('DELETE', path),
}

// Today's date in the browser, as YYYY-MM-DD.
export function localDateString(d = new Date()) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function rupees(amount) {
  return `₹${Number(amount).toFixed(2)}`
}
