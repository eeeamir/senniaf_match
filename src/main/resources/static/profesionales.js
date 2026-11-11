/* Componentes compartidos por las interfaces institucionales. Los permisos se verifican en el servidor. */
const Profesionales = {
  icono(nombre) {
    const trazos = {
      carpeta:'M3 7h6l2 2h10v11H3z M3 7V4h6l2 3', nuevo:'M12 5v14M5 12h14',
      inicio:'m3 11 9-8 9 8 M5 10v11h5v-7h4v7h5V10',
      solicitudes:'M5 4h14v17H5z M9 2h6v4H9z M8 11h8M8 15h6',
      salir:'M9 4H4v16h5 M13 7l5 5-5 5M8 12h10', menu:'M4 6h16M4 12h16M4 18h16',
      salud:'M12 21S2 15 2 8a5 5 0 0 1 10-2A5 5 0 0 1 22 8c0 7-10 13-10 13z',
      estrella:'m12 3 3 6 6 1-4 5 1 6-6-3-6 3 1-6-4-5 6-1z',
      hogar:'m3 11 9-8 9 8 M5 10v11h14V10M10 21v-7h4v7',
      informe:'M6 3h9l4 4v14H6z M14 3v5h5M9 12h7M9 16h7',
      reloj:'M12 7v5l3 2 M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0',
      buscar:'M16 16l5 5 M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
      personas:'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M18 4a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-4',
      psicologia:'M12 3v18 M12 5C7-1 3 5 5 8c-5 1-4 7 0 8-1 5 5 7 7 3 M12 5c5-6 9 0 7 3 5 1 4 7 0 8 1 5-5 7-7 3',
      escuela:'m2 9 10-5 10 5-10 5z M6 12v5c4 3 8 3 12 0v-5 M22 9v7',
      calendario:'M5 5h14v16H5z M8 2v6M16 2v6M5 10h14', flecha:'M4 12h16M15 7l5 5-5 5',
      comprobar:'m5 12 4 4L19 6', cerrar:'m6 6 12 12M6 18 12-12', criterios:'M4 6h16v15H4z M9 6V3h6v3',
      informacion:'M12 11v6 M12 7h.01 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
      credencial:'M4 5h16v16H4z M9 5V2h6v3 M10 11a2 2 0 1 0-4 0 2 2 0 0 0 4 0 M5 17c0-4 6-4 6 0 M14 10h3M14 14h3',
      persona:'M5 21v-3a7 7 0 0 1 14 0v3 M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0'
    };
    return `<svg class="pro-icono" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${trazos[nombre] || trazos.informe}"/></svg>`;
  },
  fecha(valor) {
    if (!valor) return 'Fecha no registrada';
    const fecha = new Date(valor);
    return Number.isNaN(fecha.getTime()) ? 'Fecha no disponible' : new Intl.DateTimeFormat('es-PA',{dateStyle:'medium',timeStyle:'short',timeZone:'America/Panama'}).format(fecha);
  },
  momento(valor) {
    if(!valor)return {fecha:'Fecha no registrada',hora:'Hora no registrada'};
    const d=new Date(valor);if(Number.isNaN(d.getTime()))return {fecha:'Fecha no disponible',hora:'Hora no disponible'};
    return {fecha:new Intl.DateTimeFormat('es-PA',{dateStyle:'medium',timeZone:'America/Panama'}).format(d),hora:new Intl.DateTimeFormat('es-PA',{timeStyle:'short',timeZone:'America/Panama'}).format(d)};
  },
  informe(e,mostrarMetadatos=true) {
    const m=this.momento(e.fecha),s=Sesion.leer();
    const seccion=(icono,titulo,valor)=>`<div class="pro-informe-bloque"><h4>${this.icono(icono)}${titulo}</h4><p>${limpiarHtml(valor||'Sin información registrada')}</p></div>`;
    return `<article class="pro-informe"><div class="pro-informe-cabecera"><h3>${this.icono('psicologia')} ${e.vigente?'Evaluación actual':'Evaluación anterior'}</h3><small>Versión ${e.version}</small><span class="chip ${e.vigente?'':'retirado'}">${e.vigente?'Vigente':'Versión anterior'}</span></div>${mostrarMetadatos?`<div class="pro-informe-meta"><span>${this.icono('persona')}${limpiarHtml(e.profesional)}</span><span>${this.icono('calendario')}${limpiarHtml(m.fecha)}</span><span>${this.icono('reloj')}${limpiarHtml(m.hora)}</span></div>`:''}<div class="pro-informe-bloques">${seccion('psicologia','Evaluación',e.evaluacion)}${seccion('informe','Observaciones',e.observaciones)}${seccion('salud','Recomendaciones profesionales',e.recomendaciones)}${e.areasObservadas?.length?seccion('comprobar','Áreas evaluadas',e.areasObservadas.map(a=>this.etiqueta(a)).join(', ')):''}${e.sinDiagnostico===true?seccion('informe','Diagnóstico','No se establece diagnóstico'):''}${e.hallazgosProfesionales?seccion('informe','Valoración profesional',e.hallazgosProfesionales):''}${e.diagnosticoProfesional?seccion('informe','Diagnóstico profesional',e.diagnosticoProfesional):''}${e.contextoEvaluacion&&typeof ContextoPsicologico!=='undefined'?ContextoPsicologico.informe(e.contextoEvaluacion):''}</div>${s?.rol==='PSICOLOGO'&&s.id===e.profesional&&e.vigente?`<div class="pro-acciones"><button class="boton secundario chico" type="button" data-corregir="${e.id}">Editar evaluación</button></div>`:''}</article>`;
  },
  etiqueta(valor) {
    if (valor == null || valor === '') return 'Sin información';
    const etiquetas={NO_APLICA:'No aplica',NO_EVALUADO:'No evaluado',AUN_NO_EVALUADO:'Aún no evaluado',INFORMACION_INSUFICIENTE:'Información insuficiente',INFORMACION_NO_DISPONIBLE:'Información no disponible',NINGUNA_DE_LAS_ANTERIORES:'Ninguna de las anteriores',SI:'Sí',NO:'No',NO_SABE:'No se sabe',NINGUNA_CONOCIDA:'Ninguna conocida',NINGUNA_IDENTIFICADA:'Ninguna identificada',NINGUNO_IDENTIFICADO:'Ninguno identificado',TEA:'TEA (autismo)',TDAH:'TDAH',DESACTIVACION:'Desactivación',REACTIVACION:'Reactivación',PENDIENTE:'Pendiente',APROBADA:'Aprobada',RECHAZADA:'Rechazada',ACEPTADA:'Propuesta aceptada',DESCARTADA:'Propuesta descartada',REEVALUACION:'Reevaluación solicitada'};
    return etiquetas[valor] || String(valor).toLocaleLowerCase('es').replaceAll('_',' ').replace(/^./,c=>c.toLocaleUpperCase('es'));
  },
  edad(p) {
    if(p.edad==null)return 'Sin información';
    if(p.unidadEdad!=='MESES'&&Number(p.edad)===0)return 'Menor de un año (meses no registrados)';
    const unidad=p.unidadEdad==='MESES'?(Number(p.edad)===1?'mes':'meses'):(Number(p.edad)===1?'año':'años');
    return `${p.edad} ${unidad}`;
  },
  estado(p) { return p.cierrePorAdopcion?'Cierre por adopción':p.bajaLogica?'Fuera del Match':p.perfilCompleto?'Disponible para análisis':'Pendiente de información'; },
  encabezado(titulo,texto,icono='carpeta') {
    return `<section class="hero"><h1>${limpiarHtml(titulo)}</h1><p>${limpiarHtml(texto)}</p><span class="pro-hero-icono" aria-hidden="true">${this.icono(icono)}</span></section>`;
  },
  decorarHero(icono='carpeta') {
    const h=document.querySelector('.hero');if(h&&!h.querySelector('.pro-hero-icono'))h.insertAdjacentHTML('beforeend',`<span class="pro-hero-icono" aria-hidden="true">${this.icono(icono)}</span>`);
  },
  iniciar(s,seleccionado='expedientes') {
    if(!['TRABAJADOR_SOCIAL','PSICOLOGO','COMITE','FAMILIA'].includes(s?.rol)||document.getElementById('navegacionProfesional'))return;
    document.body.classList.add('profesional');
    const opciones=s.rol==='FAMILIA'
      ? [['inicio','match.html','inicio','Inicio'],['entrevista','match.html?vista=entrevista','informe','Mi entrevista'],['propuesta','match.html?vista=propuesta','personas','Propuesta de vinculación'],['historial','match.html?vista=historial','reloj','Historial']]
      : s.rol==='COMITE'
      ? [['inicio','match.html','inicio','Inicio'],['compatibilidad','match.html?vista=compatibilidad','salud','Compatibilidad'],['solicitudes','match.html?vista=solicitudes','solicitudes','Solicitudes'],['criterios','match.html?vista=criterios','criterios','Criterios']]
      : [['inicio','funcionario.html','inicio','Inicio'],['expedientes','perfiles.html','carpeta','Mis expedientes'],...(s.rol==='TRABAJADOR_SOCIAL'?[['registro','perfil.html','nuevo','Registrar perfil']]:[])];
    const nav=document.createElement('aside');nav.className='pro-sidebar';nav.id='navegacionProfesional';
    nav.innerHTML=`<nav aria-label="Navegación principal">${opciones.map(([clave,href,icono,texto])=>`<a href="${href}" ${seleccionado===clave?'aria-current="page"':''}>${this.icono(icono)}<span>${texto}</span></a>`).join('')}</nav><div class="pro-sidebar-pie"><button type="button" id="proSalir">${this.icono('salir')}Cerrar sesión</button></div>`;
    document.body.prepend(nav);
    const marca=document.querySelector('.topbar-marca');marca.href=['COMITE','FAMILIA'].includes(s.rol)?'match.html':'funcionario.html';marca.querySelector('img').alt='SENNIAF Match';
    const rol=s.rol==='FAMILIA'?'Familia adoptante':s.rol==='COMITE'?'Comité de asignación familiar':ROLES[s.rol];
    document.getElementById('usuario').innerHTML=`<span class="chip">${this.icono('persona')}${limpiarHtml(rol)}</span>`;
    const abrir=document.createElement('button');abrir.type='button';abrir.className='pro-menu';abrir.setAttribute('aria-label','Abrir navegación');abrir.setAttribute('aria-controls',nav.id);abrir.setAttribute('aria-expanded','false');abrir.innerHTML=this.icono('menu');
    document.querySelector('.topbar-in').prepend(abrir);
    abrir.addEventListener('click',()=>{const activo=document.body.classList.toggle('pro-nav-abierta');abrir.setAttribute('aria-expanded',String(activo));});
    document.addEventListener('keydown',e=>{if(e.key==='Escape'){document.body.classList.remove('pro-nav-abierta');abrir.setAttribute('aria-expanded','false');}});
    const dialogo=document.createElement('dialog');dialogo.className='pro-dialogo';dialogo.setAttribute('aria-labelledby','tituloSalir');
    dialogo.innerHTML=`<div class="pro-modal-cabecera"><span class="pro-simbolo">${this.icono('salir')}</span><h2 id="tituloSalir">Cerrar sesión</h2></div><p>¿Estás seguro de que deseas cerrar sesión?</p><p class="ayuda">Podrás volver a ingresar cuando lo necesites.</p><p id="avisoSalidaSinGuardar" class="pro-aviso-sin-guardar" hidden></p><div class="pro-acciones"><button type="button" class="boton secundario" id="proCancelarSalida">Cancelar</button><button type="button" class="boton" id="proConfirmarSalida">${this.icono('salir')}Cerrar sesión</button></div>`;
    document.body.append(dialogo);
    this.prepararProteccionFormularios();
    document.querySelectorAll('form').forEach(form=>CamposFormulario.iniciar(form));
    document.getElementById('proSalir').addEventListener('click',()=>{
      const aviso=dialogo.querySelector('#avisoSalidaSinGuardar'),pendientes=this.formulariosPendientes();
      aviso.hidden=!pendientes.length;
      aviso.textContent=pendientes.length?'Tienes cambios sin guardar en un formulario. Si cierras sesión, esos cambios no se guardarán. La información ya guardada se conservará.':'';
      dialogo.showModal();
    });
    document.getElementById('proCancelarSalida').addEventListener('click',()=>dialogo.close());
    document.getElementById('proConfirmarSalida').addEventListener('click',async()=>{const boton=document.getElementById('proConfirmarSalida');boton.disabled=true;await Sesion.salir();});
    this.decorarHero(s.rol==='PSICOLOGO'?'psicologia':'carpeta');
    const aviso=document.createElement('span');aviso.className='pro-demostracion';aviso.textContent='Datos de demostración';document.getElementById('usuario').prepend(aviso);
  },
  mensaje(elemento,texto,tipo='aviso'){
    elemento.className='mensaje '+tipo;elemento.setAttribute('role',tipo==='error'?'alert':'status');
    elemento.innerHTML=texto?this.icono(tipo==='exito'?'comprobar':tipo==='error'?'cerrar':'informe')+`<span>${limpiarHtml(texto)}</span>`:'';
  },
  // Solo se advierte por diferencias reales respecto del formulario cargado o guardado.
  formulariosBase:new WeakMap(),
  valorFormulario(form){return JSON.stringify([...form.elements].filter(c=>c.matches('input,select,textarea')&&!['submit','button','hidden'].includes(c.type)).map(c=>[c.id||c.name,c.type==='checkbox'||c.type==='radio'?c.checked:c.value]));},
  marcarGuardado(form){if(form)this.formulariosBase.set(form,this.valorFormulario(form));},
  prepararProteccionFormularios(){
    document.addEventListener('focusin',e=>{const f=e.target.form;if(f&&!this.formulariosBase.has(f))this.marcarGuardado(f);});
  },
  formulariosPendientes(){return [...document.forms].filter(f=>f.getClientRects().length&&!f.closest('[hidden]')&&this.formulariosBase.has(f)&&this.formulariosBase.get(f)!==this.valorFormulario(f));},
  confirmar(titulo,texto,accion) {
    const d=document.createElement('dialog');d.className='pro-dialogo';d.setAttribute('aria-labelledby','tituloConfirmacion');
    d.innerHTML=`<h2 id="tituloConfirmacion">${limpiarHtml(titulo)}</h2><p>${limpiarHtml(texto)}</p><div class="pro-acciones"><button type="button" class="boton secundario" data-cancelar>Cancelar</button><button type="button" class="boton" data-confirmar>Continuar</button></div>`;
    document.body.append(d);d.addEventListener('close',()=>d.remove(),{once:true});d.querySelector('[data-cancelar]').addEventListener('click',()=>d.close());d.querySelector('[data-confirmar]').addEventListener('click',()=>{d.close();accion();});d.showModal();
  },
  justificar({titulo,contexto,opciones,confirmacion,alConfirmar,alGuardar,instruccion='Selecciona un motivo',textoRevisar='Revisar y confirmar',tituloConfirmacion='Confirmar acción',textoConfirmar='Confirmar',ayuda='Puedes editar completamente la justificación. Para «Otro», escribe tu propia explicación.'}) {
    const d=document.createElement('dialog');d.className='pro-dialogo pro-dialogo-decision';d.setAttribute('aria-label',titulo);
    d.innerHTML=`<div class="pro-modal-cabecera"><span class="pro-simbolo">${this.icono('solicitudes')}</span><h2>${limpiarHtml(titulo)}</h2></div><p class="pro-modal-contexto">${limpiarHtml(contexto)}</p><form><fieldset class="pro-motivos"><legend>${limpiarHtml(instruccion)}</legend>${opciones.map(([valor,nombre])=>`<label class="pro-motivo"><input type="radio" name="motivoDecision" value="${limpiarHtml(valor)}" required><span>${limpiarHtml(nombre)}</span></label>`).join('')}</fieldset><label>Justificación<textarea name="justificacion" required minlength="10" maxlength="1850" rows="5" placeholder="Selecciona un motivo para ver la sugerencia editable."></textarea></label><p class="ayuda">${limpiarHtml(ayuda)}</p><p data-error role="alert"></p><div class="pro-acciones"><button class="boton secundario" type="button" data-cancelar>Cancelar</button><button class="boton" type="submit">${limpiarHtml(textoRevisar)}</button></div></form>`;
    document.body.append(d);
    const form=d.querySelector('form'),texto=d.querySelector('textarea'),boton=d.querySelector('[type=submit]');
    CamposFormulario.iniciar(form);
    let guardando=false;
    d.querySelector('[data-cancelar]').addEventListener('click',()=>d.close());
    d.addEventListener('cancel',e=>{if(guardando)e.preventDefault();});
    d.addEventListener('close',()=>d.remove(),{once:true});
    form.addEventListener('change',e=>{if(e.target.name==='motivoDecision'){texto.value=opciones.find(o=>o[0]===e.target.value)?.[2]||'';texto.setCustomValidity('');CamposFormulario.error(texto,'');texto.placeholder=e.target.value==='OTRO'?'Explica tu decisión con tus propias palabras.':'Puedes modificar esta sugerencia.';}});
    texto.addEventListener('input',()=>texto.setCustomValidity(''));
    form.addEventListener('submit',e=>{
      e.preventDefault();if(guardando)return;
      texto.setCustomValidity(texto.value.trim().length<10?'Escribe una justificación de al menos 10 caracteres.':'');
      if(!form.reportValidity())return;
      const motivo=opciones.find(o=>o[0]===form.elements.motivoDecision.value);
      const justificacion=`${motivo[1].replace(/\.$/,'')}: ${texto.value.trim()}`;
      const confirmar=document.createElement('dialog');confirmar.className='pro-dialogo';confirmar.setAttribute('aria-label',tituloConfirmacion);
      confirmar.innerHTML=`<h2>${limpiarHtml(tituloConfirmacion)}</h2><p>${limpiarHtml(confirmacion)}</p><div class="pro-acciones"><button class="boton secundario" type="button" data-volver>Volver</button><button class="boton" type="button" data-confirmar>${limpiarHtml(textoConfirmar)}</button></div>`;
      document.body.append(confirmar);boton.disabled=true;
      confirmar.addEventListener('close',()=>{confirmar.remove();if(!guardando)boton.disabled=false;},{once:true});
      confirmar.querySelector('[data-volver]').onclick=()=>confirmar.close();
      confirmar.querySelector('[data-confirmar]').onclick=async()=>{
        guardando=true;confirmar.close();boton.disabled=true;d.querySelector('[data-cancelar]').disabled=true;
        let resultado;
        try{resultado=await alConfirmar(justificacion);}
        catch(error){d.querySelector('[data-error]').textContent=error.message;return;}
        finally{guardando=false;boton.disabled=false;d.querySelector('[data-cancelar]').disabled=false;}
        d.close();if(alGuardar)await alGuardar(resultado);
      };
      confirmar.showModal();
    });
    d.showModal();
  },
  errorCampo(campo,mensaje) { CamposFormulario.error(campo,mensaje); },
  validarFormulario(form) { return CamposFormulario.revisar(form); },
  erroresServidor(errores) {const campos={edadCoherente:'edad',habilidadesCoherentes:'habilidadesDetalle',gustosCoherentes:'gustosDetalle',personalidadCoherente:'personalidadDetalle',alergiasCoherentes:'alergiasDetalle',condicionesSaludCoherentes:'condicionesSaludDetalle',hermanosCoherentes:'cantidadHermanos',escolaridadCoherente:'rendimientoAcademico',hogarCoherente:'especificacionHogar',redApoyoCoherente:'notaRedApoyo',rangoEdadCoherente:'edadMaximaAceptada',disposicionHermanosCoherente:'notaHermanos',capacidadesCoherentes:'notaCondicionesSalud'};Object.entries(errores||{}).forEach(([id,mensaje])=>{const c=document.getElementById(campos[id]||id);if(c)this.errorCampo(c,mensaje);});},
  organizarFormulario() {
    const form=document.getElementById('formulario');if(!form)return;
    const iconos=['persona','escuela','estrella','personas','salud'];
    form.querySelectorAll('section.grupo').forEach((seccion,i)=>{
      if(seccion.contains(document.getElementById('idFuncionario'))){seccion.hidden=true;return;}
      const h=seccion.querySelector('h2');if(!h)return;const titulo=h.textContent.trim();
      const detalle=document.createElement('details');detalle.className='pro-form-seccion';detalle.open=i===1;
      const resumen=document.createElement('summary');resumen.innerHTML=this.icono(iconos[i])+'<span>'+limpiarHtml(titulo)+'</span><span class="pro-desplegar" aria-hidden="true">⌄</span>';
      h.remove();seccion.before(detalle);detalle.append(resumen,seccion);
    });
    form.addEventListener('invalid',e=>this.errorCampo(e.target,e.target.validationMessage),true);
    form.addEventListener('input',e=>{if(e.target.matches('input,textarea,select'))this.errorCampo(e.target,'');});
    document.querySelector('#pantallaNino h1').textContent=new URLSearchParams(location.search).has('editar')?'Actualizar perfil infantil':'Registrar perfil infantil';
    document.querySelector('#pantallaNino .hero p').textContent='Completa la información del niño, niña o adolescente.';
    document.querySelector('#pantallaNino .volver').textContent='← Volver al inicio';document.querySelector('#pantallaNino .volver').href='funcionario.html';
    document.querySelector('#pantallaNino .barra-pantalla').hidden=true;
    this.decorarHero('solicitudes');
  }
};
