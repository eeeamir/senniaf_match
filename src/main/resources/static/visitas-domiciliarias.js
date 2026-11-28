/* Las visitas se muestran según su fecha y el estado de la convivencia. */
const VisitasDomiciliarias = {
  minutos(hora) {
    const [horas, minutos] = hora.split(':').map(Number);
    return horas * 60 + minutos;
  },

  duracion(minutos) {
    if (minutos === 60) return '1 hora';
    const horas = Math.floor(minutos / 60), resto = minutos % 60;
    const partes = [horas ? horas + (horas === 1 ? ' hora' : ' horas') : '',
      resto || !horas ? resto + (resto === 1 ? ' minuto' : ' minutos') : ''].filter(Boolean);
    return partes.join(' y ');
  },

  hora(valor) {
    return new Intl.DateTimeFormat('es-PA', {timeStyle: 'short', timeZone: 'America/Panama'})
      .format(new Date('2000-01-01T' + valor.slice(0, 5) + ':00-05:00'));
  },

  ahora() {
    const partes = new Intl.DateTimeFormat('en-CA', {
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
      hourCycle: 'h23', timeZone: 'America/Panama'
    }).formatToParts(new Date());
    const valor = tipo => partes.find(parte => parte.type === tipo)?.value;
    return {fecha: `${valor('year')}-${valor('month')}-${valor('day')}`,
      hora: `${valor('hour')}:${valor('minute')}`};
  },

  fechaInterna(valor) {
    const partes = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(valor.trim());
    if (!partes || partes[3] === '0000') return '';
    const iso = `${partes[3]}-${partes[2]}-${partes[1]}`;
    const fecha = new Date(iso + 'T00:00:00Z');
    return !Number.isNaN(fecha.getTime()) && fecha.toISOString().slice(0, 10) === iso ? iso : '';
  },

  horaInterna(valor, periodo) {
    const partes = /^(0?[1-9]|1[0-2]):([0-5]\d)$/.exec(valor.trim());
    if (!partes || !['AM', 'PM'].includes(periodo)) return '';
    const hora = Number(partes[1]) % 12 + (periodo === 'PM' ? 12 : 0);
    return String(hora).padStart(2, '0') + ':' + partes[2];
  },

  campoHora(nombre, etiqueta) {
    return `<div class="visita-campo"><label for="visita-${nombre}">${etiqueta} *</label>
      <input id="visita-${nombre}" name="${nombre}" type="text" placeholder="hh:mm" maxlength="5" autocomplete="off" aria-describedby="visita-error-${nombre}" required>
      <div class="visita-periodos" role="group" aria-label="Periodo de ${etiqueta.toLowerCase()}">
        <label><input type="radio" name="${nombre}Periodo" value="AM" checked><span>a. m.</span></label>
        <label><input type="radio" name="${nombre}Periodo" value="PM"><span>p. m.</span></label>
      </div>
      <small id="visita-error-${nombre}" class="visita-error-campo" role="alert" hidden></small></div>`;
  },

  panel(datos, abierto, visitasAbiertas = new Set()) {
    const {registros, referencia, puedeRegistrar, estadoConvivencia, trabajoSocial} = datos;
    const vigente = estadoConvivencia === 'VIGENTE';
    const realizadas = registros.length;
    const completadas = Math.min(realizadas, referencia);
    const mostrarProgreso = vigente || realizadas > 0;
    const porcentaje = Math.round(100 * completadas / referencia);
    const filas = registros.map((visita, i) => `<li class="visita-realizada">
      <details data-visita-id="${visita.id}" ${visitasAbiertas.has(String(visita.id)) ? 'open' : ''}>
        <summary class="visita-cabecera">
          <span class="visita-icono">${Profesionales.icono('comprobarCircular')}</span>
          <strong>Visita domiciliaria ${i + 1}</strong>
          <span class="visita-realizada-estado">Realizada</span>
          <span class="reporte-chevron" aria-hidden="true"></span>
        </summary>
        <dl class="detalle-datos visita-detalle">
          <div><dt>Fecha de visita</dt><dd>${Vinculaciones.fechaCivil(visita.fecha)}</dd></div>
          <div><dt>Horario</dt><dd>${this.hora(visita.inicio)} – ${this.hora(visita.fin)}</dd></div>
          <div><dt>Profesional responsable</dt><dd>${limpiarHtml(visita.responsable)}</dd></div>
          <div><dt>Duración</dt><dd>${this.duracion(visita.minutos)}</dd></div>
        </dl>
        ${trabajoSocial && vigente && visita.puedeEditar ? `<div class="pro-acciones visita-edicion"><button type="button" class="enlace reporte-editar" data-editar-visita="${visita.id}" aria-label="Editar visita ${i + 1}">${Profesionales.icono('editar')}Editar</button></div>` : ''}
      </details></li>`);
    const previstas = filas.slice(0, referencia);
    if (vigente) {
      for (let i = completadas; i < referencia; i++) {
        previstas.push(`<li class="visita-pendiente"><span class="visita-icono">${Profesionales.icono('calendario')}</span>
          <strong>Visita domiciliaria ${i + 1}</strong><span class="visita-horario">Pendiente</span></li>`);
      }
    }
    const aviso = {
      PENDIENTE: '<strong>Convivencia no iniciada</strong><p>Las visitas domiciliarias podrán registrarse cuando comience la convivencia.</p>',
      INTERRUMPIDA: '<p>La convivencia fue interrumpida. Puede consultar las visitas realizadas.</p>',
      FINALIZADA: '<p>La vinculación finalizó. Puede consultar las visitas realizadas.</p>',
      NO_DISPONIBLE: '<p>El estado actual de la vinculación permite únicamente consultar las visitas.</p>'
    }[estadoConvivencia];
    const cabecera = `<span class="vinculacion-emblema">${Profesionales.icono('hogar')}</span>
      <span>Visitas domiciliarias<small class="seguimiento-meta">${completadas} de ${referencia} visitas realizadas</small></span>`;
    const contenido = `${aviso ? `<div class="visita-estado">${aviso}</div>` : ''}
      ${mostrarProgreso ? `<p class="visita-previstas"><strong>Visitas previstas</strong><span>${porcentaje} %</span></p>
        <progress max="${referencia}" value="${completadas}" aria-label="Visitas previstas completadas"></progress>` : ''}
      ${previstas.length ? `<ol class="visitas-lista">${previstas.join('')}</ol>` : ''}
      ${realizadas > referencia ? `<h4 class="visita-adicionales">Visitas adicionales</h4>
        <ol class="visitas-lista" start="${referencia + 1}">${filas.slice(referencia).join('')}</ol>
        <p class="ayuda">Total de visitas realizadas: ${realizadas}</p>` : ''}`;
    const accion = trabajoSocial && vigente && puedeRegistrar
      ? `<button type="button" class="boton" data-registrar-visita>${Profesionales.icono('nuevo')}${completadas < referencia ? 'Registrar visita domiciliaria' : 'Registrar visita adicional'}</button>` : '';
    return `<div class="vinculacion-reportes visita-panel">${SeguimientosVinculacion.apartado('visitas',
      'visitas-domiciliarias' + (completadas === referencia ? ' completadas' : ''), abierto, cabecera, contenido, accion)}</div>`;
  },

  formulario(vinculacion, datos, visita, disparador, guardar) {
    const titulo = visita ? 'Editar visita domiciliaria' : 'Registrar visita domiciliaria';
    const dialogo = SeguimientosVinculacion.dialogo(titulo, disparador);
    const encabezado = dialogo.querySelector('h2');
    const numero = visita ? datos.registros.findIndex(item => item.id === visita.id) + 1 : null;
    encabezado.outerHTML = `<header class="visita-modal-cabecera">
      <span class="pro-simbolo">${Profesionales.icono('hogar')}</span>
      <div><h2 id="tituloModalSeguimiento">${titulo}</h2><p>${limpiarHtml(vinculacion.codigo)} · ${numero ? 'Visita ' + numero : 'Convivencia preadoptiva'}</p></div>
      <button class="enlace" type="button" data-cerrar-visita aria-label="Cerrar formulario">${Profesionales.icono('cerrar')}</button>
    </header>`;
    dialogo.classList.add('visita-dialogo');
    const hoy = this.ahora().fecha;
    dialogo.insertAdjacentHTML('beforeend', `
      <form class="pro-interrupcion-form" novalidate>
        <div class="visita-campo"><label for="visita-fecha">Fecha de la visita *</label>
          <div class="visita-fecha-control">
            <input id="visita-fecha" name="fecha" type="text" placeholder="dd/mm/aaaa" maxlength="10" autocomplete="off" aria-describedby="visita-error-fecha" required autofocus>
            <label class="visita-calendario">${Profesionales.icono('calendario')}
              <input type="date" data-calendario-visita min="${datos.fechaMinima}" max="${hoy}" aria-label="Seleccionar fecha de la visita"></label>
          </div>
          <small id="visita-error-fecha" class="visita-error-campo" role="alert" hidden></small></div>
        <div class="visita-horas">
          ${this.campoHora('inicio', 'Hora de inicio')}
          ${this.campoHora('fin', 'Hora de finalización')}
        </div>
        <p class="visita-duracion">${Profesionales.icono('reloj')} Duración de la visita <output for="visita-inicio visita-fin" aria-live="polite">—</output></p>
        <p class="ayuda visita-aviso">${Profesionales.icono('informacion')}<span>Registre únicamente visitas que ya hayan finalizado.</span></p>
        ${visita ? `<label>Motivo de modificación (opcional)<textarea name="motivoModificacion" rows="2" maxlength="1000"></textarea></label>
          <p class="ayuda">Los cambios realizados quedarán registrados en el historial.</p>` : ''}
        <p class="mensaje error" role="alert" hidden></p>
        <div class="pro-acciones">
          <button class="boton secundario" type="button" data-cancelar>Cancelar</button>
          <button class="boton" type="submit">${visita ? 'Guardar cambios' : 'Registrar visita'}</button>
        </div>
      </form>`);
    const form = dialogo.querySelector('form');
    const {fecha, inicio, fin} = form.elements;
    const calendario = form.querySelector('[data-calendario-visita]');
    calendario.addEventListener('click', evento => {
      if (!calendario.showPicker) return;
      try { calendario.showPicker(); evento.preventDefault(); }
      catch { /* El navegador conserva su selector nativo. */ }
    });
    calendario.value = visita?.fecha || hoy;
    fecha.value = Vinculaciones.fechaCivil(calendario.value);
    if (visita) {
      for (const campo of [inicio, fin]) {
        const [horas, minutos] = visita[campo.name].slice(0, 5).split(':');
        campo.value = String(Number(horas) % 12 || 12).padStart(2, '0') + ':' + minutos;
        form.elements[campo.name + 'Periodo'].value = Number(horas) < 12 ? 'AM' : 'PM';
      }
    }
    const leerHora = campo => this.horaInterna(campo.value, form.elements[campo.name + 'Periodo'].value);
    const output = form.querySelector('output');
    const actualizar = () => {
      const horaInicio = leerHora(inicio), horaFin = leerHora(fin);
      const duracion = horaInicio && horaFin ? this.minutos(horaFin) - this.minutos(horaInicio) : 0;
      output.textContent = duracion > 0 ? this.duracion(duracion) : '—';
    };
    const limpiarError = campo => {
      const error = dialogo.querySelector('#visita-error-' + campo.name);
      if (error) { error.hidden = true; error.textContent = ''; }
      campo.removeAttribute('aria-invalid');
    };
    const mostrarError = (campo, mensaje) => {
      campo.setAttribute('aria-invalid', 'true');
      const error = dialogo.querySelector('#visita-error-' + campo.name);
      error.hidden = false;
      error.textContent = mensaje;
      campo.focus();
    };
    form.addEventListener('input', evento => {
      if (evento.target === calendario && calendario.value) fecha.value = Vinculaciones.fechaCivil(calendario.value);
      if (evento.target === fecha) calendario.value = this.fechaInterna(fecha.value);
      if (evento.target === calendario) limpiarError(fecha);
      limpiarError(evento.target.name === 'inicioPeriodo' ? inicio : evento.target.name === 'finPeriodo' ? fin : evento.target);
      if (evento.target === inicio || evento.target === fecha) limpiarError(fin);
      actualizar();
    });
    actualizar();

    const claveRegistro = visita ? null : SeguimientosVinculacion.claveRegistro();
    const alerta = form.querySelector('p[role=alert]');
    let enviando = false;
    const cerrar = () => {
      if (enviando) return;
      if (Profesionales.formulariosPendientes().includes(form)) {
        Profesionales.confirmar('¿Descartar los cambios?', 'Los datos de la visita aún no se han guardado.', () => dialogo.close());
      } else dialogo.close();
    };
    form.querySelector('[data-cancelar]').onclick = cerrar;
    dialogo.querySelector('[data-cerrar-visita]').onclick = cerrar;
    dialogo.addEventListener('cancel', evento => { evento.preventDefault(); cerrar(); });
    form.onsubmit = async evento => {
      evento.preventDefault();
      if (enviando) return;
      const ahora = this.ahora();
      const dia = this.fechaInterna(fecha.value);
      const horaInicio = leerHora(inicio), horaFin = leerHora(fin);
      const datosEnvio = {fecha: dia, inicio: horaInicio, fin: horaFin};
      if (visita) datosEnvio.motivoModificacion = form.elements.motivoModificacion.value;
      const superpuesta = datos.registros.some(otra => otra.id !== visita?.id && otra.fecha === dia
        && otra.inicio.slice(0, 5) < horaFin && otra.fin.slice(0, 5) > horaInicio);
      [fecha, inicio, fin].forEach(limpiarError);
      alerta.hidden = true;
      if (!dia) return mostrarError(fecha, 'Indica una fecha válida en formato dd/mm/aaaa.');
      if (dia > ahora.fecha) return mostrarError(fecha, 'No puedes registrar una visita en una fecha futura.');
      if (dia < datos.fechaMinima) return mostrarError(fecha, 'La visita no puede ser anterior al inicio de la convivencia.');
      if (!horaInicio) return mostrarError(inicio, 'Indica una hora válida de inicio en formato hh:mm.');
      if (!horaFin) return mostrarError(fin, 'Indica una hora válida de finalización en formato hh:mm.');
      if (horaFin <= horaInicio) return mostrarError(fin, 'La hora de finalización debe ser posterior a la hora de inicio.');
      if (dia === ahora.fecha && horaFin > ahora.hora) {
        return mostrarError(fin, 'Solo puedes registrar visitas que ya hayan finalizado.');
      }
      if (superpuesta) return mostrarError(fin, 'Ya existe una visita registrada que coincide con ese horario.');
      if (visita) datosEnvio.version = visita.version;
      else datosEnvio.claveRegistro = claveRegistro;
      alerta.hidden = true;
      enviando = true;
      const controles = [...form.elements].filter(control => !control.disabled);
      controles.forEach(control => { control.disabled = true; });
      let respuesta;
      try {
        respuesta = await Vinculaciones.api(`/api/vinculaciones/${vinculacion.id}/visitas-domiciliarias${visita ? '/' + visita.id : ''}`, {
          method: visita ? 'PUT' : 'POST', body: JSON.stringify(datosEnvio)
        });
      } catch (error) {
        alerta.textContent = error.message;
        alerta.hidden = false;
      } finally {
        controles.forEach(control => { control.disabled = false; });
        enviando = false;
      }
      if (!respuesta) return;
      Profesionales.marcarGuardado(form);
      dialogo.close();
      await guardar(visita ? (respuesta.version === visita.version ? 'No hay cambios para guardar.' : 'La visita quedó actualizada.')
        : 'La visita domiciliaria quedó registrada correctamente.', !visita);
    };
    Profesionales.marcarGuardado(form);
    dialogo.showModal();
  },

  evento(movimiento) {
    return {...movimiento, titulo: 'Visita domiciliaria ' + movimiento.accion.toLowerCase()};
  },

  cambios(evento) {
    const campos = evento.cambios;
    if (evento.accion === 'REGISTRADA') {
      const inicio = campos.inicio.actual, fin = campos.fin.actual;
      return `<div class="pro-cambio"><p>Fecha de la visita · ${Vinculaciones.fechaCivil(campos.fecha.actual)}</p>
        <p>Horario · ${this.hora(inicio)} – ${this.hora(fin)}</p>
        <p>Duración · ${this.duracion(this.minutos(fin) - this.minutos(inicio))}</p></div>`;
    }
    const etiqueta = {fecha: 'Fecha de la visita', inicio: 'Hora de inicio', fin: 'Hora de finalización', duracion: 'Duración'};
    const valor = (campo, dato) => {
      if (campo === 'fecha') return Vinculaciones.fechaCivil(dato);
      return campo === 'duracion' ? this.duracion(Number(dato)) : this.hora(dato);
    };
    return SeguimientosVinculacion.detalleCambios(evento, campo => etiqueta[campo], valor);
  }
};
