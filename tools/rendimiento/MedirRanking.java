package com.senniaf.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.senniaf.match.controller.ScoringController;
import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Sonda local manual: repositorios en memoria, nunca abre una base de datos. */
public class MedirRanking {
    static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    static volatile Object sumidero;
    static final ThreadLocal<Map<String, Integer>> LLAMADAS = ThreadLocal.withInitial(TreeMap::new);
    final PerfilNino n = new PerfilNino();
    final List<PerfilFamilia> familias = new ArrayList<>();
    final List<EvaluacionPsicologica> informes = new ArrayList<>();
    final PesosScoring pesos = new PesosScoring();
    final AnalizadorTextoCompatibilidad texto = new AnalizadorTextoCompatibilidad();
    final ScoringService servicio;
    final MockMvc mvc;
    final Method calcular;
    final Object necesidades;
    final List<Object> capacidades = new ArrayList<>();

    MedirRanking(int cantidad) throws Exception {
        n.setCodigoCaso("SINTETICO-N"); n.setPerfilCompleto(true); n.setEdad(10);
        n.setUnidadEdad(UnidadEdad.ANIOS); n.setTieneHermanos(SiNo.SI); n.setCantidadHermanos(2);
        n.setCondicionesSalud(List.of(CondicionSalud.ASMA, CondicionSalud.DIABETES, CondicionSalud.TDAH));
        n.setAlergias(List.of(Alergia.ALIMENTARIA)); n.setIntoleranciaLactosa(RespuestaSiNoNoSabe.NO);
        n.setCondicionesSaludDetalle("Presenta asma y diabetes. Requiere acompañamiento por TDAH.");
        n.setNecesidadesApoyo("Requiere terapia psicológica. Necesita rutinas estables. Requiere apoyo emocional. "
                + "Necesita apoyo educativo individualizado. Requiere ayuda con las tareas. Necesita supervisión cotidiana.");
        var e = new EvaluacionPsicologica(); campo(e, "id", 1L); e.setNumeroVersion(1);
        e.setRecomendaciones("Se recomienda continuar terapia psicológica. Requiere apoyo durante su adaptación.");
        informes.add(e);
        for (int i=0; i<cantidad; i++) {
            var f = new PerfilFamilia(); f.setCodigoCaso(String.format("SINTETICO-F%04d",i));
            f.setPerfilCompleto(true); f.setEdadMinimaAceptada(1); f.setEdadMaximaAceptada(17);
            f.setDisposicionHermanos(DisposicionHermanos.DISPUESTA);
            f.setRedApoyo(List.of(FuenteApoyo.PAREJA)); f.setDisponibilidadViaje(DisponibilidadViaje.ALTA);
            f.setCondicionesPuedeAcompanarHoy(List.of(CondicionSalud.ASMA)); f.setCondicionesConApoyo(List.of(CondicionSalud.DIABETES));
            f.setNotaCondicionesSalud("Podemos acompañar asma. Podemos atender diabetes con apoyo. "
                    + "Podemos acompañar TDAH. Podemos cuidar alergia alimentaria. Podemos acompañarlo a terapia psicológica.");
            f.setNotaRedApoyo("Contamos con una red de apoyo. Podemos mantener rutinas estables. "
                    + "Podemos ofrecer apoyo emocional. Podemos ayudarlo con sus tareas. Podemos acompañar supervisión cotidiana.");
            f.setNotaEstructuraHogar("Podemos ofrecer apoyo educativo individualizado. Podemos acompañarlo en su adaptación.");
            f.setNotaHermanos("Podemos recibir a los hermanos juntos."); f.setNotaDistancia("Podemos viajar y recibir visitas.");
            familias.add(f);
        }
        servicio = new ScoringService(repo(PerfilNinoRepository.class),repo(PerfilFamiliaRepository.class),
                repo(DecisionComiteRepository.class),repo(PesosScoringRepository.class));
        campo(servicio,"evaluaciones",repo(EvaluacionPsicologicaRepository.class));
        campo(servicio,"vinculacionesRepo",repo(VinculacionRepository.class));
        campo(servicio,"excluidas",repo(CombinacionExcluidaRepository.class));
        campo(servicio,"interrupciones",repo(InterrupcionConvivenciaRepository.class));
        mvc = MockMvcBuilders.standaloneSetup(new ScoringController(servicio)).build();
        calcular = Arrays.stream(ScoringService.class.getDeclaredMethods())
                .filter(m->m.getName().equals("candidato") && m.getParameterCount()==5).findFirst().orElseThrow();
        calcular.setAccessible(true);
        necesidades=texto.necesidades(n,informes);
        for(var f:familias)capacidades.add(texto.capacidades(f));
    }
    static void campo(Object o,String nombre,Object valor) throws Exception {
        Field f=o.getClass().getDeclaredField(nombre); f.setAccessible(true);f.set(o,valor);
    }
    @SuppressWarnings("unchecked") <T> T repo(Class<T> clase) {
        return (T) Proxy.newProxyInstance(clase.getClassLoader(),new Class<?>[]{clase},(o,m,a)->{
            if(m.getDeclaringClass()==Object.class) return switch(m.getName()) {
                case "hashCode"->System.identityHashCode(o); case "equals"->o==a[0]; default->clase.getSimpleName();};
            LLAMADAS.get().merge(clase.getSimpleName()+"."+m.getName(),1,Integer::sum);
            if(clase==PerfilFamiliaRepository.class && m.getName().equals("findAll"))return familias;
            if(clase==PerfilNinoRepository.class && m.getName().equals("findAll"))return List.of(n);
            if(clase==EvaluacionPsicologicaRepository.class)return informes;
            if(clase==PesosScoringRepository.class)return Optional.of(pesos);
            if(m.getReturnType()==boolean.class)return false;
            if(m.getReturnType()==Optional.class)return Optional.empty();
            return List.of();
        });
    }
    Object ejecutar(String fase) throws Exception {
        return switch(fase) {
            case "ranking" -> servicio.casos();
            case "texto" -> {var a=texto.necesidades(n,informes);for(var f:familias)sumidero=texto.capacidades(f);yield a;}
            case "puntuacion" -> {Object ultimo=null; for(int i=0;i<familias.size();i++)
                ultimo=calcular.invoke(servicio,n,familias.get(i),pesos,necesidades,capacidades.get(i)); yield ultimo;}
            case "endpoint" -> {var r=mvc.perform(get("/api/scoring/casos").header("X-User-Role","COMITE")).andReturn();
                if(r.getResponse().getStatus()!=200)throw new IllegalStateException("HTTP "+r.getResponse().getStatus());
                yield r.getResponse().getContentAsByteArray();}
            default -> throw new IllegalArgumentException(fase);
        };
    }
    public static void main(String[] args) throws Exception {
        int cantidad=Integer.parseInt(args[0]);String version=args[1];Path salida=Path.of(args[2]);
        int fork=Integer.parseInt(args[3]); var s=new MedirRanking(cantidad);
        var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        var thread=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        List<Map<String,Object>> registros=new ArrayList<>();
        for(String fase:List.of("ranking","texto","puntuacion","endpoint")) {
            for(int i=0;i<12;i++)sumidero=s.ejecutar(fase);
            System.gc();for(var pool:ManagementFactory.getMemoryPoolMXBeans())pool.resetPeakUsage();
            for(int i=0;i<30;i++) {
                LLAMADAS.get().clear();long cpu=os.getProcessCpuTime();
                long alloc=thread.getThreadAllocatedBytes(Thread.currentThread().getId());long inicio=System.nanoTime();
                sumidero=s.ejecutar(fase);long duracion=System.nanoTime()-inicio;
                Map<String,Object> r=new LinkedHashMap<>();r.put("version",version);r.put("familias",cantidad);
                r.put("fork",fork);r.put("fase",fase);r.put("repeticion",i);r.put("ms",duracion/1e6);
                r.put("cpuMs",(os.getProcessCpuTime()-cpu)/1e6);
                r.put("bytesAsignados",thread.getThreadAllocatedBytes(Thread.currentThread().getId())-alloc);
                r.put("heapPicoBytes",ManagementFactory.getMemoryPoolMXBeans().stream().filter(p->p.getType()==MemoryType.HEAP)
                        .mapToLong(p->p.getPeakUsage().getUsed()).sum());
                r.put("llamadasRepositorio",new TreeMap<>(LLAMADAS.get()));registros.add(r);
            }
            if(fase.equals("endpoint"))Files.write(salida.resolve(version+"-"+cantidad+"-payload.json"),(byte[])sumidero);
        }
        var pool=Executors.newFixedThreadPool(4);
        for(int lote=0;lote<12;lote++) {
            var inicio=new CountDownLatch(1);List<Future<Map<String,Object>>> tareas=new ArrayList<>();
            for(int i=0;i<4;i++)tareas.add(pool.submit(()->{inicio.await();long t=System.nanoTime();
                Object r=s.ejecutar("endpoint");return Map.of("version",version,"familias",cantidad,"fork",fork,
                        "fase","concurrente4","ms",(System.nanoTime()-t)/1e6,"bytes",((byte[])r).length);}));
            inicio.countDown();for(var t:tareas){var r=t.get();if(lote>=2)registros.add(r);}
        }
        pool.shutdown();Files.writeString(salida.resolve(version+"-"+cantidad+"-"+fork+".json"),JSON.writeValueAsString(registros));
        Files.writeString(salida.resolve(version+"-"+cantidad+"-"+fork+"-vm.txt"),
                "java="+System.getProperty("java.version")+"\n"+Files.readString(Path.of("/proc/self/status")));
        System.out.println("FIN "+version+" "+cantidad+" fork="+fork);
    }
}
