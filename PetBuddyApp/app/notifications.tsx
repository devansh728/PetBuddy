import { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  RefreshControl,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useQuery, useMutation } from '@apollo/client';
import Animated, { FadeInDown } from 'react-native-reanimated';
import {
  GET_NOTIFICATIONS,
  MARK_NOTIFICATION_AS_READ,
} from '../src/graphql/operations';
import type { Notification, NotificationsResult } from '../src/types';

interface NotificationUI extends Notification {
  isLocallyRead?: boolean;
}

export default function NotificationsScreen() {
  const router = useRouter();
  const [refreshing, setRefreshing] = useState(false);

  const { data, loading, refetch } = useQuery<{ getNotifications: NotificationsResult }>(
    GET_NOTIFICATIONS,
    { variables: { page: 0, pageSize: 20 } }
  );

  const [markAsReadMutation] = useMutation(MARK_NOTIFICATION_AS_READ);

  const notifications = data?.getNotifications?.notifications || [];
  const unreadCount = data?.getNotifications?.unreadCount || 0;

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    refetch()?.then(() => setRefreshing(false));
  }, [refetch]);

  const handleMarkAsRead = async (notificationId: string, isRead: boolean) => {
    if (isRead) return; // Already read

    try {
      await markAsReadMutation({
        variables: { notificationId },
        update(cache) {
          const existingData = cache.readQuery<{ getNotifications: NotificationsResult }>({
            query: GET_NOTIFICATIONS,
            variables: { page: 0, pageSize: 20 },
          });

          if (existingData) {
            const updatedNotifications = existingData.getNotifications.notifications.map(
              (notif: Notification) =>
                notif.notificationId === notificationId ? { ...notif, isRead: true } : notif
            );

            cache.writeQuery({
              query: GET_NOTIFICATIONS,
              variables: { page: 0, pageSize: 20 },
              data: {
                getNotifications: {
                  ...existingData.getNotifications,
                  notifications: updatedNotifications,
                },
              },
            });
          }
        },
      });
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
      Alert.alert('Error', 'Failed to mark notification as read');
    }
  };

  const getNotificationIcon = (type: string) => {
    const iconMap: Record<string, string> = {
      POST_CREATED: '📝',
      LIKE_CREATED: '❤️',
      LIKE_DELETED: '🤍',
      COMMENT_CREATED: '💬',
      FOLLOW_CREATED: '👥',
    };
    return iconMap[type] || '🔔';
  };

  const getNotificationMessage = (notif: Notification) => {
    const messages: Record<string, (n: Notification) => string> = {
      POST_CREATED: (n) => `${n.actorUsername} created a post: ${n.postCaption?.substring(0, 30)}...`,
      LIKE_CREATED: (n) => `${n.actorUsername} liked your post`,
      LIKE_DELETED: (n) => `${n.actorUsername} unliked your post`,
      COMMENT_CREATED: (n) => `${n.actorUsername} commented on your post`,
      FOLLOW_CREATED: (n) => `${n.actorUsername} started following you`,
    };

    const messageFn = messages[notif.notificationType];
    return messageFn ? messageFn(notif) : `${notif.actorUsername} did something`;
  };

  const notificationItem = (notif: NotificationUI, index: number) => (
    <Animated.View
      key={notif.notificationId}
      entering={FadeInDown.delay(index * 50).duration(300)}
    >
      <TouchableOpacity
        style={[
          styles.notificationCard,
          !notif.isRead && styles.notificationCardUnread,
        ]}
        onPress={() => handleMarkAsRead(notif.notificationId, notif.isRead)}
      >
        <View style={styles.notificationIcon}>
          <Text style={styles.icon}>{getNotificationIcon(notif.notificationType)}</Text>
        </View>

        <View style={styles.notificationContent}>
          <Text style={styles.notificationMessage}>
            {getNotificationMessage(notif)}
          </Text>
          <Text style={styles.notificationTime}>
            {notif.createdAt
              ? new Date(notif.createdAt).toLocaleDateString()
              : 'Just now'}
          </Text>
        </View>

        {!notif.isRead && <View style={styles.unreadDot} />}
      </TouchableOpacity>
    </Animated.View>
  );

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#4F46E5" />
        <Text style={styles.loadingText}>Loading notifications...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Text style={styles.backButtonText}>‹ Back</Text>
        </TouchableOpacity>
        <View style={styles.headerTitleContainer}>
          <Text style={styles.headerTitle}>Notifications</Text>
          {unreadCount > 0 && (
            <View style={styles.headerBadge}>
              <Text style={styles.headerBadgeText}>{unreadCount}</Text>
            </View>
          )}
        </View>
        <View style={styles.headerSpacer} />
      </View>

      {/* Notifications List */}
      <ScrollView
        style={styles.content}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
        showsVerticalScrollIndicator={false}
      >
        {notifications.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyEmoji}>🔔</Text>
            <Text style={styles.emptyTitle}>No notifications yet</Text>
            <Text style={styles.emptyText}>
              When something happens, you'll see it here
            </Text>
          </View>
        ) : (
          <View style={styles.notificationsContainer}>
            {notifications.map((notif: Notification, index: number) =>
              notificationItem(notif, index)
            )}
          </View>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F3F4F6',
  },
  header: {
    backgroundColor: '#4F46E5',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 16,
    paddingTop: 20,
  },
  backButton: {
    paddingVertical: 8,
    paddingHorizontal: 4,
  },
  backButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '500',
  },
  headerTitleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flex: 1,
    justifyContent: 'center',
  },
  headerTitle: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  headerBadge: {
    backgroundColor: '#F87171',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    minWidth: 28,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerBadgeText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '700',
  },
  headerSpacer: {
    width: 50,
  },
  content: {
    flex: 1,
    paddingHorizontal: 12,
    paddingTop: 12,
  },
  notificationsContainer: {
    gap: 8,
    paddingBottom: 20,
  },
  notificationCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 12,
    marginBottom: 4,
    borderLeftWidth: 4,
    borderLeftColor: 'transparent',
  },
  notificationCardUnread: {
    backgroundColor: '#F0F9FF',
    borderLeftColor: '#4F46E5',
  },
  notificationIcon: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#F3F4F6',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  icon: {
    fontSize: 20,
  },
  notificationContent: {
    flex: 1,
  },
  notificationMessage: {
    fontSize: 14,
    fontWeight: '500',
    color: '#1F2937',
    lineHeight: 20,
  },
  notificationTime: {
    fontSize: 12,
    color: '#9CA3AF',
    marginTop: 4,
  },
  unreadDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#4F46E5',
    marginLeft: 8,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F3F4F6',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#6B7280',
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 60,
  },
  emptyEmoji: {
    fontSize: 48,
    marginBottom: 16,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1F2937',
    marginBottom: 8,
  },
  emptyText: {
    fontSize: 14,
    color: '#6B7280',
    textAlign: 'center',
  },
});
