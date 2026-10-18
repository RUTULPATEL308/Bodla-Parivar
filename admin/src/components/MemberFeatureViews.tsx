'use client';

import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { supabase } from '@/lib/supabase';
import { AlertCircle, ExternalLink, LoaderCircle, MapPin, Phone } from 'lucide-react';
import { useAdminLanguage } from '@/components/AdminLanguageContext';

export type MemberSection = 'overview' | 'profile' | 'notices' | 'events' | 'complaints' | 'history' | 'settings' | 'navratri' | 'offerings' | 'emergency' | 'gallery' | 'notifications' | 'directory';

type RecordRow = Record<string, any>;

const cardClass = 'rounded-2xl border border-[#e8dfd2] bg-[#fffdf8] p-4 shadow-sm dark:border-slate-700 dark:bg-slate-900';
const titleClass = 'text-base font-semibold text-slate-900 dark:text-white';
const inputClass = 'w-full rounded-xl border border-[#e8dfd2] bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-amber-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white';

function EmptyMessage({ children }: { children: string }) {
  return <div className={`${cardClass} py-10 text-center text-sm text-slate-500`}>{children}</div>;
}

function useMemberRows(section: MemberSection, table: string, select = '*', order = 'created_at', ascending = false) {
  const [rows, setRows] = useState<RecordRow[]>([]);
  const [loadedKey, setLoadedKey] = useState('');
  const [error, setError] = useState('');
  const requestKey = `${section}:${table}:${select}:${order}:${ascending}`;

  useEffect(() => {
    let active = true;
    void (async () => {
      const result = await supabase.from(table).select(select).order(order, { ascending }).limit(60);
      if (!active) return;
      setRows((result.data ?? []) as RecordRow[]);
      setError(result.error?.message ?? '');
      setLoadedKey(requestKey);
    })();
    return () => { active = false; };
  }, [table, select, order, ascending, requestKey]);

  return { rows, loading: loadedKey !== requestKey, error, setRows };
}

function LoadState({ loading, error, children }: { loading: boolean; error: string; children: ReactNode }) {
  if (loading) return <div className={`${cardClass} flex items-center gap-2 text-sm text-slate-500`}><LoaderCircle className="h-4 w-4 animate-spin" /> Loading…</div>;
  if (error) return <div role="alert" className={`${cardClass} flex items-start gap-2 text-sm text-rose-700 dark:text-rose-300`}><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />{error}</div>;
  return <>{children}</>;
}

export default function MemberFeatureViews({ section, fullName }: { section: MemberSection; fullName: string }) {
  if (section === 'navratri') return <NavratriView fullName={fullName} />;
  if (section === 'offerings') return <OfferingsView fullName={fullName} />;
  if (section === 'emergency') return <EmergencyView />;
  if (section === 'gallery') return <GalleryView />;
  if (section === 'notifications') return <NotificationsView />;
  if (section === 'directory') return <DirectoryView />;
  if (section === 'history') return <VillageHistoryView />;
  return null;
}

