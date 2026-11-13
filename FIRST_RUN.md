# First-run checklist

1. Open this folder in Android Studio.
2. Use JDK 17 and install Android SDK 37 when prompted.
3. Wait for Gradle Sync to finish.
4. Run the app on Android 7.0 or newer.
5. Create an OpenRouter API key and a GitHub fine-grained personal access token.
6. Limit the GitHub token to one repository and grant only **Contents: Read and write**.
7. In PromptForge, open **GitHub setup**, enter the keys and repository information, save, then test the connection.
8. Enter a coding request, generate files, review the checklist, and press **Review complete: apply to GitHub**.

## Upload this source project to GitHub

On Windows, double-click `UPLOAD_TO_GITHUB_WINDOWS.bat` and paste the URL of a new, empty GitHub repository.

## Security

Do not commit API keys or tokens into this project. The Android app encrypts keys locally using Android Keystore. For a public multi-user release, use GitHub OAuth or a GitHub App with a small backend instead of distributing a shared token.
