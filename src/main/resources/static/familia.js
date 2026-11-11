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
   if(r.status===404)return location.replace('perfil.html');
   if(!r.ok)throw new Error('No se pudo consultar tu proceso. Inténtalo de nuevo.');
   const p=await r.json();let version=JSON.stringify([vista,p]);
   // Evita redibujar información idéntica y conserva los desplegables abiertos durante el refresco.
   if(version===this.version && vista!=='entrevista')return;
   let entrevista='';
   if(vista==='entrevista'){
    const d=await fetch('/api/familias/mi-perfil/detalle',{headers:Sesion.encabezados(),cache:'no-store'});
    if(!d.ok)throw new Error('No se pudo consultar tu entrevista.');
    const datos=await d.json();entrevista=this.entrevista(datos,p);
    version=JSON.stringify([vista,p,datos]);
    if(version===this.version)return;
   }
   this.version=version;
   const eventos=[...(p.actividad||[])].reverse(),ultimo=eventos[0];
   const estado=p.vinculacionAceptada?'Propuesta aceptada':p.reevaluacionPendiente?'Entrevista en revisión':p.perfilCompleto?'Disponible para ser considerada':'Entrevista incompleta';
   const accion=p.vinculacionAceptada?'Espera la orientación del equipo SENNIAF sobre los siguientes pasos.':p.reevaluacionPendiente||!p.perfilCompleto?'Revisa y completa tu entrevista.':'Espera la revisión de propuestas por el Comité.';
   const etapas=['Entrevista enviada','Perfil completo','Disponible para ser considerada','Revisión del Comité','Propuesta de vinculación'];
   const actual=p.vinculacionAceptada?4:p.reevaluacionPendiente||!p.perfilCompleto?1:2;
   const proceso=`<section class="tarjeta"><div class="pro-fila-titulo"><h2>${Profesionales.icono('solicitudes')} Estado del proceso de adopción</h2><span class="chip">${limpiarHtml(estado)}</span></div><ol class="familia-etapas">${etapas.map((t,i)=>`<li class="${i<actual?'completado':i===actual?'actual':'pendiente'}" ${i===actual?'aria-current="step"':''}><span class="familia-paso">${i<actual?'✓':i+1}</span><strong>${t}</strong><small>${i<actual?'Completado':i===actual?'Etapa actual':'Pendiente'}</small></li>`).join('')}</ol>${p.reevaluacionPendiente||!p.perfilCompleto?'<a class="boton" href="perfil.html?reevaluacion=1">Revisar entrevista</a>':''}</section>`;
   const pasos=p.proximosPasos?.length?p.proximosPasos:[accion];
   const fechaComunicada=p.fechaActualizacionComunicada||ultimo?.fecha;
   const propuesta=`<section class="tarjeta"><div class="pro-fila-titulo"><h2>${Profesionales.icono('personas')} Propuesta de vinculación</h2><span class="chip">${limpiarHtml(estado)}</span></div><p><strong>Tu expediente:</strong> ${limpiarHtml(p.codigoCaso)}</p><p>${p.vinculacionAceptada?'El Comité registró una propuesta aceptada para continuar el proceso de vinculación de tu familia.':'Todavía no existe una propuesta aceptada para tu familia. El equipo comunicará aquí las actualizaciones autorizadas.'}</p><p><strong>Última actualización comunicada:</strong> ${fechaComunicada?`<time>${limpiarHtml(Profesionales.fecha(fechaComunicada))} · Panamá</time>`:'Fecha no disponible'}</p><h3>Próximos pasos</h3><ul>${pasos.map(t=>`<li>${limpiarHtml(t)}</li>`).join('')}</ul>${p.vinculacionAceptada?`<p class="familia-nota-propuesta">${Profesionales.icono('informacion')}<span>Propuesta aceptada. El proceso de vinculación puede continuar; la adopción aún no está formalizada.</span></p>`:''}</section>`;
   const actividad=(todos=false)=>`<section class="tarjeta"><div class="pro-fila-titulo"><div><h2>${Profesionales.icono('reloj')} ${todos?'Actualizaciones registradas':'Actividad reciente'}</h2>${todos?'':'<p>Últimas actualizaciones registradas en tu proceso.</p>'}</div>${todos?'':'<a class="boton secundario chico" href="match.html?vista=historial">Ver historial completo →</a>'}</div><ul class="familia-actividad">${eventos.length?eventos.slice(0,todos?eventos.length:3).map(e=>`<li><strong>${limpiarHtml(e.titulo)}</strong><time>${limpiarHtml(Profesionales.fecha(e.fecha))} · Panamá</time><p>${limpiarHtml(e.descripcion)}</p></li>`).join(''):'<li>Todavía no hay actuaciones disponibles.</li>'}</ul>${todos&&p.numeroReevaluaciones?`<p class="ayuda">Reevaluaciones completadas: ${Number(p.numeroReevaluaciones)}. Solo se muestran fechas que quedaron registradas.</p>`:''}</section>`;
   const resumen=`<div class="familia-resumen">${[['carpeta','Estado actual',estado],['calendario','Última actualización',ultimo?Profesionales.fecha(ultimo.fecha):'Sin actualizaciones registradas'],['comprobar','Próxima acción',accion]].map(([i,t,v])=>`<section class="tarjeta"><span class="pro-simbolo">${Profesionales.icono(i)}</span><h3>${t}</h3><p>${limpiarHtml(v)}</p></section>`).join('')}</div>`;
   const pantallas={inicio:['Bienvenida, familia','Consulta el estado de tu proceso de adopción, las etapas y las actualizaciones registradas.','hogar'],entrevista:['Mi entrevista','Consulta las respuestas de tu hogar y actualízalas cuando cambie tu situación.','informe'],propuesta:['Propuesta de vinculación','Consulta el resultado comunicado a tu familia y los siguientes pasos.','personas'],historial:['Historial de mi proceso','Consulta las actualizaciones registradas durante tu proceso de adopción','reloj']};
   const [titulo,descripcion,icono]=pantallas[vista];
   document.getElementById('contenido').innerHTML=Profesionales.encabezado(titulo,descripcion,icono)+(vista==='entrevista'?entrevista:vista==='historial'?actividad(true):vista==='propuesta'?propuesta:proceso+resumen+actividad());
  }catch(e){this.version=null;document.getElementById('contenido').innerHTML=`<section class="tarjeta"><p role="alert">${limpiarHtml(e.message)}</p></section>`;}
  finally{this.cargando=false;}
 },
 entrevista(d,p){
  const grupos={ 'Hogar y red de apoyo':['tipoFamilia','especificacionHogar','notaEstructuraHogar','redApoyo','notaRedApoyo'], 'Preferencias familiares':['edadMinimaAceptada','edadMaximaAceptada','notaRangoEdad','disposicionHermanos','notaHermanos'], 'Salud y acompañamiento':['condicionesPuedeAcompanarHoy','condicionesConApoyo','notaCondicionesSalud'], 'Disponibilidad y entorno':['provincia','disponibilidadViaje','notaDistancia'] };
  const etiquetas={tipoFamilia:'Composición del hogar',especificacionHogar:'Descripción de otra estructura del hogar',notaEstructuraHogar:'Características familiares',redApoyo:'Red de apoyo',notaRedApoyo:'Organización del apoyo',edadMinimaAceptada:'Edad mínima',edadMaximaAceptada:'Edad máxima',notaRangoEdad:'Acompañamiento según la edad',disposicionHermanos:'Disposición ante hermanos',notaHermanos:'Vínculos entre hermanos',condicionesPuedeAcompanarHoy:'Capacidad actual',condicionesConApoyo:'Con apoyo adicional',notaCondicionesSalud:'Cuidados y acompañamiento',provincia:'Provincia',disponibilidadViaje:'Disponibilidad de viaje',notaDistancia:'Desplazamientos',continuidadCultural:'Continuidad cultural'};
  const iconos=['personas','estrella','salud','hogar'];
  return `<section class="tarjeta familia-entrevista-resumen"><div><span class="pro-capsula">Entrevista registrada</span><h2>${limpiarHtml(p.codigoCaso)}</h2><p>${p.reevaluacionPendiente?'El Comité solicitó revisar tu entrevista. Actualiza las respuestas necesarias.':'Puedes actualizar tu entrevista cuando cambie la situación de tu hogar.'}</p></div><a class="boton" href="perfil.html?reevaluacion=1">${Profesionales.icono('informe')}Editar entrevista</a></section>`+Object.entries(grupos).map(([titulo,campos],i)=>`<details class="familia-entrevista-seccion"><summary><span class="pro-simbolo">${Profesionales.icono(iconos[i])}</span><span>${titulo}</span><span class="familia-desplegar" aria-hidden="true">⌄</span></summary><dl class="detalle-datos">${campos.filter(k=>k!=='especificacionHogar'||d.tipoFamilia==='OTRA').map(k=>`<div><dt>${etiquetas[k]}</dt><dd>${limpiarHtml(d[k]==null||d[k]===''?'Sin información registrada':Array.isArray(d[k])?(d[k].length?d[k].map(v=>Profesionales.etiqueta(v)).join(', '):'Sin condiciones específicas declaradas'):typeof d[k]==='number'?d[k]:/^[A-Z_]+$/.test(d[k])?Profesionales.etiqueta(d[k]):d[k])}</dd></div>`).join('')}</dl></details>`).join('');
 }
};
