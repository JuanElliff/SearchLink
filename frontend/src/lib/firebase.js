import { initializeApp } from 'firebase/app'
import { getMessaging } from 'firebase/messaging'

let messaging = null

if (import.meta.env.VITE_FIREBASE_API_KEY) {
  try {
    const app = initializeApp({
      apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
      authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
      projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
      messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
      appId: import.meta.env.VITE_FIREBASE_APP_ID,
    })
    messaging = getMessaging(app)
  } catch {
    // Firebase no disponible; FCM degradado graciosamente
  }
}

export { messaging }
