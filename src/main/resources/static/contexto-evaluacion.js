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
 cargarEjemplo(){
  const form=document.getElementById('formEvaluacion');
  if(!Profesionales.formulariosBase.has(form))Profesionales.marcarGuardado(form);
  const valores={
   evaluacionPsicologica:'El escenario presenta recursos de comunicación e interacción que conviene valorar junto con la adaptación a nuevas rutinas. Se propone explorar la expresión de preferencias, la solicitud de ayuda y los vínculos con las figuras de referencia. La información descrita no establece un diagnóstico clínico.',
   observacionesPsicologicas:'En el ejemplo se describe una participación gradual en actividades compartidas y una mejor respuesta cuando las indicaciones son claras. Conviene registrar por separado las conductas observables y las interpretaciones profesionales, atendiendo al contexto y al ritmo de adaptación.',
   hallazgosProfesionales:'Se consideran fortalezas la posibilidad de expresar intereses y la participación en actividades estructuradas. Requieren seguimiento la adaptación a los cambios y la disponibilidad de apoyos estables. La valoración debe completarse con información profesional suficiente antes de formular conclusiones clínicas.',
   recomendacionesPsicologicas:'Anticipar los cambios de rutina, ofrecer espacios de conversación y actividades compartidas, y coordinar el apoyo educativo con las personas responsables. Revisar periódicamente la adaptación y ajustar las orientaciones conforme a las necesidades documentadas.'
  };
  Object.entries(valores).forEach(([id,v])=>document.getElementById(id).value=v);
  document.querySelectorAll('[name="areasObservadas"]').forEach(c=>c.checked=['ADAPTACION_EMOCIONAL','INTERACCION_SOCIAL','RELACIONES_AFECTIVAS','ACOMPANAMIENTO'].includes(c.value));
  document.getElementById('sinDiagnostico').checked=true;document.getElementById('diagnosticoProfesional').value='';
  document.getElementById('sinDiagnostico').dispatchEvent(new Event('change',{bubbles:true}));
  this.cargar({formato:1,metodos:['ENTREVISTA_INDIVIDUAL','OBSERVACION_DIRECTA','REVISION_ANTECEDENTES','INSTRUMENTOS'],otroMetodo:null,instrumentos:'Guía de observación estructurada para el ejercicio académico; registro descriptivo sin puntuación psicométrica.',resultadosInstrumentos:'El ejemplo organiza la participación en la conversación, la respuesta a consignas y los recursos de expresión. No permite derivar un diagnóstico ni acredita la aplicación real de una prueba.',apoyos:['EMOCIONAL','RUTINAS','VINCULOS','SEGUIMIENTO_PSICOLOGICO'],otroApoyo:null,detalleAcompanamiento:'Acompañar los cambios de rutina con anticipación y apoyos previsibles. Favorecer los vínculos significativos y coordinar el seguimiento psicológico con las personas responsables, ajustando las orientaciones al contexto registrado.'});
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
