export const MAX_NOTIFICATION_REQUEST_BYTES = 16 * 1024;
const MAX_TITLE_LENGTH = 200;
const MAX_BODY_LENGTH = 4_000;
const supportedTypes = new Set([
  'NOTICE', 'EVENT', 'COMPLAINT', 'OFFERING', 'BUSINESS', 'JOB',
  'SYSTEM_BROADCAST', 'BID_CALL', 'BID_APPROVED',
]);
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export type NotificationInput = {
  target: 'topic' | 'tokens';
  title_gu: string;
  title_en?: string;
  body_gu: string;
  body_en?: string;
  type: string;
  reference_id?: string;
};

export type ValidationFailure = { ok: false; status: number; error: string };
export type ValidationSuccess = { ok: true; value: NotificationInput };

export function isNotificationValidationFailure(
  result: Record<string, unknown> | ValidationFailure,
): result is ValidationFailure {
  return 'ok' in result && result.ok === false && typeof result.status === 'number' && typeof result.error === 'string';
}

export async function readNotificationJson(request: Request): Promise<Record<string, unknown> | ValidationFailure> {
  const declaredLength = Number(request.headers.get('content-length') ?? 0);
  if (Number.isFinite(declaredLength) && declaredLength > MAX_NOTIFICATION_REQUEST_BYTES) {
    return { ok: false, status: 413, error: 'Notification request is too large.' };
  }

  try {
    const text = await request.text();
    if (new TextEncoder().encode(text).byteLength > MAX_NOTIFICATION_REQUEST_BYTES) {
      return { ok: false, status: 413, error: 'Notification request is too large.' };
    }
    const value: unknown = JSON.parse(text);
    if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('Expected an object');
    return value as Record<string, unknown>;
  } catch {
    return { ok: false, status: 400, error: 'Notification request must be valid JSON.' };
  }
}

export function validateNotificationInput(body: Record<string, unknown>): ValidationFailure | ValidationSuccess {
  const target = body.target ?? 'topic';
  if (target !== 'topic' && target !== 'tokens') {
    return { ok: false, status: 400, error: 'Unsupported notification target.' };
  }
  const type = body.type ?? 'NOTICE';
  if (typeof type !== 'string' || !supportedTypes.has(type)) {
    return { ok: false, status: 400, error: 'Unsupported notification type.' };
  }

  const titleGu = body.title_gu;
  const bodyGu = body.body_gu;
  if (typeof titleGu !== 'string' || !titleGu.trim() || typeof bodyGu !== 'string' || !bodyGu.trim()) {
    return { ok: false, status: 400, error: 'Title and Body in Gujarati are required.' };
  }
  if (titleGu.length > MAX_TITLE_LENGTH || bodyGu.length > MAX_BODY_LENGTH) {
    return { ok: false, status: 400, error: 'Notification title or body is too long.' };
  }

  const titleEn = body.title_en;
  const bodyEn = body.body_en;
  if ((titleEn !== undefined && typeof titleEn !== 'string') || (bodyEn !== undefined && typeof bodyEn !== 'string')) {
    return { ok: false, status: 400, error: 'Optional English title and body must be text.' };
  }
  if ((typeof titleEn === 'string' && titleEn.length > MAX_TITLE_LENGTH) || (typeof bodyEn === 'string' && bodyEn.length > MAX_BODY_LENGTH)) {
    return { ok: false, status: 400, error: 'Notification title or body is too long.' };
  }

  const referenceId = body.reference_id;
  if (referenceId !== undefined && (typeof referenceId !== 'string' || !uuidPattern.test(referenceId))) {
    return { ok: false, status: 400, error: 'Invalid notification reference.' };
  }

  return {
    ok: true,
    value: {
      target,
      title_gu: titleGu.trim(),
      title_en: typeof titleEn === 'string' && titleEn.trim() ? titleEn.trim() : undefined,
      body_gu: bodyGu.trim(),
      body_en: typeof bodyEn === 'string' && bodyEn.trim() ? bodyEn.trim() : undefined,
      type,
      reference_id: referenceId,
    },
  };
}
