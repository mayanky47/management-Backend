@echo off
setlocal enabledelayedexpansion

REM This script creates a new React project using Vite and installs Tailwind CSS.
REM Arguments: %1 = Project Name, %2 = Parent Directory (e.g., D:\project\projects\frontend)

set "PROJECT_NAME=%~1"
set "PARENT_DIR=%~2"

echo DEBUG: Project Name = "!PROJECT_NAME!"
echo DEBUG: Parent Directory = "!PARENT_DIR!"

echo Navigating to parent directory: !PARENT_DIR!
cd /d "!PARENT_DIR!" || ( echo ERROR: Could not navigate to !PARENT_DIR! & exit /b 1 )

echo Creating Vite React project: !PROJECT_NAME! with TypeScript and React template...
REM Explicitly using @latest for create-vite and specifying --template react-ts
REM Pipe an empty line to 'npm create vite' to automatically confirm the package name prompt.
(echo.) | call npm create vite@latest "!PROJECT_NAME!" -- --template react-ts || ( echo ERROR: Vite project creation failed! & exit /b 1 )

echo Navigating into the new project directory: !PROJECT_NAME!
cd /d "!PROJECT_NAME!" || ( echo ERROR: Could not navigate to !PROJECT_NAME! & exit /b 1 )

echo Installing Tailwind CSS and Vite plugin...
echo Executing: npm install tailwindcss @tailwindcss/vite
call npm install tailwindcss @tailwindcss/vite || ( echo ERROR: Tailwind CSS installation failed! & exit /b 1 )

REM --- AUTOMATIC TAILWIND CSS CONFIGURATION ---
set "VITE_CONFIG_PATH=!PARENT_DIR!\!PROJECT_NAME!\vite.config.ts"
set "MAIN_CSS_PATH=!PARENT_DIR!\!PROJECT_NAME!\src\index.css"

echo.
echo Clearing vite.config.ts...
powershell -Command "'' | Set-Content '!VITE_CONFIG_PATH!'" || ( echo ERROR: Failed to clear vite.config.ts & exit /b 1 )

echo Writing Tailwind + React plugin configuration to vite.config.ts...

echo Overwriting vite.config.ts with Tailwind and React plugin configuration...

powershell -Command "Add-Content '!VITE_CONFIG_PATH!' 'import { defineConfig } from ''vite'''"
powershell -Command "Add-Content '!VITE_CONFIG_PATH!' 'import react from ''@vitejs/plugin-react'''"
powershell -Command "Add-Content '!VITE_CONFIG_PATH!' 'import tailwindcss from ''@tailwindcss/vite'''"
powershell -Command "Add-Content '!VITE_CONFIG_PATH!' ''"
powershell -Command "Add-Content '!VITE_CONFIG_PATH!' '// https://vitejs.dev/config/'"
powershell -Command "Add-Content '!VITE_CONFIG_PATH!' 'export default defineConfig({'"
powershell -Command "Add-Content '!VITE_CONFIG_PATH!' '    plugins: [tailwindcss(), react()],'"
powershell -Command "Add-Content '!VITE_CONFIG_PATH!' '})'"
if %errorlevel% neq 0 ( echo ERROR: Failed to overwrite vite.config.ts & exit /b 1 )

@REM (
@REM echo import { defineConfig } from 'vite'
@REM echo import react from '@vitejs/plugin-react'
@REM echo import tailwindcss from '@tailwindcss/vite'
@REM echo.
@REM echo // https://vitejs.dev/config/
@REM echo export default defineConfig({
@REM echo ^[tailwindcss^(^), react^(^)^],
@REM echo })
@REM ) > "!VITE_CONFIG_PATH!" || ( echo ERROR: Failed to overwrite vite.config.ts & exit /b 1 )

echo.
echo Attempting to set main CSS file content to only '@import "tailwindcss";'...
REM Overwrite the main CSS file with only '@import "tailwindcss";'
echo Setting content of !MAIN_CSS_PATH!
powershell -Command "'@import \"tailwindcss\";' | Set-Content '!MAIN_CSS_PATH!'" || ( echo ERROR: Failed to set content of main CSS file & exit /b 1 )


echo.
echo ====================================================================================
echo Tailwind CSS Configuration Attempted Automatically.
echo ====================================================================================
echo Your Vite React project "!PROJECT_NAME!" has been created, and Tailwind CSS has been installed.
echo The script attempted to configure 'vite.config.ts' and 'src/index.css' automatically.
echo Please review these files to ensure the changes were applied correctly.
echo.
echo After these steps, you can run 'npm run dev' inside your project directory:
echo cd "!PARENT_DIR!\!PROJECT_NAME!"
echo npm run dev
echo ====================================================================================
echo.

exit /b 0
