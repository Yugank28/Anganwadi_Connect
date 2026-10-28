"use client";

import React, { useState, useEffect } from "react";
import { db, AnnouncementRecord, NoticeCategory, NoticePriority } from "../../lib/firebase";
import {
  collection,
  addDoc,
  serverTimestamp,
  query,
  orderBy,
  limit,
  onSnapshot,
  writeBatch,
  doc
} from "firebase/firestore";

const PRESET_CATEGORIES: {
  value: NoticeCategory;
  labelEn: string;
  labelMr: string;
  labelHi: string;
  icon: string;
  badgeBg: string;
  badgeText: string;
}[] = [
  {
    value: "RATION",
    labelEn: "Ration Distribution",
    labelMr: "अन्न धान्य वाटप (THR)",
    labelHi: "राशन वितरण",
    icon: "🌾",
    badgeBg: "bg-emerald-50 border-emerald-200",
    badgeText: "text-emerald-700"
  },
  {
    value: "VACCINATION",
    labelEn: "Vaccination Camp",
    labelMr: "लसीकरण शिबीर",
    labelHi: "टीकाकरण शिविर",
    icon: "💉",
    badgeBg: "bg-purple-50 border-purple-200",
    badgeText: "text-purple-700"
  },
  {
    value: "ATTENDANCE",
    labelEn: "Attendance Alert",
    labelMr: "दैनिक उपस्थिती सूचना",
    labelHi: "दैनिक उपस्थिति सूचना",
    icon: "📋",
    badgeBg: "bg-blue-50 border-blue-200",
    badgeText: "text-blue-700"
  },
  {
    value: "HEALTH_CHECKUP",
    labelEn: "Health Checkup",
    labelMr: "आरोग्य तपासणी",
    labelHi: "स्वास्थ्य जांच",
    icon: "🩺",
    badgeBg: "bg-amber-50 border-amber-200",
    badgeText: "text-amber-700"
  },
  {
    value: "GENERAL",
    labelEn: "General Notice",
    labelMr: "सामान्य सूचना",
    labelHi: "सामान्य सूचना",
    icon: "📢",
    badgeBg: "bg-slate-50 border-slate-200",
    badgeText: "text-slate-700"
  }
];

const PRESET_CHILDREN = [
  { id: "ALL", name: "All Registered Parents (सर्व पालक / सभी अभिभावक)" },
  { id: "AW-042-01", name: "AW-042-01: Aarav Sachin Gaikwad (Sachin Gaikwad)" },
  { id: "AW-042-02", name: "AW-042-02: Ananya Rahul Meshram (Snehal Meshram)" },
  { id: "AW-042-03", name: "AW-042-03: Vihaan Pramod Shinde (Pramod Shinde)" }
];

