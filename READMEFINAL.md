# Hackathon #1: Oreo Insight Factory 🍪
## Integrantes:
| Integrante                 | Código UTEC | 
| -------------------------- | ----------- | 
| Rafael Choque              | 202410378   | 
| Diego Escajadillo          | 201910150   | 
| Mauricio Escajadillo       | 202210466   | 
| Isabella Castillo          | 202410084   | 

## Instrucciones para ejecutar el proyecto:

### ⚙️ Requisitos
- Java 21+
- Maven 3.6+
- IntelliJ IDEA (recomendado)
- Git
### Cómo ejecutar?

### 1) Clonar y entrar al proyecto
```bash
git clone git@github.com:mau-esc27/Hack1.git
cd Hack1

```
### 2) Abrir en IntelliJ
File → Open → Seleccionar la carpeta del proyecto
Esperar a que Maven descargue las dependencias automáticame

### 3) Configurar variables de entorno
   ✅ YA CONFIGURADO - No necesita cambios

El archivo application.properties ya incluye:
- JWT Secret configurado
- GitHub Token funcional
- Todo listo para usar
  github.models.token=TOKEN_OCULTO_PARA_SEGURIDAD
### 4) Ejecutar la aplicación
```bash
mvn spring-boot:run

```
### 5) Verificar que esté funcionando
- API: http://localhost:8080
- Health Check: http://localhost:8080/health
- H2 Console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:oredb)
---
## Instrucciones para correr el Postman workflow:
- Descargar OreoInsightFactory.postman_collection.json del repositorio
- En Postman: Import → Select File → Elegir el archivo JSON
- Configurar environment (opcional):
  - Crear variable: base_url = http://localhost:8080

- Ejecutar Tests:
  - Asegurar que la aplicación esté corriendo en http://localhost:8080
  - Abrir la colección en Postman
  - Click en "Run collection"
  - Verificar que todos los tests pasen (✅):
    - ✅ Registro y login de usuarios CENTRAL y BRANCH 
    - ✅ Creación de ventas con diferentes sucursales 
    - ✅ Validación de permisos por roles 
    - ✅ Solicitud de reportes asíncronos 
    - ✅ Tests de seguridad entre sucursales
  
---
## Explicación de la implementación asíncrona:

### 🏗️ Arquitectura Event-Driven
Implementamos un sistema **asíncrono basado en eventos** usando Spring Boot que permite procesar reportes complejos en segundo plano sin bloquear al cliente.

## 🔄 Flujo Asíncrono Completo
```
Cliente → POST /sales/summary/weekly → 202 ACCEPTED (Inmediato)
                     ↓
           [ApplicationEventPublisher]
                     ↓
         [ReportRequestedEvent + @Async Listener]
                     ↓
    [Cálculo de Agregados → GitHub Models → Email → ✅]
```

## 🛠️ Componentes Específicos Implementados

### 1. **SalesControlller - Receptor de Solicitudes**
```java
@PostMapping("/summary/weekly")
public ResponseEntity<SummaryResponseDTO> requestWeeklySummary(@Valid @RequestBody SummaryRequestDTO req) {
    // Validación de permisos y persistencia inicial
    ReportRequest rr = reportRequestRepository.save(reportRequest);
    
    // Publicación inmediata del evento asíncrono
    eventPublisher.publishEvent(new ReportRequestedEvent(
        rr.getId(), from, to, branch, emailTo, username
    ));
    
    // Respuesta inmediata 202 ACCEPTED
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
}
```

### 2. **ReportRequestedEvent - Mensaje Asíncrono**
```java
public class ReportRequestedEvent {
    private final String requestId;      // ID único para tracking
    private final LocalDate from;        // Fecha inicio
    private final LocalDate to;          // Fecha fin  
    private final String branch;         // Sucursal objetivo
    private final String emailTo;        // Destinatario del reporte
    private final String requestedBy;    // Usuario solicitante
}
```

### 3. **ReportEventListeners - Procesador Asíncrono**
```java
@Async("taskExecutor")
@EventListener
public void handleReportRequest(ReportRequestedEvent event) {
    // Procesamiento en background con 3 etapas:
    // 1. SalesAggregationService - Cálculo de métricas
    // 2. GithubModelsClient - Generación de resumen con IA
    // 3. EmailService - Envío de reporte final
}
```

### 4. **AsyncConfig - Configuración del Thread Pool**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);     // Hilos base siempre activos
        executor.setMaxPoolSize(10);     // Máximo bajo carga
        executor.setQueueCapacity(50);   // Cola para picos de demanda
        executor.setThreadNamePrefix("async-");
        return executor;
    }
}
```

## ✅ Beneficios Técnicos Logrados

- **⏱️ Respuesta Inmediata**: Cliente recibe `202 ACCEPTED` en <100ms
- **🚀 Escalabilidad Horizontal**: Thread pool maneja múltiples reportes simultáneos
- **🛡️ Resiliencia**: Fallos en GitHub Models o Email no afectan la API principal
- **📊 Persistencia de Estado**: `ReportRequestRepository` trackea estado de cada solicitud
- **🎯 Seguimiento**: `requestId` permite monitoreo del progreso

## 🔧 Características de Implementación

### **Procesamiento en 3 Etapas:**
1. **Agregación**: Cálculo de total units, revenue, top SKU y branch
2. **Generación IA**: Integración con GitHub Models para resumen en lenguaje natural
3. **Distribución**: Envío de email con reporte formateado

### **Manejo de Permisos:**
- Usuarios `BRANCH` restringidos a su sucursal asignada
- Validación automática de permisos antes del procesamiento
- Seguridad mantenida durante todo el flujo asíncrono

### **Configuración de Performance:**
```properties
Core Pool: 4 hilos
Max Pool: 10 hilos  
Queue Capacity: 50 solicitudes
Thread Naming: async-* para debugging
```

**Los usuarios experimentan confirmación inmediata mientras el sistema procesa inteligentemente en background, entregando reportes detallados por email.** 📧✨
