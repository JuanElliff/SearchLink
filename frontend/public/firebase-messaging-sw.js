// Service worker para mensajes Firebase en background (app cerrada o tab inactivo).
// Estos valores son configuración pública del proyecto Firebase (no secretos).
// Completar con los valores del proyecto antes de usar.

importScripts('https://www.gstatic.com/firebasejs/10.12.2/firebase-app-compat.js')
importScripts('https://www.gstatic.com/firebasejs/10.12.2/firebase-messaging-compat.js')

firebase.initializeApp({
  apiKey: 'AIzaSyCKXNCMRYSmjwmLMlU7D73ftB-DYOnJM9w',
  authDomain: 'searchlink-firebase.firebaseapp.com',
  projectId: 'searchlink-firebase',
  messagingSenderId: '292713464775',
  appId: '1:292713464775:web:015b01c7253d4386d58fe2',
})

const messaging = firebase.messaging()

messaging.onBackgroundMessage(payload => {
  const title = payload.data?.title ?? 'SearchLink'
  const body = payload.data?.body ?? ''
  const alertaId = payload.data?.alertaId
  self.registration.showNotification(title, {
    body,
    icon: '/icon.svg',
    requireInteraction: true,
    data: { alertaId },
  })
})

self.addEventListener('notificationclick', event => {
  event.notification.close()
  const alertaId = event.notification.data?.alertaId
  const url = alertaId ? `/alerta/${alertaId}` : '/'
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(wins => {
      const open = wins.find(w => w.url.includes('/alerta/') && 'focus' in w)
      if (open) return open.focus()
      return clients.openWindow(url)
    })
  )
})
