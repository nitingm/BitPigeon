# BitPigeon AI Agent Guide

## Project Overview
BitPigeon is an Android chat application implementing peer-to-peer (P2P) messaging via Wi-Fi Direct. It uses a clean architecture pattern with domain-driven design, dependency injection via Hilt, and Jetpack Compose for UI.

**Key Technologies:**
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Navigation
- **Architecture:** Clean Architecture (domain/infrastructure/ui layers)
- **Dependency Injection:** Hilt
- **Database:** Room with Flow-based reactive queries
- **Networking:** Wi-Fi Direct (WifiP2pManager) + Socket-based communication
- **Persistence:** DataStore for configuration, Room for chat data
- **Build System:** Gradle with version catalogs (libs.versions.toml)

## Architecture & Data Flow
- **Domain Layer** (`domain/`): Core business logic, entities, services, models
  - Entities: `ChatMessage`, `User`, `ChatGroup`, etc.
  - Services: `WifiCommunicationService` (P2P service advertising/discovery/connection), `OnlineChatService` (socket messaging)
  - Models: `ChatModel`, `AppSystemModel`, `ConversationModel` (business logic orchestrators)
- **Infrastructure Layer** (`infrastructure/`): External integrations
  - `ServerSocketManager`/`ClientSocketManager`: Handle socket communication in P2P groups
  - `WifiDirectBroadcastReceiver`: Listens for Wi-Fi P2P system events
- **UI Layer** (`ui/`): Compose screens and ViewModels
  - ViewModels inject domain models/services
  - Reactive UI using StateFlow/Flow from domain layer

**Data Flow:**
1. DNS-SD service discovery advertises BitPigeon services and discovers nearby users
2. Wi-Fi Direct establishes P2P connection (group owner = server, others = clients)
3. `OnlineChatService` starts socket server/client based on group role
4. Messages sent via `ChatModel.sendMessage()` → inserts to Room DB + broadcasts via sockets
5. Incoming messages from sockets → inserted to DB → UI updates via Flow

## Critical Workflows

### Building & Running
- **Build APK:** `./gradlew assembleDebug` or `./gradlew assembleRelease`
- **Install & Run:** `./gradlew installDebug` (requires connected device/emulator)
- **Clean Build:** `./gradlew clean build`
- **Unit Tests:** `./gradlew test` (runs on JVM, mocks Android dependencies)
- **Instrumented Tests:** `./gradlew connectedAndroidTest` (requires device/emulator)

### Debugging P2P Networking
- Enable Wi-Fi Direct permissions: ACCESS_FINE_LOCATION (pre-API 33) + NEARBY_WIFI_DEVICES (API 33+)
- Monitor logs for `WifiCommService`, `OnlineChatService`, `ServerSocketManager`
- P2P service discovery flow: Automatic advertising (when WiFi enabled) → Service discovery → User selection → Connection → Socket handshake on port 8888
- Devices automatically advertise BitPigeon services with user info in TXT records when WiFi Direct is enabled
- Group owner (server) broadcasts messages; clients send to server for relay

### Testing Patterns
- **Unit Tests:** Test domain logic (models, services) with mock DAOs/services
- **Integration Tests:** Test full flows with in-memory Room DB
- Use `TestCoroutineDispatcher` for Flow/StateFlow testing
- Mock `WifiP2pManager` for networking tests (challenging due to system APIs)

## Project-Specific Conventions

### Dependency Injection
- Use Hilt modules in `di/` for providing dependencies
- Singleton services (e.g., `WifiCommunicationService`) live app-wide
- ViewModels inject domain models, not services directly

### Reactive Data Handling
- **Flow Observation**: Prefer `StateFlow` for UI state, `Flow` for DB queries.
- **Dynamic State Derivation**: Use `flatMapLatest` in ViewModels to reactively switch data sources when a parent state changes (e.g., deriving `usersInGroup` or `messages` from an active `chatId` or `chatGroup` flow).
- **State Composition**: Use `combine` in ViewModels to merge multiple data sources (e.g., Messages + Members + Transfers) into a single UI-ready `StateFlow`.
- **Lifecycle-Aware Collection**: Always use `collectAsStateWithLifecycle()` in Composables. This ensures that collection stops when the UI is not visible, preventing resource leaks and unnecessary background work.

### UI & Side Effects
- **Stateless Composables**: Keep Composables focused on rendering. Pass all required data as parameters.
- **No Side Effects in Composition**: NEVER call ViewModel setters or update state (e.g., `setActiveChatGroupId`) directly in the body of a Composable function. These are side effects and can cause infinite loops or inconsistent UI state. Use `LaunchedEffect` if an action must be tied to a specific state change or navigation event.
- **ViewModel Responsibility**: Business logic and data fetching (e.g., `getUsersInGroup`) must reside in the ViewModel as a `StateFlow`. Avoid calling ViewModel functions that return new `Flow` objects inside a Composable's `collectAsState`.

### ID Generation
- Deterministic IDs using SHA-256 hash via `HashService`
- Chat groups: `DIRECT_${sortedUserIds}` for P2P chats, `PERSONAL_${userId}` for self-notes
- Messages: Hash of `senderId + timestamp + text` (ensures uniqueness across devices)

### Database Patterns
- Room entities with `@Entity(tableName)`, relations via foreign keys
- DAOs return `Flow<List<T>>` for reactive UI updates
- Use `OnConflictStrategy.REPLACE` for message upserts
- Type converters in `DataTypeConvertor` for complex types (e.g., `MessageData`)

### Networking Patterns
- DNS-SD service discovery: Devices advertise BitPigeon services with serialized User objects in TXT records
- P2P group: One device is "group owner" (server), others connect as clients
- Socket communication: Object streams for `ChatMessage`/`User` serialization
- Broadcast pattern: Server relays messages to all connected clients
- Connection lifecycle: Tied to Wi-Fi P2P connection state

## Key Files & Directories
- `app/src/main/java/com/codingskillshub/bitpigeon/`: Main source
  - `domain/`: Business logic (read first for feature understanding)
  - `infrastructure/`: External APIs (sockets, broadcasts)
  - `ui/`: Screens and ViewModels
  - `di/`: Hilt modules
  - `common/`: Shared utilities (config, hashing)
- `app/build.gradle.kts`: Dependencies and plugins
- `AndroidManifest.xml`: Permissions and components
- `gradle/libs.versions.toml`: Version catalog for dependencies

## Common Pitfalls
- Wi-Fi Direct requires physical proximity and compatible devices
- DNS-SD service discovery may have TXT record size limits (400 bytes); keep User info concise
- Socket connections fail if P2P group disconnects; handle reconnections
- Room DB operations are suspend; use coroutines in ViewModels
- Permission denials prevent peer discovery; prompt user appropriately
- Deterministic IDs prevent duplicates but require consistent input data
