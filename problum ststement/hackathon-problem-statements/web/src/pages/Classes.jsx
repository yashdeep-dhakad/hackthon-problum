import { useCallback, useEffect, useState } from 'react'
import { api, localDateString, rupees } from '../api.js'
import { panelsFor } from '../modules/index.js'

function addDays(date, days) {
  const d = new Date(date)
  d.setDate(d.getDate() + days)
  return d
}

export default function Classes({ me }) {
  const [tab, setTab] = useState('today')
  const [slots, setSlots] = useState([])
  const [message, setMessage] = useState(null)
  const [bookingSlotId, setBookingSlotId] = useState(null)

  const load = useCallback(() => {
    const today = new Date()
    const query = tab === 'today'
      ? `date=${localDateString(today)}`
      : `from=${localDateString(today)}&to=${localDateString(addDays(today, 6))}`
    api.get(`/api/slots?${query}`).then(setSlots).catch((e) => setMessage(e.message))
  }, [tab])

  useEffect(load, [load])

  async function book(slot) {
    if (bookingSlotId) return
    setBookingSlotId(slot.id)
    setMessage(null)
    try {
      await api.post('/api/bookings', { slotId: slot.id })
      setMessage(`Booked ${slot.batchName}, ${slot.startsAtLocal}. ${rupees(slot.price)} charged.`)
    } catch (e) {
      const outcome = e.body && e.body.outcome
      setMessage(outcome === 'FULL' ? 'Sorry, that class is full.'
        : outcome === 'ALREADY_BOOKED' ? 'You are already booked into that class.'
        : e.message)
    } finally {
      setBookingSlotId(null)
    }
    load()
  }

  // Group by day for the week view.
  const groups = []
  for (const slot of slots) {
    const day = slot.startsAtLocal ? slot.startsAtLocal.split(',')[0] : new Date(slot.startsAt).toDateString()
    if (!groups.length || groups[groups.length - 1].day !== day) groups.push({ day, slots: [] })
    groups[groups.length - 1].slots.push(slot)
  }

  return (
    <div>
      <div className="tabs">
        <button className={tab === 'today' ? 'active' : ''} onClick={() => setTab('today')}>Today</button>
        <button className={tab === 'week' ? 'active' : ''} onClick={() => setTab('week')}>This week</button>
      </div>
      {message && <p className="message">{message}</p>}
      {slots.length === 0 && <p>No classes.</p>}
      {groups.map((g) => (
        <section key={g.day}>
          {tab === 'week' && <h3>{g.day}</h3>}
          {g.slots.map((slot) => {
            const full = slot.booked >= slot.capacity
            const started = new Date(slot.startsAt) <= new Date()
            return (
              <div key={slot.id} className="card">
                <div className="row">
                  <div>
                    <strong>{slot.batchName}</strong> · {slot.startsAtLocal} · {slot.durationMinutes} min
                    <div className="muted">
                      {slot.instructor} · {slot.booked}/{slot.capacity} booked · {rupees(slot.price)}
                    </div>
                    {slot.notes && <div className="notes">{slot.notes}</div>}
                  </div>
                  <button disabled={full || started || bookingSlotId === slot.id} onClick={() => book(slot)}>
                    {started ? 'Started' : full ? 'Full' : bookingSlotId === slot.id ? 'Booking...' : 'Book'}
                  </button>
                </div>
                {panelsFor('slot').map(({ module, Component }) => (
                  <Component key={module} slot={slot} me={me} refresh={load} />
                ))}
              </div>
            )
          })}
        </section>
      ))}
    </div>
  )
}
