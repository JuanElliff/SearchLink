import { getToken, onMessage } from 'firebase/messaging'
import { messaging } from './firebase'
import { apiFetch } from '../api/client'

const FCM_REGISTERED_KEY = 'searchlink.fcm-registered'

export function fcmConfigurado() {
  return Boolean(messaging)
}

export function fcmRegistrado() {
  return localStorage.getItem(FCM_REGISTERED_KEY) === '1'
}

export async function suscribirFCM() {
  if (!messaging) return { estado: 'no-configurado' }
  if (!('Notification' in window)) return { estado: 'no-soportado' }

  const permiso = await Notification.requestPermission()
  if (permiso !== 'granted') return { estado: 'denegado' }

  const swReg = await navigator.serviceWorker.register('/firebase-messaging-sw.js', { scope: '/firebase-sw/' })
  const token = await getToken(messaging, {
    vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
    serviceWorkerRegistration: swReg,
  })
  if (!token) return { estado: 'sin-token' }

  await apiFetch('/api/dispositivos', {
    method: 'POST',
    body: { fcmToken: token, plataforma: 'WEB' },
  })

  localStorage.setItem(FCM_REGISTERED_KEY, '1')
  return { estado: 'activo' }
}

// Devuelve la función de unsuscribe de Firebase.
export function escucharMensajesForeground(callback) {
  if (!messaging) return () => {}
  return onMessage(messaging, callback)
}
