// Front-end parts of modules. Core pages render every panel registered for their placement.
//
// placement 'slot'     -> under each class on the Classes page. Props: { slot, me, refresh }
// placement 'bookings' -> at the top of My Bookings.            Props: { me, refresh }
// placement 'admin'    -> at the bottom of the Admin page.       Props: { me }
//
// To add a module's UI, create web/src/modules/<name>/ and add one entry here.

import RemindersPanel from './reminders/RemindersPanel.jsx'

export const modulePanels = [
  { module: 'reminders', placement: 'bookings', Component: RemindersPanel },
]

export function panelsFor(placement) {
  return modulePanels.filter((p) => p.placement === placement)
}
