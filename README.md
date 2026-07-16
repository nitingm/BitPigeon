#BitPigeon
BitPigeon is a decentralized, peer-to-peer (P2P) Android chat application that enables messaging without an internet connection or cellular network. By leveraging Wi-Fi Direct (P2P) technology, BitPigeon allows users in close proximity to discover each other, form groups, and exchange messages instantly and securely.
#⚠️ License Notice
Source Code is View-Only. The code in this repository is provided for educational and portfolio purposes only. It is not licensed under an open-source license. You may not distribute, modify, or use this code for any commercial or non-commercial purposes without explicit written permission from the owner. See the LICENSE file for details.
#🚀 Key Features
• True Offline Messaging: Chat with nearby users using Wi-Fi Direct—no internet required.
• Automatic Discovery: Uses DNS-SD to automatically find nearby BitPigeon users.
• Secure Identity: Deterministic user and chat IDs generated via SHA-256 hashing.
• Reactive UI: Modern interface built with Jetpack Compose.
#🏗️ Architecture
BitPigeon follows Clean Architecture principles:
• Domain Layer: Core business logic and orchestrators.
• Infrastructure Layer: Socket management and Wi-Fi Direct protocols.
• UI Layer: MVI/MVVM pattern with Hilt and StateFlow.
#🛠️ Tech Stack
• Language: Kotlin
• UI: Jetpack Compose
• DI: Hilt
• Database: Room (Reactive Flow queries)
• Networking: Wi-Fi Direct (WifiP2pManager) + Java Sockets
#🚦 Getting Started
1. Clone the repository.
2. Open in Android Studio Jellyfish+.
3. Run on physical Android devices (Wi-Fi Direct is not supported on emulators).
