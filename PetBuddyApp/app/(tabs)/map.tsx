import { useState, useEffect, useRef } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  TouchableOpacity, 
  Alert,
  Dimensions,
  Platform
} from 'react-native';
import { useRouter } from 'expo-router';
import * as Location from 'expo-location';
import { useQuery, useMutation } from '@apollo/client';
import { GET_NEARBY_INCIDENTS, UPDATE_LOCATION } from '../../src/graphql/operations';
import { APP_CONFIG } from '../../src/config/firebase.config';
import type { Incident, IncidentType } from '../../src/types';

// Platform-specific import - Metro will use .web.tsx for web and .tsx for native
import { MapView, Marker, Circle, PROVIDER_GOOGLE, isMapSupported } from '../../src/components/MapComponents';

const { width, height } = Dimensions.get('window');

// Incident type colors
const INCIDENT_COLORS: Record<string, string> = {
  ACCIDENT: '#EF4444',
  ABANDONED: '#F59E0B',
  INJURED: '#DC2626',
  LOST: '#3B82F6',
  ABUSE: '#7C3AED',
  UNKNOWN: '#6B7280',
};

const INCIDENT_EMOJIS: Record<string, string> = {
  ACCIDENT: '🚗',
  ABANDONED: '🏚️',
  INJURED: '🩹',
  LOST: '🔍',
  ABUSE: '⚠️',
  UNKNOWN: '❓',
};

