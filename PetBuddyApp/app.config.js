import 'dotenv/config';

export default {
  expo: {
    name: "PetBuddyApp",
    slug: "PetBuddyApp",
    version: "1.0.0",
    orientation: "portrait",
    icon: "./assets/icon.png",
    scheme: "petbuddy",
    userInterfaceStyle: "automatic",
    newArchEnabled: true,
    splash: {
      image: "./assets/splash-icon.png",
      resizeMode: "contain",
      backgroundColor: "#4F46E5"
    },
    ios: {
      supportsTablet: true,
      bundleIdentifier: "com.petbuddy.app",
      infoPlist: {
        NSLocationWhenInUseUsageDescription: "PetBuddy needs your location to find nearby rescue incidents and volunteers.",
        NSCameraUsageDescription: "PetBuddy needs camera access to take photos of pets and incidents.",
        NSPhotoLibraryUsageDescription: "PetBuddy needs photo library access to upload pet photos."
      }
    },
    android: {
      adaptiveIcon: {
        foregroundImage: "./assets/adaptive-icon.png",
        backgroundColor: "#4F46E5"
      },
      package: "com.petbuddy.app",
      permissions: [
        "ACCESS_COARSE_LOCATION",
        "ACCESS_FINE_LOCATION",
        "CAMERA"
      ],
      config: {
        googleMaps: {
          apiKey: process.env.GOOGLE_MAPS_API_KEY || "YOUR_GOOGLE_MAPS_API_KEY"
        }
      }
    },
    web: {
      bundler: "metro",
      output: "static",
      favicon: "./assets/favicon.png"
    },
    plugins: [
      "expo-router",
      "expo-secure-store",
      [
        "expo-location",
        {
          locationAlwaysAndWhenInUsePermission: "Allow PetBuddy to use your location."
        }
      ],
      [
        "expo-image-picker",
        {
          photosPermission: "Allow PetBuddy to access your photos.",
          cameraPermission: "Allow PetBuddy to access your camera."
        }
      ]
    ],
    experiments: {
      typedRoutes: true
    },
    // Environment variables accessible via Constants.expoConfig.extra
    extra: {
      // Firebase Config (from .env)
      firebaseApiKey: process.env.FIREBASE_API_KEY,
      firebaseAuthDomain: process.env.FIREBASE_AUTH_DOMAIN,
      firebaseProjectId: process.env.FIREBASE_PROJECT_ID,
      firebaseStorageBucket: process.env.FIREBASE_STORAGE_BUCKET,
      firebaseMessagingSenderId: process.env.FIREBASE_MESSAGING_SENDER_ID,
      firebaseAppId: process.env.FIREBASE_APP_ID,
      firebaseMeasurementId: process.env.FIREBASE_MEASUREMENT_ID,
      
      // API Config (from .env)
      apiHost: process.env.API_HOST, // Optional: override default host
      apiPort: process.env.API_PORT || "8080",
      productionApiUrl: process.env.PRODUCTION_API_URL,
      productionWsUrl: process.env.PRODUCTION_WS_URL,
      
      // EAS Build config
      eas: {
        projectId: process.env.EAS_PROJECT_ID
      }
    }
  }
};
