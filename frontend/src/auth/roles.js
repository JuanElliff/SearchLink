// Mapea un rol a su home. Única fuente de verdad para el ruteo por rol.
export function homePathForRol(rol) {
  switch (rol) {
    case 'ADMIN':
      return '/admin'
    case 'OPERADOR':
      return '/operador'
    case 'ESTANDAR':
      return '/'
    default:
      return '/login'
  }
}
