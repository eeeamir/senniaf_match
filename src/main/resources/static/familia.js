/* Presentación familiar: reutiliza la entrevista y expone únicamente actividad autorizada del caso propio. */
const Familia = {
  async mostrar(sesion, {actualizarSeguimiento = false} = {}) {
    if (this.cargando || document.querySelector('[data-reincorporacion-dialogo][open]')) return;
    this.cargando = true;
    const solicitada = new URLSearchParams(location.search).get('vista');
    const vista = ['entrevista', 'propuesta', 'historial'].includes(solicitada)
      ? solicitada
      : 'inicio';
    document.body.classList.toggle('s8-acabado', vista !== 'inicio');
    document.body.classList.toggle('familia-portal-proceso', vista === 'inicio' || vista === 'propuesta');
    Profesionales.iniciar(sesion, vista);
    try {
      const r = await fetch('/api/familias/mi-perfil', {
        headers: Sesion.encabezados(),
        cache: 'no-store',
      });
      if (r.status === 401) return Sesion.salir();
      if (!r.ok && r.status !== 404)
        throw new Error('No se pudo consultar tu proceso. Inténtalo de nuevo.');
      const sinEntrevista = r.status === 404;
      if (sinEntrevista && vista === 'entrevista') return location.replace('perfil.html');
      const vinculaciones = await Vinculaciones.api('/api/vinculaciones/propias');
      this.informacionAutorizada = new Set(vinculaciones.filter(v => v.puedeConsultarInformacion).map(v => String(v.id)));
      document.querySelectorAll('[data-asignacion-info]').forEach(dialogo => {
        if (!this.informacionAutorizada.has(dialogo.dataset.asignacionInfo)) dialogo.close();
      });
      if (document.querySelector('[data-reincorporacion-dialogo][open]')
        || (actualizarSeguimiento && document.querySelector('dialog[open]'))) return;
      const pendiente = vinculaciones.find((v) => v.estado === 'PENDIENTE_RESPUESTA');
      const p = sinEntrevista ? { perfilCompleto: false, actividad: [] } : await r.json();
      const enlaceEntrevista = sinEntrevista ? 'perfil.html' : 'perfil.html?reevaluacion=1';
      let version = JSON.stringify([vista, p, vinculaciones]);
      // Evita redibujar información idéntica y conserva los desplegables abiertos durante el refresco.
      if (version === this.version && vista !== 'entrevista') return;
      let entrevista = '';
      if (vista === 'entrevista') {
        const d = await fetch('/api/familias/mi-perfil/detalle', {
          headers: Sesion.encabezados(),
          cache: 'no-store',
        });
        if (d.status === 401) return Sesion.salir();
        if (!d.ok) throw new Error('No se pudo consultar tu entrevista.');
        const datos = await d.json();
        entrevista = this.entrevista(datos, p);
        version = JSON.stringify([vista, p, datos, vinculaciones]);
        if (version === this.version) return;
      }
      this.version = version;
      const ultima = Vinculaciones.seleccionarVinculacion(vinculaciones);
      const disponibleDeNuevo = (ultima?.estado === 'CERRADA' && ultima.familiaDisponible)
        || (ultima?.resultadoCierre === 'ADOPCION_FORMALIZADA' && ultima.reincorporacion?.disponible);
      const vigente = disponibleDeNuevo ? null : ultima;
      const progreso = Vinculaciones.obtenerProgreso(vigente);
      const eventosVinculacion = vinculaciones.flatMap((v) => Vinculaciones.obtenerProgreso(v).eventos);
      const eventos = [
        ...(p.actividad || []).filter(e => e.titulo !== 'Propuesta enviada por el Comité'
          || !vinculaciones.some(v => +new Date(v.fechaCreacion) === +new Date(e.fecha))),
        ...eventosVinculacion,
      ].sort((a, b) => new Date(b.fecha) - new Date(a.fecha));
      const ultimo = eventos[0];
      const disponible = disponibleDeNuevo || p.disponibleParaMatch === true;
      let estado = 'Entrevista incompleta';
      let mensaje = 'Completa la información de tu hogar para continuar.';
      let accion = 'Revisa y completa tu entrevista.';
      if (disponible) {
        estado = ultima?.reincorporacion?.estado === 'AUTORIZADA' && disponibleDeNuevo
          ? 'Pendiente de vinculación' : 'En espera de una propuesta';
        mensaje = 'Tu expediente está disponible para recibir propuestas del Comité.';
        accion = 'Esperar una propuesta del Comité.';
      } else if (p.perfilCompleto) {
        estado = 'Expediente familiar registrado';
        mensaje = 'Consulta las actualizaciones de tu proceso.';
        accion = 'Esperar una actualización del Comité.';
      }
      if (p.reevaluacionPendiente) {
        estado = 'Entrevista en revisión';
        mensaje = 'El Comité solicitó actualizar la información de tu hogar.';
        accion = 'Revisa y completa tu entrevista.';
      }
      if (progreso) {
        ({estado, mensaje, accion} = progreso);
        if (vigente.estado === 'CERRADA' && vigente.interrupcionRegistrada) {
          estado = 'Vinculación cerrada';
          mensaje = 'Cierre por interrupción de convivencia.';
          if (vigente.estadoDisponibilidadFamiliar === 'PENDIENTE_REVISION') accion = 'Esperar la revisión del Comité.';
        }
      }
      const enlaceProceso = pendiente ? ['match.html?vista=propuesta', 'Consultar mi vinculación']
        : p.reevaluacionPendiente || !p.perfilCompleto
          ? [enlaceEntrevista, p.perfilCompleto ? 'Revisar entrevista' : 'Completar mi entrevista'] : null;
      const proceso = Vinculaciones.inicioFamiliar(vigente, estado, mensaje, disponible, enlaceProceso);
      const antecedente = vinculaciones.filter(v => v.resultadoCierre === 'ADOPCION_FORMALIZADA')
        .sort((a, b) => (b.fechaCierre || '').localeCompare(a.fechaCierre || '') || Number(b.id) - Number(a.id))[0];
      const mostrarReincorporacion = antecedente && !vinculaciones.some(v =>
        Number(v.id) > Number(antecedente.id) || ['PENDIENTE_RESPUESTA', 'RESPUESTA_ACEPTADA'].includes(v.estado));
      const propuesta = Vinculaciones.familiar(vinculaciones)
        + (mostrarReincorporacion ? this.reincorporacion(antecedente) : '');
      const actividad = (todos = false) => {
        const visibles = todos ? eventos : eventos.slice(0, 3);
        return `
          <section class="tarjeta">
            <div class="pro-fila-titulo">
              <h2>${Profesionales.icono('reloj')}
                ${todos ? 'Actualizaciones registradas' : 'Actividad reciente'}</h2>
              ${todos ? '' : `
                <a class="boton secundario chico" href="match.html?vista=historial">
                  Ver historial completo →
                </a>`}
            </div>
            ${visibles.length ? todos
              ? Vinculaciones.lineaTiempo(visibles.map(evento => ({...evento, resumen: evento.descripcion, zona: 'Panamá'})))
              : `<ul class="familia-actividad-reciente">${visibles.map(evento => `<li>
                  <strong>${limpiarHtml(evento.titulo)}</strong><time>${Vinculaciones.fechaCivil(evento.fecha)}</time></li>`).join('')}</ul>`
              : '<p>Todavía no hay actuaciones disponibles.</p>'}
            ${todos && p.numeroReevaluaciones ? `
              <p class="ayuda">Reevaluaciones completadas: ${Number(p.numeroReevaluaciones)}.
                Solo se muestran fechas que quedaron registradas.</p>` : ''}
          </section>`;
      };
      const tarjetas = [
        ['carpeta', 'Estado actual', estado],
        ['calendario', 'Última actualización', ultimo
          ? ultimo.titulo
          : 'Sin actualizaciones registradas'],
        ['comprobar', 'Próxima acción', accion]
      ];
      const resumen = `
        <div class="familia-resumen">
          ${tarjetas.map(([icono, titulo, texto]) => `
            <section class="tarjeta">
              <span class="pro-simbolo">${Profesionales.icono(icono)}</span>
              <h3>${titulo}</h3><p>${limpiarHtml(texto)}</p>
              ${icono === 'calendario' && ultimo ? `<time>${Vinculaciones.fechaCivil(ultimo.fecha)}</time>` : ''}
            </section>`).join('')}
        </div>`;
      const pantallas = {
        inicio: [
          'Bienvenida, familia',
          'Consulta el estado de tu proceso de adopción.',
          'hogar',
        ],
        entrevista: [
          'Mi entrevista',
          'Consulta las respuestas de tu hogar y actualízalas cuando cambie tu situación.',
          'informe',
        ],
        propuesta: [
          'Mi vinculación',
          'Consulta las etapas, los tiempos y las actualizaciones de tu proceso.',
          'vinculacion',
        ],
        historial: [
          'Historial de mi proceso',
          'Consulta las actualizaciones registradas durante tu proceso de adopción',
          'reloj',
        ],
      };
      const [titulo, descripcion, icono] = pantallas[vista];
      let contenidoVista = proceso + resumen + actividad();
      if (vista === 'entrevista') contenidoVista = entrevista;
      if (vista === 'historial') contenidoVista = actividad(true);
      if (vista === 'propuesta') contenidoVista = propuesta;
      const avisoPropuesta = pendiente && !['inicio', 'propuesta'].includes(vista) ? `
        <section class="tarjeta" role="status">
          ${Profesionales.icono('informacion')} Tienes una propuesta pendiente de respuesta.
          <a href="match.html?vista=propuesta">Consultar mi vinculación</a>
        </section>` : '';
      if (actualizarSeguimiento && document.querySelector('dialog[open]')) return;
      // Una actualización de datos conserva las tarjetas que la familia abrió o cerró.
      const tarjetasPrevias = new Map([...document.querySelectorAll('details[data-vinculacion-familiar]')]
        .map(tarjeta => [tarjeta.dataset.vinculacionFamiliar, tarjeta.open]));
      document.getElementById('contenido').innerHTML =
        Profesionales.encabezado(titulo, descripcion, icono) + avisoPropuesta
        + (['inicio', 'entrevista'].includes(vista) ? this.avisoReevaluacion(p) : '')
        + contenidoVista;
      document.querySelectorAll('details[data-vinculacion-familiar]').forEach(tarjeta => {
        const abierta = tarjetasPrevias.get(tarjeta.dataset.vinculacionFamiliar);
        if (abierta !== undefined) tarjeta.open = abierta;
      });
      Vinculaciones.enlazarRespuestas(sesion);
      document.querySelector('[data-solicitar-reincorporacion]')?.addEventListener('click', () =>
        this.confirmarReincorporacion(antecedente, sesion));
    } catch (e) {
      if (actualizarSeguimiento) throw e;
      this.version = null;
      document.getElementById('contenido').innerHTML =
        `<section class="tarjeta"><p role="alert">${limpiarHtml(e.message)}</p></section>`;
    } finally {
      this.cargando = false;
    }
  },
  reincorporacion(v) {
    const {estado, puedeSolicitar, disponible, motivoNoAutorizacion} = v.reincorporacion || {};
    const estados = {
      ENTREVISTA_PENDIENTE: ['Solicitud registrada', 'comprobarCircular', 'completado'],
      REVISION_PENDIENTE: ['Pendiente de revisión institucional', 'reloj', 'pendiente'],
      AUTORIZADA: ['Reincorporación aprobada', 'comprobarCircular', 'completado'],
      NO_AUTORIZADA: ['Reincorporación no autorizada', 'advertencia', 'no-autorizado'],
    };
    const estadoVisible = estados[estado];
    if (!puedeSolicitar && !estadoVisible) return '';
    const entrevistaPendiente = estado === 'ENTREVISTA_PENDIENTE';
    const aprobada = estado === 'AUTORIZADA';
    const pasos = entrevistaPendiente
      ? [
        ['1', 'Actualiza y envía tu entrevista familiar', 'Completa la información actual de tu hogar.', 'actual'],
        ['2', 'Revisión institucional', 'Espera la evaluación de tu expediente.', 'en-espera'],
      ]
      : [
        [Profesionales.icono('comprobar'), 'Entrevista familiar actualizada y enviada', 'Completado', 'completado'],
        [Profesionales.icono(aprobada ? 'comprobar' : estado === 'NO_AUTORIZADA' ? 'advertencia' : 'reloj'),
          'Revisión institucional', aprobada ? 'Completado' : estado === 'NO_AUTORIZADA' ? 'No autorizada' : 'Pendiente',
          aprobada ? 'completado' : estado === 'NO_AUTORIZADA' ? 'no-autorizado' : 'pendiente'],
      ];
    const aviso = {
      REVISION_PENDIENTE: 'Tu información ha sido enviada correctamente. Por el momento, no necesitas realizar ninguna acción adicional.',
      AUTORIZADA: disponible ? 'Tu expediente ha sido reincorporado. Podrás ser considerado para futuros procesos de adopción.'
        : 'La reincorporación fue autorizada. La participación depende de las condiciones de elegibilidad vigentes.',
      NO_AUTORIZADA: 'El Comité resolvió la solicitud. Tu expediente permanece fuera del Match.',
    }[estado];
    return `<section class="tarjeta familia-reincorporacion">
      <header><span class="pro-simbolo">${Profesionales.icono('solicitudes')}</span><div>
        <h3>Reincorporación del expediente</h3><p>Expediente familiar ${limpiarHtml(v.expedienteFamiliar)}</p>
      </div></header>
      ${puedeSolicitar ? `<p>Si deseas participar en un nuevo proceso de adopción, puedes solicitar la reincorporación de tu expediente.</p>
        <div class="pro-acciones"><button class="boton" type="button" data-solicitar-reincorporacion>Solicitar reincorporación</button></div>` : ''}
      ${estadoVisible ? `<div class="reincorporacion-estado ${estadoVisible[2]}">
        ${Profesionales.icono(estadoVisible[1])}<strong>${estadoVisible[0]}</strong></div>
        <ol class="reincorporacion-pasos${aprobada ? ' reincorporacion-pasos-aprobados' : ''}">${pasos.map(([icono, texto, detalle, clase]) =>
          `<li class="${clase}"><span class="reincorporacion-indicador" aria-hidden="true">${icono}</span><strong>${texto}</strong><small>${detalle}</small></li>`).join('')}</ol>
        ${aviso ? `<p class="familia-siguiente reincorporacion-aviso">${Profesionales.icono('informacion')}<span>${aviso}</span></p>` : ''}
        ${estado === 'NO_AUTORIZADA' && motivoNoAutorizacion ? `<p class="pro-texto-registrado"><strong>Motivo de la decisión</strong><br>${limpiarHtml(motivoNoAutorizacion)}</p>` : ''}
        ${entrevistaPendiente ? `<div class="pro-acciones"><a class="boton" href="perfil.html?reevaluacion=1&reincorporacion=${encodeURIComponent(v.id)}">Actualizar entrevista ${Profesionales.icono('flecha')}</a></div>` : ''}
      ` : ''}
      </section>`;
  },
  confirmarReincorporacion(v, sesion) {
    if (!v?.reincorporacion?.puedeSolicitar || document.querySelector('[data-reincorporacion-dialogo]')) return;
    const d=document.createElement('dialog');
    d.className='pro-dialogo';d.dataset.reincorporacionDialogo='';d.setAttribute('aria-label','Solicitar reincorporación');
    d.innerHTML = `<header class="pro-modal-cabecera"><span class="pro-simbolo">${Profesionales.icono('solicitudes')}</span>
      <div><h2>Solicitar reincorporación</h2><p>Expediente familiar ${limpiarHtml(v.expedienteFamiliar)}</p></div></header>
      <p><strong>Para participar en un nuevo proceso de adopción:</strong></p>
      <ol class="reincorporacion-pasos">
        <li><span class="reincorporacion-indicador" aria-hidden="true">1</span>Actualiza y envía tu entrevista familiar.</li>
        <li><span class="reincorporacion-indicador" aria-hidden="true">2</span>Espera la revisión institucional.</li>
      </ol>
      <p class="familia-siguiente reincorporacion-aviso">${Profesionales.icono('informacion')}<span>Conservarás tu cuenta, expediente e historial de vinculaciones.</span></p>
      <p class="mensaje error" role="alert"></p><div class="pro-acciones"><button type="button" class="boton secundario" data-cancelar>Cancelar</button>
      <button type="button" class="boton" data-confirmar>Confirmar solicitud</button></div>`;
    const confirmar=d.querySelector('[data-confirmar]'), cancelar=d.querySelector('[data-cancelar]');
    d.onclose=()=>d.remove();
    d.oncancel=e=>{if(confirmar.disabled)e.preventDefault();};
    cancelar.onclick=()=>d.close();
    confirmar.onclick=async()=>{
      confirmar.disabled=cancelar.disabled=true;
      try {
        await Vinculaciones.api(`/api/reincorporaciones/vinculaciones/${encodeURIComponent(v.id)}`,{method:'POST',body:JSON.stringify({confirmacion:true})});
        d.close();
        await this.mostrar(sesion);
      } catch(error) {
        d.querySelector('[role=alert]').textContent=error.message;
        confirmar.disabled=cancelar.disabled=false;
      }
    };
    document.body.append(d);
    d.showModal();
  },
 avisoReevaluacion(p){
  if(!p.reevaluacionPendiente)return '';
  return `<aside class="tarjeta familia-revision-pendiente" role="status"><h2>${Profesionales.icono('solicitudes')}Tienes una actualización pendiente en tu entrevista</h2><p>El Comité solicitó revisar algunos apartados de tu entrevista para continuar con la evaluación de tu expediente.</p>${p.apartadosReevaluacion?.length?`<ul>${p.apartadosReevaluacion.map(t=>`<li>${limpiarHtml(t)}</li>`).join('')}</ul>`:''}<p class="pro-texto-registrado">${limpiarHtml(p.observacionReevaluacion||'Consulta la solicitud con el equipo responsable si necesitas orientación.')}</p><a class="boton secundario" href="match.html?vista=entrevista">Revisar mi entrevista</a></aside>`;
 },
  entrevista(d, p) {
    const grupos = {
      'Hogar y red de apoyo': [
        'tipoFamilia',
        'especificacionHogar',
        'notaEstructuraHogar',
        'redApoyo',
        'notaRedApoyo',
      ],
      'Preferencias familiares': [
        'edadMinimaAceptada',
        'edadMaximaAceptada',
        'notaRangoEdad',
        'disposicionHermanos',
        'notaHermanos',
      ],
      'Salud y acompañamiento': [
        'condicionesPuedeAcompanarHoy',
        'condicionesConApoyo',
        'notaCondicionesSalud',
      ],
      'Disponibilidad y entorno': ['provincia', 'disponibilidadViaje', 'notaDistancia'],
      'Apoyos culturales y comunitarios': [
        ...Profesionales.culturaFamilia.map(([k]) => k),
        'continuidadCultural',
      ],
    };
    const etiquetas = {
      ...Object.fromEntries(Profesionales.culturaFamilia.map(([k, t]) => [k, t])),
      tipoFamilia: 'Composición del hogar',
      especificacionHogar: 'Descripción de otra estructura del hogar',
      notaEstructuraHogar: 'Características familiares',
      redApoyo: 'Red de apoyo',
      notaRedApoyo: 'Organización del apoyo',
      edadMinimaAceptada: 'Edad mínima',
      edadMaximaAceptada: 'Edad máxima',
      notaRangoEdad: 'Acompañamiento según la edad',
      disposicionHermanos: 'Disposición ante hermanos',
      notaHermanos: 'Vínculos entre hermanos',
      condicionesPuedeAcompanarHoy: 'Capacidad actual',
      condicionesConApoyo: 'Con apoyo adicional',
      notaCondicionesSalud: 'Cuidados y acompañamiento',
      provincia: 'Provincia',
      disponibilidadViaje: 'Disponibilidad de viaje',
      notaDistancia: 'Desplazamientos',
      continuidadCultural: 'Información cultural registrada anteriormente',
    };
    const seleccion = p.apartadosReevaluacion || [];
    const iconos = ['personas', 'estrella', 'salud', 'hogar', 'personas'];
    const contenido = Object.entries(grupos)
      .map(([titulo, campos], indice) => {
        const visibles = campos.filter((campo) => {
          if (campo === 'especificacionHogar') return d.tipoFamilia === 'OTRA';
          if (campo === 'continuidadCultural') {
            return Profesionales.mostrarAntecedenteCultural(
              d.continuidadCultural,
              d,
              Profesionales.culturaFamilia.map(([clave]) => clave),
            );
          }
          return true;
        });
        return `
      <details class="familia-entrevista-seccion">
        <summary>
          <span class="pro-simbolo">${Profesionales.icono(iconos[indice])}</span>
          <span>${titulo}${
            seleccion.includes(titulo)
              ? '<small class="familia-revision-indicador">Revisión solicitada</small>'
              : ''
          }</span>
          <span class="familia-desplegar" aria-hidden="true">⌄</span>
        </summary>
        <dl class="detalle-datos">
          ${visibles
            .map(
              (campo) => `
            <div><dt>${etiquetas[campo]}</dt>
              <dd>${limpiarHtml(this.valorEntrevista(d[campo]))}</dd></div>`,
            )
            .join('')}
        </dl>
      </details>`;
      })
      .join('');
    return `
    <section class="tarjeta familia-entrevista-resumen">
      <div>
        <span class="pro-capsula">${p.perfilCompleto ? 'Entrevista registrada' : 'Entrevista incompleta'}</span>
        <h2>${limpiarHtml(p.codigoCaso)}</h2>
        <p>${
          p.reevaluacionPendiente
            ? 'El Comité solicitó revisar tu entrevista. Atiende los apartados señalados antes de enviar las correcciones.'
            : 'Puedes actualizar tu entrevista cuando cambie la situación de tu hogar.'
        }</p>
      </div>
      <a class="boton" href="perfil.html?reevaluacion=1">
        ${Profesionales.icono('informe')}${p.perfilCompleto ? 'Editar entrevista' : 'Completar mi entrevista'}
      </a>
    </section>${contenido}`;
  },
  valorEntrevista(valor) {
    if (valor == null || valor === '') return 'Sin información registrada';
    if (Array.isArray(valor)) {
      return valor.length
        ? valor.map((item) => Profesionales.etiqueta(item)).join(', ')
        : 'Sin condiciones específicas declaradas';
    }
    if (typeof valor === 'number') return valor;
    return /^[A-Z_]+$/.test(valor) ? Profesionales.etiqueta(valor) : valor;
  },
};
