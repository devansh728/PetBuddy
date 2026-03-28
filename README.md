# PetBuddy Microservice - Comprehensive Technical Documentation

**Project Type:** Distributed Microservice Architecture  
**Primary Language:** Java (Backend), TypeScript/React Native (Frontend)

---

## Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Microservices Description](#microservices-description)
4. [Data Flow & Communication](#data-flow--communication)
5. [Key Features & Use Cases](#key-features--use-cases)
6. [Database Schema & Data Models](#database-schema--data-models)
7. [Important Files & Their Purpose](#important-files--their-purpose)
8. [Configuration & Environment Variables](#configuration--environment-variables)
9. [Deployment & Infrastructure](#deployment--infrastructure)
10. [Monitoring & Performance Optimization](#monitoring--performance-optimization)

---

## System Architecture Overview

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PETBUDDY ECOSYSTEM                           │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐
│   PetBuddyApp        │  ← React Native + Expo (iOS/Android/Web)
│  (Frontend Client)   │
└──────────────────────┘
          │
          │ GraphQL Queries/Mutations + WebSocket
          │ (Bearer Token Auth)
          │
┌──────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Port 8080)                       │
│  • GraphQL Endpoint (HTTP + WebSocket)                           │
│  • Firebase Authentication Filter                               │
│  • gRPC Client to Backend Services                             │
│  • Request/Response Mapping                                    │
└──────────────────────────────────────────────────────────────────┘
          │
          ├── gRPC ──────────────────┬─────────────────┐
          │  (Port 9090)             │  (Port 9091)     │
          │                          │                  │
    ┌─────────────────┐    ┌─────────────────────┐
    │ PROFILE SERVICE │    │ SOCIAL SERVICE      │
    │  (Port 9090)    │    │  (Port 9091)        │
    │                 │    │                     │
    │ • User Mgmt     │    │ • Posts             │
    │ • Pet Mgmt      │    │ • Notifications     │
    │ • Location      │    │ • Likes/Comments    │
    │ • Volunteer     │    │ • Incident Reports  │
    │   Management    │    │ • Feed Management   │
    └─────────────────┘    └─────────────────────┘
           │                       │
           │                       │
    ┌──────┴───────┐      ┌───────┴──────────┐
    │              │      │                  │
  [PostgreSQL]   [Redis]  [RabbitMQ]    [MinIO/S3]
  Profile DB    Cache   Event Broker   Media Storage
```

### Service Communication Flow

```
Frontend Request
    │
    ├─→ Apollo GraphQL Client
    │   └─→ Injects Bearer Token
    │       └─→ HTTP POST to: http://api-gateway:8080/graphql
    │
    ├─→ API Gateway (Spring Boot)
    │   ├─→ FirebaseAuthFilter validates token
    │   ├─→ DgsComponent routes to DataFetcher
    │   ├─→ DataFetcher calls gRPC Service
    │   └─→ Returns GraphQL response
    │
    ├─→ Microservice (gRPC)
    │   ├─→ Processes business logic
    │   ├─→ Persists to PostgreSQL
    │   ├─→ Publishes events to RabbitMQ
    │   ├─→ Caches in Redis
    │   └─→ Uploads files to MinIO/S3
    │
    └─→ Response travels back to Frontend
        └─→ Apollo Cache updates
            └─→ UI re-renders
```

---

## Technology Stack

### Backend Services

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **API Gateway** | Spring Boot | 3.4.1 | GraphQL endpoint, auth filter, request routing |
| **gRPC** | gRPC Java | 1.77.1 | Microservice inter-communication |
| **Protocol Buffers** | Protobuf | 3.x | Type-safe message serialization |
| **Persistence** | PostgreSQL | 15+ | Main relational database |
| **Caching** | Redis | 7+ | Feed cache, session storage |
| **Message Broker** | RabbitMQ | 3.12+ | Event-driven notifications |
| **File Storage** | MinIO/AWS S3 | Latest | Media (images, videos) storage |
| **ORM** | JPA/Hibernate | 6.2+ | Database abstraction layer |
| **Dependency Injection** | Spring | 6.2.1 | IoC container |
| **Monitoring** | Micrometer/Prometheus | Latest | Metrics collection |
| **Logging** | SLF4J + Logback | Latest | Structured logging |

### Frontend Technologies

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Framework** | React Native | Latest | Cross-platform mobile development |
| **Expo** | Expo Router | Latest | Navigation and routing |
| **API Client** | Apollo GraphQL | 3.14.0+ | GraphQL query management |
| **Language** | TypeScript | Latest | Type-safe JavaScript |
| **State Management** | Apollo Cache | Built-in | Centralized data store |
| **Authentication** | Firebase Auth | Latest | User authentication |
| **Location** | Expo Location | Latest | GPS coordinates |
| **Maps** | Expo Maps (Platform-specific) | Latest | Geographic visualization |
| **Storage** | AsyncStorage/Expo Storage | Native | Local data persistence |
| **HTTP** | Fetch API | Native | HTTP requests |

### Infrastructure & DevOps

| Component | Tech | Purpose |
|-----------|------|---------|
| **Containerization** | Docker | Service deployment |
| **Orchestration** | Docker Compose | Local multi-container setup |
| **Monitoring (Yet to done)** | Prometheus + Grafana | Metrics visualization |


---

## Microservices Description

### 1. API Gateway Service
**Location:** `api-gateway/`  
**Port:** 8080  
**Protocol:** HTTP + GraphQL WebSocket  

#### Responsibilities:
- GraphQL API endpoint for frontend
- Firebase Authentication validation
- Request routing to backend services
- Data transformation and mapping
- Rate limiting and throttling
- Logging and error handling

#### Key Components:

**a) GraphQL Schema** (`schema.graphqls`)
```graphql
type Query {
  # User operations
  getProfile: UserProfile
  getPets: [Pet!]!
  
  # Social operations
  getFeed(page: Int, pageSize: Int): FeedResult
  getUserPosts(userId: String!): FeedResult
  getNotifications: NotificationsResult
  
  # Incident management
  getNearbyIncidents(radius: Int): [Incident!]!
}

type Mutation {
  # User management
  registerUser(input: RegisterUserInput!): UserProfile
  updateLocation(latitude: Float!, longitude: Float!): Boolean
  
  # Social operations
  createPost(input: CreatePostInput!): Post
  toggleLike(postId: String!): LikeResult
  createComment(postId: String!, content: String!): Comment
}
```

**b) DataFetchers** (DGS Components)
- `UserDataFetcher.java` - User profile, pets, location
- `SocialDataFetcher.java` - Posts, feeds, notifications, likes
- `IncidentDataFetcher.java` - Incident reporting and management

**c) Authentication Filter** (`FirebaseAuthFilter.java`)
- Extracts Bearer token from Authorization header
- Validates token with Firebase Auth
- Injects AuthContext into request

**d) gRPC Client Configuration** (`GrpcClientConfig.java`)
- Creates managed channels to backend services
- Connection pooling and timeout configuration
- Service stub creation for blocking calls

#### Tech Stack Details:
- **DGS Framework** (Netflix GraphQL Data Graph Service)
- **Spring Boot Web** (REST/GraphQL server)
- **Firebase Admin SDK** (Authentication validation)
- **gRPC Java** (Backend communication)

---

### 2. Profile-Rescue Service
**Location:** `profile-rescue-svc/demo/`  
**Port:** 9090  
**Protocol:** gRPC  

#### Responsibilities:
- User profile management
- Pet information storage
- Location tracking and updates
- Volunteer status management
- Geographic queries
- User preference management

#### Database Schema (PostgreSQL `profile` schema):

```sql
-- Users Table
CREATE TABLE users (
  id UUID PRIMARY KEY,
  firebase_uid VARCHAR(128) UNIQUE NOT NULL,
  full_name VARCHAR(255),
  email VARCHAR(255) UNIQUE NOT NULL,
  is_verified BOOLEAN DEFAULT false,
  is_volunteer BOOLEAN DEFAULT false,
  latitude DECIMAL(10, 8),
  longitude DECIMAL(11, 8),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Pets Table
CREATE TABLE pets (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  breed VARCHAR(255),
  age_months INT,
  image_url TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Geographic Index for volunteer location queries
CREATE INDEX idx_volunteers_location ON users USING GIST (
  ll_to_earth(latitude, longitude)
) WHERE is_volunteer = true;
```

#### Key gRPC Services:

**ProfileService.proto**
```protobuf
service ProfileService {
  rpc GetUserProfile(GetUserRequest) returns (UserProfileResponse);
  rpc CreateUserProfile(CreateUserRequest) returns (UserProfileResponse);
  rpc UpdateLocation(UpdateLocationRequest) returns (Empty);
  rpc UpdateVolunteerStatus(UpdateVolunteerRequest) returns (Empty);
  rpc GetPets(GetUserRequest) returns (PetsResponse);
  rpc AddPets(AddPetsRequest) returns (PetsResponse);
}
```

#### Key Business Logic:
- User registration with Firebase integration
- Pet catalog management
- Location updates with geospatial indexing
- Volunteer network management
- User preference and settings

#### Tech Stack Details:
- **Spring Boot** with gRPC server
- **PostgreSQL** with PostGIS for geospatial queries
- **JPA Hibernate** for ORM
- **Protocol Buffers** for message definition

---

### 3. Social-Stream Service
**Location:** `social-stream-svc/`  
**Port:** 9091  
**Protocol:** gRPC  

#### Responsibilities:
- Post creation, retrieval, and management
- Feed generation and pagination
- Notification management and delivery
- Like/comment interactions
- Social graph operations
- S3/MinIO media presigned URL generation
- Event publishing to RabbitMQ

#### Database Schema (PostgreSQL `social` schema):

```sql
-- Posts Table
CREATE TABLE posts (
  id UUID PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL,
  caption TEXT,
  media_url TEXT,
  location_lat DECIMAL(10, 8),
  location_lon DECIMAL(11, 8),
  like_count INT DEFAULT 0,
  comment_count INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Likes Table
CREATE TABLE likes (
  id UUID PRIMARY KEY,
  post_id UUID REFERENCES posts(id) ON DELETE CASCADE,
  user_id VARCHAR(128) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(post_id, user_id)
);

-- Comments Table
CREATE TABLE comments (
  id UUID PRIMARY KEY,
  post_id UUID REFERENCES posts(id) ON DELETE CASCADE,
  user_id VARCHAR(128) NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Notifications Table
CREATE TABLE notifications (
  id UUID PRIMARY KEY,
  recipient_user_id VARCHAR(128) NOT NULL,
  actor_user_id VARCHAR(128) NOT NULL,
  actor_username VARCHAR(255),
  notification_type VARCHAR(50),  -- POST_LIKED, POST_UNLIKED, COMMENT_CREATED, etc.
  post_id UUID REFERENCES posts(id),
  post_caption TEXT,
  media_url TEXT,
  is_read BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_recipient_created (recipient_user_id, created_at DESC),
  INDEX idx_unread (recipient_user_id, is_read)
);

-- Indexes for performance
CREATE INDEX idx_posts_user_created ON posts(user_id, created_at DESC);
CREATE INDEX idx_posts_created ON posts(created_at DESC);
CREATE INDEX idx_likes_post ON likes(post_id);
CREATE INDEX idx_comments_post ON comments(post_id);
```

#### Key gRPC Services:

**SocialService.proto**
```protobuf
service SocialService {
  rpc GetUploadUrl(GetUploadUrlRequest) returns (UploadUrlResponse);
  rpc CreatePost(CreatePostRequest) returns (PostResponse);
  rpc GetFeed(GetFeedRequest) returns (FeedResponse);
  rpc GetUserPosts(GetUserPostsRequest) returns (PostsResponse);
  rpc ToggleLike(ToggleLikeRequest) returns (LikeResponse);
  rpc CreateComment(CreateCommentRequest) returns (CommentResponse);
  rpc GetNotifications(GetNotificationsRequest) returns (NotificationsResponse);
  rpc GetUnreadNotificationCount(UnreadCountRequest) returns (UnreadCountResponse);
  rpc MarkNotificationAsRead(MarkNotificationReadRequest) returns (Empty);
}
```

#### Event-Driven Architecture (RabbitMQ):

**Events Published:**
```
Topic Exchange: social.exchange

Routing Keys:
├── post.created
│   └── Queue: social.post.created
│       └── Consumer: NotificationConsumer.handlePostCreated()
│
├── like.created
│   └── Queue: social.like.created
│       └── Consumer: NotificationConsumer.handleLikeCreated()
│
└── like.deleted
    └── Queue: social.like.deleted
        └── Consumer: NotificationConsumer.handleLikeDeleted()
```

**Event Payload Examples:**
```json
{
  "postId": "70602a0a-4c94-45e9-af84-80436a014754",
  "userId": "0xkGDANPDocdvKzBAEtdT3UWaSK2",
  "caption": "My cute pet!",
  "mediaUrl": "http://minio:9000/petbuddy-media/...",
  "postOwnerId": "another-user-id"
}
```

#### Key Business Logic:
1. **Feed Generation:** Aggregates posts from followed users with pagination
2. **Notification System:** Creates notifications when users interact with posts
3. **Media Upload:** Generates presigned S3 URLs for direct client uploads
4. **Caching:** Uses Redis to cache feed results, invalidates on new posts
5. **Social Interactions:** Manages likes and comments with atomic operations

#### S3 Integration:

**Presigned URL Generation Flow:**
```
1. Frontend requests upload URL
   └─→ GraphQL: getUploadUrl(fileName, contentType)

2. API Gateway routes to SocialDataFetcher
   └─→ Calls gRPC: getUploadUrl()

3. Social Service generates presigned URL
   └─→ S3PresignedUrlService.generatePresignedUploadUrl()
   └─→ Returns signed URL + media key

4. Frontend uploads directly to S3
   └─→ PUT request to presigned URL
   └─→ File stored in S3 bucket

5. URL returned to client
   └─→ mediaUrl: "http://minio:9000/petbuddy-media/users/{userId}/media/{fileName}"
```

#### Tech Stack Details:
- **Spring Boot** with gRPC server
- **PostgreSQL** for relational data
- **Redis** for caching and session storage
- **RabbitMQ** for event publishing
- **AWS SDK** for S3 presigned URL generation
- **Protocol Buffers** for message definition

---

## Data Flow & Communication

### 1. User Registration Flow

```
Frontend (Expo)
    │
    ├─→ Firebase Auth
    │   └─→ Creates user, returns ID token
    │
    ├─→ GraphQL Mutation: registerUser
    │   └─→ POST /graphql
    │   └─→ Variables: { email, fullName }
    │   └─→ Header: Authorization: Bearer {idToken}
    │
    ├─→ API Gateway
    │   ├─→ FirebaseAuthFilter validates token
    │   ├─→ Extracts firebaseUid from token
    │   ├─→ Calls UserDataFetcher.registerUser()
    │   └─→ gRPC to profile-service:9090
    │
    ├─→ Profile Service
    │   ├─→ Creates user record in PostgreSQL
    │   ├─→ Caches user profile in Redis
    │   └─→ Returns UserProfileResponse
    │
    └─→ Response back to Frontend
        └─→ { userId, fullName, email, ... }
```

### 2. Create Post with Media Upload

```
Frontend
├─→ Step 1: Get Upload URL
│   ├─→ GraphQL: getUploadUrl(fileName, contentType)
│   └─→ Returns: { uploadUrl, mediaKey, expiresIn }
│
├─→ Step 2: Upload Media Directly to S3
│   └─→ PUT {uploadUrl} with file content
│       └─→ MinIO/S3 stores file
│
├─→ Step 3: Create Post with Media Reference
│   ├─→ GraphQL Mutation: createPost
│   ├─→ Variables: {
│   │     caption: "My cute pet!",
│   │     mediaUrl: "http://minio:9000/petbuddy-media/...",
│   │     latitude, longitude
│   │   }
│   └─→ gRPC to social-service
│
├─→ Step 4: Process Post
│   ├─→ Save to PostgreSQL
│   ├─→ Cache in Redis
│   ├─→ Publish post.created event to RabbitMQ
│   └─→ Return PostResponse
│
└─→ Frontend receives post confirmation
    └─→ Updates Apollo Cache
        └─→ RefreshFeed triggered
```

### 3. Notification Generation Flow

```
User A likes User B's Post
    │
    ├─→ Frontend sends ToggleLike mutation to API Gateway
    │
    ├─→ Social Service
    │   ├─→ Adds like record to PostgreSQL
    │   ├─→ Increments post.like_count
    │   ├─→ Publishes like.created event to RabbitMQ
    │   │   {
    │   │     "postId": "...",
    │   │     "userId": "User-A-ID",
    │   │     "postOwnerId": "User-B-ID",
    │   │     "username": "User A Name"
    │   │   }
    │   └─→ Returns LikeResponse
    │
    ├─→ RabbitMQ Consumer (NotificationConsumer)
    │   ├─→ Receives like.created event
    │   ├─→ Checks if User A ≠ User B (no self-notifications)
    │   ├─→ Creates NotificationEntity
    │   ├─→ Saves to PostgreSQL notifications table
    │   │   {
    │   │     "recipientUserId": "User-B-ID",
    │   │     "actorUsername": "User A Name",
    │   │     "notificationType": "LIKE_CREATED",
    │   │     "isRead": false
    │   │   }
    │   └─→ Increments unread count
    │
    └─→ User B
        ├─→ Frontend polls: getUnreadNotificationCount (every 30s)
        ├─→ Bell icon updates with badge showing: 1
        │
        ├─→ User opens notifications
        ├─→ GraphQL: getNotifications (page: 0, pageSize: 20)
        ├─→ Displays: "User A Name liked your post ❤️"
        │
        ├─→ User clicks notification
        ├─→ GraphQL Mutation: markNotificationAsRead
        ├─→ Updates isRead: true
        └─→ Badge updates to: 0
```

### 4. Feed Generation with Caching

```
Frontend requests: getFeed(page: 0, pageSize: 20)
    │
    ├─→ Social Service
    │   ├─→ Check Redis Cache for key: "feed:global:0"
    │   │
    │   ├─→ Cache MISSING? (First request or invalidated)
    │   │   ├─→ Query PostgreSQL:
    │   │   │   SELECT * FROM posts
    │   │   │   ORDER BY created_at DESC
    │   │   │   LIMIT 20 OFFSET 0
    │   │   ├─→ Result: [Post1, Post2, ..., Post20]
    │   │   ├─→ Transform to DTO
    │   │   ├─→ Store in Redis with TTL: 5 minutes
    │   │   └─→ Return posts
    │   │
    │   └─→ Cache HIT?
    │       └─→ Return cached posts instantly (~1ms)
    │
    └─→ Frontend
        ├─→ Apollo Cache stores result
        ├─→ UI renders feed
        └─→ On pull-refresh: refetch() invalidates cache
```

---

## Key Features & Use Cases

### Feature 1: Social Feed Management

**Use Case:** Users browse their personalized feed of posts

```
Frontend: Feed Screen
├─→ Queries: getFeed(page: 0, pageSize: 20)
├─→ Displays: Post list with images, captions, likes
├─→ Interactions:
│   ├─→ Like/Unlike post
│   ├─→ Comment on post
│   ├─→ Share post
│   └─→ Infinite scroll (pagination)
└─→ Real-time updates: WebSocket for new posts
```

**Backend Processing:**
- Pagination with cursor-based or offset-based approach
- Redis caching for performance
- User preference filtering (optional)
- Timestamp ordering

---

### Feature 2: Notification System

**Use Case:** Users receive real-time notifications for interactions

```
Notification Types:
├─→ POST_LIKED: User A liked User B's post
├─→ POST_UNLIKED: User A removed their like
├─→ COMMENT_CREATED: User A commented on User B's post
├─→ FOLLOW_CREATED: User A started following User B
└─→ INCIDENT_REPORTED: New rescue incident near you

Notification Flow:
1. Frontend subscribes to push notifications
2. Backend publishes events to RabbitMQ
3. Notification consumer processes events
4. Updates PostgreSQL notifications table
5. Frontend polls every 30s: getUnreadNotificationCount()
6. Bell icon shows unread count badge
7. User opens notifications screen
8. Displays chronological notification list
9. User marks as read (graphql mutation)
10. Notification isRead: true, unread count decreases
```

---

### Feature 3: Media Upload with S3 Presigned URLs

**Use Case:** Users upload post images directly to S3

```
Workflow:
1. User selects image from device
2. Frontend calls: getUploadUrl(fileName, contentType)
3. Backend generates presigned URL
4. Frontend uploads to S3 directly (bypasses backend)
5. Backend stores media URL reference in database
6. Post creation associates media URL

Benefits:
- Offloads upload traffic from backend
- Faster uploads (direct to S3)
- Reduces backend memory usage
- Scalable for large files
- Secure with time-limited URLs
```

**Technical Flow:**
```
Frontend
├─→ Select image.jpg (5MB)
├─→ Call S3PresignedUrlService.generatePresignedUploadUrl()
├─→ Receives: {
│     uploadUrl: "https://s3.amazonaws.com/bucket/...",
│     mediaKey: "users/userId/media/image.jpg",
│     expiresIn: 300
│   }
└─→ PUT image.jpg to uploadUrl (direct to S3)
    └─→ S3 stores file
        └─→ File accessible at: 
            http://minio:9000/petbuddy-media/users/userId/media/image.jpg
```

---

### Feature 4: Location-Based Volunteer Management

**Use Case:** System matches volunteers near rescue incidents

```
Workflow:
1. User enables volunteer mode
2. Frontend periodically updates location (GPS)
3. Backend stores latitude/longitude in PostgreSQL
4. Geographic index on volunteers table for fast queries
5. When incident reported near (lat, lon):
   - Query volunteers within 5km radius
   - Send push notifications to nearby volunteers
   - Log incident with status tracking

Database Query Example:
SELECT * FROM users
WHERE is_volunteer = true
AND earth_distance(ll_to_earth(15.123, 75.456), ll_to_earth(latitude, longitude)) < 5000
ORDER BY earth_distance(ll_to_earth(15.123, 75.456), ll_to_earth(latitude, longitude));
```

---

### Feature 5: Pet Profile Management

**Use Case:** Users manage their pet information

```
Requests:
1. GET /graphql (Query: getPets)
   └─→ Retrieve all pets for current user
   └─→ Details: name, breed, age, image

2. POST /graphql (Mutation: addPet)
   └─→ Add new pet information
   └─→ Upload pet image via presigned URL
   └─→ Store reference in PostgreSQL

Data Model:
{
  petId: UUID,
  userId: UUID,
  name: "Buddy",
  breed: "Golden Retriever",
  ageMonths: 24,
  imageUrl: "http://minio:9000/petbuddy-media/.../pet.jpg",
  createdAt: timestamp
}
```

---

## Database Schema & Data Models

### Profile Service Schema (PostgreSQL)

```sql
-- Schema: profile

-- Users Table
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  firebase_uid VARCHAR(128) UNIQUE NOT NULL,
  full_name VARCHAR(255),
  email VARCHAR(255) UNIQUE NOT NULL,
  is_verified BOOLEAN DEFAULT false,
  is_volunteer BOOLEAN DEFAULT false,
  latitude DECIMAL(10, 8),           -- WGS84
  longitude DECIMAL(11, 8),          -- WGS84
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Pets Table
CREATE TABLE pets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  breed VARCHAR(255),
  age_months INT CHECK (age_months >= 0),
  image_url TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- User Preferences
CREATE TABLE user_preferences (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  notification_enabled BOOLEAN DEFAULT true,
  location_sharing_enabled BOOLEAN DEFAULT true,
  rescue_alerts BOOLEAN DEFAULT true,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_firebase_uid ON users(firebase_uid);
CREATE INDEX idx_user_pets ON pets(user_id);
CREATE INDEX idx_volunteers_location ON users USING GIST (ll_to_earth(latitude, longitude)) 
  WHERE is_volunteer = true;
```

### Social Service Schema (PostgreSQL)

```sql
-- Schema: social

-- Posts Table
CREATE TABLE posts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id VARCHAR(128) NOT NULL,
  caption TEXT,
  media_url TEXT,
  location_lat DECIMAL(10, 8),
  location_lon DECIMAL(11, 8),
  like_count INT DEFAULT 0 CHECK (like_count >= 0),
  comment_count INT DEFAULT 0 CHECK (comment_count >= 0),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Likes Table
CREATE TABLE likes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  user_id VARCHAR(128) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(post_id, user_id)
);

-- Comments Table
CREATE TABLE comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  user_id VARCHAR(128) NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Notifications Table (Extended Schema)
CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipient_user_id VARCHAR(128) NOT NULL,
  actor_user_id VARCHAR(128) NOT NULL,
  actor_username VARCHAR(255),
  notification_type VARCHAR(50),  -- Enum at application level
  post_id UUID REFERENCES posts(id),
  post_caption TEXT,
  media_url TEXT,
  is_read BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Performance
CREATE INDEX idx_posts_user_created ON posts(user_id, created_at DESC);
CREATE INDEX idx_posts_created_desc ON posts(created_at DESC);
CREATE INDEX idx_likes_post ON likes(post_id);
CREATE INDEX idx_likes_user ON likes(user_id);
CREATE INDEX idx_comments_post ON comments(post_id);
CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(recipient_user_id, is_read) WHERE is_read = false;
```

### Incident Management Schema (Profile Service)

```sql
-- Incidents Table
CREATE TABLE incidents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id VARCHAR(128) NOT NULL,
  incident_type VARCHAR(50) NOT NULL,  -- ACCIDENT, ABANDONED, INJURED, LOST, ABUSE
  status VARCHAR(50) DEFAULT 'OPEN',   -- OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CANCELLED
  description TEXT,
  latitude DECIMAL(10, 8) NOT NULL,
  longitude DECIMAL(11, 8) NOT NULL,
  animal_type VARCHAR(255),
  image_urls TEXT,  -- JSON array of URLs
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Incident Assignments
CREATE TABLE incident_assignments (
  id UUID PRIMARY KEY,
  incident_id UUID REFERENCES incidents(id),
  volunteer_id VARCHAR(128),
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(50)
);

-- Geographic Index
CREATE INDEX idx_incidents_location ON incidents USING GIST (
  ll_to_earth(latitude, longitude)
) WHERE status != 'CLOSED';
```

---

## Important Files & Their Purpose

### Backend Services

#### Profile-Rescue Service
```
profile-rescue-svc/demo/
├── src/main/java/com/profile/rescue/
│   ├── service/
│   │   ├── ProfileService.java           ← User profile business logic
│   │   ├── PetService.java               ← Pet management
│   │   └── LocationService.java          ← Geospatial queries
│   │
│   ├── controller/
│   │   └── ProfileGrpcService.java       ← gRPC service implementation
│   │
│   ├── entity/
│   │   ├── UserEntity.java               ← User JPA entity
│   │   ├── PetEntity.java                ← Pet JPA entity
│   │   └── IncidentEntity.java           ← Incident JPA entity
│   │
│   ├── repository/
│   │   ├── UserRepository.java           ← Spring Data JPA interface
│   │   ├── PetRepository.java
│   │   └── IncidentRepository.java
│   │
│   └── config/
│       ├── GrpcServerConfig.java         ← gRPC server setup
│       └── DatabaseConfig.java           ← PostgreSQL connection
│
└── src/main/proto/
    └── profile_service.proto              ← gRPC service definition
```

#### Social-Stream Service
```
social-stream-svc/
├── src/main/java/com/petbuddy/social/
│   ├── service/
│   │   ├── FeedService.java              ← Feed generation & caching
│   │   ├── NotificationService.java      ← Notification CRUD
│   │   ├── S3PresignedUrlService.java    ← Media upload URLs
│   │   └── EventPublisherService.java    ← RabbitMQ event publishing
│   │
│   ├── controller/
│   │   └── SocialGrpcService.java        ← gRPC service implementation
│   │
│   ├── consumer/
│   │   └── NotificationConsumer.java     ← RabbitMQ event listeners
│   │       ├── handlePostCreated()
│   │       ├── handleLikeCreated()
│   │       └── handleLikeDeleted()
│   │
│   ├── entity/
│   │   ├── PostEntity.java               ← JPA entity
│   │   ├── LikeEntity.java
│   │   ├── CommentEntity.java
│   │   ├── NotificationEntity.java
│   │   └── IncidentEntity.java
│   │
│   ├── repository/
│   │   ├── PostRepository.java           ← Spring Data JPA
│   │   ├── LikeRepository.java
│   │   ├── NotificationRepository.java
│   │   └── IncidentRepository.java
│   │
│   └── config/
│       ├── S3Config.java                 ← AWS S3/MinIO client
│       ├── RabbitMQConfig.java           ← Message broker setup
│       ├── GrpcServerConfig.java         ← gRPC server
│       └── CacheErrorConfig.java         ← Redis cache error handling
│
└── src/main/proto/
    └── social_service.proto               ← gRPC service definition
```

#### API Gateway
```
api-gateway/
├── src/main/java/com/petbuddy/gateway/
│   ├── datafetcher/
│   │   ├── UserDataFetcher.java          ← GraphQL resolvers for user
│   │   ├── SocialDataFetcher.java        ← GraphQL resolvers for social
│   │   └── IncidentDataFetcher.java      ← GraphQL resolvers for incidents
│   │
│   ├── filter/
│   │   └── FirebaseAuthFilter.java       ← Authentication filter
│   │
│   ├── context/
│   │   └── AuthContext.java              ← Holds authenticated user info
│   │
│   ├── config/
│   │   └── GrpcClientConfig.java         ← gRPC channel setup
│   │
│   └── controller/
│       └── GraphQLController.java        ← GraphQL endpoint handler
│
└── src/main/resources/
    └── schema/
        └── schema.graphqls               ← GraphQL schema definition
```

### Frontend Application

```
PetBuddyApp/
├── src/
│   ├── graphql/
│   │   └── operations.ts                 ← All GraphQL queries/mutations
│   │       ├── GET_PROFILE
│   │       ├── GET_FEED
│   │       ├── GET_NOTIFICATIONS
│   │       ├── CREATE_POST
│   │       ├── TOGGLE_LIKE
│   │       └── ... (20+ operations)
│   │
│   ├── types/
│   │   └── index.ts                      ← TypeScript interfaces
│   │       ├── UserProfile
│   │       ├── Post
│   │       ├── Notification
│   │       ├── Incident
│   │       └── ... (all data types)
│   │
│   ├── lib/
│   │   ├── apollo-client.ts              ← Apollo configuration
│   │   │   ├── HTTP Link
│   │   │   ├── Auth Link
│   │   │   ├── Cache configuration
│   │   │   └── Logging Link
│   │   │
│   │   └── storage.ts                    ← Platform-aware storage
│   │       ├── AsyncStorage (mobile)
│   │       └── LocalStorage (web)
│   │
│   ├── config/
│   │   └── firebase.config.ts            ← Firebase & API config
│   │
│   ├── hooks/
│   │   └── useAuth.tsx                   ← Authentication hook
│   │
│   └── components/
│       ├── NotificationBell.tsx          ← Header bell icon
│       └── MapComponents.*.tsx           ← Platform-specific maps
│
├── app/
│   ├── _layout.tsx                       ← Root navigation layout
│   ├── index.tsx                         ← Home/splash screen
│   ├── login.tsx                         ← Login screen
│   ├── create-post.tsx                   ← Post creation screen
│   ├── notifications.tsx                 ← Notifications screen
│   ├── report-incident.tsx               ← Incident reporting screen
│   │
│   └── (tabs)/                           ← Tab-based navigation
│       ├── _layout.tsx                   ← Tab layout
│       ├── feed.tsx                      ← Feed screen
│       ├── map.tsx                       ← Map/incidents screen
│       └── profile.tsx                   ← User profile screen
│
├── app.json                              ← Expo config
├── package.json                          ← Dependencies
├── tsconfig.json                         ← TypeScript config
└── babel.config.js                       ← Babel config
```

### Configuration Files

```
Project Root
├── docker-compose.yml                    ← Local development setup
├── JVM_OPTIMIZATION.md                   ← JVM tuning parameters
├── TECHNICAL_DOCUMENTATION.md            ← This file
├── README.md                             ← Project overview
│
├── api-gateway/
│   ├── build.gradle                      ← Gradle build config
│   ├── src/main/resources/
│   │   └── application.properties        ← Spring Boot config
│   └── src/main/proto/                   ← gRPC proto files
│
├── social-stream-svc/
│   ├── build.gradle                      ← Gradle build config
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── application-prod.properties   ← Production config
│   └── src/main/proto/                   ← gRPC proto files
│
├── profile-rescue-svc/demo/
│   ├── build.gradle
│   ├── src/main/resources/
│   │   └── application.properties
│   └── src/main/proto/
│
└── monitoring/
    ├── docker-compose.monitoring.yml     ← Monitoring stack
    ├── prometheus.yml                    ← Prometheus config
    └── README.md                         ← Monitoring guide
```

---

## Configuration & Environment Variables

### Backend Configuration

#### Profile-Rescue Service (`application.properties`)

```properties
# Spring Boot Server Config
server.port=9090
server.servlet.context-path=/

# Server Name for gRPC
grpc.server.port=9090

# PostgreSQL Database
spring.datasource.url=jdbc:postgresql://localhost:5432/petbuddy_profile
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.default_schema=profile
spring.jpa.properties.hibernate.dialect=org.hibernate.spatial.dialect.postgis.PostgisPG12Dialect

# Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=20000

# Logging
logging.level.root=INFO
logging.level.com.profile.rescue=DEBUG

# Monitoring
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.enable.jvm=true
management.metrics.enable.db.hikaricp=true
```

#### Social-Stream Service (`application.properties`)

```properties
# Server Config
server.port=9091
grpc.server.port=9091

# PostgreSQL (Social Schema)
spring.datasource.url=jdbc:postgresql://localhost:5432/petbuddy_social
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

# Redis Cache
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=5000ms

# RabbitMQ Event Broker
spring.rabbitmq.addresses=${RABBITMQ_HOST:localhost}:${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USER:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}

# AWS S3 / MinIO
aws.s3.region=ap-south-1
aws.s3.bucket=petbuddy-media
aws.s3.endpoint=http://minio:9000
aws.s3.path-style-access=true
aws.access-key=${AWS_ACCESS_KEY}
aws.secret-key=${AWS_SECRET_KEY}
aws.s3.presigned-url-expiry-minutes=5

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.default_schema=social
spring.jpa.properties.hibernate.dialect=org.hibernate.spatial.dialect.postgis.PostgisPG12Dialect

# Monitoring
management.endpoints.web.exposure.include=health,metrics,prometheus
```

#### API Gateway (`application.properties`)

```properties
# Server Config
server.port=8080
spring.application.name=api-gateway

# gRPC Client Configuration
grpc.client.profile-service.address=static://localhost:9090
grpc.client.social-service.address=static://localhost:9091

# Firebase Authentication
firebase.credentials.path=classpath:firebase-service-account.json
firebase.project-id=${FIREBASE_PROJECT_ID}

# Logging
logging.level.com.netflix.graphql.dgs=INFO
logging.level.com.petbuddy.gateway=DEBUG

# GraphQL
graphql.schema-locations=classpath:schema/schema.graphqls

# Monitoring
management.endpoints.web.exposure.include=health,metrics,prometheus
```

### Environment Variables (`.env` file)

```bash
# Firebase
FIREBASE_PROJECT_ID=petbuddy-firebase-project
FIREBASE_API_KEY=xxx
FIREBASE_AUTH_DOMAIN=xxx

# PostgreSQL
DB_PASSWORD=secure_password
POSTGRES_USER=postgres
POSTGRES_PASSWORD=secure_password

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL=false

# RabbitMQ
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_URL=amqp://guest:guest@rabbitmq:5672/

# AWS S3 Credentials
AWS_ACCESS_KEY=minio_access_key
AWS_SECRET_KEY=minio_secret_key

# App Configuration
API_HOST=localhost
API_PORT=8080
API_URL=http://localhost:8080

# Node Environment
NODE_ENV=development
```

### Frontend Configuration (`PetBuddyApp/app.config.js`)

```javascript
export default {
  expo: {
    name: 'PetBuddy',
    slug: 'petbuddy',
    version: '1.0.0',
    platforms: ['ios', 'android', 'web'],
    
    plugins: [
      ['expo-location', {
        locationAlwaysAndWhenInUsePermission: "Allow PetBuddy to access your location",
      }],
      ['expo-image-picker', {
        photosPermission: "Allow PetBuddy to access your photos",
        cameraPermission: "Allow PetBuddy to access your camera",
      }],
    ],
    
    extra: {
      eas: {
        projectId: 'xxx',
      },
    },
  },
};
```

---

## Deployment & Infrastructure

### Local Development Setup

**Docker Compose Stack:**

```yaml
version: '3.8'

services:
  # Databases
  postgres:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_PASSWORD: password
      POSTGRES_INITDB_ARGS: "--encoding=UTF8"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/01-init.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  # Message Broker
  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  # Object Storage
  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data

  # Java Services (Built Locally)
  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_PASSWORD: password
      GRPC_CLIENT_PROFILE_SERVICE_ADDRESS: static://profile-service:9090
      GRPC_CLIENT_SOCIAL_SERVICE_ADDRESS: static://social-service:9091
      REDIS_HOST: redis
      RABBITMQ_HOST: rabbitmq
    depends_on:
      - postgres
      - redis
      - rabbitmq

  profile-service:
    build: ./profile-rescue-svc/demo
    ports:
      - "9090:9090"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/petbuddy_profile
      SPRING_DATASOURCE_PASSWORD: password
      REDIS_HOST: redis
    depends_on:
      - postgres
      - redis

  social-service:
    build: ./social-stream-svc
    ports:
      - "9091:9091"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/petbuddy_social
      SPRING_DATASOURCE_PASSWORD: password
      REDIS_HOST: redis
      RABBITMQ_HOST: rabbitmq
      AWS_S3_ENDPOINT: http://minio:9000
    depends_on:
      - postgres
      - redis
      - rabbitmq
      - minio

volumes:
  postgres_data:
  redis_data:
  minio_data:
```

### Monitoring Stack

**Components:**
- **Prometheus** (Port 9090) - Metrics collection
- **Grafana** (Port 3000) - Visualization dashboard
- **Micrometer** - Metrics library for Spring Boot

**Key Metrics to Monitor:**

```
JVM Metrics:
├── jvm_memory_used_bytes - Heap memory usage
├── jvm_gc_pause_seconds - GC pause times
├── jvm_threads_live - Active thread count
└── jvm_process_uptime_seconds - Service uptime

Application Metrics:
├── http_server_requests_seconds - API response times
├── grpc_server_handled_total - gRPC request counts
├── db_hikaricp_connections_active - Database connection pool
└── spring_data_repository_invocations_seconds - Query times

Cache Metrics:
├── redis_commands_processed_total - Redis operations
└── cache_gets_total / cache_puts_total - Cache hit/miss ratio

Message Broker:
├── rabbitmq_published_total - Events published
└── spring_rabbitmq_listener_processed - Events consumed
```

**Grafana Dashboard:**
- Import ID: 4701 (JVM Micrometer)
- Show: Memory usage, GC times, request latencies
- Alerts: Heap > 80%, GC pause > 100ms, request latency > 500ms

### Performance Optimization

#### JVM Tuning for 8GB RAM Laptop

```bash
# api-gateway (256MB max heap)
java -Xms128m -Xmx256m \
     -XX:+UseSerialGC \
     -XX:+TieredCompilation \
     -XX:TieredStopAtLevel=1 \
     -XX:MaxMetaspaceSize=128m \
     -XX:+AlwaysPreTouch \
     -jar api-gateway.jar

# profile-rescue-svc (256MB max heap)
java -Xms128m -Xmx256m ... -jar profile-rescue-svc.jar

# social-stream-svc (256MB max heap)
java -Xms128m -Xmx256m ... -jar social-stream-svc.jar

# Total Memory: ~1GB (7GB left for OS + monitoring)
```

#### Database Optimization

```sql
-- Connection Pooling
-- HikariCP Config: maxPoolSize=10, minIdle=2

-- Query Optimization
CREATE INDEX idx_posts_created_desc ON posts(created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(recipient_user_id, is_read);
CREATE INDEX idx_likes_unique ON likes(post_id, user_id);

-- Analyze Query Plans
EXPLAIN ANALYZE SELECT * FROM posts ORDER BY created_at DESC LIMIT 20;
```

#### Caching Strategy

```javascript
// Redis Cache TTL Configuration
Feed Cache: 5 minutes
User Profile: 30 minutes
Notifications: Not cached (always fresh)
Pet List: 1 hour
Incident List: 5 minutes
```

#### API Response Time Targets

```
P50 (Median): < 100ms
P95: < 300ms
P99: < 500ms
Max: < 1000ms

Per Endpoint:
├── /graphql (GetFeed) - Target: 50ms (cached)
├── /graphql (GetNotifications) - Target: 100ms (DB query)
├── /graphql (CreatePost) - Target: 200ms (DB + S3 + RabbitMQ)
└── /graphql (GetUserPosts) - Target: 80ms (cached or DB)
```

---

## Summary Table: Key Technologies & Their Usage

| Component | Technology | Version | Usage | Priority |
|-----------|-----------|---------|-------|----------|
| **API Layer** | Spring Boot + GraphQL | 3.4.1 | Request handling | Critical |
| **Service Communication** | gRPC | 1.77.1 | Inter-service communication | Critical |
| **Database** | PostgreSQL | 15+ | Persistent data storage | Critical |
| **Cache** | Redis | 7+ | Performance optimization | High |
| **Queue** | RabbitMQ | 3.12+ | Event-driven notifications | High |
| **File Storage** | MinIO/S3 | Latest | Media uploads | Medium |
| **Frontend** | React Native + Expo | Latest | Mobile & web client | Critical |
| **Auth** | Firebase | Latest | User authentication | Critical |
| **Logging** | SLF4J + Logback | Latest | Debugging & analysis | Medium |

---

