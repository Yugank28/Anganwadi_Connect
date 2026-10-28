"use client";

import React, { useState, useEffect, useRef } from "react";
import { db, AnnouncementRecord, NoticeCategory } from "../../lib/firebase";
import {
  collection,
  query,
  orderBy,
  onSnapshot,
  Timestamp
} from "firebase/firestore";

type AppLanguage = "EN" | "MR" | "HI";

const REGISTERED_CHILDREN = [
  {
    id: "AW-042-01",
    name: "Aarav Sachin Gaikwad",
    guardian: "Sachin Gaikwad",
    mobile: "9890123456",
    regCode: "ACT-7842-AG",
    status: "Normal (सामान्य)",
    badgeColor: "bg-emerald-100 text-emerald-800 border-emerald-200"
  },
  {
    id: "AW-042-02",
    name: "Ananya Rahul Meshram",
    guardian: "Snehal Meshram",
    mobile: "9765432109",
    regCode: "ACT-9912-AM",
    status: "MAM (मध्यम कुपोषित)",
    badgeColor: "bg-amber-100 text-amber-800 border-amber-200"
  },
  {
    id: "AW-042-03",
    name: "Vihaan Pramod Shinde",
    guardian: "Pramod Shinde",
    mobile: "9422187654",
    regCode: "ACT-3301-VS",
    status: "Normal (सामान्य)",
    badgeColor: "bg-emerald-100 text-emerald-800 border-emerald-200"
  }
];

