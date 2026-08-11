# UE PAK Explorer — Android

A lightweight native Android tool for browsing, searching, and extracting Unreal Engine PAK files.

## ✨ Features

- Open Unreal Engine `.pak` files directly on Android.
- Browse files and folders stored inside PAK archives.
- Search files by name, extension, or path.
- View file size and PAK archive information.
- Extract individual files or multiple files.
- Copy internal file paths.
- Detect PAK encryption when possible.
- Designed to work without Root access.
- Uses Android Storage Access Framework for secure file access.
- Designed for modern ARM64 Android devices.

## 🎯 Purpose

UE PAK Explorer is a general-purpose Android tool for inspecting Unreal Engine PAK archives.

It is designed for anyone who needs to explore the contents of Unreal Engine files, including developers, modders, translators, researchers, and game enthusiasts.

The goal is to provide a simple and accessible way to inspect PAK archives directly on Android.

## 🚧 Current Status

The project is currently in the MVP stage.

The initial version focuses on:

**Open → Browse → Search → Extract**

More advanced features will be added after the core PAK reading functionality is stable.

## 🛠️ Technology

- Kotlin
- Android SDK
- Rust
- JNI
- Unreal Engine PAK parser
- Android Storage Access Framework
- ARM64

## 🔐 Encryption

The application can detect encrypted PAK archives when possible.

It does not attempt to bypass or break encryption.

If a valid encryption key is provided by the user and supported by the parser, it may be used for legitimate file inspection.

## 📦 Planned Features

- Unreal Engine `.locres` inspection
- Localization file parsing
- File metadata viewer
- Advanced file and path search
- JSON and TXT export
- Improved file tree navigation
- Support for additional Unreal Engine PAK versions
- Support for additional Unreal Engine archive formats

## 🤝 Open Source

This project is open source and is intended to be useful to the Android, Unreal Engine, modding, localization, and game-research communities.

Contributions, bug reports, and improvements are welcome.

## 📄 License

MIT License
