# 🦅 Navaja Suiza (Swiss Army Knife) - Informe Técnico del Proyecto

**Versión del Documento:** 1.0  
**Fecha:** 24 de Enero, 2026  
**Clasificación:** Ingeniería de Software Android / Herramientas de Productividad

---

## 1. Introducción Ejecutiva

**Navaja Suiza** es una suite de productividad integrada diseñada para el sistema operativo Android. A diferencia de aplicaciones convencionales que operan en su propia ventana, Navaja Suiza funciona principalmente como una **capa de servicio residente (Overlay Service)**. Esto permite al usuario evocar herramientas potentes (captura, OCR, historial) sobre cualquier otra aplicación activa sin interrumpir su flujo de trabajo principal.

El proyecto se posiciona como una "Super App" de utilidades, eliminando la necesidad de cambiar constantemente de contexto (Task Switching) para realizar acciones comunes como copiar texto de imágenes, gestionar recortes o recuperar textos anteriores.

## 2. Objetivos del Proyecto

1.  **Productividad sin Fricción:** Reducir a cero los pasos necesarios para capturar información de la pantalla.
2.  **Integración Nativa:** Proveer una experiencia de usuario (UX) que se sienta parte del sistema operativo (animaciones fluidas, respeto a los botones de navegación nativos).
3.  **Privacidad Local:** Procesamiento de imágenes (OCR) y almacenamiento de historial 100% *on-device*, sin dependencias de nube.
4.  **Estabilidad de Grado Sistema:** Gestión agresiva de memoria para bitmaps y manejo robusto de excepciones para evitar cierres inesperados en segundo plano.

## 3. Arquitectura del Sistema

El proyecto sigue una arquitectura **Multi-Module Monorepo** para garantizar la separación de responsabilidades y la escalabilidad.

### 3.1. Módulos Principales

*   **`:app`**: Capa de presentación (Launcher), gestión de permisos críticos y orquestación inicial.
*   **`:core`**: Utilidades base, extensiones, y gestores compartidos (`OverlayManager`, `PrefsManager`).
*   **`:data`**: Capa de persistencia. Implementa **Room Database** para el historial de portapapeles y Repositorios para acceso a archivos.
*   **`:service`**: El corazón de la aplicación. Contiene el `FloatingService` que mantiene el ciclo de vida de los componentes flotantes.
*   **`:ocr`**: Módulo especializado que encapsula **Google ML Kit** para el reconocimiento de texto.
*   **`:features:history`**: Módulos de UI desacoplados para el Historial y la Galería flotante.

### 3.2. Patrones de Diseño Clave

*   **Service-Centric Architecture:** La lógica de negocio principal no vive en una `Activity`, sino en un `Service` en primer plano (`ForegroundService`), lo que garantiza la supervivencia del proceso.
*   **Repository Pattern:** Abstracción del acceso a datos (Room y FileSystem) para facilitar pruebas y cambios de fuente.
*   **Window Manager Wrapper:** Se utiliza una clase `OverlayManager` para abstraer la complejidad de añadir/remover vistas directamente al `WindowManager` de Android.

---

## 4. Funcionalidades Core (Detalle Técnico)

### 4.1. Botón Flotante Inteligente (`FloatingService`)
*   **Mecanismo:** Un servicio persistente inyecta una vista (`FloatingHandleView`) en la ventana del sistema (`TYPE_APPLICATION_OVERLAY`).
*   **Smart Positioning:** El botón recuerda su posición independientemente para modos Vertical y Horizontal.
*   **Magnetic Snapping:** Animaciones físicas (SpringForce) que adhieren el botón a los bordes de la pantalla.
*   **Safety Limits:** Algoritmos de "Clamping" evitan que el botón se oculte bajo la barra de estado o la barra de navegación.

