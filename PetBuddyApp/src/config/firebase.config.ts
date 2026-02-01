import Constants from 'expo-constants';

/**
 * Firebase configuration loaded from Expo environment (app.config.js/app.json extra).
 * 
 * To set up:
 * 1. Create app.config.js (or add to app.json under "extra")
 * 2. Add your Firebase config values
 * 3. Values will be available at runtime via Constants.expoConfig.extra
 * 
 * Example app.config.js:
 * export default {
 *   expo: {
 *     extra: {
 *       firebaseApiKey: process.env.FIREBASE_API_KEY,
 *       firebaseAuthDomain: process.env.FIREBASE_AUTH_DOMAIN,
 *       // ...etc
 *     }
 *   }
 * }
 */

// Get extra config from Expo
const expoExtra = Constants.expoConfig?.extra || {};

// Firebase configuration
export const firebaseConfig = {
  apiKey: expoExtra.firebaseApiKey || "YOUR_API_KEY",
  authDomain: expoExtra.firebaseAuthDomain || "YOUR_PROJECT.firebaseapp.com",
  projectId: expoExtra.firebaseProjectId || "YOUR_PROJECT_ID",
  storageBucket: expoExtra.firebaseStorageBucket || "YOUR_PROJECT.appspot.com",
  messagingSenderId: expoExtra.firebaseMessagingSenderId || "YOUR_MESSAGING_SENDER_ID",
  appId: expoExtra.firebaseAppId || "YOUR_APP_ID",
  measurementId: expoExtra.firebaseMeasurementId || "YOUR_MEASUREMENT_ID"
};

/**
 * API Configuration
 * 
 * Platform-specific endpoints:
 * - Web: localhost works directly
 * - iOS Simulator: localhost works
 * - Android Emulator: use 10.0.2.2 (special alias for host)
 * - Physical device: use your machine's local IP (e.g., 192.168.x.x)
 */
import { Platform } from 'react-native';

// Determine the correct host based on platform
const getApiHost = (): string => {
  // Check for custom host from Expo config first
  if (expoExtra.apiHost) {
    return expoExtra.apiHost;
  }
  
  // Default hosts per platform
  if (Platform.OS === 'android') {
    // Android emulator uses 10.0.2.2 to reach host machine's localhost
    return '10.0.2.2';
  }
  
  // iOS simulator and web can use localhost
  return 'localhost';
};

const API_HOST = getApiHost();
const API_PORT = expoExtra.apiPort || '8080';

export const API_CONFIG = {
  // GraphQL endpoint - adjusts based on platform
  GRAPHQL_ENDPOINT: __DEV__ 
    ? `http://${API_HOST}:${API_PORT}/graphql` 
    : (expoExtra.productionApiUrl || "https://api.petbuddy.app/graphql"),
  
  // WebSocket endpoint for subscriptions
  GRAPHQL_WS_ENDPOINT: __DEV__
    ? `ws://${API_HOST}:${API_PORT}/graphql`
    : (expoExtra.productionWsUrl || "wss://api.petbuddy.app/graphql"),
    
  // For debugging - log the current endpoint
  get DEBUG_INFO() {
    return {
      platform: Platform.OS,
      host: API_HOST,
      graphql: this.GRAPHQL_ENDPOINT,
      isDev: __DEV__,
    };
  }
};

// App Constants
export const APP_CONFIG = {
  APP_NAME: "PetBuddy",
  RESCUE_RADIUS_METERS: 5000, // 5km default search radius
  LOCATION_UPDATE_INTERVAL: 300000, // 5 minutes in ms
  FEED_PAGE_SIZE: 20,
};

// Log config in development
if (__DEV__) {
  console.log('[Config] API Configuration:', API_CONFIG.DEBUG_INFO);
}
