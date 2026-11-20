import { useCallback, useEffect, useState } from 'react'
import { api, rupees } from '../api.js'
import { panelsFor } from '../modules/index.js'

function dayHeading(isoDate) {
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'short' })
}

export default function MyBookings({ me }) {
  const [bookings, setBookings] = useState([])
  const [message, setMessage] = useState(null)

  const load = useCallback(() => {
    api.get('/api/bookings/mine').then(setBookings).catch((e) => setMessage(e.message))
  }, [])

  useEffect(load, [load])

  async function cancel(b) {
    if (!window.confirm(`Cancel ${b.slot.batchName} on ${b.slot.startsAtLocal}?\n\n` +
      'You get 90% back if you cancel at least 24 hours before the class, otherwise 50%.')) return
    try {
      const res = await api.post(`/api/bookings/${b.id}/cancel`)
      setMessage(`Cancelled. ${rupees(res.refund)} will be refunded.`)
    } catch (e) {
      setMessage(e.message)
    }
    load()
  }

  const now = new Date()
  const upcoming = bookings.filter((b) => b.status === 'CONFIRMED' && new Date(b.slot.startsAt) > now)
  const other = bookings.filter((b) => !upcoming.includes(b))

  const days = []
  for (const b of upcoming) {
    if (!days.length || days[days.length - 1].date !== b.date) days.push({ date: b.date, items: [] })
    days[days.length - 1].items.push(b)
  }

  return (
    <div>
      {panelsFor('bookings').map(({ module, Component }) => (
        <Component key={module} me={me} refresh={load} />
      ))}
      {message && <p className="message">{message}</p>}
      <h2>Upcoming</h2>
      {days.length === 0 && <p>Nothing booked.</p>}
      {days.map((d) => (
        <section key={d.date}>
          <h3>{dayHeading(d.date)}</h3>
          {d.items.map((b) => (
            <div key={b.id} className="card row">
              <div>
                <strong>{b.slot.batchName}</strong> · {b.slot.startsAtLocal}
                <div className="muted">Paid {rupees(b.amount)}</div>
              </div>
              <button onClick={() => cancel(b)}>Cancel</button>
            </div>
          ))}
        </section>
      ))}
      <h2>Past and cancelled</h2>
      {other.map((b) => (
        <div key={b.id} className="card muted">
          {b.slot.batchName} · {b.slot.startsAtLocal} · {b.status.toLowerCase()}
        </div>
      ))}
    </div>
  )
}
