import { Tabs } from 'expo-router';
import { View, Text, StyleSheet } from 'react-native';

// Custom Tab Icon Component
function TabIcon({ name, focused }: { name: string; focused: boolean }) {
  const icons: Record<string, string> = {
    feed: '📰',
    map: '🗺️',
    profile: '👤',
  };
  
  return (
    <View style={[styles.iconContainer, focused && styles.iconContainerFocused]}>
      <Text style={styles.icon}>{icons[name] || '📱'}</Text>
    </View>
  );
}

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#4F46E5',
        tabBarInactiveTintColor: '#9CA3AF',
        tabBarStyle: {
          backgroundColor: '#fff',
          borderTopWidth: 1,
          borderTopColor: '#E5E7EB',
          height: 80,
          paddingBottom: 20,
          paddingTop: 10,
        },
        tabBarLabelStyle: {
          fontSize: 12,
          fontWeight: '600',
        },
        headerStyle: {
          backgroundColor: '#4F46E5',
        },
        headerTintColor: '#fff',
        headerTitleStyle: {
          fontWeight: 'bold',
        },
      }}
    >
      <Tabs.Screen
        name="feed"
        options={{
          title: 'Feed',
          tabBarIcon: ({ focused }) => <TabIcon name="feed" focused={focused} />,
          headerTitle: 'PetBuddy',
        }}
      />
      <Tabs.Screen
        name="map"
        options={{
          title: 'Rescue Map',
          tabBarIcon: ({ focused }) => <TabIcon name="map" focused={focused} />,
          headerTitle: 'Rescue Map',
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profile',
          tabBarIcon: ({ focused }) => <TabIcon name="profile" focused={focused} />,
          headerTitle: 'My Profile',
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  iconContainer: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  iconContainerFocused: {
    backgroundColor: '#EEF2FF',
  },
  icon: {
    fontSize: 20,
  },
});
