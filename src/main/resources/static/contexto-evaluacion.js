/* Formulario y lectura del contexto profesional; no infiere técnicas, apoyos ni diagnósticos. */
const ContextoPsicologico={
 metodos:{ENTREVISTA_INDIVIDUAL:'Entrevista individual',OBSERVACION_DIRECTA:'Observación directa',REVISION_ANTECEDENTES:'Revisión de antecedentes',INSTRUMENTOS:'Aplicación de instrumentos psicológicos',OTRO:'Otro método'},
 apoyos:{EMOCIONAL:'Acompañamiento emocional',EDUCATIVO:'Apoyo educativo o de aprendizaje',RUTINAS:'Adaptación a cambios de rutina',VINCULOS:'Fortalecimiento de vínculos afectivos',SEGUIMIENTO_PSICOLOGICO:'Seguimiento psicológico',OTRO:'Otro apoyo'},
 seleccion(nombre){return [...document.querySelectorAll(`[name="${nombre}"]:checked`)].map(c=>c.value);},
 iniciar(){
  const metodos=document.getElementById('metodosEvaluacion'),apoyos=document.getElementById('apoyosEvaluacion');if(!metodos||!apoyos)return;
  const opciones=(valores,nombre)=>Object.entries(valores).map(([v,t])=>`<label><input type="checkbox" name="${nombre}" value="${v}">${t}</label>`).join('');
  metodos.innerHTML=opciones(this.metodos,'metodosContexto');apoyos.innerHTML=opciones(this.apoyos,'apoyosContexto');
  [metodos,apoyos].forEach(g=>g.addEventListener('change',()=>this.actualizar()));
  ['otroMetodo','instrumentos','resultadosInstrumentos','otroApoyo','detalleAcompanamiento'].forEach(id=>document.getElementById(id).addEventListener('input',()=>this.actualizar()));
  this.actualizar();
 },
 actualizar(){
  const m=this.seleccion('metodosContexto'),a=this.seleccion('apoyosContexto');
  for(const [grupo,activo] of [['otroMetodoContexto',m.includes('OTRO')],['instrumentosContexto',m.includes('INSTRUMENTOS')],['otroApoyoContexto',a.includes('OTRO')]]){
   const f=document.getElementById(grupo);f.hidden=!activo;f.disabled=!activo;
   f.querySelectorAll('textarea,input').forEach(c=>c.required=activo);
  }
  document.getElementById('detalleAcompanamiento').required=a.length>0;
  ['otroMetodo','instrumentos','resultadosInstrumentos','otroApoyo','detalleAcompanamiento'].forEach(id=>{const c=document.getElementById(id);c.setCustomValidity(c.required&&!c.disabled&&!c.value.trim()?'Escribe la información solicitada; no uses solo espacios.':'');});
 },
 // Los campos condicionales ocultos no se envían como técnicas o apoyos utilizados.
 leer(){
  const metodos=this.seleccion('metodosContexto'),apoyos=this.seleccion('apoyosContexto');
  const texto=id=>document.getElementById(id).value.trim()||null;
  return {formato:1,metodos,otroMetodo:metodos.includes('OTRO')?texto('otroMetodo'):null,instrumentos:metodos.includes('INSTRUMENTOS')?texto('instrumentos'):null,resultadosInstrumentos:metodos.includes('INSTRUMENTOS')?texto('resultadosInstrumentos'):null,apoyos,otroApoyo:apoyos.includes('OTRO')?texto('otroApoyo'):null,detalleAcompanamiento:texto('detalleAcompanamiento')};
 },
 cargar(contexto){
  const c=contexto||{};
  document.querySelectorAll('[name="metodosContexto"]').forEach(e=>e.checked=(c.metodos||[]).includes(e.value));
  document.querySelectorAll('[name="apoyosContexto"]').forEach(e=>e.checked=(c.apoyos||[]).includes(e.value));
  ['otroMetodo','instrumentos','resultadosInstrumentos','otroApoyo','detalleAcompanamiento'].forEach(id=>document.getElementById(id).value=c[id]||'');
  this.actualizar();
 },
 // Rellena únicamente controles locales; mantiene el expediente, la autoría y la versión que se está editando.
 async cargarEjemplo(expediente){
  if(!expediente||expediente.edad==null)throw new Error('Espera a que se cargue el expediente antes de usar el ejemplo.');
  const ejemplo=await EjemplosDemo.siguiente('psicologico');
  const form=document.getElementById('formEvaluacion');
  if(!Profesionales.formulariosBase.has(form))Profesionales.marcarGuardado(form);
  const edad=expediente.edad;
  const etapa=edad<6?'Observar el juego, la comunicación, la exploración y la respuesta a las figuras de cuidado, de acuerdo con su etapa de desarrollo.':edad<12?'Explorar sus actividades de juego, las relaciones con pares y la experiencia escolar de acuerdo con el nivel registrado.':'Dar espacio a su participación, sus intereses, la autonomía progresiva y las expectativas sobre los cambios de cuidado.';
  const escolar=expediente.nivelEscolar==='NO_ESCOLARIZADO'?'El expediente indica que no está escolarizado; no corresponde atribuirle rendimiento académico.':`Escolaridad registrada: ${Profesionales.etiqueta(expediente.nivelEscolar)}. No se presume un nivel por la edad.`;
  const hermanos=expediente.tieneHermanos==='SI'?`El expediente registra ${expediente.cantidadHermanos??'una cantidad pendiente de confirmar de'} hermanos; revisar los vínculos y las orientaciones autorizadas.`:expediente.tieneHermanos==='NO'?'El expediente indica que no tiene hermanos.':'La información sobre hermanos está pendiente; debe verificarse antes de extraer conclusiones.';
  const apoyo=expediente.necesidadesApoyo||'Completar con el equipo la información sobre apoyos requeridos.';
  const valores={
   evaluacionPsicologica:`Ejemplo para ${expediente.codigoCaso}, de ${edad} años. Se propone revisar los siguientes aspectos: ${ejemplo.foco}. ${etapa} ${escolar}`,
   observacionesPsicologicas:`Revisar las respuestas durante actividades cotidianas y diferenciar la conducta observada de su interpretación. ${hermanos} Las observaciones de este ejemplo deben ajustarse a lo efectivamente registrado por el profesional.`,
   hallazgosProfesionales:`La información del expediente orienta la revisión de ${ejemplo.foco}. Necesidades de apoyo documentadas: ${apoyo} No se infieren diagnósticos ni antecedentes adicionales a partir de estos datos.`,
   recomendacionesPsicologicas:`${ejemplo.orientacion} ${etapa} Coordinar las orientaciones con el seguimiento social y de salud ya documentado.`
  };
  Object.entries(valores).forEach(([id,v])=>{const c=document.getElementById(id);c.value=v;c.setCustomValidity('');});
  document.querySelectorAll('[name="areasObservadas"]').forEach(c=>c.checked=ejemplo.areas.includes(c.value));
  document.getElementById('sinDiagnostico').checked=true;document.getElementById('diagnosticoProfesional').value='';
  document.getElementById('sinDiagnostico').dispatchEvent(new Event('change',{bubbles:true}));
  this.cargar({formato:1,metodos:['REVISION_ANTECEDENTES'],apoyos:['EMOCIONAL','RUTINAS','VINCULOS'],detalleAcompanamiento:`${ejemplo.orientacion} Apoyos consignados: ${apoyo}`});
  document.getElementById('cargarEjemploPsicologico').textContent='Cargar otro ejemplo';
 },
 informe(c){
  if(!c)return '';
  const bloque=(icono,titulo,contenido)=>`<div class="pro-informe-bloque"><h4>${Profesionales.icono(icono)}${titulo}</h4>${contenido}</div>`;
  const texto=(titulo,v)=>v?`<p><strong>${titulo}</strong><br>${limpiarHtml(v)}</p>`:'';
  const lista=(valores,etiquetas)=>valores?.length?`<ul>${valores.map(v=>`<li>${limpiarHtml(etiquetas[v]||Profesionales.etiqueta(v))}</li>`).join('')}</ul>`:'';
  let html='';
  if(c.metodos?.length||c.otroMetodo)html+=bloque('solicitudes','Métodos utilizados',lista(c.metodos,this.metodos)+texto('Otro método',c.otroMetodo));
  if(c.instrumentos||c.resultadosInstrumentos)html+=bloque('informe','Instrumentos psicológicos',texto('Instrumentos utilizados',c.instrumentos)+texto('Resultados pertinentes',c.resultadosInstrumentos));
  if(c.apoyos?.length||c.detalleAcompanamiento)html+=bloque('personas','Necesidades de acompañamiento',lista(c.apoyos,this.apoyos)+texto('Otro apoyo',c.otroApoyo)+texto('Necesidad, contexto y orientaciones',c.detalleAcompanamiento));
  return html;
 }
};
