/* Presentación familiar: reutiliza la entrevista y expone únicamente actividad autorizada del caso propio. */
const Familia = {
  async mostrar(sesion) {
    if (this.cargando) return;
    this.cargando = true;
    const solicitada = new URLSearchParams(location.search).get('vista');
    const vista = ['entrevista', 'propuesta', 'historial'].includes(solicitada)
      ? solicitada
      : 'inicio';
    document.body.classList.toggle('s8-acabado', vista !== 'inicio');
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
      document.querySelectorAll('[data-asignacion-info]').forEach((d) => {
        if (
          !vinculaciones.some(
            (v) => String(v.id) === d.dataset.asignacionInfo && v.puedeConsultarInformacion,
          )
        )
          d.close();
      });
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
      const vigente = Vinculaciones.seleccionarVinculacion(vinculaciones);
      const progreso = Vinculaciones.obtenerProgreso(vigente);
      const eventos = [
        ...(p.actividad || []),
        ...vinculaciones.flatMap((v) => Vinculaciones.obtenerProgreso(v).eventos),
      ].sort((a, b) => new Date(b.fecha) - new Date(a.fecha));
      const ultimo = progreso
        ? [...progreso.eventos].sort((a, b) => new Date(b.fecha) - new Date(a.fecha))[0]
        : eventos[0];
      let estado = 'Entrevista incompleta';
      let accion = 'Revisa y completa tu entrevista.';
      if (p.perfilCompleto) {
        estado = 'En espera de una propuesta del Comité';
        accion = 'El Comité revisará las propuestas según los requisitos y la disponibilidad institucional.';
      }
      if (p.reevaluacionPendiente) {
        estado = 'Entrevista en revisión';
        accion = 'Revisa y completa tu entrevista.';
      }
      if (sinEntrevista) accion = 'Completa tu entrevista para iniciar el proceso.';
      if (progreso) {
        estado = progreso.estado;
        accion = progreso.accion;
      }
      const proceso = progreso
        ? `<section class="tarjeta">${Vinculaciones.progresoFamiliar(vigente)}</section>`
        : `
      <section class="tarjeta">
        <h2>${Profesionales.icono('solicitudes')}Preparación del expediente familiar</h2>
        <p>${limpiarHtml(estado)}</p>
        <p>Todavía no tienes una propuesta registrada.</p>
        ${
          p.reevaluacionPendiente || !p.perfilCompleto
            ? `
          <a class="boton" href="${enlaceEntrevista}">
            ${p.perfilCompleto ? 'Revisar entrevista' : 'Completar mi entrevista'}
          </a>`
            : ''
        }
      </section>`;
      const propuesta = Vinculaciones.familiar(vinculaciones);
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
            ${visibles.length ? Vinculaciones.lineaTiempo(visibles.map(evento => ({...evento, resumen: evento.descripcion, zona: 'Panamá'}))) : '<p>Todavía no hay actuaciones disponibles.</p>'}
            ${todos && p.numeroReevaluaciones ? `
              <p class="ayuda">Reevaluaciones completadas: ${Number(p.numeroReevaluaciones)}.
                Solo se muestran fechas que quedaron registradas.</p>` : ''}
          </section>`;
      };
      const tarjetas = [
        ['carpeta', 'Estado actual', estado],
        ['calendario', 'Última actualización', ultimo
          ? ultimo.titulo + ' · ' + Profesionales.fecha(ultimo.fecha)
          : 'Sin actualizaciones registradas'],
        ['comprobar', 'Próxima acción', accion]
      ];
      const resumen = `
        <div class="familia-resumen">
          ${tarjetas.map(([icono, titulo, texto]) => `
            <section class="tarjeta">
              <span class="pro-simbolo">${Profesionales.icono(icono)}</span>
              <h3>${titulo}</h3><p>${limpiarHtml(texto)}</p>
            </section>`).join('')}
        </div>`;
      const pantallas = {
        inicio: [
          'Bienvenida, familia',
          'Consulta el estado de tu proceso de adopción, las etapas y las actualizaciones registradas.',
          'hogar',
        ],
        entrevista: [
          'Mi entrevista',
          'Consulta las respuestas de tu hogar y actualízalas cuando cambie tu situación.',
          'informe',
        ],
        propuesta: [
          'Mi vinculación',
          'Consulta el resultado comunicado a tu familia y los siguientes pasos.',
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
      const avisoPropuesta = pendiente && vista !== 'propuesta' ? `
        <section class="tarjeta" role="status">
          ${Profesionales.icono('informacion')} Tienes una propuesta pendiente de respuesta.
          <a href="match.html?vista=propuesta">Consultar mi vinculación</a>
        </section>` : '';
      document.getElementById('contenido').innerHTML =
        Profesionales.encabezado(titulo, descripcion, icono) + avisoPropuesta
        + (['inicio', 'entrevista'].includes(vista) ? this.avisoReevaluacion(p) : '')
        + contenidoVista;
      Vinculaciones.enlazarRespuestas(sesion);
    } catch (e) {
      this.version = null;
      document.getElementById('contenido').innerHTML =
        `<section class="tarjeta"><p role="alert">${limpiarHtml(e.message)}</p></section>`;
    } finally {
      this.cargando = false;
    }
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
