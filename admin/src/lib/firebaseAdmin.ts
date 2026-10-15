import { getApps, initializeApp, cert, type App } from 'firebase-admin/app';
import { getMessaging } from 'firebase-admin/messaging';

function getServiceAccount(): any | null {
  const configuredKey = process.env.FCM_SERVICE_ACCOUNT_KEY;
  const directJson = configuredKey?.trim().startsWith('{') ? configuredKey : null;

  if (directJson) {
    try {
      return JSON.parse(directJson);
    } catch (e) {
      console.error('Failed to parse FCM_SERVICE_ACCOUNT_KEY JSON:', e);
    }
  }

  // Prefer deployment secrets over credential files. This also keeps the
  // notification route's server bundle from tracing arbitrary filesystem paths.
  if (process.env.FIREBASE_PRIVATE_KEY && process.env.FIREBASE_CLIENT_EMAIL) {
    return {
      projectId: process.env.FIREBASE_PROJECT_ID || process.env.FCM_PROJECT_ID || 'bodla-parivar',
      clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
    };
  }

  return null;
}

export function getFirebaseAdminApp(): App {
  const currentApps = getApps();
  if (currentApps.length > 0) {
    return currentApps[0]!;
  }

  const serviceAccount = getServiceAccount();
  if (!serviceAccount) {
    throw new Error('Firebase Service Account credentials not found in admin root or environment variables.');
  }

  return initializeApp({
    credential: cert(serviceAccount),
    projectId: process.env.FIREBASE_PROJECT_ID || process.env.FCM_PROJECT_ID || undefined,
  });
}

export { getMessaging };
