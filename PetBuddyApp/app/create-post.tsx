import { useState, useEffect } from 'react';
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
  const [images, setImages] = useState<ImagePicker.ImagePickerAsset[]>([]);
  const [uploading, setUploading] = useState(false);
  
  // Debug: log images on every render
  useEffect(() => {
    console.log('[RENDER] CreatePostScreen - images state:', { count: images.length, uris: images.map(i => i.uri) });
  }, [images]);

  const [getUploadUrl] = useLazyQuery(GET_UPLOAD_URL);
  const [createPost] = useMutation(CREATE_POST, {
    refetchQueries: [{ query: GET_FEED, variables: { page: 0, pageSize: 20 } }],
  });

  const pickImage = async () => {
    console.log('[DEBUG] pickImage() called');
    
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    console.log('[DEBUG] Permission status:', status);
    
    if (status !== 'granted') {
      Alert.alert('Permission Denied', 'Camera roll permission is required.');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsMultipleSelection: true,
      selectionLimit: 10,
      allowsEditing: false,
      quality: 0.8,
    });

    console.log('[DEBUG] Picker result:', {
      canceled: result.canceled,
      assetsCount: result.assets?.length || 0,
      assets: result.assets?.map(a => ({ uri: a.uri, type: a.type })),
    });

    if (!result.canceled && result.assets.length > 0) {
      setImages((prev) => {
        const merged = [...prev, ...result.assets];
        const uniqueByUri = new Map(merged.map((asset) => [asset.uri, asset]));
        const updated = Array.from(uniqueByUri.values()).slice(0, 10);
        console.log('[DEBUG] Images state updated:', { totalCount: updated.length });
        return updated;
      });
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
      setImages([result.assets[0]]);
    }
  };

  const uploadToS3 = async (uploadUrl: string, imageUri: string, contentType: string) => {
    try {
      console.log('[DEBUG] Starting S3 upload...', { uploadUrl, imageUri, contentType });
      
      const imageResponse = await fetch(imageUri);
      const blob = await imageResponse.blob();
      console.log('[DEBUG] Blob created:', { size: blob.size, type: blob.type });
      
      const uploadResponse = await fetch(uploadUrl, {
        method: 'PUT',
        headers: {
          'Content-Type': contentType,
        },
        body: blob,
      });

      console.log('[DEBUG] Upload response:', {
        status: uploadResponse.status,
        statusText: uploadResponse.statusText,
        headers: Object.fromEntries(uploadResponse.headers.entries()),
      });

      if (!uploadResponse.ok) {
        const responseText = await uploadResponse.text().catch(() => '');
        throw new Error(`Upload failed (${uploadResponse.status}): ${responseText || uploadResponse.statusText}`);
      }

      console.log('[DEBUG] S3 upload successful');
    } catch (error: any) {
      console.error('[ERROR] S3 upload failed:', {
        message: error.message,
        stack: error.stack,
        errorType: error.name,
      });
      throw error;
    }
  };

  const handlePost = async () => {
    console.log('[DEBUG] handlePost triggered, images:', images.length);
    
    if (images.length === 0) {
      Alert.alert('No Image', 'Please select at least one image for your post.');
      return;
    }

    setUploading(true);

    try {
      for (let index = 0; index < images.length; index += 1) {
        const image = images[index];
        const fileName = image.uri.split('/').pop() || `image-${index + 1}.jpg`;
        const contentType = image.mimeType || 'image/jpeg';

        console.log(`[DEBUG] Processing image ${index + 1}/${images.length}:`, { fileName, contentType });

        const { data: uploadData, error: uploadError } = await getUploadUrl({
          variables: { fileName, contentType },
        });

        if (uploadError) {
          console.error('[ERROR] getUploadUrl failed:', uploadError);
          throw uploadError;
        }

        if (!uploadData?.getUploadUrl) {
          console.log('[DEBUG] Demo mode - creating post without S3 upload');
          await createPost({
            variables: {
              input: {
                caption: caption.trim() || null,
                mediaKey: `demo/${fileName}`,
              },
            },
          });
          continue;
        }

        const { uploadUrl, mediaKey } = uploadData.getUploadUrl;
        console.log('[DEBUG] Got presigned URL, starting S3 upload:', { mediaKey });
        
        await uploadToS3(uploadUrl, image.uri, contentType);
        console.log('[DEBUG] S3 upload complete, creating post...');

        const { data: postData, error: postError } = await createPost({
          variables: {
            input: {
              caption: caption.trim() || null,
              mediaKey,
            },
          },
        });

        if (postError) {
          console.error('[ERROR] createPost failed:', postError);
          throw postError;
        }

        console.log('[DEBUG] Post created successfully:', postData);
      }

      console.log('[DEBUG] All posts created, navigating back');
      Alert.alert('Success', `${images.length} post${images.length > 1 ? 's' : ''} created successfully.`);
      router.back();

    } catch (error: any) {
      console.error('[ERROR] Post creation pipeline failed:', {
        message: error.message,
        stack: error.stack,
        graphQLErrors: error.graphQLErrors,
        networkError: error.networkError,
      });
      Alert.alert('Error', error.message || 'Failed to create post. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Image Section */}
      <View style={styles.imageSection}>
        {images.length > 0 ? (
          <View style={styles.imageContainer}>
            <Image source={{ uri: images[0].uri }} style={styles.image} />
            {images.length > 1 && (
              <View style={styles.badge}>
                <Text style={styles.badgeText}>+{images.length - 1}</Text>
              </View>
            )}
            <TouchableOpacity 
              style={styles.removeButton}
              onPress={() => setImages([])}
            >
              <Text style={styles.removeButtonText}>✕</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.addMoreButton} onPress={pickImage}>
              <Text style={styles.addMoreButtonText}>Add More</Text>
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
        style={[styles.postButton, images.length === 0 && styles.postButtonDisabled]}
        onPress={handlePost}
        disabled={images.length === 0 || uploading}
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
  badge: {
    position: 'absolute',
    top: 12,
    left: 12,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
  },
  badgeText: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 12,
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
  addMoreButton: {
    position: 'absolute',
    bottom: 12,
    right: 12,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    borderRadius: 999,
    paddingHorizontal: 14,
    paddingVertical: 8,
  },
  addMoreButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
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
