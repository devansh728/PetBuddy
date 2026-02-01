import { useState, useCallback } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  TouchableOpacity, 
  Image,
  RefreshControl,
  Dimensions,
  Alert
} from 'react-native';
import { useRouter } from 'expo-router';
import { FlashList } from '@shopify/flash-list';
import { useQuery, useMutation } from '@apollo/client';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { GET_FEED, TOGGLE_LIKE } from '../../src/graphql/operations';
import type { Post, FeedResult } from '../../src/types';

const { width } = Dimensions.get('window');

// Post Card Component
function PostCard({ post, index, onLike }: { post: Post; index: number; onLike: (postId: string) => void }) {
  const [isLiked, setIsLiked] = useState(post.isLiked);
  const [likeCount, setLikeCount] = useState(post.likeCount);

  const handleLike = () => {
    // Optimistic UI update
    setIsLiked(!isLiked);
    setLikeCount(prev => isLiked ? prev - 1 : prev + 1);
    onLike(post.postId);
  };

  return (
    <Animated.View 
      entering={FadeInDown.delay(index * 100).duration(400)}
      style={styles.postCard}
    >
      {/* User Header */}
      <View style={styles.postHeader}>
        <View style={styles.userAvatar}>
          <Text style={styles.avatarEmoji}>🐾</Text>
        </View>
        <View style={styles.userInfo}>
          <Text style={styles.userName}>Pet Lover</Text>
          <Text style={styles.postTime}>
            {post.createdAt ? new Date(post.createdAt).toLocaleDateString() : 'Just now'}
          </Text>
        </View>
      </View>

      {/* Post Image */}
      {post.mediaUrl ? (
        <Image 
          source={{ uri: post.mediaUrl }} 
          style={styles.postImage}
          resizeMode="cover"
        />
      ) : (
        <View style={styles.imagePlaceholder}>
          <Text style={styles.placeholderEmoji}>📷</Text>
          <Text style={styles.placeholderText}>No image</Text>
        </View>
      )}

      {/* Caption */}
      {post.caption && (
        <Text style={styles.caption}>{post.caption}</Text>
      )}

      {/* Actions */}
      <View style={styles.actions}>
        <TouchableOpacity 
          style={styles.actionButton} 
          onPress={handleLike}
        >
          <Text style={styles.actionIcon}>{isLiked ? '❤️' : '🤍'}</Text>
          <Text style={styles.actionText}>{likeCount}</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.actionButton}>
          <Text style={styles.actionIcon}>💬</Text>
          <Text style={styles.actionText}>{post.commentCount}</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.actionButton}>
          <Text style={styles.actionIcon}>📤</Text>
          <Text style={styles.actionText}>Share</Text>
        </TouchableOpacity>
      </View>
    </Animated.View>
  );
}

