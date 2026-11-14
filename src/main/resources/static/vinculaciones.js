/* Comparte las vistas de vinculaciones sin presumir acogimiento ni adopción formalizada. */
const Vinculaciones = {
  estados: {
    PENDIENTE_RESPUESTA: 'Pendiente de respuesta familiar',
    RESPUESTA_ACEPTADA: 'Respuesta aceptada',
    RESPUESTA_RECHAZADA_PENDIENTE_CIERRE: 'Respuesta rechazada — pendiente de cierre institucional'
  },
  async api(ruta, opciones = {}) {
    let r;
    try { r = await fetch(ruta, {cache: 'no-store', ...opciones, headers: Sesion.encabezados({'Content-Type': 'application/json'})}); }
    catch { throw new Error('No pudimos conectar con el servidor. Inténtalo nuevamente.'); }
    if (r.status === 401) { Sesion.salir(); throw new Error('La sesión venció.'); }
    let datos;
    try { datos = await r.json(); }
    catch { throw new Error('No pudimos completar la consulta. Inténtalo nuevamente.'); }
    if (!r.ok) throw new Error(datos.mensaje || 'No pudimos completar la consulta. Inténtalo nuevamente.');
    return datos;
  },
  estado(v) {
    const rechazado = v.estado === 'RESPUESTA_RECHAZADA_PENDIENTE_CIERRE';
    const icono = v.estado === 'PENDIENTE_RESPUESTA' ? 'reloj' : rechazado ? 'advertencia' : 'comprobar';
    return `<span class="chip ${rechazado ? 'retirado' : v.estado === 'PENDIENTE_RESPUESTA' ? 'vinculacion-pendiente' : ''}">${Profesionales.icono(icono)}${limpiarHtml(this.estados[v.estado] || v.estado)}</span>`;
  },
  familiar(lista) {
    if (!lista.length) return `<section class="tarjeta"><h2>${Profesionales.icono('vinculacion')}Mi vinculación</h2><p>Por ahora no tienes vinculaciones registradas.</p></section>`;
    return lista.map(v => `<section class="tarjeta">
      <div class="pro-fila-titulo"><h2>${Profesionales.icono('vinculacion')} ${limpiarHtml(v.codigo)}</h2>${this.estado(v)}</div>
      <p>${Profesionales.icono('personas')} Expediente familiar: ${limpiarHtml(v.expedienteFamiliar)}</p>
      <p>${Profesionales.icono('calendario')} Propuesta enviada por el Comité: ${limpiarHtml(Profesionales.fecha(v.fechaCreacion))}</p>
      <p>${limpiarHtml(v.siguientePaso)}</p>
      ${v.fechaRespuesta ? `<p>Respuesta registrada: ${limpiarHtml(Profesionales.fecha(v.fechaRespuesta))}</p>` : ''}
      <p class="ayuda">${Profesionales.icono('informacion')} La respuesta en este portal no sustituye las formalidades legales ni autoriza el acogimiento preadoptivo.</p>
      ${v.estado === 'PENDIENTE_RESPUESTA' ? `<div class="pro-acciones">
        <button class="boton" data-responder="ACEPTAR" data-id="${v.id}">${Profesionales.icono('comprobar')}Aceptar propuesta</button>
        <button class="boton secundario" data-responder="RECHAZAR" data-id="${v.id}">${Profesionales.icono('cerrar')}Rechazar propuesta</button>
      </div>` : ''}
    </section>`).join('');
  },
  enlazarRespuestas(sesion) {
    document.querySelectorAll('[data-responder]').forEach(b => b.onclick = () => {
      if (document.querySelector('dialog[data-respuesta]')) return;
      const aceptar = b.dataset.responder === 'ACEPTAR', d = document.createElement('dialog');
      d.className = 'pro-dialogo'; d.dataset.respuesta = ''; d.setAttribute('aria-labelledby', 'tituloRespuesta');
      d.innerHTML = `<div class="pro-modal-cabecera"><span class="pro-simbolo">${Profesionales.icono(aceptar ? 'comprobar' : 'advertencia')}</span><h2 id="tituloRespuesta">${aceptar ? 'Aceptar' : 'Rechazar'} propuesta</h2></div>
        <p>${aceptar ? 'Se registrará tu aceptación y el equipo institucional te orientará sobre los siguientes pasos. Esto no formaliza una adopción.' : 'Se registrará tu negativa y se informará al Comité. La vinculación quedará pendiente de cierre institucional. No cambia tu idoneidad ni tu puntuación.'}</p>
        <p role="alert" data-error></p><div class="pro-acciones"><button class="boton secundario" data-cancelar>Cancelar</button><button class="boton" data-confirmar>${Profesionales.icono(aceptar ? 'comprobar' : 'cerrar')}Confirmar respuesta</button></div>`;
      document.body.append(d); d.onclose = () => d.remove();
      d.querySelector('[data-cancelar]').onclick = () => d.close();
      d.querySelector('[data-confirmar]').onclick = async e => {
        const confirmar = e.currentTarget, cancelar = d.querySelector('[data-cancelar]');
        confirmar.disabled = cancelar.disabled = true;
        const impedirCierre = evento => evento.preventDefault();
        d.addEventListener('cancel', impedirCierre);
        try {
          await this.api(`/api/vinculaciones/${b.dataset.id}/respuesta`, {method: 'POST', body: JSON.stringify({respuesta: b.dataset.responder})});
          d.close(); Familia.version = null; await Familia.mostrar(sesion);
        } catch (error) {
          d.querySelector('[data-error]').textContent = error.message || 'No pudimos guardar el cambio. Inténtalo nuevamente.';
          confirmar.disabled = cancelar.disabled = false;
          d.removeEventListener('cancel', impedirCierre);
        }
      };
      d.showModal();
    });
  },
  async iniciar(s) {
    Profesionales.iniciar(s, 'vinculaciones');
    const c = document.getElementById('contenido');
    c.innerHTML = '<p role="status">Cargando vinculaciones…</p>';
    try {
      const id = new URLSearchParams(location.search).get('id');
      if (id) { await this.detalle(c, id, s); return; }
      c.innerHTML = Profesionales.encabezado('Vinculaciones', 'Consulta las asignaciones y las respuestas familiares.', 'vinculacion') + `
        <section class="tarjeta"><form id="filtroVinculaciones">
          <label>Estado<select name="estado"><option value="">Todos los estados</option>${Object.entries(this.estados).map(([k, v]) => `<option value="${k}">${v}</option>`).join('')}</select></label>
          <label>Buscar por identificador o expediente<input name="buscar" type="search" maxlength="80"></label>
          <button class="boton" type="submit">${Profesionales.icono('buscar')}Buscar</button>
        </form></section><div id="listaVinculaciones" aria-live="polite"></div>`;
      const form = document.getElementById('filtroVinculaciones');
      let consulta = 0;
      const cargar = async () => {
        const actual = ++consulta, lista = document.getElementById('listaVinculaciones');
        try {
          const datos = await this.api('/api/vinculaciones?' + new URLSearchParams(new FormData(form)));
          if (actual !== consulta) return;
          lista.innerHTML = datos.length ? datos.map(v => `<article class="tarjeta">
            <div class="pro-fila-titulo"><h2>${Profesionales.icono('vinculacion')}${limpiarHtml(v.codigo)}</h2>${this.estado(v)}</div>
            <p>${Profesionales.icono('carpeta')} ${limpiarHtml(v.expedienteInfantil)} · ${Profesionales.icono('personas')} ${limpiarHtml(v.expedienteFamiliar)}</p>
            <p>${Profesionales.icono('calendario')}${limpiarHtml(Profesionales.fecha(v.fechaCreacion))}</p>
            <a class="boton secundario" href="vinculaciones.html?id=${v.id}">${Profesionales.icono('consulta')}Ver detalle</a>
          </article>`).join('') : `<section class="tarjeta"><h2>${Profesionales.icono('informacion')}Sin vinculaciones</h2><p>No hay vinculaciones para esta consulta.</p></section>`;
        } catch (e) {
          if (actual === consulta) lista.innerHTML = `<section class="tarjeta"><p role="alert">${Profesionales.icono('advertencia')}${limpiarHtml(e.message)}</p></section>`;
        }
      };
      form.onsubmit = e => { e.preventDefault(); Profesionales.marcarGuardado(form); cargar(); };
      await cargar();
    } catch (e) {
      c.innerHTML = `<section class="tarjeta"><p role="alert">${Profesionales.icono('advertencia')}${limpiarHtml(e.message)}</p><a class="boton secundario" href="vinculaciones.html">Volver a vinculaciones</a></section>`;
    }
  },
  async detalle(c, id, sesion) {
    const v = await this.api('/api/vinculaciones/' + encodeURIComponent(id)), d = v.decisionComite;
    const enlaceNino = `perfiles.html?caso=${encodeURIComponent(v.expedienteInfantil)}`;
    const enlaceFamilia = `perfiles.html?familia=${encodeURIComponent(v.expedienteFamiliar)}&casoOrigen=${encodeURIComponent(v.expedienteInfantil)}&origen=match`;
    c.innerHTML = Profesionales.encabezado(v.codigo, 'Detalle de la vinculación', 'vinculacion') + `
      <section class="tarjeta">${this.estado(v)}<dl class="detalle-datos">
        <div><dt>Expediente infantil</dt><dd><a href="${enlaceNino}">${limpiarHtml(v.expedienteInfantil)}</a></dd></div>
        <div><dt>Expediente familiar</dt><dd>${sesion.rol === 'COMITE' ? `<a href="${enlaceFamilia}">${limpiarHtml(v.expedienteFamiliar)}</a>` : limpiarHtml(v.expedienteFamiliar)}</dd></div>
        <div><dt>Creación</dt><dd>${limpiarHtml(Profesionales.fecha(v.fechaCreacion))}</dd></div>
        <div><dt>Disponibilidad</dt><dd>${limpiarHtml(v.disponibilidad)}</dd></div>
      </dl><h2>${Profesionales.icono('solicitudes')}Decisión del Comité</h2>
      <p>Propuesta de Match aceptada para remitirla a la familia.</p><p>${limpiarHtml(d.responsable)} · ${limpiarHtml(Profesionales.fecha(d.fecha))}</p><p>${limpiarHtml(d.motivo)}</p>
      <h2>${Profesionales.icono('solicitudes')}Proceso</h2><p>${limpiarHtml(v.siguientePaso)}</p>
      ${v.fechaRespuesta ? `<p>Respuesta familiar registrada por ${limpiarHtml(v.respondidoPor)} · ${limpiarHtml(Profesionales.fecha(v.fechaRespuesta))}</p>` : ''}
      <p class="ayuda">${Profesionales.icono('informacion')}Esta vinculación no acredita acogimiento preadoptivo ni adopción formalizada.</p></section>
      <section class="tarjeta"><h2>${Profesionales.icono('reloj')}Historial</h2><ul class="familia-actividad">
        ${v.historial.map(e => `<li><strong>${limpiarHtml(e.resumen)}</strong><p>${Profesionales.icono('persona')}${limpiarHtml(e.responsable)} · ${limpiarHtml(Profesionales.fecha(e.fecha))}</p></li>`).join('') || '<li>La decisión del Comité se conserva en la información general. No hay movimientos adicionales registrados.</li>'}
      </ul></section><a class="boton secundario" href="vinculaciones.html">${Profesionales.icono('vinculacion')}Volver a vinculaciones</a>`;
  }
};
