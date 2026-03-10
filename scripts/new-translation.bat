@echo off
REM ============================================================
REM new-translation.bat — Scaffold a new HyperFactions locale
REM Usage: scripts\new-translation.bat <locale-code>
REM Example: scripts\new-translation.bat fr-FR
REM ============================================================

if "%~1"=="" (
    echo Usage: %~nx0 ^<locale-code^>
    echo Example: %~nx0 fr-FR
    exit /b 1
)

set "LOCALE=%~1"

REM Resolve project root (parent of scripts\)
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.."
set "PROJECT_ROOT=%CD%"
popd

set "LANG_SRC=%PROJECT_ROOT%\src\main\resources\Server\Languages\en-US"
set "LANG_DST=%PROJECT_ROOT%\src\main\resources\Server\Languages\%LOCALE%"

set "HELP_SRC=%PROJECT_ROOT%\src\main\help\en-US"
set "HELP_DST=%PROJECT_ROOT%\src\main\help\%LOCALE%"

REM --- Validate source exists ---
if not exist "%LANG_SRC%\" (
    echo Error: Source language directory not found: %LANG_SRC%
    exit /b 1
)

REM --- Copy .lang files ---
set LANG_COUNT=0
if exist "%LANG_DST%\" (
    echo Language directory already exists: %LANG_DST%
    echo Skipping .lang file copy (delete the directory first to re-scaffold).
) else (
    mkdir "%LANG_DST%"
    for %%f in ("%LANG_SRC%\*.lang") do (
        copy "%%f" "%LANG_DST%\" >nul
        set /a LANG_COUNT+=1
    )
    echo Copied %LANG_COUNT% .lang file(s) to %LANG_DST%
)

REM --- Copy help markdown ---
set HELP_COUNT=0
if exist "%HELP_SRC%\" (
    if exist "%HELP_DST%\" (
        echo Help directory already exists: %HELP_DST%
        echo Skipping help file copy (delete the directory first to re-scaffold).
    ) else (
        xcopy "%HELP_SRC%" "%HELP_DST%" /E /I /Q >nul
        REM Count .md files
        for /r "%HELP_DST%" %%f in (*.md) do set /a HELP_COUNT+=1
        echo Copied %HELP_COUNT% help file(s) to %HELP_DST%
    )
) else (
    echo No help directory found at %HELP_SRC% — skipping help files.
)

REM --- Summary ---
echo.
echo === Scaffold Summary ===
echo Locale:      %LOCALE%
echo Lang files:  %LANG_COUNT% copied to src\main\resources\Server\Languages\%LOCALE%\
echo Help files:  %HELP_COUNT% copied to src\main\help\%LOCALE%\
echo.
echo Next steps:
echo   1. Add a header comment to each .lang file indicating the language and status
echo   2. Translate the values (keep keys and {0} placeholders unchanged)
echo   3. Translate the help markdown files
echo   4. Test in-game with /f settings to switch language
