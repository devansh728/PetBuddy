import { gql } from '@apollo/client';

// ==================== USER QUERIES ====================

export const GET_PROFILE = gql`
  query GetProfile {
    getProfile {
      userId
      fullName
      email
      isVerified
      isVolunteer
      latitude
      longitude
      createdAt
    }
  }
`;

export const GET_PETS = gql`
  query GetPets {
    getPets {
      petId
      name
      breed
      ageMonths
      imageUrl
      createdAt
    }
  }
`;

// ==================== USER MUTATIONS ====================

export const REGISTER_USER = gql`
  mutation RegisterUser($input: RegisterUserInput!) {
    registerUser(input: $input) {
      userId
      fullName
      email
      isVerified
      createdAt
    }
  }
`;

export const UPDATE_LOCATION = gql`
  mutation UpdateLocation($latitude: Float!, $longitude: Float!, $isVolunteer: Boolean!) {
    updateLocation(latitude: $latitude, longitude: $longitude, isVolunteer: $isVolunteer)
  }
`;

export const ADD_PET = gql`
  mutation AddPet($pets: [PetInput!]!) {
    addPet(pets: $pets) {
      petId
      name
      breed
      ageMonths
      imageUrl
      createdAt
    }
  }
`;

// Request to add a new pet
// message AddPetRequest {
//   string user_id = 1;
//   repeated PetData pets = 2;
// }

// Pet response
// message PetResponse {
//   string pet_id = 1;
//   string name = 2;
//   string breed = 3;
//   int32 age_months = 4;
//   string image_url = 5;
//   string created_at = 6;
// }

// ==================== SOCIAL QUERIES ====================

export const GET_FEED = gql`
  query GetFeed($page: Int, $pageSize: Int) {
    getFeed(page: $page, pageSize: $pageSize) {
      posts {
        postId
        userId
        authorUsername
        caption
        mediaUrl
        locationLat
        locationLon
        likeCount
        commentCount
        isLiked
        createdAt
      }
      totalPages
      hasMore
    }
  }
`;

export const GET_USER_POSTS = gql`
  query GetUserPosts($userId: ID!, $page: Int, $pageSize: Int) {
    getUserPosts(userId: $userId, page: $page, pageSize: $pageSize) {
      posts {
        postId
        userId
        authorUsername
        caption
        mediaUrl
        locationLat
        locationLon
        likeCount
        commentCount
        isLiked
        createdAt
      }
      totalPages
      hasMore
    }
  }
`;

export const GET_UPLOAD_URL = gql`
  query GetUploadUrl($fileName: String!, $contentType: String!) {
    getUploadUrl(fileName: $fileName, contentType: $contentType) {
      uploadUrl
      mediaKey
      expiresInSeconds
    }
  }
`;

export const GET_POST = gql`
  query GetPost($postId: ID!) {
    getPost(postId: $postId) {
      postId
      userId
      caption
      mediaUrl
      locationLat
      locationLon
      likeCount
      commentCount
      isLiked
      createdAt
    }
  }
`;

// ==================== SOCIAL MUTATIONS ====================

export const CREATE_POST = gql`
  mutation CreatePost($input: CreatePostInput!) {
    createPost(input: $input) {
      postId
      userId
      caption
      mediaUrl
      likeCount
      commentCount
      isLiked
      createdAt
    }
  }
`;

export const TOGGLE_LIKE = gql`
  mutation ToggleLike($postId: ID!) {
    toggleLike(postId: $postId) {
      isLiked
      likeCount
    }
  }
`;

// ==================== RESCUE QUERIES ====================

export const GET_NEARBY_INCIDENTS = gql`
  query GetNearbyIncidents($latitude: Float!, $longitude: Float!, $radiusMeters: Float) {
    getNearbyIncidents(latitude: $latitude, longitude: $longitude, radiusMeters: $radiusMeters) {
      incidentId
      reporterId
      latitude
      longitude
      type
      status
      description
      animalType
      imageUrl
      volunteersNotified
      createdAt
    }
  }
`;

// ==================== RESCUE MUTATIONS ====================

export const REPORT_INCIDENT = gql`
  mutation ReportIncident($input: ReportIncidentInput!) {
    reportIncident(input: $input) {
      incidentId
      reporterId
      latitude
      longitude
      type
      status
      description
      animalType
      volunteersNotified
      createdAt
    }
  }
`;

export const UPDATE_VOLUNTEER_STATUS = gql`
  mutation UpdateVolunteerStatus($isVolunteer: Boolean!) {
    updateVolunteerStatus(isVolunteer: $isVolunteer)
  }
`;

// ==================== NOTIFICATION QUERIES ====================

export const GET_NOTIFICATIONS = gql`
  query GetNotifications($page: Int, $pageSize: Int) {
    getNotifications(page: $page, pageSize: $pageSize) {
      notifications {
        notificationId
        actorUsername
        notificationType
        postId
        postCaption
        mediaUrl
        isRead
        createdAt
      }
      totalPages
      hasMore
      unreadCount
    }
  }
`;

export const GET_UNREAD_NOTIFICATION_COUNT = gql`
  query GetUnreadNotificationCount {
    getUnreadNotificationCount
  }
`;

// ==================== NOTIFICATION MUTATIONS ====================

export const MARK_NOTIFICATION_AS_READ = gql`
  mutation MarkNotificationAsRead($notificationId: ID!) {
    markNotificationAsRead(notificationId: $notificationId)
  }
`;
