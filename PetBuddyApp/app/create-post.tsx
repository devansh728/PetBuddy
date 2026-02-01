import { useState } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  TextInput,
  TouchableOpacity, 
  Image,
  ScrollView,
  Alert,
  ActivityIndicator
} from 'react-native';
import { useRouter } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { useMutation, useLazyQuery } from '@apollo/client';
import { CREATE_POST, GET_UPLOAD_URL, GET_FEED } from '../src/graphql/operations';

export default function CreatePostScreen() {
  const router = useRouter();
  
  const [caption, setCaption] = useState('');
  const [image, setImage] = useState<ImagePicker.ImagePickerAsset | null>(null);
  const [uploading, setUploading] = useState(false);

  const [getUploadUrl] = useLazyQuery(GET_UPLOAD_URL);
  const [createPost] = useMutation(CREATE_POST, {
    refetchQueries: [{ query: GET_FEED, variables: { page: 0, pageSize: 20 } }],
  });

  const pickImage = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    
    if (status !== 'granted') {
      Alert.alert('Permission Denied', 'Camera roll permission is required.');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.8,
    });

    if (!result.canceled && result.assets[0]) {
      setImage(result.assets[0]);
    }
  };

  const takePhoto = async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    
    if (status !== 'granted') {
      Alert.alert('Permission Denied', 'Camera permission is required.');
      return;
    }

    const result = await ImagePicker.launchCameraAsync({
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.8,
    });

    if (!result.canceled && result.assets[0]) {
      setImage(result.assets[0]);
    }
  };

  const uploadToS3 = async (uploadUrl: string, imageUri: string, contentType: string) => {
    const response = await fetch(imageUri);
    const blob = await response.blob();
    
    await fetch(uploadUrl, {
      method: 'PUT',
      headers: {
        'Content-Type': contentType,
      },
      body: blob,
    });
  };

  const handlePost = async () => {
    if (!image) {
      Alert.alert('No Image', 'Please select an image for your post.');
      return;
    }

    setUploading(true);

    try {
      // Get file info
      const fileName = image.uri.split('/').pop() || 'image.jpg';
      const contentType = image.mimeType || 'image/jpeg';

      // 1. Get presigned URL from backend
      const { data: uploadData } = await getUploadUrl({
        variables: { fileName, contentType },
      });

      if (!uploadData?.getUploadUrl) {
        // Demo mode - skip S3 upload
        console.log('Demo mode: Skipping S3 upload');
        
        await createPost({
          variables: {
            input: {
              caption: caption.trim() || null,
              mediaKey: `demo/${fileName}`,
            },
          },
        });

        Alert.alert('Success', 'Your post has been created!');
        router.back();
        return;
      }

      const { uploadUrl, mediaKey } = uploadData.getUploadUrl;

      // 2. Upload image directly to S3
      await uploadToS3(uploadUrl, image.uri, contentType);

      // 3. Create post with the media key
      await createPost({
        variables: {
          input: {
            caption: caption.trim() || null,
            mediaKey,
          },
        },
      });

      Alert.alert('Success', 'Your post has been created!');
      router.back();

    } catch (error: any) {
      console.error('Post creation failed:', error);
      Alert.alert('Error', error.message || 'Failed to create post. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Image Section */}
      <View style={styles.imageSection}>
        {image ? (
          <View style={styles.imageContainer}>
            <Image source={{ uri: image.uri }} style={styles.image} />
            <TouchableOpacity 
              style={styles.removeButton}
              onPress={() => setImage(null)}
            >
              <Text style={styles.removeButtonText}>✕</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <View style={styles.imagePlaceholder}>
            <Text style={styles.placeholderEmoji}>📸</Text>
            <Text style={styles.placeholderText}>Add a photo of your pet</Text>
            
            <View style={styles.imageButtons}>
              <TouchableOpacity style={styles.imageButton} onPress={pickImage}>
                <Text style={styles.imageButtonIcon}>🖼️</Text>
                <Text style={styles.imageButtonText}>Gallery</Text>
              </TouchableOpacity>
              
              <TouchableOpacity style={styles.imageButton} onPress={takePhoto}>
                <Text style={styles.imageButtonIcon}>📷</Text>
                <Text style={styles.imageButtonText}>Camera</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}
      </View>

      {/* Caption Section */}
      <View style={styles.captionSection}>
        <TextInput
          style={styles.captionInput}
          placeholder="Write a caption..."
          placeholderTextColor="#9CA3AF"
          multiline
          maxLength={500}
          value={caption}
          onChangeText={setCaption}
        />
        <Text style={styles.charCount}>{caption.length}/500</Text>
      </View>

      {/* Post Button */}
      <TouchableOpacity 
        style={[styles.postButton, !image && styles.postButtonDisabled]}
        onPress={handlePost}
        disabled={!image || uploading}
      >
        {uploading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.postButtonText}>Share Post</Text>
        )}
      </TouchableOpacity>

      {/* Cancel Button */}
      <TouchableOpacity 
        style={styles.cancelButton}
        onPress={() => router.back()}
        disabled={uploading}
      >
        <Text style={styles.cancelButtonText}>Cancel</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  content: {
    padding: 20,
  },
  imageSection: {
    marginBottom: 20,
  },
  imageContainer: {
    position: 'relative',
    borderRadius: 16,
    overflow: 'hidden',
  },
  image: {
    width: '100%',
    aspectRatio: 1,
    borderRadius: 16,
  },
  removeButton: {
    position: 'absolute',
    top: 12,
    right: 12,
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  removeButtonText: {
    color: '#fff',
    fontSize: 16,
  },
  imagePlaceholder: {
    backgroundColor: '#F3F4F6',
    borderRadius: 16,
    padding: 40,
    alignItems: 'center',
    borderWidth: 2,
    borderStyle: 'dashed',
    borderColor: '#E5E7EB',
  },
  placeholderEmoji: {
    fontSize: 50,
    marginBottom: 12,
  },
  placeholderText: {
    fontSize: 16,
    color: '#6B7280',
    marginBottom: 20,
  },
  imageButtons: {
    flexDirection: 'row',
    gap: 16,
  },
  imageButton: {
    backgroundColor: '#fff',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 12,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  imageButtonIcon: {
    fontSize: 24,
    marginBottom: 4,
  },
  imageButtonText: {
    fontSize: 14,
    color: '#4F46E5',
    fontWeight: '600',
  },
  captionSection: {
    marginBottom: 24,
  },
  captionInput: {
    backgroundColor: '#F9FAFB',
    borderRadius: 12,
    padding: 16,
    fontSize: 16,
    minHeight: 120,
    textAlignVertical: 'top',
    color: '#1F2937',
  },
  charCount: {
    textAlign: 'right',
    fontSize: 12,
    color: '#9CA3AF',
    marginTop: 8,
  },
  postButton: {
    backgroundColor: '#4F46E5',
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: 12,
  },
  postButtonDisabled: {
    backgroundColor: '#A5B4FC',
  },
  postButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  cancelButton: {
    paddingVertical: 16,
    alignItems: 'center',
  },
  cancelButtonText: {
    color: '#6B7280',
    fontSize: 16,
  },
});
