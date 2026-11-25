# Registro de Decisiones Arquitectónicas y Técnicas (ADR)
## Cards for democraXSSy — Curso IF-6000 Redes en los Negocios
**Universidad de Costa Rica · Escuela de Ciencias de la Computación e Informática**  
**Equipo de Proyecto:** José Andrés · Joel · Paolo

---

## 1. Spring Boot (Java 21) como stack del backend

- **Contexto:** Se requería implementar el backend para el juego *Cards for democraXSSy* cumpliendo estrictamente con el contrato OpenAPI 3.1.0 (`openapi.yaml`). El servicio debía manejar peticiones HTTP/REST, aplicar filtros de seguridad globales (CORS de alta precedencia), validar solicitudes rigurosamente y empaquetarse de forma autónoma para ejecutarse en una máquina virtual Linux headless con recursos limitados.
- **Opciones evaluadas:**
  - **Opción A (Node.js / Express o Fastify):** Provee ligereza y modelo asíncrono no bloqueante, pero su tipado dinámico en JavaScript o transpilado en TypeScript añade fricción para el control estricto de tipos en tiempo de ejecución. Además, el manejo de concurrencia y mutabilidad de estados de juego compartidos en memoria requiere cuidados adicionales para evitar condiciones de carrera.
  - **Opción B (Python / FastAPI):** Desarrollo ágil y validación integrada con Pydantic, pero el Global Interpreter Lock (GIL) y el manejo de hilos para partidas concurrentes en memoria introduce complejidad de arquitectura; asimismo, presenta mayor consumo relativo de memoria en comparación con un runtime precompilado optimizado.
  - **Opción C (Spring Boot 4.2.0 con Java 21 LTS y Maven):** Ecosistema empresarial maduro, soporte de tipado estático robusto, motor de inyección de dependencias desacoplado, filtros de servlet (`OncePerRequestFilter`) para control total del ciclo de vida de peticiones y empaquetado autocontenido en un *fat JAR* ejecutable mediante `spring-boot-maven-plugin`.
- **Decisión:** Se seleccionó **Spring Boot 4.2.0 sobre Java 21 LTS** con Maven y el módulo `spring-boot-starter-webmvc`.
- **Justificación:** Java 21 LTS ofrece optimizaciones de rendimiento y de gestión de memoria clave para entornos restringidos. Spring Boot permite configurar de manera declarativa y segura filtros HTTP prioritarios (`CorsFilter` con `Ordered.HIGHEST_PRECEDENCE`), desacoplar controladores y servicios, y garantizar estabilidad en producción corriendo como servicio systemd con un consumo de heap acotado (`-Xmx256m`).
- **Evidencia:**
  - `backend/pom.xml`: declaración de `<java.version>21</java.version>`, dependencia `spring-boot-starter-webmvc` y versión del padre `spring-boot-starter-parent` `4.2.0`.
  - `backend/src/main/java/org/democraxssy/DemocraxssyBackendApplication.java`: clase principal de arranque de Spring Boot.
  - `backend/src/main/java/org/democraxssy/salud/SaludController.java`: implementación de los métodos `HEAD` y `GET` para `/api/salud`.
  - `backend/src/main/java/org/democraxssy/web/CorsFilter.java`: filtro de servlet con `@Order(Ordered.HIGHEST_PRECEDENCE)` y ejecución forzada en despachos de error (`shouldNotFilterErrorDispatch() = false`).
- **Responsable:** José Andrés.

---

## 2. Estado en memoria, sin base de datos

- **Contexto:** Las partidas de *Cards for democraXSSy* tienen una naturaleza intrínsecamente efímera: se crean dinámicamente, admiten entre 4 y 22 jugadores (en número par), se desarrollan a lo largo de rondas continuas de votación durante unos 10 a 20 minutos, y se destruyen al finalizar o por inactividad. El entorno de ejecución en la nube es una máquina virtual `VM.Standard.E2.1.Micro` de Oracle Cloud Infrastructure (OCI) que dispone de únicamente 1 GB de memoria RAM física total.
- **Opciones evaluadas:**
  - **Opción A (Base de datos relacional PostgreSQL / MySQL o embebida H2/SQLite):** Pros: persistencia duradera de datos ante reinicios. Contras: sobrecarga innecesaria de I/O en disco, consumo excesivo de memoria RAM (un motor como PostgreSQL o MySQL consume entre 250 MB y 400 MB en reposo, inviabilizando la VM de 1 GB) y complejidad accidental en migraciones de esquema para datos que pierden valor una vez concluida la partida.
  - **Opción B (Base de datos en memoria externa como Redis):** Pros: alta velocidad y expiración nativa mediante TTL. Contras: introduce un proceso adicional de sistema operativo compitiendo por los escasos recursos de la VM micro y añade un punto extra de falla en la arquitectura.
  - **Opción C (Estado puramente en memoria dentro del proceso de la JVM):** Pros: acceso en microsegundos, cero sobrecarga de red o I/O, control de concurrencia mediante estructuras de datos concurrentes (`ConcurrentHashMap`), recolección de basura automática y limpieza programada de partidas inactivas. Contras: pérdida de partidas activas si el proceso se reinicia (escenario aceptable para un juego de salón efímero).
