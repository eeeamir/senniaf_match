const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const context = vm.createContext({Profesionales: {icono: n=>`<svg data-icono="${n}"></svg>`, fecha: String, fechaCivil: String, etiqueta: String}, limpiarHtml: x=>String(x??'').replaceAll('<','&lt;')});
vm.runInContext(fs.readFileSync('src/main/resources/static/vinculaciones.js','utf8'),context);
const V = vm.runInContext('Vinculaciones',context);
const fecha='2026-11-20T09:00:00-05:00';
const base={codigo:'VIN-SINTETICA',fechaCreacion:fecha,fechaRespuesta:fecha};
test('Interrupción registrada domina la quinta etapa aun sin autorización antigua',()=>{
 const p=V.obtenerProgreso({...base,estado:'CERRADA',interrupcionRegistrada:true,seguimientoFamiliar:{interrupcionRegistradaEl:fecha,interrupcionRol:'TRABAJADOR_SOCIAL'}});
 assert.equal(p.estado,'Vinculación cerrada');assert.equal(p.etapas[4].texto,'Interrumpida');assert.equal(p.etapas[4].clase,'interrumpido');
 assert.match(p.etapas[3].texto,/no documentada/);assert.notEqual(p.etapas[3].clase,'completado');assert.match(p.etapas[4].detalle,/Trabajo Social/);
});
test('Cierre conserva estado principal incluso con revisión de interrupción pendiente',()=>{
 assert.equal(V.obtenerProgreso({...base,estado:'CERRADA',interrupcionRegistrada:true,interrupcionPendiente:true}).estado,'Vinculación cerrada');
});
test('Rechazo con fecha es rojo y no completa etapas posteriores',()=>{
 const v={...base,estado:'RECHAZADA_POR_FAMILIA',respuestaFamiliar:'RECHAZADA'};const p=V.obtenerProgreso(v);
 assert.equal(p.etapas[1].clase,'interrumpido');assert.ok(p.etapas.slice(2).every(x=>x.clase!=='completado'));
 assert.match(V.proceso(v),/chip vinculacion-negativa/);assert.match(V.estado(v),/chip vinculacion-negativa/);
});
test('Aceptación conserva la etapa histórica y acompañamiento vigente',()=>{
 const p=V.obtenerProgreso({...base,estado:'RESPUESTA_ACEPTADA'});assert.equal(p.etapas[1].texto,'Aceptada');assert.equal(p.etapas[2].clase,'actual');
});
test('Una fecha sin resultado no inventa aceptación en un cierre histórico',()=>{
 const p=V.obtenerProgreso({...base,estado:'CERRADA'});assert.equal(p.etapas[1].texto,'Respuesta registrada');assert.notEqual(p.etapas[1].clase,'completado');
});
test('Autorización sin inicio no acredita convivencia iniciada',()=>{
 const p=V.obtenerProgreso({...base,estado:'RESPUESTA_ACEPTADA',autorizacionPreadoptiva:{registradaEl:fecha}});assert.equal(p.etapas[3].clase,'completado');assert.equal(p.etapas[4].texto,'No iniciada');
});
test('Interrupción, revisión y cierre son eventos diferentes con sus fechas originales',()=>{
 const fechas=['2026-11-18T11:00:00-05:00','2026-11-19T12:00:00-05:00','2026-11-20T13:00:00-05:00'];
 const p=V.obtenerProgreso({...base,estado:'CERRADA',interrupcionRegistrada:true,seguimientoFamiliar:{interrupcionRegistradaEl:fechas[0],revisionRegistradaEl:fechas[1],cierreRegistradoEl:fechas[2]}});
 assert.deepEqual(Array.from(p.eventos.slice(-3),e=>e.fecha),fechas);assert.equal(new Set(p.eventos.slice(-3).map(e=>e.titulo)).size,3);
});
test('Un solo marcador por evento y títulos largos conservados como texto',()=>{
 const eventos=[{titulo:'Título extenso que puede ocupar más de una línea',fecha,resumen:'Detalle'},{titulo:'<b>Texto literal</b>',fecha}];const h=V.lineaTiempo(eventos);
 assert.equal((h.match(/class="vinculacion-punto"/g)||[]).length,2);assert.match(h,/&lt;b>/);assert.ok(!h.includes('vinculacion-emblema'));
});
test('Autoría familiar y fecha del hecho se muestran solo cuando constan',()=>{
 const v={...base,estado:'CERRADA',interrupcionRegistrada:true,seguimientoFamiliar:{interrupcionRol:'FAMILIA',fechaInterrupcion:'2026-11-19'}};
 const e=V.obtenerProgreso(v).etapas[4];assert.match(e.detalle,/La familia registró/);assert.equal(e.fechaHecho,'2026-11-19');
 assert.equal(V.obtenerProgreso({...base,estado:'CERRADA',interrupcionRegistrada:true}).etapas[4].fechaHecho,undefined);
});
