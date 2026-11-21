/* Solicitudes durante la vinculación; permisos, agenda y transiciones se validan en el servidor. */
const Citas = {
  motivos: {
    ORIENTACION: ['Orientación durante la vinculación', 'Deseo solicitar una cita para recibir orientación sobre mi proceso de vinculación'],
    SEGUIMIENTO: ['Seguimiento de la vinculación', 'Me gustaría solicitar una cita para conversar sobre el seguimiento de mi proceso de vinculación'],
    ENCUENTRO: ['Coordinación de un encuentro', 'Deseo solicitar una cita para coordinar un encuentro relacionado con mi proceso de vinculación'],
    OBSERVACION: ['Consulta sobre una observación', 'Me gustaría aclarar una observación relacionada con mi proceso de vinculación'],
    DUDAS: ['Aclaración de dudas', 'Deseo solicitar una cita para resolver algunas dudas relacionadas con mi proceso de vinculación'],
    OTRO: ['Otro motivo', ''],
  },
  rechazos: {
    SIN_DISPONIBILIDAD: ['Sin disponibilidad en la fecha propuesta', 'Por el momento no contamos con disponibilidad para la fecha y hora solicitadas. Puede presentar otra propuesta'],
    COORDINACION_PREVIA: ['Es necesario coordinar previamente', 'Antes de confirmar esta cita, es necesario realizar una coordinación previa. Le mantendremos informado por los medios disponibles en el sistema'],
    NO_ATENDER: ['No es posible atender la solicitud por el momento', 'Por el momento no es posible atender esta solicitud. Le recomendamos comunicarse nuevamente mediante una nueva solicitud cuando corresponda'],
    OTRO: ['Otro motivo', ''],
  },
  cambios: {
    SIN_DISPONIBILIDAD: ['Sin disponibilidad en el horario solicitado', 'No contamos con disponibilidad en la fecha solicitada. Le proponemos realizar la cita el {momento}'],
    AJUSTE_AGENDA: ['Ajuste de agenda', 'Por motivos de agenda, le proponemos una nueva fecha para su cita: {momento}'],
    NUEVO_HORARIO: ['Coordinación de un nuevo horario', 'Hemos revisado su solicitud y le proponemos realizar la cita el {momento} Por favor, confirme si tiene disponibilidad'],
    OTRO: ['Otro motivo', ''],
  },
  estados: {
    PENDIENTE: ['Pendiente', 'reloj'], CONFIRMADA: ['Confirmada', 'comprobar'],
    CAMBIO_PROPUESTO: ['Cambio propuesto', 'calendario'], RECHAZADA: ['Rechazada', 'cerrar'],
    CANCELADA: ['Cancelada', 'cerrar'], REALIZADA: ['Realizada', 'comprobar'],
  },
  acciones: {
    ACEPTAR: ['Aceptar', 'Confirmar cita', 'Confirmar cita'],
    RECHAZAR: ['Rechazar', 'Rechazar solicitud', 'Confirmar rechazo'],
    PROPONER: ['Proponer otra fecha', 'Proponer nueva fecha', 'Enviar propuesta'],
    ACEPTAR_PROPUESTA: ['Aceptar nueva fecha', 'Aceptar nueva fecha', 'Confirmar aceptación'],
    OTRO_HORARIO: ['Solicitar otro horario', 'Solicitar otro horario', 'Enviar propuesta'],
    CANCELAR: ['Cancelar', 'Cancelar cita', 'Confirmar cancelación'],
    REALIZADA: ['Marcar realizada', 'Registrar cita realizada', 'Confirmar realización'],
  },
  async api(ruta, opciones = {}) {
    let respuesta;
    try { respuesta = await fetch(ruta, { ...opciones, cache: 'no-store', headers: Sesion.encabezados({ 'Content-Type': 'application/json' }) }); }
    catch (_) { throw new Error('No se pudo conectar. Inténtalo de nuevo.'); }
    if (respuesta.status === 401) { location.replace('index.html'); throw new Error('Inicia sesión para continuar.'); }
    let datos;
    try { datos = await respuesta.json(); } catch (_) { throw new Error('No se pudo consultar la información. Inténtalo de nuevo.'); }
    if (!respuesta.ok) {
      const error = new Error(respuesta.status >= 500 ? 'No se pudo completar la operación. Inténtalo de nuevo.' : datos.mensaje || 'No se pudo completar la operación.');
      error.estado = respuesta.status; throw error;
    }
    return datos;
  },
  async iniciar() {
    this.sesion = Sesion.requerir(['FAMILIA', 'TRABAJADOR_SOCIAL']);
    if (!this.sesion) return;
    this.familia = this.sesion.rol === 'FAMILIA';
    this.solicitudes = []; this.disponibles = [];
    Profesionales.iniciar(this.sesion, 'citas');
    const titulo = this.familia ? 'Mis citas' : 'Gestión de citas';
    const descripcion = this.familia ? 'Solicita y consulta tus citas durante la vinculación.' : 'Revisa y responde las solicitudes de las familias';
    this.contenido = document.getElementById('contenido');
    this.contenido.innerHTML = Profesionales.encabezado(titulo, descripcion, 'calendario') + `
      <p id="estadoCitas" role="status" aria-live="polite"></p>
      <div class="citas-barra"><label for="filtroCitas">Mostrar solicitudes<select id="filtroCitas">
        <option value="TODAS">Todas</option>${Object.entries(this.estados).map(([k, [n]]) => `<option value="${k}">${limpiarHtml(n)}</option>`).join('')}
      </select></label><div class="pro-acciones">${this.familia ? '<button type="button" class="boton" id="solicitarCita" aria-describedby="contextoCitas" disabled>Solicitar cita</button>' : ''}
      <button type="button" class="boton secundario" id="actualizarCitas">Actualizar</button></div></div>
      <p id="contextoCitas" class="citas-contexto"></p>
      <section aria-labelledby="tituloSolicitudes"><h2 class="pro-subtitulo" id="tituloSolicitudes">${this.familia ? 'Mis solicitudes' : 'Solicitudes de las familias'}</h2><div id="listaCitas" class="citas-lista"></div></section>`;
    this.filtro = document.getElementById('filtroCitas');
    this.filtro.value = this.familia ? 'TODAS' : 'PENDIENTE';
    this.filtro.addEventListener('change', () => this.pintar());
    document.getElementById('actualizarCitas').addEventListener('click', () => this.cargar());
    document.getElementById('solicitarCita')?.addEventListener('click', () => this.dialogo('SOLICITAR'));
    await this.cargar();
  },
  async cargar() {
    if (this.cargando) return;
    this.cargando = true; const boton = document.getElementById('actualizarCitas'); boton.disabled = true;
    try {
      [this.opciones, this.solicitudes] = await Promise.all([this.api('/api/citas/opciones'), this.api('/api/citas')]);
      const habilitadas = this.opciones.vinculaciones.filter(v => v.habilitada);
      const abiertas = new Set(this.solicitudes.filter(c => ['PENDIENTE', 'CAMBIO_PROPUESTO'].includes(c.estado)).map(c => c.vinculacionId));
      this.disponibles = habilitadas.filter(v => !abiertas.has(v.id));
      let contexto = '';
      if (!this.opciones.configurada) contexto = 'Los horarios de citas todavía no están habilitados.';
      else if (this.familia && !habilitadas.length) contexto = 'Podrás solicitar citas cuando tu vinculación esté aceptada y habilitada, con un trabajador social asignado.';
      else if (this.familia && !this.disponibles.length) contexto = 'Ya tienes una solicitud pendiente para tu vinculación. Puedes consultarla en Mis solicitudes.';
      document.getElementById('contextoCitas').textContent = contexto;
      const solicitar = document.getElementById('solicitarCita');
      if (solicitar) solicitar.disabled = !this.opciones.configurada || !this.disponibles.length;
      this.pintar();
    } catch (e) { this.aviso(e.message, true); }
    finally { this.cargando = false; boton.disabled = false; }
  },
  aviso(texto, error = false) { Profesionales.mensaje(document.getElementById('estadoCitas'), texto, error ? 'error' : 'exito'); },
  fecha(valor) { return new Intl.DateTimeFormat('es-PA', { timeZone: 'America/Panama', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(valor)); },
  hora(valor) { const [h, m] = valor.slice(0, 5).split(':').map(Number); return `${h % 12 || 12}:${String(m).padStart(2, '0')} ${h < 12 ? 'a. m.' : 'p. m.'}`; },
  horaInstante(valor) { return new Intl.DateTimeFormat('es-PA', { timeZone: 'America/Panama', hour: 'numeric', minute: '2-digit', hour12: true }).format(new Date(valor)); },
  fechaHora(valor) { return `${this.fecha(valor)} · ${this.horaInstante(valor)}`; },
  pintar() {
    const lista = document.getElementById('listaCitas');
    const visibles = this.solicitudes.filter(c => this.filtro.value === 'TODAS' || c.estado === this.filtro.value);
    if (!visibles.length) {
      lista.innerHTML = `<div class="tarjeta citas-vacio"><span class="pro-simbolo">${Profesionales.icono('calendario')}</span><h2>${this.solicitudes.length ? 'No hay solicitudes con este estado' : this.familia ? 'Todavía no tienes solicitudes de cita' : 'No hay solicitudes de cita'}</h2><p>${this.solicitudes.length ? 'Selecciona otro estado para consultar las demás solicitudes.' : this.familia ? 'Aquí podrás consultar tus propuestas y las respuestas de Trabajo Social.' : 'Las solicitudes de las familias asignadas aparecerán aquí.'}</p></div>`;
      return;
    }
    lista.innerHTML = visibles.map(c => {
      const estado = this.estados[c.estado];
      return `<article class="tarjeta citas-tarjeta" id="cita-${Number(c.id)}" tabindex="-1">
        <header class="citas-cabecera"><div><p>${limpiarHtml(c.vinculacion)}${this.familia ? '' : ' · Familia ' + limpiarHtml(c.familia)}</p><h2>${limpiarHtml(this.motivos[c.motivo]?.[0] || 'Solicitud de cita')}</h2></div>
        <span class="citas-estado citas-estado-${limpiarHtml(c.estado)}">${Profesionales.icono(estado[1])}${limpiarHtml(estado[0])}</span></header>
        <dl class="citas-datos"><div><dt>${['CONFIRMADA', 'REALIZADA'].includes(c.estado) ? 'Fecha acordada' : ['PENDIENTE', 'CAMBIO_PROPUESTO'].includes(c.estado) ? 'Fecha propuesta' : 'Fecha registrada'}</dt><dd>${limpiarHtml(this.fecha(c.inicio))}</dd></div>
        <div><dt>Hora · Panamá</dt><dd>${limpiarHtml(this.hora(c.hora))} – ${limpiarHtml(this.horaInstante(c.fin))}</dd></div>
        <div><dt>Duración${['PENDIENTE', 'CAMBIO_PROPUESTO'].includes(c.estado) ? ' estimada' : ''}</dt><dd>${c.duracion === 60 ? '1 hora' : '2 horas'}</dd></div></dl>
        <div class="citas-mensaje"><strong>${c.ultimoRol === 'TRABAJADOR_SOCIAL' ? 'Respuesta de Trabajo Social' : this.familia ? 'Tu mensaje' : 'Mensaje de la familia'}</strong><p>${limpiarHtml(c.mensaje)}</p></div>
        ${c.respuesta && c.respuesta !== c.mensaje ? `<div class="citas-mensaje"><strong>Respuesta anterior de Trabajo Social</strong><p>${limpiarHtml(c.respuesta)}</p></div>` : ''}
        <div class="citas-acciones">${c.acciones.map(a => `<button type="button" class="boton ${['RECHAZAR', 'PROPONER', 'OTRO_HORARIO', 'CANCELAR'].includes(a) ? 'secundario' : ''}" data-accion="${a}" data-cita="${Number(c.id)}">${limpiarHtml(this.acciones[a][0])}</button>`).join('')}</div>
        <p class="citas-actualizada">Última actualización: ${limpiarHtml(this.fechaHora(c.actualizada))}</p>
        <details class="citas-historial" data-historial="${Number(c.id)}"><summary>Ver historial</summary><div data-eventos></div></details></article>`;
    }).join('');
    lista.querySelectorAll('[data-accion]').forEach(b => b.addEventListener('click', () => this.dialogo(b.dataset.accion, this.solicitudes.find(c => c.id === Number(b.dataset.cita)))));
    lista.querySelectorAll('[data-historial]').forEach(d => d.addEventListener('toggle', async () => {
      if (!d.open || d.dataset.cargado) return;
      d.dataset.cargado = 'si'; const bloque = d.querySelector('[data-eventos]'); bloque.textContent = 'Consultando historial…';
      try {
        const c = await this.api('/api/citas/' + Number(d.dataset.historial));
        bloque.innerHTML = `<ol class="citas-eventos">${c.historial.map(e => `<li><strong>${limpiarHtml(e.actor)} · ${limpiarHtml(this.estados[e.estado]?.[0] || e.estado)}</strong><p>${limpiarHtml(this.fechaHora(e.fechaAccion))}</p><p>${limpiarHtml(this.fechaHora(e.inicio))} – ${limpiarHtml(this.horaInstante(e.fin))} · ${e.duracion === 60 ? '1 hora' : '2 horas'}</p><p>${limpiarHtml(e.mensaje)}</p></li>`).join('')}</ol>`;
      } catch (e) { bloque.textContent = e.message; delete d.dataset.cargado; }
    }));
  },
  async dialogo(accion, cita = null) {
    const nueva = accion === 'SOLICITAR';
    const reprogramar = ['PROPONER', 'OTRO_HORARIO'].includes(accion);
    const editableFecha = nueva || reprogramar;
    const confirmarFecha = ['ACEPTAR', 'ACEPTAR_PROPUESTA'].includes(accion);
    const editarDuracion = ['ACEPTAR', 'PROPONER'].includes(accion);
    const editarMensaje = nueva || ['ACEPTAR', 'RECHAZAR', 'PROPONER', 'OTRO_HORARIO'].includes(accion);
    const motivos = nueva ? this.motivos : accion === 'RECHAZAR' ? this.rechazos : accion === 'PROPONER' ? this.cambios : null;
    const inicioFoco = document.activeElement;
    const d = document.createElement('dialog'); d.className = 'pro-dialogo citas-dialogo'; d.setAttribute('aria-labelledby', 'tituloDialogoCita');
    const titulo = nueva ? 'Solicitar cita' : this.acciones[accion][1];
    const campo = (id, nombre, control) => `<div class="citas-campo"><label for="${id}">${nombre}</label>${control}</div>`;
    const duracionInicial = editarDuracion || nueva ? 120 : cita?.duracion || 120;
    const vinInicial = nueva ? this.disponibles[0]?.id : cita.vinculacionId;
    if (!vinInicial) return;
    const vinculo = nueva && this.disponibles.length > 1 ? campo('citaVinculacion', 'Vinculación', `<select id="citaVinculacion" required>${this.disponibles.map(v => `<option value="${Number(v.id)}">${limpiarHtml(v.codigo)}</option>`).join('')}</select>`) : `<p class="citas-ayuda">${limpiarHtml(nueva ? this.disponibles[0].codigo : cita.vinculacion)}</p>`;
    d.innerHTML = `<h2 id="tituloDialogoCita">${limpiarHtml(titulo)}</h2><form novalidate><div class="citas-campos">${vinculo}
      ${motivos ? campo('citaMotivo', nueva ? 'Motivo de la cita' : accion === 'RECHAZAR' ? 'Motivo del rechazo' : 'Motivo del cambio', `<select id="citaMotivo" required><option value="">Selecciona un motivo</option>${Object.entries(motivos).map(([k, [n]]) => `<option value="${k}">${limpiarHtml(n)}</option>`).join('')}</select>`) : ''}
      ${editableFecha ? `<div class="citas-fecha-hora">${campo('citaFecha', 'Fecha propuesta', `<input type="date" id="citaFecha" min="${this.opciones.hoy}" required>`)}${campo('citaHora', 'Hora · Panamá', '<select id="citaHora" required disabled><option value="">Selecciona primero una fecha</option></select>')}</div>` : confirmarFecha ? `<div class="citas-resumen"><p><strong>${limpiarHtml(this.fecha(cita.inicio))}</strong></p><p>${limpiarHtml(this.hora(cita.hora))} · Panamá</p></div>` : `<p class="citas-ayuda">${accion === 'CANCELAR' ? 'La cancelación quedará registrada y conservará las propuestas anteriores.' : 'Confirma que la cita se realizó. La actuación quedará registrada en el historial.'}</p>`}
      ${editarDuracion ? campo('citaDuracion', 'Duración', '<select id="citaDuracion"><option value="120">2 horas</option><option value="60">1 hora</option></select>') : editableFecha || confirmarFecha ? `<p class="citas-ayuda">Duración estimada: ${duracionInicial === 60 ? '1 hora' : '2 horas'}</p>` : ''}
      ${editableFecha || confirmarFecha ? '<p id="citaFinalizacion" class="citas-ayuda" aria-live="polite"></p><p id="citaDisponibilidad" class="citas-ayuda citas-disponibilidad" role="status"></p>' : ''}
      ${editableFecha ? '<p class="citas-ayuda">La fecha y hora propuestas están sujetas a confirmación.</p>' : ''}
      ${editarMensaje ? campo('citaMensaje', 'Mensaje', '<textarea id="citaMensaje" rows="3" minlength="10" maxlength="1000" required placeholder="Describe brevemente tu solicitud"></textarea><button type="button" class="citas-restablecer" id="restablecerCita" hidden>Restablecer mensaje sugerido</button>') : ''}
      </div><p data-error role="alert"></p><div class="pro-acciones"><button class="boton secundario" type="button" data-cancelar>Cancelar</button><button class="boton" type="submit">${limpiarHtml(nueva ? 'Enviar solicitud' : this.acciones[accion][2])}</button></div></form>`;
    document.body.append(d);
    const form = d.querySelector('form'), guardar = form.querySelector('[type=submit]'), cancelar = d.querySelector('[data-cancelar]');
    const fecha = d.querySelector('#citaFecha'), hora = d.querySelector('#citaHora'), duracion = d.querySelector('#citaDuracion');
    const motivo = d.querySelector('#citaMotivo'), mensaje = d.querySelector('#citaMensaje'), restablecer = d.querySelector('#restablecerCita');
    const vinc = d.querySelector('#citaVinculacion'), estado = d.querySelector('#citaDisponibilidad');
    let sugerido = '', consulta = 0, enviando = false, disponible = !confirmarFecha;
    const minutos = () => Number(duracion?.value || duracionInicial);
    const fechaActual = () => fecha?.value || cita?.fecha;
    const horaActual = () => hora?.value || (editableFecha ? '' : cita?.hora.slice(0, 5));
    const momento = () => fechaActual() && horaActual() ? `${this.fecha(fechaActual() + 'T12:00:00-05:00')} a las ${this.hora(horaActual())}` : '';
    const propuesta = () => {
      if (nueva) return this.motivos[motivo.value]?.[1] || '';
      if (accion === 'RECHAZAR') return this.rechazos[motivo.value]?.[1] || '';
      if (accion === 'OTRO_HORARIO') return 'Gracias por la propuesta. Me gustaría consultar la posibilidad de realizar la cita en otra fecha y hora';
      if (accion === 'ACEPTAR') return `Su solicitud de cita ha sido confirmada para el ${momento()} Le esperamos`;
      return momento() ? (this.cambios[motivo?.value]?.[1] || '').replace('{momento}', momento()) : '';
    };
    const actualizarMensaje = () => {
      if (mensaje) {
        const nuevo = propuesta();
        if (!mensaje.value.trim() || mensaje.value === sugerido) mensaje.value = nuevo;
        sugerido = nuevo; restablecer.hidden = !nuevo || mensaje.value === nuevo;
      }
      const fin = d.querySelector('#citaFinalizacion');
      if (fin) {
        const h = horaActual();
        if (!h) fin.textContent = '';
        else {
          const [hh, mm] = h.split(':').map(Number), total = hh * 60 + mm + minutos();
          fin.textContent = total < 1440 ? 'Finalización: ' + this.hora(`${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`) : 'El intervalo debe finalizar dentro del día seleccionado.';
        }
      }
    };
    const cargarHoras = async () => {
      if (!editableFecha && !confirmarFecha) return;
      const turno = ++consulta, f = fechaActual(), anterior = horaActual(); disponible = false;
      if (hora) { hora.disabled = true; hora.innerHTML = '<option value="">Selecciona una hora</option>'; }
      if (!f || (fecha && !fecha.validity.valid)) { estado.textContent = ''; actualizarMensaje(); return; }
      estado.textContent = 'Consultando horarios…'; guardar.disabled = true;
      try {
        const horas = await this.api(`/api/citas/disponibilidad?vinculacionId=${Number(vinc?.value || vinInicial)}&fecha=${encodeURIComponent(f)}&duracion=${minutos()}`);
        if (turno !== consulta || !d.isConnected) return;
        if (hora) {
          hora.innerHTML = '<option value="">Selecciona una hora</option>' + horas.map(h => `<option value="${h}">${this.hora(h)}</option>`).join('');
          hora.disabled = !horas.length;
          if (horas.includes(anterior)) hora.value = anterior;
          disponible = !!horas.length;
          estado.textContent = horas.length ? '' : 'No hay horarios disponibles para esa fecha y duración. Selecciona otra fecha.';
        } else {
          disponible = horas.includes(anterior);
          estado.textContent = disponible ? '' : 'La fecha seleccionada ya no está disponible. Puedes solicitar o proponer otro horario.';
        }
      } catch (e) { if (turno === consulta) estado.textContent = e.message; }
      finally { if (turno === consulta && d.isConnected) { guardar.disabled = !disponible; actualizarMensaje(); } }
    };
    motivo?.addEventListener('change', actualizarMensaje);
    mensaje?.addEventListener('input', () => { restablecer.hidden = !sugerido || mensaje.value === sugerido; });
    restablecer?.addEventListener('click', () => { mensaje.value = sugerido; mensaje.dispatchEvent(new Event('input', { bubbles: true })); mensaje.focus(); });
    hora?.addEventListener('change', actualizarMensaje);
    fecha?.addEventListener('change', cargarHoras); duracion?.addEventListener('change', cargarHoras); vinc?.addEventListener('change', cargarHoras);
    cancelar.addEventListener('click', () => d.close());
    d.addEventListener('keydown', e => {
      if (e.key !== 'Tab') return;
      const controles = [...d.querySelectorAll('input,select,textarea,button,[tabindex]')]
        .filter(el => !el.disabled && el.tabIndex >= 0 && el.getClientRects().length);
      const primero = controles[0], ultimo = controles[controles.length - 1];
      if (!primero) return;
      if (e.shiftKey && document.activeElement === primero) { e.preventDefault(); ultimo.focus(); }
      else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primero.focus(); }
    });
    d.addEventListener('cancel', e => { if (enviando) e.preventDefault(); });
    d.addEventListener('close', () => { consulta++; d.remove(); if (inicioFoco?.isConnected) inicioFoco.focus(); }, { once: true });
    CamposFormulario.iniciar(form);
    form.addEventListener('submit', async e => {
      e.preventDefault();
      if (enviando || !CamposFormulario.revisar(form) || ((editableFecha || confirmarFecha) && !disponible)) return;
      if (mensaje && (/\[(fecha|hora)\]/i.test(mensaje.value) || mensaje.value.trim().length < 10)) { d.querySelector('[data-error]').textContent = 'Escribe un mensaje válido antes de continuar.'; mensaje.focus(); return; }
      enviando = true; guardar.disabled = true; cancelar.disabled = true; form.setAttribute('aria-busy', 'true');
      const datos = nueva ? { vinculacionId: Number(vinc?.value || vinInicial), motivo: motivo.value, mensaje: mensaje.value.trim(), fecha: fecha.value, hora: hora.value } : { accion, version: cita.version };
      if (!nueva) {
        if (mensaje) datos.mensaje = mensaje.value.trim();
        if (motivo) datos.motivo = motivo.value;
        if (editableFecha) { datos.fecha = fecha.value; datos.hora = hora.value; }
        if (editarDuracion) datos.duracion = minutos();
      }
      try {
        const guardada = await this.api(nueva ? '/api/citas' : `/api/citas/${Number(cita.id)}/acciones`, { method: 'POST', body: JSON.stringify(datos) });
        d.close(); await this.cargar();
        this.aviso(nueva ? 'Solicitud enviada correctamente' : ['ACEPTAR', 'ACEPTAR_PROPUESTA'].includes(accion) ? 'Cita confirmada' : ['PROPONER', 'OTRO_HORARIO'].includes(accion) ? 'Propuesta enviada' : accion === 'RECHAZAR' ? 'Solicitud rechazada' : accion === 'CANCELAR' ? 'Solicitud cancelada' : 'Cita registrada como realizada');
        document.getElementById('cita-' + Number(guardada.id))?.focus();
      } catch (error) {
        d.querySelector('[data-error]').textContent = error.message;
        if (error.estado === 409) await this.cargar();
      } finally { enviando = false; guardar.disabled = false; cancelar.disabled = false; form.removeAttribute('aria-busy'); }
    });
    d.showModal(); actualizarMensaje();
    if (confirmarFecha) await cargarHoras();
  },
};
Citas.iniciar();
