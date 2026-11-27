/* Vistas del Comité sobre las API existentes. No calcula ni altera puntuaciones. */
const Comite = {
  async iniciar(s) {
    const parametros=new URLSearchParams(location.search);
    this.sesion=s;this.vista=parametros.get('vista')||(parametros.get('caso')?'compatibilidad':'inicio');this.codigo=parametros.get('caso');this.familia=parametros.get('familia');this.contenido=document.getElementById('contenido');
    document.body.classList.toggle('s8-acabado',this.vista!=='inicio');
    Profesionales.iniciar(s,this.vista);document.getElementById('avisosPendientes').hidden=true;
    this.contenido.innerHTML='<p role="status">Cargando información…</p>';
    try {
      if(this.vista==='solicitudes'){await this.solicitudes();return;}
      if(this.vista==='criterios'){await this.criterios();return;}
      if(this.vista==='reevaluaciones'){location.replace('match.html?vista=compatibilidad');return;}
      this.casos=await this.api('/api/scoring/casos');this.programacion=await this.api('/api/scoring/pesos/programacion');
      if(this.vista==='compatibilidad'){this.revisionesPendientes=await this.api('/api/scoring/reevaluaciones-pendientes');if(this.codigo&&this.familia)await this.analisis();else this.compatibilidad();}
      else this.inicio();
    }catch(e){this.contenido.innerHTML=Profesionales.encabezado('Panel del Comité','Consulta las propuestas y solicitudes.','personas')+`<p class="mensaje error" role="alert">${limpiarHtml(e.message)}</p>`;}
  },
  async api(ruta,opciones={}) {
    const r=await fetch(ruta,{...opciones,cache:'no-store',headers:Sesion.encabezados({'Content-Type':'application/json'})});const datos=await r.json();
    if(!r.ok){const error=new Error(datos.mensaje||'No se pudo cargar la información. Inténtalo de nuevo.');error.estado=r.status;throw error;}return datos;
  },
  icono(n){return Profesionales.icono(n);},
  vinculo(c,f){return `match.html?vista=compatibilidad&caso=${encodeURIComponent(c)}${f?'&familia='+encodeURIComponent(f):''}`;},
  estadoInformacion(familia) {
    const estados = Object.values(familia.estadosFactores || {});
    if (!estados.length) return 'Desglose disponible';
    return estados.includes('DESCONOCIDO') ? 'Información por aclarar' : 'Criterios evaluados';
  },
  puntaje(familia, separador) {
    if (familia.maximoAplicable === 0) return '<small>Sin criterios aplicables con peso</small>';
    return `<strong>${familia.puntaje}</strong><small>${separador}${familia.maximoAplicable}</small>`;
  },
  medidor(familia) {
    if (!(familia.maximoAplicable > 0)) return '';
    const porcentaje = Math.max(0, Math.min(100, 100 * familia.puntaje / familia.maximoAplicable));
    return `<div class="pro-medidor"><span style="width:${porcentaje}%"></span></div>`;
  },
  inicio(){
    this.contenido.innerHTML=Profesionales.encabezado('Panel del Comité','Revisa la compatibilidad y gestiona las solicitudes recibidas.','personas')+`
      <div class="pro-dashboard"><section class="tarjeta pro-modulo"><span class="pro-simbolo">${this.icono('salud')}</span><h2>Compatibilidad</h2><p>Consulta los expedientes y las familias propuestas.</p><a class="boton" href="match.html?vista=compatibilidad">Ver compatibilidad ${this.icono('flecha')}</a></section>
      <section class="tarjeta pro-modulo violeta"><span class="pro-simbolo">${this.icono('solicitudes')}</span><h2>Solicitudes</h2><p>Revisa las peticiones de desactivación o reactivación.</p><a class="boton" href="match.html?vista=solicitudes">Ver solicitudes ${this.icono('flecha')}</a></section></div>
      <section class="tarjeta"><h2>Expedientes para analizar</h2><p>Abre un caso para consultar sus familias propuestas.</p>${this.casos.length?this.casos.map(c=>`<article class="pro-reciente"><span class="pro-simbolo">${this.icono('carpeta')}</span><div><strong>${limpiarHtml(c.codigo)}</strong><p>Comparar expedientes</p></div><a class="boton secundario chico" href="${this.vinculo(c.codigo)}">Ver compatibilidad ${this.icono('flecha')}</a></article>`).join(''):'<p>No hay expedientes disponibles para analizar.</p>'}</section>`;
  },
  compatibilidad(){
    const seleccionado=this.codigo?this.casos.filter(c=>c.codigo===this.codigo):this.casos;
    this.contenido.innerHTML=Profesionales.encabezado('Análisis de compatibilidad','Revisa las familias propuestas para cada expediente infantil.','salud')+this.avisoProgramacion(this.programacion?.pendiente)+`
      ${this.codigo?'<p><a href="match.html?vista=compatibilidad">← Ver todos los expedientes</a></p>':''}
      <div class="pro-herramientas"><label class="pro-buscar">${this.icono('buscar')}<input id="buscarCompatibilidad" type="search" placeholder="Buscar expediente infantil…" aria-label="Buscar expediente infantil"></label><select id="ordenCompatibilidad" aria-label="Orden de compatibilidad"><option value="mayor">Mayor compatibilidad</option><option value="menor">Menor compatibilidad</option></select></div><div id="propuestasComite"></div>
      <section class="tarjeta pro-familias-reevaluacion"><h2>${this.icono('reloj')}Familias en reevaluación</h2>${this.revisionesPendientes?.length?this.revisionesPendientes.map(c=>`<article class="pro-reevaluacion-fila"><h3>${limpiarHtml(c)}</h3><span class="chip">${this.icono('reloj')}En revisión de entrevista</span><p>No recibe nuevas recomendaciones hasta completar su actualización.</p><a class="boton secundario" href="perfiles.html?familia=${encodeURIComponent(c)}&origen=match">Ver reevaluación ${this.icono('flecha')}</a></article>`).join(''):'<p>No hay familias con una reevaluación pendiente.</p>'}</section>`;
    const pintar=()=>{
      const busca=document.getElementById('buscarCompatibilidad').value.trim().toLocaleLowerCase('es');const orden=document.getElementById('ordenCompatibilidad').value==='mayor'?-1:1;
      const casos=seleccionado.filter(c=>c.codigo.toLocaleLowerCase('es').includes(busca));
      document.getElementById('propuestasComite').innerHTML=casos.length?casos.map(c=>`<section class="tarjeta pro-compatibilidad-caso"><div class="pro-reciente"><span class="pro-simbolo">${this.icono('carpeta')}</span><div><span class="pro-capsula">Expediente infantil</span><h2>${limpiarHtml(c.codigo)}</h2><p>Familias propuestas para análisis</p></div></div>
        ${c.familias.length?[...c.familias].sort((a,b)=>orden*(a.puntaje-b.puntaje)).map(f=>`<article class="pro-compatibilidad-familia"><div class="pro-fila-titulo"><div><span class="pro-capsula">Familia propuesta</span><h3>${limpiarHtml(f.codigo)}</h3><span class="chip">${this.estadoInformacion(f)}</span></div><div class="pro-puntaje">${this.puntaje(f,"/")}${f.maximoAplicable>0?'<p class="ayuda">Puntos acreditados</p>':''}</div></div>${Profesionales.avisoInterrupcion(f.antecedenteInterrupcion)}${this.medidor(f)}<div class="pro-propuesta-pie"><p>${Object.entries(f.f).slice(0,2).map(([k,v])=>limpiarHtml(FACTORES[k]?.n||k)+': '+(f.estadosFactores?.[k]==='NO_APLICA'?'No aplica':v[0]+' puntos')).join(' · ')}</p><a class="boton" href="${this.vinculo(c.codigo,f.codigo)}">Ver análisis completo ${this.icono('flecha')}</a></div></article>`).join(''):`<p>${c.propuestaAceptada?'Este expediente ya tiene una propuesta aceptada. Consulta su historial.':'No hay familias disponibles para este expediente.'}</p><a href="perfiles.html?caso=${encodeURIComponent(c.codigo)}#panelAuditoria">Consultar historial</a>`}
        </section>`).join(''):'<div class="tarjeta"><p>No hay expedientes disponibles con ese código.</p></div>';
    };document.getElementById('buscarCompatibilidad').addEventListener('input',pintar);document.getElementById('ordenCompatibilidad').addEventListener('change',pintar);pintar();
  },
  async analisis(){
    const caso=this.casos.find(c=>c.codigo===this.codigo),familia=caso?.familias.find(f=>f.codigo===this.familia);
    if(caso&&!familia&&caso.familiasRechazadas?.includes(this.familia)){this.contenido.innerHTML=Profesionales.encabezado('Combinación no disponible','Esta combinación tiene un rechazo o una interrupción registrada. Consulta los antecedentes antes de continuar.','informacion')+`<a class="boton secundario" href="match.html?vista=compatibilidad">Volver a compatibilidad</a><p><a href="perfiles.html?caso=${encodeURIComponent(this.codigo)}#panelAuditoria">Consultar antecedentes</a></p>`;return;}
    if(!caso||!familia){this.contenido.innerHTML=Profesionales.encabezado('Análisis no disponible','La propuesta ya no está disponible en el análisis actual.','salud')+`<a class="boton secundario" href="match.html?vista=compatibilidad">Volver a compatibilidad</a><p><a href="perfiles.html?caso=${encodeURIComponent(this.codigo)}&origen=match">Consultar el expediente y su historial</a></p>`;return;}
    const pesos=await this.api('/api/scoring/pesos');
    if(pesos.version!==caso.versionPesos){this.casos=await this.api('/api/scoring/casos');return this.analisis();}
    const bloqueado=caso.propuestaAceptada;
    const gruposNino=[['escuela','Datos generales y escolaridad','Edad, escolaridad y rendimiento'],['estrella','Habilidades e intereses','Gustos, capacidades y personalidad'],['personas','Contexto familiar','Hermanos y vínculos importantes'],['salud','Salud y apoyos','Condiciones y necesidades de atención'],['informe','Informes profesionales','Información social y psicológica'],['personas','Identidad y continuidad cultural','Idioma, tradiciones, vínculos y recomendaciones de acompañamiento']];
    const gruposFamilia=[['informe','Información general','Datos registrados por la familia'],['hogar','Composición del hogar','Estructura y entorno familiar'],['solicitudes','Entrevista familiar','Respuestas y preferencias registradas'],['salud','Capacidad de acompañamiento','Cuidados y necesidades que puede atender'],['personas','Red de apoyo','Apoyos disponibles para la familia'],['heart-handshake','Apoyos culturales y comunitarios','Apoyos que la familia puede ofrecer para respetar la identidad y los vínculos del niño']];
    const tarjeta=(titulo,codigo,grupos,href,clase='')=>`<section class="tarjeta pro-expediente-tarjeta ${clase}"><header>${this.icono('personas')}<div><h2>${titulo}</h2><p>${limpiarHtml(codigo)}</p></div></header><ul>${grupos.map(([icono,t,d])=>`<li>${this.icono(icono)}<div><strong>${t}</strong><small>${d}</small></div></li>`).join('')}</ul><a class="boton secundario" href="${href}">Ver ${titulo.toLocaleLowerCase('es')} completo ${this.icono('flecha')}</a></section>`;
    this.contenido.innerHTML=`<p class="pro-breadcrumb"><a href="match.html?vista=compatibilidad">← Volver a compatibilidad</a></p>`+Profesionales.encabezado(caso.codigo+' y '+familia.codigo,'Revisa los dos expedientes y los criterios utilizados para esta propuesta.','salud')+this.avisoProgramacion(this.programacion?.pendiente,'mini')+`
      ${Profesionales.avisoInterrupcion(familia.antecedenteInterrupcion)}<div class="pro-dashboard">${tarjeta('Expediente infantil',caso.codigo,gruposNino,`perfiles.html?caso=${encodeURIComponent(caso.codigo)}&origen=match&familiaOrigen=${encodeURIComponent(familia.codigo)}`)}${tarjeta('Expediente familiar',familia.codigo,gruposFamilia,`perfiles.html?familia=${encodeURIComponent(familia.codigo)}&casoOrigen=${encodeURIComponent(caso.codigo)}&origen=match`,'verde')}</div>
      ${this.resultadoCriterios(familia,pesos)}
${Profesionales.avisoReevaluaciones(familia.reevaluaciones)}
      <section class="tarjeta pro-seccion"><h2>Decisión del Comité</h2><p>${bloqueado?'Este expediente ya tiene una propuesta aceptada.':'Selecciona la acción que corresponde a esta propuesta.'}</p>${bloqueado?'':`<div class="pro-acciones"><button class="boton" type="button" data-decision="ACEPTADA">${this.icono('comprobar')}Aceptar propuesta</button><button class="boton secundario" type="button" data-decision="DESCARTADA">Rechazar propuesta</button><button class="boton secundario" type="button" data-decision="REEVALUACION">Solicitar reevaluación</button></div>`}<p id="estadoDecision" role="status"></p></section>`;

    this.contenido.querySelector('#reevaluarCompatibilidad')?.addEventListener('click', () => this.reevaluar());
    this.contenido.querySelectorAll('[data-decision]').forEach(b=>b.addEventListener('click',()=>this.decidir(caso,familia,b.dataset.decision)));
  },
  resultadoCriterios(familia, pesos) {
    return Profesionales.analisisCompatibilidad({
      titulo:'Análisis de compatibilidad', descripcion:'Consulta el resultado y sus criterios.',
      puntaje:familia.puntaje, maximo:familia.maximoAplicable, factores:familia.f,
      pesos, estados:familia.estadosFactores, actualizar:true,
      nota:`<p class="ayuda pro-ayuda-con-icono">${this.icono('informacion')}<span>Los criterios que no aplican se excluyen del puntaje máximo. Peso de criterios evaluados: ${familia.pesoEvaluado}.</span></p>`
    });
  },
  async reevaluar() {
    if (this.reevaluando) return;
    this.reevaluando = true;
    const aviso = document.getElementById('estadoReevaluacion');
    aviso.textContent = 'Actualizando el análisis…';
    this.contenido.querySelectorAll('#reevaluarCompatibilidad,[data-decision]').forEach(b => b.disabled = true);
    try {
      const actual = await this.api('/api/scoring/casos/' + encodeURIComponent(this.codigo));
      this.casos = this.casos.map(c => c.codigo === actual.codigo ? actual : c);
      await this.analisis();
      const detalle = this.contenido.querySelector('.analisis-desglose');
      if (detalle) detalle.open = true;
      const estado = document.getElementById('estadoReevaluacion');
      if (estado) estado.textContent = 'Análisis actualizado con la información vigente.';
      document.getElementById('reevaluarCompatibilidad')?.focus();
    } catch (e) {
      aviso.textContent = 'No se pudo actualizar el análisis. El resultado mostrado corresponde a la consulta anterior. ' + e.message;
    } finally {
      this.reevaluando = false;
      this.contenido.querySelectorAll('#reevaluarCompatibilidad,[data-decision]').forEach(b => b.disabled = false);
    }
  },
  decidir(caso,familia,tipo){
    const motivos={
      ACEPTADA:[['FAVORABLE','Compatibilidad favorable','Después de revisar ambos expedientes, se considera que la familia cuenta con condiciones favorables para continuar con el proceso de vinculación.'],['CONDICIONES','Condiciones adecuadas para continuar','La revisión de los apoyos y de la disponibilidad de la familia permite considerar adecuadas las condiciones para continuar el proceso de vinculación con seguimiento profesional.'],['OTRO','Otro','']],
      DESCARTADA:[['INFORMACION','Información insuficiente','La información disponible resulta insuficiente para continuar con esta propuesta. Se considera conveniente evaluar otras alternativas y completar los antecedentes necesarios.'],['CAPACIDADES','Las necesidades requieren otras capacidades de acompañamiento','Después de revisar la información disponible, se considera conveniente evaluar otras alternativas que respondan mejor a las necesidades identificadas en el expediente.'],['OTRO','Otro','']],
      REEVALUACION:[['INCOMPLETA','Información incompleta','Se solicita completar la información de los apartados seleccionados para que el Comité pueda realizar una revisión adecuada de las condiciones familiares.'],['DESACTUALIZADA','Información desactualizada','Se solicita actualizar la información de los apartados seleccionados para contar con datos recientes sobre las condiciones actuales de la familia.'],['ACLARACION','Se necesita una aclaración','Se solicita aclarar la información proporcionada en los apartados seleccionados para comprender mejor las circunstancias descritas y continuar con la evaluación.'],['INCONSISTENCIAS','Inconsistencias en la entrevista','Se identificaron diferencias en la información registrada que requieren aclaración antes de continuar con la evaluación.']]
    };
    const titulos={ACEPTADA:'Aceptar propuesta',DESCARTADA:'Rechazar propuesta',REEVALUACION:'Solicitar reevaluación'};
    if(!motivos[tipo])return;
    Profesionales.justificar({titulo:titulos[tipo],ayuda:tipo==='REEVALUACION'?'Indica lo que debe revisar la familia en los apartados seleccionados.':undefined,textoRevisar:'Revisar y confirmar',maximo:tipo==='REEVALUACION'?1500:1850,contexto:tipo==='REEVALUACION'?'Expediente familiar · '+familia.codigo:caso.codigo+' · '+familia.codigo,opciones:motivos[tipo],
      confirmacion:tipo==='ACEPTADA'?'¿Deseas aceptar esta propuesta para continuar el proceso de vinculación? Esta decisión no formaliza una adopción.':tipo==='DESCARTADA'?'¿Deseas descartar esta propuesta y conservar la decisión justificada en el historial?':'¿Deseas solicitar la reevaluación? La familia deberá actualizar su entrevista antes de continuar el análisis.',
      alError:error=>{if(tipo==='REEVALUACION'||error.estado!==409)return false;const aviso=document.getElementById('estadoDecision');aviso.className='mensaje error';aviso.textContent=error.message;const boton=document.createElement('button');boton.className='boton secundario';boton.textContent='Actualizar compatibilidad';boton.onclick=async()=>{boton.disabled=true;this.contenido.querySelectorAll('[data-decision]').forEach(b=>b.disabled=true);try{this.casos=await this.api('/api/scoring/casos');this.programacion=await this.api('/api/scoring/pesos/programacion');await this.analisis();}catch(e){aviso.textContent=e.message;boton.disabled=false;this.contenido.querySelectorAll('[data-decision]').forEach(b=>b.disabled=false);}};aviso.append(document.createElement('br'),boton);return true;},
      alConfirmar:(motivo,seleccion)=>this.api(`/api/scoring/casos/${encodeURIComponent(caso.codigo)}/decisiones`,{method:'POST',body:JSON.stringify({familia:familia.codigo,decision:tipo,motivo,versionPesos:caso.versionPesos,...(tipo==='REEVALUACION'?seleccion:{})})}),
      alGuardar:async datos=>{
        if(tipo==='REEVALUACION'){
          this.codigo=null;this.familia=null;this.vista='compatibilidad';history.replaceState(null,'','match.html?vista=compatibilidad');
          try{this.casos=this.casos.map(c=>({...c,familias:c.familias.filter(f=>f.codigo!==familia.codigo)}));this.revisionesPendientes=await this.api('/api/scoring/reevaluaciones-pendientes');this.compatibilidad();}
          catch(error){this.contenido.innerHTML=Profesionales.encabezado('Análisis de compatibilidad','Consulta las propuestas disponibles.','salud');const pendiente=document.createElement('p');Profesionales.mensaje(pendiente,'La solicitud quedó guardada. No se pudo actualizar el listado: '+error.message,'aviso');this.contenido.append(pendiente);}
          const aviso=document.createElement('p');Profesionales.mensaje(aviso,'La solicitud de reevaluación se registró correctamente','exito');this.contenido.prepend(aviso);return;
        }
        try{this.casos=await this.api('/api/scoring/casos');await this.analisis();const aviso=document.getElementById('estadoDecision');if(aviso)aviso.textContent=datos.mensaje||'Decisión registrada correctamente.';else this.contenido.insertAdjacentHTML('afterbegin',`<p role="status" class="mensaje exito">${limpiarHtml(datos.mensaje||'Decisión registrada correctamente.')}</p>`);}catch(e){this.contenido.insertAdjacentHTML('afterbegin',`<p role="status" class="mensaje aviso">La decisión se guardó. ${limpiarHtml(e.message)} Recarga para consultar el estado actualizado.</p>`);}}
    });
  },
  async solicitudes(){
    const [expedientes,familiares]=await Promise.all([this.api('/api/ninos?incluirInactivos=true'),this.api('/api/reincorporaciones')]);
    const infantiles=(await Promise.all(expedientes.map(p=>this.api(`/api/ninos/${encodeURIComponent(p.codigoCaso)}/solicitudes-estado`)))).flat();
    const datos=[...infantiles,...familiares.map(s=>({...s,familiar:true}))];
    const pendiente=s=>['PENDIENTE','ENTREVISTA_PENDIENTE','REVISION_PENDIENTE'].includes(s.estado);
    const pendientes=datos.filter(pendiente);
    const porRevisar=pendientes.filter(s=>s.estado!=='ENTREVISTA_PENDIENTE').length;
    this.contenido.innerHTML=Profesionales.encabezado('Solicitudes de expedientes','Consulta las solicitudes recibidas y sus decisiones.','solicitudes')+`<section class="tarjeta"><div class="pro-fila-titulo"><div><h2>Solicitudes</h2><p>Consulta las solicitudes recibidas y sus decisiones.</p></div><span class="pro-conteo">${porRevisar} <small>Por revisar</small></span></div><div class="pro-pestanas" role="tablist" aria-label="Estado de solicitudes"><button id="tabPendientes" type="button" role="tab" aria-selected="true" aria-controls="listaSolicitudesComite">Pendientes · ${pendientes.length}</button><button id="tabResueltas" type="button" role="tab" aria-selected="false" aria-controls="listaSolicitudesComite">Resueltas · ${datos.length-pendientes.length}</button></div><label>Tipo de expediente<select id="filtroSolicitudesComite"><option value="">Todos los expedientes</option><option value="infantil">Expedientes infantiles</option><option value="familiar">Expedientes familiares</option></select></label><div id="listaSolicitudesComite" role="tabpanel" aria-labelledby="tabPendientes"></div></section>`;
    const pintar=resueltas=>{
      const filtro=document.getElementById('filtroSolicitudesComite').value;
      const seleccion=datos.filter(s=>pendiente(s)!==resueltas && (!filtro || Boolean(s.familiar)===(filtro==='familiar'))).sort((a,b)=>new Date(b.fechaSolicitud)-new Date(a.fechaSolicitud));
      document.getElementById('tabPendientes').setAttribute('aria-selected',String(!resueltas));document.getElementById('tabResueltas').setAttribute('aria-selected',String(resueltas));const lista=document.getElementById('listaSolicitudesComite');lista.setAttribute('aria-labelledby',resueltas?'tabResueltas':'tabPendientes');
      lista.innerHTML=seleccion.length?seleccion.map(s=>this.tarjetaSolicitud(s)).join(''):`<div class="pro-vacio"><span class="pro-simbolo">${this.icono('comprobar')}</span><div><h3>${resueltas?'Sin solicitudes resueltas':'Sin solicitudes pendientes'}</h3><p>${filtro?'No hay solicitudes con los filtros seleccionados.':resueltas?'Las decisiones del Comité aparecerán aquí.':'No hay solicitudes pendientes de revisión.'}</p></div></div>`;
      lista.querySelectorAll('[data-revisar-reincorporacion]').forEach(b=>b.onclick=()=>
        this.revisarReincorporacion(familiares.find(s=>String(s.id)===b.dataset.revisarReincorporacion)));
    };document.getElementById('tabPendientes').addEventListener('click',()=>pintar(false));document.getElementById('tabResueltas').addEventListener('click',()=>pintar(true));
    document.getElementById('filtroSolicitudesComite').addEventListener('change',()=>pintar(document.getElementById('tabResueltas').getAttribute('aria-selected')==='true'));
    pintar(false);
  },
  tarjetaSolicitud(s) {
    const codigo = encodeURIComponent(s.codigoCaso);
    const destino = s.familiar ? `perfiles.html?familia=${codigo}&origen=solicitudes#gestionFamiliar`
      : `perfiles.html?caso=${codigo}&origen=solicitudes#panelSolicitudes`;
    return `<article class="pro-reciente solicitud-resumen">
      <span class="pro-simbolo">${this.icono(s.familiar ? 'personas' : 'solicitudes')}</span>
      <div><strong>${limpiarHtml(s.codigoCaso)} · ${limpiarHtml(Profesionales.tituloSolicitud(s))}</strong>
        <dl class="solicitud-meta"><div><dt>Solicitante</dt><dd>${limpiarHtml(s.solicitante || 'Sin registro')} · ${limpiarHtml(Profesionales.fechaSolicitud(s.fechaSolicitud))}</dd></div>
        ${s.resueltoPor || s.fechaResolucion ? `<div><dt>Resolución</dt><dd>${limpiarHtml(s.resueltoPor || 'Sin registro')} · ${limpiarHtml(Profesionales.fechaSolicitud(s.fechaResolucion))}</dd></div>` : ''}</dl>
      </div><div class="solicitud-estado">${Profesionales.estadoSolicitud(s)}</div><div class="pro-acciones">
        <a class="boton secundario chico" href="${destino}">Consultar expediente ${this.icono('flecha')}</a>
        ${s.familiar && s.estado === 'REVISION_PENDIENTE' ? `<button type="button" class="boton chico" data-revisar-reincorporacion="${Number(s.id)}">Revisar solicitud</button>` : ''}
      </div></article>`;
  },
  revisarReincorporacion(s) {
    if (!s || document.querySelector('[data-revisar-reincorporacion-dialogo]')) return;
    const revisable=s.estado==='REVISION_PENDIENTE', d=document.createElement('dialog');
    d.className='pro-dialogo';d.dataset.revisarReincorporacionDialogo='';d.setAttribute('aria-label','Revisar reincorporación familiar');
    d.innerHTML=`<div class="pro-modal-cabecera"><span class="pro-simbolo">${this.icono('solicitudes')}</span><h2>Revisar reincorporación familiar</h2></div>
      <p>${limpiarHtml(s.codigoCaso)} · Antecedente ${limpiarHtml(s.vinculacion)} · ${limpiarHtml(Profesionales.fechaSolicitud(s.fechaSolicitud))}</p>
      <dl class="detalle-datos"><div><dt>Proceso anterior</dt><dd>Adopción formalizada</dd></div>
        <div><dt>Entrevista familiar</dt><dd>${s.estado==='ENTREVISTA_PENDIENTE'?'Pendiente de actualización':'Actualizada'}</dd></div></dl>
      <p><a class="boton secundario" href="perfiles.html?familia=${encodeURIComponent(s.codigoCaso)}&origen=solicitudes" target="_blank" rel="noopener">${this.icono('consulta')}Consultar expediente familiar</a></p>
      ${revisable?`<form class="pro-interrupcion-form" novalidate>
        <fieldset class="pro-motivos"><legend>Resultado de la revisión *</legend>
        <label class="pro-motivo"><input type="radio" name="resultado" value="autorizar">${this.icono("comprobar")}<span>Autorizar reincorporación</span></label>
        <label class="pro-motivo"><input type="radio" name="resultado" value="denegar">${this.icono("advertencia")}<span>No autorizar reincorporación</span></label></fieldset>
        <label>Justificación de la decisión *<textarea name="fundamento" rows="4" maxlength="2000"></textarea></label>
        <p data-efecto hidden></p><label class="pro-confirmacion-check"><input type="checkbox" name="confirmacion">
        <span>Confirmo que se realizó la revisión institucional y, si autorizo, que la familia reúne las condiciones de elegibilidad.</span></label>
        <p class="mensaje error" role="alert"></p><div class="pro-acciones"><button type="button" class="boton secundario" data-cancelar>Cancelar</button>
        <button type="submit" class="boton">Registrar decisión</button></div></form>`:`
        <p>${Vinculaciones.estadoReincorporacion(s.estado)}</p>
        ${s.resueltoPor?`<p>${limpiarHtml(s.resueltoPor)} · ${limpiarHtml(Profesionales.fechaSolicitud(s.fechaResolucion))}</p><p class="pro-texto-registrado">${limpiarHtml(s.motivoResolucion)}</p>`:''}
        <button type="button" class="boton secundario" data-cancelar>Cerrar</button>`}`;
    const cancelar=d.querySelector('[data-cancelar]');
    d.onclose=()=>d.remove();cancelar.onclick=()=>d.close();
    if(revisable){
      const form=d.querySelector('form'), campo=form.elements.fundamento, enviar=form.querySelector('[type=submit]');
      const sugerir=Profesionales.sugerenciasDecision(campo);
      const opciones={
        autorizar:[
          ['Condiciones favorables','Tras revisar la información actualizada y los antecedentes familiares, se confirma que la familia reúne las condiciones institucionales para participar nuevamente en el Match.'],
          ['Reevaluación favorable','Se registra la autorización de reincorporación con fundamento en el resultado favorable de la evaluación familiar correspondiente.']],
        denegar:[
          ['Evaluaciones pendientes','No se autoriza la reincorporación debido a que se requieren evaluaciones institucionales complementarias.'],
          ['Condiciones insuficientes','Tras la revisión institucional, se determina que las condiciones actuales no permiten autorizar la reincorporación.'],
          ['Requisitos pendientes','La reincorporación no se autoriza debido a que existen requisitos institucionales pendientes de cumplimiento.']]
      };
      for(const item of form.querySelectorAll('[name=resultado]')) item.onchange=()=>{
        sugerir([...opciones[item.value],['Otro motivo','']]);
        const efecto=form.querySelector('[data-efecto]');
        efecto.hidden=false;
        efecto.className='mensaje '+(item.value==='autorizar'?'exito':'aviso');
        efecto.textContent=item.value==='autorizar'?'La reincorporación es exclusivamente familiar. La adopción anterior permanece cerrada.'
          :'La familia permanecerá fuera del Match.';
      };
      d.oncancel=e=>{if(enviar.disabled)e.preventDefault();};
      form.onsubmit=async e=>{
        e.preventDefault();
        if(enviar.disabled)return;
        const error=form.querySelector('[role=alert]');
        const resultado=form.elements.resultado.value;
        if(!resultado||campo.value.trim().length<10||!form.elements.confirmacion.checked){
          error.textContent='Selecciona el resultado, escribe una justificación de al menos 10 caracteres y confirma la revisión.';return;
        }
        enviar.disabled=cancelar.disabled=true;
        try{
          await this.api(`/api/reincorporaciones/${Number(s.id)}/resolucion`,{method:'POST',
            body:JSON.stringify({autorizar:resultado==='autorizar',fundamento:campo.value,confirmacion:true})});
        }catch(fallo){
          error.textContent=fallo.message;enviar.disabled=cancelar.disabled=false;return;
        }
        d.close();
        await this.solicitudes();
      };
    }
    document.body.append(d);d.showModal();
  },
  fechaProgramada(fecha){return new Intl.DateTimeFormat('es-PA',{timeZone:'America/Panama',dateStyle:'long',timeStyle:'short',hour12:true}).format(new Date(fecha));},
  avisoProgramacion(p,modo='resumen'){
    if(!p)return '';
    if(modo==='mini')return '<p class="mensaje aviso">Hay una actualización de criterios programada. Los valores vigentes se mantienen hasta su activación.</p>';
    if(modo==='resumen')return `<section class="tarjeta pro-aviso-programacion" role="status"><h2>${this.icono('calendario')}Actualización de criterios programada</h2><p>Se han programado ajustes en los pesos del análisis de compatibilidad. Los criterios actuales seguirán vigentes hasta que se active la nueva configuración.</p><a class="boton secundario" href="match.html?vista=criterios">${this.icono('consulta')}Consultar programación ${this.icono('flecha')}</a></section>`;
    return `<section class="tarjeta pro-aviso-programacion pro-modulo-pesos" role="status"><h2>${this.icono('reloj')}Actualización de criterios programada</h2><p>Los nuevos pesos entrarán en vigor el <strong>${limpiarHtml(this.fechaProgramada(p.activarEl))}</strong>, hora de Panamá. Hasta entonces se mantendrán los valores actuales.</p><dl class="detalle-datos">${Object.entries(FACTORES).filter(([k])=>p.anteriores[k]!==p.nuevos[k]).map(([k,f])=>`<div><dt>${limpiarHtml(f.n)}</dt><dd>${p.anteriores[k]} → ${p.nuevos[k]} puntos</dd></div>`).join('')}</dl><p><strong>Justificación:</strong> ${limpiarHtml(p.justificacion)}</p></section>`;
  },
  historialPesos(historial) {
    const fecha = valor => limpiarHtml(Profesionales.fechaSolicitud(valor).replace(' · ', ', '));
    const meta = (icono, texto) => `<p class="pesos-meta">${this.icono(icono)}<span>${texto}</span></p>`;
    const tarjetas = [...historial].reverse().map(({autor, fecha: registrada, cambio: c}) => {
      const pendiente = c.estado === 'PENDIENTE';
      const titulo = pendiente ? 'Cambio programado' : c.estado === 'APLICADA' ? 'Cambio aplicado' : 'Pesos actualizados';
      const cambios = Object.entries(FACTORES).filter(([k]) => c.anteriores?.[k] !== c.nuevos?.[k])
        .map(([k,f]) => `<div><dt>${limpiarHtml(f.n)}</dt><dd>${c.anteriores?.[k] ?? '—'} → ${c.nuevos?.[k] ?? '—'} puntos</dd></div>`).join('');
      return `<li class="${pendiente ? 'programado' : 'aplicado'}"><article class="pesos-registro">
        <span class="pesos-estado">${this.icono(pendiente ? 'reloj' : 'comprobar')}${titulo}</span>
        ${meta('persona', limpiarHtml(autor) + ' · ' + fecha(registrada))}
        <h3>Configuración ${c.versionAnterior ?? '—'} → ${c.versionNueva ?? '—'}</h3>
        ${cambios ? `<dl class="pesos-cambios">${cambios}</dl>` : ''}
        ${c.activarEl ? meta('calendario', 'Fecha prevista: ' + fecha(c.activarEl)) : ''}
        ${c.activadaEl ? meta('confirmarFecha', 'Aplicación efectiva: ' + fecha(c.activadaEl)) : ''}
        <div class="pesos-justificacion"><h4>Justificación del cambio</h4><p>${limpiarHtml(c.justificacion || 'Sin justificación registrada en este antecedente.')}</p></div>
      </article></li>`;
    }).join('');
    return `<section class="tarjeta"><details class="pro-historial-pesos"><summary>
      <span class="pro-simbolo">${this.icono('historial')}</span>
      <span><strong>Historial de ajustes</strong><small>Consulta los cambios realizados en los criterios de compatibilidad.</small></span>
      <span class="pesos-flecha" aria-hidden="true">⌄</span>
      </summary>${tarjetas ? `<ol class="pesos-linea">${tarjetas}</ol>` : '<p>No hay ajustes registrados.</p>'}</details></section>`;
  },
  async criterios(){
    const pesos=await this.api('/api/scoring/pesos');const [historial,agenda]=await Promise.all([this.api('/api/scoring/pesos/historial'),this.api('/api/scoring/pesos/programacion')]);if(pesos.version!==agenda.versionVigente)return this.criterios();
    this.programacion=agenda;
    this.contenido.innerHTML=Profesionales.encabezado('Configuración de pesos','Consulta los criterios vigentes y programa los próximos cambios.','criterios')+this.avisoProgramacion(agenda.pendiente,'completo')+`
      <section class="tarjeta pro-modulo-pesos"><h2>${this.icono('criterios')}Pesos actualmente vigentes</h2><p>Los pesos indican la importancia de cada factor. Los cambios requieren al menos 24 horas de anticipación y se activan a las 7:00 a. m., hora de Panamá.</p>
      <div class="pro-pesos-vigentes">${Object.entries(FACTORES).map(([k,f])=>`<div><span class="peso-icono peso-${k}">${this.icono(({edad:'calendario',hermanos:'personas',salud:'salud',apoyo:'personas',logistica:'ubicacion'})[k])}</span><span>${limpiarHtml(f.n)}</span><strong>${pesos[k]} puntos</strong></div>`).join('')}</div>
      ${agenda.pendiente?'<p class="ayuda">Hay una actualización pendiente. Consulta sus valores y fecha antes de realizar otro cambio.</p>':`<details class="pro-seccion"><summary>${this.icono('calendario')}Programar nuevos pesos</summary><form id="formPesos">${Object.entries(FACTORES).map(([k,f])=>`<div class="pro-ajuste-fila"><span class="pro-simbolo">${this.icono(k==='edad'?'calendario':k==='salud'?'salud':k==='logistica'?'hogar':'personas')}</span><label for="peso-${k}">${limpiarHtml(f.n)}</label><input id="peso-${k}" type="number" name="${k}" value="${pesos[k]}" min="0" max="100" step="1" required></div>`).join('')}<div class="pro-ajuste-total"><span>Suma de los criterios</span><strong id="sumaPesos">100 / 100</strong></div><p class="ayuda">Introduce números enteros entre 0 y 100 que sumen exactamente 100.</p><label for="justificacionPesos">Justificación del cambio</label><textarea id="justificacionPesos" minlength="10" maxlength="300" required rows="4"></textarea><p class="ayuda pro-ayuda-con-icono">${this.icono('informacion')}<span>La fecha se calcula en el servidor; la hora de activación siempre será las 7:00 a. m. de Panamá.</span></p><div class="pro-acciones"><button class="boton secundario" type="reset">Cancelar</button><button class="boton" type="submit">Revisar programación</button></div></form></details>`}
      <p id="pesoEstado" role="status"></p><div id="confirmacionProgramacion"></div></section>
      ${this.historialPesos(historial)}`;
    const form=document.getElementById('formPesos');if(!form)return;
    Profesionales.sugerencias(document.getElementById('justificacionPesos'),[
      ['Revisión de criterios','Se propone este ajuste tras revisar la distribución de los factores utilizados en la evaluación de compatibilidad.'],
      ['Ajuste de prioridades','Se considera necesario modificar la importancia relativa de determinados factores para mejorar la valoración de las propuestas.'],
      ['Mejora de la evaluación','Se propone actualizar la ponderación de los criterios a partir de las necesidades identificadas durante la revisión institucional.'],['Otra justificación','']]);
    const valores=()=>Object.fromEntries(Object.keys(FACTORES).map(k=>[k,Number(form.elements[k].value)]));
    const total=()=>document.getElementById('sumaPesos').textContent=Object.values(valores()).reduce((a,b)=>a+b,0)+' / 100';let revision=0;form.oninput=()=>{revision++;total();document.getElementById('confirmacionProgramacion').replaceChildren();};form.onreset=()=>{revision++;setTimeout(()=>{total();document.getElementById('confirmacionProgramacion').replaceChildren();document.getElementById('pesoEstado').replaceChildren();document.getElementById('pesoEstado').className='';},0);};
    let enviando=false;
    form.onsubmit=async e=>{
      e.preventDefault();if(enviando||!form.reportValidity())return;const propuestos=valores(),motivo=document.getElementById('justificacionPesos').value.trim(),estado=document.getElementById('pesoEstado');estado.className='';estado.textContent='';
      if(Object.values(propuestos).some(v=>!Number.isInteger(v)||v<0||v>100)||Object.values(propuestos).reduce((a,b)=>a+b,0)!==100){estado.className='mensaje error';estado.textContent='Revisa los valores: deben ser enteros que sumen exactamente 100.';return;}
      if(Object.keys(FACTORES).every(k=>propuestos[k]===pesos[k])){estado.className='mensaje error';estado.textContent='No hay cambios por programar.';return;}
      if(motivo.length<10){estado.className='mensaje error';estado.textContent='Explica el cambio con al menos 10 caracteres.';return;}
      try{
        const revisionActual=revision;const fecha=await this.api('/api/scoring/pesos/programacion');if(revisionActual!==revision)return;if(fecha.pendiente){await this.criterios();return;}
        const panel=document.getElementById('confirmacionProgramacion');panel.innerHTML=`<section class="pro-confirmacion-programacion"><h3>${this.icono('calendario')}Revisa la programación</h3><p>Los nuevos pesos entrarán en vigor el <strong>${limpiarHtml(this.fechaProgramada(fecha.primeraActivacionValida))}</strong>, hora de Panamá. Hasta entonces se mantendrán los valores actuales.</p><dl class="detalle-datos">${Object.entries(FACTORES).map(([k,f])=>`<div><dt>${limpiarHtml(f.n)}</dt><dd>${pesos[k]} → ${propuestos[k]} puntos</dd></div>`).join('')}</dl><p><strong>Justificación:</strong> ${limpiarHtml(motivo)}</p><button class="boton" data-confirmar-programacion>Confirmar programación</button></section>`;
        panel.querySelector('button').onclick=async()=>{if(enviando)return;enviando=true;form.querySelectorAll('button,input,textarea').forEach(b=>b.disabled=true);panel.querySelector('button').disabled=true;
          try{await this.api('/api/scoring/pesos',{method:'PUT',body:JSON.stringify({...propuestos,justificacion:motivo,versionEsperada:pesos.version})});await this.criterios();document.getElementById('pesoEstado').className='mensaje exito';document.getElementById('pesoEstado').textContent='¡Listo! Los nuevos criterios de compatibilidad quedaron programados. Se aplicarán automáticamente en la fecha indicada.';}
          catch(error){estado.className='mensaje error';estado.textContent=error.message;enviando=false;form.querySelectorAll('button,input,textarea').forEach(b=>b.disabled=false);panel.querySelector('button').disabled=false;}
        };panel.scrollIntoView({block:'nearest'});
      }catch(error){estado.className='mensaje error';estado.textContent=error.message;}
    };
  }
};
