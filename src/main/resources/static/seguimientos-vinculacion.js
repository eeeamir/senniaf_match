const SeguimientosVinculacion = {
  especialidades: {
    TRABAJADOR_SOCIAL: {
      titulo: 'Seguimiento social', icono: 'informe', color: 'menta',
      descripcion: 'Adaptación familiar, condiciones de cuidado y necesidades de acompañamiento.',
      tipos: {
        INTERRELACION: 'Encuentro de interrelación familiar', ENTREVISTA_FAMILIAR: 'Entrevista familiar',
        ORIENTACION_FAMILIAR: 'Orientación familiar', VISITA_DOMICILIARIA: 'Visita domiciliaria',
        TELEFONICO: 'Seguimiento telefónico', OTRO: 'Otro', ENCUENTRO: 'Encuentro de seguimiento'
      },
      campos: {
        adaptacion: ['Adaptación y dinámica familiar', [
          ['Rutinas familiares', 'Se observa una adaptación favorable a las rutinas y actividades del entorno familiar.'],
          ['Adaptación gradual', 'El niño se encuentra en un proceso gradual de adaptación al entorno familiar.'],
          ['Acompañamiento adicional', 'Se identifican dificultades de adaptación que requieren acompañamiento adicional.']
        ]],
        contexto: ['Condiciones de cuidado y entorno familiar', [
          ['Cuidados adecuados', 'Se observan condiciones adecuadas de cuidado y atención a las necesidades del niño.'],
          ['Orientación familiar', 'La familia requiere orientación adicional para atender determinadas necesidades del niño.'],
          ['Información por ampliar', 'Se requiere ampliar la información para valorar las condiciones de cuidado.']
        ]],
        dificultades: ['Dificultades identificadas', [
          ['Sin dificultades relevantes', 'Durante este seguimiento no se identificaron dificultades relevantes.'],
          ['Nuevas rutinas', 'Se observan dificultades relacionadas con la adaptación a nuevas rutinas.'],
          ['Comunicación familiar', 'Se identifican aspectos de la comunicación familiar que requieren fortalecimiento.'],
          ['Organización de cuidados', 'Se identifican dificultades para coordinar la distribución de las tareas de cuidado.']
        ]],
        recomendaciones: ['Recomendaciones profesionales', [
          ['Seguimiento periódico', 'Mantener el seguimiento periódico y observar la evolución de la adaptación familiar.'],
          ['Orientar a la familia', 'Brindar orientación a la familia sobre las necesidades de acompañamiento identificadas.'],
          ['Seguimiento adicional', 'Realizar un seguimiento adicional para valorar las dificultades observadas.'],
          ['Acuerdos de cuidado', 'Acordar con la familia responsables para las tareas de cuidado y revisar su cumplimiento en el próximo encuentro.']
        ]]
      }
    },
    PSICOLOGO: {
      titulo: 'Seguimiento psicológico', icono: 'psicologia', color: 'lavanda',
      descripcion: 'Adaptación emocional, evolución del vínculo afectivo y acompañamiento psicológico.',
      tipos: {
        SESION_INDIVIDUAL: 'Sesión individual', SESION_FAMILIAR: 'Sesión familiar',
        INTERRELACION: 'Encuentro de interrelación familiar', ENTREVISTA_PSICOLOGICA: 'Entrevista psicológica',
        OBSERVACION_VINCULO: 'Observación del vínculo afectivo', ORIENTACION_FAMILIAR: 'Orientación familiar',
        VISITA_DOMICILIARIA: 'Visita domiciliaria', TELEFONICO: 'Seguimiento telefónico',
        OTRO: 'Otro', ENTREVISTA: 'Entrevista de seguimiento'
      },
      campos: {
        adaptacion: ['Adaptación emocional', [
          ['Regulación emocional', 'Se observa una evolución favorable en la expresión y regulación de las emociones.'],
          ['Adaptación gradual', 'El niño continúa adaptándose emocionalmente a los cambios en su entorno.'],
          ['Acompañamiento emocional', 'Se identifican dificultades emocionales que requieren acompañamiento psicológico.']
        ]],
        contexto: ['Evolución del vínculo afectivo', [
          ['Fortalecimiento del vínculo', 'Se observa un fortalecimiento progresivo del vínculo afectivo con la familia.'],
          ['Vínculo en desarrollo', 'El vínculo afectivo se encuentra en desarrollo y requiere observación continua.'],
          ['Interacción afectiva', 'Se identifican dificultades en la interacción afectiva que requieren acompañamiento profesional.'],
          ['Búsqueda de apoyo', 'Durante la interacción se observó que el niño buscó apoyo en figuras familiares ante situaciones nuevas.']
        ]],
        dificultades: ['Dificultades emocionales o conductuales identificadas', [
          ['Sin dificultades relevantes', 'Durante esta sesión no se observaron dificultades emocionales relevantes.'],
          ['Situaciones nuevas', 'Se observan manifestaciones de inseguridad ante situaciones nuevas.'],
          ['Expresión emocional', 'Se identifican dificultades para comunicar y expresar emociones.']
        ]],
        recomendaciones: ['Recomendaciones profesionales', [
          ['Continuidad del acompañamiento', 'Dar continuidad al acompañamiento psicológico y observar la evolución emocional.'],
          ['Comunicación y escucha', 'Promover espacios de comunicación, escucha y expresión emocional en el entorno familiar.'],
          ['Nueva valoración', 'Realizar un seguimiento adicional para ampliar la valoración de las dificultades observadas.'],
          ['Transiciones', 'Acompañar las transiciones con rutinas previsibles y revisar la respuesta emocional en la próxima sesión.']
        ]]
      }
    }
  },

  etapas: {ENCUENTROS_PREVIOS: 'Encuentros previos', CONVIVENCIA_PREADOPTIVA: 'Convivencia preadoptiva'},

  etiqueta(rol, campo) {
    return {fechaAtencion: 'Fecha de atención', tipo: 'Tipo de seguimiento', etapa: 'Etapa',
      participantes: 'Participantes', proximoSeguimiento: 'Próximo seguimiento previsto'}[campo]
      || this.especialidades[rol].campos[campo]?.[0] || campo;
  },

  valor(rol, campo, valor) {
    if (valor == null) return 'Sin registrar';
    if (campo === 'tipo') return this.especialidades[rol].tipos[valor] || valor;
    if (campo === 'etapa') return this.etapas[valor] || valor;
    if (campo === 'fechaAtencion' || campo === 'proximoSeguimiento') return Vinculaciones.fechaCivil(valor);
    return valor;
  },

  panel(datos, abiertos) {
    return `<div class="vinculacion-reportes">${Object.entries(this.especialidades).map(([rol, especialidad]) => {
      const consulta = datos.rolesConsulta.includes(rol);
      const registros = datos.registros.filter(registro => registro.rol === rol);
      const ultimo = registros[0];
      const resumen = ultimo
        ? 'Último seguimiento · ' + Vinculaciones.fechaCivil(ultimo.fechaAtencion)
        : 'Sin seguimientos registrados';
      const dias = ultimo ? Math.round((Date.parse(datos.hoy) - Date.parse(ultimo.fechaAtencion)) / 86400000) : null;
      const tiempo = dias === 0 ? 'Hoy' : dias > 0 ? 'Hace ' + Vinculaciones.dias(dias) : '';
      return `<details class="detalle-grupo vinculacion-reporte ${especialidad.color}" data-especialidad="${rol}" ${abiertos.has(rol) ? 'open' : ''}>
        <summary><span class="vinculacion-emblema">${Profesionales.icono(especialidad.icono)}</span><span>${especialidad.titulo}${consulta ? `<small class="seguimiento-meta">${resumen}</small><small class="seguimiento-meta">${ultimo?.contenido.proximoSeguimiento
          ? 'Próximo seguimiento previsto · ' + Vinculaciones.fechaCivil(ultimo.contenido.proximoSeguimiento) : 'Sin próxima fecha prevista'}</small>` : ''}</span>
        ${tiempo ? `<strong class="seguimiento-tiempo">${tiempo}</strong>` : ''}</summary>

        <div class="vinculacion-reporte-contenido">
          <p>${especialidad.descripcion}</p>
          ${consulta ? registros.map(registro => this.tarjeta(registro)).join('') || '<p>No hay seguimientos registrados en este apartado.</p>'
              : '<p>La consulta está reservada a la especialidad correspondiente y al Comité.</p>'}
          ${datos.rolRegistro === rol ? '<div class="pro-acciones"><button type="button" class="boton" data-registrar-seguimiento>Registrar seguimiento</button></div>' : ''}
        </div>
      </details>`;
    }).join('')}</div>`;
  },

  tarjeta(registro) {
    const {id, rol, profesional, fechaAtencion, etapa, contenido, puedeEditar} = registro;
    const observaciones = contenido.adaptacion.replace(/\s+/g, ' ');
    const resumen = observaciones.length > 180 ? observaciones.slice(0, 177) + '…' : observaciones;
    return `<article class="pro-informe">
      <h3>${limpiarHtml(this.valor(rol, 'tipo', contenido.tipo))}</h3>
      <p class="ayuda">${limpiarHtml(Vinculaciones.fechaCivil(fechaAtencion))} · ${limpiarHtml(profesional)} · ${limpiarHtml(this.etapas[etapa])}</p>
      <p>${limpiarHtml(resumen)}</p>
      <div class="pro-acciones">
        <button type="button" class="boton secundario chico" data-ver-seguimiento="${id}">Ver seguimiento completo</button>
        ${puedeEditar ? `<button type="button" class="boton secundario chico" data-editar-seguimiento="${id}">Editar seguimiento</button>` : ''}
      </div>
    </article>`;
  },

  dialogo(titulo, disparador) {
    const dialogo = document.createElement('dialog');
    dialogo.className = 'pro-dialogo';
    dialogo.setAttribute('aria-labelledby', 'tituloModalSeguimiento');
    dialogo.innerHTML = `<h2 id="tituloModalSeguimiento">${limpiarHtml(titulo)}</h2>`;
    document.body.append(dialogo);
    dialogo.addEventListener('close', () => {
      dialogo.remove();
      if (disparador.isConnected) disparador.focus();
    }, {once: true});
    return dialogo;
  },

  contexto(vinculacion) {
    return `<p class="ayuda">${limpiarHtml(vinculacion.codigo)} · Expediente infantil ${limpiarHtml(vinculacion.expedienteInfantil)} ·
      Expediente familiar ${limpiarHtml(vinculacion.expedienteFamiliar)}</p>`;
  },

  ver(vinculacion, registro, disparador) {
    const dialogo = this.dialogo(this.especialidades[registro.rol].titulo, disparador);
    const campos = {fechaAtencion: registro.fechaAtencion, etapa: registro.etapa, ...registro.contenido};
    dialogo.insertAdjacentHTML('beforeend', this.contexto(vinculacion) + `
      <p>${limpiarHtml(registro.profesional)} · Registrado el ${limpiarHtml(Vinculaciones.fechaHora(registro.registrado))}</p>
      <dl class="detalle-datos">${Object.entries(campos).map(([campo, valor]) => `
        <div><dt>${limpiarHtml(this.etiqueta(registro.rol, campo))}</dt>
        <dd class="pro-texto-registrado">${limpiarHtml(this.valor(registro.rol, campo, valor))}</dd></div>`).join('')}</dl>
      <div class="pro-acciones"><button class="boton secundario" type="button" data-cerrar>Cerrar</button></div>`);
    dialogo.querySelector('[data-cerrar]').onclick = () => dialogo.close();
    dialogo.showModal();
  },

  formulario(vinculacion, datos, registro, disparador, alGuardar) {
    const rol = registro?.rol || datos.rolRegistro;
    const especialidad = this.especialidades[rol];
    const dialogo = this.dialogo((registro ? 'Editar ' : 'Registrar ') + especialidad.titulo.toLowerCase(), disparador);
    dialogo.classList.add('seguimiento-dialogo', especialidad.color);
    dialogo.insertAdjacentHTML('beforeend', this.contexto(vinculacion) + `
      <p>Etapa de la atención <strong data-etapa></strong></p>
      <form class="pro-interrupcion-form">
        <label>Fecha de atención<input type="date" name="fechaAtencion" aria-describedby="fecha-atencion-legible" required min="${limpiarHtml(datos.primeraFecha)}" max="${limpiarHtml(datos.hoy)}"><small class="ayuda" id="fecha-atencion-legible"></small></label>
        <label>Tipo de seguimiento<select name="tipo" required><option value="">Selecciona un tipo</option>
          ${Object.entries(especialidad.tipos).filter(([tipo]) => !['ENCUENTRO', 'ENTREVISTA'].includes(tipo) || tipo === registro?.contenido.tipo).map(([valor, nombre]) => `<option value="${valor}">${nombre}</option>`).join('')}
        </select></label>
        <label>Participantes<textarea name="participantes" rows="2" maxlength="500" required></textarea></label>
        <p class="ayuda">Selecciona una sugerencia y adapta el texto según lo observado. Elegir otra reemplazará el contenido.</p>
        ${Object.entries(especialidad.campos).map(([campo, [etiqueta]]) => `
          <div><label for="seguimiento-${campo}">${etiqueta}</label>
          <textarea id="seguimiento-${campo}" name="${campo}" rows="3" maxlength="2000" required></textarea></div>`).join('')}
        <label>Fecha prevista del próximo seguimiento (opcional)<input type="date" name="proximoSeguimiento" aria-describedby="fecha-proxima-legible"><small class="ayuda" id="fecha-proxima-legible"></small></label>
        ${registro ? '<label>Motivo de modificación (opcional)<textarea name="motivoModificacion" rows="2" maxlength="1000"></textarea></label>' : ''}
        <p class="mensaje error" role="alert" hidden></p>
        <div class="pro-acciones">
          <button class="boton secundario" type="button" data-cancelar>Cancelar</button>
          <button class="boton" type="submit">${registro ? 'Guardar cambios' : 'Registrar seguimiento'}</button>
        </div>
      </form>`);
    const form = dialogo.querySelector('form');
    const fecha = form.elements.fechaAtencion;
    const proxima = form.elements.proximoSeguimiento;
    fecha.value = registro?.fechaAtencion || datos.hoy;
    Object.entries(registro?.contenido || {}).forEach(([campo, valor]) => { form.elements[campo].value = valor; });
    Object.entries(especialidad.campos).forEach(([campo, [, opciones]]) =>
      Profesionales.sugerencias(form.elements[campo], opciones, {seleccionUnica: true}));
    const actualizarEtapa = () => {
      proxima.min = fecha.value;
      dialogo.querySelector('#fecha-atencion-legible').textContent = fecha.value ? Vinculaciones.fechaCivil(fecha.value) : '';
      dialogo.querySelector('#fecha-proxima-legible').textContent = proxima.value ? Vinculaciones.fechaCivil(proxima.value) : '';
      const etapa = registro && fecha.value === registro.fechaAtencion ? registro.etapa
        : datos.inicioConvivencia && fecha.value >= datos.inicioConvivencia ? 'CONVIVENCIA_PREADOPTIVA' : 'ENCUENTROS_PREVIOS';
      dialogo.querySelector('[data-etapa]').textContent = fecha.value ? this.etapas[etapa] : 'Selecciona la fecha de atención';
    };
    fecha.addEventListener('input', actualizarEtapa);
    proxima.addEventListener('input', actualizarEtapa);
    actualizarEtapa();

    // La clave pertenece a este envío y se conserva durante los reintentos del formulario.
    const claveRegistro = registro ? null : this.claveRegistro();
    const enviar = form.querySelector('[type=submit]');
    const cancelar = form.querySelector('[data-cancelar]');
    const error = form.querySelector('[role=alert]');
    let enviando = false;
    const cerrar = () => {
      if (enviando) return;
      if (Profesionales.formulariosPendientes().includes(form)) {
        Profesionales.confirmar('¿Descartar los cambios?', 'Los cambios de este formulario aún no se han guardado.', () => dialogo.close());
      } else dialogo.close();
    };
    cancelar.onclick = cerrar;
    dialogo.addEventListener('cancel', evento => { evento.preventDefault(); cerrar(); });
    form.onsubmit = async evento => {
      evento.preventDefault();
      if (enviando) return;
      const payload = Object.fromEntries(new FormData(form));
      for (const campo of ['participantes', ...Object.keys(especialidad.campos)]) {
        if (payload[campo].trim()) continue;
        error.hidden = false;
        error.textContent = `Completa el campo «${this.etiqueta(rol, campo)}».`;
        form.elements[campo].focus();
        return;
      }
      payload.proximoSeguimiento ||= null;
      if (registro) payload.version = registro.version;
      else payload.claveRegistro = claveRegistro;
      const url = `/api/vinculaciones/${vinculacion.id}/seguimientos${registro ? '/' + registro.id : ''}`;
      error.hidden = true;
      enviando = true;
      const controles = [...form.elements].filter(control => !control.disabled);
      controles.forEach(control => { control.disabled = true; });
      let guardado;
      try {
        guardado = await Vinculaciones.api(url, {method: registro ? 'PUT' : 'POST', body: JSON.stringify(payload)});
      } catch (fallo) {
        error.textContent = fallo.message;
        error.hidden = false;
      } finally {
        enviando = false;
        controles.forEach(control => { control.disabled = false; });
      }
      if (!guardado) return;
      Profesionales.marcarGuardado(form);
      dialogo.close();
      const mensaje = !registro ? 'El seguimiento quedó registrado.'
        : guardado.version === registro.version ? 'No hay cambios para guardar.' : 'El seguimiento quedó actualizado.';
      await alGuardar(rol, guardado.id, mensaje);
    };
    Profesionales.marcarGuardado(form);
    dialogo.showModal();
  },

  claveRegistro() {
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 15) | 64;
    bytes[8] = (bytes[8] & 63) | 128;
    const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('');
    return [hex.slice(0, 8), hex.slice(8, 12), hex.slice(12, 16), hex.slice(16, 20), hex.slice(20)].join('-');
  },

  evento(movimiento) {
    return {...movimiento, titulo: this.especialidades[movimiento.rol].titulo + ' ' + movimiento.accion.toLowerCase()};
  },

  cambios(evento) {
    if (evento.accion !== 'MODIFICADO') return '';
    return this.detalleCambios(evento, campo => this.etiqueta(evento.rol, campo),
      (campo, dato) => this.valor(evento.rol, campo, dato));
  },

  detalleCambios(evento, etiqueta, valor) {
    return `<details class="pro-evento-detalle"><summary>Ver cambios realizados</summary>
      ${Object.entries(evento.cambios).map(([campo, cambio]) => `<article class="pro-cambio">
        <h4>${limpiarHtml(etiqueta(campo))}</h4>
        <div class="pro-cambio-valores">
          <span class="pro-texto-registrado"><strong>Antes</strong><br>${limpiarHtml(valor(campo, cambio.anterior))}</span>
          <span class="pro-cambio-flecha" aria-hidden="true">→</span>
          <span class="pro-texto-registrado"><strong>Después</strong><br>${limpiarHtml(valor(campo, cambio.actual))}</span>
        </div></article>`).join('')}
      ${evento.motivo ? `<p class="pro-texto-registrado"><strong>Motivo de modificación</strong><br>${limpiarHtml(evento.motivo)}</p>` : ''}
    </details>`;
  }
};
