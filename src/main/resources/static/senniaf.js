/* Sesión y cabecera compartidas por todas las pantallas. */
const ROLES = {
  FAMILIA: 'Familia adoptante',
  TRABAJADOR_SOCIAL: 'Trabajador social',
  PSICOLOGO: 'Psicólogo',
  COMITE: 'Comité de Asignación'
};

const Sesion = {
  leer() { return JSON.parse(sessionStorage.getItem('senniaf.sesion') || 'null'); },
  salir() { sessionStorage.removeItem('senniaf.sesion'); location.href = 'index.html'; },

  /* Pantalla de inicio de cada rol. */
  inicio(s) {
    if (s.rol === 'COMITE' || s.tipo === 'FAMILIA') return 'match.html';
    return 'funcionario.html';
  },

  /* Devuelve la sesión si el rol puede estar en esta página; si no, redirige. */
  requerir(rolesPermitidos) {
    const s = this.leer();
    if (!s) { location.replace('index.html'); return null; }
    if (!rolesPermitidos.includes(s.rol)) { location.replace(this.inicio(s)); return null; }
    return s;
  },

  /* Nombre, rol, enlaces opcionales y botón de salida en la cabecera. */
  pintarCabecera(s, enlaces = '') {
    document.getElementById('usuario').innerHTML =
      `<span>${s.nombre}</span><span class="chip">${ROLES[s.rol]}</span>${enlaces}` +
      `<button class="enlace" id="btnSalir" type="button">Cerrar sesión</button>`;
    document.getElementById('btnSalir').addEventListener('click', () => Sesion.salir());
  },

  /* Marca local de “ya completó su entrevista” (respaldo si el servidor no responde). */
  marcarEntrevista(id) { localStorage.setItem('senniaf.entrevista.' + id, '1'); },

  /* A dónde debe ir una familia al iniciar sesión. */
  async destinoFamilia(s) {
    try {
      const r = await fetch('/api/familias/mi-perfil', { headers: { 'X-User-Role': 'FAMILIA', 'X-User-Id': s.id } });
      if (r.ok) { this.marcarEntrevista(s.id); return 'match.html'; }
      if (r.status === 404) return 'perfil.html';
    } catch (_) { /* sin conexión: se usa la marca local */ }
    return localStorage.getItem('senniaf.entrevista.' + s.id) ? 'match.html' : 'perfil.html';
  }
};
