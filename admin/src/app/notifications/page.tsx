'use client';

import React, { useState } from 'react';
import { Send, CheckCircle2, AlertCircle, Bell, Users, Radio, Sparkles } from 'lucide-react';
import { supabase } from '@/lib/supabase';
import { translateText } from '@/lib/translation';

export default function NotificationsBroadcastPage() {
  const [titleGu, setTitleGu] = useState('');
  const [titleEn, setTitleEn] = useState('');
  const [bodyGu, setBodyGu] = useState('');
  const [bodyEn, setBodyEn] = useState('');
  const [type, setType] = useState('NOTICE');
  const [target, setTarget] = useState<'topic' | 'tokens'>('topic');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<{ success: boolean; message: string; details?: any } | null>(null);
  const [translating, setTranslating] = useState(false);

  const translateEnglishContent = async (field: 'title' | 'body') => {
    const sourceText = (field === 'title' ? titleEn : bodyEn).trim();
    if (!sourceText || (field === 'title' ? titleGu : bodyGu).trim()) return;
    setTranslating(true);
    try {
      const translated = await translateText(sourceText);
      if (field === 'title') setTitleGu(translated);
      else setBodyGu(translated);
    } catch (error) {
      setResult({ success: false, message: error instanceof Error ? error.message : 'Translation failed.' });
    } finally {
      setTranslating(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!titleGu.trim() || !bodyGu.trim()) {
      setResult({
        success: false,
        message: 'કૃપા કરીને શીર્ષક અને સંદેશ ગુજરાતીમાં ભરો (Gujarati title & body are required).',
      });
      return;
    }

    setLoading(true);
    setResult(null);

    try {
      const { data: sessionData, error: sessionError } = await supabase.auth.getSession();
      const accessToken = sessionData.session?.access_token;
      if (sessionError || !accessToken) throw new Error('Sign in again to send this notification.');
      const res = await fetch('/api/notifications/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` },
        body: JSON.stringify({
          title_gu: titleGu.trim(),
          title_en: titleEn.trim() || titleGu.trim(),
          body_gu: bodyGu.trim(),
          body_en: bodyEn.trim() || bodyGu.trim(),
          type,
          target,
        }),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || 'Failed to dispatch push notification');
      }

      setResult({
        success: true,
        message: 'સૂચના સફળતાપૂર્વક તમામ મોબાઇલ વપરાશકર્તાઓને મોકલાઈ ગઈ છે! (Notification sent successfully to all mobile devices!)',
        details: data,
      });

      // Reset form
      setTitleGu('');
      setTitleEn('');
      setBodyGu('');
      setBodyEn('');
    } catch (err: any) {
      setResult({
        success: false,
        message: err.message || 'Error sending push notification',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-brand-text font-gujarati flex items-center gap-2">
            <Send className="w-7 h-7 text-saffron" />
            <span>મોબાઇલ સૂચના પ્રસારણ (Broadcast Mobile Notification)</span>
          </h1>
          <p className="text-xs text-brand-muted mt-1">
            Send real-time Push Notifications directly to all Bodla Parivar mobile users via Firebase Cloud Messaging (FCM).
          </p>
        </div>
      </div>

      {/* Status Alert */}
      {result && (
        <div
          className={`p-4 rounded-xl border flex items-start gap-3 transition ${
            result.success
              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
              : 'bg-rose-50 border-rose-200 text-rose-800'
          }`}
        >
          {result.success ? (
            <CheckCircle2 className="w-5 h-5 mt-0.5 shrink-0 text-emerald-600" />
          ) : (
            <AlertCircle className="w-5 h-5 mt-0.5 shrink-0 text-rose-600" />
          )}
          <div className="text-sm">
            <p className="font-semibold">{result.message}</p>
            {result.details?.fcm && (
              <p className="text-xs opacity-75 mt-1">
                FCM Mode: {result.details.fcm.mode || 'all_users'} | Dispatch Status: Sent
              </p>
            )}
          </div>
        </div>
      )}

      {/* Main Broadcast Form Card */}
      <div className="bg-cream-surface border border-brand-border rounded-2xl p-6 shadow-sm">
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Target Audience & Notification Type */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-brand-muted uppercase mb-1">
                ટાર્ગેટ (Target Audience)
              </label>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setTarget('topic')}
                  className={`flex-1 flex items-center justify-center gap-2 px-3 py-2 text-xs font-semibold rounded-lg border transition ${
                    target === 'topic'
                      ? 'bg-saffron text-white border-saffron shadow-sm'
                      : 'bg-cream text-brand-text border-brand-border hover:bg-cream-hover'
                  }`}
                >
                  <Radio className="w-3.5 h-3.5" />
                  <span>તમામ વપરાશકર્તાઓ (Topic: all_users)</span>
                </button>
                <button
                  type="button"
                  onClick={() => setTarget('tokens')}
                  className={`flex-1 flex items-center justify-center gap-2 px-3 py-2 text-xs font-semibold rounded-lg border transition ${
                    target === 'tokens'
                      ? 'bg-saffron text-white border-saffron shadow-sm'
                      : 'bg-cream text-brand-text border-brand-border hover:bg-cream-hover'
                  }`}
                >
                  <Users className="w-3.5 h-3.5" />
                  <span>ડિવાઇસ ટોકન્સ (Direct Tokens)</span>
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-brand-muted uppercase mb-1">
                કેટેગરી / પ્રકાર (Category)
              </label>
              <select
                value={type}
                onChange={(e) => setType(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-brand-border bg-cream text-sm text-brand-text focus:outline-none focus:ring-2 focus:ring-saffron"
              >
                <option value="NOTICE">સૂચના (Notice)</option>
                <option value="EVENT">કાર્યક્રમ (Event)</option>
                <option value="OFFERING">ચઢાવો / બોલી (Offering / Bidding)</option>
                <option value="COMPLAINT">ફરિયાદ અપડેટ (Complaint Update)</option>
                <option value="SYSTEM_BROADCAST">સામાન્ય પ્રસારણ (System Broadcast)</option>
              </select>
            </div>
          </div>

          {/* Gujarati Content */}
          <div className="bg-amber-50/50 border border-amber-200/60 rounded-xl p-4 space-y-3">
            <div className="flex items-center gap-1.5 text-xs font-bold text-amber-800 uppercase tracking-wide">
              <Sparkles className="w-3.5 h-3.5" />
              <span>ગુજરાતી વિગતો (Gujarati - Primary)</span>
            </div>

            <div>
              <label className="block text-xs font-medium text-brand-text mb-1">
                સૂચનાનું શીર્ષક (Title in Gujarati) *
              </label>
              <input
                type="text"
                required
                value={titleGu}
                onChange={(e) => setTitleGu(e.target.value)}
                placeholder="દા.ત. ગ્રામ પંચાયત સામાન્ય સભાનું આયોજન"
                className="w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm text-brand-text focus:outline-none focus:ring-2 focus:ring-saffron"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-brand-text mb-1">
                સંદેશ / વિગતો (Message Body in Gujarati) *
              </label>
              <textarea
                required
                rows={3}
                value={bodyGu}
                onChange={(e) => setBodyGu(e.target.value)}
                placeholder="દા.ત. આવતીકાલે સવારે ૧૦ વાગ્યે પંચાયત ભવન ખાતે ગામના સર્વે લોકોને હાજર રહેવા વિનંતી..."
                className="w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm text-brand-text focus:outline-none focus:ring-2 focus:ring-saffron"
              />
            </div>
          </div>

          {/* English Content */}
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-3">
            <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700 uppercase tracking-wide">
              <span>English Content (Optional)</span>
            </div>

            <div>
              <label className="block text-xs font-medium text-brand-text mb-1">
                Title in English
              </label>
              <input
                type="text"
                value={titleEn}
                onChange={(e) => setTitleEn(e.target.value)}
                onBlur={() => void translateEnglishContent('title')}
                placeholder="e.g. Gram Panchayat General Meeting"
                className="w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm text-brand-text focus:outline-none focus:ring-2 focus:ring-saffron"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-brand-text mb-1">
                Message Body in English
              </label>
              <textarea
                rows={2}
                value={bodyEn}
                onChange={(e) => setBodyEn(e.target.value)}
                onBlur={() => void translateEnglishContent('body')}
                placeholder="e.g. All villagers are cordially invited to the village general assembly meeting tomorrow at 10 AM."
                className="w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm text-brand-text focus:outline-none focus:ring-2 focus:ring-saffron"
              />
            </div>
          </div>

          {/* Submit Button */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="submit"
              disabled={loading}
              className="px-6 py-2.5 rounded-xl bg-saffron text-white text-sm font-semibold hover:bg-saffron-dark transition shadow-sm disabled:opacity-50 flex items-center gap-2"
            >
              {loading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>મોકલી રહ્યું છે... (Sending...)</span>
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  <span>તમામ મોબાઇલ પર સૂચના મોકલો (Broadcast to All Mobiles)</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
