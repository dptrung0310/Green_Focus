# Repository Guidelines

## Project Structure & Module Organization
GreenFocus is an Android (Kotlin + Jetpack Compose) focus app backed by Firebase. Key locations:
- `app/src/main/java/com/example/greenfocus/` — app code: `data/` (models, repositories), `di/` (Firebase modules), `fcm/` (push messaging), `ui/` (Compose `screen/`, `components/`, `navigation/`, `theme/`), and `util/`.
- `app/src/main/res/` — drawables, mipmaps, raw sounds; `app/src/main/assets/trees.json` — tree catalog.
- `app/src/test/` and `app/src/androidTest/` — unit and instrumented tests.
- `functions/` — Firebase Cloud Functions (TypeScript, source in `src/`, compiled to `lib/`).
- Root configs: `firestore.rules`, `firestore.indexes.json`, `firebase.json`, `gradle/libs.versions.toml`.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` — build the debug APK.
- `./gradlew installDebug` — build and install on a connected device or emulator.
- `./gradlew test` — run JVM unit tests in `app/src/test/`.
- `./gradlew connectedAndroidTest` — run instrumented tests on a device/emulator.
- `./gradlew lint` — run Android Lint.
- Functions (from `functions/`): `npm run build` (compile), `npm run lint` (ESLint), `npm run serve` (local emulator), `npm run deploy`.

## Coding Style & Naming Conventions
- Kotlin: 4-space indentation, `PascalCase` for classes and Composables, `camelCase` for functions/properties, `UPPER_SNAKE_CASE` for constants.
- Suffix files by role: `*ViewModel.kt`, `*Repository.kt`, `*Screen.kt`, `*UiState.kt`.
- Keep UI state in `UiState` classes; expose state from ViewModels, no logic in Composables.
- Prefer string resources over hardcoded text. TypeScript in `functions/` follows the Google ESLint config (2-space indent).

## Testing Guidelines
- Frameworks: JUnit4 for unit tests, Espresso + Compose UI test for instrumented tests.
- Name test classes `<Subject>Test` and methods with descriptive `fun` names.
- Place fast logic tests in `app/src/test/`; device-dependent tests in `app/src/androidTest/`.
- Run the relevant suite before opening a PR.

## Commit & Pull Request Guidelines
- Commit messages are short and imperative, often prefixed by author or type, e.g. `Hung: Added Setting screen`, `feats: ...`, `merge: ...`. Keep the summary under ~72 characters.
- PRs target feature branches and merge via pull request (see history). Include a clear description, linked issues, and screenshots or screen recordings for UI changes.
- Ensure the build passes and tests run before requesting review.

## Security & Configuration Tips
- Do not commit secrets. `google-services.json` and `local.properties` are environment-specific.
- Update `firestore.rules` and `firestore.indexes.json` when data access patterns change.
