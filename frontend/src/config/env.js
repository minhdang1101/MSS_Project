export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'

// Keycloak (login server). The realm and the client are defined in docker/keycloak/library-realm.json.
export const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180'
export const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM ?? 'library'
export const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'library-web'
