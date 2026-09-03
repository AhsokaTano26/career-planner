@echo off
REM ============================================================
REM Set Node 20 as default node/npm (align with Docker node:20)
REM Auto-requests admin: just double-click this file and click "Yes" on the UAC prompt.
REM Note: this file is intentionally pure ASCII to avoid Chinese-Windows
REM       ANSI/UTF-8 encoding pitfalls in .bat files.
REM ============================================================

REM ---------- Step 0: check + auto-elevate ----------
net session >nul 2>&1
if %errorlevel% equ 0 goto :run

echo Requesting admin privileges (please click "Yes" on the UAC prompt)...
set "SELF=%~f0"
set "VBS=%TEMP%\getadmin_%RANDOM%.vbs"
> "%VBS%" echo Set UAC = CreateObject("Shell.Application")
>>"%VBS%" echo UAC.ShellExecute "%SELF%", "", "", "runas", 1
cscript //nologo "%VBS%"
del "%VBS%" >nul 2>&1
exit /b

:run
echo Running with admin privileges.
echo.

set "NVM_HOME=D:\devtools\nvm"
set "SYMLINK=%NVM_HOME%\nodejs"
set "V20=%NVM_HOME%\v20.20.2"

REM ---------- Step 1: enable Developer Mode ----------
echo [1/3] Enabling Developer Mode...
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" /t REG_DWORD /v AllowDevelopmentWithoutDevLicense /d 1 /f
if errorlevel 1 ( echo [FAIL] registry write failed & pause & exit /b 1 )
echo [OK]
echo.

REM ---------- Step 2: create nvm symlink ----------
echo [2/3] Creating symlink %SYMLINK% -^> %V20%
if exist "%SYMLINK%" rmdir "%SYMLINK%"
mklink /D "%SYMLINK%" "%V20%"
if errorlevel 1 ( echo [FAIL] mklink failed & pause & exit /b 1 )
echo [OK]
echo.

REM ---------- Step 3: fix Machine PATH ----------
echo [3/3] Fixing Machine PATH (remove legacy Node24, put nvm first)...
powershell -NoProfile -Command "$m=[Environment]::GetEnvironmentVariable('Path','Machine');$a=[System.Collections.ArrayList]::new();foreach($x in $m.Split(';')){if($x -and ($x -notlike 'C:\Program Files\nodejs*') -and ($x -notlike 'D:\devtools\nvm*')){[void]$a.Add($x)}};$n=@('D:\devtools\nvm','D:\devtools\nvm\nodejs')+($a.ToArray());[Environment]::SetEnvironmentVariable('Path',($n -join ';'),'Machine')"
if errorlevel 1 ( echo [FAIL] PATH update failed & pause & exit /b 1 )
echo [OK]
echo.

echo ============================================================
echo ALL DONE. Please open a NEW terminal and run:  node -v
echo Expected output: v20.20.2
echo To switch back to 24:  nvm use 24
echo   (if symlink permission error, log off once so Developer Mode takes effect)
echo ============================================================
pause