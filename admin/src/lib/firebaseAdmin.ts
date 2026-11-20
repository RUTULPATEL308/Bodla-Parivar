import { getApps, initializeApp, cert, type App } from 'firebase-admin/app';
import { getMessaging } from 'firebase-admin/messaging';

function getServiceAccount(): any | null {
  const configuredKey = process.env.FCM_SERVICE_ACCOUNT_KEY;
  const directJson = configuredKey?.trim().startsWith('{') ? configuredKey : null;

  if (directJson) {
    try {
      return JSON.parse(directJson.trim());
    } catch (e) {
      console.error('Failed to parse FCM_SERVICE_ACCOUNT_KEY JSON:', e);
    }
  }

  // Use deployment secrets only. Never keep a Firebase service account private key in source.
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
  const privateKey = process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n');
  const projectId = process.env.FIREBASE_PROJECT_ID || process.env.FCM_PROJECT_ID || "bodla-parivar";

  if (!clientEmail || !privateKey) return null;

  return {
    projectId,
    clientEmail,
    privateKey,
  };
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
