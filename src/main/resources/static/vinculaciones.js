/* Comparte las vistas de vinculaciones sin presumir acogimiento ni adopción formalizada. */
const Vinculaciones = {
  estados: {
    RECHAZADA_POR_FAMILIA: 'Propuesta rechazada por la familia',
    CERRADA: 'Cerrada',
    PENDIENTE_RESPUESTA: 'Pendiente de respuesta familiar',
    RESPUESTA_ACEPTADA: 'Respuesta aceptada',
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
  estado(v) {
    const rechazado = v.estado === 'RESPUESTA_RECHAZADA_PENDIENTE_CIERRE';
    const icono = v.estado === 'PENDIENTE_RESPUESTA' ? 'reloj' : rechazado ? 'advertencia' : 'comprobar';
    return `<span class="chip ${rechazado ? 'retirado' : v.estado === 'PENDIENTE_RESPUESTA' ? 'vinculacion-pendiente' : ''}">${Profesionales.icono(icono)}${limpiarHtml(this.estados[v.estado] || v.estado)}</span>${v.interrupcionRegistrada?` <span class="chip retirado">${Profesionales.icono('advertencia')}${v.interrupcionPendiente?'Interrupción pendiente de revisión':'Interrupción revisada'}</span>`:''}`;
  },
  familiar(lista) {
    if (!lista.length) return `<section class="tarjeta"><h2>${Profesionales.icono('vinculacion')}Mi vinculación</h2><p>Por ahora no tienes vinculaciones registradas.</p></section>`;
    return lista.map(v => `<section class="tarjeta">
      <div class="pro-fila-titulo"><h2>${Profesionales.icono('vinculacion')} ${limpiarHtml(v.codigo)}</h2>${this.estado(v)}</div>
      <p>${Profesionales.icono('personas')} Expediente familiar: ${limpiarHtml(v.expedienteFamiliar)}</p>
      <p>${Profesionales.icono('calendario')} Propuesta enviada por el Comité: ${limpiarHtml(Profesionales.fecha(v.fechaCreacion))}</p>
      <p>${limpiarHtml(v.siguientePaso)}</p>
      ${v.fechaRespuesta ? `<p>Respuesta registrada: ${limpiarHtml(Profesionales.fecha(v.fechaRespuesta))}</p>` : ''}
      <p class="ayuda">${Profesionales.icono('informacion')} La respuesta en este portal no sustituye las formalidades legales ni autoriza el acogimiento preadoptivo.</p>
      ${v.estado === 'PENDIENTE_RESPUESTA' ? `<div class="pro-acciones">
        <button class="boton" data-responder="ACEPTAR" data-id="${v.id}">${Profesionales.icono('comprobar')}Aceptar propuesta</button>
        <button class="boton secundario" data-responder="RECHAZAR" data-id="${v.id}">${Profesionales.icono('cerrar')}Rechazar propuesta</button>
      </div>` : ''}
    </section>`).join('');
  },
  enlazarRespuestas(sesion) {
    document.querySelectorAll('[data-responder]').forEach(b => b.onclick = () => {
      if (document.querySelector('dialog[data-respuesta]')) return;
      const aceptar = b.dataset.responder === 'ACEPTAR', d = document.createElement('dialog');
      d.className = 'pro-dialogo'; d.dataset.respuesta = ''; d.setAttribute('aria-labelledby', 'tituloRespuesta');
      d.innerHTML = `<div class="pro-modal-cabecera"><span class="pro-simbolo">${Profesionales.icono(aceptar ? 'comprobar' : 'advertencia')}</span><h2 id="tituloRespuesta">${aceptar ? 'Aceptar' : 'Rechazar'} propuesta</h2></div>
        <p>${aceptar ? 'Se registrará tu aceptación y el equipo institucional te orientará sobre los siguientes pasos. Esto no formaliza una adopción.' : 'Se registrará tu respuesta. Esta combinación no volverá a recomendarse automáticamente. No cambia tu idoneidad ni tu puntuación.'}</p>
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
          d.close(); Familia.version = null; await Familia.mostrar(sesion);
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
        </form></section><div id="listaVinculaciones" aria-live="polite"></div>`;
      const form = document.getElementById('filtroVinculaciones');
      let consulta = 0;
      const cargar = async () => {
        const actual = ++consulta, lista = document.getElementById('listaVinculaciones');
        try {
          const datos = await this.api('/api/vinculaciones?' + new URLSearchParams(new FormData(form)));
          if (actual !== consulta) return;
          lista.innerHTML = datos.length ? datos.map(v => `<article class="tarjeta vinculacion-tarjeta">
            <div class="pro-fila-titulo"><h2>${Profesionales.icono('vinculacion')}${limpiarHtml(v.codigo)}</h2>${this.estado(v)}</div>
            <p>${Profesionales.icono('carpeta')} ${limpiarHtml(v.expedienteInfantil)} · ${Profesionales.icono('personas')} ${limpiarHtml(v.expedienteFamiliar)}</p>
            <div class="vinculacion-datos"><p>${Profesionales.icono('calendario')}${limpiarHtml(Profesionales.fecha(v.fechaCreacion))}</p><p>${Profesionales.icono('criterios')}Compatibilidad original: <strong>${v.puntajeOriginal??'No disponible'} / 100</strong> · Criterios ${v.versionOriginal??'no disponibles'}</p></div>
            <a class="boton secundario" href="vinculaciones.html?id=${v.id}">${Profesionales.icono('consulta')}Ver detalle</a>
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
    const enlaceNino = `perfiles.html?caso=${encodeURIComponent(v.expedienteInfantil)}`;
    const enlaceFamilia = `perfiles.html?familia=${encodeURIComponent(v.expedienteFamiliar)}&casoOrigen=${encodeURIComponent(v.expedienteInfantil)}&origen=match`;
    c.innerHTML = `<p class="pro-breadcrumb"><a href="vinculaciones.html">${Profesionales.icono('flecha')}Volver a Vinculaciones</a></p>`+Profesionales.encabezado(v.codigo, 'Detalle de la vinculación', 'vinculacion') + `
      <section class="tarjeta">${this.estado(v)}<dl class="detalle-datos">
        <div><dt>Expediente infantil</dt><dd><a href="${enlaceNino}">${limpiarHtml(v.expedienteInfantil)}</a></dd></div>
        <div><dt>Expediente familiar</dt><dd>${sesion.rol === 'COMITE' ? `<a href="${enlaceFamilia}">${limpiarHtml(v.expedienteFamiliar)}</a>` : limpiarHtml(v.expedienteFamiliar)}</dd></div>
        <div><dt>Creación</dt><dd>${limpiarHtml(Profesionales.fecha(v.fechaCreacion))}</dd></div>
        <div><dt>Disponibilidad</dt><dd>${limpiarHtml(v.disponibilidad)}</dd></div>
      </dl></section>${this.compatibilidadOriginal(v.compatibilidadOriginal)}<section class="tarjeta"><h2>${Profesionales.icono('solicitudes')}Decisión del Comité</h2>
      <p>Propuesta de Match aceptada para remitirla a la familia.</p><p>${limpiarHtml(d.responsable)} · ${limpiarHtml(Profesionales.fecha(d.fecha))}</p><p>${limpiarHtml(d.motivo)}</p>
      </section><section class="tarjeta"><h2>${Profesionales.icono('solicitudes')}Proceso</h2><p>${limpiarHtml(v.siguientePaso)}</p>
      ${v.fechaRespuesta ? `<p>Respuesta familiar registrada por ${limpiarHtml(v.respondidoPor)} · ${limpiarHtml(Profesionales.fecha(v.fechaRespuesta))}</p>` : ''}
      <p class="ayuda">${Profesionales.icono('informacion')}Esta vinculación no acredita acogimiento preadoptivo ni adopción formalizada.</p></section>
      ${this.panelInterrupcion(v)}${this.panelCierre(v)}
      <section class="tarjeta"><h2>${Profesionales.icono('reloj')}Historial</h2><ul class="familia-actividad">
        ${v.historial.map(e => `<li><strong>${limpiarHtml(e.resumen)}</strong><p>${Profesionales.icono('persona')}${limpiarHtml(e.responsable)} · ${limpiarHtml(Profesionales.fecha(e.fecha))}</p></li>`).join('') || '<li>La decisión del Comité se conserva en la información general. No hay movimientos adicionales registrados.</li>'}
      </ul></section>`;
    c.querySelector('[data-cierre]')?.addEventListener('click',()=>this.registrarCierre(c,v,sesion));
    c.querySelectorAll('[data-disponibilidad]').forEach(b=>b.onclick=()=>Profesionales.justificar({
      titulo:'Revisar disponibilidad del expediente',maximo:500,contexto:b.dataset.disponibilidad==='NINO'?'Expediente infantil':'Expediente familiar',
      opciones:[['OTRO','Revisión de disponibilidad y elegibilidad','']],
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
  compatibilidadOriginal(h){
    if(!h)return '<section class="tarjeta"><h2>Compatibilidad original</h2><p>No hay análisis histórico disponible.</p></section>';
    const c=h.contexto||{},factores={edad:'Rango de edad',hermanos:'Vínculos entre hermanos',salud:'Salud y desarrollo',apoyo:'Red de apoyo',logistica:'Logística'},dato=v=>v==null?'No disponible':limpiarHtml(String(v));
    return `<section class="tarjeta vinculacion-historica"><div class="pro-fila-titulo"><div><h2>${Profesionales.icono('criterios')}Compatibilidad original</h2><p>Se conserva el análisis registrado al aceptar esta propuesta.</p><span class="chip">Criterios: ${dato(h.version)}</span></div><div class="pro-puntaje">${dato(h.puntaje)}<small>/100</small></div></div>
      <details class="pro-seccion"><summary>${Profesionales.icono('consulta')}Consultar los cinco factores originales</summary><div class="tabla"><table><thead><tr><th>Factor</th><th>Peso original</th><th>Puntos</th><th>Fundamento registrado</th></tr></thead><tbody>${Object.entries(factores).map(([k,n])=>`<tr><td>${n}</td><td>${dato(c.pesos?.[k])}</td><td>${dato(c.factores?.[k]?.[0])}</td><td>${dato(c.factores?.[k]?.[1])}</td></tr>`).join('')}</tbody></table></div>${!h.contexto?'<p class="ayuda">Este antecedente no conserva el desglose. No se sustituye por un cálculo nuevo.</p>':''}<p class="ayuda">Máximo aplicable registrado: ${dato(c.maximoAplicable)} · Peso evaluado: ${dato(c.pesoEvaluado)}</p></details></section>`;
  },
  panelCierre(v){
    if(!v.cierre && !v.puedeCerrar)return '';
    const x=v.cierre;
    return `<section class="tarjeta"><h2>${Profesionales.icono('informe')}Cierre institucional</h2>
      <p>El Comité registra la confirmación de la autoridad competente. Este registro no autoriza legalmente el cierre ni formaliza una adopción.</p>
      ${x?`<dl class="detalle-datos"><div><dt>Fecha del cierre</dt><dd>${limpiarHtml(x.fecha)}</dd></div>${x.referencia?`<div><dt>Referencia institucional registrada</dt><dd>${limpiarHtml(x.referencia)}</dd></div>`:''}${x.autoridad?`<div><dt>Autoridad registrada</dt><dd>${limpiarHtml(x.autoridad)}</dd></div>`:''}</dl><p class="pro-texto-registrado">${limpiarHtml(x.motivo)}</p><p>${limpiarHtml(x.registradoPor)} · ${limpiarHtml(Profesionales.fecha(x.registradoEl))}</p>
      <h3>Revisión independiente de los expedientes</h3><p>El cierre no devuelve automáticamente los expedientes al Match. Cada uno requiere revisión de disponibilidad y elegibilidad.</p>
      ${[['NINO','Expediente infantil',x.ninoLiberado],['FAMILIA','Expediente familiar',x.familiaLiberada]].map(([k,n,revisado])=>`<p><strong>${n}:</strong> ${revisado?'Revisión registrada; sujeto a las condiciones vigentes de Match.':'Reserva conservada; pendiente de revisión.'}</p>${v.puedeRevisarDisponibilidad&&!revisado?`<button class="boton secundario" data-disponibilidad="${k}">${Profesionales.icono('consulta')}Revisar ${n.toLowerCase()}</button>`:''}`).join('')}`:''}
      ${v.puedeCerrar?`<button class="boton secundario" data-cierre>${Profesionales.icono('informe')}Registrar cierre institucional</button>`:''}</section>`;
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
  panelInterrupcion(v){
    const i=v.interrupcion;
    if(!i && !v.puedeReportarInterrupcion)return '';
    return `<section class="tarjeta"><h2>${Profesionales.icono('advertencia')}Interrupción de convivencia</h2>${i?`
      <p><strong>${i.fechaRevision?'Revisión registrada':'Pendiente de revisión del Comité'}</strong></p>
      <dl class="detalle-datos"><div><dt>Fecha de la interrupción</dt><dd>${limpiarHtml(i.fechaInterrupcion)}</dd></div><div><dt>Referencia institucional de la convivencia</dt><dd>${limpiarHtml(i.referenciaActuacion)}</dd></div></dl>
      <p class="pro-texto-registrado">${limpiarHtml(i.justificacion)}</p><p>${Profesionales.icono('persona')}${limpiarHtml(i.registradoPor)} · ${limpiarHtml(Profesionales.fecha(i.fechaRegistro))}</p>
      ${i.fechaRevision?`<h3>Revisión del Comité</h3><p class="pro-texto-registrado">${limpiarHtml(i.revisionJustificacion)}</p><p>${limpiarHtml(i.revisadoPor)} · ${limpiarHtml(Profesionales.fecha(i.fechaRevision))}</p>`:''}
      `:'<p>Deja constancia de una interrupción de convivencia respaldada por la actuación institucional correspondiente.</p>'}
      <p class="ayuda">${Profesionales.icono('informacion')}El registro conserva los antecedentes y la reserva de los expedientes. No cambia su idoneidad ni formaliza una adopción.</p>
      ${v.puedeReportarInterrupcion?`<button class="boton secundario" data-interrupcion>${Profesionales.icono('advertencia')}Registrar interrupción de convivencia</button>`:''}
      ${v.puedeRevisarInterrupcion?`<button class="boton" data-revisar-interrupcion>${Profesionales.icono('solicitudes')}Registrar revisión</button>`:''}</section>`;
  },
  registrarInterrupcion(c,v,sesion){
    const d=document.createElement('dialog');d.className='pro-dialogo';d.setAttribute('aria-labelledby','tituloInterrupcion');
    const hoy=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Panama',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
    d.innerHTML=`<div class="pro-modal-cabecera"><span class="pro-simbolo">${Profesionales.icono('advertencia')}</span><h2 id="tituloInterrupcion">Registrar interrupción de convivencia</h2></div>
      <p>Deja constancia de lo ocurrido y de las circunstancias que deben considerarse durante la revisión del Comité.</p>
      <form class="pro-interrupcion-form"><label>Fecha de la interrupción<input type="date" name="fechaInterrupcion" required max="${hoy}" value="${hoy}"></label>
      <label>Referencia institucional de la convivencia<input name="referenciaActuacion" minlength="5" maxlength="200" required></label>
      <p class="ayuda">Indica la actuación que respalda la convivencia. La aceptación en el portal, por sí sola, no la autoriza.</p>
      <label>Motivo de la interrupción<textarea name="justificacion" minlength="10" maxlength="2000" rows="5" required placeholder="Describe brevemente qué ocurrió y los aspectos importantes para la revisión."></textarea></label>
      <p data-error role="alert"></p><div class="pro-acciones"><button type="button" class="boton secundario" data-cancelar>Cancelar</button><button class="boton" type="submit">Revisar y confirmar</button></div></form>`;
    document.body.append(d);const form=d.querySelector('form');let guardando=false;
    d.onclose=()=>d.remove();d.addEventListener('cancel',e=>{if(guardando)e.preventDefault();});d.querySelector('[data-cancelar]').onclick=()=>d.close();
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
