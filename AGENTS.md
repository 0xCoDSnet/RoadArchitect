# Road Architect · AGENTS Manifest

## 1. Mission


Generate and maintain clean, reliable code that builds and manages road networks connecting natural structures in **Minecraft 1.21.2** using the Fabric, NeoForge and Quilt modding toolchains through Architectury.
## 2. Coding Guidelines

* **Java 21** as the language level.
* Use **SLF4J** (`LOGGER`) for all logging — never resort to `System.out` or `printStackTrace`.
* Follow **Google Java Style** (4‑space indents, 120‑character line limit).
* Avoid the Java 10 `var` keyword for local variables.
* Provide **JUnit 5** tests with **Minecraft‑Test** whenever feasible.

## 3. Repository Structure

* `modules/common` — общая логика.
* `modules/fabric`, `modules/neoforge`, `modules/quilt` — код специфичный для соответствующих загрузчиков.

## 4. Do & Don’t

### 4.1 Do

* Leverage Fabric, NeoForge and Quilt events and hooks for all world interaction.
* Re‑use helper utilities in `util/*` to prevent code duplication.

### 4.2 Don’t

* Perform blocking I/O on the main server thread.
* Hard‑code block or item IDs — always use `Identifier` look‑ups.

## 5. Build & Testing

* `./gradlew build` builds all modules.
* `./gradlew test` runs the test suites (JUnit 5 + Minecraft-Test).

