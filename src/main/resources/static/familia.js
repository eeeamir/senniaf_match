/* Presentación familiar: reutiliza la entrevista y expone únicamente actividad autorizada del caso propio. */
const Familia = {
 async mostrar(sesion) {
  if(this.cargando)return;this.cargando=true;
  const solicitada=new URLSearchParams(location.search).get('vista');
  const vista=['entrevista','propuesta','historial'].includes(solicitada)?solicitada:'inicio';
  Profesionales.iniciar(sesion,vista);
  try {
   const r=await fetch('/api/familias/mi-perfil',{headers:Sesion.encabezados(),cache:'no-store'});
   if(r.status===401)return Sesion.salir();
   if(!r.ok && r.status!==404)throw new Error('No se pudo consultar tu proceso. Inténtalo de nuevo.');
   const sinEntrevista=r.status===404;
   if(sinEntrevista && vista==='entrevista')return location.replace('perfil.html');
   const vinculaciones=await Vinculaciones.api('/api/vinculaciones/propias');
   const pendiente=vinculaciones.find(v=>v.estado==='PENDIENTE_RESPUESTA');
   const p=sinEntrevista?{perfilCompleto:false,actividad:[]}:await r.json();
   const enlaceEntrevista=sinEntrevista?'perfil.html':'perfil.html?reevaluacion=1';
   let version=JSON.stringify([vista,p,vinculaciones]);
   // Evita redibujar información idéntica y conserva los desplegables abiertos durante el refresco.
   if(version===this.version && vista!=='entrevista')return;
   let entrevista='';
   if(vista==='entrevista'){
    const d=await fetch('/api/familias/mi-perfil/detalle',{headers:Sesion.encabezados(),cache:'no-store'});
    if(d.status===401)return Sesion.salir();
    if(!d.ok)throw new Error('No se pudo consultar tu entrevista.');
    const datos=await d.json();entrevista=this.entrevista(datos,p);
    version=JSON.stringify([vista,p,datos,vinculaciones]);
    if(version===this.version)return;
   }
   this.version=version;
   const eventos=[...(p.actividad||[]),...vinculaciones.filter(v=>v.fechaRespuesta).map(v=>({titulo:Vinculaciones.estados[v.estado],descripcion:v.siguientePaso,fecha:v.fechaRespuesta}))].sort((a,b)=>new Date(b.fecha)-new Date(a.fecha)),ultimo=eventos[0];
   const estado=vinculaciones.length?Vinculaciones.estados[vinculaciones[0].estado]:p.vinculacionAceptada?'Propuesta enviada por el Comité':p.reevaluacionPendiente?'Entrevista en revisión':p.perfilCompleto?'Disponible para ser considerada':'Entrevista incompleta';
   const accion=vinculaciones.length?vinculaciones[0].siguientePaso:sinEntrevista?'Completa tu entrevista para iniciar el proceso.':p.vinculacionAceptada?'Espera la orientación del equipo SENNIAF sobre los siguientes pasos.':p.reevaluacionPendiente||!p.perfilCompleto?'Revisa y completa tu entrevista.':'Espera la revisión de propuestas por el Comité.';
   const etapas=['Entrevista enviada','Perfil completo','Disponible para ser considerada','Revisión del Comité','Mi vinculación'];
   const actual=sinEntrevista?0:p.vinculacionAceptada?4:p.reevaluacionPendiente||!p.perfilCompleto?1:2;
   const proceso=`<section class="tarjeta"><div class="pro-fila-titulo"><h2>${Profesionales.icono('solicitudes')} Estado del proceso de adopción</h2><span class="chip">${limpiarHtml(estado)}</span></div><ol class="familia-etapas">${etapas.map((t,i)=>`<li class="${i<actual?'completado':i===actual?'actual':'pendiente'}" ${i===actual?'aria-current="step"':''}><span class="familia-paso">${i<actual?Profesionales.icono('comprobar'):i+1}</span><strong>${t}</strong><small>${i<actual?'Completado':i===actual?'Etapa actual':'Pendiente'}</small></li>`).join('')}</ol>${p.reevaluacionPendiente||!p.perfilCompleto?`<a class="boton" href="${enlaceEntrevista}">${p.perfilCompleto?'Revisar entrevista':'Completar mi entrevista'}</a>`:''}</section>`;
   const propuesta=Vinculaciones.familiar(vinculaciones);
   const actividad=(todos=false)=>todos&&!eventos.length?`<section class="tarjeta"><div class="pro-modal-cabecera"><span class="pro-simbolo">${Profesionales.icono('reloj')}</span><h2>Todavía no hay actualizaciones.</h2></div><p>Aquí podrás consultar los cambios y las comunicaciones relacionadas con tu proceso cuando estén disponibles.</p>${p.numeroReevaluaciones?`<p class="ayuda">Reevaluaciones completadas: ${Number(p.numeroReevaluaciones)}. Solo se muestran fechas que quedaron registradas.</p>`:''}</section>`:`<section class="tarjeta"><div class="pro-fila-titulo"><div><h2>${Profesionales.icono('reloj')} ${todos?'Actualizaciones registradas':'Actividad reciente'}</h2>${todos?'':'<p>Últimas actualizaciones registradas en tu proceso.</p>'}</div>${todos?'':'<a class="boton secundario chico" href="match.html?vista=historial">Ver historial completo →</a>'}</div><ul class="familia-actividad">${eventos.length?eventos.slice(0,todos?eventos.length:3).map(e=>`<li><strong>${limpiarHtml(e.titulo)}</strong><time>${limpiarHtml(Profesionales.fecha(e.fecha))} · Panamá</time><p>${limpiarHtml(e.descripcion)}</p></li>`).join(''):'<li>Todavía no hay actuaciones disponibles.</li>'}</ul>${todos&&p.numeroReevaluaciones?`<p class="ayuda">Reevaluaciones completadas: ${Number(p.numeroReevaluaciones)}. Solo se muestran fechas que quedaron registradas.</p>`:''}</section>`;
   const resumen=`<div class="familia-resumen">${[['carpeta','Estado actual',estado],['calendario','Última actualización',ultimo?Profesionales.fecha(ultimo.fecha):'Sin actualizaciones registradas'],['comprobar','Próxima acción',accion]].map(([i,t,v])=>`<section class="tarjeta"><span class="pro-simbolo">${Profesionales.icono(i)}</span><h3>${t}</h3><p>${limpiarHtml(v)}</p></section>`).join('')}</div>`;
   const pantallas={inicio:['Bienvenida, familia','Consulta el estado de tu proceso de adopción, las etapas y las actualizaciones registradas.','hogar'],entrevista:['Mi entrevista','Consulta las respuestas de tu hogar y actualízalas cuando cambie tu situación.','informe'],propuesta:['Mi vinculación','Consulta el resultado comunicado a tu familia y los siguientes pasos.','vinculacion'],historial:['Historial de mi proceso','Consulta las actualizaciones registradas durante tu proceso de adopción','reloj']};
   const [titulo,descripcion,icono]=pantallas[vista];
   document.getElementById('contenido').innerHTML=Profesionales.encabezado(titulo,descripcion,icono)+(pendiente&&vista!=='propuesta'?`<section class="tarjeta" role="status">${Profesionales.icono('informacion')} Tienes una propuesta pendiente de respuesta. <a href="match.html?vista=propuesta">Consultar mi vinculación</a></section>`:'')+(vista==='entrevista'?entrevista:vista==='historial'?actividad(true):vista==='propuesta'?propuesta:proceso+resumen+actividad());
   Vinculaciones.enlazarRespuestas(sesion);
  }catch(e){this.version=null;document.getElementById('contenido').innerHTML=`<section class="tarjeta"><p role="alert">${limpiarHtml(e.message)}</p></section>`;}
  finally{this.cargando=false;}
 },
 entrevista(d,p){
  const grupos={ 'Hogar y red de apoyo':['tipoFamilia','especificacionHogar','notaEstructuraHogar','redApoyo','notaRedApoyo'], 'Preferencias familiares':['edadMinimaAceptada','edadMaximaAceptada','notaRangoEdad','disposicionHermanos','notaHermanos'], 'Salud y acompañamiento':['condicionesPuedeAcompanarHoy','condicionesConApoyo','notaCondicionesSalud'], 'Disponibilidad y entorno':['provincia','disponibilidadViaje','notaDistancia'], 'Apoyos culturales y comunitarios':['continuidadCultural'] };
  const etiquetas={tipoFamilia:'Composición del hogar',especificacionHogar:'Descripción de otra estructura del hogar',notaEstructuraHogar:'Características familiares',redApoyo:'Red de apoyo',notaRedApoyo:'Organización del apoyo',edadMinimaAceptada:'Edad mínima',edadMaximaAceptada:'Edad máxima',notaRangoEdad:'Acompañamiento según la edad',disposicionHermanos:'Disposición ante hermanos',notaHermanos:'Vínculos entre hermanos',condicionesPuedeAcompanarHoy:'Capacidad actual',condicionesConApoyo:'Con apoyo adicional',notaCondicionesSalud:'Cuidados y acompañamiento',provincia:'Provincia',disponibilidadViaje:'Disponibilidad de viaje',notaDistancia:'Desplazamientos',continuidadCultural:'Apoyos culturales, lingüísticos y comunitarios'};
  const iconos=['personas','estrella','salud','hogar','personas'];
  return `<section class="tarjeta familia-entrevista-resumen"><div><span class="pro-capsula">${p.perfilCompleto?'Entrevista registrada':'Entrevista incompleta'}</span><h2>${limpiarHtml(p.codigoCaso)}</h2><p>${p.reevaluacionPendiente?'El Comité solicitó revisar tu entrevista. Actualiza las respuestas necesarias.':'Puedes actualizar tu entrevista cuando cambie la situación de tu hogar.'}</p></div><a class="boton" href="perfil.html?reevaluacion=1">${Profesionales.icono('informe')}${p.perfilCompleto?'Editar entrevista':'Completar mi entrevista'}</a></section>`+Object.entries(grupos).map(([titulo,campos],i)=>`<details class="familia-entrevista-seccion ${campos.length===1?'pro-detalle-amplio':''}"><summary><span class="pro-simbolo">${Profesionales.icono(iconos[i])}</span><span>${titulo}</span><span class="familia-desplegar" aria-hidden="true">⌄</span></summary><dl class="detalle-datos">${campos.filter(k=>k!=='especificacionHogar'||d.tipoFamilia==='OTRA').map(k=>`<div><dt>${etiquetas[k]}</dt><dd>${limpiarHtml(d[k]==null||d[k]===''?'Sin información registrada':Array.isArray(d[k])?(d[k].length?d[k].map(v=>Profesionales.etiqueta(v)).join(', '):'Sin condiciones específicas declaradas'):typeof d[k]==='number'?d[k]:/^[A-Z_]+$/.test(d[k])?Profesionales.etiqueta(d[k]):d[k])}</dd></div>`).join('')}</dl></details>`).join('');
 }
};
