import { useState } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  TouchableOpacity, 
  ScrollView,
  Switch,
  Alert
} from 'react-native';
import { useRouter } from 'expo-router';
import { useQuery } from '@apollo/client';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { GET_PROFILE, GET_PETS } from '../../src/graphql/operations';
import { useAuth } from '../../src/hooks/useAuth';
import type { UserProfile, Pet } from '../../src/types';

export default function ProfileScreen() {
  const router = useRouter();
  const { user, signOut } = useAuth();
  const [isVolunteer, setIsVolunteer] = useState(false);

  const { data: profileData, loading: profileLoading } = useQuery<{ getProfile: UserProfile }>(GET_PROFILE);
  const { data: petsData, loading: petsLoading } = useQuery<{ getPets: Pet[] }>(GET_PETS);

  // Mock data for demo
  const mockProfile: UserProfile = {
    userId: 'demo',
    fullName: user?.displayName || 'Pet Lover',
    email: user?.email || 'demo@petbuddy.app',
    isVerified: true,
    isVolunteer: isVolunteer,
  };

  const mockPets: Pet[] = [
    { petId: '1', name: 'Buddy', breed: 'Golden Retriever', ageMonths: 24 },
    { petId: '2', name: 'Whiskers', breed: 'Persian Cat', ageMonths: 18 },
  ];

  const profile = profileData?.getProfile || mockProfile;
  const pets = petsData?.getPets || mockPets;

  const handleSignOut = async () => {
    Alert.alert(
      'Sign Out',
      'Are you sure you want to sign out?',
      [
        { text: 'Cancel', style: 'cancel' },
        { 
          text: 'Sign Out', 
          style: 'destructive',
          onPress: async () => {
            try {
              await signOut();
              router.replace('/login');
            } catch (error) {
              console.error('Sign out failed:', error);
            }
          }
        },
      ]
    );
  };

  const handleAddPet = () => {
    Alert.alert('Coming Soon', 'Add pet feature will be available in the next update!');
  };

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      {/* Profile Header */}
      <Animated.View 
        entering={FadeInDown.duration(400)}
        style={styles.header}
      >
        <View style={styles.avatarContainer}>
          <View style={styles.avatar}>
            <Text style={styles.avatarEmoji}>🐾</Text>
          </View>
          {profile.isVerified && (
            <View style={styles.verifiedBadge}>
              <Text style={styles.verifiedText}>✓</Text>
            </View>
          )}
        </View>
        
        <Text style={styles.name}>{profile.fullName}</Text>
        <Text style={styles.email}>{profile.email}</Text>
        
        {profile.isVolunteer && (
          <View style={styles.volunteerBadge}>
            <Text style={styles.volunteerBadgeText}>🦸 Volunteer</Text>
          </View>
        )}
      </Animated.View>

      {/* Stats */}
      <Animated.View 
        entering={FadeInDown.delay(100).duration(400)}
        style={styles.statsContainer}
      >
        <View style={styles.statItem}>
          <Text style={styles.statValue}>{pets.length}</Text>
          <Text style={styles.statLabel}>Pets</Text>
        </View>
        <View style={styles.statDivider} />
        <View style={styles.statItem}>
          <Text style={styles.statValue}>12</Text>
          <Text style={styles.statLabel}>Posts</Text>
        </View>
        <View style={styles.statDivider} />
        <View style={styles.statItem}>
          <Text style={styles.statValue}>3</Text>
          <Text style={styles.statLabel}>Rescues</Text>
        </View>
      </Animated.View>

      {/* Settings Section */}
      <Animated.View 
        entering={FadeInDown.delay(200).duration(400)}
        style={styles.section}
      >
        <Text style={styles.sectionTitle}>Settings</Text>
        
        <View style={styles.settingItem}>
          <View style={styles.settingInfo}>
            <Text style={styles.settingIcon}>🦸</Text>
            <Text style={styles.settingLabel}>Volunteer Mode</Text>
          </View>
          <Switch
            value={isVolunteer}
            onValueChange={setIsVolunteer}
            trackColor={{ false: '#E5E7EB', true: '#A5B4FC' }}
            thumbColor={isVolunteer ? '#4F46E5' : '#9CA3AF'}
          />
        </View>
        
        <TouchableOpacity style={styles.settingItem}>
          <View style={styles.settingInfo}>
            <Text style={styles.settingIcon}>🔔</Text>
            <Text style={styles.settingLabel}>Notifications</Text>
          </View>
          <Text style={styles.settingArrow}>›</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.settingItem}>
          <View style={styles.settingInfo}>
            <Text style={styles.settingIcon}>🔒</Text>
            <Text style={styles.settingLabel}>Privacy</Text>
          </View>
          <Text style={styles.settingArrow}>›</Text>
        </TouchableOpacity>
      </Animated.View>

      {/* My Pets Section */}
      <Animated.View 
        entering={FadeInDown.delay(300).duration(400)}
        style={styles.section}
      >
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>My Pets</Text>
          <TouchableOpacity onPress={handleAddPet}>
            <Text style={styles.addButton}>+ Add Pet</Text>
          </TouchableOpacity>
        </View>
        
        {pets.map((pet) => (
          <View key={pet.petId} style={styles.petCard}>
            <View style={styles.petAvatar}>
              <Text style={styles.petEmoji}>
                {pet.name.toLowerCase().includes('cat') ? '🐱' : '🐕'}
              </Text>
            </View>
            <View style={styles.petInfo}>
              <Text style={styles.petName}>{pet.name}</Text>
              <Text style={styles.petBreed}>{pet.breed || 'Unknown breed'}</Text>
              {pet.ageMonths && (
                <Text style={styles.petAge}>
                  {pet.ageMonths >= 12 
                    ? `${Math.floor(pet.ageMonths / 12)} years old`
                    : `${pet.ageMonths} months old`}
                </Text>
              )}
            </View>
          </View>
        ))}
      </Animated.View>

      {/* Sign Out */}
      <Animated.View 
        entering={FadeInDown.delay(400).duration(400)}
        style={styles.section}
      >
        <TouchableOpacity 
          style={styles.signOutButton}
          onPress={handleSignOut}
        >
          <Text style={styles.signOutText}>Sign Out</Text>
        </TouchableOpacity>
      </Animated.View>

      <View style={styles.footer}>
        <Text style={styles.footerText}>PetBuddy v1.0.0</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F3F4F6',
  },
  header: {
    backgroundColor: '#4F46E5',
    paddingTop: 20,
    paddingBottom: 30,
    alignItems: 'center',
  },
  avatarContainer: {
    position: 'relative',
    marginBottom: 16,
  },
  avatar: {
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: '#fff',
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarEmoji: {
    fontSize: 50,
  },
  verifiedBadge: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#10B981',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 3,
    borderColor: '#4F46E5',
  },
  verifiedText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
  },
  name: {
    fontSize: 24,
    fontWeight: '700',
    color: '#fff',
  },
  email: {
    fontSize: 14,
    color: 'rgba(255, 255, 255, 0.8)',
    marginTop: 4,
  },
  volunteerBadge: {
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    marginTop: 12,
  },
  volunteerBadgeText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  statsContainer: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginTop: -20,
    borderRadius: 16,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 3,
  },
  statItem: {
    flex: 1,
    alignItems: 'center',
  },
  statValue: {
    fontSize: 24,
    fontWeight: '700',
    color: '#1F2937',
  },
  statLabel: {
    fontSize: 12,
    color: '#6B7280',
    marginTop: 4,
  },
  statDivider: {
    width: 1,
    backgroundColor: '#E5E7EB',
  },
  section: {
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginTop: 16,
    borderRadius: 16,
    padding: 16,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1F2937',
    marginBottom: 12,
  },
  addButton: {
    color: '#4F46E5',
    fontSize: 14,
    fontWeight: '600',
  },
  settingItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F3F4F6',
  },
  settingInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  settingIcon: {
    fontSize: 20,
    marginRight: 12,
  },
  settingLabel: {
    fontSize: 16,
    color: '#1F2937',
  },
  settingArrow: {
    fontSize: 20,
    color: '#9CA3AF',
  },
  petCard: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F3F4F6',
  },
  petAvatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#EEF2FF',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  petEmoji: {
    fontSize: 24,
  },
  petInfo: {
    flex: 1,
  },
  petName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1F2937',
  },
  petBreed: {
    fontSize: 14,
    color: '#6B7280',
    marginTop: 2,
  },
  petAge: {
    fontSize: 12,
    color: '#9CA3AF',
    marginTop: 2,
  },
  signOutButton: {
    backgroundColor: '#FEE2E2',
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
  },
  signOutText: {
    color: '#DC2626',
    fontSize: 16,
    fontWeight: '600',
  },
  footer: {
    alignItems: 'center',
    paddingVertical: 24,
  },
  footerText: {
    fontSize: 12,
    color: '#9CA3AF',
  },
});
