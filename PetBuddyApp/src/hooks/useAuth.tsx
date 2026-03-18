// src/hooks/useAuth.tsx
import { useState, useEffect, useCallback, createContext, useContext, ReactNode } from 'react';
import { initializeApp, getApps, FirebaseApp } from 'firebase/app';
import {
  initializeAuth,          // ← USE THIS instead of getAuth
  getReactNativePersistence, // ← ADD THIS
  signInWithCredential,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut as firebaseSignOut,
  onAuthStateChanged,
  GoogleAuthProvider,
  updateProfile,
  User,
  Auth,
} from 'firebase/auth';
import ReactNativeAsyncStorage from '@react-native-async-storage/async-storage';
import { useMutation } from '@apollo/client';
import { storage } from '../lib/storage';
import { firebaseConfig } from '../config/firebase.config';
import { REGISTER_USER } from '../graphql/operations';

// ── Firebase init (once) ────────────────────────────
let app: FirebaseApp;
let auth: Auth;

if (getApps().length === 0) {
  app = initializeApp(firebaseConfig);
  // ✅ Use initializeAuth with AsyncStorage persistence
  auth = initializeAuth(app, {
    persistence: getReactNativePersistence(ReactNativeAsyncStorage),
  });
} else {
  app = getApps()[0];
  // If already initialized, safe to use getAuth
  const { getAuth } = require('firebase/auth');
  auth = getAuth(app);
}

// ── Types ───────────────────────────────────────────
interface AuthContextType {
  user: User | null;
  authToken: string | null;
  loading: boolean;
  error: string | null;
  registerWithEmail: (email: string, password: string, fullName: string) => Promise<void>;
  loginWithEmail: (email: string, password: string) => Promise<void>;
  signInWithGoogle: (idToken: string, accessToken?: string) => Promise<void>;
  signOut: () => Promise<void>;
  refreshToken: () => Promise<string | null>;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

// ── Provider ────────────────────────────────────────
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [authToken, setAuthToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [registerUserMutation] = useMutation(REGISTER_USER);

  // ── Sync user to backend ──────────────────────────
  const syncUserToBackend = useCallback(
    async (firebaseUser: User) => {
      try {
        await registerUserMutation({
          variables: {
            input: {
              email: firebaseUser.email,
              fullName: firebaseUser.displayName || 'User',
            },
          },
        });
        console.log('[Auth] User synced to backend');
      } catch (err: any) {
        // User might already exist — that's fine
        console.log('[Auth] Backend sync note:', err.message);
      }
    },
    [registerUserMutation]
  );

  // ── Auth state listener ───────────────────────────
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      setUser(firebaseUser);

      if (firebaseUser) {
        try {
          const token = await firebaseUser.getIdToken();
          setAuthToken(token);
          await storage.setItem('authToken', token);
          await syncUserToBackend(firebaseUser);
        } catch (err) {
          console.error('[Auth] Failed to get ID token:', err);
          setError('Failed to authenticate');
        }
      } else {
        setAuthToken(null);
        await storage.deleteItem('authToken');
      }

      setLoading(false);
    });

    return () => unsubscribe();
  }, [syncUserToBackend]);

  // ── Token auto-refresh ────────────────────────────
  useEffect(() => {
    if (!user) return;
    const interval = setInterval(async () => {
      try {
        const token = await user.getIdToken(true);
        setAuthToken(token);
        await storage.setItem('authToken', token);
      } catch (err) {
        console.error('[Auth] Token refresh failed:', err);
      }
    }, 45 * 60 * 1000);
    return () => clearInterval(interval);
  }, [user]);

  // ── Register with email ───────────────────────────
  const registerWithEmail = useCallback(
    async (email: string, password: string, fullName: string) => {
      setLoading(true);
      setError(null);
      try {
        const credential = await createUserWithEmailAndPassword(auth, email, password);
        await updateProfile(credential.user, { displayName: fullName });
      } catch (err: any) {
        setError(humanizeFirebaseError(err.code));
        throw err;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  // ── Login with email ──────────────────────────────
  const loginWithEmail = useCallback(async (email: string, password: string) => {
    setLoading(true);
    setError(null);
    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (err: any) {
      setError(humanizeFirebaseError(err.code));
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  // ── Google sign in ────────────────────────────────
  const signInWithGoogle = useCallback(async (idToken: string, accessToken?: string) => {
    setLoading(true);
    setError(null);
    try {
      const credential = GoogleAuthProvider.credential(idToken, accessToken);
      await signInWithCredential(auth, credential);
    } catch (err: any) {
      setError(err.message || 'Google sign-in failed');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  // ── Sign out ──────────────────────────────────────
  const signOut = useCallback(async () => {
    setLoading(true);
    try {
      await firebaseSignOut(auth);
      await storage.deleteItem('authToken');
      setUser(null);
      setAuthToken(null);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // ── Refresh token ─────────────────────────────────
  const refreshToken = useCallback(async (): Promise<string | null> => {
    if (!user) return null;
    try {
      const token = await user.getIdToken(true);
      setAuthToken(token);
      await storage.setItem('authToken', token);
      return token;
    } catch (err) {
      return null;
    }
  }, [user]);

  const clearError = useCallback(() => setError(null), []);

  return (
    <AuthContext.Provider
      value={{
        user, authToken, loading, error,
        registerWithEmail, loginWithEmail, signInWithGoogle,
        signOut, refreshToken, clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}

export { auth };

function humanizeFirebaseError(code: string): string {
  const map: Record<string, string> = {
    'auth/email-already-in-use': 'This email is already registered.',
    'auth/invalid-email': 'Invalid email address.',
    'auth/weak-password': 'Password must be at least 6 characters.',
    'auth/user-not-found': 'No account found with this email.',
    'auth/wrong-password': 'Incorrect password.',
    'auth/too-many-requests': 'Too many attempts. Try again later.',
    'auth/invalid-credential': 'Invalid email or password.',
  };
  return map[code] || 'Something went wrong. Please try again.';
}