export default function FeedScreen() {
  const router = useRouter();
  const [page, setPage] = useState(0);
  const [refreshing, setRefreshing] = useState(false);

  const { data, loading, error, refetch, fetchMore } = useQuery<{ getFeed: FeedResult }>(GET_FEED, {
    variables: { page: 0, pageSize: 20 },
  });

  const [toggleLike] = useMutation(TOGGLE_LIKE);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    setPage(0);
    await refetch({ page: 0, pageSize: 20 });
    setRefreshing(false);
  }, [refetch]);

  const handleLoadMore = useCallback(() => {
    if (data?.getFeed?.hasMore && !loading) {
      const nextPage = page + 1;
      setPage(nextPage);
      fetchMore({
        variables: { page: nextPage, pageSize: 20 },
      });
    }
  }, [data, page, loading, fetchMore]);

  const handleLike = async (postId: string) => {
    try {
      await toggleLike({ 
        variables: { postId },
        // Optimistic response already handled in PostCard
      });
    } catch (err) {
      console.error('Like failed:', err);
    }
  };

  const handleCreatePost = () => {
    router.push('/create-post');
  };

  // Mock data for demo when backend is not connected
  const mockPosts: Post[] = [
    {
      postId: '1',
      userId: 'demo',
      caption: 'Just rescued this adorable puppy! 🐶 Looking for a forever home.',
      mediaUrl: '',
      likeCount: 42,
      commentCount: 8,
      isLiked: false,
      createdAt: new Date().toISOString(),
    },
    {
      postId: '2',
      userId: 'demo',
      caption: 'Morning walk with my best friend ❤️ #DogLife',
      mediaUrl: '',
      likeCount: 128,
      commentCount: 23,
      isLiked: true,
      createdAt: new Date(Date.now() - 3600000).toISOString(),
    },
    {
      postId: '3',
      userId: 'demo',
      caption: 'Adoption day! Welcome to the family, Whiskers 🐱',
      mediaUrl: '',
      likeCount: 256,
      commentCount: 45,
      isLiked: false,
      createdAt: new Date(Date.now() - 86400000).toISOString(),
    },
  ];

  const posts = data?.getFeed?.posts || mockPosts;

  if (error && !data) {
    console.log('Feed error (using mock data):', error.message);
  }

  return (
    <View style={styles.container}>
      <FlashList
        data={posts}
        renderItem={({ item, index }) => (
          <PostCard post={item} index={index} onLike={handleLike} />
        )}
        keyExtractor={(item) => item.postId}
        estimatedItemSize={400}
        refreshControl={
          <RefreshControl 
            refreshing={refreshing} 
            onRefresh={handleRefresh}
            colors={['#4F46E5']}
            tintColor="#4F46E5"
          />
        }
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.5}
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.headerText}>What's happening in the pet world?</Text>
          </View>
        }
        ListEmptyComponent={
          loading ? (
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>Loading posts...</Text>
            </View>
          ) : (
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyEmoji}>🐾</Text>
              <Text style={styles.emptyText}>No posts yet</Text>
              <Text style={styles.emptySubtext}>Be the first to share!</Text>
            </View>
          )
        }
      />

      {/* FAB - Create Post */}
      <TouchableOpacity 
        style={styles.fab} 
        onPress={handleCreatePost}
        activeOpacity={0.8}
      >
        <Text style={styles.fabIcon}>+</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F3F4F6',
  },
  header: {
    padding: 16,
    backgroundColor: '#fff',
    marginBottom: 8,
  },
  headerText: {
    fontSize: 16,
    color: '#6B7280',
  },
  postCard: {
    backgroundColor: '#fff',
    marginBottom: 8,
    paddingVertical: 16,
  },
  postHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginBottom: 12,
  },
  userAvatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#EEF2FF',
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarEmoji: {
    fontSize: 20,
  },
  userInfo: {
    marginLeft: 12,
  },
  userName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1F2937',
  },
  postTime: {
    fontSize: 12,
    color: '#9CA3AF',
    marginTop: 2,
  },
  postImage: {
    width: width,
    height: width,
    backgroundColor: '#E5E7EB',
  },
  imagePlaceholder: {
    width: width,
    height: 200,
    backgroundColor: '#F3F4F6',
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderEmoji: {
    fontSize: 40,
    marginBottom: 8,
  },
  placeholderText: {
    color: '#9CA3AF',
  },
  caption: {
    fontSize: 15,
    color: '#1F2937',
    paddingHorizontal: 16,
    paddingTop: 12,
    lineHeight: 22,
  },
  actions: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#F3F4F6',
    marginTop: 12,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: 24,
  },
  actionIcon: {
    fontSize: 18,
    marginRight: 6,
  },
  actionText: {
    fontSize: 14,
    color: '#6B7280',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: 100,
  },
  emptyEmoji: {
    fontSize: 60,
    marginBottom: 16,
  },
  emptyText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1F2937',
  },
  emptySubtext: {
    fontSize: 14,
    color: '#9CA3AF',
    marginTop: 4,
  },
  fab: {
    position: 'absolute',
    right: 20,
    bottom: 20,
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#4F46E5',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#4F46E5',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  fabIcon: {
    fontSize: 28,
    color: '#fff',
    fontWeight: '300',
  },
});
