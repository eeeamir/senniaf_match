const VisitasDomiciliarias = {
  minutos(hora) {
    const [horas, minutos] = hora.split(':').map(Number);
    return horas * 60 + minutos;
  },

  duracion(minutos) {
    const horas = Math.floor(minutos / 60), resto = minutos % 60;
    return [horas ? horas + ' h' : '', resto || !horas ? resto + ' min' : ''].filter(Boolean).join(' ');
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

  panel(datos, abierto) {
    const {registros, referencia, puedeRegistrar} = datos;
    const realizadas = registros.length;
    const pendientes = Math.max(0, referencia - realizadas);
    const filas = registros.map((visita, i) => `<li><div>
      <strong>${Profesionales.icono('comprobar')}Visita ${i + 1}</strong>
      <p class="ayuda">${Vinculaciones.fechaCivil(visita.fecha)} · ${this.hora(visita.inicio)} – ${this.hora(visita.fin)}</p>
    </div><div class="visita-horario"><strong>${this.duracion(visita.minutos)}</strong>
      ${visita.puedeEditar ? `<button type="button" class="enlace" data-editar-visita="${visita.id}" aria-label="Editar visita ${i + 1}">Editar</button>` : ''}</div></li>`);
    for (let i = 0; i < pendientes; i++) filas.push(`<li><strong>${Profesionales.icono('reloj')}Visita ${realizadas + i + 1}</strong><span>Pendiente</span></li>`);
    return `<details class="detalle-grupo vinculacion-reporte visitas-domiciliarias ${pendientes === 0 ? 'completadas' : ''}" data-panel-visitas ${abierto ? 'open' : ''}>
      <summary><span class="vinculacion-emblema">${Profesionales.icono('hogar')}</span>Visitas domiciliarias realizadas
        <strong class="visita-horario">${realizadas} de ${referencia}</strong></summary>
      <div class="vinculacion-reporte-contenido">
        <progress max="${referencia}" value="${Math.min(realizadas, referencia)}" aria-label="Visitas de referencia completadas"></progress>
        ${realizadas === 0 ? '<p class="ayuda">Todavía no se han registrado visitas domiciliarias.</p>' : ''}
        <ol class="visitas-lista">${filas.join('')}</ol>
        <div class="pro-fila-titulo"><p>${pendientes ? pendientes + (pendientes === 1 ? ' visita pendiente' : ' visitas pendientes')
          : 'Visitas de referencia completadas'}</p>
        ${puedeRegistrar ? '<button type="button" class="boton" data-registrar-visita>+ Registrar visita domiciliaria</button>' : ''}</div>
      </div>
    </details>`;
  },

  formulario(vinculacion, datos, visita, disparador, guardar) {
    const titulo = visita ? 'Editar visita domiciliaria' : 'Registrar visita domiciliaria';
    const dialogo = SeguimientosVinculacion.dialogo(titulo, disparador);
    const encabezado = dialogo.querySelector('h2');
    encabezado.insertAdjacentHTML('afterbegin', Profesionales.icono(visita ? 'calendario' : 'hogar'));
    encabezado.insertAdjacentHTML('afterend', `<button class="enlace" type="button" data-cerrar-visita aria-label="Cerrar formulario">${Profesionales.icono('cerrar')}</button>`);
    dialogo.classList.add('visita-dialogo');

    const hoy = this.ahora().fecha;
    const numero = visita ? datos.registros.findIndex(item => item.id === visita.id) + 1 : null;
    dialogo.insertAdjacentHTML('beforeend', `
      <p class="ayuda">${limpiarHtml(vinculacion.codigo)} · ${numero ? 'Visita ' + numero : 'Convivencia preadoptiva'}</p>
      <form class="pro-interrupcion-form" novalidate>
        <p class="ayuda">Los campos marcados con * son obligatorios.</p>
        <div class="visita-campo"><label for="visita-fecha">Fecha de la visita *</label>
          <input id="visita-fecha" name="fecha" type="date" min="${datos.fechaMinima}" max="${hoy}" aria-describedby="visita-error-fecha" required autofocus>
          <small id="visita-error-fecha" class="visita-error-campo" role="alert" hidden></small></div>
        <div class="visita-horas">
          <div class="visita-campo"><label for="visita-inicio">Hora de inicio *</label>
            <input id="visita-inicio" name="inicio" type="time" aria-describedby="visita-error-inicio" required>
            <small id="visita-error-inicio" class="visita-error-campo" role="alert" hidden></small></div>
          <div class="visita-campo"><label for="visita-fin">Hora de finalización *</label>
            <input id="visita-fin" name="fin" type="time" aria-describedby="visita-error-fin" required>
            <small id="visita-error-fin" class="visita-error-campo" role="alert" hidden></small></div>
        </div>
        <p class="visita-duracion">${Profesionales.icono('reloj')} Duración de la visita <output for="visita-inicio visita-fin" aria-live="polite">—</output></p>
        <p class="ayuda">Registre únicamente visitas que ya hayan finalizado.</p>
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
    fecha.value = visita?.fecha || hoy;
    inicio.value = visita ? visita.inicio.slice(0, 5) : '';
    fin.value = visita ? visita.fin.slice(0, 5) : '';
    const output = form.querySelector('output');
    const actualizar = () => {
      const duracion = inicio.value && fin.value ? this.minutos(fin.value) - this.minutos(inicio.value) : 0;
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
      limpiarError(evento.target);
      if (evento.target === inicio || evento.target === fecha) limpiarError(fin);
      actualizar();
    });
    actualizar();

    const claveRegistro = visita ? null : SeguimientosVinculacion.claveRegistro();
    const alerta = form.querySelector('[role=alert]');
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
      const datosEnvio = Object.fromEntries(new FormData(form));
      const superpuesta = datos.registros.some(otra => otra.id !== visita?.id && otra.fecha === fecha.value
        && otra.inicio.slice(0, 5) < fin.value && otra.fin.slice(0, 5) > inicio.value);
      [fecha, inicio, fin].forEach(limpiarError);
      alerta.hidden = true;
      if (!fecha.value) return mostrarError(fecha, 'Indica la fecha de la visita.');
      if (fecha.value > ahora.fecha) return mostrarError(fecha, 'No puedes registrar una visita en una fecha futura.');
      if (fecha.value < datos.fechaMinima) return mostrarError(fecha, 'La visita no puede ser anterior al inicio de la convivencia.');
      if (!inicio.value) return mostrarError(inicio, 'Indica la hora de inicio.');
      if (!fin.value) return mostrarError(fin, 'Indica la hora de finalización.');
      if (fin.value <= inicio.value) return mostrarError(fin, 'La hora de finalización debe ser posterior a la hora de inicio.');
      if (fecha.value === ahora.fecha && fin.value > ahora.hora) {
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
        : 'La visita domiciliaria quedó registrada.');
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
    const etiqueta = {fecha: 'Fecha de la visita', inicio: 'Hora de inicio', fin: 'Hora de finalización'};
    const valor = (campo, dato) => campo === 'fecha' ? Vinculaciones.fechaCivil(dato) : this.hora(dato);
    return SeguimientosVinculacion.detalleCambios(evento, campo => etiqueta[campo], valor);
  }
};
