'use client';

import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { supabase } from '@/lib/supabase';
import { AlertCircle, ExternalLink, LoaderCircle, MapPin, Phone, Trash2 } from 'lucide-react';
import { useAdminLanguage } from '@/components/AdminLanguageContext';
import { GujlishInput, GujlishTextarea } from '@/components/GujlishInput';
import html2canvas from 'html2canvas';

export type MemberSection = 'overview' | 'profile' | 'notices' | 'events' | 'complaints' | 'history' | 'settings' | 'navratri' | 'offerings' | 'emergency' | 'gallery' | 'notifications' | 'directory' | 'result_uploads' | 'notice_design' | 'besanu_design';

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
  if (section === 'result_uploads') return <ResultUploadsView />;
  if (section === 'notice_design') return <MobileParityFuneralDesignView kind="antim" />;
  if (section === 'besanu_design') return <MobileParityFuneralDesignView kind="besanu" />;
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
          <div className="mt-5 grid grid-cols-2 gap-3 border-y border-slate-100 py-4"><div><p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Top amount · ઊંચી રકમ</p><p className="mt-1 text-xl font-bold text-[#153b68]">₹{topBid.toLocaleString('en-IN')}</p>{canBid && <p className="mt-1 text-xs font-bold text-amber-700">{isGujarati ? `બોલી વાર ${Number(offering.bid_call_count ?? 0)}/3` : `Bid call ${Number(offering.bid_call_count ?? 0)}/3`}</p>}</div><div><p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Bids · બોલીઓ</p><p className="mt-1 text-xl font-bold text-[#153b68]">{eligibleBids.length}</p></div></div>
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
  const { rows, loading, error, setRows } = useMemberRows('notifications', 'notifications', '*', 'created_at');
  const [canBroadcast, setCanBroadcast] = useState(true);
  const [checkingPermission, setCheckingPermission] = useState(true);
  const [showCompose, setShowCompose] = useState(false);
  const [currentProfileId, setCurrentProfileId] = useState<string | null>(null);

  // Form states
  const [bodyGu, setBodyGu] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [attachmentUrl, setAttachmentUrl] = useState('');
  const [attachmentName, setAttachmentName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [improvingBody, setImprovingBody] = useState(false);
  const [feedback, setFeedback] = useState<{ success: boolean; message: string } | null>(null);

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const { data: { user } } = await supabase.auth.getUser();
        if (!user) {
          if (active) setCheckingPermission(false);
          return;
        }
        const { data: profile } = await supabase
          .from('profiles')
          .select('id')
          .eq('auth_user_id', user.id)
          .eq('status', 'ACTIVE')
          .maybeSingle();
        if (active && profile?.id) setCurrentProfileId(profile.id);
        if (!active) return;
        setCanBroadcast(true);
      } catch {
        // ignore
      } finally {
        if (active) setCheckingPermission(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const handleEnhance = async (field: 'title' | 'body') => {
    const text = bodyGu;
    if (!text.trim()) {
      setFeedback({ success: false, message: 'લખાણ સુધારવા માટે પહેલા લખાણ દાખલ કરો.' });
      return;
    }
    setImprovingBody(true);

    try {
      const res = await fetch('/api/notifications/enhance', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: text.trim(), type: field }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'AI enhancement failed');
      if (data.enhanced_gu) setBodyGu(data.enhanced_gu);
      setFeedback({ success: true, message: 'AI દ્વારા લખાણ સુધારી દેવાયું!' });
    } catch (err: any) {
      setFeedback({ success: false, message: err.message || 'AI Enhancement failed' });
    } finally {
      setImprovingBody(false);
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 25 * 1024 * 1024) {
      setFeedback({ success: false, message: 'ફાઇલનું માપ 25 MB કરતાં ઓછું હોવું જોઈએ.' });
      return;
    }
    setUploading(true);
    try {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) throw new Error('સત્ર સમાપ્ત થયું. ફરી લૉગિન કરો.');
      const { data: sessionData } = await supabase.auth.getSession();
      const token = sessionData.session?.access_token;
      if (!token) throw new Error('સત્ર સમાપ્ત થયું. ફરી લૉગિન કરો.');

      const formData = new FormData();
      formData.append('file', file);
      const uploadResponse = await fetch('/api/notices/upload', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });
      const uploadData = await uploadResponse.json();
      if (!uploadResponse.ok) {
        throw new Error(uploadData.error || 'ફાઇલ અપલોડ થઈ નથી.');
      }
      if (uploadData.isImage) {
        setImageUrl(uploadData.publicUrl);
        setAttachmentUrl('');
      } else {
        setAttachmentUrl(uploadData.publicUrl);
        setImageUrl('');
      }
      setAttachmentName(file.name);
      setFeedback({ success: true, message: `ફાઇલ "${file.name}" અપલોડ થઈ ગઈ!` });
    } catch (err: any) {
      setFeedback({ success: false, message: err.message || 'File upload failed' });
    } finally {
      setUploading(false);
    }
  };

  const handleSend = async (e: FormEvent) => {
    e.preventDefault();
    if (!bodyGu.trim()) {
      setFeedback({ success: false, message: 'સંદેશ ગુજરાતીમાં જરૂરી છે.' });
      return;
    }
    setSubmitting(true);
    setFeedback(null);
    try {
      const { data: sessionData } = await supabase.auth.getSession();
      const token = sessionData.session?.access_token;
      if (!token) throw new Error('સત્ર સમાપ્ત થયું. ફરી લૉગિન કરો.');

      let generatedTitle = bodyGu.trim().replace(/\s+/g, ' ').slice(0, 80);
      try {
        const titleResponse = await fetch('/api/notifications/enhance', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ text: bodyGu.trim(), type: 'title' }),
        });
        const titleData = await titleResponse.json();
        if (titleResponse.ok && titleData.enhanced_gu?.trim()) {
          generatedTitle = titleData.enhanced_gu.trim();
        }
      } catch {
        // Use a concise title derived from the message when AI is unavailable.
      }

      const res = await fetch('/api/notifications/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          title_gu: generatedTitle,
          title_en: '',
          body_gu: bodyGu.trim(),
          body_en: '',
          image_url: imageUrl.trim() || undefined,
          attachment_url: attachmentUrl.trim() || undefined,
          type: 'NOTICE',
          target: 'topic',
        }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'સૂચના મોકલવામાં નિષ્ફળ.');
      setFeedback({ success: true, message: 'સૂચના સફળતાપૂર્વક મોકલાઈ ગઈ છે!' });
      setBodyGu('');
      setImageUrl('');
      setAttachmentUrl('');
      setAttachmentName('');
      setShowCompose(false);
      // Reload notifications
      const refreshed = await supabase.from('notifications').select('*').order('created_at', { ascending: false }).limit(60);
      if (refreshed.data) setRows(refreshed.data as RecordRow[]);
    } catch (err: any) {
      setFeedback({ success: false, message: err.message || 'Error sending notification' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteNotification = async (notificationId: string) => {
    if (!window.confirm('આ સૂચન કાઢી નાખવી છે?')) return;
    setFeedback(null);
    try {
      const { data: sessionData } = await supabase.auth.getSession();
      const token = sessionData.session?.access_token;
      if (!token) throw new Error('સત્ર સમાપ્ત થયું. ફરી લૉગિન કરો.');
      const res = await fetch(`/api/notifications/${notificationId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'સૂચન કાઢી શકાયું નથી.');
      setRows((currentRows) => currentRows.filter((row) => row.id !== notificationId));
      setFeedback({ success: true, message: 'સૂચન સફળતાપૂર્વક કાઢી નાખ્યું.' });
    } catch (err: any) {
      setFeedback({ success: false, message: err.message || 'સૂચન કાઢી શકાયું નથી.' });
    }
  };

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#e8dfd2] pb-4">
        <div>
          <h2 className={titleClass}>સૂચનાઓ</h2>
          <p className="mt-1 text-sm text-slate-500">બોદલા પરિવારના તાજેતરના અપડેટ્સ અને સીધી સૂચનાઓ.</p>
        </div>
        {canBroadcast && (
          <button
            onClick={() => setShowCompose(!showCompose)}
            className="flex items-center gap-2 rounded-xl bg-amber-600 px-4 py-2 text-xs font-bold text-white shadow-sm hover:bg-amber-700 transition"
          >
            <span>{showCompose ? 'સૂચનાઓની યાદી જુઓ' : '+ નવી સૂચના મોકલો'}</span>
          </button>
        )}
      </div>

      {feedback && (
        <div className={`p-3 rounded-xl border text-xs font-semibold ${feedback.success ? 'bg-emerald-50 border-emerald-200 text-emerald-800' : 'bg-rose-50 border-rose-200 text-rose-800'}`}>
          {feedback.message}
        </div>
      )}

      {showCompose && canBroadcast ? (
        <div className={`${cardClass} p-5 space-y-4`}>
          <div className="border-b pb-3">
            <h3 className="text-sm font-bold text-slate-900 dark:text-white">તમામ સભ્યોને સીધી સૂચના મોકલો</h3>
            <p className="text-xs text-slate-500 mt-0.5">Gujlish ટાઇપિંગ, દસ્તાવેજ/ફોટો અપલોડ અને AI સુધારા સાથે સૂચના મોકલો.</p>
          </div>
          <form onSubmit={handleSend} className="space-y-4">
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="text-xs font-semibold text-slate-700 dark:text-slate-200">સંદેશ (Gujarati Message Body) *</label>
                <button
                  type="button"
                  onClick={() => void handleEnhance('body')}
                  disabled={improvingBody || !bodyGu.trim()}
                  className="text-xs px-2 py-0.5 rounded bg-amber-50 text-amber-900 border border-amber-200 hover:bg-amber-100 disabled:opacity-50"
                >
                  {improvingBody ? 'AI સુધારે છે...' : '✨ AI સંદેશ સુધારો'}
                </button>
              </div>
              <GujlishTextarea
                required
                rows={3}
                value={bodyGu}
                onValueChange={setBodyGu}
                placeholder="સંદેશ લખો (દા.ત. તમામ ગ્રામજનોને કાલે સવારે હાજર રહેવા વિનંતી...)"
                className={inputClass}
              />
            </div>

            {/* Document/Photo Upload */}
            <div className="p-3 rounded-xl border border-dashed border-amber-300 bg-amber-50/50">
              <label className="text-xs font-bold text-amber-900 block mb-1.5">ફોટો અથવા દસ્તાવેજ અપલોડ (Attach Photo/Document)</label>
              <div className="flex flex-wrap items-center gap-3">
                <input
                  type="file"
                  accept="image/*,application/pdf"
                  onChange={handleFileUpload}
                  className="text-xs text-slate-600 file:mr-2 file:py-1 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-amber-600 file:text-white hover:file:bg-amber-700 cursor-pointer"
                />
                {uploading && <span className="text-xs text-amber-700 animate-pulse font-medium">અપલોડ થઈ રહ્યું છે...</span>}
                {imageUrl && (
                  <span className="text-xs font-semibold text-emerald-700">✓ {attachmentName || 'Attachment Uploaded'}</span>
                )}
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowCompose(false)}
                className="px-4 py-2 rounded-xl border border-slate-300 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                રદ કરો
              </button>
              <button
                type="submit"
                disabled={submitting || uploading}
                className="px-5 py-2 rounded-xl bg-amber-600 text-xs font-bold text-white hover:bg-amber-700 transition disabled:opacity-50"
              >
                {submitting ? 'મોકલી રહ્યું છે…' : 'સૂચના મોકલો'}
              </button>
            </div>
          </form>
        </div>
      ) : (
        <LoadState loading={loading} error={error}>
          {rows.length ? (
            rows.map((row) => (
              <article key={row.id} className={cardClass}>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className={titleClass}>{row.title_en || row.title_gu}</h3>
                  <div className="flex items-center gap-2">
                    <time className="text-xs text-slate-500">{row.created_at ? new Date(row.created_at).toLocaleDateString() : ''}</time>
                    {currentProfileId && row.created_by === currentProfileId && (
                      <button
                        type="button"
                        onClick={() => void handleDeleteNotification(row.id)}
                        className="inline-flex items-center gap-1 rounded-lg border border-rose-200 px-2 py-1 text-xs font-semibold text-rose-700 hover:bg-rose-50 dark:border-rose-900 dark:text-rose-300"
                        aria-label="સૂચન કાઢી નાખો"
                      >
                        <Trash2 size={14} />
                        કાઢો
                      </button>
                    )}
                  </div>
                </div>
                {row.title_gu && row.title_en && row.title_en !== row.title_gu && (
                  <p className="mt-1 text-xs text-slate-500">{row.title_gu}</p>
                )}
                <p className="mt-3 text-sm text-slate-600 dark:text-slate-300 whitespace-pre-wrap">{row.body_en || row.body_gu}</p>
                {(row.image_url || row.payload?.image_url) && (
                  <div className="mt-3">
                    <a
                      href={row.image_url || row.payload.image_url}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-sky-700 hover:bg-slate-50 dark:border-slate-700 dark:text-sky-400"
                    >
                      <span>📎 ફોટો / દસ્તાવેજ જુઓ (View Attachment)</span>
                    </a>
                  </div>
                )}
              </article>
            ))
          ) : (
            <EmptyMessage>You&apos;re all caught up. New community updates will appear here.</EmptyMessage>
          )}
        </LoadState>
      )}
    </section>
  );
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

function ResultUploadsView() {
  const [enabled, setEnabled] = useState<boolean | null>(null);
  const [studentName, setStudentName] = useState('');
  const [standard, setStandard] = useState('');
  const [schoolName, setSchoolName] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let active = true;
    void supabase.from('village_settings').select('result_upload_enabled').order('created_at', { ascending: true }).limit(1).maybeSingle().then(({ data }) => {
      if (active) setEnabled(data?.result_upload_enabled === true);
    });
    return () => { active = false; };
  }, []);

  const submitResult = async (event: FormEvent) => {
    event.preventDefault();
    if (!file || !enabled || saving) return;
    if (file.size > 10 * 1024 * 1024) { setMessage('Please choose a file smaller than 10 MB.'); return; }
    if (!['image/jpeg', 'image/png', 'image/webp', 'application/pdf'].includes(file.type)) { setMessage('Please upload a JPG, PNG, WebP, or PDF file.'); return; }
    setSaving(true);
    setMessage('');
    const { data: auth } = await supabase.auth.getUser();
    const user = auth.user;
    if (!user) { setMessage('Please sign in again before submitting.'); setSaving(false); return; }
    const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
    const filePath = `${user.id}/${Date.now()}_${safeName}`;
    const upload = await supabase.storage.from('result-uploads').upload(filePath, file, { contentType: file.type });
    if (upload.error) { setMessage(upload.error.message); setSaving(false); return; }
    const insert = await supabase.from('result_uploads').insert({
      user_id: user.id,
      student_name: studentName.trim(),
      standard: standard.trim(),
      school_name: schoolName.trim(),
      file_path: filePath,
      file_name: file.name,
      content_type: file.type,
    });
    if (insert.error) {
      await supabase.storage.from('result-uploads').remove([filePath]);
      setMessage(insert.error.message);
    } else {
      setStudentName('');
      setStandard('');
      setSchoolName('');
      setFile(null);
      setMessage('Result submitted successfully.');
    }
    setSaving(false);
  };

  return <section className="space-y-4">
    <div><h2 className={titleClass}>Result Upload</h2><p className="mt-1 text-sm text-slate-500">Submit a student result document securely.</p></div>
    {enabled === null ? <div className={cardClass}>Loading result upload settings…</div> : !enabled ? <EmptyMessage>Result upload is currently closed.</EmptyMessage> : (
      <form className={`${cardClass} space-y-3`} onSubmit={submitResult}>
        <label className="block text-xs font-semibold text-slate-600">Student name<input required value={studentName} onChange={(event) => setStudentName(event.target.value)} className={`${inputClass} mt-1`} /></label>
        <label className="block text-xs font-semibold text-slate-600">Standard / Class<input required value={standard} onChange={(event) => setStandard(event.target.value)} className={`${inputClass} mt-1`} /></label>
        <label className="block text-xs font-semibold text-slate-600">School name<input required value={schoolName} onChange={(event) => setSchoolName(event.target.value)} className={`${inputClass} mt-1`} /></label>
        <label className="block text-xs font-semibold text-slate-600">Result file<input required type="file" accept=".jpg,.jpeg,.png,.webp,.pdf,image/jpeg,image/png,image/webp,application/pdf" onChange={(event) => setFile(event.target.files?.[0] ?? null)} className={`${inputClass} mt-1`} /></label>
        {message && <p role="status" className="text-sm">{message}</p>}
        <button disabled={saving} type="submit" className="min-h-11 rounded-xl bg-saffron px-4 text-sm font-bold text-white disabled:opacity-60">{saving ? 'Submitting…' : 'Submit Result'}</button>
      </form>
    )}
  </section>;
}

export function MobileParityFuneralDesignView({ kind }: { kind: 'antim' | 'besanu' }) {
  const isBesanu = kind === 'besanu';
  const posterRef = useRef<HTMLElement>(null);
  const [name, setName] = useState('');
  const [deathDate, setDeathDate] = useState('');
  const [weekday, setWeekday] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [address, setAddress] = useState('');
  const [cremationGround, setCremationGround] = useState('');
  const [familyMembers, setFamilyMembers] = useState(['', '', '', '', '']);
  const [mobileNumber, setMobileNumber] = useState('');
  const [photoUrl, setPhotoUrl] = useState('');
  const [photoName, setPhotoName] = useState('');
  const [publishing, setPublishing] = useState(false);
  const [message, setMessage] = useState('');

  const formatDate = (value: string) => {
    if (!value) return '';
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  };

  const formatTime = (value: string) => {
    if (!value) return '';
    const [hourText, minute = '00'] = value.split(':');
    const hour = Number(hourText);
    if (!Number.isFinite(hour)) return value;
    const hour12 = hour % 12 || 12;
    return `${hour12.toString().padStart(2, '0')}:${minute} ${hour < 12 ? 'AM' : 'PM'}`;
  };

  const selectDeathDate = (value: string) => {
    setDeathDate(value);
    if (!value) {
      setWeekday('');
      return;
    }
    const date = new Date(`${value}T12:00:00`);
    const names = ['રવિવાર', 'સોમવાર', 'મંગળવાર', 'બુધવાર', 'ગુરુવાર', 'શુક્રવાર', 'શનિવાર'];
    setWeekday(names[date.getDay()] || '');
  };

  const uploadPhoto = async (file?: File) => {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      setMessage('કૃપા કરીને ફોટો ફાઇલ પસંદ કરો.');
      return;
    }
    setPublishing(true);
    setMessage('');
    try {
      const { data: sessionData } = await supabase.auth.getSession();
      const token = sessionData.session?.access_token;
      if (!token) throw new Error('સત્ર સમાપ્ત થયું. ફરી લૉગિન કરો.');

      const formData = new FormData();
      formData.append('file', file);
      const response = await fetch('/api/notices/upload', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || 'ફોટો અપલોડ થઈ શક્યો નથી.');
      setPhotoUrl(data.publicUrl);
      setPhotoName(file.name);
    } catch (error: any) {
      setMessage(error.message || 'ફોટો અપલોડ થઈ શક્યો નથી.');
    } finally {
      setPublishing(false);
    }
  };

  const publish = async () => {
    if (!name.trim() || !deathDate || !eventDate || !address.trim()) {
      setMessage(isBesanu
        ? 'નામ, અવસાનની તારીખ, બેસણું તારીખ અને સ્થાન જરૂરી છે.'
        : 'નામ, અવસાનની તારીખ, અંતિમ યાત્રા તારીખ અને સરનામું જરૂરી છે.');
      return;
    }
    setPublishing(true);
    setMessage('');
    try {
      const title = isBesanu ? `${name.trim()} - બેસણું સમાચાર` : `${name.trim()} - અંતિમ યાત્રા સમાચાર`;
      const { data: sessionData } = await supabase.auth.getSession();
      const token = sessionData.session?.access_token;
      if (!token) throw new Error('સત્ર સમાપ્ત થયું. ફરી લૉગિન કરો.');

      if (!posterRef.current) throw new Error('પોસ્ટર તૈયાર થઈ શક્યું નથી. ફરી પ્રયત્ન કરો.');
      const posterCanvas = await html2canvas(posterRef.current, {
        scale: 2,
        useCORS: true,
        backgroundColor: '#fffdf7',
        logging: false,
      });
      const posterBlob = await new Promise<Blob>((resolve, reject) => {
        posterCanvas.toBlob(
          (blob) => blob ? resolve(blob) : reject(new Error('પોસ્ટર ઈમેજ બનાવી શકાઈ નથી.')),
          'image/png',
          1
        );
      });
      const posterFile = new File(
        [posterBlob],
        `${isBesanu ? 'besanu' : 'antim-yatra'}-${Date.now()}.png`,
        { type: 'image/png' }
      );
      const posterFormData = new FormData();
      posterFormData.append('file', posterFile);
      const posterResponse = await fetch('/api/notices/upload', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: posterFormData,
      });
      const posterResult = await posterResponse.json();
      if (!posterResponse.ok) throw new Error(posterResult.error || 'પોસ્ટર અપલોડ થઈ શક્યું નથી.');

      const response = await fetch('/api/notifications/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          target: 'topic',
          type: 'NOTICE',
          title_gu: title,
          title_en: title,
          notification_title_gu: title,
          notification_title_en: isBesanu ? 'Besanu' : 'Antim Yatra',
          body_gu: title,
          body_en: title,
          image_url: posterResult.publicUrl,
        }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || 'નોટિસ પ્રકાશિત થઈ શકી નથી.');
      setMessage('નોટિસ પ્રકાશિત થઈ ગઈ છે અને મોબાઇલ સૂચન મોકલવામાં આવ્યું છે.');
    } catch (error: any) {
      setMessage(error.message || 'નોટિસ પ્રકાશિત થઈ શકી નથી.');
    } finally {
      setPublishing(false);
    }
  };

  const posterBackground = isBesanu
    ? 'linear-gradient(135deg, rgba(224,237,232,.92), rgba(196,214,206,.92)), url(/poster-bg.jpg)'
    : 'url(/poster-bg.jpg)';
  const posterColors = isBesanu
    ? {
        title: '#1A3C40',
        secondary: '#2C6E6A',
        gold: '#8B6914',
        lightGold: '#B8860B',
        card: '#F5EDE0',
        ink: '#1C1208',
        muted: '#5A5030',
        photo: '#E0EDE8',
      }
    : {
        title: '#4A1220',
        secondary: '#6E2332',
        gold: '#9C7A38',
        lightGold: '#D4AF37',
        card: '#FBF4E8',
        ink: '#2C1810',
        muted: '#5E453A',
        photo: '#EFE5D0',
      };

  return (
    <section className="space-y-4">
      <div>
        <h2 className={titleClass}>{isBesanu ? 'બેસણું પોસ્ટ' : 'અંતિમ યાત્રા પોસ્ટ'}</h2>
        <p className="mt-1 text-sm text-slate-500">મોબાઇલ એપની જેમ સમગ્ર ગુજરાતી માહિતી સાથે પોસ્ટ બનાવો અને પ્રકાશિત કરો.</p>
      </div>
      <div className="grid gap-4 lg:grid-cols-[minmax(0,380px)_minmax(320px,1fr)]">
        <div className={`${cardClass} space-y-3`}>
          <GujlishInput
            value={name}
            onValueChange={setName}
            label="અહીં નામ લખો *"
            required
            className={inputClass}
          />
          <label className="block text-xs font-semibold text-slate-600">અવસાનની તારીખ *<input required type="date" value={deathDate} onChange={(event) => selectDeathDate(event.target.value)} className={`${inputClass} mt-1`} /></label>
          <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-900">વાર: {weekday || 'તારીખ પસંદ કર્યા પછી આપમેળે આવશે'}</div>
          <label className="block text-xs font-semibold text-slate-600">{isBesanu ? 'બેસણુંની તારીખ *' : 'અંતિમ યાત્રાની તારીખ *'}<input required type="date" value={eventDate} onChange={(event) => setEventDate(event.target.value)} className={`${inputClass} mt-1`} /></label>
          <div className="grid grid-cols-2 gap-2">
            <label className="text-xs font-semibold text-slate-600">{isBesanu ? 'સમય (શરૂ)' : 'સમય'}<input type="time" value={startTime} onChange={(event) => setStartTime(event.target.value)} className={`${inputClass} mt-1`} /></label>
            {isBesanu && <label className="text-xs font-semibold text-slate-600">સમય (સુધી)<input type="time" value={endTime} onChange={(event) => setEndTime(event.target.value)} className={`${inputClass} mt-1`} /></label>}
          </div>
          <GujlishTextarea
            value={address}
            onValueChange={setAddress}
            label={isBesanu ? 'સ્થળ / સરનામું *' : 'અંતિમ યાત્રાનું સરનામું *'}
            required
            className={inputClass}
            rows={2}
          />
          {!isBesanu && (
            <GujlishInput
              value={cremationGround}
              onValueChange={setCremationGround}
              label="સ્મશાન ઘાટ"
              className={inputClass}
            />
          )}
          <div className="space-y-2">
            <p className="text-xs font-bold text-slate-700">પરિવારના સભ્યોના નામ</p>
            {familyMembers.map((member, index) => (
              <GujlishInput
                key={index}
                value={member}
                onValueChange={(value) => setFamilyMembers((current) => current.map((item, i) => i === index ? value : item))}
                label={index === 0 ? undefined : undefined}
                placeholder={`સભ્ય ${index + 1}`}
                className={inputClass}
              />
            ))}
          </div>
          <label className="block text-xs font-semibold text-slate-600">મોબાઇલ નંબર<input inputMode="numeric" value={mobileNumber} onChange={(event) => setMobileNumber(event.target.value.replace(/\D/g, '').slice(0, 15))} className={`${inputClass} mt-1`} /></label>
          <label className="block text-xs font-semibold text-slate-600">ફોટો અપલોડ કરો<input type="file" accept="image/*" onChange={(event) => void uploadPhoto(event.target.files?.[0])} className={`${inputClass} mt-1`} /></label>
          {photoName && <p className="text-xs font-semibold text-emerald-700">✓ {photoName}</p>}
          {message && <p className="rounded-lg bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-700">{message}</p>}
          <div className="flex gap-2">
            <button type="button" onClick={() => window.print()} className="min-h-11 flex-1 rounded-xl border border-amber-300 text-sm font-bold text-amber-800">Print / PDF</button>
            <button type="button" disabled={publishing} onClick={() => void publish()} className="min-h-11 flex-1 rounded-xl bg-saffron px-4 text-sm font-bold text-white disabled:opacity-60">{publishing ? 'પ્રકાશિત થાય છે...' : 'નોટિસમાં પ્રકાશિત કરો'}</button>
          </div>
        </div>
        <article
          ref={posterRef}
          className="relative mx-auto aspect-[3/4] w-full max-w-[520px] overflow-hidden rounded-[18px] border border-[#d9c79f] bg-cover bg-center text-center shadow-[0_12px_35px_rgba(74,18,32,.16)]"
          style={{ backgroundImage: posterBackground }}
        >
          <div className="pointer-events-none absolute inset-[2.2%] rounded-[12px] border-2" style={{ borderColor: posterColors.gold }} />
          <div className="pointer-events-none absolute inset-[3.4%] rounded-[10px] border" style={{ borderColor: `${posterColors.lightGold}CC` }} />
          {[
            ['top-2.5 left-[3.2%]', 'top'],
            ['top-2.5 right-[3.2%]', 'top'],
            ['bottom-2.5 left-[3.2%]', 'bottom'],
            ['bottom-2.5 right-[3.2%]', 'bottom'],
          ].map(([position]) => (
            <span key={position} className={`pointer-events-none absolute h-2 w-2 rounded-full border border-white/70 ${position}`} style={{ backgroundColor: posterColors.gold }} />
          ))}

          <div className="relative mx-auto flex h-full max-w-[520px] flex-col items-center px-[7.5%] pb-[3%] pt-[2.8%]">
            <p className="text-[17px] font-bold tracking-[2.5px]" style={{ color: posterColors.gold }}>ૐ શાંતિઃ</p>
            <h3 className="mt-1 text-[28px] font-extrabold leading-tight tracking-[1px]" style={{ color: posterColors.title }}>
              {isBesanu ? 'બેસણું' : 'અંતિમ યાત્રા'}
            </h3>
            <div className="my-[6px] flex w-[65%] items-center gap-2" aria-hidden="true">
              <span className="h-px flex-1" style={{ backgroundColor: `${posterColors.gold}B3` }} />
              <span className="text-[11px]" style={{ color: posterColors.gold }}>✦</span>
              <span className="h-px flex-1" style={{ backgroundColor: `${posterColors.gold}B3` }} />
            </div>

            <div
              className="flex h-[138px] w-[138px] items-center justify-center overflow-hidden rounded-full border-[4px] p-[4px] shadow-[0_8px_18px_rgba(44,24,16,.16)] sm:h-[180px] sm:w-[180px]"
              style={{ borderColor: posterColors.gold, backgroundColor: posterColors.photo }}
            >
              <div className="h-full w-full overflow-hidden rounded-full border" style={{ borderColor: `${posterColors.lightGold}CC` }}>
                {photoUrl ? (
                  <img src={photoUrl} alt={name || 'સ્વર્ગસ્થ વ્યક્તિનો ફોટો'} className="h-full w-full object-cover" />
                ) : (
                  <span className="flex h-full w-full items-center justify-center text-[11px] font-bold" style={{ background: 'radial-gradient(circle, #EFE5D0 0%, #D6C3A1 100%)', color: posterColors.muted }}>
                    ફોટો
                  </span>
                )}
              </div>
            </div>

            <h4 className="mt-[12px] min-h-[29px] text-[24px] font-semibold leading-[29px]" style={{ color: posterColors.title }}>
              સ્વ. {name || 'અહીં નામ લખો'}
            </h4>
            <p className="mt-[2px] min-h-[18px] max-w-full text-[12.5px] font-semibold leading-[18px]" style={{ color: posterColors.secondary }}>
              {deathDate || weekday
                ? `અવસાન તા. ${deathDate ? formatDate(deathDate) : '[તારીખ]'}${weekday ? `, વાર : ${weekday}` : ''}`
                : '[તારીખ] • [વાર]'}
            </p>
            <div className="my-[7px] flex w-[82%] items-center gap-2" aria-hidden="true">
              <span className="h-px flex-1" style={{ backgroundColor: `${posterColors.gold}B3` }} />
              <span className="text-[11px]" style={{ color: posterColors.gold }}>✦</span>
              <span className="h-px flex-1" style={{ backgroundColor: `${posterColors.gold}B3` }} />
            </div>

            <p className="min-h-[34px] max-w-full whitespace-pre-line px-1 text-[12px] font-semibold leading-[17px]" style={{ color: posterColors.ink }}>
              {isBesanu
                ? 'બેસણાની શોકસભામાં આપની ઉપસ્થિતિથી શોકગ્રસ્ત પરિવારને સાંત્વના આપવાની વિનંતી છે.'
                : `દિલગીરી સાથે જણાવવાનું કે અમારા વ્હાલા ${name || '[અહીં નામ લખો]'} નું${deathDate ? ` તા. ${formatDate(deathDate)} ના રોજ` : ''} અવસાન થયેલ છે.\nભગવાન એમના દિવ્ય આત્માને શાંતિ અર્પે અને પરિવારજનોને આ દુઃખ સહન કરવાની શક્તિ આપે ...`}
            </p>

            <div className="mt-[9px] w-full rounded-[11px] border px-[14px] py-[7px] text-[12px] leading-[20px]" style={{ backgroundColor: posterColors.card, borderColor: `${posterColors.gold}99`, color: posterColors.ink }}>
              <p className="text-[14px] font-bold" style={{ color: posterColors.secondary }}>{isBesanu ? 'બેસણું / શોકસભા' : 'સ્મશાન યાત્રા / અંતિમ યાત્રા'}</p>
              <p className="min-h-[20px]">
                {isBesanu ? 'બેસણું' : 'અંતિમ યાત્રા'}
                {` તા. ${eventDate ? formatDate(eventDate) : '[તારીખ]'}`}
                {startTime ? ` ના ${Number(startTime.split(':')[0]) < 12 ? 'સવારે' : 'સાંજે'} ${formatTime(startTime)} વાગ્યે` : ''}
                {isBesanu && endTime ? ` થી ${formatTime(endTime)} સુધી` : ''}
                {!isBesanu && address ? ` અમારા નિવાસસ્થાન ${address} થી નીકળીને` : ''}
                {!isBesanu && cremationGround ? ` ${cremationGround} જશે.` : !isBesanu ? ' જશે.' : ''}
              </p>
            </div>

            <div className="mt-[5px] flex flex-col items-center text-[11px] leading-[14px]" style={{ color: posterColors.title }}>
              <span className="inline-block rounded-[4px] px-[10px] py-[1px] font-bold text-white" style={{ backgroundColor: posterColors.title }}>લિ.</span>
              <div className="mt-[2px] flex flex-col items-center">
                {(familyMembers.some((member) => member.trim())
                  ? familyMembers.filter((member) => member.trim())
                  : ['પરિવારજનો']
                ).slice(0, 5).map((member, index) => (
                  <p key={`${member}-${index}`} className="min-h-[14px] font-semibold">{member}</p>
                ))}
              </div>
            </div>
            <p className="mt-[2px] min-h-[15px] text-[12px] font-bold leading-[15px]" style={{ color: posterColors.muted }}>
              મો. {mobileNumber || '—'}
            </p>
            <div className="mt-[8px] flex w-[65%] items-center gap-2" aria-hidden="true">
              <span className="h-px flex-1" style={{ backgroundColor: `${posterColors.gold}B3` }} />
              <span className="text-[11px]" style={{ color: posterColors.gold }}>✦</span>
              <span className="h-px flex-1" style={{ backgroundColor: `${posterColors.gold}B3` }} />
            </div>
            <div className="mt-[6px] w-[48%] rounded-md px-2 py-[3px]" style={{ backgroundColor: 'rgba(255,255,255,.42)' }}>
              <img src="/bodla_parivar_logo.png" alt="Bodla Parivar" className="h-[32px] w-full object-contain" />
            </div>
          </div>
        </article>
      </div>
    </section>
  );
}

function FuneralNoticeDesignView({ kind }: { kind: 'antim' | 'besanu' }) {
  const [name, setName] = useState('');
  const isBesanu = kind === 'besanu';
  const [deathDate, setDeathDate] = useState('');
  const [weekday, setWeekday] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [time, setTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [place, setPlace] = useState('');
  const [cremationGround, setCremationGround] = useState('');
  const [familyMembers, setFamilyMembers] = useState(['', '', '', '', '']);
  const [mobile, setMobile] = useState('');
  const [photoUrl, setPhotoUrl] = useState('');
  const [publishing, setPublishing] = useState(false);
  const [message, setMessage] = useState('');

  const updateDeathDate = (value: string) => {
    setDeathDate(value);
    if (!value) {
      setWeekday('');
      return;
    }
    const date = new Date(`${value}T12:00:00`);
    const dayNames = ['રવિવાર', 'સોમવાર', 'મંગળવાર', 'બુધવાર', 'ગુરુવાર', 'શુક્રવાર', 'શનિવાર'];
    setWeekday(Number.isNaN(date.getTime()) ? '' : dayNames[date.getDay()]);
  };

  const uploadPhoto = async (file?: File) => {
    if (!file) return;
    setPublishing(true);
    setMessage('');
    try {
      const { data: { user } } = await supabase.auth.getUser();
      const path = `web/${user?.id || 'member'}/${Date.now()}-${file.name.replace(/[^a-zA-Z0-9._-]/g, '_')}`;
      const { error } = await supabase.storage.from('antim-yatra-posters').upload(path, file, { contentType: file.type, upsert: false });
      if (error) throw error;
      const { data } = supabase.storage.from('antim-yatra-posters').getPublicUrl(path);
      setPhotoUrl(data.publicUrl);
    } catch (error: any) {
      setMessage(error.message || 'Photo upload failed');
    } finally {
      setPublishing(false);
    }
  };

  const publish = async () => {
    if (!name.trim() || !deathDate || !eventDate || !place.trim()) {
      setMessage(isBesanu ? 'નામ, સ્વર્ગવાસ તારીખ, બેસણું તારીખ અને સ્થાન જરૂરી છે.' : 'નામ, સ્વર્ગવાસ તારીખ, અંતિમ યાત્રા તારીખ અને સ્થાન જરૂરી છે.');
      return;
    }
    setPublishing(true);
    setMessage('');
    try {
      const familyText = familyMembers.filter(Boolean).join(', ');
      const title = isBesanu ? `${name} - બેસણું સમાચાર` : `${name} - અંતિમ યાત્રા સમાચાર`;
      const description = [
        `નામ: ${name}`,
        `સ્વર્ગવાસ તારીખ: ${deathDate}${weekday ? ` (${weekday})` : ''}`,
        `${isBesanu ? 'બેસણું તારીખ' : 'અંતિમ યાત્રા તારીખ'}: ${eventDate}`,
        isBesanu ? `સમય: ${time}${endTime ? ` - ${endTime}` : ''}` : `સમય: ${time}`,
        `સ્થાન: ${place}`,
        !isBesanu && cremationGround ? `શમશાન/ચિતાભૂમિ: ${cremationGround}` : '',
        familyText ? `પરિવારના સભ્યો: ${familyText}` : '',
        mobile ? `મોબાઇલ: ${mobile}` : '',
      ].filter(Boolean).join('\n');
      const { data, error } = await supabase.rpc('publish_antim_yatra_notice', {
        p_payload: {
          title_gu: title,
          title_en: title,
          body_gu: title,
          body_en: title,
          description_gu: description,
          description_en: description,
          image_url: photoUrl || undefined,
        },
      });
      if (error) throw error;
      if (!data?.success) throw new Error('Notice could not be published');
      setMessage('નોટિસ સફળતાપૂર્વક પ્રકાશિત થઈ ગઈ છે.');
    } catch (error: any) {
      setMessage(error.message || 'Notice could not be published');
    } finally {
      setPublishing(false);
    }
  };

  return <section className="space-y-4">
    <div><h2 className={titleClass}>{isBesanu ? 'બેસણું પોસ્ટ' : 'અંતિમ યાત્રા પોસ્ટ'}</h2><p className="mt-1 text-sm text-slate-500">મોબાઇલ એન્ડ્રોઇડની સમાન સંપૂર્ણ વિગત સાથે શ્રીમતી/શ્રી આપવાનો સંદેશ બનાવો.</p></div>
    <div className="grid gap-4 lg:grid-cols-[minmax(0,360px)_1fr]">
      <div className={`${cardClass} space-y-3`}>
        <label className="block text-xs font-semibold text-slate-600">નામ<input value={name} onChange={(event) => setName(event.target.value)} className={`${inputClass} mt-1`} /></label>
        <label className="block text-xs font-semibold text-slate-600">સ્વર્ગવાસ તારીખ<input type="date" value={deathDate} onChange={(event) => setDeathDate(event.target.value)} className={`${inputClass} mt-1`} /></label>
        <label className="block text-xs font-semibold text-slate-600">ગુજરાતી વાર<input value={weekday} onChange={(event) => setWeekday(event.target.value)} placeholder="સોમવાર / મંગળવાર..." className={`${inputClass} mt-1`} /></label>
        <label className="block text-xs font-semibold text-slate-600">{isBesanu ? 'બેસણું તારીખ' : 'અંતિમ યાત્રા તારીખ'}<input type="date" value={eventDate} onChange={(event) => setEventDate(event.target.value)} className={`${inputClass} mt-1`} /></label>
        <div className="grid grid-cols-2 gap-2">
          <label className="text-xs font-semibold text-slate-600">{isBesanu ? 'શરૂ સમય' : 'સમય'}<input type="time" value={time} onChange={(event) => setTime(event.target.value)} className={`${inputClass} mt-1`} /></label>
          {isBesanu && <label className="text-xs font-semibold text-slate-600">અંત સમય<input type="time" value={endTime} onChange={(event) => setEndTime(event.target.value)} className={`${inputClass} mt-1`} /></label>}
        </div>
        <label className="block text-xs font-semibold text-slate-600">{isBesanu ? 'બેસણું સ્થળ / સરનામું' : 'અંતિમ યાત્રા સ્થળ / સરનામું'}<textarea value={place} onChange={(event) => setPlace(event.target.value)} className={`${inputClass} mt-1`} rows={2} /></label>
        {!isBesanu && <label className="block text-xs font-semibold text-slate-600">શમશાન/ચિતાભૂમિ<input value={cremationGround} onChange={(event) => setCremationGround(event.target.value)} className={`${inputClass} mt-1`} /></label>}
        <div className="space-y-2">{familyMembers.map((member, index) => <label key={index} className="block text-xs font-semibold text-slate-600">{`પરિવાર સભ્ય ${index + 1}`}<input value={member} onChange={(event) => setFamilyMembers((current) => current.map((value, i) => i === index ? event.target.value : value))} className={`${inputClass} mt-1`} /></label>)}</div>
        <label className="block text-xs font-semibold text-slate-600">મોબાઇલ નંબર<input value={mobile} onChange={(event) => setMobile(event.target.value)} className={`${inputClass} mt-1`} /></label>
        <label className="block text-xs font-semibold text-slate-600">ફોટો<input type="file" accept="image/*" onChange={(event) => void uploadPhoto(event.target.files?.[0])} className={`${inputClass} mt-1`} /></label>
        {message && <p className="text-xs font-semibold text-slate-700">{message}</p>}
        <div className="flex gap-2">
          <button type="button" onClick={() => window.print()} className="min-h-11 flex-1 rounded-xl border border-amber-300 text-sm font-bold text-amber-800">Print / PDF</button>
          <button type="button" disabled={publishing} onClick={() => void publish()} className="min-h-11 flex-1 rounded-xl bg-saffron px-4 text-sm font-bold text-white disabled:opacity-60">{publishing ? 'પ્રકાશિત થાય છે...' : 'નોટિસમાં પ્રકાશિત કરો'}</button>
        </div>
      </div>
      <article className="min-h-[420px] rounded-3xl border border-amber-200 bg-white p-8 text-center shadow-sm">
        <p className="text-4xl">ॐ शान्ति</p>
        <h3 className="mt-8 text-2xl font-bold">{name || 'નામ'}</h3>
        <p className="mt-4 text-lg">{isBesanu ? 'બેસણું કાર્યક્રમની માહિતી' : 'અંતિમ યાત્રાની માહિતી'}</p>
        {photoUrl && <img src={photoUrl} alt={name || 'Condolence photo'} className="mx-auto mt-5 h-28 w-28 rounded-full object-cover border-4 border-amber-100" />}
        <p className="mt-6 text-base">{deathDate && `સ્વર્ગવાસ: ${deathDate}${weekday ? ` (${weekday})` : ''}`}</p>
        <p className="mt-2 text-base">{eventDate && `${isBesanu ? 'બેસણું' : 'અંતિમ યાત્રા'} તારીખ: ${eventDate}`}</p>
        <p className="mt-2 text-base">{time && `સમય: ${time}`}</p>
        <p className="mt-2 text-base">{place && `સ્થળ: ${place}`}</p>
        {!isBesanu && cremationGround && <p className="mt-2 text-base">ચિતાભૂમિ: {cremationGround}</p>}
        {familyMembers.some(Boolean) && <p className="mt-2 text-base">પરિવાર: {familyMembers.filter(Boolean).join(', ')}</p>}
        {mobile && <p className="mt-2 text-base">મોબાઇલ: {mobile}</p>}
      </article>
    </div>
  </section>;
}

const historySections = [
  ['Families and roots', "The history of Bodla is woven together with the memories and roots of the families who made their home here. Across generations, families' work, traditions and connection to the village have helped shape Bodla's common identity."],
  ['Places of worship', "Bodla's temples and sacred places express both religious faith and the shared life of the village. Through worship, traditions and community occasions, these places have brought residents together."],
  ['Education and public life', "The village primary school and expanding access to education are an important part of Bodla's social change. The booklet also records the role of the Gram Panchayat and efforts in public life."],
  ['Village development', "Agriculture has long been an important foundation of life and livelihood for Bodla's families. Public facilities, community organizations and youth activities have responded to the village's changing needs."],
];

function VillageHistoryView() {
  return <section className="space-y-4"><div><h2 className={titleClass}>Bodla village history</h2><p className="mt-1 text-sm text-slate-500">Memories, roots and a story shared across generations.</p></div><p className={`${cardClass} text-sm leading-7 text-slate-600 dark:text-slate-300`}>This overview connects Bodla&apos;s story with its families and shared village life. Refer to the original Gujarati booklet for names, dates and figures.</p>{historySections.map(([title, text]) => <article key={title} className={cardClass}><h3 className={titleClass}>{title}</h3><p className="mt-2 text-sm leading-7 text-slate-600 dark:text-slate-300">{text}</p></article>)}</section>;
}