- **Decisión:** Se eligió un **modelo de estado completamente en memoria** gestionado dentro de la JVM de Spring Boot, sin base de datos relacional, NoSQL ni almacenamiento secundario.
- **Justificación:** Es la solución más eficiente para operar holgadamente dentro de los estrictos límites de la capa gratuita (Always Free) de OCI. Permite configurar la JVM con un heap máximo de 256 MB (`-Xmx256m`) y recolector serial (`-XX:+UseSerialGC`), garantizando que el servicio completo se mantenga por debajo del tope de memoria de 500 MB impuesto por cgroups en systemd, dejando suficiente RAM libre para el sistema operativo y el servidor web Nginx.
- **Evidencia:**
  - `backend/pom.xml`: ausencia deliberada de dependencias como `spring-boot-starter-data-jpa`, Hibernate, JDBC o conectores SQL; descripción explícita: `"Backend del juego democraXSSy (Spring Boot, estado en memoria)"`.
  - `infra/cloud-init.yaml`: unidad de servicio `/etc/systemd/system/democraxssy-back.service` con directivas `ExecStart=/usr/bin/java -Xmx${jvm_max_heap} -Xss512k -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -jar ...` y `MemoryMax=500M`.
  - `openapi.yaml`: ciclo de vida transitorio de partidas (`POST /api/partidas` para instanciar y `DELETE /api/partidas/{id}` para descartar).
- **Responsable:** José Andrés.

---

## 3. Regla del HTTP 409 y validación estricta del contrato

- **Contexto:** La especificación de la cátedra en `openapi.yaml` impone un mecanismo riguroso y no convencional de rechazo de solicitudes: cualquier petición que envíe parámetros de consulta (*query parameters*), cabeceras no autorizadas, formatos de token incorrectos, cabeceras de sobreescritura de método HTTP o cuerpos JSON con propiedades adicionales, desconocidas o repetidas debe responder inmediatamente con el código de estado **HTTP 409 (Conflict)** y un cuerpo JSON con el esquema `Error`, en lugar de los códigos estándar HTTP 400 o 422.
- **Opciones evaluadas:**
  - **Opción A (Validación estándar permisiva de frameworks):** Frameworks como Spring MVC o Express ignoran por defecto parámetros de consulta no mapeados y descartan campos JSON desconocidos silenciosamente. Pros: comportamiento estándar en la web comercial. Contras: incumplimiento absoluto del contrato `openapi.yaml`, lo cual causaría la reprobación inmediata en las pruebas automatizadas de la cátedra.
  - **Opción B (Validación estricta a nivel de filtro, Jackson e interceptor):** Configurar el deserializador JSON para fallar ante cualquier propiedad desconocida (`DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES`), interceptar cabeceras prohibidas, auditar parámetros de consulta en cada solicitud y mapear todas estas infracciones hacia HTTP 409. Pros: cumplimiento del 100 % del contrato formal. Contras: requiere disciplina de desarrollo y alineación simétrica en el cliente frontend para no enviar cabeceras automáticas de librerías HTTP.
- **Decisión:** Implementar **validación estricta integral orientada a HTTP 409** en el backend y diseñar el cliente frontend (`frontend/src/api/client.ts`) para adherirse minuciosamente a estas restricciones.
  - Cero parámetros de consulta en la totalidad de las rutas de la API.
  - Cabecera `Authorization` restringida exclusivamente a la estructura `Bearer <token>` de 64 caracteres hexadecimales en minúscula (`^[0-9a-f]{64}$`), prohibida en endpoints públicos (`HEAD /api/salud`, `POST /api/partidas`, `POST /api/partidas/{id}/jugadores`).
  - Rechazo tajante de cabeceras de sobreescritura de método: `X-HTTP-Method-Override`, `X-HTTP-Method` y `X-Method-Override`.
  - Exigencia estricta de `Content-Type: application/json` únicamente cuando hay cuerpo; rechazo de cuerpo y de `Content-Type` en peticiones `HEAD`, `GET` y `DELETE`.
  - Validación de objetos JSON sin campos desconocidos ni duplicados.
