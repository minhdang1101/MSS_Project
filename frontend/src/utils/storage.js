const TOKEN_KEY = 'dlms_token'
const USER_KEY = 'dlms_user'
const ROLE_KEY = 'dlms_role'
const USERNAME_KEY = 'dlms_username'

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  getUser: () => localStorage.getItem(USER_KEY),
  getRole: () => localStorage.getItem(ROLE_KEY),
  getUsername: () => localStorage.getItem(USERNAME_KEY),
  set: (token, user, role, username) => {
    localStorage.setItem(TOKEN_KEY, token)
    if (user) localStorage.setItem(USER_KEY, user)
    if (role) localStorage.setItem(ROLE_KEY, role)
    if (username) localStorage.setItem(USERNAME_KEY, username)
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(ROLE_KEY)
    localStorage.removeItem(USERNAME_KEY)
  },
}
