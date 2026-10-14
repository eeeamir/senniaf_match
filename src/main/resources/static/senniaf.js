/* Sesión y cabecera compartidas por todas las pantallas. */
const ROLES = {
  FAMILIA: 'Familia adoptante',
  TRABAJADOR_SOCIAL: 'Trabajador social',
  PSICOLOGO: 'Psicólogo',
  COMITE: 'Comité de Asignación'
};

const Sesion = {
  leer() { try { return JSON.parse(sessionStorage.getItem('senniaf.sesion') || 'null'); } catch (_) { return null; } },
  esc(x) {return String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));},
  headers(extra = {}) { const s = this.leer(); return { ...extra, ...(s?.token ? { Authorization: 'Bearer ' + s.token } : {}) }; },
  async salir() { const s=this.leer(); if(s?.token) { try {await fetch('/api/auth/salir',{method:'POST',headers:this.headers()});}catch(_){} }
    sessionStorage.removeItem('senniaf.sesion'); location.href='index.html'; },

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
      `<span>${this.esc(s.nombre)}</span><span class="chip">${this.esc(ROLES[s.rol]||'')}</span>${enlaces}` +
      `<button class="enlace" id="btnSalir" type="button">Cerrar sesión</button>`;
    document.getElementById('btnSalir').addEventListener('click', () => Sesion.salir());
  },


  /* A dónde debe ir una familia al iniciar sesión. */
  async destinoFamilia(s) {
    try {
      const r = await fetch('/api/familias/mi-perfil', { headers: this.headers() });
      if (r.ok) return 'match.html';
      if (r.status === 404) return 'perfil.html';
    } catch (_) { /* sin conexión: no se asume que exista una entrevista */ }
    return 'perfil.html';
  }
};
