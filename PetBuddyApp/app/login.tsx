import { useState } from 'react';
import { 
  View, 
  Text, 
  TouchableOpacity, 
  StyleSheet, 
  Image,
  Alert,
  Dimensions,
  Platform
} from 'react-native';
import { useRouter } from 'expo-router';
import { useMutation } from '@apollo/client';
import Animated, { 
  FadeIn, 
  FadeInDown, 
  FadeInUp 
} from 'react-native-reanimated';
import { 
  getAuth, 
  signInWithPopup, 
  GoogleAuthProvider 
} from 'firebase/auth';
import { useAuth } from '../src/hooks/useAuth';
import { REGISTER_USER } from '../src/graphql/operations';

const { width, height } = Dimensions.get('window');

export default function LoginScreen() {
  const router = useRouter();
  const { signInWithGoogle, user } = useAuth();
  const [loading, setLoading] = useState(false);

  const [registerUser] = useMutation(REGISTER_USER);

  const handleGoogleSignIn = async () => {
    setLoading(true);
    
    try {
      if (Platform.OS === 'web') {
        // Web: Use Firebase popup authentication
        const auth = getAuth();
        const provider = new GoogleAuthProvider();
        provider.addScope('email');
        provider.addScope('profile');
        
        const result = await signInWithPopup(auth, provider);
        
        if (result.user) {
          console.log('Signed in as:', result.user.email);
          // User is now signed in via Firebase, AuthProvider will detect this
          router.replace('/(tabs)/feed');
        }
      } else {
        // Native: Use @react-native-google-signin/google-signin (not implemented in demo)
        Alert.alert(
          'Demo Mode',
          'For the demo, tap "Continue as Guest" to explore the app.\n\nIn production, this would use Google Sign-In.',
          [{ text: 'OK' }]
        );
      }
    } catch (error: any) {
      console.error('Sign in failed:', error);
      const errorMessage = error.code === 'auth/popup-closed-by-user' 
        ? 'Sign-in popup was closed' 
        : error.message || 'Please try again';
      
      if (Platform.OS === 'web') {
        console.warn('Google Sign-In error:', errorMessage);
      } else {
        Alert.alert('Sign In Failed', errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = async () => {
    setLoading(true);
    
    try {
      // For demo purposes - skip to main app
      // In production, this would be actual Firebase auth
      router.replace('/(tabs)/feed');
    } catch (error: any) {
      Alert.alert('Error', error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      {/* Background Gradient */}
      <View style={styles.backgroundGradient} />
      
      {/* Pet Background Image Placeholder */}
      <Animated.View 
        entering={FadeIn.duration(1000)}
        style={styles.backgroundImageContainer}
      >
        <View style={styles.backgroundImagePlaceholder}>
          <Text style={styles.pawEmoji}>🐾</Text>
        </View>
      </Animated.View>

      {/* Content */}
      <View style={styles.content}>
        {/* Logo */}
        <Animated.View 
          entering={FadeInDown.delay(300).duration(800)}
          style={styles.logoContainer}
        >
          <View style={styles.logoCircle}>
            <Text style={styles.logoEmoji}>🐕</Text>
          </View>
          <Text style={styles.appName}>PetBuddy</Text>
          <Text style={styles.tagline}>Rescue. Connect. Care.</Text>
        </Animated.View>

        {/* Features */}
        <Animated.View 
          entering={FadeInUp.delay(600).duration(800)}
          style={styles.featuresContainer}
        >
          <View style={styles.featureRow}>
            <Text style={styles.featureIcon}>📍</Text>
            <Text style={styles.featureText}>Find rescue incidents nearby</Text>
          </View>
          <View style={styles.featureRow}>
            <Text style={styles.featureIcon}>📸</Text>
            <Text style={styles.featureText}>Share pet moments</Text>
          </View>
          <View style={styles.featureRow}>
            <Text style={styles.featureIcon}>🆘</Text>
            <Text style={styles.featureText}>Report & help animals in need</Text>
          </View>
        </Animated.View>

        {/* Buttons */}
        <Animated.View 
          entering={FadeInUp.delay(900).duration(800)}
          style={styles.buttonsContainer}
        >
          <TouchableOpacity 
            style={styles.googleButton}
            onPress={handleGoogleSignIn}
            disabled={loading}
          >
            <Text style={styles.googleIcon}>G</Text>
            <Text style={styles.googleButtonText}>
              {loading ? 'Signing in...' : 'Continue with Google'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={styles.demoButton}
            onPress={handleDemoLogin}
            disabled={loading}
          >
            <Text style={styles.demoButtonText}>Continue as Guest</Text>
          </TouchableOpacity>

          <Text style={styles.termsText}>
            By continuing, you agree to our Terms & Privacy Policy
          </Text>
        </Animated.View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#4F46E5',
  },
  backgroundGradient: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: '#4F46E5',
  },
  backgroundImageContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: height * 0.4,
    justifyContent: 'center',
    alignItems: 'center',
    opacity: 0.3,
  },
  backgroundImagePlaceholder: {
    width: 200,
    height: 200,
    justifyContent: 'center',
    alignItems: 'center',
  },
  pawEmoji: {
    fontSize: 120,
  },
  content: {
    flex: 1,
    justifyContent: 'flex-end',
    paddingHorizontal: 24,
    paddingBottom: 60,
  },
  logoContainer: {
    alignItems: 'center',
    marginBottom: 40,
  },
  logoCircle: {
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  logoEmoji: {
    fontSize: 50,
  },
  appName: {
    fontSize: 42,
    fontWeight: '800',
    color: '#fff',
    letterSpacing: 1,
  },
  tagline: {
    fontSize: 16,
    color: 'rgba(255, 255, 255, 0.8)',
    marginTop: 8,
    letterSpacing: 2,
  },
  featuresContainer: {
    marginBottom: 40,
  },
  featureRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  featureIcon: {
    fontSize: 20,
    marginRight: 12,
  },
  featureText: {
    fontSize: 16,
    color: 'rgba(255, 255, 255, 0.9)',
  },
  buttonsContainer: {
    alignItems: 'center',
  },
  googleButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#fff',
    paddingVertical: 16,
    paddingHorizontal: 32,
    borderRadius: 12,
    width: '100%',
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 5,
  },
  googleIcon: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#4285F4',
    marginRight: 12,
  },
  googleButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  demoButton: {
    paddingVertical: 16,
    paddingHorizontal: 32,
    width: '100%',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: 'rgba(255, 255, 255, 0.5)',
    borderRadius: 12,
    marginBottom: 24,
  },
  demoButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#fff',
  },
  termsText: {
    fontSize: 12,
    color: 'rgba(255, 255, 255, 0.6)',
    textAlign: 'center',
  },
});
