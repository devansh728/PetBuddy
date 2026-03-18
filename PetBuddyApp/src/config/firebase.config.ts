// src/config/firebase.config.ts
import { Platform } from 'react-native';
import Constants from 'expo-constants';

const expoExtra = Constants.expoConfig?.extra || {};

// ── Firebase ────────────────────────────────────────
export const firebaseConfig = {
  apiKey: expoExtra.firebaseApiKey || "YOUR_API_KEY",
  authDomain: expoExtra.firebaseAuthDomain || "YOUR_PROJECT.firebaseapp.com",
  projectId: expoExtra.firebaseProjectId || "YOUR_PROJECT_ID",
  storageBucket: expoExtra.firebaseStorageBucket || "YOUR_PROJECT.appspot.com",
  messagingSenderId: expoExtra.firebaseMessagingSenderId || "YOUR_MESSAGING_SENDER_ID",
  appId: expoExtra.firebaseAppId || "YOUR_APP_ID",
};

// ── API Host Detection ──────────────────────────────
const getApiHost = (): string => {
  // 1. Explicit override from app.config.js — highest priority
  if (expoExtra.apiHost) {
    return expoExtra.apiHost;
  }

  // 2. Expo Go exposes the dev server host (your laptop's IP!)
  //    This is the BEST automatic method for physical devices
  const debuggerHost = Constants.expoConfig?.hostUri        // SDK 49+
    ?? Constants.manifest?.debuggerHost                      // older SDK
    ?? Constants.manifest2?.extra?.expoGo?.debuggerHost;     // SDK 48

  if (debuggerHost) {
    // debuggerHost = "192.168.1.42:8081" — strip the port
    const host = debuggerHost.split(':')[0];
    console.log('[Config] Auto-detected host from Expo:', host);
    return host;
  }

  // 3. Platform fallbacks (emulators only)
  if (Platform.OS === 'android') {
    return '10.0.2.2';  // Android emulator only
  }

  return 'localhost'; // iOS simulator / web
};

const API_HOST = getApiHost();
const API_PORT = expoExtra.apiPort || '8080';

export const API_CONFIG = {
  GRAPHQL_ENDPOINT: __DEV__
    ? `http://${API_HOST}:${API_PORT}/graphql`
    : (expoExtra.productionApiUrl || "https://api.petbuddy.app/graphql"),

  GRAPHQL_WS_ENDPOINT: __DEV__
    ? `ws://${API_HOST}:${API_PORT}/graphql`
    : (expoExtra.productionWsUrl || "wss://api.petbuddy.app/graphql"),

  get DEBUG_INFO() {
    return {
      platform: Platform.OS,
      host: API_HOST,
      port: API_PORT,
      graphql: this.GRAPHQL_ENDPOINT,
      isDev: __DEV__,
    };
  },
};

export const APP_CONFIG = {
  APP_NAME: "PetBuddy",
  RESCUE_RADIUS_METERS: 5000,
  LOCATION_UPDATE_INTERVAL: 300000,
  FEED_PAGE_SIZE: 20,
};

if (__DEV__) {
  console.log('[Config] API:', API_CONFIG.DEBUG_INFO);
}