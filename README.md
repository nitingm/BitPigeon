# BitPigeon

BitPigeon is a decentralized, peer-to-peer (P2P) Android chat application that enables messaging without an internet connection or cellular network. By leveraging **Wi-Fi Direct (P2P)** technology, BitPigeon allows users in close proximity to discover each other, form groups, and exchange messages instantly and securely.

## ⚠️ License Notice
**Source Code is View-Only.** The code in this repository is provided for educational and portfolio purposes only. It is **not** licensed under an open-source license. You may not distribute, modify, or use this code for any commercial or non-commercial purposes (including publishing to any app store) without explicit written permission from the owner. See the [LICENSE](LICENSE) file for details.

## 🚀 Key Features
- **True Offline Messaging**: Chat with nearby users using Wi-Fi Direct—no internet, routers, or access points required.
- **Automatic Discovery**: Uses DNS-SD (DNS-based Service Discovery) to automatically find and identify nearby BitPigeon users via TXT records.
- **Group Communication**: Support for P2P groups where one device acts as a host (Group Owner) and others connect as clients.
- **Reactive UI**: Built with Jetpack Compose and state-driven architecture for a fluid user experience.
- **Secure Identity**: Deterministic user and chat IDs generated via SHA-256 hashing to ensure uniqueness across devices.

## 🏗️ Architecture
BitPigeon follows **Clean Architecture** principles with a focus on Domain-Driven Design:

- **Domain Layer**: Contains core business logic, entities (`ChatMessage`, `User`), and orchestrators (`ChatModel`, `AppSystemModel`).
- **Infrastructure Layer**: Handles low-level networking (Socket management, Wi-Fi Direct Broadcasts) and data persistence.
- **UI Layer**: Modern Android UI using Jetpack Compose, following the MVI/MVVM pattern with Hilt for dependency injection.

### Data Flow
1. **Discovery**: Devices advertise their presence via DNS-SD TXT records containing serialized user info.
2. **Connection**: Wi-Fi Direct establishes a high-speed P2P link.
3. **Communication**: A socket-based server/client relationship is established (Port 8888).
4. **Synchronization**: Messages are saved to the local Room DB and simultaneously broadcasted to connected peers.

## 🛠️ Tech Stack
- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Database**: [Room](https://developer.android.com/training/data-storage/room) (Flow-based reactive queries)
- **Networking**: Wi-Fi Direct (`WifiP2pManager`) + Java Sockets
- **Local Storage**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Configuration)

## 🚦 Getting Started
### Prerequisites
- Physical Android devices (Wi-Fi Direct is not supported on emulators).
- Minimum API Level: 24 (Android 7.0).
- Permissions: `NEARBY_WIFI_DEVICES` (Android 13+) or `ACCESS_FINE_LOCATION` (Older versions).

### Installation
1. Clone the repository:
2. Open the project in **Android Studio Jellyfish** or newer.
3. Build and run the app:

## 🧪 Testing
- **Unit Tests**: Run `./gradlew test` to test domain logic and models.
- **Debug Logs**: Filter Logcat by tags: `WifiCommService`, `OnlineChatService`, or `ServerSocketManager`.

---
© 2026 Nitin Mavalange. All Rights Reserved.
