/* Sesión y cabecera compartidas por todas las pantallas. */
const ROLES = {
  FAMILIA: 'Familia adoptante',
  TRABAJADOR_SOCIAL: 'Trabajador social',
  PSICOLOGO: 'Psicólogo',
  COMITE: 'Comité de Asignación'
};
const limpiarHtml = (valor) => String(valor ?? '').replace(/[&<>"']/g,
  c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const FACTORES = {
  edad:     { n: 'Rango de edad', max: 20, origen: 'Preferencia declarada' },
  hermanos: { n: 'Vínculos entre hermanos', max: 25, origen: 'Necesidad del niño o niña' },
  salud:    { n: 'Salud y desarrollo', max: 35, origen: 'Capacidad declarada y necesidades' },
  apoyo:    { n: 'Red de apoyo', max: 15, origen: 'Declaración de la familia' },
  logistica:{ n: 'Logística', max: 5, origen: 'Disponibilidad para visitas' }
};


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


  htmlDecision(d,codigo) {
    const contexto=d.contextoScoring;
    const tabla=contexto ? `<details><summary>Fundamento conservado · versión ${d.versionPesos}</summary><div class="tabla"><table><thead><tr><th>Criterio</th><th>Puntos</th><th>Peso utilizado</th><th>Razón registrada</th></tr></thead><tbody>${Object.entries(contexto.factores).map(([k,f])=>`<tr><td>${limpiarHtml(FACTORES[k]?.n || k)}</td><td>${f[0]}</td><td>${contexto.pesos[k]}</td><td>${limpiarHtml(f[1])}</td></tr>`).join('')}</tbody></table></div></details>` : '<p class="mensaje aviso">Registro anterior: se conservan puntaje y versión, pero no se almacenó el desglose ni la configuración completa. No se han reconstruido.</p>';
    return `<li><details><summary><strong>${limpiarHtml(d.familia)} · ${limpiarHtml(typeof Profesionales==='undefined'?d.decision:Profesionales.etiqueta(d.decision))}</strong> · ${limpiarHtml(typeof Profesionales==='undefined'?d.fecha:Profesionales.fecha(d.fecha))} · ${d.puntaje} puntos · pesos v${d.versionPesos}</summary><p>Registrada por ${limpiarHtml(d.por)}. Se muestra el análisis utilizado al decidir.</p><p>${limpiarHtml(d.motivo)}</p>${tabla}<a href="perfiles.html?familia=${encodeURIComponent(d.familia)}&casoOrigen=${encodeURIComponent(codigo)}&origen=match">Consultar perfil familiar actual</a></details></li>`;
  },

  async cargarPendientes(id) {
    const contenedor = document.getElementById(id);
    if (!contenedor || this.leer()?.rol !== 'COMITE') return;
    contenedor.hidden = false;
    try {
      const r = await fetch('/api/ninos/solicitudes-estado/pendientes', {headers:this.encabezados()});
      if (!r.ok) throw new Error('No se pudieron consultar las solicitudes pendientes.');
      const datos = await r.json();
      contenedor.innerHTML = `<details ${datos.length ? 'open' : ''}><summary><strong>Solicitudes pendientes · ${datos.length}</strong></summary>` +
        (datos.length ? `<ul>${datos.map(s => `<li><a href="perfiles.html?caso=${encodeURIComponent(s.codigoCaso)}&origen=match#panelSolicitudes">${limpiarHtml(s.codigoCaso)} · ${s.tipo === 'DESACTIVACION' ? 'Desactivación del Match' : 'Reactivación en el Match'}</a> · ${limpiarHtml(s.solicitante)}</li>`).join('')}</ul>` : '<p>No hay solicitudes pendientes de revisión.</p>') + '</details>';
    } catch(e) { contenedor.textContent=e.message; }
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
