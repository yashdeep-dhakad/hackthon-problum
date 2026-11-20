import { useState } from 'react'
import { api, setToken } from '../api.js'

export default function Login({ onSignedIn }) {
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState(null)

  async function submit(e) {
    e.preventDefault()
    setError(null)
    try {
      const res = mode === 'login'
        ? await api.post('/api/auth/login', { email: form.email, password: form.password })
        : await api.post('/api/auth/signup', form)
      setToken(res.token)
      onSignedIn(res.member)
    } catch (err) {
      setError(err.message)
    }
  }

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  return (
    <div className="login">
      <h1>SlotBook</h1>
      <form onSubmit={submit}>
        {mode === 'signup' && <input placeholder="Your name" value={form.name} onChange={set('name')} />}
        <input placeholder="Email" value={form.email} onChange={set('email')} />
        <input placeholder="Password" type="password" value={form.password} onChange={set('password')} />
        <button type="submit">{mode === 'login' ? 'Sign in' : 'Create account'}</button>
        {error && <p className="error">{error}</p>}
      </form>
      <button className="link" onClick={() => setMode(mode === 'login' ? 'signup' : 'login')}>
        {mode === 'login' ? 'New here? Create an account' : 'Have an account? Sign in'}
      </button>
      <p className="hint">Demo logins are in README.md.</p>
    </div>
  )
}