### 4.2. Sistema de Recortes ("Snipping Tool")
*   **Captura:** Utiliza `MediaProjection API` para obtener una captura bruta del FrameBuffer.
*   **Interfaz de Recorte (`SnippingOverlayView`):** Una vista personalizada dibuja una máscara semitransparente. El usuario define un `RectF` de interés.
*   **Optimización:** Los Bitmaps se manipulan bajo un estricto control de memoria. Se implementa `recycle()` agresivo al cerrar las vistas para evitar `OutOfMemoryError`.
*   **UX Nativa:** Soporte completo para el botón "Atrás" del sistema y autoevaluación de espacio para posicionar los botones de acción (Guardar/Cancelar) sin que se corten.

### 4.3. Escáner OCR en Tiempo Real (`TextRecognizerManager`)
*   **Motor:** Google ML Kit (Text Recognition V2).
*   **Flujo:**
    1.  Captura invisible de pantalla completa.
    2.  Análisis de la imagen para detectar bloques (`Text.TextBlock`).
    3.  Superposición (`HighlightOverlayView`) que dibuja cajas delimitadoras interactivas sobre la imagen original.
    4.  Interacción táctil que permite copiar el texto directamente desde la "imagen congelada".

### 4.4. Historial Universal y Galería
*   **Clipboard Watcher:** Un monitor que escucha cambios en el `ClipboardManager` y persiste el texto en una base de datos Room cifrada localmente.
*   **Galería Flotante:** Un `RecyclerView` inflado en una ventana flotante que permite visualizar, compartir y eliminar capturas sin abrir la app principal.

---

## 5. Implementación Técnica Destacada

### 5.1. Gestión de Ventanas (Overlay Engineering)
La aplicación no utiliza `Activities` para sus funciones principales. Manipula directamente `WindowManager.LayoutParams`.
```kotlin
// Ejemplo de configuración de flags para permitir toques fuera de la ventana (Passthrough)
params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
               WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
```
Recientemente se implementó un sistema híbrido de **Foco de Ventana**:
*   Por defecto, las ventanas no tienen foco (para no robar teclado).
*   Al interactuar, se solicita `requestFocus()` para capturar eventos de sistema como el botón **Back** o **Home**, cerrando la interfaz elegantemente.

### 5.2. Trampolín de Compartir (`ShareActivity`)
Dado que un `Service` no puede lanzar un `Intent.ACTION_SEND` (Share Sheet) de manera estándar en versiones modernas de Android sin una ventana visible, se implementó una `ShareActivity` transparente (Trampoline).
*   El servicio llama a esta actividad pasándole la URI del archivo.
*   La actividad lanza el selector de compartir y se cierra inmediatamente.
*   Esto "engaña" al sistema para permitir compartir archivos desde un contexto flotante.

---

## 6. Stack Tecnológico

| Categoría | Tecnologías |
| :--- | :--- |
| **Lenguaje** | Kotlin 1.9+ (Coroutines, Flow) |
| **UI** | Android Views (XML), Custom Views, RecyclerView |
| **Arquitectura** | MVVM (Lite), Repository Pattern |
| **Persistencia** | Room Database (SQLite), SharedPreferences |
| **Multimedia** | MediaProjection API, Bitmap Operations |
| **IA / ML** | Google ML Kit (On-device Text Recognition) |
| **Animaciones** | Jetpack Dynamic Animation (Spring) |
| **Inyección de Dependencias** | Manual (Service Locator simple para velocidad) |
| **Build System** | Gradle (Kotlin DSL) |

---

## 7. Estado Actual y Calidad
El proyecto ha pasado por una fase intensiva de **Hardening (Endurecimiento)**:
*   ✅ **Auditoría de Fugas de Memoria:** Ciclo de vida estricto en Vistas y Bitmaps.
*   ✅ **Navegación Consistente:** Integración unificada con botones nativos (Atrás/Home).
*   ✅ **Layout Responsivo:** Adaptación inteligente a cambios de orientación y tamaños de pantalla.
*   ✅ **Manejo de Errores:** Bloques `try-catch` estratégicos en puntos críticos (Captura, OCR, Intent Launching).

---
**Autor:** Antigravity AI Agent  
**Para:** Navaja Suiza Engineering Team
