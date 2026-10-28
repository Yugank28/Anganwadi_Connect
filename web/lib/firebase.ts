import { initializeApp, getApps, getApp, FirebaseApp } from "firebase/app";
import { getFirestore, Firestore, Timestamp } from "firebase/firestore";

// Anganwadi Parent Connect Firebase Configuration
// Replace with your real Firebase Project credentials from Firebase Console:
// https://console.firebase.google.com/ -> Project Settings -> General -> Your apps -> Web app
const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY || "AIzaSyC6X4j2_V9nI7yJTp-lLrtt_yBCfXWP9tA",
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN || "anganwadiconnect-cb692.firebaseapp.com",
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID || "anganwadiconnect-cb692",
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET || "anganwadiconnect-cb692.firebasestorage.app",
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID || "1037048649491",
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID || "1:1037048649491:android:5322953d4f6d701a42ace6"
};

// Singleton App Initialization (prevents re-initialization error in Next.js hot reload)
const app: FirebaseApp = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);

// Initialize Cloud Firestore instance
export const db: Firestore = getFirestore(app);

// Data Model Interface for Anganwadi Real-Time Announcements & Alerts
export type NoticeCategory = "RATION" | "VACCINATION" | "ATTENDANCE" | "HEALTH_CHECKUP" | "GENERAL";
export type NoticePriority = "NORMAL" | "HIGH" | "URGENT";

export interface AnnouncementRecord {
  id?: string;
  trackingCode: string;
  category: NoticeCategory;
  categoryLabelMr: string;
  categoryLabelHi: string;
  categoryLabelEn: string;
  title: string;
  description: string;
  distributionDate?: string;
  pickupTiming?: string;
  venue?: string;
  targetChildId: string; // "ALL" or specific Child ID e.g., "AW-042-01"
  targetAudienceLabel: string;
  isUrgent: boolean;
  priority: NoticePriority;
  senderName: string;
  senderRole: string;
  centerId: string;
  centerName: string;
  timestamp: Timestamp | any;
  createdAtMillis?: number;
}
