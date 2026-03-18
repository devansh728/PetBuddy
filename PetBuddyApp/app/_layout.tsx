// app/_layout.tsx
import { useEffect } from 'react';
import { Stack, useRouter, useSegments } from 'expo-router';
import { ApolloProvider } from '@apollo/client';
import { StatusBar } from 'expo-status-bar';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { apolloClient } from '../src/lib/apollo-client';
import { AuthProvider, useAuth } from '../src/hooks/useAuth';

// ── Auth Guard with redirect logic ──────────────────
function AuthGate({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const router = useRouter();
  const segments = useSegments();

  useEffect(() => {
    if (loading) return; 
    const onAuthScreen = 
      segments.length === undefined ||          // index.tsx
      segments[0] === 'login';

    if (!user && !onAuthScreen) {
      router.replace('/');
    } else if (user && onAuthScreen) {
      router.replace('/(tabs)/feed');
    }
  }, [user, loading, segments]);

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#4F46E5" />
      </View>
    );
  }

  return <>{children}</>;
}

// ── Root Layout ─────────────────────────────────────
export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <ApolloProvider client={apolloClient}>
        <AuthProvider>
          <AuthGate>
            <Stack
              screenOptions={{
                headerStyle: { backgroundColor: '#4F46E5' },
                headerTintColor: '#fff',
                headerTitleStyle: { fontWeight: 'bold' },
              }}
            >
              <Stack.Screen
                name="index"
                options={{ headerShown: false }}
              />
              <Stack.Screen
                name="(tabs)"
                options={{ headerShown: false }}
              />
              <Stack.Screen
                name="create-post"
                options={{
                  title: 'New Post',
                  presentation: 'modal',
                }}
              />
              <Stack.Screen
                name="report-incident"
                options={{
                  title: 'Report Incident',
                  presentation: 'modal',
                }}
              />
            </Stack>
            <StatusBar style="auto" />
          </AuthGate>
        </AuthProvider>
      </ApolloProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F9FAFB',
  },
});

// import { Stack } from 'expo-router';
// import { ApolloProvider } from '@apollo/client';
// import { StatusBar } from 'expo-status-bar';
// import { View, ActivityIndicator, StyleSheet } from 'react-native';
// import { SafeAreaProvider } from 'react-native-safe-area-context';
// import { apolloClient } from '../src/lib/apollo-client';
// import { AuthProvider, useAuth } from '../src/hooks/useAuth';

// // Auth Guard Component
// function AuthGate({ children }: { children: React.ReactNode }) {
//   const { user, loading } = useAuth();

//   if (loading) {
//     return (
//       <View style={styles.loadingContainer}>
//         <ActivityIndicator size="large" color="#4F46E5" />
//       </View>
//     );
//   }

//   return <>{children}</>;
// }

// // Root Layout
// export default function RootLayout() {
//   return (
//     <SafeAreaProvider>
//       <ApolloProvider client={apolloClient}>
//         <AuthProvider>
//           <AuthGate>
//             <Stack
//               screenOptions={{
//                 headerStyle: { backgroundColor: '#4F46E5' },
//                 headerTintColor: '#fff',
//                 headerTitleStyle: { fontWeight: 'bold' },
//               }}
//             >
//               <Stack.Screen 
//                 name="index" 
//                 options={{ headerShown: false }} 
//               />
//               <Stack.Screen 
//                 name="login" 
//                 options={{ headerShown: false }} 
//               />
//               <Stack.Screen 
//                 name="(tabs)" 
//                 options={{ headerShown: false }} 
//               />
//               <Stack.Screen 
//                 name="create-post" 
//                 options={{ 
//                   title: 'New Post',
//                   presentation: 'modal' 
//                 }} 
//               />
//               <Stack.Screen 
//                 name="report-incident" 
//                 options={{ 
//                   title: 'Report Incident',
//                   presentation: 'modal' 
//                 }} 
//               />
//             </Stack>
//             <StatusBar style="auto" />
//           </AuthGate>
//         </AuthProvider>
//       </ApolloProvider>
//     </SafeAreaProvider>
//   );
// }

// const styles = StyleSheet.create({
//   loadingContainer: {
//     flex: 1,
//     justifyContent: 'center',
//     alignItems: 'center',
//     backgroundColor: '#F9FAFB',
//   },
// });
