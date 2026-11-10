import Keycloak from 'keycloak-js'
import { KEYCLOAK_CLIENT_ID, KEYCLOAK_REALM, KEYCLOAK_URL } from '../config/env.js'

/** The one Keycloak client of the web app: it signs the user in on Keycloak's page and holds the tokens. */
const keycloak = new Keycloak({ url: KEYCLOAK_URL, realm: KEYCLOAK_REALM, clientId: KEYCLOAK_CLIENT_ID })

let starting = null

/**
 * Starts Keycloak once (also under React StrictMode, which runs effects twice). With check-sso a user who already
 * signed in on Keycloak is recognised without a redirect, and a visitor is simply left signed out.
 * Resolves to true when signed in.
 */
export function startKeycloak() {
  if (!starting) {
    starting = keycloak.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false })
  }
  return starting
}

/** Keycloak's own "forgot password" page, for the link on our sign-in page. */
export const resetPasswordUrl = () =>
  `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/login-actions/reset-credentials?client_id=${KEYCLOAK_CLIENT_ID}`
  + `&redirect_uri=${encodeURIComponent(window.location.origin + '/')}`

/** What the token says about the user: name, card number or staff code, and the role. */
export function identityOf(kc) {
  const token = kc.tokenParsed ?? {}
  const roles = token.realm_access?.roles ?? []
  let role = null
  if (roles.includes('LIBRARIAN')) role = 'LIBRARIAN'
  else if (roles.includes('MEMBER')) role = 'MEMBER'
  // Keycloak keeps user names in lower case (tv-0412); the library writes card numbers in capitals
  const username = token.preferred_username ? token.preferred_username.toUpperCase() : null
  return { user: token.name ?? username, username, role }
}

export default keycloak
