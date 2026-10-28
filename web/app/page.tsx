"use client";

import React from "react";

export default function Home() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 p-6 flex flex-col justify-center items-center">
      <div className="w-16 h-16 rounded-2xl bg-blue-600 text-white flex items-center justify-center font-black text-2xl shadow-lg mb-4">
        AW
      </div>

      <h1 className="text-xl font-black text-slate-900 text-center leading-tight">
        Anganwadi Parent Connect
      </h1>
      <p className="text-xs text-slate-500 text-center mt-1 max-w-xs">
        Production-Ready Multi-Device Real-Time Sync System powered by Next.js 14, Capacitor, and Firebase Firestore
      </p>

      {/* Two Phone Selector Cards */}
      <div className="w-full mt-8 space-y-4">
        <a
          href="/admin"
          className="block w-full bg-white border border-slate-200 hover:border-blue-500 rounded-2xl p-5 shadow-sm hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="text-2xl p-2 bg-blue-50 rounded-xl group-hover:scale-110 transition-transform">
                📢
              </span>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-sm font-bold text-slate-900">Phone A: Sevika Admin</h2>
                  <span className="text-[10px] bg-blue-100 text-blue-700 font-bold px-1.5 py-0.5 rounded">
                    SENDER
                  </span>
                </div>
                <p className="text-[11px] text-slate-500 mt-0.5">
                  Post ration dates, vaccination alerts, and attendance updates to Cloud Firestore.
                </p>
              </div>
            </div>
            <span className="text-slate-400 group-hover:text-blue-600 text-sm">→</span>
          </div>
        </a>

        <a
          href="/parent"
          className="block w-full bg-white border border-slate-200 hover:border-emerald-500 rounded-2xl p-5 shadow-sm hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="text-2xl p-2 bg-emerald-50 rounded-xl group-hover:scale-110 transition-transform">
                👨‍👩‍👧
              </span>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-sm font-bold text-slate-900">Phone B: Parent Dashboard</h2>
                  <span className="text-[10px] bg-emerald-100 text-emerald-700 font-bold px-1.5 py-0.5 rounded">
                    RECEIVER
                  </span>
                </div>
                <p className="text-[11px] text-slate-500 mt-0.5">
                  Instant live push banners, onSnapshot cloud listener, and glowing notice feed.
                </p>
              </div>
            </div>
            <span className="text-slate-400 group-hover:text-emerald-600 text-sm">→</span>
          </div>
        </a>
      </div>

      <div className="mt-8 text-center text-[11px] text-slate-400">
        Open Phone A in one browser tab and Phone B in another to experience real-time sync across devices without refreshing.
      </div>
    </div>
  );
}
