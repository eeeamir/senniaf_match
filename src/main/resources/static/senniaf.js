/* Sesión y cabecera compartidas por todas las pantallas. */
const ROLES = {
  FAMILIA: 'Familia adoptante',
  TRABAJADOR_SOCIAL: 'Trabajador social',
  PSICOLOGO: 'Psicólogo',
  COMITE: 'Comité de Asignación'
};
const limpiarHtml = (valor) => String(valor ?? '').replace(/[&<>"']/g,
  c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

/* Mensajes junto al control; conserva las respuestas y las descripciones accesibles. */
const CamposFormulario = {
  error(campo,mensaje) {
    if(!campo)return;
    const grupo=campo.closest('.campo')||campo.parentElement;
    const id='error-'+(campo.id||campo.closest('[data-grupo]')?.dataset.grupo||campo.name);
    let aviso=document.getElementById(id);
    if(!aviso){aviso=document.createElement('span');aviso.id=id;aviso.className='campo-error pro-error-campo';aviso.setAttribute('role','alert');
      if(campo.closest('.campo'))grupo.append(aviso);else campo.insertAdjacentElement('afterend',aviso);}
    aviso.textContent=mensaje||'';aviso.hidden=!mensaje;
    campo.setAttribute('aria-invalid',String(Boolean(mensaje)));
    const descripciones=new Set((campo.getAttribute('aria-describedby')||'').split(/\s+/).filter(Boolean));
    if(mensaje)descripciones.add(id);else descripciones.delete(id);
    if(descripciones.size)campo.setAttribute('aria-describedby',[...descripciones].join(' '));else campo.removeAttribute('aria-describedby');
    if(mensaje)campo.closest('details')?.setAttribute('open','');
  },
  iniciar(form) {
    if(!form||form.dataset.camposPreparados)return;form.dataset.camposPreparados='true';
    form.addEventListener('invalid',e=>this.error(e.target,e.target.validationMessage),true);
    form.addEventListener('input',e=>{const c=e.target;if(!c.matches('input,textarea,select'))return;
      if(c.dataset.errorBasico){c.setCustomValidity('');delete c.dataset.errorBasico;}
      this.error(c,'');});
    form.addEventListener('change',e=>{if(e.target.matches('input,textarea,select'))this.error(e.target,'');});
  },
  revisar(form) {
    this.iniciar(form);
    form.querySelectorAll('input,textarea,select').forEach(c=>{
      if(c.disabled||['checkbox','radio','hidden','button','submit'].includes(c.type))return;
      if(c.dataset.errorBasico){c.setCustomValidity('');delete c.dataset.errorBasico;}
      let error='';
      if(c.required&&!c.value.trim())error=c.validity.badInput?'Escribe un número válido.':'Completa este campo.';
      else if(c.type==='number'&&c.value!==''){
        if(c.validity.rangeUnderflow)error='Usa un valor igual o mayor que '+c.min+'.';
        else if(c.validity.rangeOverflow)error='Usa un valor igual o menor que '+c.max+'.';
        else if(!/^[0-9]+$/.test(c.value))error='Usa un número entero, sin letras ni decimales.';
      }
      if(error){c.setCustomValidity(error);c.dataset.errorBasico='true';}
      else if(c.validity.valid)this.error(c,'');
    });
    const valido=form.checkValidity();
    if(!valido)form.reportValidity();
    return valido;
  },
  servidor(form,errores,mapeo={}) {
    let primero=null;
    Object.entries(errores||{}).forEach(([id,mensaje])=>{
      const c=document.getElementById(mapeo[id]||id);
      if(c&&form.contains(c)){this.error(c,mensaje);primero=primero||c;}
    });
    primero?.focus();
  }
};

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
