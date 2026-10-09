'use client';

import React, { useState, useEffect, useRef, useCallback } from 'react';
import StatusBadge from '@/components/StatusBadge';
import {
  HeartHandshake, Plus, Search, Radio, X, Check, ChevronDown,
  DollarSign, User, Phone, Clock, TrendingUp, Eye, Gavel,
  CheckCircle2, XCircle, Hourglass, Bell, Activity, ArrowUpRight, Trash2, PhoneCall
} from 'lucide-react';
import { Offering, OfferingBid } from '@/lib/types';
import { supabase, isSupabaseConfigured } from '@/lib/supabase';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------
interface LiveBidEntry extends OfferingBid {
  isNew?: boolean;
  offeringTitle?: string;
}

// ---------------------------------------------------------------------------
// Helper: format relative time
// ---------------------------------------------------------------------------
function timeAgo(dateStr: string): string {
  const diff = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
  if (diff < 60) return `${diff}s ago`;
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return new Date(dateStr).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
}

// ---------------------------------------------------------------------------
// Bid Status Badge
// ---------------------------------------------------------------------------
function BidStatusBadge({ status }: { status: OfferingBid['status'] }) {
  const map = {
    PENDING:  { bg: 'bg-amber-50 border-amber-200 text-amber-700',  icon: <Hourglass className="w-3 h-3" />,     label: 'Pending' },
    APPROVED: { bg: 'bg-emerald-50 border-emerald-200 text-emerald-700', icon: <CheckCircle2 className="w-3 h-3" />, label: 'Approved' },
    REJECTED: { bg: 'bg-rose-50 border-rose-200 text-rose-700',     icon: <XCircle className="w-3 h-3" />,        label: 'Rejected' },
  };
  const s = map[status];
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold border ${s.bg}`}>
      {s.icon}{s.label}
    </span>
  );
}

// ---------------------------------------------------------------------------
// Main Page
// ---------------------------------------------------------------------------
export default function AdminOfferingsPage() {
  const [offerings, setOfferings]     = useState<Offering[]>([]);
  const [bids, setBids]               = useState<Record<string, OfferingBid[]>>({});
  const [search, setSearch]           = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [isLiveConnected, setIsLiveConnected] = useState(false);
  const [realtimeUnavailable, setRealtimeUnavailable] = useState(false);
  const [realtimeError, setRealtimeError] = useState<string | null>(null);
  const [lastSnapshotAt, setLastSnapshotAt] = useState<string | null>(null);
  const [isLoading, setIsLoading]     = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [recentLiveBids, setRecentLiveBids] = useState<LiveBidEntry[]>([]);
  const [newBidFlash, setNewBidFlash] = useState<string | null>(null); // offeringId that just got a new bid

  // Bids history modal
  const [selectedOffering, setSelectedOffering] = useState<Offering | null>(null);
  const [manualBidderName, setManualBidderName] = useState('');
  const [manualBidAmount, setManualBidAmount] = useState('');
  const [isSavingManualBid, setIsSavingManualBid] = useState(false);

  // New Offering Form
  const [formData, setFormData] = useState({
    title_gu: '',
    title_en: '',
    description_gu: '',
    description_en: '',
    amount: '',
    quantity: '1',
    location_gu: 'બોદલા મંદિર સંકુલ',
    location_en: 'Bodla Temple Complex',
    status: 'ACTIVE' as Offering['status']
  });

  const offeringsRef = useRef(offerings);
  const refreshSnapshotRef = useRef<(() => Promise<void>) | null>(null);
  const realtimeEventVersionRef = useRef(0);
  offeringsRef.current = offerings;

  const resetBidCallCount = (offeringId: string) => {
    setOfferings(prev => prev.map(item => item.id === offeringId
      ? { ...item, bid_call_count: 0 }
      : item
    ));
    setSelectedOffering(prev => prev?.id === offeringId
      ? { ...prev, bid_call_count: 0 }
      : prev
    );
  };

  // -------------------------------------------------------------------------
  // Load data + subscribe
  // -------------------------------------------------------------------------
  useEffect(() => {
    let offeringChannel: ReturnType<typeof supabase.channel> | null = null;
    let bidsChannel: ReturnType<typeof supabase.channel> | null = null;
    let offeringsConnected = false;
    let bidsConnected = false;

    async function refreshSnapshot() {
      const eventVersionAtStart = realtimeEventVersionRef.current;
      const [offeringsResult, bidsResult] = await Promise.all([
        supabase.from('offerings').select('*').order('created_at', { ascending: false }),
        supabase.from('offering_bids').select('*').order('created_at', { ascending: false })
      ]);
      if (eventVersionAtStart !== realtimeEventVersionRef.current) return;

      if (offeringsResult.error) console.error('Failed to refresh offerings:', offeringsResult.error);
      if (bidsResult.error) console.error('Failed to refresh bids:', bidsResult.error);
      const snapshotErrors = [offeringsResult.error?.message, bidsResult.error?.message].filter(Boolean);
      setRealtimeError(snapshotErrors.length ? snapshotErrors.join(' · ') : null);

      const offeringsData = offeringsResult.data as Offering[] | null;
      const bidsData = bidsResult.data as OfferingBid[] | null;
      if (offeringsData) setOfferings(offeringsData);
      if (bidsData) {
        const grouped: Record<string, OfferingBid[]> = {};
        bidsData.forEach(bid => {
          if (!grouped[bid.offering_id]) grouped[bid.offering_id] = [];
          grouped[bid.offering_id].push(bid);
        });
        setBids(grouped);
        setRecentLiveBids(bidsData.slice(0, 5).map(bid => ({
          ...bid,
          offeringTitle: offeringsData?.find(offering => offering.id === bid.offering_id)?.title_en
        })));
      }
      if (!offeringsResult.error && !bidsResult.error) setLastSnapshotAt(new Date().toLocaleTimeString());
    }
    refreshSnapshotRef.current = refreshSnapshot;

    async function loadRealtimeData() {
      if (!isSupabaseConfigured) { setIsLiveConnected(false); return; }
      setIsLoading(true);
      try {
        await refreshSnapshot();

        // Subscribe to Offerings
        offeringChannel = supabase
          .channel(`offerings_admin_${Date.now()}`)
          .on('postgres_changes', { event: '*', schema: 'public', table: 'offerings' }, (payload) => {
            realtimeEventVersionRef.current += 1;
            if (payload.eventType === 'INSERT') {
              const r = payload.new as Offering;
              setOfferings(prev => [r, ...prev.filter(i => i.id !== r.id)]);
            } else if (payload.eventType === 'UPDATE') {
              const r = payload.new as Offering;
              setOfferings(prev => prev.map(i => i.id === r.id ? r : i));
            } else if (payload.eventType === 'DELETE') {
              setOfferings(prev => prev.filter(i => i.id !== payload.old.id));
            }
          })
          .subscribe(status => {
            offeringsConnected = status === 'SUBSCRIBED';
            setIsLiveConnected(offeringsConnected && bidsConnected);
            if (status === 'CHANNEL_ERROR' || status === 'TIMED_OUT' || status === 'CLOSED') {
              setRealtimeUnavailable(true);
              setRealtimeError(`Offerings channel: ${status}`);
            } else if (offeringsConnected && bidsConnected) {
              setRealtimeUnavailable(false);
              setRealtimeError(null);
            }
          });

        // Subscribe to Bids — this is the key live feed
        bidsChannel = supabase
          .channel(`bids_admin_${Date.now()}`)
          .on('postgres_changes', { event: '*', schema: 'public', table: 'offering_bids' }, (payload) => {
            realtimeEventVersionRef.current += 1;
            if (payload.eventType === 'INSERT') {
              const newBid = payload.new as OfferingBid;
              resetBidCallCount(newBid.offering_id);
              // Find the offering title from current state
              const title = offeringsRef.current.find(o => o.id === newBid.offering_id)?.title_en;
              const liveEntry: LiveBidEntry = { ...newBid, isNew: true, offeringTitle: title };

              setBids(prev => {
                const existing = prev[newBid.offering_id] || [];
                return { ...prev, [newBid.offering_id]: [newBid, ...existing.filter(b => b.id !== newBid.id)] };
              });

              setRecentLiveBids(prev => [liveEntry, ...prev].slice(0, 10));
              setNewBidFlash(newBid.offering_id);
              setTimeout(() => setNewBidFlash(null), 3000);

              // If the bids modal for this offering is open, auto-update is already handled above
            } else if (payload.eventType === 'UPDATE') {
              const updated = payload.new as OfferingBid;
              setBids(prev => {
                const existing = prev[updated.offering_id] || [];
                return { ...prev, [updated.offering_id]: existing.map(b => b.id === updated.id ? updated : b) };
              });
            } else if (payload.eventType === 'DELETE') {
              const deletedId = (payload.old as Pick<OfferingBid, 'id'>).id;
              setBids(prev => Object.fromEntries(
                Object.entries(prev).map(([offeringId, entries]) => [
                  offeringId,
                  entries.filter(bid => bid.id !== deletedId)
                ])
              ));
              setRecentLiveBids(prev => prev.filter(bid => bid.id !== deletedId));
            }
          })
          .subscribe(status => {
            bidsConnected = status === 'SUBSCRIBED';
            setIsLiveConnected(offeringsConnected && bidsConnected);
            if (status === 'CHANNEL_ERROR' || status === 'TIMED_OUT' || status === 'CLOSED') {
              setRealtimeUnavailable(true);
              setRealtimeError(`Bids channel: ${status}`);
            } else if (offeringsConnected && bidsConnected) {
              setRealtimeUnavailable(false);
              setRealtimeError(null);
            }
          });

      } catch (err) {
        console.error('Realtime error:', err);
      } finally {
        setIsLoading(false);
      }
    }

    loadRealtimeData();
    const refreshTimer = isSupabaseConfigured
      ? window.setInterval(() => {
          void refreshSnapshot().catch(err => console.error('Realtime refresh failed:', err));
        }, 5000)
      : undefined;
    return () => {
      if (refreshTimer !== undefined) window.clearInterval(refreshTimer);
      if (offeringChannel) supabase.removeChannel(offeringChannel);
      if (bidsChannel) supabase.removeChannel(bidsChannel);
      refreshSnapshotRef.current = null;
    };
  }, []);

  // -------------------------------------------------------------------------
  // Actions
  // -------------------------------------------------------------------------
  const handleStatusChange = async (id: string, newStatus: Offering['status']) => {
    setOfferings(prev => prev.map(i => i.id === id ? { ...i, status: newStatus } : i));
    if (isSupabaseConfigured) {
      await supabase.from('offerings').update({ status: newStatus }).eq('id', id);
    }
  };

  const handleBidAction = async (
    bidId: string, offeringId: string, newStatus: OfferingBid['status']
  ) => {
    if (isSupabaseConfigured) {
      const { error } = await supabase.from('offering_bids').update({ status: newStatus }).eq('id', bidId);
      if (error) {
        alert('Bid status could not be saved: ' + error.message);
        return;
      }
      if (newStatus === 'APPROVED') {
        const approvedBid = bids[offeringId]?.find(b => b.id === bidId);
        const offering = offerings.find(o => o.id === offeringId);
        if (approvedBid) {
          const { error: amountError } = await supabase.from('offerings').update({ amount: approvedBid.amount }).eq('id', offeringId);
          if (amountError) alert('Bid was approved, but the offering amount could not be updated: ' + amountError.message);
          // Notify all users of the new approved bid
          const { error: notificationError } = await supabase.from('notifications').insert({
            title_gu: `🔔 ${offering?.title_gu ?? 'ચઢાવો'} – નવી બોલી`,
            title_en: `New Bid Approved: ${offering?.title_en ?? 'Offering'}`,
            body_gu: `₹${approvedBid.amount.toLocaleString('en-IN')} – ${approvedBid.bidder_name}`,
            body_en: `₹${approvedBid.amount.toLocaleString('en-IN')} by ${approvedBid.bidder_name}`,
            type: 'OFFERING',
            reference_id: offeringId,
          });
          if (notificationError) alert('Bid was approved, but the notification could not be sent: ' + notificationError.message);
        }
      }
    }
    setBids(prev => {
      const existing = prev[offeringId] || [];
      return { ...prev, [offeringId]: existing.map(b => b.id === bidId ? { ...b, status: newStatus } : b) };
    });
  };


  const handleDeleteBid = async (bidId: string, offeringId: string) => {
    if (!isSupabaseConfigured) return;
    if (!window.confirm('Delete this bid permanently?')) return;
    
    // Optimistically calculate new top bid and update offerings state
    const currentBids = bids[offeringId] || [];
    const remainingBids = currentBids.filter(b => b.id !== bidId);
    const newTopBidEntry = remainingBids.filter(bid => bid.status !== 'REJECTED').reduce<OfferingBid | null>((highest, bid) =>
      !highest || bid.amount > highest.amount ? bid : highest, null
    );
    const newTopBid = newTopBidEntry?.amount ?? null;
    const deletedBid = currentBids.find(bid => bid.id === bidId);
    const currentOffering = offerings.find(offering => offering.id === offeringId);
    const deletedWinningBid = currentOffering?.winning_bid_amount === deletedBid?.amount;

    const { data: deletedRows, error } = await supabase
      .from('offering_bids')
      .delete()
      .eq('id', bidId)
      .select('id');
    if (error) {
      alert('Delete failed: ' + error.message);
      return;
    }
    if (!deletedRows?.length) {
      alert('The bid was not deleted from the database. Check staff access and refresh the offering.');
      await refreshSnapshotRef.current?.();
      return;
    }

    setBids(prev => ({
      ...prev,
      [offeringId]: remainingBids
    }));
    setRecentLiveBids(prev => prev.filter(bid => bid.id !== bidId));
    
    // Update local offerings list so UI reflects the new top bid
    setOfferings(prev => prev.map(o => {
      if (o.id === offeringId) {
        return {
          ...o,
          winning_bid_amount: newTopBid,
          ...(o.amount === deletedBid?.amount ? { amount: newTopBid ?? undefined } : {}),
          ...(o.status === 'COMPLETED' && deletedWinningBid
            ? { winning_bidder_name: newTopBidEntry?.bidder_name ?? null }
            : {})
        };
      }
      return o;
    }));
    
    // Update the selected offering if it is displayed
    if (selectedOffering?.id === offeringId) {
       setSelectedOffering(prev => prev ? {
         ...prev,
         winning_bid_amount: newTopBid,
         ...(prev.amount === deletedBid?.amount ? { amount: newTopBid ?? undefined } : {}),
         ...(prev.status === 'COMPLETED' && deletedWinningBid
           ? { winning_bidder_name: newTopBidEntry?.bidder_name ?? null }
           : {})
       } : null);
    }
  };

  const handleDeleteOffering = async (offering: Offering) => {
    if (!isSupabaseConfigured) return;
    if (!window.confirm(`Delete "${offering.title_en}" and all of its bids permanently?`)) return;

    const { count, error } = await supabase
      .from('offerings')
      .delete({ count: 'exact' })
      .eq('id', offering.id);
    if (error) {
      alert('Offering could not be deleted: ' + error.message);
      return;
    }
    if (count === 0) {
      alert('No offering was deleted. Run database/migrations/014_staff_delete_offerings.sql in Supabase SQL Editor, then retry.');
      return;
    }

    setOfferings(prev => prev.filter(item => item.id !== offering.id));
    setBids(prev => {
      const next = { ...prev };
      delete next[offering.id];
      return next;
    });
    setRecentLiveBids(prev => prev.filter(bid => bid.offering_id !== offering.id));
    setSelectedOffering(current => current?.id === offering.id ? null : current);
  };

  const handleAddInPersonBid = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedOffering) return;
    const amount = Number(manualBidAmount);
    if (!manualBidderName.trim() || !Number.isFinite(amount) || amount <= 0) return;

    setIsSavingManualBid(true);
    const { data, error } = await supabase
      .from('offering_bids')
      .insert({
        offering_id: selectedOffering.id,
        bidder_name: manualBidderName.trim(),
        amount,
        status: 'APPROVED'
      })
      .select('*')
      .single();
    setIsSavingManualBid(false);

    if (error) {
      alert('Bid could not be saved: ' + error.message);
      return;
    }

    const bid = data as OfferingBid;
    setBids(prev => ({
      ...prev,
      [selectedOffering.id]: [bid, ...(prev[selectedOffering.id] || []).filter(item => item.id !== bid.id)]
    }));
    setRecentLiveBids(prev => [{ ...bid, isNew: true, offeringTitle: selectedOffering.title_en }, ...prev].slice(0, 10));
    resetBidCallCount(selectedOffering.id);
    setManualBidderName('');
    setManualBidAmount('');
  };

  const handleCallBid = async () => {
    if (!selectedOffering) return;
    const latestOffering = offerings.find(item => item.id === selectedOffering.id) || selectedOffering;
    const nextCall = latestOffering.bid_call_count + 1;
    const topBid = selectedBids.filter(bid => bid.status !== 'REJECTED').reduce<OfferingBid | null>((highest, bid) =>
      !highest || bid.amount > highest.amount ? bid : highest, null
    );
    if (!topBid) {
      alert('Add at least one bid before calling the bid.');
      return;
    }

    let winnerName: string | null = null;
    if (nextCall === 3) {
      if (topBid.bidder_name && topBid.bidder_name.trim().length > 0) {
        winnerName = topBid.bidder_name.trim();
      } else {
        winnerName = window.prompt('Confirm the in-person bidder name:', '')?.trim() || null;
        if (!winnerName) return;
      }
    }

    const { data, error } = await supabase.rpc('call_offering_bid', {
      p_offering_id: selectedOffering.id,
      p_winner_name: winnerName
    });
    if (error) {
      alert('Could not record bid call: ' + error.message);
      return;
    }

    const bidCallCount = Number(data);
    const nextStatus = bidCallCount === 3 ? 'COMPLETED' : latestOffering.status;
    const updatedOffering: Offering = {
      ...latestOffering,
      bid_call_count: bidCallCount,
      status: nextStatus,
      winning_bidder_name: winnerName,
      winning_bid_amount: winnerName ? topBid.amount : latestOffering.winning_bid_amount
    };
    setOfferings(prev => prev.map(item => item.id === updatedOffering.id ? updatedOffering : item));
    setSelectedOffering(updatedOffering);

    // Notify all users about the bid call (using valid 'OFFERING' notification type)
    const gujaratiCount = ['એક', 'બે', 'ત્રણ'][bidCallCount - 1] ?? `${bidCallCount}`;
    const isClosed = bidCallCount >= 3;
    const { error: notificationError } = await supabase.from('notifications').insert({
      title_gu: `🔔 ${updatedOffering.title_gu} – ₹${topBid.amount.toLocaleString('en-IN')} · ${bidCallCount} વાર`,
      title_en: `Bid Call ${bidCallCount}/3: ${updatedOffering.title_en}`,
      body_gu: isClosed
        ? `બોલી બંધ! વિજેતા: ${winnerName}`
        : `₹${topBid.amount.toLocaleString('en-IN')} – ${gujaratiCount} વાર બોલો`,
      body_en: isClosed
        ? `Bidding closed! Winner: ${winnerName}`
        : `₹${topBid.amount.toLocaleString('en-IN')} – Call ${bidCallCount} of 3`,
      type: 'OFFERING',
      reference_id: updatedOffering.id,
    });
    if (notificationError) alert('Bid call was recorded, but the notification could not be sent: ' + notificationError.message);
  };


  const handleCreateOffering = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.title_gu && !formData.title_en) return;
    if (isSupabaseConfigured) {
      const { error } = await supabase.from('offerings').insert([{
        title_gu:       formData.title_gu || formData.title_en,
        title_en:       formData.title_en || formData.title_gu,
        description_gu: formData.description_gu,
        description_en: formData.description_en,
        amount:         formData.amount ? parseFloat(formData.amount) : null,
        quantity:       formData.quantity ? parseInt(formData.quantity) : 1,
        location_gu:    formData.location_gu,
        location_en:    formData.location_en,
        status:         formData.status
      }]);
      if (error) { alert('Insert failed: ' + error.message); return; }
    }
    setIsModalOpen(false);
    setFormData({
      title_gu: '', title_en: '', description_gu: '', description_en: '',
      amount: '', quantity: '1',
      location_gu: 'બોદલા મંદિર સંકુલ', location_en: 'Bodla Temple Complex',
      status: 'ACTIVE'
    });
  };

  // -------------------------------------------------------------------------
  // Derived state
  // -------------------------------------------------------------------------
  const filtered = offerings.filter(item => {
    const matchesSearch = item.title_gu.includes(search) || item.title_en.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || item.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const totalBidsCount  = Object.values(bids).reduce((acc, arr) => acc + arr.length, 0);
  const pendingBidsCount = Object.values(bids).reduce((acc, arr) => acc + arr.filter(b => b.status === 'PENDING').length, 0);
  const activeCount     = offerings.filter(i => i.status === 'ACTIVE').length;

  // Selected offering's bids (sorted newest first)
  const selectedBids = selectedOffering
    ? (bids[selectedOffering.id] || []).slice().sort(
        (a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
      )
    : [];

  const eligibleBids = selectedBids.filter(bid => bid.status !== 'REJECTED');
  const highestBid = eligibleBids.length > 0 ? Math.max(...eligibleBids.map(bid => bid.amount)) : null;
  const currentSelectedOffering = selectedOffering
    ? offerings.find(item => item.id === selectedOffering.id) || selectedOffering
    : null;

  // -------------------------------------------------------------------------
  // Render
  // -------------------------------------------------------------------------
  return (
    <div className="w-full min-w-0 max-w-full space-y-3 overflow-x-hidden">
      {/* ── Header ─────────────────────────────────────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="text-xl font-bold text-brand-text font-gujarati flex items-center gap-2">
              <HeartHandshake className="w-6 h-6 text-saffron" />
              <span>ચઢાવો – લાઇવ બોલી (Live Bidding)</span>
            </h1>
            <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold border ${
              isLiveConnected
                ? 'bg-emerald-50 text-emerald-800 border-emerald-300'
                : 'bg-amber-50 text-amber-800 border-amber-300'
            }`}>
              <Radio className={`w-3.5 h-3.5 ${isLiveConnected ? 'animate-pulse text-emerald-600' : 'text-amber-600'}`} />
              {isLiveConnected ? 'LIVE' : 'Connecting…'}
            </span>
          </div>
          <p className="text-xs text-brand-muted mt-1">
            {isLiveConnected
              ? 'Realtime connected'
              : realtimeUnavailable
                ? `Realtime unavailable · polling every 5s${lastSnapshotAt ? ` · Snapshot ${lastSnapshotAt}` : ''}`
                : 'Connecting to realtime feed…'}
            {' · '}
            <span className="font-semibold text-saffron">{activeCount} Active</span> offerings ·{' '}
            <span className="font-semibold text-amber-600">{pendingBidsCount} Pending</span> bids
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => void refreshSnapshotRef.current?.()}
            className="px-3 py-2 rounded-lg border border-brand-border bg-white text-brand-text text-sm font-semibold hover:bg-cream transition"
          >
            Refresh feed
          </button>
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-4 py-2 rounded-lg bg-saffron text-white text-sm font-semibold hover:bg-amber-600 transition shadow-md flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            <span>નવો ચઢાવો ઉમેરો</span>
          </button>
        </div>
      </div>

      {/* ── Stat Cards ─────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
        {[
          { label: 'Active Offerings', labelGu: 'સક્રિય ચઢાવો', value: activeCount, icon: <Activity className="w-5 h-5 text-emerald-600" />, color: 'bg-emerald-50 border-emerald-200' },
          { label: 'Total Bids', labelGu: 'કુલ બોલીઓ', value: totalBidsCount, icon: <Gavel className="w-5 h-5 text-saffron" />, color: 'bg-amber-50 border-amber-200' },
          { label: 'Pending Review', labelGu: 'સમીક્ષા બાકી', value: pendingBidsCount, icon: <Hourglass className="w-5 h-5 text-rose-500" />, color: 'bg-rose-50 border-rose-200' },
          { label: 'Live Feed', labelGu: 'તાજા ફેરફારો', value: recentLiveBids.length, icon: <Bell className="w-5 h-5 text-purple-600" />, color: 'bg-purple-50 border-purple-200' },
        ].map(stat => (
          <div key={stat.label} className={`${stat.color} border rounded-lg p-2.5 flex items-center gap-2.5`}>
            <div className="p-1.5 bg-white rounded-md shadow-sm">{stat.icon}</div>
            <div>
              <div className="text-xl font-bold text-brand-text">{stat.value}</div>
              <div className="text-[11px] text-brand-muted font-gujarati leading-tight">{stat.labelGu}</div>
              <div className="text-[10px] text-brand-muted/70">{stat.label}</div>
            </div>
          </div>
        ))}
      </div>

      {realtimeError && (
        <p role="status" className="border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          Live feed status: {realtimeError}
        </p>
      )}

      {/* ── Search + Filter ─────────────────────────────────────────────── */}
      <div className="min-w-0 bg-cream-surface border border-brand-border rounded-lg p-2.5 flex flex-col md:flex-row items-center gap-2">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-brand-muted absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="ચઢાવો શોધો / Search offerings..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2 rounded-lg border border-brand-border bg-white text-sm focus:outline-none focus:border-saffron"
          />
        </div>
        <div className="flex flex-wrap gap-2 w-full md:w-auto">
          {['ALL', 'ACTIVE', 'PENDING_APPROVAL', 'APPROVED', 'COMPLETED', 'REJECTED'].map(s => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-1.5 rounded-lg border text-[11px] font-bold transition whitespace-nowrap ${
                statusFilter === s
                  ? 'bg-saffron text-white border-saffron'
                  : 'bg-white border-brand-border text-brand-text hover:bg-amber-50'
              }`}
            >
              {s === 'ALL' ? 'તમામ' : s.replace(/_/g, ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* ── Offerings Table ─────────────────────────────────────────────── */}
      <div className="w-full min-w-0 bg-cream-surface border border-brand-border rounded-2xl overflow-hidden shadow-sm">
        {isLoading && (
          <div className="px-6 py-3 bg-amber-50 border-b border-amber-200 text-xs text-amber-700 font-medium flex items-center gap-2">
            <div className="w-3 h-3 border-2 border-amber-500 border-t-transparent rounded-full animate-spin" />
            Loading live data…
          </div>
        )}
        <div className="hidden w-full min-w-0 overflow-x-auto xl:block">
          <table className="w-full min-w-[1100px] table-fixed text-left text-sm text-brand-text">
            <colgroup>
              <col className="w-[27%]" />
              <col className="w-[14%]" />
              <col className="w-[12%]" />
              <col className="w-[11%]" />
              <col className="w-[36%]" />
            </colgroup>
            <thead className="bg-cream text-[11px] text-brand-muted uppercase border-b border-brand-border">
              <tr>
                <th className="py-3.5 px-4">ચઢાવો / Offering</th>
                <th className="py-3.5 px-4">ઉચ્ચ રકમ / Top Amount</th>
                <th className="py-3.5 px-4">બોલીઓ / Bids</th>
                <th className="py-3.5 px-4">સ્થિતિ / Status</th>
                <th className="sticky right-0 z-20 bg-cream py-3.5 px-4 text-right">ક્રિયાઓ / Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-brand-border">
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-sm text-brand-muted">
                    {isLoading ? 'Loading…' : 'No offerings found.'}
                  </td>
                </tr>
              ) : filtered.map(item => {
                const offeringBids = bids[item.id] || [];
                const pending = offeringBids.filter(b => b.status === 'PENDING').length;
                const topBid   = offeringBids.length > 0 ? Math.max(...offeringBids.map(b => b.amount)) : null;
                const isFlashing = newBidFlash === item.id;

                return (
                  <tr
                    key={item.id}
                    className={`transition ${isFlashing ? 'bg-emerald-50 animate-pulse' : 'hover:bg-cream/40'}`}
                  >
                    {/* Title */}
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-brand-text flex items-center gap-2">
                        {item.status === 'ACTIVE' && (
                          <span className="relative flex h-2.5 w-2.5">
                            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
                            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500" />
                          </span>
                        )}
                        <span className="font-gujarati">{item.title_gu}</span>
                      </div>
                      <div className="text-xs text-brand-muted">{item.title_en}</div>
                    </td>

                    {/* Amount */}
                    <td className="py-3.5 px-4">
                      {topBid ? (
                        <div>
                          <div className="font-bold text-saffron text-sm flex items-center gap-1">
                            <TrendingUp className="w-3.5 h-3.5" />
                            ₹{topBid.toLocaleString()}
                          </div>
                          {item.amount && topBid > item.amount && (
                            <div className="text-[10px] text-emerald-600 font-medium">
                              +₹{(topBid - item.amount).toLocaleString()} above base
                            </div>
                          )}
                        </div>
                      ) : (
                        <span className="text-brand-muted text-xs">
                          {item.amount ? `₹${item.amount.toLocaleString()}` : '—'}
                        </span>
                      )}
                    </td>

                    {/* Bids count */}
                    <td className="py-3.5 px-4">
                      <button
                        onClick={() => setSelectedOffering(item)}
                        className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-bold transition ${
                          isFlashing
                            ? 'bg-emerald-500 text-white shadow-lg'
                            : 'bg-amber-50 text-amber-800 hover:bg-amber-100 border border-amber-200'
                        }`}
                      >
                        <Gavel className="w-3.5 h-3.5" />
                        <span>{offeringBids.length} Bids</span>
                        {pending > 0 && (
                          <span className="bg-red-500 text-white rounded-full w-4 h-4 flex items-center justify-center text-[9px] font-black ml-0.5">
                            {pending}
                          </span>
                        )}
                        {isFlashing && <ArrowUpRight className="w-3 h-3" />}
                      </button>
                    </td>

                    {/* Status */}
                    <td className="py-3.5 px-4">
                      <StatusBadge status={item.status} />
                    </td>

                    {/* Actions */}
                    <td className="sticky right-0 z-10 bg-cream-surface py-3.5 px-4 text-right shadow-[-8px_0_8px_-8px_rgba(0,0,0,0.25)]">
                      <div className="flex flex-nowrap items-center justify-end gap-1 whitespace-nowrap">
                        <button
                          onClick={() => setSelectedOffering(item)}
                          className="px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700 text-[11px] font-semibold hover:bg-slate-200 transition flex items-center gap-1"
                        >
                          <Eye className="w-3 h-3" />Review bids
                        </button>
                        {item.status === 'PENDING_APPROVAL' && (
                          <button onClick={() => handleStatusChange(item.id, 'APPROVED')}
                            className="px-2.5 py-1 rounded-lg bg-emerald-600 text-white text-[11px] font-semibold hover:bg-emerald-700 transition">
                            મંજૂર
                          </button>
                        )}
                        {(item.status === 'APPROVED' || item.status === 'PENDING_APPROVAL') && (
                          <button onClick={() => handleStatusChange(item.id, 'ACTIVE')}
                            className="px-2.5 py-1 rounded-lg bg-saffron text-white text-[11px] font-semibold hover:bg-amber-600 transition">
                            Active
                          </button>
                        )}
                        {item.status === 'ACTIVE' && (
                          <button onClick={() => handleStatusChange(item.id, 'COMPLETED')}
                            className="px-2.5 py-1 rounded-lg bg-stone-700 text-white text-[11px] font-semibold hover:bg-stone-800 transition">
                            Complete
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => void handleDeleteOffering(item)}
                          className="flex min-h-9 items-center gap-1 border border-rose-200 px-2.5 py-1 text-rose-700 hover:bg-rose-50 text-[11px] font-semibold transition"
                          title="Delete offering and its bids permanently"
                        >
                          <Trash2 className="h-3.5 w-3.5" />Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <div className="grid gap-2 p-2 xl:hidden">
          {filtered.length === 0 ? (
            <p className="py-10 text-center text-sm text-brand-muted">
              {isLoading ? 'Loading…' : 'No offerings found.'}
            </p>
          ) : filtered.map(item => {
            const offeringBids = bids[item.id] || [];
            const pending = offeringBids.filter(bid => bid.status === 'PENDING').length;
            const topBid = offeringBids.length > 0 ? Math.max(...offeringBids.map(bid => bid.amount)) : null;
            return (
              <article key={item.id} className="min-w-0 space-y-2 border border-brand-border bg-white p-2.5">
                <div className="flex min-w-0 items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h2 className="break-words text-sm font-semibold text-brand-text">{item.title_gu}</h2>
                    <p className="mt-0.5 break-words text-xs text-brand-muted">{item.title_en}</p>
                  </div>
                  <StatusBadge status={item.status} />
                </div>

                <div className="grid grid-cols-2 gap-2 border-y border-brand-border py-2 text-xs">
                  <div>
                    <p className="text-brand-muted">Top amount</p>
                    <p className="mt-1 font-bold text-saffron">
                      ₹{(topBid ?? item.amount ?? 0).toLocaleString()}
                    </p>
                  </div>
                  <div>
                    <p className="text-brand-muted">Bids</p>
                    <button type="button" onClick={() => setSelectedOffering(item)} className="mt-1 inline-flex min-h-8 items-center gap-1.5 border border-amber-200 bg-amber-50 px-2.5 font-semibold text-amber-900">
                      <Gavel className="h-3.5 w-3.5" />{offeringBids.length}
                      {pending > 0 && <span className="rounded-full bg-rose-600 px-1.5 text-[10px] text-white">{pending}</span>}
                    </button>
                  </div>
                </div>

                <div className="flex flex-wrap gap-1.5">
                  <button type="button" onClick={() => setSelectedOffering(item)} className="inline-flex min-h-9 items-center gap-1.5 border border-brand-border bg-cream px-3 text-xs font-semibold text-brand-text">
                    <Eye className="h-3.5 w-3.5" />Review bids
                  </button>
                  {item.status === 'PENDING_APPROVAL' && (
                    <button type="button" onClick={() => void handleStatusChange(item.id, 'APPROVED')} className="min-h-9 border border-emerald-200 bg-emerald-50 px-3 text-xs font-semibold text-emerald-800">Approve</button>
                  )}
                  {(item.status === 'APPROVED' || item.status === 'PENDING_APPROVAL') && (
                    <button type="button" onClick={() => void handleStatusChange(item.id, 'ACTIVE')} className="min-h-9 border border-amber-200 bg-amber-50 px-3 text-xs font-semibold text-amber-900">Activate</button>
                  )}
                  {item.status === 'ACTIVE' && (
                    <button type="button" onClick={() => void handleStatusChange(item.id, 'COMPLETED')} className="min-h-9 border border-stone-300 bg-stone-100 px-3 text-xs font-semibold text-stone-800">Complete</button>
                  )}
                  <button type="button" onClick={() => void handleDeleteOffering(item)} className="inline-flex min-h-9 items-center gap-1.5 border border-rose-200 bg-rose-50 px-3 text-xs font-semibold text-rose-700">
                    <Trash2 className="h-3.5 w-3.5" />Delete
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      </div>

      {/* ════════════════════════════════════════════════════════════════
          Modal: Live Bid History for a specific Offering
      ════════════════════════════════════════════════════════════════ */}
      {selectedOffering && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white border border-brand-border rounded-2xl w-full max-w-2xl shadow-2xl flex flex-col max-h-[90vh] overflow-hidden">

            {/* Modal Header */}
            <div className="px-6 py-4 border-b border-brand-border bg-gradient-to-r from-amber-50 to-orange-50 flex items-start justify-between shrink-0">
              <div>
                <div className="flex items-center gap-2">
                  <Gavel className="w-5 h-5 text-saffron" />
                  <h2 className="text-base font-bold text-brand-text font-gujarati">
                    બોલી ઇતિહાસ – {selectedOffering.title_gu}
                  </h2>
                  {isLiveConnected && (
                    <span className="flex items-center gap-1 text-[10px] bg-emerald-100 text-emerald-700 px-2 py-0.5 rounded-full font-bold border border-emerald-200">
                      <Radio className="w-2.5 h-2.5 animate-pulse" />LIVE
                    </span>
                  )}
                </div>
                <p className="text-xs text-brand-muted mt-0.5">{selectedOffering.title_en}</p>
                <p className="mt-1 text-[11px] text-brand-muted">
                  {isLiveConnected ? 'Live bid updates connected.' : `Refreshing bid history every 5 seconds.${lastSnapshotAt ? ` Last sync ${lastSnapshotAt}.` : ''}`}
                </p>
              </div>
              <button onClick={() => setSelectedOffering(null)} className="p-1.5 rounded-lg hover:bg-brand-border text-brand-muted transition ml-4">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Bid Stats Strip */}
            <div className="px-6 py-3 bg-amber-50/70 border-b border-brand-border grid grid-cols-3 gap-4 shrink-0">
              <div className="text-center">
                <div className="text-xl font-black text-saffron">
                  {selectedBids.length}
                </div>
                <div className="text-[10px] text-brand-muted font-medium uppercase tracking-wide">Total Bids</div>
              </div>
              <div className="text-center border-x border-brand-border">
                <div className="text-xl font-black text-emerald-600">
                  {highestBid ? `₹${highestBid.toLocaleString()}` : '—'}
                </div>
                <div className="text-[10px] text-brand-muted font-medium uppercase tracking-wide">Highest Bid</div>
              </div>
              <div className="text-center">
                <div className="text-xl font-black text-amber-600">
                  {selectedBids.filter(b => b.status === 'PENDING').length}
                </div>
                <div className="text-[10px] text-brand-muted font-medium uppercase tracking-wide">Pending</div>
              </div>
            </div>

            {currentSelectedOffering && (
              <div className="px-5 py-4 border-b border-brand-border space-y-3 shrink-0">
                {currentSelectedOffering.status === 'ACTIVE' && currentSelectedOffering.bid_call_count < 3 && (
                  <>
                    <form onSubmit={handleAddInPersonBid} className="grid grid-cols-1 sm:grid-cols-[1fr_10rem_auto] gap-2 items-end">
                      <label className="text-xs font-semibold text-brand-muted">
                        બોલી આપનારનું નામ
                        <input
                          required
                          value={manualBidderName}
                          onChange={event => setManualBidderName(event.target.value)}
                          placeholder="નામ દાખલ કરો"
                          className="mt-1 w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm font-normal text-brand-text"
                        />
                      </label>
                      <label className="text-xs font-semibold text-brand-muted">
                        બોલી રકમ (₹)
                        <input
                          required
                          type="number"
                          min="1"
                          step="0.01"
                          value={manualBidAmount}
                          onChange={event => setManualBidAmount(event.target.value)}
                          placeholder="1001"
                          className="mt-1 w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm font-normal text-brand-text"
                        />
                      </label>
                      <button
                        type="submit"
                        disabled={isSavingManualBid}
                        className="px-3 py-2 rounded-lg bg-emerald-700 text-white text-xs font-bold disabled:opacity-50"
                      >
                        <PhoneCall className="w-3.5 h-3.5 inline mr-1" />{isSavingManualBid ? 'Saving…' : 'Add Thai bid'}
                      </button>
                    </form>

                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 rounded-lg bg-cream p-3">
                      <div>
                        <p className="text-sm font-bold text-brand-text">
                          {highestBid !== null ? `₹${highestBid.toLocaleString('en-IN')}` : '—'}
                          {' · '}{currentSelectedOffering.bid_call_count}/3 વાર
                        </p>
                        <p className="text-xs text-brand-muted">ત્રીજી વાર પછી બોલી બંધ થશે અને વિજેતાની પુષ્ટિ પૂછાશે.</p>
                      </div>
                      <button
                        type="button"
                        disabled={!selectedBids.length}
                        onClick={handleCallBid}
                        className="px-4 py-2 rounded-lg bg-saffron text-white text-xs font-bold disabled:opacity-50"
                      >
                        {currentSelectedOffering.bid_call_count === 2
                          ? 'ત્રીજી વાર · બોલી બંધ કરો'
                          : `${highestBid === null ? '₹—' : `₹${highestBid.toLocaleString('en-IN')}`} · ${currentSelectedOffering.bid_call_count + 1} વાર બોલો`}
                      </button>
                    </div>
                  </>
                )}
                {currentSelectedOffering.status === 'COMPLETED' && currentSelectedOffering.winning_bidder_name && (
                  <div className="rounded-lg bg-emerald-50 border border-emerald-200 px-3 py-2 text-sm text-emerald-800">
                    વિજેતા: <strong>{currentSelectedOffering.winning_bidder_name}</strong>
                    {currentSelectedOffering.winning_bid_amount !== null && ` · ₹${currentSelectedOffering.winning_bid_amount.toLocaleString('en-IN')}`}
                  </div>
                )}
              </div>
            )}

            {/* Bid List — live updating */}
            <div className="flex-1 overflow-y-auto px-4 py-4 space-y-2.5">
              {selectedBids.length === 0 ? (
                <div className="text-center py-16 text-brand-muted">
                  <Gavel className="w-10 h-10 mx-auto mb-3 opacity-20" />
                  <p className="text-sm font-medium">કોઈ બોલી હજુ સુધી આવી નથી.</p>
                  <p className="text-xs mt-1">No bids placed yet. Waiting for users…</p>
                </div>
              ) : (
                selectedBids.map((bid, idx) => {
                  const isTop = bid.amount === highestBid;
                  return (
                    <div
                      key={bid.id}
                      className={`rounded-xl border px-4 py-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between transition-all ${
                        isTop
                          ? 'bg-gradient-to-r from-amber-50 to-orange-50 border-amber-300 shadow-sm'
                          : 'bg-white border-brand-border hover:border-slate-300'
                      }`}
                    >
                      {/* Left: bidder info */}
                      <div className="flex items-start gap-3">
                        {/* Rank badge */}
                        <div className={`w-7 h-7 rounded-full flex items-center justify-center text-[11px] font-black shrink-0 mt-0.5 ${
                          idx === 0 && isTop ? 'bg-amber-400 text-white' : 'bg-slate-100 text-slate-600'
                        }`}>
                          {idx === 0 && isTop ? '🥇' : `#${idx + 1}`}
                        </div>

                        <div>
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-bold text-brand-text flex items-center gap-1">
                              <User className="w-3.5 h-3.5 text-brand-muted" />
                              {bid.bidder_name}
                            </span>
                            {isTop && (
                              <span className="text-[10px] bg-amber-400 text-white px-2 py-0.5 rounded-full font-bold flex items-center gap-0.5">
                                <TrendingUp className="w-2.5 h-2.5" />TOP BID
                              </span>
                            )}
                            <BidStatusBadge status={bid.status} />
                          </div>
                          <div className="flex items-center gap-3 mt-0.5">
                            {bid.bidder_phone && (
                              <span className="text-xs text-brand-muted flex items-center gap-1">
                                <Phone className="w-3 h-3" />{bid.bidder_phone}
                              </span>
                            )}
                            <span className="text-xs text-brand-muted flex items-center gap-1">
                              <Clock className="w-3 h-3" />{timeAgo(bid.created_at)}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Right: amount + actions */}
                      <div className="flex w-full flex-wrap items-center justify-between gap-2 sm:w-auto sm:justify-end">
                        <div className={`text-lg font-black ${isTop ? 'text-saffron' : 'text-brand-text'}`}>
                          ₹{bid.amount.toLocaleString()}
                        </div>
                        {bid.status === 'PENDING' && (
                          <div className="flex items-center gap-1">
                            <button
                              onClick={() => handleBidAction(bid.id, selectedOffering.id, 'APPROVED')}
                              className="flex min-h-9 items-center gap-1 px-2 py-1 bg-emerald-500 hover:bg-emerald-600 text-white text-[10px] font-bold rounded-lg transition"
                            >
                              <Check className="w-3 h-3" />Approve
                            </button>
                            <button
                              onClick={() => handleBidAction(bid.id, selectedOffering.id, 'REJECTED')}
                              className="flex min-h-9 items-center gap-1 px-2 py-1 bg-rose-100 hover:bg-rose-200 text-rose-700 text-[10px] font-bold rounded-lg transition"
                            >
                              <X className="w-3 h-3" />Reject
                            </button>
                          </div>
                        )}
                        <button
                          type="button"
                          title="Delete bid permanently"
                          aria-label={`Delete bid from ${bid.bidder_name}`}
                          onClick={() => handleDeleteBid(bid.id, selectedOffering.id)}
                          className="flex min-h-9 shrink-0 items-center gap-1 px-2.5 py-1.5 text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 rounded-lg text-[11px] font-semibold transition"
                        >
                          <Trash2 className="w-3.5 h-3.5" />Delete bid
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Footer */}
            <div className="px-6 py-3 border-t border-brand-border bg-cream-surface shrink-0 flex items-center justify-between">
              <p className="text-[11px] text-brand-muted flex items-center gap-1.5">
                <Radio className={`w-3 h-3 ${isLiveConnected ? 'text-emerald-500 animate-pulse' : 'text-amber-500'}`} />
                {isLiveConnected
                  ? 'Auto-updating in real-time. New bids appear instantly.'
                  : `Realtime unavailable; history polls every 5 seconds.${lastSnapshotAt ? ` Last sync ${lastSnapshotAt}.` : ''}`}
              </p>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => void refreshSnapshotRef.current?.()}
                  className="px-3 py-1.5 rounded-lg border border-brand-border bg-white text-brand-text text-xs font-semibold hover:bg-cream transition"
                >
                  Refresh bids
                </button>
                <button
                  type="button"
                  onClick={() => setSelectedOffering(null)}
                  className="px-4 py-1.5 rounded-lg bg-slate-100 text-slate-700 text-xs font-semibold hover:bg-slate-200 transition"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ════════════════════════════════════════════════════════════════
          Modal: Create New Live Offering
      ════════════════════════════════════════════════════════════════ */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-cream-surface border border-brand-border rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-brand-border pb-3">
              <h2 className="text-base font-bold text-brand-text font-gujarati flex items-center gap-2">
                <HeartHandshake className="w-5 h-5 text-saffron" />
                <span>નવો ચઢાવો – Live Offering</span>
              </h2>
              <button onClick={() => setIsModalOpen(false)} className="p-1.5 rounded-lg hover:bg-cream text-brand-muted">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateOffering} className="space-y-3.5 text-sm">
              <div>
                <label className="block text-xs font-semibold text-brand-text mb-1">ચઢાવાનું નામ (ગુજ.) *</label>
                <input type="text" required
                  placeholder="શ્રી રામ મંદિર મહાઆરતી ચઢાવો"
                  value={formData.title_gu}
                  onChange={e => setFormData({ ...formData, title_gu: e.target.value })}
                  className="w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm focus:outline-none focus:border-saffron"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-brand-text mb-1">Title (English)</label>
                <input type="text"
                  placeholder="Shri Ram Temple Maha Aarti Offering"
                  value={formData.title_en}
                  onChange={e => setFormData({ ...formData, title_en: e.target.value })}
                  className="w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm focus:outline-none focus:border-saffron"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-brand-text mb-1">Base Amount (₹)</label>
                  <input type="number" min="0" placeholder="5100"
                    value={formData.amount}
                    onChange={e => setFormData({ ...formData, amount: e.target.value })}
                    className="w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm focus:outline-none focus:border-saffron"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-brand-text mb-1">Initial Status</label>
                  <select value={formData.status}
                    onChange={e => setFormData({ ...formData, status: e.target.value as Offering['status'] })}
                    className="w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm focus:outline-none focus:border-saffron">
                    <option value="ACTIVE">ACTIVE (Live bidding)</option>
                    <option value="APPROVED">APPROVED</option>
                    <option value="PENDING_APPROVAL">PENDING APPROVAL</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-brand-text mb-1">Location (ગુજ.)</label>
                <input type="text" placeholder="બોદલા મંદિર સંકુલ"
                  value={formData.location_gu}
                  onChange={e => setFormData({ ...formData, location_gu: e.target.value })}
                  className="w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm focus:outline-none focus:border-saffron"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-brand-text mb-1">Description</label>
                <textarea rows={2} placeholder="ચઢાવાની વિગતો…"
                  value={formData.description_gu}
                  onChange={e => setFormData({ ...formData, description_gu: e.target.value })}
                  className="w-full px-3 py-2 rounded-lg border border-brand-border bg-white text-sm focus:outline-none focus:border-saffron"
                />
              </div>
              <div className="flex items-center justify-end gap-3 pt-3 border-t border-brand-border">
                <button type="button" onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-lg border border-brand-border text-brand-text text-xs font-semibold hover:bg-cream transition">
                  Cancel
                </button>
                <button type="submit"
                  className="px-5 py-2 rounded-lg bg-saffron text-white text-xs font-semibold hover:bg-amber-600 transition shadow-sm flex items-center gap-1.5">
                  <Radio className="w-3.5 h-3.5" />
                  Publish Live
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Marquee animation style */}
      <style jsx global>{`
        @keyframes marquee {
          0%   { transform: translateX(0); }
          100% { transform: translateX(-50%); }
        }
        .animate-marquee {
          animation: marquee 20s linear infinite;
        }
      `}</style>
    </div>
  );
}