export default function AdminBroadcastConsole() {
  const [category, setCategory] = useState<NoticeCategory>("RATION");
  const [targetChildId, setTargetChildId] = useState<string>("ALL");
  const [title, setTitle] = useState<string>("Take-Home Ration (THR) Distribution on 24-Oct");
  const [description, setDescription] = useState<string>(
    "All registered parents must bring their ration card between 8:30 AM and 1:00 PM to receive THR packets, fortified oil, and eggs."
  );
  const [distributionDate, setDistributionDate] = useState<string>("24-Oct-2026");
  const [pickupTiming, setPickupTiming] = useState<string>("08:30 AM – 01:00 PM");
  const [venue, setVenue] = useState<string>("AW-Center-042 (Shivaji Nagar Community Hall)");
  const [priority, setPriority] = useState<NoticePriority>("HIGH");
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [lastDispatchedTime, setLastDispatchedTime] = useState<string | null>(null);
  const [recentNotices, setRecentNotices] = useState<AnnouncementRecord[]>([]);
  const [bulkSyncProgress, setBulkSyncProgress] = useState<string | null>(null);

  // Real-time listener for Sevika to see live cloud updates
  useEffect(() => {
    try {
      const q = query(collection(db, "announcements"), orderBy("timestamp", "desc"), limit(10));
      const unsubscribe = onSnapshot(
        q,
        (snapshot) => {
          const docs: AnnouncementRecord[] = [];
          snapshot.forEach((docSnap) => {
            docs.push({ id: docSnap.id, ...(docSnap.data() as Omit<AnnouncementRecord, "id">) });
          });
          setRecentNotices(docs);
        },
        (error) => {
          console.error("Firestore Admin feed error:", error);
        }
      );
      return () => unsubscribe();
    } catch (err) {
      console.error("Failed to connect Firestore listener:", err);
    }
  }, []);

  // Quick Preset Category Filler
  const handleCategoryChange = (cat: NoticeCategory) => {
    setCategory(cat);
    if (cat === "RATION") {
      setTitle("Take-Home Ration (THR) Distribution on 24-Oct");
      setDescription("Monthly supplementary nutrition ration packets (wheat flour, lentils, chana, fortified oil) available for pickup.");
      setDistributionDate("24-Oct-2026");
      setPickupTiming("08:30 AM – 01:00 PM");
      setVenue("AW-Center-042 (Shivaji Nagar Community Hall)");
      setPriority("HIGH");
    } else if (cat === "VACCINATION") {
      setTitle("DPT & Measles Booster Vaccination Session");
      setDescription("Dr. Shinde (PHC Medical Officer) will administer pending vaccine doses. Please bring your MCP card.");
      setDistributionDate("28-Oct-2026");
      setPickupTiming("09:00 AM – 12:30 PM");
      setVenue("Anganwadi Main Hall (Room 1)");
      setPriority("URGENT");
    } else if (cat === "ATTENDANCE") {
      setTitle("Daily Anganwadi Attendance Recorded");
      setDescription("Morning attendance has been finalized. Healthy mid-day khichdi served to all present children.");
      setDistributionDate(new Date().toLocaleDateString("en-GB"));
      setPickupTiming("10:00 AM");
      setVenue("AW-Center-042");
      setPriority("NORMAL");
    } else if (cat === "HEALTH_CHECKUP") {
      setTitle("Quarterly Pediatric Growth & Height-Weight Monitoring");
      setDescription("Special nutritional status screening (SAM/MAM checkup). Doctor will provide nutritional guidance.");
      setDistributionDate("30-Oct-2026");
      setPickupTiming("10:00 AM – 02:00 PM");
      setVenue("Shivaji Nagar PHC Center");
      setPriority("HIGH");
    } else {
      setTitle("Anganwadi Parents Committee Meeting");
      setDescription("Important discussion on children's pre-school activities and nutritional meals schedule.");
      setDistributionDate("02-Nov-2026");
      setPickupTiming("04:00 PM");
      setVenue("Anganwadi Premises");
      setPriority("NORMAL");
    }
  };

  // Submit Broadcast to Cloud Firestore
  const handlePostNotice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !description.trim()) {
      alert("Please provide both title and description.");
      return;
    }

    setIsSubmitting(true);
    try {
      const selectedCatConfig = PRESET_CATEGORIES.find((c) => c.value === category)!;
      const trackingCode = `AW-${category.substring(0, 3)}-${Math.floor(1000 + Math.random() * 9000)}`;

      const newRecord: Omit<AnnouncementRecord, "id"> = {
        trackingCode,
        category,
        categoryLabelEn: selectedCatConfig.labelEn,
        categoryLabelMr: selectedCatConfig.labelMr,
        categoryLabelHi: selectedCatConfig.labelHi,
        title: title.trim(),
        description: description.trim(),
        distributionDate: distributionDate.trim(),
        pickupTiming: pickupTiming.trim(),
        venue: venue.trim(),
        targetChildId,
        targetAudienceLabel:
          targetChildId === "ALL"
            ? "All Registered Parents (सर्व पालक)"
            : `Child: ${targetChildId}`,
        isUrgent: priority === "URGENT",
        priority,
        senderName: "Rekha Tai Patil (Anganwadi Sevika)",
        senderRole: "Anganwadi Worker / Teacher",
        centerId: "AW-Center-042",
        centerName: "AW-Center-042 (Shivaji Nagar)",
        timestamp: serverTimestamp(),
        createdAtMillis: Date.now()
      };

      const docRef = await addDoc(collection(db, "announcements"), newRecord);

      const dispatchedTime = new Date().toLocaleTimeString("en-IN", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
      });
      setLastDispatchedTime(dispatchedTime);
      console.log("Firestore insert successful with Document ID:", docRef.id);
    } catch (err: any) {
      console.error("Failed to post announcement to Firestore:", err);
      alert(`Firestore Write Error: ${err.message || "Check Firebase credentials in lib/firebase.ts"}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Fast Bulk Sync Button: One-click batch write to Firestore
  const handleBulkFastSync = async () => {
    setBulkSyncProgress("Preparing batch writes to Cloud Firestore...");
    try {
      const batch = writeBatch(db);
      const testAnnouncements = [
        {
          trackingCode: "AW-RAT-9921",
          category: "RATION" as NoticeCategory,
          categoryLabelEn: "Ration Distribution",
          categoryLabelMr: "अन्न धान्य वाटप (THR)",
          categoryLabelHi: "राशन वितरण",
          title: "Take-Home Ration (THR) Distribution on 24-Oct",
          description: "All registered parents must bring their ration card between 8:30 AM and 1:00 PM to receive THR packets, fortified oil, and eggs.",
          distributionDate: "24-Oct-2026",
          pickupTiming: "08:30 AM – 01:00 PM",
          venue: "AW-Center-042 (Shivaji Nagar Community Hall)",
          targetChildId: "ALL",
          targetAudienceLabel: "All Registered Parents",
          isUrgent: true,
          priority: "HIGH" as NoticePriority,
          senderName: "Rekha Tai Patil (Anganwadi Sevika)",
          senderRole: "Anganwadi Worker / Teacher",
          centerId: "AW-Center-042",
          centerName: "AW-Center-042 (Shivaji Nagar)",
          timestamp: serverTimestamp(),
          createdAtMillis: Date.now() - 3600000
        },
        {
          trackingCode: "AW-VAC-3312",
          category: "VACCINATION" as NoticeCategory,
          categoryLabelEn: "Vaccination Camp",
          categoryLabelMr: "लसीकरण शिबीर",
          categoryLabelHi: "टीकाकरण शिविर",
          title: "Quarterly Pediatric & Immunization Camp",
          description: "Dr. Shinde (PHC Medical Officer) will conduct physical exams and administer due DPT/Measles boosters on 28-Oct.",
          distributionDate: "28-Oct-2026",
          pickupTiming: "09:30 AM – 01:30 PM",
          venue: "PHC Health Sub-Center 042",
          targetChildId: "ALL",
          targetAudienceLabel: "All Registered Parents",
          isUrgent: true,
          priority: "URGENT" as NoticePriority,
          senderName: "Rekha Tai Patil (Anganwadi Sevika)",
          senderRole: "Anganwadi Worker / Teacher",
          centerId: "AW-Center-042",
          centerName: "AW-Center-042 (Shivaji Nagar)",
          timestamp: serverTimestamp(),
          createdAtMillis: Date.now() - 7200000
        },
        {
          trackingCode: "AW-ATT-8814",
          category: "ATTENDANCE" as NoticeCategory,
          categoryLabelEn: "Attendance Alert",
          categoryLabelMr: "दैनिक उपस्थिती सूचना",
          categoryLabelHi: "दैनिक उपस्थिति सूचना",
          title: "Daily Attendance: Aarav Sachin Gaikwad",
          description: "Aarav Sachin Gaikwad marked Present today at Anganwadi Centre. Received nutritious morning breakfast.",
          distributionDate: new Date().toLocaleDateString("en-GB"),
          pickupTiming: "09:15 AM",
          venue: "AW-Center-042",
          targetChildId: "AW-042-01",
          targetAudienceLabel: "Child: AW-042-01",
          isUrgent: false,
          priority: "NORMAL" as NoticePriority,
          senderName: "Rekha Tai Patil (Anganwadi Sevika)",
          senderRole: "Anganwadi Worker / Teacher",
          centerId: "AW-Center-042",
          centerName: "AW-Center-042 (Shivaji Nagar)",
          timestamp: serverTimestamp(),
          createdAtMillis: Date.now() - 10800000
        }
      ];

      for (const item of testAnnouncements) {
        const newDocRef = doc(collection(db, "announcements"));
        batch.set(newDocRef, item);
      }

      await batch.commit();
      setBulkSyncProgress("Batch committed! 3 records synchronized to Cloud Firestore.");
      setTimeout(() => setBulkSyncProgress(null), 5000);
    } catch (err: any) {
      console.error("Bulk sync error:", err);
      setBulkSyncProgress(`Sync Error: ${err.message || "Failed"}`);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-20">
      {/* Top App Bar */}
      <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-md border-b border-slate-200 px-4 py-3 shadow-sm">
        <div className="max-w-xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-blue-600 text-white flex items-center justify-center font-bold text-lg shadow-sm">
              AW
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h1 className="text-sm font-bold text-slate-900 leading-tight">
                  Sevika Broadcast Console
                </h1>
                <span className="px-1.5 py-0.5 text-[10px] font-bold bg-blue-100 text-blue-700 rounded-md">
                  PHONE A
                </span>
              </div>
              <p className="text-[11px] text-slate-500">
                Rekha Tai Patil • AW-Center-042 (Shivaji Nagar)
              </p>
            </div>
          </div>

          <a
            href="/parent"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-lg transition-colors border border-slate-300"
          >
            <span>Open Phone B ↗</span>
          </a>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-xl mx-auto px-4 pt-4 space-y-4">
        {/* Cloud Dispatched Live Badge */}
        {lastDispatchedTime && (
          <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center justify-between text-xs text-emerald-800 shadow-sm animate-fadeIn">
            <div className="flex items-center gap-2">
              <span className="relative flex h-2.5 w-2.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
              </span>
              <span className="font-semibold">
                Dispatched to Cloud Database at {lastDispatchedTime}
              </span>
            </div>
            <span className="text-[10px] uppercase font-bold tracking-wider bg-emerald-200/60 px-2 py-0.5 rounded text-emerald-900">
              Live Realtime
            </span>
          </div>
        )}

        {/* Bulk Sync Quick Action */}
        <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div>
            <h2 className="text-xs font-bold text-slate-900">
              One-Click Cloud Sync (Seed Real Records)
            </h2>
            <p className="text-[11px] text-slate-500">
              Batch-inserts Ration, Vaccination, and Attendance records into Cloud Firestore.
            </p>
          </div>
          <button
            type="button"
            onClick={handleBulkFastSync}
            className="w-full sm:w-auto px-4 py-2 bg-slate-900 hover:bg-slate-800 active:scale-95 text-white text-xs font-semibold rounded-xl transition-all shadow-sm flex items-center justify-center gap-1.5"
          >
            <span>⚡ Fast Cloud Sync</span>
          </button>
        </div>

        {bulkSyncProgress && (
          <div className="text-xs text-blue-700 bg-blue-50 border border-blue-200 rounded-xl p-2.5 text-center font-medium">
            {bulkSyncProgress}
          </div>
        )}

        {/* Broadcast Form */}
        <div className="bg-white border border-slate-200 rounded-2xl p-4 sm:p-5 shadow-sm space-y-4">
          <div className="border-b border-slate-100 pb-3 flex items-center justify-between">
            <h2 className="text-sm font-bold text-slate-800 flex items-center gap-2">
              <span>📢</span>
              <span>Compose Real-Time Parent Broadcast</span>
            </h2>
            <span className="text-[11px] text-slate-400 font-medium">Firestore Powered</span>
          </div>

          <form onSubmit={handlePostNotice} className="space-y-4">
            {/* Category Pills */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Notice Category (प्रवर्ग निवडा):
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {PRESET_CATEGORIES.map((cat) => {
                  const isSelected = category === cat.value;
                  return (
                    <button
                      key={cat.value}
                      type="button"
                      onClick={() => handleCategoryChange(cat.value)}
                      className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-medium border text-left transition-all ${
                        isSelected
                          ? "bg-blue-50 border-blue-600 text-blue-700 shadow-sm ring-1 ring-blue-600"
                          : "bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100"
                      }`}
                    >
                      <span className="text-base">{cat.icon}</span>
                      <div className="truncate">
                        <div className="font-semibold">{cat.labelEn}</div>
                        <div className="text-[10px] text-slate-500 truncate">{cat.labelMr}</div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Target Audience Dropdown */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Target Parent / Child Audience:
              </label>
              <select
                value={targetChildId}
                onChange={(e) => setTargetChildId(e.target.value)}
                className="w-full text-xs px-3 py-2.5 rounded-xl border border-slate-300 bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-600"
              >
                {PRESET_CHILDREN.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Title */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Notice Title (शीर्षक):
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g. Take-Home Ration Distribution on 24-Oct"
                className="w-full text-xs px-3 py-2.5 rounded-xl border border-slate-300 bg-white text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-600"
                required
              />
            </div>

            {/* Description */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Detailed Message / Instructions (सविस्तर माहिती):
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
                placeholder="Provide details about timings, items, documents to bring..."
                className="w-full text-xs px-3 py-2 rounded-xl border border-slate-300 bg-white text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-600"
                required
              />
            </div>

            {/* Event Specific Parameters */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 bg-slate-50 p-3 rounded-xl border border-slate-200">
              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  Date (तारीख):
                </label>
                <input
                  type="text"
                  value={distributionDate}
                  onChange={(e) => setDistributionDate(e.target.value)}
                  className="w-full text-xs px-2.5 py-1.5 rounded-lg border border-slate-300 bg-white"
                />
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  Pickup Timing (वेळ):
                </label>
                <input
                  type="text"
                  value={pickupTiming}
                  onChange={(e) => setPickupTiming(e.target.value)}
                  className="w-full text-xs px-2.5 py-1.5 rounded-lg border border-slate-300 bg-white"
                />
              </div>

              <div className="sm:col-span-2">
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  Venue / Location (ठिकाण):
                </label>
                <input
                  type="text"
                  value={venue}
                  onChange={(e) => setVenue(e.target.value)}
                  className="w-full text-xs px-2.5 py-1.5 rounded-lg border border-slate-300 bg-white"
                />
              </div>
            </div>

            {/* Priority Selector */}
            <div className="flex items-center justify-between pt-1">
              <span className="text-xs font-semibold text-slate-700">Priority Level:</span>
              <div className="flex items-center gap-1.5">
                {(["NORMAL", "HIGH", "URGENT"] as NoticePriority[]).map((p) => {
                  const isPSelected = priority === p;
                  return (
                    <button
                      key={p}
                      type="button"
                      onClick={() => setPriority(p)}
                      className={`text-[11px] px-3 py-1 rounded-lg font-semibold transition-all ${
                        isPSelected
                          ? p === "URGENT"
                            ? "bg-rose-600 text-white shadow-sm"
                            : p === "HIGH"
                            ? "bg-amber-600 text-white shadow-sm"
                            : "bg-blue-600 text-white shadow-sm"
                          : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                      }`}
                    >
                      {p}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Submit Action Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3 bg-blue-600 hover:bg-blue-700 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {isSubmitting ? (
                <span>Writing to Cloud Firestore...</span>
              ) : (
                <>
                  <span>🚀 Disptach Live Alert to All Parent Devices</span>
                </>
              )}
            </button>
          </form>
        </div>

        {/* Recent Broadcasts List from Cloud */}
        <div className="space-y-2">
          <div className="flex items-center justify-between px-1">
            <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
              Live Cloud Feed ({recentNotices.length} notices in Firestore)
            </h3>
            <span className="text-[10px] text-emerald-600 font-semibold flex items-center gap-1">
              <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
              onSnapshot Active
            </span>
          </div>

          <div className="space-y-2.5">
            {recentNotices.map((item) => (
              <div
                key={item.id}
                className="bg-white border border-slate-200 rounded-xl p-3 shadow-sm hover:border-slate-300 transition-colors space-y-1.5"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs px-2 py-0.5 bg-slate-100 text-slate-700 font-semibold rounded-md border border-slate-200">
                      {item.categoryLabelEn}
                    </span>
                    <span className="text-[10px] font-mono text-slate-400">
                      {item.trackingCode}
                    </span>
                  </div>
                  {item.isUrgent && (
                    <span className="text-[10px] font-bold px-1.5 py-0.5 bg-rose-50 border border-rose-200 text-rose-600 rounded">
                      URGENT
                    </span>
                  )}
                </div>

                <h4 className="text-xs font-bold text-slate-900 leading-snug">
                  {item.title}
                </h4>

                <p className="text-[11px] text-slate-600 line-clamp-2">
                  {item.description}
                </p>

                <div className="flex items-center justify-between pt-1 border-t border-slate-100 text-[10px] text-slate-400">
                  <span>Audience: {item.targetAudienceLabel}</span>
                  <span>{item.distributionDate || "Live broadcast"}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}
