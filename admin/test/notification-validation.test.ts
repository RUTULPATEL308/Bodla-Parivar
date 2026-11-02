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

test('keeps the lock screen title and the notification picture', () => {
  const result = validateNotificationInput({
    ...validInput,
    notification_title_gu: ' અંતિમ યાત્રા / સ્મશાન યાત્રા ',
    image_url: ' https://demo.supabase.co/storage/v1/object/public/antim-yatra-posters/uid/poster.png ',
  });
  assert.equal(result.ok, true);
  if (result.ok) {
    assert.equal(result.value.notification_title_gu, 'અંતિમ યાત્રા / સ્મશાન યાત્રા');
    assert.equal(result.value.image_url, 'https://demo.supabase.co/storage/v1/object/public/antim-yatra-posters/uid/poster.png');
  }
});

test('rejects pictures that are not public storage urls', () => {
  assert.equal(validateNotificationInput({ ...validInput, image_url: 'http://demo.supabase.co/x.png' }).ok, false);
  assert.equal(validateNotificationInput({ ...validInput, image_url: 'https://evil.test/photo.png' }).ok, false);
  assert.equal(validateNotificationInput({ ...validInput, image_url: 42 }).ok, false);
});

test('rejects a lock screen title that is not text', () => {
  assert.equal(validateNotificationInput({ ...validInput, notification_title_gu: {} }).ok, false);
  assert.equal(validateNotificationInput({ ...validInput, notification_title_en: [] }).ok, false);
});
