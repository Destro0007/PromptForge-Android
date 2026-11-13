# PromptForge Android - Final Source Product

PromptForge turns a plain-English request into Android source files, reviews the generated file list, and applies approved files to a connected GitHub repository.

## Included
- Native Android app in Kotlin and Jetpack Compose
- OpenRouter free-model routing with editable fallback list
- Existing repository context retrieval for better edits
- GitHub connection test and branch selection
- Create/update files through GitHub Contents API
- Explicit review checkbox before upload
- Android Keystore encryption for keys
- Unsafe path protection
- GitHub Actions CI that builds a downloadable debug APK
- Windows one-click GitHub upload helper

## First build
1. Upload this project to a new GitHub repository, or open it in Android Studio.
2. GitHub Actions automatically runs **Android CI**.
3. Open the workflow run and download `PromptForge-debug-apk`.
4. Install the APK on an Android 7.0+ phone after allowing installation from your browser/files app.

## App setup
Create your own OpenRouter key and GitHub fine-grained personal access token. Limit the token to the one repository you want PromptForge to edit, with **Contents: Read and write**. In the app, enter the keys, owner, repository, and branch, save securely, and test the connection.

## Important
No API key or GitHub token is included. A distributable app cannot safely contain a shared secret. Free model names, availability, quotas, and quality can change. For a public multi-user release, replace personal-token entry with a backend-assisted GitHub App/OAuth flow and add server-side abuse controls.
