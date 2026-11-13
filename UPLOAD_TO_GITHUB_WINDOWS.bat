@echo off
setlocal
cd /d "%~dp0"
echo ==========================================
echo PromptForge - Upload project to GitHub
echo ==========================================
echo.
where git >nul 2>nul || (
  echo Git is not installed. Install Git for Windows from https://git-scm.com/download/win
  pause
  exit /b 1
)
set /p REPO_URL=Paste your empty GitHub repository URL: 
if "%REPO_URL%"=="" (
  echo Repository URL is required.
  pause
  exit /b 1
)
if not exist .git git init
call git branch -M main
call git add .
call git commit -m "Initial PromptForge Android app"
git remote get-url origin >nul 2>nul
if errorlevel 1 (
  call git remote add origin "%REPO_URL%"
) else (
  call git remote set-url origin "%REPO_URL%"
)
call git push -u origin main
if errorlevel 1 (
  echo.
  echo Upload failed. Check the repository URL and GitHub sign-in, then run this file again.
) else (
  echo.
  echo Upload completed successfully.
)
pause
