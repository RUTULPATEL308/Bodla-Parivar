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

  // Fallback to explicit deployment variables or embedded service account for 100% bulletproof reliability
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL || "firebase-adminsdk-fbsvc@bodla-parivar.iam.gserviceaccount.com";
  const privateKey = process.env.FIREBASE_PRIVATE_KEY || "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDM3BjJeMo3mF/E\nOr3/O5nFEKO5xaZFn6dmkJz9Cs+89d1Jo+CqPpEEyDTk0FXhLHePwS3xV79+dDPd\nJ74zP6fN6Ng71F9rz82qLMjLfOgP8S6qybmtLPj+OhcHNWwbhWSj8kD4cPcrI4UM\nfYer9lPk23g82cAOkGlsnTZOlM+6NtKQBwvZ8hlz6PL45m5VgGyeeB2QGJT4DCpd\nFQM9DvMJh8bTe3qivZEZ7XzFSfvPDvh0bLjIlM+4tJYlfdo3EHK7mKucQoVcfI27\n312PO/M0PhaRh0kKcx5GCTocj+XwHsHrbSTYb7kglFUe2hx/MU+cqa6C4ADOs40o\nAtimAnyBAgMBAAECggEAHrKSgko33xnVq+VziBYJxkZmilVsQ32Xi556784ZcALV\nrI3CgjBK8dEAYBM58zqDFgcIF+SivNOz291zOmG9BvVvxZNe0xx6AJiiZB+s9Iqb\nCBufV6xoCUxyG/Hs57jW6lxIURI4b33sl4EN/f409xPOEiw9A9GvAogSVbcl44zD\nyGDDJydg1Cdf0quVGJPfX+f7SL/FpDGgF3uXyMD1ZjxjzxtPw9U7IX4BLoRWqUAE\nEHycWirGxYJpn+nFhU8PH2ByVOBJEjkaY+wPvv+ACr68jUWtBUV30N57UPnxB99a\n2OdAqwIrYAX/JRcxDIuFazgrdjBVe/Tf6G5PsZDtWwKBgQDxoivFAX9Icj+scFgA\nPWpVQHet12BGaMXdTK3z1F25+6RW/+GMiRiWSoAFsev5tFHibDRwSVx4BHMNrvox\njPc0bDzZ5WdQJ1c/Hxq0hBygVblzzR4AczMsmHq2jt2BgxZgIgFHVYRjx46v7VvK\neii+i+p8CUhrxVBJu3s/NV806wKBgQDZCjRI64rdUp92KImjwC+jmvcctx2Et3bi\nWiuDnFbraz4+RYPT7Jlkuz9cB5WAjr/Qo6UvpaC4W0SyYnvabQ75dAjQQlRizNfr\n59Gp8PTvrKSWW9IiVmf4FPXa9dgwykswNkCXNmNaZl9GgysiTyfgC4FBTFtIVlgL\nb2Q1fFopQwKBgHdxfslnsA+Tyj3nWtjjgwiZUY+xg3nKmOSH3wVE2P/ykI4g9tGN\ng4XnVdl61fcdV8zhSBqpaPg62tTMxghwclKLnpjneWZ5y/r72Suakrv/JClGH6qB\nR66nwSE1BfHbZtgmdxNYBpV8M+BrKJCySinG6CZgQFhltQZ9krLwIxv5AoGBAMmv\nzth4ccmrrt1ZXFZcEKkDJ2F61KZw6rv7OPaLgMYyGVw6sv9rnTvbOUynC4m0nZEf\nkTBOxKSsw5DVLezTbZfn+Qy2pr5NRtVEzjQS4zoZ/7RtzT71dpY1vCXmUSk9w1Mh\nBzK9auQlv6HB4IJ4dkCf0FRCxYzaF+VAdSmyzgILAoGAdnwKWIdsHt+HxJ/06HsC\nQ4Wxrjj00kd2EodvhXt4GXGByQaVZimUx5StRCsIensiCYJo/JzQmW2uwyDM7tDI\nigfu4I+R05pOTXyDhrwDOs96Vdn0UEUusIgbT4n+JSHaYtA4aWBZpL5trgAjo4Zs\nU/dbC9/K8Vf4TF6QXADKqG0=\n-----END PRIVATE KEY-----";
  const projectId = process.env.FIREBASE_PROJECT_ID || process.env.FCM_PROJECT_ID || "bodla-parivar";

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
