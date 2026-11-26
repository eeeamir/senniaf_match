/* Comparte las vistas de vinculaciones sin presumir acogimiento ni adopción formalizada. */
const Vinculaciones = {
  estados: {
    RECHAZADA_POR_FAMILIA: 'Propuesta rechazada por la familia',
    CERRADA: 'Vinculación cerrada',
    PENDIENTE_RESPUESTA: 'Pendiente de respuesta familiar',
    RESPUESTA_ACEPTADA: 'Propuesta aceptada por la familia',
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
  estado(v, familiar = false) {
    const rechazado = ['RECHAZADA_POR_FAMILIA', 'RESPUESTA_RECHAZADA_PENDIENTE_CIERRE']
      .includes(v.estado);
    let icono = 'comprobar';
    let clase = v.estado === 'RESPUESTA_ACEPTADA' ? 'vinculacion-positiva' : '';
    if (v.estado === 'PENDIENTE_RESPUESTA') {
      icono = 'reloj';
      clase = 'vinculacion-pendiente';
    } else if (rechazado || v.estado === 'CERRADA') {
      icono = rechazado ? 'advertencia' : 'informacion';
      clase = rechazado ? 'vinculacion-negativa' : 'retirado';
    }
    const etiqueta = familiar && v.estado === 'RESPUESTA_ACEPTADA'
      ? 'Propuesta aceptada' : this.estados[v.estado] || v.estado;
    return `
      <div class="vinculacion-estados">
        <span class="chip ${clase}">${Profesionales.icono(icono)}${limpiarHtml(etiqueta)}</span>
        ${v.interrupcionRegistrada && v.estado !== 'CERRADA' ? `
          <span class="chip retirado vinculacion-antecedente">
            ${Profesionales.icono('advertencia')}
            ${v.interrupcionPendiente ? 'Interrupción pendiente de revisión' : 'Interrupción revisada'}
          </span>` : ''}
      </div>`;
  },
  familiar(lista) {
    if (!lista.length) {
      return `
        <section class="tarjeta">
          <h2>${Profesionales.icono('vinculacion')}Mi vinculación</h2>
          <p>Por ahora no tienes vinculaciones registradas.</p>
        </section>`;
    }
    return `
      <div class="vinculaciones-lista familiar-amplia">
        ${lista
          .map(
            (v) => `
          <section class="tarjeta vinculacion-familiar vinculacion-tarjeta">
            <header class="vinculacion-cabecera">
              <div class="familia-identificacion">
                <span class="vinculacion-emblema">${Profesionales.icono('heart-handshake')}</span>
                <div>
                  <h2>${limpiarHtml(v.codigo)} · Mi vinculación</h2>
                  <p>Expediente familiar ${limpiarHtml(v.expedienteFamiliar)}</p>
                  <p>Propuesta recibida: ${limpiarHtml(this.fechaHora(v.fechaCreacion))}</p>
                </div>
              </div>
              ${this.estado(v, true)}
            </header>
            ${this.progresoFamiliar(v)}
            ${this.eventosFamiliares(v)}
            <div class="familia-propuesta-acciones">
              ${
                v.puedeConsultarInformacion
                  ? `
                <section class="familia-informacion-asignada">
                  <span class="vinculacion-emblema">${Profesionales.icono('informe')}</span>
                  <div>
                    <h3>Información del niño asignado</h3>
                    <p>Consulta la información autorizada de esta asignación.</p>
                  </div>
                  <button type="button" class="boton secundario chico" data-informacion-nino="${v.id}">
                    ${Profesionales.icono('consulta')}Ver información ${Profesionales.icono('flecha')}
                  </button>
                </section>`
                  : ''
              }
              ${
                v.estado === 'PENDIENTE_RESPUESTA'
                  ? `
                <div class="familia-respuesta">
                  <p>Tu decisión sobre la propuesta</p>
                  <div class="pro-acciones">
                    <button class="boton" data-responder="ACEPTAR" data-id="${v.id}">
                      ${Profesionales.icono('comprobar')}Aceptar
                    </button>
                    <button class="boton secundario" data-responder="RECHAZAR" data-id="${v.id}">
                      ${Profesionales.icono('cerrar')}Rechazar
                    </button>
                  </div>
                </div>`
                  : ''
              }
            </div>
            <p class="ayuda aviso-legal-familia">
              ${Profesionales.icono('informacion')}
              <span>La respuesta en este portal no sustituye las formalidades legales
                ni autoriza el acogimiento preadoptivo.</span>
            </p>
          </section>`,
          )
          .join('')}
      </div>`;
  },
  seleccionarVinculacion(lista) {
    const ordenadas = [...lista].sort((a, b) => Number(b.id) - Number(a.id));
    const vigente = ordenadas.find(
      (v) =>
        ['PENDIENTE_RESPUESTA', 'RESPUESTA_ACEPTADA'].includes(v.estado) &&
        !v.interrupcionRegistrada,
    );
    return vigente || ordenadas[0] || null;
  },
  obtenerProgreso(v) {
    if (!v) return null;
    const autorizacion = v.autorizacionPreadoptiva;
    const seguimiento = v.seguimientoFamiliar || {};
    const rechazada =
      ['RECHAZADA_POR_FAMILIA', 'RESPUESTA_RECHAZADA_PENDIENTE_CIERRE'].includes(v.estado) ||
      v.respuestaFamiliar === 'RECHAZADA';
    const aceptada =
      v.estado === 'RESPUESTA_ACEPTADA' ||
      v.respuestaFamiliar === 'ACEPTADA';
    const cerrada = v.estado === 'CERRADA';
    // Compatibilidad de presentación del único antecedente confirmado; no crea constancia documental.
    const autorizacionHistorica = Number(v.id) === 1 && v.codigo === 'VIN-1' && cerrada && v.interrupcionRegistrada && !autorizacion;
    const inicioPendiente = this.inicioPendiente(autorizacion?.fechaInicioConvivencia);
    const iniciada = Boolean(autorizacion?.fechaInicioConvivencia) && !inicioPendiente;
    const detenida = rechazada || cerrada || v.interrupcionRegistrada;
    let actual = aceptada ? 2 : 1;
    let estado = 'Tienes una propuesta pendiente de respuesta';
    let mensaje = 'La propuesta del Comité está registrada.';
    let accion = 'Consulta la información y decide si deseas aceptar o rechazar la propuesta';
    if (aceptada) {
      estado = 'Acompañamiento e interrelación';
      mensaje =
        'Aceptaste la propuesta. Ahora te encuentras en la etapa de acompañamiento e interrelación.';
      accion =
        'El equipo responsable coordinará el acompañamiento y te comunicará las actuaciones siguientes.';
    }
    if (autorizacion) {
      actual = 4;
      estado = 'Autorización preadoptiva registrada';
      mensaje = 'Se registró una autorización preadoptiva emitida por SENNIAF.';
      accion = 'El equipo responsable coordinará el inicio de convivencia.';
      if (inicioPendiente) mensaje = 'La convivencia está pendiente de comenzar el '
        + this.fechaCivil(autorizacion.fechaInicioConvivencia) + '.';
    }
    if (iniciada) {
      estado = 'Convivencia preadoptiva iniciada';
      mensaje = 'El inicio de convivencia está registrado.';
      accion = 'El equipo técnico dará seguimiento a la convivencia preadoptiva.';
    }
    if (rechazada) {
      estado = 'Propuesta rechazada';
      mensaje =
        'Tu rechazo quedó registrado. Esta respuesta no modifica automáticamente tu idoneidad.';
      accion =
        v.estado === 'RESPUESTA_RECHAZADA_PENDIENTE_CIERRE'
          ? 'No hay acciones pendientes de tu parte. El Comité debe registrar el cierre y revisar la disponibilidad.'
          : 'No hay acciones pendientes de tu parte para esta propuesta. La disponibilidad depende de los requisitos y revisiones institucionales.';
    }
    if (v.interrupcionRegistrada) {
      estado = v.interrupcionPendiente ? 'En revisión por el Comité' : 'Convivencia interrumpida';
      mensaje = v.interrupcionPendiente
        ? 'El Comité está revisando la interrupción de tu convivencia preadoptiva.'
        : 'El Comité registró la revisión de la interrupción de tu convivencia preadoptiva.';
      accion = v.interrupcionPendiente
        ? 'Esperar el resultado de la revisión institucional'
        : 'Consulta las actuaciones registradas. El equipo institucional te orientará sobre los siguientes pasos.';
    }
    if (cerrada) {
      mensaje = v.interrupcionRegistrada ? 'El cierre institucional quedó registrado. La interrupción se conserva en el historial.' : 'El cierre institucional quedó registrado.';
      estado = 'Vinculación cerrada';
      accion =
        'No hay acciones pendientes de tu parte en esta vinculación. La disponibilidad requiere revisión institucional independiente.';
    }
    const titulos = [
      'Propuesta del Comité',
      'Respuesta familiar',
      'Acompañamiento e interrelación',
      'Autorización preadoptiva',
      'Convivencia preadoptiva',
    ];
    const etapas = titulos.map((titulo, indice) => {
      let clase = 'pendiente';
      let texto = detenida ? 'No iniciada' : 'Pendiente';
      if (indice === 0) {
        clase = 'completado';
        texto = 'Propuesta registrada';
      } else if (indice === 1 && (aceptada || rechazada)) {
        clase = rechazada ? 'interrumpido' : 'completado';
        texto = rechazada ? 'Rechazada' : 'Aceptada';
      } else if (indice === 2 && aceptada) {
        clase = !detenida && actual === 2 ? 'actual' : 'registrada';
        texto = clase === 'actual' ? 'Etapa actual' : 'Iniciada';
      } else if (indice === 3 && autorizacion) {
        clase = 'completado';
        texto = 'Autorización registrada';
      } else if (indice === 4 && iniciada) {
        clase = v.interrupcionRegistrada ? 'interrumpido' : cerrada ? 'registrada' : 'actual';
        texto = v.interrupcionRegistrada ? 'Convivencia interrumpida' : 'Inicio registrado';
      } else if (indice === 4 && autorizacion && !iniciada) {
        texto = inicioPendiente && !detenida ? 'Pendiente de comenzar' : 'No iniciada';
      } else if (!detenida && indice === actual) {
        clase = 'actual';
        texto = 'Etapa actual';
      }
      if (v.interrupcionRegistrada) {
        if ((indice === 3 && !autorizacion) || (indice === 2 && !aceptada)) {
          clase = 'sin-constancia';
          texto = indice === 3 ? 'Autorización no documentada en este antecedente' : 'Sin actuación específica registrada';
        }
        if (indice === 4) {
          clase = 'interrumpido';
          texto = 'Interrumpida';
        }
      }
      if (indice === 3 && autorizacionHistorica) {
        clase = 'completado';
        texto = 'Autorizada';
      }
      if (indice === 1 && v.fechaRespuesta && !aceptada && !rechazada) {
        clase = 'registrada';
        texto = 'Respuesta registrada';
      }
      const rolInterrupcion = seguimiento.interrupcionRol || v.interrupcion?.rolProfesional;
      const detalle = indice === 4 && v.interrupcionRegistrada
        ? rolInterrupcion === 'FAMILIA' ? 'La familia registró la interrupción de la convivencia.'
          : rolInterrupcion === 'TRABAJADOR_SOCIAL' ? 'Trabajo Social registró la interrupción de la convivencia.'
          : rolInterrupcion === 'PSICOLOGO' ? 'Psicología registró la interrupción de la convivencia.'
          : 'La interrupción de la convivencia quedó registrada.' : '';
      const fechaHecho = indice === 4 && v.interrupcionRegistrada ? seguimiento.fechaInterrupcion || v.interrupcion?.fechaInterrupcion : null;
      return { titulo, clase, texto, detalle, fechaHecho };
    });
    const eventos = [
      { titulo: 'Propuesta registrada', fecha: v.fechaCreacion },
      {
        titulo: rechazada
          ? 'Respuesta rechazada'
          : aceptada
            ? 'Respuesta aceptada'
            : 'Respuesta registrada',
        fecha: v.fechaRespuesta,
      },
      {
        titulo: 'Acompañamiento e interrelación iniciados',
        fecha: aceptada ? v.fechaRespuesta : null,
      },
      {
        titulo: 'Autorización preadoptiva registrada',
        fecha: autorizacion?.registradaEl,
        detalle: autorizacion?.fechaEmision
          ? 'Fecha de emisión: ' + this.fechaCivil(autorizacion.fechaEmision) : '',
      },
      {
        titulo: 'Inicio de convivencia registrado',
        fecha: autorizacion?.inicioRegistradoEl,
        detalle: autorizacion?.fechaInicioConvivencia
          ? 'Inicio de convivencia: ' + this.fechaCivil(autorizacion.fechaInicioConvivencia) : '',
      },
      { titulo: 'Interrupción registrada', fecha: v.seguimientoFamiliar?.interrupcionRegistradaEl },
      {
        titulo: 'Revisión del Comité registrada',
        fecha: v.seguimientoFamiliar?.revisionRegistradaEl,
      },
      {
        titulo: 'Cierre institucional registrado',
        fecha: v.seguimientoFamiliar?.cierreRegistradoEl,
      },
    ]
      .filter((e) => e.fecha)
      .map((e) => ({ ...e, descripcion: [v.codigo, e.detalle].filter(Boolean).join(' · ') }));
    return { estado, mensaje, accion, etapas, eventos };
  },
  fechaHora(valor) {
    if (!valor) return 'Fecha no registrada';
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) return 'Fecha no disponible';
    const partes = new Intl.DateTimeFormat('es-PA', {day: '2-digit', month: '2-digit', year: 'numeric',
      timeZone: 'America/Panama'}).formatToParts(fecha);
    const parte = tipo => partes.find(item => item.type === tipo)?.value;
    return `${parte('day')}/${parte('month')}/${parte('year')} · ${Profesionales.momento(valor).hora}`;
  },
  fechaCivil(valor) {
    if (!valor) return 'Fecha no registrada';
    const [anio, mes, dia] = valor.split('-');
    return `${dia}/${mes}/${anio}`;
  },
  dias(cantidad) {
    return cantidad == null ? 'Duración no disponible' : cantidad + (cantidad === 1 ? ' día' : ' días');
  },
  inicioPendiente(fecha) {
    return Boolean(fecha) && fecha > new Intl.DateTimeFormat('sv-SE', {dateStyle: 'short', timeZone: 'America/Panama'}).format(new Date());
  },
  tiempoConvivencia(tiempos) {
    if (!tiempos?.convivenciaInterrumpida && !tiempos?.convivenciaFinalizada) {
      if (!tiempos?.fechaInicio) return 'Convivencia no iniciada';
      if (this.inicioPendiente(tiempos.fechaInicio)) return 'Pendiente de comenzar';
    }
    return this.dias(tiempos.convivencia);
  },
  tiemposEtapa(v, indice, familiar = false) {
    const t = v.tiempos;
    if (!t) return '';
    const a = v.autorizacionPreadoptiva;
    const texto = [];
    if (familiar && indice === 0 && v.fechaCreacion) texto.push(this.fechaHora(v.fechaCreacion));
    if (indice === 1 && (v.fechaRespuesta || v.estado === 'PENDIENTE_RESPUESTA')) {
      if (familiar && v.fechaRespuesta) texto.push(this.fechaHora(v.fechaRespuesta));
      texto.push(this.dias(t.respuesta) + (t.respuesta == null ? '' : ' desde la propuesta'));
    }
    if (indice === 3 && a?.fechaEmision) {
      texto.push('Fecha de emisión · ' + this.fechaCivil(a.fechaEmision));
      if (v.respuestaFamiliar === 'ACEPTADA') texto.push(this.dias(t.autorizacion) + (t.autorizacion == null ? '' : ' desde la aceptación familiar'));
    }
    if (indice === 4 && (t.fechaInicio || t.convivenciaInterrumpida)) {
      if (t.fechaInicio) {
        texto.push('Inicio de convivencia · ' + this.fechaCivil(t.fechaInicio));
        if (a?.fechaEmision && !this.inicioPendiente(t.fechaInicio)) texto.push(this.dias(t.inicio) + (t.inicio == null ? '' : ' desde la emisión de la autorización'));
      }
      const fin = t.convivenciaInterrumpida ? ' hasta la interrupción' : t.convivenciaFinalizada ? ' hasta el cierre' : ' transcurridos';
      const convivencia = t.convivencia == null ? this.tiempoConvivencia(t)
        : this.dias(t.convivencia) + ' de convivencia' + fin;
      texto.push(convivencia);
    }
    const etiqueta = familiar ? 'small' : 'p';
    return texto.map(linea => `<${etiqueta} class="vinculacion-tiempo">${limpiarHtml(linea)}</${etiqueta}>`).join('');
  },
  progresoFamiliar(v) {
    const progreso = this.obtenerProgreso(v);
    if (!progreso) return '';
    return `
      <section class="familia-seguimiento" aria-label="Progreso de ${limpiarHtml(v.codigo)}">
        <header>
          <span class="vinculacion-emblema">${Profesionales.icono('solicitudes')}</span>
          <div><h3>${limpiarHtml(progreso.estado)}</h3><p>${limpiarHtml(progreso.mensaje)}</p></div>
        </header>
        ${v.tiempos ? `<p class="vinculacion-tiempo">Tiempo de vinculación · ${this.dias(v.tiempos.vinculacion)}
          · ${this.tiempoConvivencia(v.tiempos)}
          ${v.tiempos.fechaInicio ? '<small>Referencia de convivencia · ' + v.tiempos.diasReferencia + ' días calendario</small>' : ''}
        </p>` : ''}
        <ol class="familia-etapas">
          ${progreso.etapas
            .map(
              (etapa, indice) => `
            <li class="${etapa.clase}" ${etapa.clase === 'actual' ? 'aria-current="step"' : ''}>
              <span class="familia-paso">${etapa.clase === 'interrumpido' ? Profesionales.icono('advertencia') : etapa.clase === 'completado' ? Profesionales.icono('comprobar') : indice + 1}</span>
              <strong>${etapa.titulo}</strong><small>${etapa.texto}</small>${etapa.detalle ? `<small class="familia-etapa-detalle">${limpiarHtml(etapa.detalle)}</small>` : ''}${etapa.fechaHecho ? `<small class="familia-etapa-detalle">Fecha de interrupción: ${limpiarHtml(this.fechaCivil(etapa.fechaHecho))}</small>` : ''}
              ${this.tiemposEtapa(v, indice, true)}
            </li>
          `,
            )
            .join('')}
        </ol>
        <button class="boton secundario chico" type="button" data-actualizar-familiar>Actualizar seguimiento</button>
        <p data-estado-seguimiento-familiar role="status" hidden></p>
        <p class="familia-siguiente">${limpiarHtml(progreso.accion)}</p>
      </section>`;
  },
  eventosFamiliares(v) {
    return this.lineaTiempo(this.obtenerProgreso(v).eventos);
  },
  async informacionFamiliar(id){
    try{
      const datos=await this.api(`/api/vinculaciones/${encodeURIComponent(id)}/informacion-asignada`);
      const d=document.createElement('dialog');d.className='pro-dialogo vinculacion-informacion-dialogo';d.dataset.asignacionInfo=String(id);d.setAttribute('aria-label','Información del niño asignado');
      const etiquetas={edad:'Edad',unidadEdad:'Unidad de edad',nivelEscolar:'Escolaridad',rendimientoAcademico:'Rendimiento académico',alergias:'Alergias registradas',intoleranciaLactosa:'Intolerancia a la lactosa',condicionesSalud:'Condiciones de salud registradas',habilidades:'Habilidades',gustos:'Intereses',personalidad:'Características personales registradas'};
      const valor=v=>v==null?'No disponible':Array.isArray(v)?v.length?v.map(x=>Profesionales.etiqueta(x)).join(', '):'Sin información registrada':Profesionales.etiqueta(v);
      d.innerHTML=`<h2 tabindex="-1">${Profesionales.icono('informe')}Información del niño asignado</h2><p>${limpiarHtml(datos.vinculacion)}</p><dl class="detalle-datos">${Object.entries(etiquetas).map(([k,t])=>`<div><dt>${t}</dt><dd>${limpiarHtml(k==='unidadEdad'&&datos[k]==='ANIOS'?'Años':valor(datos[k]))}</dd></div>`).join('')}</dl><p class="ayuda">${Profesionales.icono('informacion')}Esta consulta contiene los datos estructurados disponibles de la asignación. El equipo responsable te orientará sobre la información médica, psicológica y educativa que requiera explicación adicional.</p><button type="button" class="boton secundario" data-cerrar>Cerrar</button>`;
      d.onclose=()=>d.remove();d.querySelector('[data-cerrar]').onclick=()=>d.close();document.body.append(d);d.showModal();d.querySelector('h2').focus({preventScroll:true});d.scrollTop=0;
    }catch(e){const aviso=document.createElement('p');aviso.className='mensaje error';aviso.setAttribute('role','alert');aviso.textContent=e.message;document.querySelector('#contenido').prepend(aviso);}
  },
  descripcion(v){
    if(v.interrupcionPendiente)return 'Hay una interrupción registrada pendiente de revisión institucional.';
    return ({PENDIENTE_RESPUESTA:'La propuesta fue enviada a la familia y estamos a la espera de su respuesta.',RESPUESTA_ACEPTADA:'La familia aceptó la propuesta. Consulta el estado y las actuaciones registradas.',RECHAZADA_POR_FAMILIA:'La familia rechazó la propuesta. Su respuesta quedó registrada.',RESPUESTA_RECHAZADA_PENDIENTE_CIERRE:'La respuesta negativa quedó registrada. El cierre institucional sigue pendiente.',CERRADA:'El cierre quedó registrado. La disponibilidad de cada expediente se revisa por separado.'})[v.estado]||'Consulta las actuaciones registradas en el detalle.';
  },
  enlazarRespuestas(sesion) {
    document.querySelectorAll('[data-actualizar-familiar]').forEach(boton => boton.onclick = async () => {
      if (document.querySelector('dialog[open]') || Profesionales.formulariosPendientes().length) return;
      boton.disabled = true;
      try { await Familia.mostrar(sesion, {actualizarSeguimiento: true}); }
      catch (error) {
        const estado = boton.parentElement.querySelector('[data-estado-seguimiento-familiar]');
        estado.textContent = error.message;
        estado.hidden = false;
      } finally {
        boton.disabled = false;
        if (!document.querySelector('dialog[open]')) document.querySelector('[data-actualizar-familiar]')?.focus();
      }
    });
    document.querySelectorAll('[data-informacion-nino]').forEach(b=>b.onclick=()=>this.informacionFamiliar(b.dataset.informacionNino));
    document.querySelectorAll('[data-responder]').forEach(b => b.onclick = () => {
      if (document.querySelector('dialog[data-respuesta]')) return;
      const aceptar = b.dataset.responder === 'ACEPTAR', d = document.createElement('dialog');
      d.className = 'pro-dialogo'; d.dataset.respuesta = ''; d.setAttribute('aria-labelledby', 'tituloRespuesta');
      d.innerHTML = `<div class="pro-modal-cabecera"><span class="pro-simbolo">${Profesionales.icono(aceptar ? 'comprobar' : 'advertencia')}</span><h2 id="tituloRespuesta">${aceptar ? 'Aceptar' : 'Rechazar'} propuesta</h2></div>
        <p>${aceptar ? 'Se registrará tu aceptación y el equipo institucional te orientará sobre los siguientes pasos. Esto no formaliza una adopción.' : 'Se registrará tu rechazo y finalizará el acceso digital a la información del niño. Esta combinación no volverá a recomendarse automáticamente. No cambia tu idoneidad ni tu puntuación.'}</p>
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
          document.querySelectorAll('[data-asignacion-info]').forEach(info=>info.close());d.close(); Familia.version = null; await Familia.mostrar(sesion);
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
    document.body.classList.add('s8-acabado');
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
        </form></section><div id="listaVinculaciones" class="vinculaciones-lista" aria-live="polite"></div>`;
      const form = document.getElementById('filtroVinculaciones');
      let consulta = 0;
      const cargar = async () => {
        const actual = ++consulta, lista = document.getElementById('listaVinculaciones');
        try {
          const datos = await this.api('/api/vinculaciones?' + new URLSearchParams(new FormData(form)));
          if (actual !== consulta) return;
          lista.innerHTML = datos.length ? datos.map(v => `<article class="tarjeta vinculacion-tarjeta">
            <header class="vinculacion-cabecera"><h2><span class="vinculacion-emblema">${Profesionales.icono('heart-handshake')}</span>${limpiarHtml(v.codigo)}</h2>${this.estado(v)}</header>
            <dl class="vinculacion-resumen"><div><dt>${Profesionales.icono('carpeta')}Expediente infantil</dt><dd>${limpiarHtml(v.expedienteInfantil)}</dd></div><div><dt>${Profesionales.icono('personas')}Expediente familiar</dt><dd>${limpiarHtml(v.expedienteFamiliar)}</dd></div><div><dt>${Profesionales.icono('calendario')}Creación</dt><dd>${limpiarHtml(this.fechaHora(v.fechaCreacion))}</dd></div><div><dt>${Profesionales.icono('criterios')}Compatibilidad original</dt><dd><strong>${v.puntajeOriginal==null?'No disponible':Sesion.puntajeHistorico(v.puntajeOriginal,v.maximoAplicableOriginal)}</strong><small>Versión de criterios: ${limpiarHtml(String(v.versionOriginal??'no disponibles'))}</small></dd></div></dl>
            <p class="vinculacion-siguiente">${limpiarHtml(this.descripcion(v))}</p>
            <footer class="vinculacion-acciones"><a class="boton secundario" href="vinculaciones.html?id=${v.id}">${Profesionales.icono('consulta')}Ver seguimiento</a></footer>
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
  async detalle(c, id, sesion, actualizado = null) {
    const v = actualizado || await this.api('/api/vinculaciones/' + encodeURIComponent(id)), d = v.decisionComite;
    const contexto = new URLSearchParams({origen: 'vinculacion', vinculacionOrigen: String(v.id)});
    const enlaceNino = `perfiles.html?caso=${encodeURIComponent(v.expedienteInfantil)}&${contexto}`;
    const enlaceFamilia = `perfiles.html?familia=${encodeURIComponent(v.expedienteFamiliar)}&${contexto}`;
    const mostrarIncidencias = sesion.rol !== 'PSICOLOGO';
    const pestañas = [['proceso','solicitudes','Proceso'], ['reportes','informe','Reportes profesionales'],
      ...(mostrarIncidencias ? [['incidencias','advertencia','Incidencias']] : []), ['historial','reloj','Historial de cambios']];
    const panelActivo=c.querySelector('[data-seguimiento][aria-selected="true"]')?.dataset.seguimiento||'proceso';
    c.innerHTML = `<div class="vinculacion-detalle"><p class="pro-breadcrumb"><a href="vinculaciones.html">${Profesionales.icono('flecha')}Volver a Vinculaciones</a></p>`+
      Profesionales.encabezado(v.codigo+' · Detalle de vinculación','Seguimiento de la asignación familiar.','heart-handshake')+`
      <section class="tarjeta vinculacion-general">
        <div class="pro-fila-titulo"><h2>${Profesionales.icono('carpeta')}Información general</h2>
          <button class="boton secundario chico" type="button" data-actualizar-vinculacion>Actualizar</button></div>
        <p data-estado-actualizacion role="status" hidden></p>
        <dl class="detalle-datos vinculacion-tiempos">
          <div><dt>Fecha de creación</dt><dd>${limpiarHtml(this.fechaHora(v.fechaCreacion))}</dd></div>
          <div><dt>Tiempo transcurrido de vinculación</dt><dd class="vinculacion-tiempo-destacado">${this.dias(v.tiempos?.vinculacion)}</dd></div>
          <div><dt>Inicio de convivencia</dt><dd>${v.tiempos?.fechaInicio ? this.fechaCivil(v.tiempos.fechaInicio) : 'Sin inicio registrado'}</dd></div>
          <div><dt>Tiempo de convivencia</dt><dd class="vinculacion-tiempo-destacado convivencia">${this.tiempoConvivencia(v.tiempos)}</dd>
          ${v.tiempos ? `<small>Periodo de referencia · ${v.tiempos.diasReferencia} días calendario</small>` : ''}</div>
        </dl>
      </section>
      <div class="vinculacion-accesos" aria-label="Accesos de la vinculación">
        ${this.acceso('carpeta','Expediente infantil',v.expedienteInfantil,enlaceNino,'Ver expediente','azul')}
        ${this.acceso('personas','Expediente familiar',v.expedienteFamiliar,sesion.rol==='COMITE'?enlaceFamilia:null,'Ver expediente','menta')}
      </div>
      ${this.compatibilidadOriginal(v.compatibilidadOriginal,d.fecha)}
      <nav class="pro-pestanas vinculacion-pestanas" role="tablist" aria-label="Apartados de seguimiento">${pestañas.map(([k,i,n])=>`<button type="button" role="tab" id="tab-seguimiento-${k}" aria-controls="seguimiento-${k}" aria-selected="false" data-seguimiento="${k}">${Profesionales.icono(i)}${n}</button>`).join('')}</nav>
      <div class="vinculacion-panel" role="tabpanel" id="seguimiento-proceso" aria-labelledby="tab-seguimiento-proceso" hidden>
        ${this.proceso(v)}${this.panelAutorizacion(v)}${this.panelCierre(v)}
      </div>
      <div class="vinculacion-panel" role="tabpanel" id="seguimiento-reportes" aria-labelledby="tab-seguimiento-reportes" hidden><p role="status">Cargando seguimientos profesionales…</p></div>
      ${mostrarIncidencias ? `<div class="vinculacion-panel" role="tabpanel" id="seguimiento-incidencias" aria-labelledby="tab-seguimiento-incidencias" hidden>
        ${this.panelInterrupcion(v)||`<section class="tarjeta vinculacion-vacio"><span class="vinculacion-emblema durazno">${Profesionales.icono('advertencia')}</span><h2>Sin incidencias registradas</h2><p>No hay una interrupción de convivencia registrada en esta vinculación.</p></section>`}
      </div>` : ''}
      <div class="vinculacion-panel" role="tabpanel" id="seguimiento-historial" aria-labelledby="tab-seguimiento-historial" hidden>${this.historialConDecision(v)}</div></div>`;
    c.querySelector('.hero').insertAdjacentHTML('beforeend',`<div class="vinculacion-estado-hero">${this.estado(v)}</div>`);
    this.enlazarPestanas(c, panelActivo, this.enlazarSeguimientos(c, v));
    const actualizar = c.querySelector('[data-actualizar-vinculacion]');
    actualizar.onclick = async () => {
      if (document.querySelector('dialog[open]') || Profesionales.formulariosPendientes().length) return;
      actualizar.disabled = true;
      try {
        const datos = await this.api(`/api/vinculaciones/${v.id}`);
        if (!actualizar.isConnected || document.querySelector('dialog[open]') || Profesionales.formulariosPendientes().length) return;
        await this.detalle(c, v.id, sesion, datos);
        c.querySelector('[data-actualizar-vinculacion]')?.focus();
      } catch (error) {
        const estado = c.querySelector('[data-estado-actualizacion]');
        if (estado) { estado.textContent = error.message; estado.hidden = false; }
      } finally { actualizar.disabled = false; }
    };
    c.querySelector('[data-autorizacion]')?.addEventListener('click',()=>this.registrarAutorizacion(c,v,sesion));
    c.querySelector('[data-cierre]')?.addEventListener('click',()=>this.registrarCierre(c,v,sesion));
    c.querySelectorAll('[data-disponibilidad]').forEach(b=>b.onclick=()=>Profesionales.justificar({
      titulo:'Revisar disponibilidad del expediente',maximo:500,contexto:b.dataset.disponibilidad==='NINO'?'Expediente infantil':'Expediente familiar',
      opciones:this.motivosDisponibilidad[b.dataset.disponibilidad],
      confirmacion:'Confirma que revisaste por separado la disponibilidad y elegibilidad de este expediente. Esta actuación no modifica la idoneidad ni los requisitos vigentes.',
      alConfirmar:justificacion=>this.api(`/api/vinculaciones/${v.id}/disponibilidad`,{method:'POST',body:JSON.stringify({expediente:b.dataset.disponibilidad,justificacion})}),
      alGuardar:async datos=>{await this.detalle(c,v.id,sesion,datos);c.insertAdjacentHTML('afterbegin','<p class="mensaje exito" role="status">La revisión de este expediente quedó registrada.</p>');}
    }));
    c.querySelector('[data-interrupcion]')?.addEventListener('click',()=>this.registrarInterrupcion(c,v,sesion));
    c.querySelector('[data-revisar-interrupcion]')?.addEventListener('click',()=>Profesionales.justificar({
      titulo:'Revisar interrupción de convivencia',contexto:v.codigo,
      opciones:[['REVISION','Revisión institucional','Se revisaron los antecedentes de la interrupción y se documentan las actuaciones de seguimiento correspondientes.'],['OTRO','Otro','']],
      confirmacion:'La revisión quedará registrada. La reserva de los expedientes se mantiene hasta la actuación institucional correspondiente.',
      alConfirmar:justificacion=>this.api(`/api/vinculaciones/${v.id}/interrupcion/revision`,{method:'POST',body:JSON.stringify({justificacion})}),
      alGuardar:async datos=>{await this.detalle(c,v.id,sesion,datos);c.insertAdjacentHTML('afterbegin','<p class="mensaje exito" role="status">La revisión quedó registrada.</p>');}
    }));
  },
  acceso(icono,titulo,valor,enlace,accion,color){
    return `<section class="tarjeta vinculacion-acceso ${color}"><span class="vinculacion-emblema">${Profesionales.icono(icono)}</span><div><h2>${titulo}</h2><p>${limpiarHtml(String(valor??'No disponible'))}</p>${enlace?`<a class="boton secundario chico" href="${enlace}">${accion}${Profesionales.icono('flecha')}</a>`:'<p class="ayuda">Consulta del expediente reservada al Comité.</p>'}</div></section>`;
  },
  enlazarPestanas(c,activo,alMostrar){
    const botones=[...c.querySelectorAll('[data-seguimiento]')];
    const mostrar = apartado => {
      botones.forEach(boton => {
        const activo = boton.dataset.seguimiento === apartado;
        boton.setAttribute('aria-selected', String(activo));
        boton.tabIndex = activo ? 0 : -1;
        c.querySelector('#seguimiento-' + boton.dataset.seguimiento).hidden = !activo;
      });
      alMostrar(apartado);
    };
    botones.forEach((b,i)=>{b.onclick=()=>mostrar(b.dataset.seguimiento);b.onkeydown=e=>{if(!['ArrowLeft','ArrowRight','Home','End'].includes(e.key))return;e.preventDefault();const j=e.key==='Home'?0:e.key==='End'?botones.length-1:(i+(e.key==='ArrowRight'?1:-1)+botones.length)%botones.length;botones[j].focus();mostrar(botones[j].dataset.seguimiento);};});
    mostrar(botones.some(b=>b.dataset.seguimiento===activo)?activo:'proceso');
  },
  proceso(v) {
    const progreso = this.obtenerProgreso(v);
    const autorizacion = v.autorizacionPreadoptiva;
    const fechas = [
      v.fechaCreacion, v.fechaRespuesta,
      progreso.etapas[2].texto === 'Iniciada' || progreso.etapas[2].clase === 'actual'
        ? v.fechaRespuesta : null,
      autorizacion?.registradaEl, v.interrupcionRegistrada ? v.seguimientoFamiliar?.interrupcionRegistradaEl || v.interrupcion?.fechaRegistro : autorizacion?.inicioRegistradoEl
    ];
    return `
      <section class="tarjeta">
        <h2>${Profesionales.icono('solicitudes')}Etapas del proceso</h2>
        <ol class="vinculacion-etapas">
          ${progreso.etapas.map((etapa, indice) => `
            <li class="${etapa.clase}">
              <span class="vinculacion-hito">
                ${Profesionales.icono(etapa.clase === 'interrumpido' ? 'advertencia' : etapa.clase === 'completado' ? 'comprobar' : 'informacion')}
              </span>
              <div><h3>${etapa.titulo}</h3>${etapa.detalle ? `<p>${limpiarHtml(etapa.detalle)}</p>` : ''}${etapa.fechaHecho ? `<p>Fecha de interrupción: ${limpiarHtml(this.fechaCivil(etapa.fechaHecho))}</p>` : ''}${this.tiemposEtapa(v, indice)}</div>
              <div class="vinculacion-etapa-meta">
                <span class="chip ${etapa.clase === 'interrumpido' ? 'vinculacion-negativa' : etapa.clase === 'completado' ? 'vinculacion-positiva' : 'retirado'}">${etapa.texto}</span>
                ${fechas[indice] ? `<time>${indice === 3 || indice === 4 ? 'Registro · ' : ''}${limpiarHtml(this.fechaHora(fechas[indice]))}</time>` : ''}
              </div>
            </li>`).join('')}
        </ol>
      </section>`;
  },
  historialConDecision(v, seguimientos = [], visitas = []) {
    const decision = v.decisionComite;
    const eventos = [...(v.historial || [])];
    if (!eventos.some(evento => evento.accion === 'DECISION_' + decision.decision)) {
      eventos.unshift({
        accion: 'DECISION_' + decision.decision,
        titulo: 'Decisión inicial del Comité · ' + Profesionales.etiqueta(decision.decision),
        responsable: decision.responsable,
        fecha: decision.fecha,
        resumen: decision.motivo
      });
    }
    if (seguimientos.length || visitas.length) {
      eventos.push(...seguimientos.map(movimiento => SeguimientosVinculacion.evento(movimiento)),
        ...visitas.map(movimiento => VisitasDomiciliarias.evento(movimiento)));
      eventos.sort((a, b) => new Date(b.fecha) - new Date(a.fecha));
    } else eventos.sort((a, b) => new Date(a.fecha) - new Date(b.fecha));
    return this.historial(eventos);
  },
  enlazarSeguimientos(contenedor, vinculacion) {
    const reportes = contenedor.querySelector('#seguimiento-reportes');
    const historial = contenedor.querySelector('#seguimiento-historial');
    let datos, visitas, cargando;
    const cargar = (mensaje = '', rol, id) => {
      if (cargando) return cargando;
      const abiertos = new Set([...reportes.querySelectorAll('details[open][data-especialidad]')]
        .map(detalle => detalle.dataset.especialidad));
      if (rol) abiertos.add(rol);
      const visitasAbiertas = reportes.querySelector('[data-panel-visitas]')?.open ?? true;
      reportes.setAttribute('aria-busy', 'true');
      historial.querySelector('[data-estado-reportes]')?.remove();
      const ruta = `/api/vinculaciones/${vinculacion.id}`;
      cargando = Promise.allSettled([this.api(ruta + '/seguimientos'), this.api(ruta + '/visitas-domiciliarias')])
        .then(([resultado, resultadoVisitas]) => {
          if (!reportes.isConnected) return;
          datos = resultado.status === 'fulfilled' ? resultado.value : null;
          visitas = resultadoVisitas.status === 'fulfilled' ? resultadoVisitas.value : null;
          const errores = [resultado, resultadoVisitas].filter(item => item.status === 'rejected');
          const aviso = errores.length ? `<div data-estado-reportes>
            <p class="mensaje error" role="alert">No se pudo cargar toda la información profesional. ${limpiarHtml(errores[0].reason.message)}</p>
            <button class="boton secundario chico" type="button" data-reintentar-reportes>Reintentar consulta</button></div>` : '';
          reportes.innerHTML = (mensaje ? `<p class="mensaje exito" role="status">${limpiarHtml(mensaje)}</p>` : '')
            + (visitas ? VisitasDomiciliarias.panel(visitas, visitasAbiertas) : '')
            + (datos ? SeguimientosVinculacion.panel(datos, abiertos) : '') + aviso;
          historial.innerHTML = aviso + this.historialConDecision(vinculacion, datos?.historial, visitas?.historial);
          if (id && !reportes.hidden) reportes.querySelector(`[data-ver-seguimiento="${id}"]`)?.focus();
          else if (mensaje && !reportes.hidden) reportes.querySelector('[data-panel-visitas] summary')?.focus();
        }).catch(error => {
          if (reportes.isConnected) {
            reportes.innerHTML = `<p class="mensaje error" role="alert">${limpiarHtml(error.message)}</p>
              <button class="boton secundario chico" type="button" data-reintentar-reportes>Reintentar consulta</button>`;
          }
        }).finally(() => {
          reportes.removeAttribute('aria-busy');
          cargando = null;
        });
      return cargando;
    };
    reportes.onclick = evento => {
      const boton = evento.target.closest('button');
      if (!boton) return;
      if (boton.hasAttribute('data-reintentar-reportes')) { cargar(); return; }
      if (cargando) return;
      const visita = visitas?.registros.find(item => String(item.id) === boton.dataset.editarVisita);
      if (visitas?.trabajoSocial && (visita?.puedeEditar || (visitas.puedeRegistrar && boton.hasAttribute('data-registrar-visita')))) {
        VisitasDomiciliarias.formulario(vinculacion, visitas, visita, boton, mensaje => cargar(mensaje));
        return;
      }
      if (!datos) return;
      const {verSeguimiento, editarSeguimiento} = boton.dataset;
      const registro = datos.registros.find(item => String(item.id) === (verSeguimiento || editarSeguimiento));
      if (verSeguimiento && registro) {
        SeguimientosVinculacion.ver(vinculacion, registro, boton);
      } else if ((editarSeguimiento && registro?.puedeEditar)
          || (boton.hasAttribute('data-registrar-seguimiento') && datos.rolRegistro)) {
        SeguimientosVinculacion.formulario(vinculacion, datos, registro, boton,
          (rol, id, mensaje) => cargar(mensaje, rol, id));
      }
    };
    historial.onclick = evento => {
      if (evento.target.closest('[data-reintentar-reportes]')) cargar();
    };
    return apartado => {
      if (!datos && !visitas && (apartado === 'reportes' || apartado === 'historial')) cargar();
    };
  },
  lineaTiempo(eventos) {
    if (!eventos.length) return '<p>No hay movimientos adicionales registrados.</p>';
    return `<ol class="vinculacion-linea">${eventos.map(evento => `<li>
      <span class="vinculacion-punto" aria-hidden="true"><i></i></span>
      <h3>${limpiarHtml(evento.titulo || Profesionales.etiqueta(evento.accion))}</h3>
      <span class="vinculacion-conector" aria-hidden="true"></span>
      <div class="vinculacion-evento">${evento.seguimientoId || evento.visitaId
        ? `<p class="vinculacion-evento-meta"><span><time>${limpiarHtml(this.fechaHora(evento.fecha))}</time> · ${limpiarHtml(evento.responsable)}</span></p>
          ${evento.visitaId ? VisitasDomiciliarias.cambios(evento) : SeguimientosVinculacion.cambios(evento)}`
        : `<p class="vinculacion-evento-meta">${evento.responsable ? `${Profesionales.icono('persona')}${limpiarHtml(evento.responsable)} · ` : ''}<time>${limpiarHtml(this.fechaHora(evento.fecha))}${evento.zona ? ' · ' + limpiarHtml(evento.zona) : ''}</time></p>
          ${evento.resumen || evento.detalle ? `<p>${limpiarHtml(evento.accion === 'CONVIVENCIA_INICIADA' ? 'Se registró la fecha de inicio de la convivencia preadoptiva.' : evento.resumen || evento.detalle)}</p>` : ''}`}</div>
    </li>`).join('')}</ol>`;
  },
  historial(eventos) {
    return `<section class="tarjeta"><h2>${Profesionales.icono('reloj')}Historial de cambios</h2>${this.lineaTiempo(eventos)}</section>`;
  },
  compatibilidadOriginal(h,fecha){
    if(!h)return '<section id="compatibilidadRegistrada" class="tarjeta"><h2>Compatibilidad original</h2><p>No hay análisis histórico disponible.</p></section>';
    const c=h.contexto||{},factores={edad:'Rango de edad',hermanos:'Vínculos entre hermanos',salud:'Salud y desarrollo',apoyo:'Red de apoyo',logistica:'Logística'},dato=v=>v==null?'No disponible':limpiarHtml(String(v));
    const notaSalud = c.reglas === 'EVIDENCIA_2' && c.pesos?.salud > 0
      && c.factores?.salud?.[0] === c.pesos.salud
      && c.factores.salud[1]?.includes('con apoyo adicional se acredita la mitad')
      ? '<p class="ayuda">El fundamento original menciona la regla general de cobertura con apoyo adicional. El cálculo registrado acreditó todos los puntos de Salud y desarrollo.</p>' : '';
    return `<section id="compatibilidadRegistrada" class="tarjeta vinculacion-historica"><div class="pro-fila-titulo"><div><h2>${Profesionales.icono('criterios')}Compatibilidad original</h2><p>Se conserva el análisis registrado al aceptar esta propuesta.</p><span class="chip">Versión de criterios: ${dato(h.version)}</span> <span class="chip retirado">Resultado histórico</span></div><div class="pro-puntaje">${Sesion.puntajeHistorico(h.puntaje,h.maximoAplicable ?? c.maximoAplicable)}</div></div>
      ${fecha?`<p class="ayuda">${Profesionales.icono('calendario')}${limpiarHtml(this.fechaHora(fecha))}</p>`:''}<details class="pro-seccion"><summary>${Profesionales.icono('consulta')}Ver desglose de compatibilidad</summary><div class="tabla"><table><thead><tr><th>Factor</th><th>Peso original</th><th>Puntos</th><th>Fundamento registrado</th></tr></thead><tbody>${Object.entries(factores).map(([k,n])=>`<tr><td>${n}</td><td>${dato(c.pesos?.[k])}</td><td>${dato(c.factores?.[k]?.[0])}</td><td>${dato(c.factores?.[k]?.[1])}</td></tr>`).join('')}</tbody></table></div>${notaSalud}${!h.contexto?'<p class="ayuda">Este antecedente no conserva el desglose. No se sustituye por un cálculo nuevo.</p>':''}<p class="ayuda">Máximo aplicable registrado: ${dato(c.maximoAplicable)} · Peso evaluado: ${dato(c.pesoEvaluado)}</p></details></section>`;
  },
  panelCierre(v) {
    if (!v.cierre && !v.puedeCerrar) return '';
    const x = v.cierre;
    const estados = {
      PENDIENTE_REVISION: ['Pendiente de revisión', 'El Comité debe revisar su disponibilidad antes de volver al Match.', 'reloj'],
      DISPONIBLE: ['Disponibilidad autorizada', 'Puede participar en nuevas propuestas si cumple las condiciones vigentes.', 'comprobar'],
      NO_ELEGIBLE: ['Requiere completar condiciones', 'La revisión está registrada; el expediente todavía no cumple las condiciones vigentes.', 'informacion'],
      ASIGNADO: ['Con otra asignación vigente', 'No participa en nuevas propuestas mientras conserve esa asignación.', 'vinculacion']
    };
    return `<section class="tarjeta vinculacion-cierre"><h2>${Profesionales.icono('informe')}Cierre institucional</h2>
      <div class="vinculacion-cierre-resumen"><span class="chip retirado">${Profesionales.icono(x ? 'informacion' : 'reloj')}${x ? 'Cierre registrado' : 'Pendiente de cierre'}</span>
      ${x ? `<dl class="detalle-datos"><div><dt>Fecha del cierre</dt><dd>${limpiarHtml(this.fechaCivil(x.fecha))}</dd></div><div><dt>Responsable del registro</dt><dd>${limpiarHtml(x.registradoPor)}</dd></div><div><dt>Registro</dt><dd>${limpiarHtml(this.fechaHora(x.registradoEl))}</dd></div>${x.referencia ? `<div><dt>Referencia institucional</dt><dd>${limpiarHtml(x.referencia)}</dd></div>` : ''}${x.autoridad ? `<div><dt>Autoridad registrada</dt><dd>${limpiarHtml(x.autoridad)}</dd></div>` : ''}</dl><p class="pro-texto-registrado">${limpiarHtml(x.motivo)}</p>` : '<p>El Comité registra un cierre respaldado por la actuación de la autoridad competente.</p>'}</div>
      ${x ? `<section class="vinculacion-disponibilidad"><h3>Estado de los expedientes</h3><div class="vinculacion-expedientes">${[['NINO','Expediente infantil',x.ninoLiberado,x.estadoInfantil,v.expedienteInfantil],['FAMILIA','Expediente familiar',x.familiaLiberada,x.estadoFamiliar,v.expedienteFamiliar]].map(([k,n,revisado,estado,codigo]) => {
        const info = estados[estado] || estados[revisado ? 'DISPONIBLE' : 'PENDIENTE_REVISION'];
        return `<article><h4>${Profesionales.icono(k === 'NINO' ? 'carpeta' : 'personas')}${n}</h4><strong>${info[0]}</strong><p>${info[1]}</p><div class="pro-acciones">${v.puedeRevisarDisponibilidad && !revisado ? `<button class="boton secundario" data-disponibilidad="${k}">${Profesionales.icono('consulta')}Revisar disponibilidad</button>` : ''}<a class="boton secundario" href="perfiles.html?${k === 'NINO' ? 'caso' : 'familia'}=${encodeURIComponent(codigo)}&origen=vinculacion&vinculacionOrigen=${Number(v.id)}">Consultar expediente</a></div></article>`;
      }).join('')}</div></section><aside class="vinculacion-proximo"><h3>${Profesionales.icono('flecha')}Próximo paso</h3><p>Después del cierre, los expedientes del niño y de la familia se revisan por separado. Una vez autorizados, podrán volver a participar en nuevas propuestas de compatibilidad si cumplen las condiciones vigentes.</p></aside>` : ''}
      ${v.puedeCerrar ? `<button class="boton secundario" data-cierre>${Profesionales.icono('informe')}Registrar cierre institucional</button>` : ''}
      <p class="ayuda">Este registro no sustituye la actuación de la autoridad competente ni formaliza una adopción.</p></section>`;
  },
  registrarCierre(c,v,sesion){
    if(document.querySelector('[data-dialogo-cierre]'))return;
    const d=document.createElement('dialog');d.className='pro-dialogo';d.dataset.dialogoCierre='';d.setAttribute('aria-labelledby','tituloCierre');
    d.innerHTML=`<h2 id="tituloCierre">${Profesionales.icono('informe')}Registrar cierre institucional</h2><p>Registra únicamente un cierre confirmado por la autoridad competente. Ambos expedientes conservarán su reserva hasta ser revisados por separado.</p>
    <form class="pro-interrupcion-form"><p class="ayuda">Tu identidad y la fecha y hora del registro se guardarán automáticamente.</p><label>Motivo o resultado<textarea name="motivo" rows="4" minlength="10" maxlength="2000" required></textarea></label><label class="pro-confirmacion-check"><input type="checkbox" name="confirmacionAutoridad" required><span>Confirmo que este cierre cuenta con el respaldo institucional correspondiente.</span></label><p data-error role="alert"></p><div class="pro-acciones"><button type="button" class="boton secundario" data-cancelar>Cancelar</button><button type="submit" class="boton">Registrar cierre</button></div></form>`;
    document.body.append(d);Profesionales.sugerencias(d.querySelector('[name=motivo]'),[
      ['Finalización confirmada','Se registró la finalización del proceso después de comprobar la actuación institucional correspondiente.'],['Revisión institucional','Se completó la revisión del caso y se documentó el resultado de la actuación institucional.'],['Cierre del seguimiento','Se registró el cierre del seguimiento conforme a la información revisada por el equipo responsable.'],['Otro motivo','']]);let guardando=false;const form=d.querySelector('form');CamposFormulario.iniciar(form);d.onclose=()=>d.remove();d.addEventListener('cancel',e=>{if(guardando)e.preventDefault();});d.querySelector('[data-cancelar]').onclick=()=>{if(!guardando)d.close();};
    form.onsubmit=async e=>{e.preventDefault();if(guardando||!Profesionales.validarFormulario(form))return;guardando=true;form.querySelectorAll('button').forEach(b=>b.disabled=true);
      try{const datos=Object.fromEntries(new FormData(form));datos.confirmacionAutoridad=form.elements.confirmacionAutoridad.checked;const actualizado=await this.api(`/api/vinculaciones/${v.id}/cierre`,{method:'POST',body:JSON.stringify(datos)});Profesionales.marcarGuardado(form);d.close();await this.detalle(c,v.id,sesion,actualizado);c.insertAdjacentHTML('afterbegin','<p class="mensaje exito" role="status">El cierre institucional quedó registrado. Cada expediente conserva su reserva hasta revisión.</p>');}
      catch(error){d.querySelector('[data-error]').textContent=error.message;guardando=false;form.querySelectorAll('button').forEach(b=>b.disabled=false);}
    };d.showModal();
  },
  motivosDisponibilidad: {
    NINO: [
      ['DISPONIBILIDAD','Disponibilidad para una nueva propuesta','La revisión se centra en la disponibilidad actual del niño para considerar una nueva propuesta. Detalla las actuaciones institucionales que sustentan esta confirmación.'],
      ['APOYOS','Necesidades y apoyos para la continuidad del proceso','La revisión considera las necesidades actuales del niño y los apoyos que deben mantenerse. Describe los antecedentes documentados que permiten continuar el proceso.'],
      ['OPINION','Opinión del niño y condiciones de acompañamiento','La revisión considera la opinión del niño según su edad y madurez y las condiciones de acompañamiento. Indica cómo se recabó o, si no fue posible, deja constancia de ello y del respaldo institucional disponible.'],
      ['OTRO','Otro','']
    ],
    FAMILIA: [
      ['DISPONIBILIDAD','Disponibilidad para continuar con nuevas propuestas','La revisión se centra en la disponibilidad actual de la familia para considerar nuevas propuestas. Detalla la información confirmada y las actuaciones que sustentan esta revisión.'],
      ['CUIDADOS','Condiciones de cuidado y red de apoyo','La revisión considera las condiciones actuales de cuidado y la red de apoyo familiar. Describe los antecedentes documentados relevantes para confirmar su disponibilidad.'],
      ['ACTUALIZACION','Actualización de la entrevista y antecedentes','La revisión considera la información vigente de la entrevista y los antecedentes de la familia. Detalla los elementos verificados que sustentan su disponibilidad, sin sustituir los requisitos institucionales.'],
      ['OTRO','Otro','']
    ]
  },
  motivosInterrupcion: [
    ['DIFICULTADES_CONTINUAR','La familia manifestó dificultades para continuar.','Se registra la comunicación de dificultades de la familia para continuar con la convivencia. Se solicita revisar las circunstancias y el acompañamiento necesario.'],
    ['ADAPTACION','Se presentaron dificultades importantes de adaptación.','Se comunican dificultades de adaptación durante la convivencia que requieren revisión y acompañamiento del equipo responsable.'],
    ['BIENESTAR_NINO','Se identificó una situación que requiere proteger el bienestar del niño.','Se comunica una situación que requiere valorar la protección del bienestar del niño. Se deja constancia para la intervención y revisión del equipo responsable, sin anticipar conclusiones.'],
    ['CONDICIONES_CUIDADO','Cambiaron las condiciones de cuidado de la familia.','Se comunica un cambio en las condiciones de cuidado de la familia que requiere revisar la continuidad de la convivencia y los apoyos necesarios.'],
    ['OTRO','Otro motivo.','']
  ],
  panelAutorizacion(v) {
    const autorizacion = v.autorizacionPreadoptiva;
    if (!autorizacion && !v.puedeRegistrarAutorizacion) return '';
    return `
      <section class="tarjeta vinculacion-autorizacion">
        <h2>${Profesionales.icono('informe')}Registro de autorización preadoptiva</h2>
        <p>
          Registra una autorización previamente emitida por la autoridad competente
          para continuar con el proceso de vinculación
        </p>
        ${autorizacion ? `
          <dl class="detalle-datos">
            <div><dt>Resolución o autorización institucional</dt>
              <dd>${limpiarHtml(autorizacion.referencia)}</dd></div>
            <div><dt>Fecha de emisión</dt>
              <dd>${limpiarHtml(this.fechaCivil(autorizacion.fechaEmision))}</dd></div>
            <div><dt>Inicio de convivencia</dt>
              <dd>${autorizacion.fechaInicioConvivencia
                ? limpiarHtml(this.fechaCivil(autorizacion.fechaInicioConvivencia))
                : 'Sin inicio registrado'}</dd></div>
            <div><dt>Responsable del registro</dt>
              <dd>${limpiarHtml(autorizacion.responsable)}
                · ${limpiarHtml(Profesionales.etiqueta(autorizacion.rol))}
                · ${limpiarHtml(this.fechaHora(autorizacion.registradaEl))} · Panamá</dd></div>
          </dl>` : ''}
        ${v.puedeRegistrarAutorizacion ? `
          <button type="button" class="boton" data-autorizacion>
            ${Profesionales.icono('informe')}${autorizacion ? 'Registrar inicio de convivencia' : 'Registrar autorización preadoptiva'}
          </button>` : ''}
      </section>`;
  },
  registrarAutorizacion(c,v,sesion){
    const a=v.autorizacionPreadoptiva,d=document.createElement('dialog');d.className='pro-dialogo s8-autorizacion-dialogo';d.setAttribute('aria-labelledby','tituloAutorizacion');
    const hoy=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Panama',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
    d.innerHTML=`<div class="pro-modal-cabecera"><span class="pro-simbolo">${Profesionales.icono('informe')}</span><h2 id="tituloAutorizacion">${a ? 'Registrar inicio de convivencia' : 'Registrar autorización de convivencia preadoptiva'}</h2></div><p>Confirma que SENNIAF ya emitió la autorización institucional correspondiente. Esta acción registrará su existencia en SENNIAF Match y actualizará la etapa del proceso.</p><form><label>Resolución o referencia institucional existente<input name="referencia" required minlength="5" maxlength="200" value="${limpiarHtml(a?.referencia||'')}" ${a?'readonly':''}></label><label>Fecha de emisión<input name="fechaEmision" type="date" required max="${hoy}" value="${limpiarHtml(a?.fechaEmision||'')}" ${a?'readonly':''}></label><label>Fecha de inicio de convivencia (opcional)<input name="fechaInicioConvivencia" type="date" max="${hoy}"></label><p class="ayuda">${a?'La autorización registrada se conserva. Registra la fecha de inicio de convivencia únicamente si ya ocurrió.':'Deja el inicio vacío si la convivencia todavía no ha comenzado. No registres fechas previstas como hechos ocurridos.'}</p><label class="pro-confirmacion-check"><input name="confirmacion" type="checkbox" required><span>Confirmo que SENNIAF emitió la autorización y que los datos corresponden a la actuación institucional existente.</span></label><p data-error role="alert"></p><div class="pro-acciones"><button type="button" class="boton secundario" data-cancelar>Cancelar</button><button type="submit" class="boton">Revisar y confirmar</button></div></form>`;
    document.body.append(d);const f=d.querySelector('form');let guardando=false;CamposFormulario.iniciar(f);d.onclose=()=>d.remove();d.oncancel=e=>{if(guardando)e.preventDefault();};d.querySelector('[data-cancelar]').onclick=()=>d.close();
    f.onsubmit=e=>{e.preventDefault();if(guardando||!Profesionales.validarFormulario(f))return;const datos={referencia:f.elements.referencia.value,fechaEmision:f.elements.fechaEmision.value,fechaInicioConvivencia:f.elements.fechaInicioConvivencia.value||null,confirmacion:f.elements.confirmacion.checked};
      Profesionales.confirmar('Confirmar constancia institucional','Se registrará una autorización existente. El portal no emitirá una resolución ni autorizará legalmente la convivencia.',async()=>{if(guardando)return;guardando=true;f.querySelectorAll('button,input').forEach(b=>b.disabled=true);
        try{const actualizado=await this.api(`/api/vinculaciones/${v.id}/autorizacion-preadoptiva`,{method:'POST',body:JSON.stringify(datos)});Profesionales.marcarGuardado(f);d.close();await this.detalle(c,v.id,sesion,actualizado);}
        catch(error){f.querySelector('[data-error]').textContent=error.message;guardando=false;f.querySelectorAll('button,input').forEach(b=>b.disabled=false);}
      });
    };d.showModal();
  },
  panelInterrupcion(v){
    const i=v.interrupcion;
    if(!i && !v.puedeReportarInterrupcion)return '';
    return `<section class="tarjeta"><h2>${Profesionales.icono('advertencia')}Interrupción de convivencia</h2>${i?`
      <p><strong>${i.fechaRevision?'Revisión registrada':'Pendiente de revisión del Comité'}</strong></p>
      <dl class="detalle-datos">${i.motivo?`<div><dt>Motivo de la interrupción</dt><dd>${limpiarHtml(this.motivosInterrupcion.find(([k])=>k===i.motivo)?.[1]||i.motivo)}</dd></div>`:''}${i.fechaInterrupcion?`<div><dt>Fecha de la interrupción registrada anteriormente</dt><dd>${limpiarHtml(i.fechaInterrupcion)}</dd></div>`:''}${i.referenciaActuacion?`<div><dt>Referencia institucional registrada anteriormente</dt><dd>${limpiarHtml(i.referenciaActuacion)}</dd></div>`:''}</dl>
      <p class="pro-texto-registrado">${limpiarHtml(i.justificacion)}</p><p>${Profesionales.icono('persona')}${limpiarHtml(i.registradoPor)} · ${limpiarHtml(Profesionales.etiqueta(i.rolProfesional))} · ${limpiarHtml(this.fechaHora(i.fechaRegistro))} · Panamá</p>
      ${i.fechaRevision?`<h3>Revisión del Comité</h3><p class="pro-texto-registrado">${limpiarHtml(i.revisionJustificacion)}</p><p>${limpiarHtml(i.revisadoPor)} · ${limpiarHtml(this.fechaHora(i.fechaRevision))}</p>`:''}
      `:'<p>Deja constancia de una interrupción de convivencia respaldada por la actuación institucional correspondiente.</p>'}
      <p class="ayuda">${Profesionales.icono('informacion')}El registro conserva los antecedentes y la reserva de los expedientes. No cambia su idoneidad ni formaliza una adopción.</p>
      ${v.puedeReportarInterrupcion?`<button class="boton secundario" data-interrupcion>${Profesionales.icono('advertencia')}Registrar interrupción de convivencia</button>`:''}
      ${v.puedeRevisarInterrupcion?`<button class="boton" data-revisar-interrupcion>${Profesionales.icono('solicitudes')}Registrar revisión</button>`:''}</section>`;
  },
  registrarInterrupcion(c,v,sesion){
    const d=document.createElement('dialog');d.className='pro-dialogo';d.setAttribute('aria-labelledby','tituloInterrupcion');
    d.innerHTML=`<div class="pro-modal-cabecera"><span class="pro-simbolo">${Profesionales.icono('advertencia')}</span><h2 id="tituloInterrupcion">Registrar interrupción de la convivencia</h2></div>
      <p>Deja constancia de lo ocurrido y de las circunstancias que deben considerarse durante la revisión del Comité.</p>
      <form class="pro-interrupcion-form"><label>Motivo de la interrupción<select name="motivo" required><option value="">Selecciona el motivo que mejor describa la situación</option>${this.motivosInterrupcion.map(([k,t])=>`<option value="${k}">${t}</option>`).join('')}</select></label>
      <label>Descripción de lo ocurrido<textarea name="justificacion" minlength="10" maxlength="2000" rows="5" required placeholder="Explica lo sucedido y las circunstancias que deba considerar el Comité."></textarea></label>
      <p data-error role="alert"></p><div class="pro-acciones"><button type="button" class="boton secundario" data-cancelar>Cancelar</button><button class="boton" type="submit">Revisar y confirmar</button></div></form>`;
    document.body.append(d);const form=d.querySelector('form');let guardando=false;
    d.onclose=()=>d.remove();d.addEventListener('cancel',e=>{if(guardando)e.preventDefault();});d.querySelector('[data-cancelar]').onclick=()=>d.close();
    form.elements.motivo.onchange=()=>{form.elements.justificacion.value=this.motivosInterrupcion.find(([k])=>k===form.elements.motivo.value)?.[2]||'';form.elements.justificacion.dispatchEvent(new Event('input',{bubbles:true}));};
    CamposFormulario.iniciar(form);
    form.onsubmit=e=>{e.preventDefault();if(guardando || !Profesionales.validarFormulario(form))return;
      const datos=Object.fromEntries(new FormData(form));
      Profesionales.confirmar('Confirmar interrupción','La información quedará disponible para revisión del Comité. Los expedientes conservarán su reserva.',async()=>{
        if(guardando)return;guardando=true;form.querySelectorAll('button').forEach(b=>b.disabled=true);
        try{const actualizado=await this.api(`/api/vinculaciones/${v.id}/interrupcion`,{method:'POST',body:JSON.stringify(datos)});Profesionales.marcarGuardado(form);d.close();await this.detalle(c,v.id,sesion,actualizado);c.insertAdjacentHTML('afterbegin','<p class="mensaje exito" role="status">La información se registró correctamente y quedó disponible para su revisión.</p>');}
        catch(error){d.querySelector('[data-error]').textContent=error.message;guardando=false;form.querySelectorAll('button').forEach(b=>b.disabled=false);}
      });
    };d.showModal();
  }
};
