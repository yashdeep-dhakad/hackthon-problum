import { useEffect, useState } from 'react'
import { api } from '../api.js'

export default function Notifications() {
  const [items, setItems] = useState([])

  useEffect(() => {
    api.get('/api/notifications').then(setItems)
  }, [])

  async function markRead() {
    await api.post('/api/notifications/read')
    setItems(items.map((n) => ({ ...n, read: true })))
  }

  return (
    <div>
      <button onClick={markRead}>Mark all read</button>
      {items.length === 0 && <p>No notifications.</p>}
      {items.map((n) => (
        <div key={n.id} className={`card ${n.read ? 'muted' : ''}`}>
          {n.message}
          <div className="muted small">{new Date(n.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</div>
        </div>
      ))}
    </div>
  )
}
