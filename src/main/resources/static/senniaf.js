/* Sesión y cabecera compartidas por todas las pantallas. */
const ROLES = {
  FAMILIA: 'Familia adoptante',
  TRABAJADOR_SOCIAL: 'Trabajador social',
  PSICOLOGO: 'Psicólogo',
  COMITE: 'Comité de Asignación'
};
const limpiarHtml = (valor) => String(valor ?? '').replace(/[&<>"']/g,
  c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const Sesion = {
  leer() { return JSON.parse(sessionStorage.getItem('senniaf.sesion') || 'null'); },
  async salir() {
    const s = this.leer();
    if (s?.token) {
      try { await fetch('/api/auth/logout', { method: 'POST', headers: { Authorization: 'Bearer ' + s.token } }); }
      catch (_) { /* permitir cierre local si no hay conexión */ }
    }
    sessionStorage.removeItem('senniaf.sesion'); location.href = 'index.html';
  },
  encabezados(extra = {}) {
    const s = this.leer();
    return { ...extra, ...(s?.token ? { Authorization: 'Bearer ' + s.token } : {}) };
  },

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
      `<span>${limpiarHtml(s.nombre)}</span><span class="chip">${limpiarHtml(ROLES[s.rol])}</span>${enlaces}` +
      `<button class="enlace" id="btnSalir" type="button">Cerrar sesión</button>`;
    document.getElementById('btnSalir').addEventListener('click', () => Sesion.salir());
  },


  /* A dónde debe ir una familia al iniciar sesión. */
  async destinoFamilia(s) {
    try {
      const r = await fetch('/api/familias/mi-perfil', { headers: this.encabezados() });
      if (r.ok) return 'match.html';
      if (r.status === 404) return 'perfil.html';
    } catch (_) { /* sin conexión: no se asume que exista una entrevista */ }
    return 'perfil.html';
  }
};
