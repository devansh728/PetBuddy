import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { useQuery } from '@apollo/client';
import { GET_UNREAD_NOTIFICATION_COUNT } from '../../src/graphql/operations';

export function NotificationBell() {
  const router = useRouter();
  const { data } = useQuery<{ getUnreadNotificationCount: number }>(
    GET_UNREAD_NOTIFICATION_COUNT,
    { pollInterval: 30000 } // Poll every 30 seconds
  );

  const unreadCount = data?.getUnreadNotificationCount || 0;

  return (
    <TouchableOpacity
      style={styles.bellContainer}
      onPress={() => router.push('/notifications')}
    >
      <Text style={styles.bell}>🔔</Text>
      {unreadCount > 0 && (
        <View style={styles.badge}>
          <Text style={styles.badgeText}>
            {unreadCount > 9 ? '9+' : unreadCount}
          </Text>
        </View>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  bellContainer: {
    position: 'relative',
    marginRight: 16,
    width: 40,
    height: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  bell: {
    fontSize: 20,
  },
  badge: {
    position: 'absolute',
    top: 2,
    right: 2,
    backgroundColor: '#DC2626',
    borderRadius: 10,
    minWidth: 20,
    height: 20,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#fff',
  },
  badgeText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: 'bold',
    textAlign: 'center',
    paddingHorizontal: 2,
  },
});
