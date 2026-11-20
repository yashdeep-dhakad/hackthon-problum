import { useEffect, useState } from 'react'
import { api, rupees } from '../api.js'

export default function MyPlan() {
  const [plan, setPlan] = useState(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    api.get('/api/me/plan').then(setPlan).catch(() => setFailed(true))
  }, [])

  if (failed) {
    return (
      <div className="error-page">
        <h2>Something went wrong</h2>
        <p>We could not load this page. Please try again later.</p>
      </div>
    )
  }
  if (!plan) return null

  return (
    <div className="card">
      <h2>{plan.plan}</h2>
      <p>{rupees(plan.monthlyPrice)} a month · {plan.classesPerMonth} classes a month</p>
      <p>Used this month: {plan.classesUsedThisMonth} of {plan.classesPerMonth}</p>
    </div>
  )
}
