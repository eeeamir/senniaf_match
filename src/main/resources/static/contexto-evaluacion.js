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
