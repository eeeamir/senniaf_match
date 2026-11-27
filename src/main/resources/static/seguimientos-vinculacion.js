const SeguimientosVinculacion = {
  especialidades: {
    TRABAJADOR_SOCIAL: {
      titulo: 'Seguimiento social', icono: 'informe', color: 'menta',
      tipos: {
        INTERRELACION: 'Encuentro de interrelación familiar', ENTREVISTA_FAMILIAR: 'Entrevista familiar',
        ORIENTACION_FAMILIAR: 'Orientación familiar', VISITA_DOMICILIARIA: 'Visita domiciliaria',
        TELEFONICO: 'Seguimiento telefónico', OTRO: 'Otro', ENCUENTRO: 'Encuentro de seguimiento'
      },
      campos: {
        adaptacion: 'Adaptación y dinámica familiar', contexto: 'Condiciones de cuidado y entorno familiar',
        dificultades: 'Dificultades identificadas', recomendaciones: 'Recomendaciones profesionales'
      },
      encuentros: {
        adaptacion: ['¿Cómo fue la interacción entre el niño y la familia?',
          ['Interacción favorable', 'El niño participó con interés en las actividades compartidas con la familia.'],
          ['Acercamiento gradual', 'El niño se incorporó progresivamente a las actividades, respetándose su ritmo de acercamiento.'],
          ['Interacción limitada', 'La participación del niño fue limitada durante el encuentro.']],
        contexto: ['¿Cómo se está preparando la familia para atender las necesidades del niño?',
          ['Preparación familiar', 'La familia manifestó disposición para organizar sus responsabilidades de cuidado y mantener la continuidad escolar del niño.'],
          ['Necesita orientación', 'La familia expresó dudas sobre la organización de los cuidados y solicitó orientación profesional.'],
          ['Red de apoyo', 'La familia identificó personas de confianza que podrían brindar apoyo durante la transición al hogar.']],
        dificultades: ['¿Qué situaciones requieren atención para favorecer la integración familiar?',
          ['Sin dificultades relevantes', 'Durante el encuentro no se identificaron dificultades sociales relevantes.'],
          ['Reserva inicial', 'El niño mostró cierta reserva al iniciar el encuentro y participó con mayor confianza conforme avanzaron las actividades.'],
          ['Inquietudes familiares', 'La familia manifestó inquietudes sobre la transición y las responsabilidades de cuidado.']],
        recomendaciones: ['¿Qué acciones se recomiendan para favorecer la vinculación?',
          ['Acercamiento gradual', 'Continuar los encuentros progresivos, respetando las preferencias y los tiempos del niño.'],
          ['Orientación familiar', 'Orientar a la familia sobre las necesidades del niño y la preparación del entorno familiar.'],
          ['Acuerdos de cuidado', 'Revisar con la familia los acuerdos de cuidado antes de iniciar la convivencia.']]
      },
      convivencia: {
        adaptacion: ['¿Cómo se está integrando el niño a las actividades y rutinas del hogar?',
          ['Rutinas familiares', 'El niño participa en las actividades cotidianas del hogar y se adapta favorablemente a las rutinas familiares.'],
          ['Adaptación gradual', 'El niño se encuentra en un proceso gradual de integración a las rutinas del hogar.'],
          ['Requiere acompañamiento', 'Se identificaron dificultades de adaptación que requieren acompañamiento adicional.']],
        contexto: ['¿Cómo está atendiendo la familia las necesidades de cuidado, salud y educación del niño?',
          ['Cuidados adecuados', 'La familia atiende las necesidades cotidianas de cuidado, salud y educación del niño.'],
          ['Red de apoyo activa', 'La familia cuenta con personas de confianza que colaboran en las tareas de cuidado.'],
          ['Necesita orientación', 'La familia necesita orientación para ajustar los cuidados a las necesidades del niño.']],
        dificultades: ['¿Qué situaciones afectan la adaptación o el bienestar del niño durante la convivencia?',
          ['Sin dificultades relevantes', 'Durante el seguimiento no se identificaron dificultades sociales relevantes.'],
          ['Adaptación a nuevas rutinas', 'El niño aún requiere apoyo para familiarizarse con algunas rutinas del hogar.'],
          ['Organización de cuidados', 'Se identificaron dificultades en la distribución de las responsabilidades de cuidado.']],
        recomendaciones: ['¿Qué acciones pueden favorecer el bienestar del niño y su integración familiar?',
          ['Seguimiento periódico', 'Mantener el seguimiento periódico de la adaptación al hogar.'],
          ['Acuerdos de cuidado', 'Revisar con la familia la distribución de las tareas de cuidado y los apoyos disponibles.'],
          ['Orientación adicional', 'Orientar a la familia sobre los ajustes cotidianos que pueden facilitar la integración del niño.']]
      }
    },
    PSICOLOGO: {
      titulo: 'Seguimiento psicológico', icono: 'psicologia', color: 'lavanda',
      tipos: {
        SESION_INDIVIDUAL: 'Sesión individual', SESION_FAMILIAR: 'Sesión familiar',
        INTERRELACION: 'Encuentro de interrelación familiar', ENTREVISTA_PSICOLOGICA: 'Entrevista psicológica',
        OBSERVACION_VINCULO: 'Observación del vínculo afectivo', ORIENTACION_FAMILIAR: 'Orientación familiar',
        VISITA_DOMICILIARIA: 'Visita domiciliaria', TELEFONICO: 'Seguimiento telefónico',
        OTRO: 'Otro', ENTREVISTA: 'Entrevista de seguimiento'
      },
      campos: {
        adaptacion: 'Adaptación emocional', contexto: 'Evolución del vínculo afectivo',
        dificultades: 'Dificultades emocionales o conductuales identificadas', recomendaciones: 'Recomendaciones profesionales'
      },
      encuentros: {
        adaptacion: ['¿Cómo responde emocionalmente el niño al proceso de vinculación?',
          ['Expresión emocional', 'El niño expresó sus emociones y preferencias con espontaneidad durante las actividades compartidas.'],
          ['Seguridad progresiva', 'El niño se mostró reservado al inicio y participó con mayor confianza conforme avanzó el encuentro.'],
          ['Incertidumbre', 'El niño manifestó incertidumbre respecto a los próximos encuentros y posibles cambios en su entorno.']],
        contexto: ['¿Cómo evoluciona la relación afectiva entre el niño y la familia?',
          ['Confianza progresiva', 'Se observó un acercamiento gradual hacia la familia, que respetó los tiempos de participación del niño.'],
          ['Interacción espontánea', 'El niño inició interacciones espontáneas con la familia durante las actividades compartidas.'],
          ['Vínculo en desarrollo', 'La relación afectiva se encuentra en una etapa inicial y requiere continuidad de los encuentros.']],
        dificultades: ['¿Qué preocupaciones o manifestaciones emocionales requieren acompañamiento?',
          ['Sin dificultades relevantes', 'Durante la atención no se observaron dificultades emocionales o conductuales relevantes.'],
          ['Temores ante cambios', 'El niño expresó preocupación por posibles cambios en sus rutinas y entorno habitual.'],
          ['Expresión emocional limitada', 'El niño presentó dificultades para comunicar algunas de sus preocupaciones.']],
        recomendaciones: ['¿Qué acompañamiento puede favorecer el bienestar emocional del niño y su vinculación?',
          ['Preparación emocional', 'Brindar información anticipada sobre los próximos encuentros y favorecer espacios de expresión emocional.'],
          ['Respetar sus tiempos', 'Continuar el acercamiento gradual respetando los límites y preferencias expresados por el niño.'],
          ['Continuidad psicológica', 'Continuar el acompañamiento psicológico y observar la evolución emocional y afectiva.']]
      },
      convivencia: {
        adaptacion: ['¿Cómo responde emocionalmente el niño a la convivencia y a los cambios cotidianos?',
          ['Regulación emocional', 'El niño muestra mayor confianza para expresar sus emociones y solicitar apoyo ante dificultades.'],
          ['Adaptación gradual', 'El niño continúa adaptándose emocionalmente a los cambios asociados con la convivencia familiar.'],
          ['Acompañamiento emocional', 'Se identificaron manifestaciones de malestar relacionadas con los cambios cotidianos que requieren acompañamiento psicológico.']],
        contexto: ['¿Cómo evoluciona la relación afectiva entre el niño y su familia?',
          ['Fortalecimiento del vínculo', 'El niño se comunica con mayor confianza con la familia en las actividades cotidianas.'],
          ['Búsqueda de apoyo', 'El niño busca apoyo en figuras familiares cuando necesita ayuda.'],
          ['Vínculo en desarrollo', 'El vínculo afectivo continúa desarrollándose a través de las interacciones cotidianas.']],
        dificultades: ['¿Qué necesidades emocionales o conductuales requieren atención durante la convivencia?',
          ['Sin dificultades relevantes', 'Durante la atención no se observaron dificultades emocionales o conductuales relevantes.'],
          ['Inseguridad ante cambios', 'El niño manifestó inseguridad ante cambios en las rutinas del hogar.'],
          ['Expresión emocional limitada', 'Al niño le resulta difícil expresar algunas preocupaciones sobre la convivencia.']],
        recomendaciones: ['¿Qué acompañamiento puede favorecer el bienestar emocional del niño dentro del entorno familiar?',
          ['Comunicación y escucha', 'Promover espacios de conversación en los que el niño pueda expresar lo que siente.'],
          ['Rutinas previsibles', 'Mantener rutinas previsibles y anticipar los cambios que afecten al niño.'],
          ['Continuidad psicológica', 'Dar continuidad al acompañamiento psicológico y revisar la adaptación al hogar.']]
      }
    }
  },

  etapas: {ENCUENTROS_PREVIOS: 'Encuentros previos a la convivencia', CONVIVENCIA_PREADOPTIVA: 'Convivencia preadoptiva'},

  orientaciones(rol, etapa, tipo) {
    const especialidad = this.especialidades[rol];
    const convivencia = etapa === 'CONVIVENCIA_PREADOPTIVA';
    const base = convivencia ? especialidad.convivencia : especialidad.encuentros;
    const social = rol === 'TRABAJADOR_SOCIAL';
    const relacion = convivencia ? 'las rutinas del hogar' : 'los próximos encuentros';
    const familia = ['TELEFONICO', 'ENTREVISTA_FAMILIAR', 'ORIENTACION_FAMILIAR'].includes(tipo);
    const individual = ['SESION_INDIVIDUAL', 'ENTREVISTA_PSICOLOGICA', 'ENTREVISTA'].includes(tipo);
    let variaciones = {};
    if (familia) {
      const medio = tipo === 'TELEFONICO' ? 'la llamada' : tipo === 'ORIENTACION_FAMILIAR' ? 'la orientación' : 'la entrevista';
      variaciones = {
        adaptacion: [`¿Qué comunicó la familia durante ${medio} sobre la adaptación del niño?`,
          ['Adaptación referida', `La familia informó que el niño se siente progresivamente más cómodo con ${relacion}.`],
          ['Avances comunicados', 'La familia informó que el niño participa con mayor confianza.'],
          ['Adaptación con apoyo', 'La familia comunicó que el niño se beneficia de contar con apoyo al afrontar cambios.']],
        contexto: [social ? `¿Qué comunicó la familia durante ${medio} sobre la organización de los cuidados?`
          : `¿Qué comunicó la familia durante ${medio} sobre el vínculo afectivo?`,
          social ? ['Organización de cuidados', convivencia ? 'La familia informó cómo distribuye los cuidados y quiénes brindan apoyo.'
            : 'La familia informó cómo prevé organizar los cuidados y con quién podría contar.']
            : ['Confianza referida', 'La familia informó que el niño se comunica con mayor confianza.'],
          social ? ['Apoyo familiar', 'La familia identificó personas de confianza a quienes puede acudir para recibir apoyo.']
            : ['Búsqueda de apoyo', 'La familia informó que el niño acude a una figura de confianza cuando necesita ayuda.'],
          social ? ['Orientación solicitada', 'La familia solicitó orientación sobre los cuidados que necesita el niño.']
            : ['Acercamiento referido', 'La familia describió un acercamiento gradual, a un ritmo que el niño considera cómodo.']],
        dificultades: [`¿Qué dificultades o preocupaciones se comunicaron durante ${medio}?`,
          ['Sin dificultades reportadas', 'No se reportaron dificultades relevantes durante la atención.'],
          ['Preocupaciones comunicadas', `La familia expresó inquietudes sobre ${relacion} y solicitó orientación.`],
          ['Información por ampliar', 'La familia señaló que necesita más tiempo para describir las dificultades presentadas.']]
      };
      if (tipo === 'ORIENTACION_FAMILIAR') variaciones.recomendaciones = [
        '¿Qué orientaciones o acuerdos conviene retomar con la familia?',
        ['Revisar acuerdos', `Retomar los acuerdos sobre ${relacion} y revisar las dudas de la familia en la próxima atención.`],
        ...base.recomendaciones.slice(1, 3)];
    } else if (individual) {
      variaciones = {
        adaptacion: [`¿Qué expresó el niño en la sesión sobre ${relacion}?`,
          ['Expresión emocional', `El niño pudo expresar cómo se siente respecto a ${relacion} durante la sesión.`],
          ['Mayor confianza', 'El niño se mostró más cómodo al hablar conforme avanzó la sesión.'],
          ['Sentimientos diversos', 'El niño expresó sentimientos diversos al hablar de los cambios en su entorno.']],
        contexto: ['¿Cómo describe el niño su relación con la familia durante la sesión?',
          ['Confianza expresada', 'El niño expresó que se siente más cómodo al conversar con la familia.'],
          ['Figura de apoyo', 'El niño identificó a una persona de la familia a quien puede pedir ayuda.'],
          ['Acercamiento gradual', 'El niño expresó su preferencia por acercarse a la familia de manera gradual.']],
        dificultades: ['¿Qué preocupaciones expresó el niño durante la sesión?',
          ['Sin dificultades expresadas', 'El niño no expresó preocupaciones relevantes durante la sesión.'],
          ['Preocupación expresada', `El niño expresó inquietud por los cambios relacionados con ${relacion}.`],
          ['Inquietudes difíciles de expresar', 'Durante la sesión, al niño le resultó difícil poner en palabras algunas de sus inquietudes.']]
      };
    } else if (tipo === 'VISITA_DOMICILIARIA') {
      variaciones = {
        adaptacion: [`¿Qué pudo observarse durante la visita en relación con ${relacion}?`,
          convivencia ? ['Participación en el hogar', 'Durante la visita, el niño participó en las actividades cotidianas del hogar.']
            : ['Preparación observada', 'Durante la visita se conversó con la familia sobre la preparación de los próximos encuentros.'],
          ['Rutinas abordadas', 'Durante la visita se conversó con la familia sobre el apoyo que el niño necesita en sus rutinas.'],
          ['Participación por ampliar', 'La visita ofreció información parcial sobre la participación del niño y conviene ampliarla.']],
        contexto: [social ? '¿Qué condiciones de cuidado se pudieron valorar durante la visita?'
          : '¿Qué pudo observarse del vínculo afectivo durante la visita?',
          social ? ['Condiciones del hogar', 'Durante la visita se identificaron espacios y recursos del hogar que pueden apoyar el cuidado del niño.']
            : ['Interacción observada', 'Durante la visita se observó una interacción tranquila entre el niño y la familia.'],
          social ? ['Recursos disponibles', 'Durante la visita se identificaron recursos que la familia puede utilizar para apoyar los cuidados.']
            : ['Escucha familiar', 'Durante la visita se observó que la familia dio espacio al niño para expresar sus preferencias.'],
          social ? ['Cuidados por revisar', 'Durante la visita se identificaron aspectos de la organización del cuidado que requieren seguimiento.']
            : ['Acercamiento gradual', 'Durante la visita el niño se acercó a la familia de forma gradual.']],
        dificultades: ['¿Qué situaciones observadas durante la visita requieren acompañamiento?',
          ['Sin dificultades observadas', 'Durante la visita no se observaron dificultades relevantes.'],
          ['Situación por revisar', 'Durante la visita se identificó una situación que requiere seguimiento.'],
          ['Observación limitada', 'El tiempo de observación fue limitado y conviene revisar este aspecto en otra atención.']]
      };
    } else if (tipo === 'OTRO') {
      variaciones = {
        adaptacion: [`¿Qué información se obtuvo en esta atención sobre ${relacion}?`,
          ['Avances identificados', `La información obtenida indica avances en la adaptación del niño a ${relacion}.`],
          ['Adaptación gradual', 'La información recabada describe una adaptación que avanza gradualmente.'],
          ['Apoyo adicional', 'La información recabada indica que el niño requiere apoyo adicional durante los cambios.']],
        contexto: [social ? '¿Qué información se obtuvo sobre los cuidados y apoyos familiares?'
          : '¿Qué información se obtuvo sobre el vínculo afectivo?',
          social ? ['Cuidados organizados', 'La información disponible indica que la familia ha organizado las tareas de cuidado.']
            : ['Confianza en desarrollo', 'La información disponible indica un aumento gradual de la confianza en la familia.'],
          social ? ['Apoyos disponibles', 'Se identificaron apoyos familiares que pueden contribuir a los cuidados.']
            : ['Apoyo afectivo', 'Se identificó una figura familiar a la que el niño puede acudir en busca de apoyo.'],
          ['Información parcial', 'La información disponible es parcial y conviene ampliarla en otra atención.']],
        dificultades: ['¿Qué dificultades se observaron o se comunicaron durante la atención?',
          ['Sin dificultades comunicadas', 'Durante la atención no se comunicaron dificultades relevantes.'],
          ['Inquietudes identificadas', `La información recabada indica inquietudes relacionadas con ${relacion}.`],
          ['Situación por aclarar', 'La información disponible requiere aclaración antes de valorar las dificultades.']]
      };
    }
    return Object.fromEntries(Object.entries({...base, ...variaciones}).map(([campo, [pregunta, ...opciones]]) => [
      campo, [pregunta, campo === 'recomendaciones' ? opciones
        : [...opciones, ['No evaluado', 'Este aspecto no fue evaluado durante la presente atención.']]]
    ]));
  },

  etiqueta(rol, campo) {
    return {fechaAtencion: 'Fecha de atención', tipo: 'Tipo de seguimiento', etapa: 'Etapa',
      participantes: 'Participantes', proximoSeguimiento: 'Próximo seguimiento previsto'}[campo]
      || this.especialidades[rol].campos[campo] || campo;
  },

  valor(rol, campo, valor) {
    if (valor == null) return 'Sin registrar';
    if (campo === 'tipo') return this.especialidades[rol].tipos[valor] || valor;
    if (campo === 'etapa') return this.etapas[valor] || valor;
    if (campo === 'fechaAtencion' || campo === 'proximoSeguimiento') return Vinculaciones.fechaCivil(valor);
    return valor;
  },

  apartado(id, clases, abierto, cabecera, contenido, accion = '') {
    return `<section class="vinculacion-reporte ${clases}" data-apartado="${id}">
      <h3 class="reporte-encabezado"><button type="button" class="reporte-desplegar" data-desplegar-reporte
        aria-expanded="${abierto}" aria-controls="contenido-${id}">${cabecera}
        <span class="reporte-chevron">${Profesionales.icono('flecha')}</span></button></h3>
      ${accion ? `<div class="reporte-accion">${accion}</div>` : ''}
      <div id="contenido-${id}" class="vinculacion-reporte-contenido" ${abierto ? '' : 'hidden'}>${contenido}</div>
    </section>`;
  },

  panel(datos, abiertos) {
    return `<div class="vinculacion-reportes">${Object.entries(this.especialidades).map(([rol, especialidad]) => {
      const consulta = datos.rolesConsulta.includes(rol);
      const registros = datos.registros.filter(registro => registro.rol === rol);
      const ultimo = registros[0];
      const resumen = ultimo ? 'Último seguimiento: ' + Vinculaciones.fechaCivil(ultimo.fechaAtencion)
        : 'Sin seguimientos registrados';
      const proximo = ultimo?.contenido.proximoSeguimiento;
      const cabecera = `<span class="vinculacion-emblema">${Profesionales.icono(especialidad.icono)}</span>
        <span>${especialidad.titulo}${consulta ? `<small class="seguimiento-meta">${resumen}</small>
        ${proximo ? `<small class="seguimiento-meta">Próximo seguimiento previsto: ${Vinculaciones.fechaCivil(proximo)}</small>` : ''}` : ''}</span>`;
      const contenido = consulta ? registros.map(registro => this.tarjeta(registro)).join('')
        : '<p>La consulta está reservada a la especialidad correspondiente y al Comité.</p>';
      const accion = datos.rolRegistro === rol
        ? `<button type="button" class="boton" data-registrar-seguimiento>${Profesionales.icono('nuevo')}Registrar seguimiento</button>` : '';
      return this.apartado(rol, especialidad.color, abiertos.has(rol), cabecera, contenido, accion);
    }).join('')}</div>`;
  },

  tarjeta(registro) {
    const {id, rol, profesional, fechaAtencion, etapa, contenido, puedeEditar} = registro;
    const observaciones = contenido.adaptacion.replace(/\s+/g, ' ');
    const resumen = observaciones.length > 180 ? observaciones.slice(0, 177) + '…' : observaciones;
    const icono = {
      ORIENTACION_FAMILIAR: 'conversacion', INTERRELACION: 'personas', ENCUENTRO: 'personas',
      VISITA_DOMICILIARIA: 'hogar', TELEFONICO: 'telefono', SESION_INDIVIDUAL: 'persona',
      SESION_FAMILIAR: 'personas', OBSERVACION_VINCULO: 'heart-handshake'
    }[contenido.tipo] || 'informe';
    return `<article class="pro-informe seguimiento-tarjeta">
      <header class="seguimiento-tarjeta-cabecera">
        <span class="vinculacion-emblema">${Profesionales.icono(icono)}</span>
        <div><h4>${limpiarHtml(this.valor(rol, 'tipo', contenido.tipo))}</h4>
          <p class="ayuda">${limpiarHtml(Vinculaciones.fechaCivil(fechaAtencion))} · ${limpiarHtml(profesional)}</p></div>
      </header>
      <span class="seguimiento-tarjeta-etapa">${limpiarHtml(this.etapas[etapa])}</span>
      <p class="seguimiento-tarjeta-resumen">${limpiarHtml(resumen)}</p>
      <div class="pro-acciones">
        <button type="button" class="enlace reporte-editar" data-ver-seguimiento="${id}">${Profesionales.icono('consulta')}Ver seguimiento</button>
        ${puedeEditar ? `<button type="button" class="enlace reporte-editar" data-editar-seguimiento="${id}">${Profesionales.icono('editar')}Editar</button>` : ''}
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

  opcionesParticipantes: ['Niño', 'Familia', 'Trabajador social', 'Psicólogo', 'Cuidador', 'Otro'],

  enlazarParticipantes(form, anterior = '') {
    const grupo = form.querySelector('.seguimiento-participantes');
    const opciones = [...grupo.querySelectorAll('input[type=checkbox]')];
    const otro = opciones.at(-1);
    const detalle = grupo.querySelector('textarea');
    const campo = form.elements.participantes;
    const separar = anterior.match(/^(?:(.*?), )?Otro: ([\s\S]+)$/);
    const listado = separar ? separar[1] || '' : anterior;
    const nombres = listado.split(', ');
    const conocidas = opciones.filter(opcion => nombres.includes(opcion.value));
    const canonico = conocidas.map(opcion => opcion.value).join(', ') === listado;
    const legado = Boolean(anterior) && !canonico;
    if (legado) {
      otro.checked = true;
      detalle.value = anterior;
    } else {
      conocidas.forEach(opcion => { opcion.checked = true; });
      if (separar) {
        otro.checked = true;
        detalle.value = separar[2];
      }
    }
    const descripcionInicial = detalle.value;
    const actualizar = () => {
      detalle.closest('label').hidden = !otro.checked;
      detalle.disabled = !otro.checked;
      const seleccionadas = opciones.filter(opcion => opcion.checked && opcion !== otro).map(opcion => opcion.value);
      if (otro.checked) {
        const descripcion = detalle.value.trim();
        seleccionadas.push(legado && detalle.value === descripcionInicial && !seleccionadas.length ? anterior
          : descripcion ? 'Otro: ' + descripcion : 'Otro');
      }
      campo.value = seleccionadas.join(', ');
    };
    // Las casillas opcionales no necesitan la validación de campos de texto del formulario.
    grupo.addEventListener('input', evento => {
      if (opciones.includes(evento.target)) evento.stopPropagation();
    });
    grupo.addEventListener('change', evento => {
      if (!opciones.includes(evento.target)) return;
      evento.stopPropagation();
      actualizar();
    });
    detalle.addEventListener('input', actualizar);
    actualizar();
    return detalle;
  },

  formulario(vinculacion, datos, registro, disparador, alGuardar) {
    const rol = registro?.rol || datos.rolRegistro;
    const especialidad = this.especialidades[rol];
    const dialogo = this.dialogo((registro ? 'Editar ' : 'Registrar ') + especialidad.titulo.toLowerCase(), disparador);
    dialogo.classList.add('seguimiento-dialogo', especialidad.color);
    const titulo = dialogo.querySelector('h2');
    const cabecera = document.createElement('header');
    cabecera.className = 'seguimiento-titulo';
    titulo.before(cabecera);
    cabecera.innerHTML = `<span class="vinculacion-emblema">${Profesionales.icono(especialidad.icono)}</span>`;
    cabecera.append(titulo);
    cabecera.insertAdjacentHTML('beforeend', this.contexto(vinculacion));
    dialogo.insertAdjacentHTML('beforeend', `
      <form class="pro-interrupcion-form" novalidate>
        <div class="seguimiento-desplazable">
          <p class="seguimiento-etapa">Etapa del proceso <strong data-etapa></strong></p>
          <div class="seguimiento-cabecera-campos">
            <label>Fecha de atención *<input type="date" lang="es-PA" name="fechaAtencion" required min="${limpiarHtml(datos.primeraFecha)}" max="${limpiarHtml(datos.hoy)}"></label>
            <label>Tipo de seguimiento *<select name="tipo" required><option value="">Selecciona el tipo de seguimiento</option>
            ${Object.entries(especialidad.tipos).filter(([tipo]) => !['ENCUENTRO', 'ENTREVISTA'].includes(tipo) || tipo === registro?.contenido.tipo).map(([valor, nombre]) => `<option value="${valor}">${nombre}</option>`).join('')}
            </select></label>
          </div>
          <fieldset class="seguimiento-participantes">
            <legend>Participantes (opcional) <small>Selecciona uno o varios</small></legend>
            <div class="pro-sugerencias">
              ${this.opcionesParticipantes.map(nombre => `
                <label class="seguimiento-opcion"><input type="checkbox" value="${nombre}">
                  <span class="pro-sugerencia">${Profesionales.icono('comprobar')}${nombre}</span></label>`).join('')}
            </div>
            <label hidden>Especifica quién participó<textarea id="seguimiento-otro-participante" rows="1" maxlength="500"></textarea></label>
            <input type="hidden" name="participantes">
          </fieldset>
          <h3>Observaciones profesionales</h3>
          ${Object.entries(especialidad.campos).map(([campo, etiqueta]) => `
            <div><label for="seguimiento-${campo}">${etiqueta} *</label>
            <p class="seguimiento-pregunta" id="pregunta-${campo}"></p>
            <textarea id="seguimiento-${campo}" name="${campo}" aria-describedby="pregunta-${campo}" rows="2" maxlength="2000" required></textarea></div>`).join('')}
          <label>Fecha prevista del próximo seguimiento (opcional)<input type="date" lang="es-PA" name="proximoSeguimiento"></label>
          ${registro ? '<label>Motivo de modificación (opcional)<textarea name="motivoModificacion" rows="2" maxlength="1000"></textarea></label>' : ''}
        </div>
        <footer class="seguimiento-pie">
          <p class="mensaje error" role="alert" hidden></p>
          <div class="pro-acciones">
            <button class="boton secundario" type="button" data-cancelar>Cancelar</button>
            <button class="boton" type="submit">${registro ? 'Guardar cambios' : 'Guardar seguimiento'}</button>
          </div>
        </footer>
      </form>`);
    const form = dialogo.querySelector('form');
    form.querySelector('.seguimiento-desplazable').prepend(cabecera);
    CamposFormulario.iniciar(form);
    const {fechaAtencion: fecha, proximoSeguimiento: proxima, tipo} = form.elements;
    fecha.value = registro?.fechaAtencion || datos.hoy;
    Object.entries(registro?.contenido || {}).forEach(([campo, valor]) => {
      if (campo !== 'participantes' && form.elements[campo]) form.elements[campo].value = valor ?? '';
    });
    const participantes = this.enlazarParticipantes(form, registro?.contenido.participantes ?? '');
    let contextoActual;
    const actualizarEtapa = () => {
      proxima.min = fecha.value;
      const etapa = registro && fecha.value === registro.fechaAtencion ? registro.etapa
        : datos.inicioConvivencia && fecha.value >= datos.inicioConvivencia ? 'CONVIVENCIA_PREADOPTIVA' : 'ENCUENTROS_PREVIOS';
      dialogo.querySelector('[data-etapa]').textContent = fecha.value ? this.etapas[etapa] : 'Selecciona la fecha de atención';
      const contexto = etapa + ':' + tipo.value;
      if (contexto === contextoActual) return;
      contextoActual = contexto;
      Object.entries(this.orientaciones(rol, etapa, tipo.value)).forEach(([campo, [pregunta, opciones]]) => {
        const control = form.elements[campo];
        dialogo.querySelector('#pregunta-' + campo).textContent = pregunta;
        control.parentElement.querySelector('.pro-sugerencias')?.remove();
        Profesionales.sugerencias(control, opciones, {seleccionUnica: true});
      });
    };
    fecha.addEventListener('input', actualizarEtapa);
    tipo.addEventListener('change', actualizarEtapa);
    actualizarEtapa();

    // La clave pertenece a este envío y se conserva durante los reintentos del formulario.
    const claveRegistro = registro ? null : this.claveRegistro();
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
      error.hidden = true;
      const invalido = (campo, mensaje) => {
        CamposFormulario.error(campo, mensaje);
        campo.focus();
      };
      if (!fecha.value) return invalido(fecha, 'Selecciona la fecha de atención.');
      if (!fecha.validity.valid) return invalido(fecha, 'Selecciona una fecha de atención dentro del intervalo permitido.');
      if (!tipo.value) return invalido(tipo, 'Selecciona el tipo de seguimiento.');
      if (!proxima.validity.valid) return invalido(proxima, 'El próximo seguimiento no puede ser anterior a la fecha de atención.');
      const payload = Object.fromEntries(new FormData(form));
      if (payload.participantes.length > 500) return invalido(participantes, 'Los participantes admiten hasta 500 caracteres en total.');
      for (const campo of Object.keys(especialidad.campos)) {
        if (!payload[campo].trim()) return invalido(form.elements[campo], `Completa el campo «${this.etiqueta(rol, campo)}».`);
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
