# 🦅 GravyLens (antes Navaja Suiza)

> **Tu capa de productividad invisible: OCR en tiempo real, Historial Inteligente y Herramientas Flotantes.**

**GravyLens** es más que una simple aplicación; es una poderosa "navaja suiza" que vive sobre tu sistema Android. Funciona como un **Servicio de Superposición** (Overlay), permitiéndote invocar herramientas esenciales como captura de texto, OCR y recuperación de portapapeles sobre *cualquier* otra aplicación, sin interrumpir lo que estás haciendo.

## 🚀 ¿Qué hace GravyLens?

### 👁️ Botón Flotante Inteligente
Tu acceso directo a la productividad.
- **Siempre Disponible**: Invoca las herramientas desde cualquier pantalla.
- **Atracción Magnética**: El botón se adhiere elegantemente a los bordes con físicas naturales.
- **Memoria de Posición**: Recuerda dónde lo dejaste, adaptándose si giras la pantalla.

### ✂️ Recorte de Pantalla y OCR Instantáneo
Olvídate de tomar capturas completas para luego recortarlas.
- **Captura Precisa**: Selecciona solo el área que te interesa.
- **OCR en Tiempo Real**: Extrae texto de imágenes instantáneamente usando la potencia de **Google ML Kit**.
- **Interacción Directa**: Toca el texto detectado en la pantalla para copiarlo al instante.

### 📜 Historial Universal
Nunca pierdas un texto copiado.
- **Gestor de Portapapeles**: Guarda automáticamente todo lo que copias en una base de datos local cifrada.
- **Galería Flotante**: Revisa tus recortes y textos anteriores sin salir de tu aplicación actual.
- **Privacidad Total**: Todo el procesamiento y almacenamiento ocurre **100% en tu dispositivo**. Nada se sube a la nube.

## 🛠️ Tecnología y Arquitectura

Diseñado con estándares de ingeniería modernos para estabilidad y rendimiento.

- **Lenguaje**: Kotlin 1.9+
- **Arquitectura**: MVVM centrada en Servicios (Service-Centric)
- **Motor**: Foreground Service con gestión avanzada de WindowManager
- **Base de Datos**: Room (SQLite)
- **Inteligencia Artificial**: Google ML Kit (Reconocimiento de Texto V2)

## 📦 Instalación y Desarrollo

### Requisitos
- Android Studio Hedgehog o superior
- JDK 17
- Android SDK API 34

### Compilar el proyecto

1. **Clonar el repositorio**:
   ```bash
   git clone https://github.com/jhosuehag/GravyLens.git
   ```
2. **Abrir en Android Studio**.
3. **Compilar**:
   ```bash
   ./gradlew clean assembleDebug
   ```
4. **Ejecutar** en tu dispositivo o emulador.

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para más detalles.
