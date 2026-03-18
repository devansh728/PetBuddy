import { 
  ApolloClient, 
  InMemoryCache, 
  createHttpLink,
  ApolloLink,
  from
} from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { API_CONFIG } from '../config/firebase.config';
import { storage } from './storage';

// HTTP Link to GraphQL endpoint
const httpLink = createHttpLink({
  uri: API_CONFIG.GRAPHQL_ENDPOINT,
});

// Auth Link to inject Bearer token
const authLink = setContext(async (_, { headers }) => {
  // Get token from platform-aware storage
  let token: string | null = null;
  
  try {
    token = await storage.getItem('authToken');
  } catch (error) {
    console.warn('Failed to get auth token:', error);
  }
  
  return {
    headers: {
      ...headers,
      authorization: token ? `Bearer ${token}` : '',
    },
  };
});

// Logging Link (for development)
const loggingLink = new ApolloLink((operation, forward) => {
  if (__DEV__) {
    console.log(`[GraphQL] ${operation.operationName}`, operation.variables);
  }
  
  return forward(operation).map((response) => {
    if (__DEV__) {
      console.log(`[GraphQL Response] ${operation.operationName}`, response);
    }
    return response;
  });
});

// Apollo Client Cache Configuration
const cache = new InMemoryCache({
  typePolicies: {
    Query: {
      fields: {
        // Pagination merge for feed
        getFeed: {
          keyArgs: false,
          merge(existing, incoming) {
            if (!existing) return incoming;
            return {
              ...incoming,
              posts: [...(existing.posts || []), ...(incoming.posts || [])],
            };
          },
        },
      },
    },
    // Normalize posts by ID for cache updates
    Post: {
      keyFields: ['postId'],
    },
    UserProfile: {
      keyFields: ['userId'],
    },
    Incident: {
      keyFields: ['incidentId'],
    },
  },
  // Removed deprecated canonizeResults option (Apollo 3.14.0+)
});

// Create Apollo Client
export const apolloClient = new ApolloClient({
  link: from([loggingLink, authLink, httpLink]),
  cache,
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'cache-and-network',
      errorPolicy: 'all',
    },
    query: {
      fetchPolicy: 'cache-first',
      errorPolicy: 'all',
    },
    mutate: {
      errorPolicy: 'all',
    },
  },
});

export default apolloClient;
