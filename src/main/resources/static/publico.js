/* Mantiene la navegación pública sin duplicar los formularios de acceso existentes. */
(() => {
  const nav = document.querySelector('.publico-nav');
  if (!nav) return;
  const acceso = document.body.classList.contains('acceso-publico');
  const actualizar = () => {
    const registro = acceso && document.getElementById('vistaLogin')?.hidden;
    nav.querySelectorAll('a').forEach(a => {
      const activo = acceso ? a.dataset.acceso === (registro ? 'registro' : 'login')
        : a.getAttribute('href') === location.pathname.split('/').pop();
      if (activo) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
    });
  };
  if (acceso) {
    const abrir = () => {
      document.querySelector(`[data-ir="${location.hash === '#registro' ? 'vistaRegistro' : 'vistaLogin'}"]`)?.click();
      actualizar();
    };
    nav.querySelectorAll('[data-acceso]').forEach(a => a.addEventListener('click', e => {
      e.preventDefault(); history.pushState(null, '', a.getAttribute('href')); abrir();
    }));
    addEventListener('hashchange', abrir); addEventListener('popstate', abrir);
    const observer = new MutationObserver(actualizar);
    ['vistaLogin', 'vistaRegistro', 'vistaCodigo'].forEach(id => {
      const e = document.getElementById(id); if (e) observer.observe(e, { attributes: true, attributeFilter: ['hidden'] });
    });
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', abrir, { once: true }); else abrir();
  }
  actualizar();
})();