export default function ParentLiveDashboard() {
  const [selectedChildId, setSelectedChildId] = useState<string>("AW-042-01");
  const [filterMode, setFilterMode] = useState<"ALL" | "MY_CHILD" | "GENERAL">("ALL");
  const [language, setLanguage] = useState<AppLanguage>("MR"); // Default Marathi
  const [announcements, setAnnouncements] = useState<AnnouncementRecord[]>([]);
  const [isLiveConnected, setIsLiveConnected] = useState<boolean>(true);
  const [lastSyncTime, setLastSyncTime] = useState<string>("Just now");
  const [unreadCount, setUnreadCount] = useState<number>(0);

  // Live Push Banner Alert State
  const [liveBannerNotice, setLiveBannerNotice] = useState<AnnouncementRecord | null>(null);
  const [highlightedNoticeIds, setHighlightedNoticeIds] = useState<Set<string>>(new Set());

  // Ref to track first load vs subsequent live incoming messages
  const initialLoadCompleted = useRef<boolean>(false);
  const seenNoticeIds = useRef<Set<string>>(new Set());

  // Web Audio chime player for instantaneous incoming notification alerts
  const playAlertChime = () => {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = "sine";
      osc.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
      osc.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.15); // A5

      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.6);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start();
      osc.stop(ctx.currentTime + 0.6);

      // Trigger mobile device vibration if supported
      if (typeof window !== "undefined" && "vibrate" in navigator) {
        navigator.vibrate([150, 75, 150]);
      }
    } catch (e) {
      console.warn("Audio chime failed to play:", e);
    }
  };

  // Real-Time onSnapshot Firestore Listener (Phone B behavior)
  useEffect(() => {
    setIsLiveConnected(true);
    const q = query(collection(db, "announcements"), orderBy("timestamp", "desc"));

    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        setIsLiveConnected(true);
        const now = new Date();
        setLastSyncTime(now.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", second: "2-digit" }));

        const items: AnnouncementRecord[] = [];
        let newlyArrivedAlert: AnnouncementRecord | null = null;
        const newHighlights = new Set(highlightedNoticeIds);

        snapshot.docChanges().forEach((change) => {
          if (change.type === "added") {
            const data = change.doc.data() as AnnouncementRecord;
            const docId = change.doc.id;

            // Check if this document is genuinely new after initial load
            if (initialLoadCompleted.current && !seenNoticeIds.current.has(docId)) {
              newlyArrivedAlert = { id: docId, ...data };
              newHighlights.add(docId);
            }
            seenNoticeIds.current.add(docId);
          }
        });

        snapshot.forEach((docSnap) => {
          items.push({ id: docSnap.id, ...(docSnap.data() as Omit<AnnouncementRecord, "id">) });
        });

        setAnnouncements(items);
        setUnreadCount(items.length);

        // If a new record arrived in real-time, trigger slide-down banner and audio chime!
        if (newlyArrivedAlert) {
          setLiveBannerNotice(newlyArrivedAlert);
          setHighlightedNoticeIds(new Set(newHighlights));
          playAlertChime();

          // Auto dismiss banner after 8 seconds
          setTimeout(() => {
            setLiveBannerNotice(null);
          }, 8000);
        }

        // Flag that initial data load is complete
        initialLoadCompleted.current = true;
      },
      (error) => {
        console.error("Firestore onSnapshot error:", error);
        setIsLiveConnected(false);
      }
    );

    return () => unsubscribe();
  }, []);

  const currentChild = REGISTERED_CHILDREN.find((c) => c.id === selectedChildId)!;

  // Filter announcements for this parent
  const filteredAnnouncements = announcements.filter((item) => {
    if (filterMode === "MY_CHILD") {
      return item.targetChildId === selectedChildId;
    }
    if (filterMode === "GENERAL") {
      return item.targetChildId === "ALL";
    }
    // "ALL" shows broadcasts sent to everyone OR specifically to this child
    return item.targetChildId === "ALL" || item.targetChildId === selectedChildId;
  });

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-24 font-sans antialiased">
      {/* Real-Time Incoming Push Banner (Phone B Real-Time Trigger) */}
      {liveBannerNotice && (
        <div className="fixed top-2 inset-x-0 z-50 px-4 max-w-md mx-auto transition-all transform animate-bounce-short">
          <div className="bg-slate-900 text-white border-2 border-amber-400 rounded-2xl p-4 shadow-2xl flex items-start gap-3 ring-4 ring-amber-400/30">
            <div className="w-10 h-10 rounded-xl bg-amber-400 text-slate-950 flex items-center justify-center font-black text-xl shrink-0 shadow-md">
              🔔
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between gap-1 mb-0.5">
                <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-amber-400 text-slate-950">
                  Instant Cloud Push
                </span>
                <span className="text-[10px] text-slate-300">Just now</span>
              </div>
              <h4 className="text-xs font-bold text-white truncate">
                {liveBannerNotice.title}
              </h4>
              <p className="text-[11px] text-slate-200 line-clamp-2 mt-0.5">
                {liveBannerNotice.description}
              </p>
              <div className="mt-2 flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setLiveBannerNotice(null)}
                  className="px-2.5 py-1 bg-amber-400 text-slate-950 rounded-lg text-[10px] font-bold hover:bg-amber-300"
                >
                  View Notice
                </button>
                <button
                  type="button"
                  onClick={() => setLiveBannerNotice(null)}
                  className="text-[10px] text-slate-400 hover:text-white px-2 py-1"
                >
                  Dismiss
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Top Header Bar */}
      <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200 px-4 py-3 shadow-sm">
        <div className="max-w-md mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-blue-600 text-white flex items-center justify-center font-bold text-sm shadow-sm">
              पालक
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h1 className="text-sm font-bold text-slate-900 leading-tight">
                  Anganwadi Parent Connect
                </h1>
                <span className="px-1.5 py-0.5 text-[10px] font-bold bg-emerald-100 text-emerald-800 rounded-md">
                  PHONE B
                </span>
              </div>
              <div className="flex items-center gap-1.5 mt-0.5">
                <span className="relative flex h-2 w-2">
                  <span
                    className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${
                      isLiveConnected ? "bg-emerald-400" : "bg-rose-400"
                    }`}
                  ></span>
                  <span
                    className={`relative inline-flex rounded-full h-2 w-2 ${
                      isLiveConnected ? "bg-emerald-500" : "bg-rose-500"
                    }`}
                  ></span>
                </span>
                <span className="text-[10px] font-medium text-slate-500">
                  {isLiveConnected ? `Live Sync (${lastSyncTime})` : "Disconnected"}
                </span>
              </div>
            </div>
          </div>

          {/* Trilingual Selector */}
          <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl border border-slate-200 text-[11px] font-bold">
            {(["MR", "HI", "EN"] as AppLanguage[]).map((lang) => (
              <button
                key={lang}
                type="button"
                onClick={() => setLanguage(lang)}
                className={`px-2 py-0.5 rounded-lg transition-all ${
                  language === lang
                    ? "bg-white text-blue-700 shadow-xs border border-slate-200"
                    : "text-slate-500 hover:text-slate-800"
                }`}
              >
                {lang === "MR" ? "मराठी" : lang === "HI" ? "हिंदी" : "EN"}
              </button>
            ))}
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-md mx-auto px-4 pt-4 space-y-4">
        {/* Child Identity Card */}
        <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-sm relative overflow-hidden">
          <div className="absolute top-0 right-0 w-24 h-24 bg-blue-50 rounded-full blur-2xl -mr-6 -mt-6 pointer-events-none"></div>

          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-500 text-white flex items-center justify-center font-bold text-lg shadow-sm">
                {currentChild.name.charAt(0)}
              </div>
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider text-blue-700 bg-blue-50 px-2 py-0.5 rounded-md border border-blue-200">
                  {currentChild.id}
                </span>
                <h2 className="text-sm font-bold text-slate-900 mt-0.5">
                  {currentChild.name}
                </h2>
                <p className="text-[11px] text-slate-500">
                  पालक: {currentChild.guardian} • {currentChild.mobile}
                </p>
              </div>
            </div>

            <span className={`text-[10px] font-bold px-2 py-1 rounded-lg border ${currentChild.badgeColor}`}>
              {currentChild.status}
            </span>
          </div>

          {/* Child Switcher Dropdown */}
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
            <span className="text-[11px] font-medium text-slate-500">
              Switch Child Account:
            </span>
            <select
              value={selectedChildId}
              onChange={(e) => setSelectedChildId(e.target.value)}
              className="text-xs bg-slate-50 border border-slate-300 rounded-lg px-2 py-1 text-slate-800 font-semibold focus:outline-none focus:ring-1 focus:ring-blue-600"
            >
              {REGISTERED_CHILDREN.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.id})
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Real-time Notice Controls & Feed Tabs */}
        <div className="flex items-center justify-between pt-1">
          <div className="flex items-center gap-1.5">
            <span className="text-xs font-bold text-slate-800">
              {language === "MR" ? "सूचना व वाटप वेळापत्रक" : language === "HI" ? "सूचना एवं वितरण सारणी" : "Official Notices"}
            </span>
            <span className="px-2 py-0.5 text-[10px] font-bold bg-blue-600 text-white rounded-full">
              {filteredAnnouncements.length}
            </span>
          </div>

          <div className="flex items-center gap-1 bg-slate-200/80 p-0.5 rounded-xl text-[11px] font-medium">
            <button
              type="button"
              onClick={() => setFilterMode("ALL")}
              className={`px-2.5 py-1 rounded-lg transition-all ${
                filterMode === "ALL" ? "bg-white text-slate-900 font-bold shadow-xs" : "text-slate-600"
              }`}
            >
              सर्व (All)
            </button>
            <button
              type="button"
              onClick={() => setFilterMode("MY_CHILD")}
              className={`px-2.5 py-1 rounded-lg transition-all ${
                filterMode === "MY_CHILD" ? "bg-white text-slate-900 font-bold shadow-xs" : "text-slate-600"
              }`}
            >
              माझे बाळ
            </button>
            <button
              type="button"
              onClick={() => setFilterMode("GENERAL")}
              className={`px-2.5 py-1 rounded-lg transition-all ${
                filterMode === "GENERAL" ? "bg-white text-slate-900 font-bold shadow-xs" : "text-slate-600"
              }`}
            >
              केंद्राच्या
            </button>
          </div>
        </div>

        {/* Announcements Stream */}
        <div className="space-y-3">
          {filteredAnnouncements.length === 0 ? (
            <div className="bg-white border border-slate-200 rounded-2xl p-8 text-center shadow-sm space-y-2">
              <span className="text-3xl">📭</span>
              <p className="text-xs font-bold text-slate-700">No notices posted yet</p>
              <p className="text-[11px] text-slate-500 max-w-xs mx-auto">
                Open the Sevika Broadcast Console on Phone A to post a notice. It will appear here immediately without refreshing.
              </p>
              <a
                href="/admin"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-block mt-2 px-3 py-1.5 bg-blue-600 text-white text-xs font-semibold rounded-lg shadow-sm"
              >
                Go to Sevika Admin Console ↗
              </a>
            </div>
          ) : (
            filteredAnnouncements.map((item) => {
              const isNewlyArrived = item.id && highlightedNoticeIds.has(item.id);
              const categoryLabel =
                language === "MR" ? item.categoryLabelMr : language === "HI" ? item.categoryLabelHi : item.categoryLabelEn;

              return (
                <div
                  key={item.id || item.trackingCode}
                  className={`bg-white border rounded-2xl p-4 shadow-sm transition-all relative overflow-hidden ${
                    isNewlyArrived
                      ? "border-amber-400 ring-2 ring-amber-400 ring-offset-2 animate-pulse bg-amber-50/20"
                      : "border-slate-200 hover:border-slate-300"
                  }`}
                >
                  {/* Glowing Highlight Badge */}
                  {isNewlyArrived && (
                    <div className="absolute top-2 right-2 flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-400 text-slate-950 text-[10px] font-bold shadow-sm animate-pulse">
                      <span>✨</span>
                      <span>JUST RECEIVED</span>
                    </div>
                  )}

                  {/* Category Header */}
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-[11px] font-bold px-2 py-0.5 bg-blue-50 text-blue-700 border border-blue-200 rounded-md">
                      {categoryLabel}
                    </span>

                    {item.isUrgent && (
                      <span className="text-[10px] font-bold px-1.5 py-0.5 bg-rose-50 text-rose-600 border border-rose-200 rounded">
                        तातडीची सूचना (URGENT)
                      </span>
                    )}

                    <span className="text-[10px] text-slate-400 ml-auto font-mono">
                      {item.trackingCode}
                    </span>
                  </div>

                  {/* Title & Body */}
                  <h3 className="text-sm font-bold text-slate-900 leading-snug">
                    {item.title}
                  </h3>

                  <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                    {item.description}
                  </p>

                  {/* Schedule Details Card if available */}
                  {(item.distributionDate || item.pickupTiming || item.venue) && (
                    <div className="mt-3 bg-slate-50 border border-slate-200 rounded-xl p-2.5 text-[11px] space-y-1">
                      {item.distributionDate && (
                        <div className="flex items-center gap-1.5 text-slate-700">
                          <span className="font-semibold text-slate-900">📅 तारीख (Date):</span>
                          <span>{item.distributionDate}</span>
                        </div>
                      )}
                      {item.pickupTiming && (
                        <div className="flex items-center gap-1.5 text-slate-700">
                          <span className="font-semibold text-slate-900">⏰ वेळ (Time):</span>
                          <span>{item.pickupTiming}</span>
                        </div>
                      )}
                      {item.venue && (
                        <div className="flex items-center gap-1.5 text-slate-700">
                          <span className="font-semibold text-slate-900">📍 ठिकाण (Venue):</span>
                          <span>{item.venue}</span>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Sevika Signature & Timestamp */}
                  <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-[10px] text-slate-400">
                    <div className="flex items-center gap-1 text-slate-600 font-medium">
                      <span>✍️</span>
                      <span>{item.senderName}</span>
                    </div>
                    <span>{item.centerId}</span>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </main>
    </div>
  );
}
