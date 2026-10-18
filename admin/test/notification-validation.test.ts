import assert from 'node:assert/strict';
import test from 'node:test';
// @ts-expect-error Node's native TypeScript loader requires an explicit extension.
import { MAX_NOTIFICATION_REQUEST_BYTES, readNotificationJson, validateNotificationInput } from '../src/lib/notificationValidation.ts';

const validInput = { title_gu: 'સૂચના', body_gu: 'સમુદાય માટે સંદેશ' };

test('rejects malformed JSON', async () => {
  const result = await readNotificationJson(new Request('https://example.test', { method: 'POST', body: '{' }));
  assert.deepEqual(result, { ok: false, status: 400, error: 'Notification request must be valid JSON.' });
});

test('rejects oversized bodies before validation', async () => {
  const body = JSON.stringify({ ...validInput, body_gu: 'a'.repeat(MAX_NOTIFICATION_REQUEST_BYTES) });
  const result = await readNotificationJson(new Request('https://example.test', { method: 'POST', body }));
  assert.deepEqual(result, { ok: false, status: 413, error: 'Notification request is too large.' });
});

test('rejects invalid targets and optional fields', () => {
  assert.equal(validateNotificationInput({ ...validInput, target: 'broadcast' }).ok, false);
  assert.equal(validateNotificationInput({ ...validInput, title_en: {} }).ok, false);
  assert.equal(validateNotificationInput({ ...validInput, reference_id: 'not-a-uuid' }).ok, false);
});

test('accepts a bounded topic notification and normalizes text', () => {
  const result = validateNotificationInput({ ...validInput, title_gu: '  સૂચના  ', body_gu: ' સંદેશ ', type: 'NOTICE' });
  assert.equal(result.ok, true);
  if (result.ok) {
    assert.equal(result.value.target, 'topic');
    assert.equal(result.value.title_gu, 'સૂચના');
    assert.equal(result.value.body_gu, 'સંદેશ');
  }
});
