# 🦅 GravyLens (formerly Navaja Suiza)

> **Productivity Overlay Service featuring Real-time OCR, Smart History, and Floating Tools.**

**GravyLens** is not just an app; it's a powerful productivity layer for your Android device. It operates as a persistent **Overlay Service**, allowing you to summon essential tools like text capture, OCR, and clipboard history on top of *any* other application, without breaking your flow.

## 🚀 Key Features

### 👁️ Smart Floating Handle
- **Always Available**: access your tools from anywhere.
- **Magnetic Snapping**: satisfying animations that stick to screen edges.
- **Intelligent Positioning**: remembers where you left it, adapting to screen rotation.
- **Non-Intrusive**: auto-hides or minimizes to stay out of your way.

### ✂️ Screen Snipping & OCR
- **Capture Everything**: take screenshots of specific regions.
- **Instant OCR**: extract text from images in real-time using **Google ML Kit**.
- **Interactive Overlay**: tap on detected text to copy it instantly.

### 📜 Universal History
- **Clipboard Manager**: automatically saves copied text to a local, encrypted database.
- **Floating Gallery**: browse your past captures and snippets without leaving your current app.
- **Privacy First**: all data is processed and stored **100% on-device**. No cloud upload.

## 🛠️ Architecture & Tech Stack

Data privacy and system stability are our top priorities.

- **Language**: Kotlin 1.9+
- **Architecture**: MVVM Service-Centric
- **Background**: Foreground Service with WindowManager
- **Persistence**: Room Database (SQLite)
- **ML Engine**: Google ML Kit (Text Recognition V2)
- **UI**: Android Views & Minimalist Overlay Design

## 📦 Installation

### Prerequisites
- Android Studio Hedgehog or higher
- JDK 17
- Android SDK API 34

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Navaja-Suiza.git
   ```
2. **Open in Android Studio**.
3. **Build the project**:
   ```bash
   ./gradlew clean assembleDebug
   ```
4. **Run** on your device or emulator.

## 🤝 Contributing

Contributions are welcome! Please feel free to verify the `TECHNICAL_REPORT.md` for deep-dive architecture details before submitting a Pull Request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
