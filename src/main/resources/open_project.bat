@echo off
setlocal enabledelayedexpansion

if "%~1"=="" (
    echo [ERROR] No project path provided.
    exit /b 1
)
if "%~2"=="" (
    echo [ERROR] No project type provided.
    exit /b 1
)

set "PROJECT_PATH=%~1"
set "PROJECT_TYPE=%~2"

REM Strip any remaining quotes forcibly
for %%I in (!PROJECT_PATH!) do set "PROJECT_PATH=%%~I"
for %%I in (!PROJECT_TYPE!) do set "PROJECT_TYPE=%%~I"

echo DEBUG: PROJECT_PATH = "!PROJECT_PATH!"
echo DEBUG: PROJECT_TYPE = "!PROJECT_TYPE!"

echo Attempting to open project: !PROJECT_PATH! (Type: !PROJECT_TYPE!)

if /i "!PROJECT_TYPE!"=="React" (
    echo Opening with Visual Studio Code...
    call code "!PROJECT_PATH!"
) else if /i "!PROJECT_TYPE!"=="Spring" (
    echo Opening with IntelliJ IDEA...
    call idea "!PROJECT_PATH!"
) else if /i "!PROJECT_TYPE!"=="HTML/CSS/JS" (
    echo Opening with Visual Studio Code...
    call code "!PROJECT_PATH!"
) else if /i "!PROJECT_TYPE!"=="Python" (
    echo Opening with Visual Studio Code...
    call code "!PROJECT_PATH!"
) else if /i "!PROJECT_TYPE!"=="Java" (
    echo Opening with IntelliJ IDEA...
    call idea "!PROJECT_PATH!"
) else (
    echo [WARN] Unknown project type. Opening folder in Explorer...
    call start "" explorer "!PROJECT_PATH!"
)

echo Script finished.
exit /b 0
