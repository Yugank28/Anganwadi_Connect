"use client";

import React from "react";
import "./globals.css";

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="h-full antialiased">
      <head>
        <title>Anganwadi Parent Connect | Real-Time Sync</title>
        <meta
          name="viewport"
          content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover"
        />
        <meta name="theme-color" content="#2563EB" />
        <meta
          name="description"
          content="Anganwadi Parent Connect - Multi-device real-time sync with Firebase Firestore"
        />
      </head>
      <body className="min-h-full bg-slate-100 flex flex-col justify-start items-center selection:bg-blue-100 selection:text-blue-900">
        {/* Device Frame Wrapper for Mobile Experience */}
        <div className="w-full max-w-md min-h-screen bg-slate-50 border-x border-slate-200/80 shadow-lg flex flex-col relative overflow-x-hidden">
          {children}

          {/* Quick Dual-Device Switcher Navigation Bar at bottom */}
          <nav className="fixed bottom-0 inset-x-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200 py-2.5 px-4 max-w-md mx-auto shadow-md">
            <div className="flex items-center justify-around text-xs font-semibold">
              <a
                href="/admin"
                className="flex flex-col items-center gap-0.5 text-slate-600 hover:text-blue-600 active:scale-95 transition-all"
              >
                <span className="text-base">📢</span>
                <span className="text-[10px]">Phone A (Sevika)</span>
              </a>

              <div className="h-5 w-px bg-slate-200"></div>

              <a
                href="/parent"
                className="flex flex-col items-center gap-0.5 text-blue-600 active:scale-95 transition-all"
              >
                <span className="text-base">👨‍👩‍👧</span>
                <span className="text-[10px]">Phone B (Parent)</span>
              </a>
            </div>
          </nav>
        </div>
      </body>
    </html>
  );
}
