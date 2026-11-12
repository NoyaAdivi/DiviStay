@echo off
cd /d "%~dp0"
set "PY=%USERPROFILE%\anaconda3\python.exe"
if not exist "%PY%" set "PY=%LOCALAPPDATA%\anaconda3\python.exe"
if not exist "%PY%" set "PY=C:\ProgramData\anaconda3\python.exe"
if not exist "%PY%" (echo Could not find Anaconda Python. & pause & exit /b)
echo Using %PY%
echo Installing the screenshot tool (only the first time)...
"%PY%" -m pip install --quiet playwright
"%PY%" -m playwright install chromium
"%PY%" screenshots.py
pause
