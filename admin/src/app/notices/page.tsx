'use client';

import React, { useEffect, useRef, useState } from 'react';
import Image from 'next/image';
import { Bell, Bold, FileText, ImagePlus, Italic, LoaderCircle, Pin, Plus, Strikethrough, Video, X, Languages, Eye, Trash2 } from 'lucide-react';
import { Notice } from '@/lib/types';
import { supabase } from '@/lib/supabase';
import { translateText } from '@/lib/translation';
import PrintControls from '@/components/PrintControls';

const MAX_MEDIA_SIZE = 100 * 1024 * 1024;
const MEDIA_ACCEPT = 'image/jpeg,image/png,image/webp,video/mp4,video/webm,video/quicktime,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document';

async function optimizeImage(file: File): Promise<File> {
  if (!file.type.startsWith('image/') || file.size < 700_000) return file;
  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1, 2560 / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  const context = canvas.getContext('2d');
  if (!context) return file;
  context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/webp', 0.96));
  if (!blob || blob.size >= file.size) return file;
  const baseName = file.name.replace(/\.[^.]+$/, '');
  return new File([blob], `${baseName}.webp`, { type: 'image/webp', lastModified: file.lastModified });
}

function wrapSelection(
  input: HTMLTextAreaElement | null,
  value: string,
  setValue: (value: string) => void,
  marker: string
) {
  if (!input) return;
  const start = input.selectionStart;
  const end = input.selectionEnd;
  const selected = value.slice(start, end);
  const updated = `${value.slice(0, start)}${marker}${selected || 'text'}${marker}${value.slice(end)}`;
  setValue(updated);
  requestAnimationFrame(() => {
    input.focus();
    input.setSelectionRange(start + marker.length, start + marker.length + (selected || 'text').length);
  });
}