function NavratriView({ fullName }: { fullName: string }) {
  const { rows: options, loading, error } = useMemberRows('navratri', 'navratri_options', '*', 'display_order', true);
  const [selected, setSelected] = useState<RecordRow | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submitBooking = async (event: FormEvent) => {
    event.preventDefault();
    if (!selected || submitting) return;
    setSubmitting(true);
    setMessage('');
    const { data: { user } } = await supabase.auth.getUser();
    const parsedAmount = values.amount ? Number(values.amount) : null;
    const result = await supabase.from('navratri_bookings').insert({
      option_id: selected.id,
      user_id: user?.id ?? null,
      customer_name: values.name || fullName || '',
      phone: values.phone || null,
      amount: parsedAmount,
      quantity: Math.max(1, Number(values.quantity || 1)),
      notes: values.notes || null,
      form_data: values,
      payment_method: 'CASH',
      payment_status: 'PENDING',
      status: 'PENDING',
    });
    setSubmitting(false);
    if (result.error) setMessage(result.error.message);
    else {
      setMessage('Booking request submitted. Payment is collected in cash.');
      setSelected(null);
      setValues({});
    }
  };

  return <section className="space-y-4"><div><h2 className={titleClass}>Navratri services</h2><p className="mt-1 text-sm text-slate-500">Browse services and submit booking requests. Payments are handled in cash.</p></div>
    <LoadState loading={loading} error={error}>{options.filter((option) => option.is_active).length ? <div className="grid gap-3 sm:grid-cols-2">{options.filter((option) => option.is_active).map((option) => <article key={option.id} className={cardClass}><h3 className={titleClass}>{option.title_en || option.title_gu}</h3><p className="mt-1 text-xs text-slate-500">{option.title_gu}</p><p className="mt-3 text-sm text-slate-600 dark:text-slate-300">{option.description_en || option.description_gu}</p>{option.option_type === 'REPORT' ? option.report_url ? <a className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-blue-700" href={option.report_url} target="_blank" rel="noreferrer">Open report <ExternalLink className="h-4 w-4" /></a> : <p className="mt-4 text-xs text-slate-500">Report will be published here.</p> : <button onClick={() => setSelected(option)} className="mt-4 rounded-xl bg-navy-900 px-4 py-2 text-sm font-semibold text-white">Request booking</button>}</article>)}</div> : <EmptyMessage>No Navratri services are available right now.</EmptyMessage>}</LoadState>
    {message && <p role="status" className="text-sm text-slate-700 dark:text-slate-300">{message}</p>}
    {selected && <form onSubmit={submitBooking} className={`${cardClass} space-y-3`}><div className="flex items-start justify-between gap-3"><div><h3 className={titleClass}>{selected.title_en || selected.title_gu}</h3><p className="text-xs text-slate-500">Cash payment only</p></div><button type="button" onClick={() => setSelected(null)} className="text-sm text-slate-500">Close</button></div><label className="block text-xs font-medium">Name<input required className={`${inputClass} mt-1`} value={values.name ?? fullName} onChange={(e) => setValues({ ...values, name: e.target.value })} /></label>{(selected.form_fields ?? []).map((field: RecordRow) => <label key={field.key} className="block text-xs font-medium">{field.label_en || field.label_gu}{field.required && ' *'}<input required={field.required} type={field.type === 'PHONE' ? 'tel' : field.type === 'AMOUNT' ? 'number' : 'text'} min={field.type === 'AMOUNT' ? 0 : undefined} className={`${inputClass} mt-1`} value={values[field.key] ?? ''} onChange={(e) => setValues({ ...values, [field.key]: e.target.value })} /></label>)}<label className="block text-xs font-medium">Notes<textarea className={`${inputClass} mt-1`} value={values.notes ?? ''} onChange={(e) => setValues({ ...values, notes: e.target.value })} /></label><button disabled={submitting} className="rounded-xl bg-navy-900 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">{submitting ? 'Submitting…' : 'Submit request'}</button></form>}
  </section>;
}

function OfferingsView({ fullName }: { fullName: string }) {
  const { lang } = useAdminLanguage();
  const isGujarati = lang === 'gu';
  const [rows, setRows] = useState<RecordRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [bids, setBids] = useState<Record<string, RecordRow[]>>({});
  const [amounts, setAmounts] = useState<Record<string, string>>({});
  const [winnerNames, setWinnerNames] = useState<Record<string, string>>({});
  const [winnerAccess, setWinnerAccess] = useState<Record<string, boolean>>({});
  const [savingWinnerId, setSavingWinnerId] = useState('');
  const [feedback, setFeedback] = useState('');
  const [submittingId, setSubmittingId] = useState('');
  useEffect(() => {
    let active = true;
    const refreshOfferings = async () => {
      const { data, error: offeringsError } = await supabase.from('offerings').select('*').order('created_at', { ascending: false }).limit(60);
      if (!active) return;
      setLoading(false);
      if (offeringsError) { setError('Offerings could not be refreshed. Please try again shortly.'); setFeedback('Offerings could not be refreshed. Please try again shortly.'); return; }
      setError('');
      setRows((data ?? []) as RecordRow[]);
    };
    void refreshOfferings();
    const onRefresh = () => { void refreshOfferings(); };
    const channel = supabase.channel('member_offerings_feed').on('postgres_changes', { event: '*', schema: 'public', table: 'offerings' }, onRefresh).subscribe();
    const poll = window.setInterval(onRefresh, 5000);
    return () => { active = false; window.clearInterval(poll); void supabase.removeChannel(channel); };
  }, []);
  const visible = rows.filter((row) => ['ACTIVE', 'APPROVED', 'COMPLETED'].includes(row.status));
  const visibleIds = visible.map((row) => row.id);
  const idsKey = visibleIds.join(',');
  const awaitingWinnerIds = visible.filter((row) => row.status === 'COMPLETED' && row.winner_name_status === 'AWAITING_WINNER').map((row) => row.id);
  const winnerCheckKey = awaitingWinnerIds.join(',');

  useEffect(() => {
    let active = true;
    const refreshBids = async () => {
      const offeringIds = idsKey ? idsKey.split(',') : [];
      if (!offeringIds.length) { if (active) setBids({}); return; }
      const { data, error: bidsError } = await supabase.from('offering_bids').select('id,offering_id,bidder_name,amount,status,created_at').in('offering_id', offeringIds).order('created_at', { ascending: false });
      if (!active) return;
      if (bidsError) { setFeedback('Live bid data could not be refreshed. Please try again shortly.'); return; }
      const grouped: Record<string, RecordRow[]> = {};
      (data ?? []).forEach((bid: RecordRow) => { grouped[bid.offering_id] = [...(grouped[bid.offering_id] ?? []), bid]; });
      setBids(grouped);
    };
    void refreshBids();
    const channel = supabase.channel(`member_offering_bids_${idsKey || 'none'}`).on('postgres_changes', { event: '*', schema: 'public', table: 'offering_bids' }, () => { void refreshBids(); }).subscribe();
    const poll = window.setInterval(() => { void refreshBids(); }, 5000);
    return () => { active = false; window.clearInterval(poll); void supabase.removeChannel(channel); };
  }, [idsKey]);

  useEffect(() => {
    let active = true;
    if (!winnerCheckKey) return () => { active = false; };
    void Promise.all(winnerCheckKey.split(',').map(async (offeringId) => {
      const { data, error: winnerError } = await supabase.rpc('is_current_user_offering_winner', { p_offering_id: offeringId });
      return [offeringId, !winnerError && data === true] as const;
    })).then((entries) => { if (active) setWinnerAccess(Object.fromEntries(entries)); });
    return () => { active = false; };
  }, [winnerCheckKey]);

  const refreshBids = async (offeringId?: string) => {
    const offeringIds = offeringId ? [offeringId] : (idsKey ? idsKey.split(',') : []);
    if (!offeringIds.length) return;
    const { data, error: bidsError } = await supabase.from('offering_bids').select('id,offering_id,bidder_name,amount,status,created_at').in('offering_id', offeringIds).order('created_at', { ascending: false });
    if (bidsError) { setFeedback('Live bid data could not be refreshed. Please try again shortly.'); return; }
    setBids((current) => {
      const grouped = { ...current };
      offeringIds.forEach((id) => { grouped[id] = []; });
      (data ?? []).forEach((bid: RecordRow) => { grouped[bid.offering_id] = [...(grouped[bid.offering_id] ?? []), bid]; });
      return grouped;
    });
  };
  const placeBid = async (event: FormEvent<HTMLFormElement>, offering: RecordRow) => {
    event.preventDefault();
    const value = Number(amounts[offering.id]);
    const currentTop = Math.max(Number(offering.amount ?? 0), ...(bids[offering.id] ?? []).filter((bid) => bid.status !== 'REJECTED').map((bid) => Number(bid.amount)));
    const minimum = Math.max(1, Number(offering.amount ?? 0), currentTop > 0 ? currentTop + 1 : 0);
    if (!Number.isFinite(value) || value <= 0 || value < minimum) { setFeedback(`Enter a bid of at least ₹${minimum.toLocaleString('en-IN')}.`); return; }
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) { setFeedback('Sign in before placing a bid.'); return; }
    setSubmittingId(offering.id);
    const result = await supabase.from('offering_bids').insert({ offering_id: offering.id, bidder_name: fullName || 'Community member', amount: value }).select('id').maybeSingle();
    setSubmittingId('');
    if (result.error) {
      const isLowBid = result.error.code === '23514' && /BID_TOO_LOW/i.test(result.error.message);
      setFeedback(isLowBid ? 'Another member placed a higher bid. The live amount is refreshing; enter a higher amount and try again.' : 'Your bid could not be saved. Please try again.');
      const { data: latestOffering } = await supabase.from('offerings').select('*').eq('id', offering.id).maybeSingle();
      if (latestOffering) setRows((current) => current.map((row) => row.id === offering.id ? latestOffering as RecordRow : row));
      await refreshBids(offering.id);
    } else if (!result.data) setFeedback('Your bid was not saved. Refresh and try again.');
    else { setFeedback('Your bid has been submitted.'); setAmounts((current) => ({ ...current, [offering.id]: '' })); await refreshBids(); }
  };

  const submitWinnerName = async (event: FormEvent<HTMLFormElement>, offering: RecordRow) => {
    event.preventDefault();
    const name = (winnerNames[offering.id] || fullName).trim();
    if (name.length < 2 || name.length > 60) { setFeedback('Enter a winner name between 2 and 60 characters.'); return; }
    setSavingWinnerId(offering.id);
    setFeedback('');
    const { error: submitError } = await supabase.rpc('submit_winner_display_name', { p_offering_id: offering.id, p_name: name });
    setSavingWinnerId('');
    if (submitError) { setFeedback(submitError.message || 'Winner name could not be saved. Please try again.'); return; }
    setRows((current) => current.map((row) => row.id === offering.id
      ? { ...row, winner_name_status: 'CONFIRMED', winner_display_name: name }
      : row));
    setWinnerAccess((current) => ({ ...current, [offering.id]: false }));
    setFeedback('Winner name submitted.');
    await refreshOfferingsNow();
  };

  const refreshOfferingsNow = async () => {
    const { data } = await supabase.from('offerings').select('*').order('created_at', { ascending: false }).limit(60);
    if (data) setRows(data as RecordRow[]);
  };

  return <section className="w-full min-w-0 space-y-5">
    <div className="flex flex-wrap items-end justify-between gap-3 border-b border-[#e8dfd2] pb-4"><div><p className="text-xs font-bold uppercase tracking-[0.16em] text-amber-700">Bodla Parivar</p><h2 className="mt-1 text-xl font-bold text-[#153b68]">{isGujarati ? 'સમુદાયના ચઢાવા · લાઈવ બોલી' : 'Community offerings · Live bidding'}</h2><p className="mt-1 text-sm text-slate-600">ચઢાવો · હાલની ઓફરો અને બોલીઓ</p></div><span className="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-bold text-emerald-800"><span className="h-2 w-2 animate-pulse rounded-full bg-emerald-500" />{isGujarati ? 'લાઈવ · આપમેળે અપડેટ' : 'LIVE · refreshes automatically'}</span></div>
    {feedback && <p role="status" className="rounded-xl border border-sky-200 bg-sky-50 px-4 py-3 text-sm text-sky-900">{feedback}</p>}
    <LoadState loading={loading} error={error}>{visible.length ? <div className="grid w-full min-w-0 grid-cols-1 gap-4 lg:grid-cols-2 2xl:grid-cols-3">{visible.map((offering) => {
      const eligibleBids = (bids[offering.id] ?? []).filter((bid) => bid.status !== 'REJECTED');
      const topBid = Math.max(Number(offering.amount ?? 0), ...eligibleBids.map((bid) => Number(bid.amount)));
      const minimum = Math.max(1, Number(offering.amount ?? 0), topBid > 0 ? topBid + 1 : 0);
      const canBid = offering.status === 'ACTIVE' && !offering.bid_closed_at && Number(offering.bid_call_count ?? 0) < 3;
      const isWinner = winnerAccess[offering.id] === true;
      return <article key={offering.id} className="flex min-w-0 flex-col overflow-hidden rounded-2xl border border-[#d7e0ec] bg-white shadow-sm">
        {offering.image_url && <img src={offering.image_url} alt="" className="h-48 w-full object-cover" />}
        <div className="flex flex-1 flex-col p-5"><div className="flex flex-wrap items-center justify-between gap-2"><span className="text-xs font-semibold uppercase tracking-wide text-slate-500">Offering · ચઢાવો</span><span className={`rounded-full px-3 py-1 text-[11px] font-bold ${canBid ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'}`}>{canBid ? 'ACTIVE' : offering.status}</span></div>
          <h3 className="mt-3 text-lg font-bold text-[#153b68]">{offering.title_gu || offering.title_en}</h3><p className="text-sm font-medium text-slate-600">{offering.title_en}</p>
          {(offering.description_gu || offering.description_en) && <p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-600">{offering.description_gu || offering.description_en}</p>}
          <div className="mt-5 grid grid-cols-2 gap-3 border-y border-slate-100 py-4"><div><p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Top amount · ઊંચી રકમ</p><p className="mt-1 text-xl font-bold text-[#153b68]">₹{topBid.toLocaleString('en-IN')}</p></div><div><p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Bids · બોલીઓ</p><p className="mt-1 text-xl font-bold text-[#153b68]">{eligibleBids.length}</p></div></div>
          {canBid ? <form onSubmit={(e) => void placeBid(e, offering)} className="mt-4 flex flex-wrap items-end gap-2"><label className="min-w-0 flex-1 text-xs font-semibold text-slate-600">Minimum next bid · ઓછામાં ઓછી ₹{minimum.toLocaleString('en-IN')}<input required type="number" min={minimum} step="1" className={`${inputClass} mt-1`} placeholder={`₹${minimum}`} value={amounts[offering.id] ?? ''} onChange={(e) => setAmounts((current) => ({ ...current, [offering.id]: e.target.value }))} /></label><button disabled={submittingId === offering.id} className="min-h-11 shrink-0 rounded-xl bg-[#153b68] px-5 text-sm font-bold text-white transition hover:bg-[#0e2e54] disabled:cursor-wait disabled:opacity-60">{submittingId === offering.id ? 'Submitting…' : 'Place bid'}</button></form> : <p className="mt-auto pt-4 text-xs font-semibold text-slate-500">Bidding is closed for this offering.</p>}
          {offering.winner_name_status === 'AWAITING_WINNER' && isWinner && <form onSubmit={(e) => void submitWinnerName(e, offering)} className="mt-4 space-y-2 rounded-xl border border-amber-200 bg-amber-50 p-4"><div><h4 className="font-semibold text-amber-950">{isGujarati ? 'આ ચઢાવામાં તમે વિજેતા થયા છો' : 'You won this offering'}</h4><p className="text-xs text-amber-800">{isGujarati ? 'પરિણામ સાથે પ્રકાશિત કરવા નામની પુષ્ટિ કરો.' : 'Confirm the name to publish with the result.'}</p></div><label className="block text-xs font-semibold text-amber-950">{isGujarati ? 'વિજેતાનું નામ' : 'Winner display name'}<input required minLength={2} maxLength={60} value={winnerNames[offering.id] ?? fullName} onChange={(e) => setWinnerNames((current) => ({ ...current, [offering.id]: e.target.value }))} className={`${inputClass} mt-1`} /></label><button disabled={savingWinnerId === offering.id} className="rounded-xl bg-navy-900 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">{savingWinnerId === offering.id ? (isGujarati ? 'સાચવી રહ્યા છીએ…' : 'Saving…') : (isGujarati ? 'વિજેતાના નામની પુષ્ટિ કરો' : 'Confirm winner name')}</button></form>}
          {offering.winner_name_status === 'CONFIRMED' && offering.winner_display_name && <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 p-4"><p className="text-xs font-semibold text-emerald-800">{isGujarati ? 'પુષ્ટિ થયેલ વિજેતા' : 'Confirmed winner'}</p><p className="mt-1 font-bold text-emerald-950">{offering.winner_display_name}</p>{offering.winning_bid_amount != null && <p className="mt-1 text-sm text-emerald-800">₹{Number(offering.winning_bid_amount).toLocaleString('en-IN')}</p>}</div>}
        </div></article>;
    })}</div> : <EmptyMessage>No active offerings right now.</EmptyMessage>}</LoadState>
  </section>;
}
function EmergencyView() {
  const { rows, loading, error } = useMemberRows('emergency', 'emergency_contacts', '*', 'display_order', true);
  const contacts = rows.length ? rows.filter((row) => row.is_active) : [
    { id: '108', name_en: 'Emergency Ambulance', organization: '108 GVK EMRI', phone: '108', category: 'AMBULANCE' },
    { id: '112', name_en: 'Police Emergency', organization: 'Gujarat Police', phone: '112', alternate_phone: '100', category: 'POLICE' },
    { id: '101', name_en: 'Fire Services', organization: 'Fire Services Mehsana', phone: '101', category: 'FIRE' },
    { id: '181', name_en: 'Women Helpline', organization: 'Abhayam', phone: '181', category: 'HELPLINE' },
    { id: '1098', name_en: 'Child Helpline', organization: 'Childline India', phone: '1098', category: 'HELPLINE' },
  ];
  return <section className="space-y-4"><div><h2 className={titleClass}>Emergency contacts</h2><p className="mt-1 text-sm text-slate-500">Verified local services and helplines.</p></div><LoadState loading={loading} error={error}>{contacts.map((contact) => <article key={contact.id} className={`${cardClass} flex items-center justify-between gap-3`}><div><h3 className="font-semibold text-slate-900 dark:text-white">{contact.name_en || contact.name_gu}</h3><p className="text-xs text-slate-500">{contact.organization || contact.category}</p>{contact.alternate_phone && <p className="mt-1 text-xs text-slate-500">Alternate: {contact.alternate_phone}</p>}</div><a href={`tel:${contact.phone}`} aria-label={`Call ${contact.name_en || contact.name_gu}`} className="flex shrink-0 items-center gap-2 rounded-xl bg-emerald-100 px-3 py-2 text-sm font-bold text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"><Phone className="h-4 w-4" />{contact.phone}</a></article>)}</LoadState></section>;
}

function GalleryView() {
  const { rows, loading, error } = useMemberRows('gallery', 'gallery_albums', '*,media:gallery_media(id,file_url,thumbnail_url,caption_en,caption_gu,media_type)', 'created_at');
  const albums = rows.filter((row) => row.status === 'PUBLISHED');
  return <section className="space-y-4"><div><h2 className={titleClass}>Community gallery</h2><p className="mt-1 text-sm text-slate-500">Photos and videos from Bodla Parivar.</p></div><LoadState loading={loading} error={error}>{albums.length ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{albums.map((album) => <article key={album.id} className={`${cardClass} overflow-hidden p-0`}>{album.cover_image_url && <img src={album.cover_image_url} alt="" className="h-44 w-full object-cover" />}<div className="p-4"><h3 className={titleClass}>{album.title_en || album.title_gu}</h3><p className="mt-1 text-xs text-slate-500">{album.title_gu}</p><p className="mt-2 text-sm text-slate-600 dark:text-slate-300">{album.description_en || album.description_gu}</p><div className="mt-3 grid grid-cols-2 gap-2">{(album.media ?? []).slice(0, 4).map((media: RecordRow) => <a key={media.id} href={media.file_url} target="_blank" rel="noreferrer"><img src={media.thumbnail_url || media.file_url} alt={media.caption_en || media.caption_gu || 'Community gallery'} className="h-24 w-full rounded-lg object-cover" /></a>)}</div></div></article>)}</div> : <EmptyMessage>No gallery albums have been published yet.</EmptyMessage>}</LoadState></section>;
}

function NotificationsView() {
  const { rows, loading, error } = useMemberRows('notifications', 'notifications', '*', 'created_at');
  return <section className="space-y-4"><div><h2 className={titleClass}>Notifications</h2><p className="mt-1 text-sm text-slate-500">Recent updates from Bodla Parivar.</p></div><LoadState loading={loading} error={error}>{rows.length ? rows.map((row) => <article key={row.id} className={cardClass}><div className="flex flex-wrap items-center justify-between gap-2"><h3 className={titleClass}>{row.title_en || row.title_gu}</h3><time className="text-xs text-slate-500">{row.created_at ? new Date(row.created_at).toLocaleDateString() : ''}</time></div><p className="mt-1 text-xs text-slate-500">{row.title_gu}</p><p className="mt-3 text-sm text-slate-600 dark:text-slate-300">{row.body_en || row.body_gu}</p></article>) : <EmptyMessage>You’re all caught up. New community updates will appear here.</EmptyMessage>}</LoadState></section>;
}

function DirectoryView() {
  const { rows: businesses, loading, error } = useMemberRows('directory', 'businesses', '*,category:business_categories(name_en,name_gu)', 'created_at');
  const [places, setPlaces] = useState<RecordRow[]>([]);
  const [jobs, setJobs] = useState<RecordRow[]>([]);
  useEffect(() => {
    let active = true;
    void Promise.all([supabase.from('places').select('*').eq('status', 'PUBLISHED').limit(40), supabase.from('jobs').select('*').eq('status', 'PUBLISHED').limit(40)]).then(([placeResult, jobResult]) => {
      if (active) { setPlaces((placeResult.data ?? []) as RecordRow[]); setJobs((jobResult.data ?? []) as RecordRow[]); }
    });
    return () => { active = false; };
  }, []);
  const visibleBusinesses = businesses.filter((business) => business.status === 'APPROVED' || business.is_verified);
  return <section className="space-y-5"><div><h2 className={titleClass}>Community directory</h2><p className="mt-1 text-sm text-slate-500">Businesses, local opportunities, and useful places.</p></div><LoadState loading={loading} error={error}><div><h3 className="mb-2 font-semibold">Businesses</h3>{visibleBusinesses.length ? <div className="grid gap-3 sm:grid-cols-2">{visibleBusinesses.map((item) => <article key={item.id} className={cardClass}><h4 className="font-semibold">{item.name_en || item.name_gu}</h4><p className="mt-1 text-xs text-slate-500">{item.category?.name_en || item.category?.name_gu}</p><p className="mt-2 text-sm">{item.description_en || item.description_gu}</p>{item.address_en && <p className="mt-2 flex items-center gap-1 text-xs text-slate-500"><MapPin className="h-3.5 w-3.5" />{item.address_en}</p>}{item.phone && <a href={`tel:${item.phone}`} className="mt-3 inline-flex items-center gap-1 text-sm font-semibold text-blue-700"><Phone className="h-4 w-4" />{item.phone}</a>}</article>)}</div> : <EmptyMessage>No businesses are listed yet.</EmptyMessage>}</div><div><h3 className="mb-2 font-semibold">Jobs and opportunities</h3>{jobs.length ? <div className="space-y-2">{jobs.map((item) => <article key={item.id} className={cardClass}><h4 className="font-semibold">{item.title_en || item.title_gu}</h4><p className="mt-1 text-sm">{item.company_name || item.location_en}</p>{item.contact_phone && <a href={`tel:${item.contact_phone}`} className="mt-2 block text-sm text-blue-700">{item.contact_phone}</a>}</article>)}</div> : <EmptyMessage>No current job listings.</EmptyMessage>}</div><div><h3 className="mb-2 font-semibold">Places</h3>{places.length ? <div className="grid gap-3 sm:grid-cols-2">{places.map((item) => <article key={item.id} className={cardClass}><h4 className="font-semibold">{item.name_en || item.name_gu}</h4><p className="mt-1 text-sm text-slate-500">{item.description_en || item.address_en}</p>{item.phone && <a href={`tel:${item.phone}`} className="mt-2 block text-sm text-blue-700">{item.phone}</a>}</article>)}</div> : <EmptyMessage>No places are listed yet.</EmptyMessage>}</div></LoadState></section>;
}

const historySections = [
  ['Families and roots', 'The history of Bodla is woven together with the memories and roots of the families who made their home here. Across generations, families’ work, traditions and connection to the village have helped shape Bodla’s common identity.'],
  ['Places of worship', 'Bodla’s temples and sacred places express both religious faith and the shared life of the village. Through worship, traditions and community occasions, these places have brought residents together.'],
  ['Education and public life', 'The village primary school and expanding access to education are an important part of Bodla’s social change. The booklet also records the role of the Gram Panchayat and efforts in public life.'],
  ['Village development', 'Agriculture has long been an important foundation of life and livelihood for Bodla’s families. Public facilities, community organizations and youth activities have responded to the village’s changing needs.'],
];

function VillageHistoryView() {
  return <section className="space-y-4"><div><h2 className={titleClass}>Bodla village history</h2><p className="mt-1 text-sm text-slate-500">Memories, roots and a story shared across generations.</p></div><p className={`${cardClass} text-sm leading-7 text-slate-600 dark:text-slate-300`}>This overview connects Bodla’s story with its families and shared village life. Refer to the original Gujarati booklet for names, dates and figures.</p>{historySections.map(([title, text]) => <article key={title} className={cardClass}><h3 className={titleClass}>{title}</h3><p className="mt-2 text-sm leading-7 text-slate-600 dark:text-slate-300">{text}</p></article>)}</section>;
}


