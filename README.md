<!-- Plugin description -->
# Maestro Assistant IntelliJ Plugin

**Maestro Assistant** brings rich language support and seamless test execution
for [Maestro UI test](https://maestro.mobile.dev/) flows right into IntelliJ IDEA, Android Studio, and other JetBrains
IDEs. Write, validate, and run your Maestro YAML tests with confidence—no terminal context-switching required!

<img src="https://github.com/Trendyol/maestro-assistant/blob/main/assets/Image%201354x1302.png?raw=true" height="600"/>
<img src="https://github.com/Trendyol/maestro-assistant/blob/main/assets/Maestro%20Plugin%20GIF.gif?raw=true" height="500"/>


---

## ✨ Features

- **Maestro YAML File Detection:**  
  Recognizes Maestro test files automatically, even within existing project folders.
- **Syntax Highlighting:**  
  Distinguishes commands, arguments, and outputs with smart color schemes.
- **Auto-Completion:**  
  Context-aware, intelligent completion for commands, arguments, and Maestro-specific actions.
- **Inline Validation:**  
  Real-time error detection with helpful messages—catch mistakes early!
- **Documentation on Hover:**  
  Get Maestro command documentation without leaving your editor.
- **Run Maestro Tests In-IDE:**  
  Execute test flows directly from the IDE and view real-time results.
- **Tool Window & UI:**  
  Visual test explorer window with status indicators and easy one-click runs.
- **Hints & Navigations:**  
  Go to definition, command reference, and variable highlights.

---

## 🚀 Quick Start

### 1. Install the Plugin

**From source:**

1. Clone this repo:
   ```bash
   git clone https://github.com/Trendyol/maestro-assistant.git
   ```
2. Open in IntelliJ IDEA (Ultimate/Community, or Android Studio).
3. Go to **Build > Prepare Plugin Module for Deployment**.
4. Install the generated `.zip` via **Settings > Plugins > Install Plugin from Disk…**

**Jetbrains Marketplace:**  
[Maestro-Assistant](https://plugins.jetbrains.com/plugin/27807-maestro-assistant)

---

### 2. Usage

1. **Open or create your Maestro YAML test files (`.yaml`/`.yml`)**.
2. Enjoy auto-completion, error highlighting, and command documentation as you type.
3. Use the **“Maestro Tests”** tool window to explore and run tests right in your project.
4. Right-click a test file or use the context menu and select **Run Maestro Test**.

---

## 🛠 Development

### Requirements

- JDK 17+
- IntelliJ IDEA Community/Ultimate
- Gradle (or use the wrapper `./gradlew`)

### Building

```bash
./gradlew build
```

### Running in Developer Mode

```bash
./gradlew runIde
```

---

## 🤝 Contributing

Found a bug, have an idea, or want to add a feature?  
Feel free to [open an issue](https://github.com/Trendyol/maestro-assistant/issues) or submit a pull request!

See [CONTRIBUTING.md](CONTRIBUTING.md) for more details.

---

## 📦 Release Notes

See [CHANGELOG.md](CHANGELOG.md) for past releases and what's coming next.

---

## 📚 Resources

- [Maestro Documentation](https://maestro.mobile.dev/)
- [JetBrains Plugin SDK](https://plugins.jetbrains.com/docs/intellij)

---

## License

[MIT](LICENSE)

---

**Made with ❤️ for the Maestro + JetBrains community by Trendyol**

---
<!-- Plugin description end -->