import { useEffect, useState } from 'react'
import { api, getToken, setToken } from './api.js'
import Login from './pages/Login.jsx'
import Classes from './pages/Classes.jsx'
import MyBookings from './pages/MyBookings.jsx'
import MyPlan from './pages/MyPlan.jsx'
import Notifications from './pages/Notifications.jsx'
import Admin from './pages/Admin.jsx'

const PAGES = {
  classes: { title: 'Classes', Component: Classes },
  bookings: { title: 'My Bookings', Component: MyBookings },
  plan: { title: 'My Plan', Component: MyPlan },
  notifications: { title: 'Notifications', Component: Notifications },
  admin: { title: 'Admin', Component: Admin, adminOnly: true },
}

function currentPage() {
  const name = window.location.hash.replace('#/', '')
  return PAGES[name] ? name : 'classes'
}

export default function App() {
  const [me, setMe] = useState(null)
  const [checked, setChecked] = useState(false)
  const [page, setPage] = useState(currentPage())

  useEffect(() => {
    const onHash = () => setPage(currentPage())
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  }, [])

  useEffect(() => {
    if (!getToken()) {
      setChecked(true)
      return
    }
    api.get('/api/me')
      .then(setMe)
      .catch(() => setToken(null))
      .finally(() => setChecked(true))
  }, [])

  if (!checked) return null
  if (!me) return <Login onSignedIn={setMe} />

  const { Component } = PAGES[page]
  return (
    <div className="app">
      <header>
        <span className="brand">SlotBook</span>
        <nav>
          {Object.entries(PAGES)
            .filter(([, p]) => !p.adminOnly || me.admin)
            .map(([key, p]) => (
              <a key={key} href={`#/${key}`} className={key === page ? 'active' : ''}>
                {p.title}
              </a>
            ))}
        </nav>
        <span className="who">
          {me.name}{' '}
          <button
            className="link"
            onClick={() => {
              setToken(null)
              setMe(null)
            }}
          >
            Sign out
          </button>
        </span>
      </header>
      <main>
        <Component me={me} />
      </main>
    </div>
  )
}
