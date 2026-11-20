import { useEffect, useState } from 'react'
import { api } from '../../api.js'

export default function RemindersPanel() {
  const [settings, setSettings] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.get('/api/modules/reminders/settings').then(setSettings).catch((e) => setError(e.message))
  }, [])

  async function save(next) {
    try {
      setSettings(await api.put('/api/modules/reminders/settings', next))
      setError(null)
    } catch (e) {
      setError(e.message)
    }
  }

  if (!settings) return error ? <p className="error">{error}</p> : null

  return (
    <div className="panel">
      <label>
        <input
          type="checkbox"
          checked={settings.enabled}
          onChange={(e) => save({ ...settings, enabled: e.target.checked })}
        />{' '}
        Remind me before my classes
      </label>
      {settings.enabled && (
        <select
          value={settings.minutesBefore}
          onChange={(e) => save({ ...settings, minutesBefore: Number(e.target.value) })}
        >
          <option value={30}>30 minutes before</option>
          <option value={60}>1 hour before</option>
          <option value={120}>2 hours before</option>
          <option value={1440}>1 day before</option>
        </select>
      )}
      {error && <p className="error">{error}</p>}
    </div>
  )
}
