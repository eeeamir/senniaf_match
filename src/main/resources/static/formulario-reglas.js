/* Reglas de presentación compartidas con las validaciones de los DTO; nunca guardan datos. */
const ReglasPerfil = {
  exclusivas: new Set(['NINGUNA_CONOCIDA','NINGUNA_IDENTIFICADA','NINGUNO_IDENTIFICADO','NINGUNA_POR_AHORA','AUN_NO_EVALUADO','INFORMACION_INSUFICIENTE','INFORMACION_NO_DISPONIBLE']),
  grados: {
    INICIAL:['Maternal','Prejardín','Jardín'],
    PRIMARIA:['1er grado','2do grado','3er grado','4to grado','5to grado','6to grado'],
    PREMEDIA:['7mo grado','8vo grado','9no grado'],
    MEDIA:['10mo grado','11mo grado','12mo grado']
  },
  iniciar() {
    document.querySelectorAll('.grupo-checks').forEach(grupo=>{
      grupo.addEventListener('change',e=>{
        if(!e.target.matches('input[type=checkbox]'))return;
        if(e.target.checked)grupo.querySelectorAll('input').forEach(c=>{
          if(c!==e.target&&(this.exclusivas.has(e.target.value)||this.exclusivas.has(c.value)))c.checked=false;
        });
        // La capacidad actual y la capacidad que requiere apoyo son respuestas distintas.
        const nombre=grupo.dataset.grupo;
        const otro=nombre==='condicionesPuedeAcompanarHoy'?'condicionesConApoyo':nombre==='condicionesConApoyo'?'condicionesPuedeAcompanarHoy':null;
        if(otro&&e.target.checked)document.querySelectorAll(`[data-grupo="${otro}"] input`).forEach(c=>{if(c.value===e.target.value)c.checked=false;});
        this.revisarGrupo(grupo);
        if(['alergias','condicionesSalud'].includes(nombre)&&typeof actualizarAvisoMedico==='function')actualizarAvisoMedico();
      });
    });
    document.getElementById('nivelEscolar').addEventListener('change',()=>this.refrescarNino(''));
    document.getElementById('gradoEscolarSeleccion').addEventListener('change',()=>this.actualizarGrado());
    document.getElementById('gradoEscolarOtro').addEventListener('input',()=>this.actualizarGrado());
    document.getElementById('tieneHermanos').addEventListener('change',()=>{
      document.getElementById('cantidadHermanos').value='';this.actualizarHermanos();
    });
    document.getElementById('cantidadHermanos').addEventListener('input',()=>this.actualizarHermanos());
    ['tipoFamilia','disposicionHermanos','edadMinimaAceptada','edadMaximaAceptada'].forEach(id=>document.getElementById(id).addEventListener('change',()=>this.refrescarFamilia()));
    document.querySelectorAll('#formulario input,#formulario textarea,#formularioFamilia textarea').forEach(c=>{
      if(c.type==='text')c.maxLength=c.id==='especificacionHogar'?1000:255;
      if(c.tagName==='TEXTAREA')c.maxLength=c.id==='notaRangoEdad'||c.id==='personalidadDetalle'||c.id==='hermanosDetalle'?255:c.id==='especificacionHogar'?1000:2000;
      c.addEventListener('input',()=>{if(c.required)c.setCustomValidity(c.value.trim()?'':'Completa este campo.');});
    });
    this.refrescarNino();this.refrescarFamilia();
  },
  revisarGrupo(grupo) {
    const marcados=[...grupo.querySelectorAll('input:checked')],primero=grupo.querySelector('input');
    const obligatorio=['habilidades','gustos','personalidad','alergias','condicionesSalud','redApoyo'].includes(grupo.dataset.grupo);
    primero.setCustomValidity(obligatorio&&!marcados.length?'Selecciona una opción, o indica que la información no está disponible.':marcados.length>1&&marcados.some(c=>this.exclusivas.has(c.value))?'Las opciones seleccionadas son excluyentes.':'');
    const detalle=document.getElementById(grupo.dataset.grupo+'Detalle');
    if(detalle){
      detalle.required=marcados.some(c=>['OTRA','OTRO'].includes(c.value)||(grupo.dataset.grupo==='alergias'&&c.value==='ALIMENTARIA'));
      detalle.setCustomValidity(detalle.required&&!detalle.value.trim()?'Explica la opción seleccionada.':'');
    }
  },
  refrescarNino(gradoGuardado) {
    const nivel=document.getElementById('nivelEscolar').value,grado=document.getElementById('gradoEscolar'),selector=document.getElementById('gradoEscolarSeleccion');
    const previo=gradoGuardado===undefined?grado.value:gradoGuardado||'';
    if(gradoGuardado==='')document.getElementById('gradoEscolarOtro').value='';
    const sinEscolaridad=nivel==='NO_ESCOLARIZADO',desconocido=nivel==='INFORMACION_NO_DISPONIBLE';
    const opciones=sinEscolaridad?['No aplica']:desconocido?['Información no disponible']:[...(this.grados[nivel]||[]),'No evaluado','Información no disponible','Otro'];
    selector.innerHTML='<option value="">Seleccione…</option>'+opciones.map(v=>`<option value="${limpiarHtml(v)}">${limpiarHtml(v)}</option>`).join('');
    selector.disabled=sinEscolaridad||desconocido||!nivel;
    selector.required=Boolean(nivel)&&!selector.disabled;
    if(sinEscolaridad)selector.value='No aplica';
    else if(desconocido)selector.value='Información no disponible';
    else if(opciones.includes(previo))selector.value=previo;
    else if(previo){selector.value='Otro';document.getElementById('gradoEscolarOtro').value=previo;}
    else if(nivel==='OTRO')selector.value='Otro';
    if(selector.value!=='Otro')document.getElementById('gradoEscolarOtro').value='';
    this.actualizarGrado();
    const rendimiento=document.getElementById('rendimientoAcademico');
    rendimiento.disabled=sinEscolaridad;rendimiento.required=Boolean(nivel)&&!sinEscolaridad&&!desconocido;
    rendimiento.querySelector('[value=NO_APLICA]').hidden=!sinEscolaridad;
    if(sinEscolaridad)rendimiento.value='NO_APLICA';
    else if(desconocido)rendimiento.value='INFORMACION_NO_DISPONIBLE';
    else if(rendimiento.value==='NO_APLICA')rendimiento.value='';
    this.actualizarHermanos();
    document.querySelectorAll('#formulario .grupo-checks').forEach(g=>this.revisarGrupo(g));
  },
  actualizarGrado() {
    const selector=document.getElementById('gradoEscolarSeleccion'),otro=document.getElementById('gradoEscolarOtro');
    const activo=selector.value==='Otro';otro.disabled=!activo;otro.required=activo;
    document.getElementById('gradoOtroCampo').hidden=!activo;
    otro.setCustomValidity(activo&&!otro.value.trim()?'Especifica el grado o programa educativo.':'');
    document.getElementById('gradoEscolar').value=activo?otro.value.trim():selector.value;
  },
  actualizarHermanos() {
    const respuesta=document.getElementById('tieneHermanos').value,cantidad=document.getElementById('cantidadHermanos');
    cantidad.disabled=respuesta!=='SI';cantidad.required=respuesta==='SI';cantidad.min=respuesta==='SI'?'1':'0';cantidad.step='1';
    if(respuesta==='NO')cantidad.value='0';else if(respuesta!=='SI')cantidad.value='';
    cantidad.setCustomValidity(respuesta==='SI'&&(!Number.isInteger(Number(cantidad.value))||Number(cantidad.value)<=0)?'Indica una cantidad entera mayor que cero.':'');
  },
  refrescarFamilia() {
    const otra=document.getElementById('tipoFamilia').value==='OTRA',detalle=document.getElementById('especificacionHogar');
    document.getElementById('especificacionHogarCampo').hidden=!otra;detalle.disabled=!otra;detalle.required=otra;
    if(!otra)detalle.value='';
    detalle.setCustomValidity(otra&&!detalle.value.trim()?'Especifica cómo está conformado tu hogar.':'');
    const hermanos=document.getElementById('disposicionHermanos').value,nota=document.getElementById('notaHermanos');
    nota.required=['OTRA','NINGUNA_DE_LAS_ANTERIORES'].includes(hermanos);
    nota.setCustomValidity(nota.required&&!nota.value.trim()?'Explica la situación de tu hogar respecto a grupos de hermanos.':'');
    const minima=document.getElementById('edadMinimaAceptada'),maxima=document.getElementById('edadMaximaAceptada');
    minima.step=maxima.step='1';
    maxima.setCustomValidity(minima.value!==''&&maxima.value!==''&&Number(minima.value)>Number(maxima.value)?'La edad máxima debe ser igual o mayor que la mínima.':'');
    document.querySelectorAll('#formularioFamilia .grupo-checks').forEach(g=>this.revisarGrupo(g));
  }
};
