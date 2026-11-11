/* Escenarios completos y bolsa aleatoria por sesión: solo llena controles locales. */
const EjemplosDemo = {
  bolsas: {}, ultimo: {}, catalogo: null,
  async siguiente(tipo) {
    if(!this.catalogo){
      const r=await fetch('escenarios-demo.json',{cache:'no-cache'});
      if(!r.ok)throw new Error('No se pudieron cargar los ejemplos. Tus respuestas se conservan.');
      this.catalogo=await r.json();
    }
    const escenarios=this.catalogo[tipo];
    if(!escenarios?.length)throw new Error('No hay escenarios disponibles para este formulario.');
    const clave='senniaf.ejemplos.'+tipo;
    let estado=this.bolsas[tipo];
    if(!estado){
      try{const guardado=JSON.parse(sessionStorage.getItem(clave)||'null');if(guardado&&Array.isArray(guardado.pendientes)&&guardado.pendientes.every(i=>Number.isInteger(i)&&i>=0&&i<escenarios.length)&&new Set(guardado.pendientes).size===guardado.pendientes.length)estado=guardado;}catch(e){/* El almacenamiento es opcional. */}
      estado=estado||{pendientes:[],ultimo:null};this.bolsas[tipo]=estado;
    }
    if(!estado.pendientes.length){
      estado.pendientes=escenarios.map((_,i)=>i);
      for(let i=estado.pendientes.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[estado.pendientes[i],estado.pendientes[j]]=[estado.pendientes[j],estado.pendientes[i]];}
      const fin=estado.pendientes.length-1;
      if(fin>0&&estado.pendientes[fin]===estado.ultimo)[estado.pendientes[0],estado.pendientes[fin]]=[estado.pendientes[fin],estado.pendientes[0]];
    }
    const indice=estado.pendientes.pop();estado.ultimo=indice;
    try{sessionStorage.setItem(clave,JSON.stringify(estado));}catch(e){/* Sigue funcionando en memoria. */}
    return JSON.parse(JSON.stringify(escenarios[indice]));
  },
  aplicar(form,datos) {
    form.querySelectorAll('input,select,textarea').forEach(c=>{c.setCustomValidity('');c.removeAttribute('aria-invalid');});
    form.querySelectorAll('.pro-error-campo').forEach(e=>e.textContent='');
    Object.entries(datos).forEach(([id,valor])=>{
      if(Array.isArray(valor))form.querySelectorAll(`[data-grupo="${id}"] input`).forEach(c=>c.checked=valor.includes(c.value));
      else{const campo=document.getElementById(id);if(campo&&form.contains(campo))campo.value=valor??'';}
    });
  }
};