export default function MapScreen() {
  const router = useRouter();
  const mapRef = useRef<any>(null);
  
  const [location, setLocation] = useState<Location.LocationObject | null>(null);
  const [isVolunteer, setIsVolunteer] = useState(false);
  const [selectedIncident, setSelectedIncident] = useState<Incident | null>(null);

  const [updateLocation] = useMutation(UPDATE_LOCATION);

  // Get current location
  useEffect(() => {
    (async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('Permission Denied', 'Location permission is required to show nearby incidents.');
        return;
      }

      const currentLocation = await Location.getCurrentPositionAsync({});
      setLocation(currentLocation);
    })();
  }, []);

  // Query nearby incidents
  const { data, loading, refetch } = useQuery<{ getNearbyIncidents: Incident[] }>(GET_NEARBY_INCIDENTS, {
    variables: {
      latitude: location?.coords.latitude || 0,
      longitude: location?.coords.longitude || 0,
      radiusMeters: APP_CONFIG.RESCUE_RADIUS_METERS,
    },
    skip: !location,
  });

  // Mock incidents for demo
  const mockIncidents: Incident[] = location ? [
    {
      incidentId: '1',
      reporterId: 'demo',
      latitude: location.coords.latitude + 0.01,
      longitude: location.coords.longitude + 0.005,
      type: 'INJURED' as IncidentType,
      status: 'OPEN' as any,
      description: 'Injured dog near the park',
      animalType: 'dog',
      volunteersNotified: 3,
      createdAt: new Date().toISOString(),
    },
    {
      incidentId: '2',
      reporterId: 'demo',
      latitude: location.coords.latitude - 0.008,
      longitude: location.coords.longitude + 0.01,
      type: 'ABANDONED' as IncidentType,
      status: 'OPEN' as any,
      description: 'Abandoned cat found',
      animalType: 'cat',
      volunteersNotified: 5,
      createdAt: new Date(Date.now() - 3600000).toISOString(),
    },
  ] : [];

  const incidents = data?.getNearbyIncidents || mockIncidents;

  const handleToggleVolunteer = async () => {
    if (!location) return;

    const newStatus = !isVolunteer;
    setIsVolunteer(newStatus);

    try {
      await updateLocation({
        variables: {
          latitude: location.coords.latitude,
          longitude: location.coords.longitude,
          isVolunteer: newStatus,
        },
      });
      
      Alert.alert(
        newStatus ? 'Volunteer Mode On' : 'Volunteer Mode Off',
        newStatus 
          ? 'You will receive alerts for nearby rescue incidents.' 
          : 'You will no longer receive rescue alerts.'
      );
    } catch (err) {
      console.error('Failed to update volunteer status:', err);
    }
  };

  const handleReportIncident = () => {
    router.push('/report-incident');
  };

  const handleCenterOnUser = () => {
    if (location && mapRef.current) {
      mapRef.current.animateToRegion({
        latitude: location.coords.latitude,
        longitude: location.coords.longitude,
        latitudeDelta: 0.02,
        longitudeDelta: 0.02,
      });
    }
  };

  if (!location) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingEmoji}>🗺️</Text>
        <Text style={styles.loadingText}>Getting your location...</Text>
      </View>
    );
  }

  // Web fallback - show a simple list instead of map
  if (!isMapSupported || !MapView) {
    return (
      <View style={styles.container}>
        <View style={styles.webFallback}>
          <Text style={styles.webFallbackEmoji}>🗺️</Text>
          <Text style={styles.webFallbackTitle}>Rescue Map</Text>
          <Text style={styles.webFallbackSubtitle}>
            Map view is available on mobile devices only
          </Text>
          <Text style={styles.webFallbackLocation}>
            📍 Your location: {location.coords.latitude.toFixed(4)}, {location.coords.longitude.toFixed(4)}
          </Text>
          
          <View style={styles.webIncidentList}>
            <Text style={styles.webIncidentListTitle}>
              Nearby Incidents ({incidents.length})
            </Text>
            {incidents.map((incident) => (
              <View key={incident.incidentId} style={styles.webIncidentItem}>
                <Text style={styles.webIncidentEmoji}>
                  {INCIDENT_EMOJIS[incident.type] || '❓'}
                </Text>
                <View style={styles.webIncidentInfo}>
                  <Text style={styles.webIncidentType}>{incident.type}</Text>
                  <Text style={styles.webIncidentDesc}>{incident.description}</Text>
                </View>
              </View>
            ))}
          </View>
          
          <TouchableOpacity 
            style={styles.reportFab}
            onPress={handleReportIncident}
          >
            <Text style={styles.reportFabIcon}>🆘</Text>
            <Text style={styles.reportFabText}>Report Incident</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <MapView
        ref={mapRef}
        style={styles.map}
        provider={Platform.OS === 'android' ? PROVIDER_GOOGLE : undefined}
        initialRegion={{
          latitude: location.coords.latitude,
          longitude: location.coords.longitude,
          latitudeDelta: 0.02,
          longitudeDelta: 0.02,
        }}
        showsUserLocation
        showsMyLocationButton={false}
      >
        {/* Search radius circle */}
        <Circle
          center={{
            latitude: location.coords.latitude,
            longitude: location.coords.longitude,
          }}
          radius={APP_CONFIG.RESCUE_RADIUS_METERS}
          strokeColor="rgba(79, 70, 229, 0.3)"
          fillColor="rgba(79, 70, 229, 0.1)"
          strokeWidth={2}
        />

        {/* Incident markers */}
        {incidents.map((incident) => (
          <Marker
            key={incident.incidentId}
            coordinate={{
              latitude: incident.latitude,
              longitude: incident.longitude,
            }}
            onPress={() => setSelectedIncident(incident)}
          >
            <View style={[
              styles.markerContainer, 
              { backgroundColor: INCIDENT_COLORS[incident.type] || INCIDENT_COLORS.UNKNOWN }
            ]}>
              <Text style={styles.markerEmoji}>
                {INCIDENT_EMOJIS[incident.type] || INCIDENT_EMOJIS.UNKNOWN}
              </Text>
            </View>
          </Marker>
        ))}
      </MapView>

      {/* Legend */}
      <View style={styles.legend}>
        <Text style={styles.legendTitle}>Incidents Nearby: {incidents.length}</Text>
        <View style={styles.legendItems}>
          {Object.entries(INCIDENT_EMOJIS).slice(0, 4).map(([type, emoji]) => (
            <View key={type} style={styles.legendItem}>
              <Text>{emoji}</Text>
              <Text style={styles.legendText}>{type}</Text>
            </View>
          ))}
        </View>
      </View>

      {/* Selected Incident Card */}
      {selectedIncident && (
        <View style={styles.incidentCard}>
          <View style={styles.incidentHeader}>
            <Text style={styles.incidentEmoji}>
              {INCIDENT_EMOJIS[selectedIncident.type]}
            </Text>
            <View>
              <Text style={styles.incidentType}>{selectedIncident.type}</Text>
              <Text style={styles.incidentAnimal}>{selectedIncident.animalType || 'Unknown animal'}</Text>
            </View>
            <TouchableOpacity 
              style={styles.closeButton}
              onPress={() => setSelectedIncident(null)}
            >
              <Text>✕</Text>
            </TouchableOpacity>
          </View>
          <Text style={styles.incidentDescription}>{selectedIncident.description}</Text>
          <Text style={styles.incidentMeta}>
            {selectedIncident.volunteersNotified} volunteers notified
          </Text>
          <TouchableOpacity style={styles.respondButton}>
            <Text style={styles.respondButtonText}>I Can Help</Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Action Buttons */}
      <View style={styles.actionButtons}>
        <TouchableOpacity 
          style={styles.actionButton}
          onPress={handleCenterOnUser}
        >
          <Text style={styles.actionButtonIcon}>📍</Text>
        </TouchableOpacity>
        
        <TouchableOpacity 
          style={[
            styles.actionButton, 
            isVolunteer && styles.actionButtonActive
          ]}
          onPress={handleToggleVolunteer}
        >
          <Text style={styles.actionButtonIcon}>{isVolunteer ? '🦸' : '🙋'}</Text>
        </TouchableOpacity>
      </View>

      {/* Report FAB */}
      <TouchableOpacity 
        style={styles.reportFab}
        onPress={handleReportIncident}
      >
        <Text style={styles.reportFabIcon}>🆘</Text>
        <Text style={styles.reportFabText}>Report</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  map: {
    width,
    height,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F3F4F6',
  },
  loadingEmoji: {
    fontSize: 60,
    marginBottom: 16,
  },
  loadingText: {
    fontSize: 16,
    color: '#6B7280',
  },
  legend: {
    position: 'absolute',
    top: 16,
    left: 16,
    right: 16,
    backgroundColor: '#fff',
    padding: 12,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  legendTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1F2937',
    marginBottom: 8,
  },
  legendItems: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  legendItem: {
    alignItems: 'center',
  },
  legendText: {
    fontSize: 10,
    color: '#6B7280',
    marginTop: 2,
  },
  markerContainer: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#fff',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 3,
  },
  markerEmoji: {
    fontSize: 16,
  },
  incidentCard: {
    position: 'absolute',
    bottom: 100,
    left: 16,
    right: 16,
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 8,
    elevation: 5,
  },
  incidentHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  incidentEmoji: {
    fontSize: 32,
    marginRight: 12,
  },
  incidentType: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1F2937',
  },
  incidentAnimal: {
    fontSize: 14,
    color: '#6B7280',
  },
  closeButton: {
    marginLeft: 'auto',
    padding: 8,
  },
  incidentDescription: {
    fontSize: 14,
    color: '#374151',
    marginBottom: 8,
  },
  incidentMeta: {
    fontSize: 12,
    color: '#9CA3AF',
    marginBottom: 12,
  },
  respondButton: {
    backgroundColor: '#4F46E5',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  respondButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  actionButtons: {
    position: 'absolute',
    right: 16,
    bottom: 180,
  },
  actionButton: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#fff',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  actionButtonActive: {
    backgroundColor: '#4F46E5',
  },
  actionButtonIcon: {
    fontSize: 20,
  },
  reportFab: {
    position: 'absolute',
    left: 16,
    bottom: 100,
    backgroundColor: '#DC2626',
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 30,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#DC2626',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 5,
  },
  reportFabIcon: {
    fontSize: 20,
    marginRight: 8,
  },
  reportFabText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