export default function AdminNoticesPage() {
  const [notices, setNotices] = useState<Notice[]>([]);
  const [titleGu, setTitleGu] = useState('');
  const [titleEn, setTitleEn] = useState('');
  const [bodyGu, setBodyGu] = useState('');
  const [bodyEn, setBodyEn] = useState('');
  const [media, setMedia] = useState<File | null>(null);
  const [optimizing, setOptimizing] = useState(false);
  const [preview, setPreview] = useState<Notice | null>(null);
  const [previewZoom, setPreviewZoom] = useState(1);
  const [isPinned, setIsPinned] = useState(false);
  const [showComposer, setShowComposer] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [translating, setTranslating] = useState(false);
  const [error, setError] = useState('');
  const guInput = useRef<HTMLTextAreaElement>(null);
  const enInput = useRef<HTMLTextAreaElement>(null);

  async function translateEnglishToGujarati() {
    const text = bodyEn.trim();
    if (!text) return;
    setTranslating(true);
    setError('');
    try {
      const segments = text.split(/(\*[^*\n]+\*|_[^_\n]+_|~[^~\n]+~)/g);
      const translated = await Promise.all(segments.map(async (segment) => {
        const match = segment.match(/^(\*|_|~)([\s\S]+)\1$/);
        if (!segment.trim()) return segment;
        if (match) return `${match[1]}${await translateText(match[2])}${match[1]}`;
        return translateText(segment);
      }));
      setBodyGu(translated.join(''));
    } catch (translationError) {
      setError(translationError instanceof Error ? translationError.message : 'Translation failed.');
    } finally {
      setTranslating(false);
    }
  }

  async function translateTitleToGujarati() {
    if (!titleEn.trim()) return;
    setTranslating(true);
    setError('');
    try {
      setTitleGu(await translateText(titleEn.trim()));
    } catch (translationError) {
      setError(translationError instanceof Error ? translationError.message : 'Translation failed.');
    } finally {
      setTranslating(false);
    }
  }

  const formattedPreview = (value: string) => value.split(/(https?:\/\/[^\s<>]+|\*[^*]+\*|_[^_]+_|~[^~]+~)/gi).map((part, index) => {
    if (/^https?:\/\//i.test(part)) {
      const trailing = part.match(/[).,!?:;]+$/)?.[0] ?? '';
      const href = trailing ? part.slice(0, -trailing.length) : part;
      return <React.Fragment key={index}><a href={href} target="_blank" rel="noopener noreferrer" className="break-all text-blue-700 underline">{href}</a>{trailing}</React.Fragment>;
    }
    if (part.startsWith('*') && part.endsWith('*')) return <strong key={index}>{part.slice(1, -1)}</strong>;
    if (part.startsWith('_') && part.endsWith('_')) return <em key={index}>{part.slice(1, -1)}</em>;
    if (part.startsWith('~') && part.endsWith('~')) return <s key={index}>{part.slice(1, -1)}</s>;
    return <React.Fragment key={index}>{part}</React.Fragment>;
  });

  async function loadNotices() {
    setLoading(true);
    const { data, error: loadError } = await supabase
      .from('notices')
      .select('id,title_gu,title_en,description_gu,description_en,image_url,attachment_url,status,is_pinned,publish_at,created_at')
      .order('created_at', { ascending: false });
    if (loadError) setError(loadError.message);
    else {
      setError('');
      setNotices((data ?? []) as Notice[]);
    }
    setLoading(false);
  }

  useEffect(() => {
    let active = true;
    void supabase
      .from('notices')
      .select('id,title_gu,title_en,description_gu,description_en,image_url,attachment_url,status,is_pinned,publish_at,created_at')
      .order('created_at', { ascending: false })
      .then(({ data, error: loadError }) => {
        if (!active) return;
        if (loadError) setError(loadError.message);
        else setNotices((data ?? []) as Notice[]);
        setLoading(false);
      });
    return () => { active = false; };
  }, []);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    if (!titleGu.trim() || !bodyGu.trim()) {
      setError('àª—à«àªœàª°àª¾àª¤à«€ àª¶à«€àª°à«àª·àª• àª…àª¨à«‡ àª¸àª‚àª¦à«‡àª¶ àªœàª°à«‚àª°à«€ àª›à«‡.');
      return;
    }
    if (media && media.size > MAX_MEDIA_SIZE) {
      setError('àª«àª¾àª‡àª² 100 MB àª•àª°àª¤àª¾àª‚ àª¨àª¾àª¨à«€ àª¹à«‹àªµà«€ àªœà«‹àªˆàª.');
      return;
    }

    setSaving(true);
    const noticeId = crypto.randomUUID();
    let uploadedPath: string | null = null;
    try {
      let imageUrl: string | null = null;
      let attachmentUrl: string | null = null;
      if (media) {
        const safeName = media.name.replace(/[^a-zA-Z0-9._-]/g, '_');
        uploadedPath = `${noticeId}/${Date.now()}-${safeName}`;
        const { error: uploadError } = await supabase.storage
          .from('notices')
          .upload(uploadedPath, media, { contentType: media.type, upsert: false });
        if (uploadError) throw uploadError;
        const { data } = supabase.storage.from('notices').getPublicUrl(uploadedPath);
        if (media.type.startsWith('image/')) imageUrl = data.publicUrl;
        else attachmentUrl = data.publicUrl;
      }

      const { error: insertError } = await supabase.from('notices').insert({
        id: noticeId,
        title_gu: titleGu.trim(),
        title_en: titleEn.trim() || titleGu.trim(),
        description_gu: bodyGu.trim(),
        description_en: bodyEn.trim() || bodyGu.trim(),
        image_url: imageUrl,
        attachment_url: attachmentUrl,
        status: 'PUBLISHED',
        is_pinned: isPinned,
        publish_at: new Date().toISOString(),
      });
      if (insertError) throw insertError;

      let notificationWarning = '';
      const { data: sessionData } = await supabase.auth.getSession();
      const accessToken = sessionData.session?.access_token;
      if (!accessToken) {
        notificationWarning = 'Notice published, but notification could not be sent because your session expired.';
      } else {
        try {
          const notificationResponse = await fetch('/api/notifications/send', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${accessToken}`,
            },
            body: JSON.stringify({
              title_gu: titleGu.trim(),
              title_en: titleEn.trim() || titleGu.trim(),
              body_gu: bodyGu.trim(),
              body_en: bodyEn.trim() || bodyGu.trim(),
              type: 'NOTICE',
              target: 'topic',
              reference_id: noticeId,
            }),
          });
          const notificationResult = await notificationResponse.json();
          if (!notificationResponse.ok) {
            notificationWarning = notificationResult.error || 'Notice published, but its notification could not be sent.';
          }
        } catch {
          notificationWarning = 'Notice published, but its notification could not be sent.';
        }
      }

      setTitleGu('');
      setTitleEn('');
      setBodyGu('');
      setBodyEn('');
      setMedia(null);
      setIsPinned(false);
      setShowComposer(false);
      if (notificationWarning) setError(notificationWarning);
      await loadNotices();
    } catch (submitError) {
      if (uploadedPath) await supabase.storage.from('notices').remove([uploadedPath]);
      setError(submitError instanceof Error ? submitError.message : 'àª¸à«‚àªšàª¨àª¾ àªªà«àª°àª•àª¾àª¶àª¿àª¤ àª¥àªˆ àª¶àª•à«€ àª¨àª¥à«€.');
    } finally {
      setSaving(false);
    }
  }

  async function deleteNotice(notice: Notice) {
    if (!window.confirm(`Permanently delete â€œ${notice.title_en || notice.title_gu}â€ and its uploaded media?`)) return;
    setError('');
    const { data: deletedRow, error: deleteError } = await supabase
      .from('notices')
      .delete()
      .eq('id', notice.id)
      .select('id')
      .maybeSingle();
    if (deleteError) {
      setError(`Could not delete notice: ${deleteError.message}`);
      return;
    }
    if (!deletedRow) {
      setError('Could not delete notice. No row was deleted. Check staff access and that migration 024 was applied to the admin Supabase project.');
      return;
    }

    const mediaPaths = [notice.image_url, notice.attachment_url]
      .map((url) => {
        if (!url) return null;
        const marker = '/storage/v1/object/public/notices/';
        const markerIndex = url.indexOf(marker);
        return markerIndex < 0 ? null : decodeURIComponent(url.slice(markerIndex + marker.length).split('?')[0]);
      })
      .filter((path): path is string => Boolean(path));
    if (mediaPaths.length) {
      const { error: mediaError } = await supabase.storage.from('notices').remove(mediaPaths);
      if (mediaError) setError(`Notice deleted, but uploaded media could not be removed: ${mediaError.message}`);
    }
    await loadNotices();
  }

  const toolbarButton = (label: string, marker: string, icon: React.ReactNode, input: React.RefObject<HTMLTextAreaElement>, value: string, setValue: (value: string) => void) => (
    <button
      key={label}
      type="button"
      title={label}
      aria-label={label}
      onClick={() => wrapSelection(input.current, value, setValue, marker)}
      className="grid h-8 w-8 place-items-center border-r border-brand-border text-brand-text hover:bg-saffron-container last:border-r-0"
    >
      {icon}
    </button>
  );

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h1 className="flex items-center gap-2 font-gujarati text-2xl font-bold text-brand-text">
            <Bell className="h-7 w-7 text-saffron" />
            <span>àª—à«àª°àª¾àª® àª¸à«‚àªšàª¨àª¾àª“ (Notices)</span>
          </h1>
          <p className="mt-1 text-sm text-brand-muted">Publish community updates with photos, videos, documents, and formatted text.</p>
        </div>
        <div className="no-print flex flex-wrap items-center gap-2"><PrintControls /><button
          type="button"
          onClick={() => setShowComposer((open) => !open)}
          className="inline-flex items-center justify-center gap-2 bg-saffron px-4 py-2.5 text-sm font-semibold text-white hover:bg-saffron-dark"
        >
          {showComposer ? <X className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
          {showComposer ? 'àª¬àª‚àª§ àª•àª°à«‹' : 'àª¨àªµà«€ àªªà«‹àª¸à«àªŸ'}
        </button></div>
      </header>

      {error && <p role="alert" className="border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">{error}</p>}

      {showComposer && (
        <form onSubmit={handleSubmit} className="space-y-5 border border-brand-border bg-cream-surface p-5 sm:p-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="space-y-1 text-sm font-semibold text-brand-text">
              àª—à«àªœàª°àª¾àª¤à«€ àª¶à«€àª°à«àª·àª• *
              <input required value={titleGu} onChange={(event) => setTitleGu(event.target.value)} className="w-full border border-brand-border bg-white px-3 py-2 font-normal outline-none focus:border-saffron" />
            </label>
            <label className="space-y-1 text-sm font-semibold text-brand-text">
              English title
              <input value={titleEn} onChange={(event) => setTitleEn(event.target.value)} onBlur={() => void translateTitleToGujarati()} className="w-full border border-brand-border bg-white px-3 py-2 font-normal outline-none focus:border-saffron" />
              <button type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => void translateTitleToGujarati()} disabled={translating || !titleEn.trim()} className="mt-1 inline-flex items-center gap-1.5 rounded border border-brand-border px-2 py-1 text-xs font-semibold disabled:opacity-50"><Languages className="h-3.5 w-3.5" /> Translate title to Gujarati</button>
            </label>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="space-y-1 text-sm font-semibold text-brand-text">
              àª—à«àªœàª°àª¾àª¤à«€ àª¸àª‚àª¦à«‡àª¶ *
              <div className="flex border border-brand-border bg-white">
                {toolbarButton('Bold', '*', <Bold className="h-4 w-4" />, guInput, bodyGu, setBodyGu)}
                {toolbarButton('Italic', '_', <Italic className="h-4 w-4" />, guInput, bodyGu, setBodyGu)}
                {toolbarButton('Strikethrough', '~', <Strikethrough className="h-4 w-4" />, guInput, bodyGu, setBodyGu)}
              </div>
              <textarea ref={guInput} required rows={6} value={bodyGu} onChange={(event) => setBodyGu(event.target.value)} placeholder="àª²àª–àª¾àª£ àªªàª¸àª‚àª¦ àª•àª°à«‹ àª…àª¨à«‡ àª‰àªªàª°àª¨àª¾ àª¬àªŸàª¨àª¥à«€ àª«à«‹àª°à«àª®à«‡àªŸ àª•àª°à«‹" className="w-full resize-y border border-brand-border bg-white px-3 py-2 font-normal outline-none focus:border-saffron" />
            </label>
            <label className="space-y-1 text-sm font-semibold text-brand-text">
              English message
              <button type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => void translateEnglishToGujarati()} disabled={translating || !bodyEn.trim()} className="mb-1 inline-flex items-center gap-1.5 rounded border border-brand-border px-2 py-1 text-xs font-semibold disabled:opacity-50">
                {translating ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" /> : <Languages className="h-3.5 w-3.5" />}
                {translating ? 'Translatingâ€¦' : 'Translate to Gujarati'}
              </button>
              <div className="flex border border-brand-border bg-white">
                {toolbarButton('Bold', '*', <Bold className="h-4 w-4" />, enInput, bodyEn, setBodyEn)}
                {toolbarButton('Italic', '_', <Italic className="h-4 w-4" />, enInput, bodyEn, setBodyEn)}
                {toolbarButton('Strikethrough', '~', <Strikethrough className="h-4 w-4" />, enInput, bodyEn, setBodyEn)}
              </div>
              <textarea ref={enInput} rows={6} value={bodyEn} onChange={(event) => setBodyEn(event.target.value)} onBlur={() => void translateEnglishToGujarati()} placeholder="English message (Gujarati is translated when you leave this field)" className="w-full resize-y border border-brand-border bg-white px-3 py-2 font-normal outline-none focus:border-saffron" />
            </label>
          </div>

          <div className="flex flex-col gap-3 border-t border-brand-border pt-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-wrap items-center gap-3">
              <label className="inline-flex cursor-pointer items-center gap-2 border border-brand-border bg-white px-3 py-2 text-sm font-medium text-brand-text hover:bg-cream">
                <ImagePlus className="h-4 w-4 text-leaf" />
                <span>{media ? media.name : 'àª®à«€àª¡àª¿àª¯àª¾ àªœà«‹àª¡à«‹'}</span>
                <input type="file" accept={MEDIA_ACCEPT} className="sr-only" onChange={async (event) => {
                  const selected = event.target.files?.[0];
                  if (!selected) return;
                  setOptimizing(true);
                  try { setMedia(await optimizeImage(selected)); }
                  catch { setMedia(selected); }
                  finally { setOptimizing(false); }
                }} />
              </label>
              {media && <button type="button" onClick={() => setMedia(null)} aria-label="Remove attachment" className="p-2 text-brand-muted hover:text-rose-700"><X className="h-4 w-4" /></button>}
              <span className="text-xs text-brand-muted">{optimizing ? 'Optimizing imageâ€¦' : 'Image, video, PDF or Word document Â· max 100 MB. Large images are resized and optimized as WebP.'}</span>
            </div>
            <label className="inline-flex items-center gap-2 text-sm text-brand-text">
              <input type="checkbox" checked={isPinned} onChange={(event) => setIsPinned(event.target.checked)} className="h-4 w-4 accent-amber-600" />
              <Pin className="h-4 w-4 text-saffron" /> Pin notice
            </label>
          </div>

          <div className="flex justify-end border-t border-brand-border pt-4">
            <button type="submit" disabled={saving} className="inline-flex items-center gap-2 bg-saffron px-5 py-2.5 text-sm font-semibold text-white hover:bg-saffron-dark disabled:cursor-wait disabled:opacity-60">
              {saving && <LoaderCircle className="h-4 w-4 animate-spin" />}
              {saving ? 'àªªà«àª°àª•àª¾àª¶àª¿àª¤ àª¥àªˆ àª°àª¹à«àª¯à«àª‚ àª›à«‡â€¦' : 'àªªà«àª°àª•àª¾àª¶àª¿àª¤ àª•àª°à«‹'}
            </button>
          </div>
        </form>
      )}

      <section data-print-area className="border-y border-brand-border">
        <div className="no-print flex items-center justify-between border-b border-brand-border py-3">
          <h2 className="font-semibold text-brand-text">àªªà«àª°àª•àª¾àª¶àª¿àª¤ àª…àª¨à«‡ àª…àª—àª¾àª‰àª¨à«€ àª¸à«‚àªšàª¨àª¾àª“</h2>
          <span className="text-xs text-brand-muted">{notices.length} total</span>
        </div>
        {loading ? <p className="py-8 text-center text-sm text-brand-muted">Loading noticesâ€¦</p> : notices.length === 0 ? (
          <p className="py-8 text-center text-sm text-brand-muted">àª¹àªœà« àª•à«‹àªˆ àª¸à«‚àªšàª¨àª¾ àª¨àª¥à«€.</p>
        ) : (
          <ul className="divide-y divide-brand-border">
            {notices.map((notice) => (
              <li key={notice.id} className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <p className="flex items-center gap-2 font-semibold text-brand-text">
                    {notice.is_pinned && <Pin className="h-3.5 w-3.5 shrink-0 text-saffron" />}
                    <span data-print-lang="gu" className="break-words">{notice.title_gu}</span>
                  </p>
                  <p className="mt-1 text-xs text-brand-muted"><span data-print-lang="en">{notice.title_en} · </span>{new Date(notice.created_at).toLocaleDateString()}</p>
                  <p data-print-lang="gu" className="mt-1 line-clamp-2 whitespace-pre-wrap text-sm text-brand-muted">{formattedPreview(notice.description_gu ?? '')}</p>
                  <div className="mt-2 flex gap-3 text-xs text-leaf">
                    {notice.image_url && <a href={notice.image_url} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1"><ImagePlus className="h-3.5 w-3.5" /> Image</a>}
                    {notice.attachment_url && <a href={notice.attachment_url} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1">{/\.(mp4|webm|mov)(\?|$)/i.test(notice.attachment_url) ? <Video className="h-3.5 w-3.5" /> : <FileText className="h-3.5 w-3.5" />} Attachment</a>}
                  </div>
                </div>
                <div className="flex self-end sm:self-center">
                <button type="button" onClick={() => { setPreviewZoom(1); setPreview(notice); }} title="Preview notice" aria-label={`Preview ${notice.title_en}`} className="p-2 text-brand-muted hover:text-saffron"><Eye className="h-4 w-4" /></button>
                <button type="button" onClick={() => void deleteNotice(notice)} title="Delete notice permanently" aria-label={`Delete ${notice.title_en || notice.title_gu}`} className="p-2 text-brand-muted hover:bg-rose-50 hover:text-rose-700">
                  <Trash2 className="h-4 w-4" />
                </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
      {preview && (
        <div role="dialog" aria-modal="true" aria-label="Notice preview" className="fixed inset-0 z-50 grid place-items-center bg-black/60 p-4" onClick={() => setPreview(null)}>
          <article className="max-h-[90vh] w-full max-w-2xl overflow-auto rounded bg-white p-5" onClick={(event) => event.stopPropagation()}>
            <button type="button" onClick={() => setPreview(null)} className="float-right rounded p-2 hover:bg-cream" aria-label="Close preview"><X className="h-5 w-5" /></button>
            <h2 className="mb-4 pr-10 text-xl font-bold">{preview.title_gu}</h2>
            <p className="whitespace-pre-wrap leading-7">{formattedPreview(preview.description_gu ?? '')}</p>
            {preview.image_url && (
              <>
                <div className="mt-5 max-h-[55vh] overflow-auto rounded bg-black/5" aria-label="Zoomable notice image">
                  <div style={{ width: `${previewZoom * 100}%` }}>
                    <Image src={preview.image_url} alt="Notice attachment" width={1280} height={720} unoptimized className="h-auto w-full object-contain" />
                  </div>
                </div>
                <div className="mt-2 flex items-center gap-2" role="group" aria-label="Image zoom controls">
                  <button type="button" onClick={() => setPreviewZoom((zoom) => Math.max(0.5, zoom / 1.25))} className="rounded border border-brand-border px-3 py-1" aria-label="Zoom out">−</button>
                  <span className="min-w-12 text-center text-sm text-brand-muted">{Math.round(previewZoom * 100)}%</span>
                  <button type="button" onClick={() => setPreviewZoom((zoom) => Math.min(4, zoom * 1.25))} className="rounded border border-brand-border px-3 py-1" aria-label="Zoom in">+</button>
                  <button type="button" onClick={() => setPreviewZoom(1)} className="rounded border border-brand-border px-3 py-1 text-sm">Reset</button>
                </div>
              </>
            )}
            {preview.attachment_url && (/\.(mp4|webm|mov)(\?|$)/i.test(preview.attachment_url)
              ? <video src={preview.attachment_url} controls playsInline className="mt-5 max-h-[55vh] w-full rounded bg-black" />
              : /\.pdf(\?|$)/i.test(preview.attachment_url)
                ? <iframe src={preview.attachment_url} title="Notice document preview" className="mt-5 h-[55vh] w-full rounded border" />
                : <p className="mt-5 text-sm text-brand-muted">Document preview is unavailable for this file type.</p>)}
            {preview.attachment_url && <a href={preview.attachment_url + (preview.attachment_url.includes('?') ? '&download' : '?download')} className="mt-4 inline-flex rounded bg-saffron px-4 py-2 font-semibold text-white">Download attachment</a>}
          </article>
        </div>
      )}
    </div>
  );
}
