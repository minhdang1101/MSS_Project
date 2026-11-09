export const LIBRARIAN = 'LIBRARIAN'
export const MEMBER = 'MEMBER'

/** Where each kind of user lands after signing in. */
export const homeFor = (role) => (role === MEMBER ? '/me' : '/')
