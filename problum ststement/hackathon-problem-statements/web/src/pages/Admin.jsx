import { useEffect, useState } from 'react'
import { api, localDateString, rupees } from '../api.js'
import { panelsFor } from '../modules/index.js'

export default function Admin({ me }) {
  const [date, setDate] = useState(localDateString())
  const [slots, setSlots] = useState([])
  const [open, setOpen] = useState(null)
  const [attendance, setAttendance] = useState([])
  const [month, setMonth] = useState('2026-10')
  const [report, setReport] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.get(`/api/slots?date=${date}`).then(setSlots).catch((e) => setError(e.message))
  }, [date])

  async function showAttendance(slot) {
    setOpen(slot.id)
    setAttendance(await api.get(`/api/admin/slots/${slot.id}/attendance`))
  }

  async function replayInbox(e) {
    const file = e.target.files[0]
    if (!file) return
    const res = await fetch('/api/admin/inbox/replay', {
      method: 'POST',
      headers: { Authorization: `Bearer ${localStorage.getItem('slotbook.token')}`, 'Content-Type': 'text/plain' },
      body: await file.text(),
    })
    const body = await res.json().catch(() => ({}))
    setError(res.ok ? null : body.error || `Replay failed (${res.status})`)
    if (res.ok) window.alert(`Sent ${body.sent} messages to the inbox (${body.skipped} skipped).`)
    e.target.value = ''
  }

  async function loadReport() {
    try {
      setReport(await api.get(`/api/admin/reports/refunds?month=${month}`))
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div>
      {error && <p className="error">{error}</p>}
      <h2>Attendance</h2>
      <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      {slots.map((slot) => (
        <div key={slot.id} className="card">
          <button className="link" onClick={() => showAttendance(slot)}>
            {slot.batchName} · {slot.startsAtLocal} · {slot.booked}/{slot.capacity}
          </button>
          {open === slot.id && (
            <ol>
              {attendance.map((a) => (
                <li key={a.bookingId}>{a.memberName} ({a.email})</li>
              ))}
            </ol>
          )}
        </div>
      ))}

      <h2>Refunds</h2>
      <input value={month} onChange={(e) => setMonth(e.target.value)} placeholder="YYYY-MM" />
      <button onClick={loadReport}>Show</button>
      {report && (
        <p>
          {report.refunds} refunds in {report.month}, total {rupees(report.total)}
        </p>
      )}

      <h2>Inbox</h2>
      <label className="muted">
        Replay inbox messages from a file (one JSON object per line):{' '}
        <input type="file" accept=".jsonl,.json,.txt" onChange={replayInbox} />
      </label>

      {panelsFor('admin').map(({ module, Component }) => (
        <Component key={module} me={me} />
      ))}
    </div>
  )
}