- **Justificación:** Asegura la interoperabilidad exacta con los clientes evaluadores del curso IF-6000 y garantiza que el sistema respete la arquitectura de seguridad y diseño de protocolos exigida por la cátedra.
- **Evidencia:**
  - `openapi.yaml`: sección "Validación de solicitudes" (líneas 28-39) y definición del componente `#/components/responses/Conflicto`.
  - `frontend/src/api/client.ts`: función `aceptaAuth()` (líneas 42-48) para evitar inyectar `Authorization` en rutas públicas, y creación de instancia Axios limpia sin cabeceras por defecto en línea 63 (`const instance = axios.create()`).
  - `infra/cloud-init.yaml`: bloque de Nginx (líneas 68-73) que intencionalmente no añade directivas `proxy_set_header X-Forwarded-For` ni similares, para que Nginx no introduzca cabeceras adicionales que disparen el 409 en el backend.
- **Responsable:** José Andrés.

---

## 4. Terraform en lugar de OCI Container Instances

- **Contexto:** Se debía implementar y automatizar la infraestructura del backend en Oracle Cloud Infrastructure. Al inicio se evaluó el servicio *OCI Container Instances* (utilizado en el Laboratorio #6 del curso) para ejecutar el backend como contenedor sin administrar servidores. Era imperativo contrastar esta alternativa contra las restricciones de suscripción, cuotas y requerimientos de red del proyecto.
- **Opciones evaluadas:**
  - **Opción A (OCI Container Instances):** Servicio administrado *serverless* de contenedores. Tras la evaluación técnica formal (`infra/evidencia/01-container-instances-evaluacion.md`), se comprobó que la forma x86 `CI.Standard.E4.Flex` tiene cuota 0 (`standard-e4-core-count = 0`) en la cuenta; únicamente estaba habilitada la forma ARM `CI.Standard.A1.Flex`, la cual consume el crédito de bienvenida de \$300. Además, asigna direcciones IP efímeras en cada recreación (obligando a reprogramar DNS manualmente en cada despliegue), no cuenta con terminación HTTPS integrada (exigiendo un contenedor *sidecar* adicional con riesgo de pérdida de certificados) y requiere compilación cruzada hacia arquitectura `arm64`.
  - **Opción B (Configuración manual en consola web de OCI):** Rápida de iniciar, pero carece de reproducibilidad, es propensa a errores humanos y no cumple con el requerimiento académico de implementar Infraestructura como Código (IaC).
  - **Opción C (Infraestructura como Código con Terraform y VM Compute):** Declaración formal de recursos en archivos HCL (`compute.tf`, `public-ip.tf`), aprovisionamiento automatizado mediante `cloud-init.yaml` y asociación de una IP pública reservada estática.
- **Decisión:** Descartar OCI Container Instances y optar por **Terraform con el proveedor oficial `oracle/oci`**, desplegando una máquina virtual `VM.Standard.E2.1.Micro` Always Free configurada íntegramente mediante `cloud-init`.
- **Justificación:** Cero costo recurrente garantizado al usar la forma Always Free `VM.Standard.E2.1.Micro`; estabilidad de red gracias a la asignación de una IP pública reservada que no cambia entre despliegues; arquitectura x86_64 nativa; y cumplimiento de las directrices de DevOps validadas y aprobadas explícitamente por el profesor el 19 de noviembre de 2026.
- **Evidencia:**
  - `infra/evidencia/01-container-instances-evaluacion.md`: reporte técnico completo con comandos `oci limits` y prueba de humo realizada el 14-15 de noviembre de 2026.
  - `infra/compute.tf`, `infra/public-ip.tf`, `infra/provider.tf` y `infra/versions.tf`.
  - `infra/evidencia/02-terraform-plan.txt` y `infra/evidencia/03-terraform-apply.txt`.
  - `infra/evidencia/capturas/07-terraform-apply.png`.
- **Responsable:** Joel.

---

## 5. Segunda VM Always Free separada del frontend

- **Contexto:** La capa Always Free de Oracle Cloud permite a cada arrendamiento (*tenancy*) contar con hasta dos instancias de cómputo basadas en la arquitectura AMD `VM.Standard.E2.1.Micro` (1 OCPU, 1 GB de RAM cada una). Con el frontend ya alojado y en funcionamiento en la primera de estas VMs, se debía determinar la topología de despliegue para el nuevo backend de Spring Boot.
- **Opciones evaluadas:**
  - **Opción A (Monolito en una única VM compartida):** Hospedar frontend (Nginx estático) y backend (JVM Java 21) en la misma máquina virtual. Pros: menor cantidad de nodos gestionados. Contras: contención severa de recursos; 1 GB de memoria RAM es insuficiente para sostener simultáneamente el sistema operativo Oracle Linux 9, Nginx, Certbot y la JVM de Spring Boot (la cual requiere entre 256 MB y 500 MB), provocando la activación letal del *OOM Killer*. Asimismo, acopla los ciclos de despliegue y reinicios de ambos componentes.
  - **Opción B (Instancia ARM Ampere `VM.Standard.A1.Flex`):** Mayor capacidad (hasta 4 OCPUs y 24 GB de RAM en capa gratuita). Contras: agotamiento crónico de capacidad (*Out of capacity*) en los tres dominios de disponibilidad de la región `us-ashburn-1`, impidiendo su aprovisionamiento de manera predecible.
  - **Opción C (Segunda VM `VM.Standard.E2.1.Micro` dedicada e independiente):** Aprovisionar la segunda máquina virtual gratuita permitida por la suscripción (`democraxssy-back-e2`), asociándole una IP pública reservada propia y reutilizando la infraestructura de red preexistente (VCN y subred pública).
- **Decisión:** Desplegar una **segunda máquina virtual independiente `VM.Standard.E2.1.Micro`** dedicada de forma exclusiva al backend, conectada a la subred pública `10.0.8.0/24` de la VCN `if6000`.
- **Justificación:** Brinda aislamiento total de fallos y de recursos entre capas de presentación y de negocio; aprovecha al 100 % el cupo de capa gratuita de OCI sin costos imprevistos; desacopla el ciclo de vida del backend respecto al frontend; y garantiza que el gigabyte completo de memoria esté dedicado a la JVM y al reverse proxy Nginx.
- **Evidencia:**
  - `infra/compute.tf`: recurso `oci_core_instance.backend_instance` con shape `"VM.Standard.E2.1.Micro"` y hostname `"democraxssy-back"`.
  - `infra/public-ip.tf`: recurso `oci_core_public_ip.backend_ip` con `lifetime = "RESERVED"`, asignando la IP pública fija `157.151.151.205`.
  - `infra/variables.tf`: configuración de variables `compartment_ocid` (`IF6000-26-GUI`) y `subnet_ocid` (`10.0.8.0/24`).
  - `infra/evidencia/capturas/01-oci-instancias.png` (muestra activas `instancia-front` y `democraxssy-back-e2`) y `infra/evidencia/capturas/03-oci-ip-reservada.png`.
- **Responsable:** Joel.

---

## 6. Subdominio propio de la API en DuckDNS y soporte HTTP sin HSTS

- **Contexto:** Para la evaluación de la cátedra es un requisito fundamental demostrar que el cliente puede comunicarse con el backend tanto por HTTPS como por HTTP plano (empleando el proxy de Nginx). Si el servidor backend impusiera la cabecera `Strict-Transport-Security` (HSTS), los navegadores web forzarían internamente la actualización de cualquier solicitud `http://` hacia `https://`, imposibilitando la demostración del transporte HTTP plano.
- **Opciones evaluadas:**
  - **Opción A (Compartir dominio y prefijos con el frontend bajo HSTS global):** Utilizar `democraxssy-if6000.duckdns.org/api` con HSTS activado (`max-age=86400`). Contras: el navegador recuerda la política HSTS para todo el dominio y subdominios, bloqueando permanentemente cualquier intento de conexión HTTP en texto claro.
  - **Opción B (Acceso directo por dirección IP en HTTP):** Intentar conectar a `http://157.151.151.205/`. Contras: Nginx rechaza conexiones por IP directa por motivos de seguridad (`return 444` en el bloque `server_name _`), y los certificados TLS no validan sobre nombres no coincidentes.
  - **Opción C (Subdominio dedicado en DuckDNS con soporte HTTP y HTTPS sin directiva HSTS):** Crear el subdominio `api-democraxssy-if6000.duckdns.org` vinculado a la IP reservada del backend, configurando Nginx para escuchar en los puertos 80 y 443 de forma simétrica sin redirección 301 obligatoria y omitiendo de forma intencional la cabecera HSTS en el bloque HTTP.
- **Decisión:** Registrar el subdominio **`api-democraxssy-if6000.duckdns.org` en DuckDNS** y configurar Nginx para servir la API en HTTP (puerto 80) y HTTPS (puerto 443) **sin redirección automática ni HSTS**.
- **Justificación:** Cumple a cabalidad con la rúbrica de evaluación al permitir contrastar en vivo una llamada directa cifrada por HTTPS contra una llamada en texto plano por HTTP a través del proxy de mitigación de contenido mixto, sin que los mecanismos defensivos del navegador interfieran en la demostración académica.
- **Evidencia:**
  - `infra/README.md`: sección "Diseño de Nginx" (documentando la ausencia deliberada de HSTS en el backend).
  - `infra/cloud-init.yaml`: bloque de configuración `/etc/nginx/conf.d/api.conf` (líneas 53-59), donde el puerto 80 no contiene `return 301 https://...` ni directivas `Strict-Transport-Security`.
  - `infra/evidencia/05-estado-actual.txt`: capturas de respuestas curl en puerto 80 (`HTTP/1.1 200 OK`) y en puerto 443 (`HTTP/1.1 200 OK`) respondiendo con cabeceras idénticas y sin redirección.
  - `infra/evidencia/capturas/05-duckdns-subdominio.png`.
- **Responsable:** Joel.

---

## 7. HTTP y HTTPS con Nginx y Let's Encrypt (Certbot)

- **Contexto:** El proceso de backend en Spring Boot escucha localmente en `127.0.0.1:8080`. Se requería exponer este servicio de forma pública en Internet protegiéndolo de accesos indebidos, gestionando la terminación TLS con certificados válidos y reconocidos por navegadores (evitando advertencias de seguridad), aplicando limitación de tasa (*rate limiting*) y asegurando la renovación automática de los certificados dentro de una máquina virtual con apenas 1 GB de RAM.
- **Opciones evaluadas:**
  - **Opción A (Terminación TLS directa en Tomcat / Spring Boot):** Configurar almacenes de claves PKCS12 / JKS directamente en la aplicación Java. Pros: prescinde de un proxy intermediario. Contras: incrementa el consumo de CPU y memoria de la JVM al procesar el cifrado/descifrado TLS, dificulta la gestión de los desafíos ACME de Let's Encrypt y carece de funcionalidades avanzadas como *rate limiting* por IP o descarte inmediato de tráfico malicioso.
  - **Opción B (Caddy Server como reverse proxy):** Caddy gestiona TLS automáticamente en un solo binario. Pros: simplicidad extrema de configuración. Contras: binario compilado en Go que no forma parte de los repositorios BaseOS de Oracle Linux 9; requiere repositorios de terceros y consume más memoria en reposo que Nginx.
  - **Opción C (Nginx + Certbot en entorno virtual Python aislado):** Utilizar Nginx como reverse proxy liviano y compilar Certbot en un `venv` aislado (`/opt/certbot`) mediante `pip`, prescindiendo de los repositorios EPEL (cuyos metadatos de más de 200 MB agotaban la RAM de la máquina virtual durante la instalación).
- **Decisión:** Emplear **Nginx como reverse proxy frontal en puertos 80 y 443** reenviando a `127.0.0.1:8080`, integrando certificados gratuitos de **Let's Encrypt mediante Certbot (instalado en venv)** y un cron job para renovación desatendida.
- **Justificación:** Nginx tiene una huella en memoria casi imperceptible (menos de 15 MB de RAM); la instalación de Certbot vía `pip` en `/opt/certbot` solventó el bloqueo de agotamiento de memoria detectado en el diagnóstico inicial de `cloud-init`; la zona de *rate limiting* (`limit_req_zone $binary_remote_addr zone=api:10m rate=20r/s`) previene abusos de denegación de servicio; y el cron job en `/etc/cron.d/certbot-renew` garantiza alta disponibilidad continua sin intervención manual.
- **Evidencia:**
  - `infra/cloud-init.yaml`:
    - Instalación de Nginx mediante repositorios oficiales de Oracle Linux 9 (`--enablerepo=ol9_baseos_latest --enablerepo=ol9_appstream`).
    - Despliegue de Certbot en venv (`python3 -m venv /opt/certbot` y `/opt/certbot/bin/pip install certbot certbot-nginx`).
    - Cron job en `/etc/cron.d/certbot-renew`: `17 3,15 * * * root /usr/bin/certbot renew -q`.
    - Configuración de proxy y rate limiting en `/etc/nginx/nginx.conf` y `/etc/nginx/conf.d/api.conf`.
  - `infra/evidencia/04-cloud-init-diagnostico.md`: diagnóstico del problema de memoria con repositorios EPEL y justificación del venv.
  - `infra/evidencia/05-estado-actual.txt`: certificado TLS verificado (`subject=CN=api-democraxssy-if6000.duckdns.org`, `issuer=Let's Encrypt, CN=YE1`) y estado de servicios activo.
  - `infra/evidencia/capturas/06-navegador-https.png`.
- **Responsable:** Joel.

---

## 8. Proxy anti-SSRF del frontend y comunicación con el backend HTTP

- **Contexto:** El cliente web (frontend) se sirve en producción obligatoriamente a través de HTTPS (`https://democraxssy-if6000.duckdns.org`). Cuando un usuario o evaluador selecciona una URL de backend en protocolo HTTP plano (`http://...`), los navegadores bloquean la solicitud debido a la política de seguridad de *Mixed Content* (contenido mixto activo). Para posibilitar este tráfico se requería un proxy reverso, pero abrir un proxy arbitrario introduce un vector crítico de seguridad: *Server-Side Request Forgery* (SSRF), el cual podría permitir a un atacante escanear la red interna de OCI o sustraer credenciales del servicio de metadatos de la nube.
- **Opciones evaluadas:**
  - **Opción A (Permitir conexión directa HTTP desde el cliente):** Dejar que el navegador realice la petición directa. Pros: cero lógica en el servidor web. Contras: falla invariablemente en el 100 % de los navegadores por bloqueo de Mixed Content.
  - **Opción B (Proxy abierto sin restricciones en Nginx):** Reenviar cualquier tráfico hacia el host solicitado en la URL. Pros: configuración trivial. Contras: vulnerabilidad de seguridad crítica (SSRF) que expondría el endpoint de metadatos de OCI (`169.254.169.254`), interfaces locales (`127.0.0.1`), y recursos privados de la VCN.
  - **Opción C (Proxy bajo ruta dedicada `/api/http/` con doble barrera anti-SSRF en Nginx y nftables, replicado en desarrollo con Vite):** Resolver la URL en el cliente transformando `http://host:puerto/ruta` en `/api/http/host:puerto/ruta`, implementar un plugin middleware en `vite.config.ts` para desarrollo local, y en producción desplegar dos capas de contención: lista negra de destinos en Nginx y reglas de filtrado de paquetes a nivel de kernel mediante `nftables` asociadas al usuario del sistema `nginx`.
- **Decisión:** Implementar la arquitectura de **proxy `/api/http/` con resolución automática en el cliente y doble anillo de protección anti-SSRF** en producción (Nginx + nftables), complementado con el middleware dev en Vite.
- **Justificación:** Resuelve de manera transparente y estándar el problema de contenido mixto sin degradar la seguridad del servidor web; impide ataques SSRF tanto a nivel de aplicación (Nginx bloquea dominios e IPs reservadas/privadas y metadatos de OCI) como a nivel de red/kernel (las reglas de `nftables` impiden que el usuario `nginx` establezca sockets hacia redes RFC 1918 aunque un DNS malicioso resuelva un nombre público hacia una IP interna); y garantiza paridad exacta entre el entorno de desarrollo (`npm run dev`) y producción.
- **Evidencia:**
  - `frontend/src/api/client.ts`: función `resolveBaseUrl()` (líneas 53-60), la cual reescribe URLs `http://` hacia `/api/http/${u.host}${u.pathname}`.
  - `frontend/vite.config.ts`: función `proxyHttpDev()` (líneas 14-42) que replica el comportamiento de Nginx en local usando `node:http`.
  - `docs/ESTADO_DEL_PROYECTO.md`: sección 1 y sección 5 ("Proxy anti-SSRF con lista de destinos prohibidos en Nginx y regla de nftables para el usuario nginx").
- **Responsable:** Joel.

---

## 9. Mazos propios y reglas de contenido y sanitización

- **Contexto:** El juego requiere un volumen suficiente de cartas para sostener partidas fluidas y no repetitivas de hasta 22 jugadores simultáneos. La cátedra exige un mazo original con cotas mínimas obligatorias (al menos 50 cartas negras y al menos 250 cartas blancas), restricciones de longitud (máximo 140 caracteres en negras y 80 en blancas) y soporte para cartas de respuesta compuesta (`elegir: 2`). Adicionalmente, el humor y la temática del juego giran en torno a ciberseguridad, inyecciones de código y política, por lo que nombres y cartas incluyen caracteres como `<`, `>`, comillas e incluso etiquetas `<script>`, exigiendo un protocolo infranqueable contra vulnerabilidades de Cross-Site Scripting (XSS).
- **Opciones evaluadas:**
  - **Opción A (Reutilizar un mazo genérico de Cards Against Humanity descargado de internet):** Pros: mínimo esfuerzo. Contras: temáticas ajenas a los conceptos del curso de Redes y Seguridad, ausencia de cartas con `elegir: 2`, incumplimiento de las longitudes máximas y riesgo de plagio académico.
  - **Opción B (Generación sintética sin validación estricta de XSS en frontend):** Creación de cartas con renderizado permisivo (`innerHTML`). Contras: vulnerabilidad de ejecución remota de scripts en los navegadores de los clientes al renderizar cartas maliciosas.
  - **Opción C (Mazo temático original propio y sanitización por construcción en React):** Redacción original de un mazo contextualizado en redes, administración de sistemas, ciberseguridad y burocracia estatal; almacenamiento estático en `cartas.json` respetando las longitudes límite; y política estricta de renderizado como texto plano puro en la interfaz gráfica.
- **Decisión:** Diseñar e incorporar el **mazo temático original en `backend/src/main/resources/mazos/cartas.json`** con **55 cartas negras** (38 de `elegir: 1` y 17 de `elegir: 2`) y **260 cartas blancas**, asegurando una longitud máxima observada de 80 caracteres en negras y 76 en blancas; complementado con la regla de **sanitización por escape nativo de React** (prohibición total de `dangerouslySetInnerHTML`).
- **Justificación:** Supera ampliamente los requerimientos cuantitativos de la cátedra (≥50 negras y ≥250 blancas); enriquece la experiencia académica y lúdica del juego con sátiras sobre protocolos, ataques y auditorías de TI; y neutraliza de raíz cualquier ataque XSS, garantizando que cadenas como `<script>alert(1)</script>` o `<svg onload=alert(1)>` se muestren inofensivamente como texto plano en las cartas, la mesa y la barra de jugadores.
- **Evidencia:**
  - `backend/src/main/resources/mazos/cartas.json`: archivo de datos con 55 cartas negras y 260 cartas blancas integrado mediante PR #12.
  - `openapi.yaml`: sección "Seguridad para clientes" (líneas 46-49): *"Nombres, cartas y frases pueden contener los caracteres < y >, e incluso etiquetas de script... Muéstralos siempre como texto plano, nunca como HTML"*.
  - `docs/ESTADO_DEL_PROYECTO.md`: sección 1 ("XSS: nombres y cartas se muestran siempre como texto. React escapa solo; no se usa dangerouslySetInnerHTML en ningún lado").
  - `docs/PENDIENTES_FRONTEND.md`: pruebas registradas el 26 de octubre verificando la inocuidad de payloads como `<svg onload=alert(1)>`.
- **Responsable:** Paolo.

---

## 10. Botones rápidos de servidor e indicador de conexión directa vs. proxy

- **Contexto:** En la pantalla inicial de configuración, los usuarios y evaluadores deben poder alternar con rapidez entre distintos servidores de prueba (el backend propio en producción vía HTTPS, el backend propio vía HTTP a través del proxy y el servidor oficial del profesor). La introducción manual de URLs en un campo de texto era propensa a erratas y entorpecía las demostraciones en vivo. Asimismo, resultaba crítico reflejar de forma inequívoca el mecanismo de transporte activo (conexión directa vs. proxy anti-SSRF) sin discrepancias entre el indicador visual y el comportamiento real del cliente de red.
- **Opciones evaluadas:**
  - **Opción A (Entrada manual exclusiva mediante campo de texto libre):** Pros: interfaz austera y mínimo código. Contras: fricción operativa alta, errores frecuentes de digitación y falta de visibilidad técnica durante las pruebas del profesor.
  - **Opción B (Botones de servidor desacoplados y cálculo redundante de transporte):** Dispersar las URLs en múltiples componentes y reimplementar la detección de HTTP/HTTPS con expresiones regulares independientes en la UI. Contras: riesgo de divergencia (*split-brain*), donde la interfaz afirme estar en modo "Directo" mientras el interceptor de red enruta la petición por el proxy `/api/http/`.
  - **Opción C (Presets centralizados en módulo de configuración y revalidación reactiva):** Centralizar las definiciones en `frontend/src/config/servers.ts`, renderizar botones de acceso rápido que actualizan el formulario con revalidación (`setValue` con `{ shouldValidate: true }`) en `Configuracion.tsx` y utilizar la misma lógica unificada de `resolveBaseUrl()` de `frontend/src/api/client.ts` para determinar el modo de transporte.
- **Decisión:** Implementar **botones rápidos de servidor centralizados en `SERVER_PRESETS`** dentro de `frontend/src/config/servers.ts`, integrados en `Configuracion.tsx` con limpieza reactiva de errores de validación, y basar cualquier indicador de transporte directamente en el comportamiento canónico de `resolveBaseUrl()`.  
  *[Confirmar detalle final con José/Joel en revisión del PR sobre la ubicación del badge informativo en la cabecera de la mesa versus la pantalla de configuración]*
- **Justificación:** Optimiza sustancialmente la experiencia de usuario y agiliza la defensa del proyecto; elimina fallos tipográficos; y garantiza consistencia arquitectónica al subordinar la representación en la UI a la función canónica de resolución de transporte de la capa de red.
- **Evidencia:**
  - `frontend/src/config/servers.ts`: definición de `SERVER_PRESETS` con las tres opciones preconfiguradas (`ownBackendHttps`, `ownBackendHttp`, `professorServer`).
  - `frontend/src/screens/Configuracion.tsx`: renderizado de la barra de botones `server-presets` (líneas 126-136) y revalidación del formulario (`onClick={() => setValue("backendUrl", preset.url, { shouldValidate: true })}` corregida en commit `235745d`).
  - `frontend/src/api/client.ts`: función `resolveBaseUrl()` (líneas 53-60).
  - `docs/PENDIENTES_FRONTEND.md`: sección 3 ("Ideas opcionales: Mostrar si la conexión es directa o por proxy junto al estado de la mesa").
- **Responsable:** Paolo.

---

## 11. División del trabajo y flujo de integración por Pull Requests

- **Contexto:** El desarrollo de *Cards for democraXSSy* involucra tres áreas de especialización técnica concurrentes: aprovisionamiento de infraestructura en nube con IaC (OCI, Terraform, redes, seguridad de capa de transporte), implementación del backend (Spring Boot, concurrencia en memoria, validación HTTP 409) y desarrollo de la interfaz de usuario (React, TypeScript, state store, UX, mazos y sanitización XSS). Para evitar colisiones en el código, desviaciones del contrato `openapi.yaml` y roturas en producción, era indispensable definir una metodología de integración controlada y división de responsabilidades.
- **Opciones evaluadas:**
  - **Opción A (Commits directos a la rama principal `main` sin revisión):** Pros: velocidad aparente al prescindir de revisiones. Contras: alto riesgo de introducir regresiones no detectadas, colisiones de código y despliegues inestables que afectarían las pruebas en vivo.
  - **Opción B (Repositorios independientes para frontend, backend e infraestructura):** Pros: aislamiento físico total de repositorios. Contras: desincronización de esquemas compartidos (`openapi.yaml`), duplicación de incidencias y dificultad para coordinar pruebas integrales de extremo a extremo (E2E).
  - **Opción C (Monorepo con GitFlow simplificado y Pull Requests obligatorios con revisión cruzada):** Consolidar todo el código en un único repositorio (`backend/`, `frontend/`, `infra/`, `docs/`, `openapi.yaml`), trabajar en ramas temáticas (`feat/*`, `fix/*`, `infra/*`, `chore/*`) e integrar cambios exclusivamente hacia `main` mediante Pull Requests revisados y aprobados por los compañeros de equipo antes del despliegue.
- **Decisión:** Adoptar un **monorepo estructurado con flujo estricto de Pull Requests (PR)** en GitHub, separando la titularidad técnica por áreas y realizando revisiones cruzadas obligatorias:
  - **José Andrés:** Responsable principal del **Backend**. Diseño de la arquitectura en Spring Boot (Java 21), gestión del estado en memoria, endpoints de API y mecanismos de validación estricta de contrato (regla del 409).
  - **Joel:** Responsable principal de **Infraestructura y DevOps**. Aprovisionamiento con Terraform en OCI, automatización con `cloud-init`, configuración de Nginx, TLS con Certbot, seguridad de red (nftables, SELinux, anti-SSRF) y scripts de despliegue.
  - **Paolo:** Responsable principal del **Frontend, Contenido y Documentación**. Desarrollo de la SPA en React con TypeScript, experiencia de usuario y mesa de juego, creación del mazo de cartas temático (`cartas.json`), sanitización contra XSS y redacción técnica de arquitectura.
  - **Equipo completo (José Andrés, Joel y Paolo):** Integración continua, revisión cruzada de PRs (PRs #1 al #16 en historial de git), realización de partidas de prueba conjuntas con 4 jugadores en producción y preparación colaborativa para la defensa oral del proyecto.
- **Justificación:** Maximiza la calidad y robustez del software; asegura que todos los integrantes comprendan la totalidad del flujo del sistema para la defensa académica; mantiene un registro histórico inmutable de decisiones en el árbol de Git; y garantiza la estabilidad de la rama `main` en los entornos de producción en la nube.
- **Evidencia:**
  - Registro de integración de Git (`git log --oneline`):
    - PR #6 (`feat/mejora-frontend`): diseño de cartas y rondas.
    - PR #7 (`feat/mesa-de-juego`): interfaz de mesa, barra de jugadores y validaciones de errores.
    - PR #8 (`chore/monorepo`): unificación del repositorio en estructura monorepo.
    - PR #9 (`infra/terraform-backend`): despliegue de infraestructura IaC en OCI.
    - PR #10 (`feat/backend-salud`): esqueleto Spring Boot, endpoint `/api/salud` y CORS global.
    - PR #12 (`feat/mazos-cartas`): incorporación del mazo inicial de cartas.
    - PR #14 (`feat/server-presets`): botones rápidos de servidor.
    - PR #16 (`fix/presets-revalidar`): corrección y revalidación reactiva del formulario.
  - `docs/ESTADO_DEL_PROYECTO.md` y `docs/PENDIENTES_FRONTEND.md`: trazabilidad de pruebas conjuntas en producción de 4 jugadores.
- **Responsable:** Equipo completo (José Andrés, Joel y Paolo).
