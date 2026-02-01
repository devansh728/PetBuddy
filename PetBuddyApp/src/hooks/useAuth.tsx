import { useState, useEffect, useCallback, createContext, useContext } from 'react';
import { initializeApp, getApps, FirebaseApp } from 'firebase/app';
import { 
  getAuth, 
  signInWithCredential,
  signOut as firebaseSignOut,
  onAuthStateChanged,
  GoogleAuthProvider,
  User,
  Auth
} from 'firebase/auth';
import { storage } from '../lib/storage';
import { firebaseConfig } from '../config/firebase.config';

// Initialize Firebase
let app: FirebaseApp;
let auth: Auth;

if (getApps().length === 0) {
  app = initializeApp(firebaseConfig);
  auth = getAuth(app);
} else {
  app = getApps()[0];
  auth = getAuth(app);
}

// Auth Context Types
interface AuthContextType {
  user: User | null;
  authToken: string | null;
  loading: boolean;
  error: string | null;
  signInWithGoogle: (idToken: string, accessToken?: string) => Promise<void>;
  signOut: () => Promise<void>;
  refreshToken: () => Promise<string | null>;
}

const AuthContext = createContext<AuthContextType | null>(null);

// Auth Provider Component
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [authToken, setAuthToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Listen to auth state changes
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      setUser(firebaseUser);
      
      if (firebaseUser) {
        try {
          const token = await firebaseUser.getIdToken();
          setAuthToken(token);
          await storage.setItem('authToken', token);
        } catch (err) {
          console.error('Failed to get ID token:', err);
          setError('Failed to authenticate');
        }
      } else {
        setAuthToken(null);
        await storage.deleteItem('authToken');
      }
      
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  // Auto-refresh token before expiry (tokens expire in 1 hour)
  useEffect(() => {
    if (!user) return;

    const refreshInterval = setInterval(async () => {
      try {
        const token = await user.getIdToken(true); // Force refresh
        setAuthToken(token);
        await storage.setItem('authToken', token);
      } catch (err) {
        console.error('Token refresh failed:', err);
      }
    }, 45 * 60 * 1000); // Refresh every 45 minutes

    return () => clearInterval(refreshInterval);
  }, [user]);

  // Sign in with Google credential
  const signInWithGoogle = useCallback(async (idToken: string, accessToken?: string) => {
    setLoading(true);
    setError(null);
    
    try {
      const credential = GoogleAuthProvider.credential(idToken, accessToken);
      await signInWithCredential(auth, credential);
    } catch (err: any) {
      console.error('Google sign-in failed:', err);
      setError(err.message || 'Sign in failed');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  // Sign out
  const signOut = useCallback(async () => {
    setLoading(true);
    
    try {
      await firebaseSignOut(auth);
      await storage.deleteItem('authToken');
      setUser(null);
      setAuthToken(null);
    } catch (err: any) {
      console.error('Sign out failed:', err);
      setError(err.message || 'Sign out failed');
    } finally {
      setLoading(false);
    }
  }, []);

  // Manual token refresh
  const refreshToken = useCallback(async (): Promise<string | null> => {
    if (!user) return null;
    
    try {
      const token = await user.getIdToken(true);
      setAuthToken(token);
      await storage.setItem('authToken', token);
      return token;
    } catch (err) {
      console.error('Manual token refresh failed:', err);
      return null;
    }
  }, [user]);

  return (
    <AuthContext.Provider 
      value={{ 
        user, 
        authToken, 
        loading, 
        error, 
        signInWithGoogle, 
        signOut,
        refreshToken 
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// Hook to use auth context
export function useAuth() {
  const context = useContext(AuthContext);
  
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  
  return context;
}

export { auth };
