@echo off
echo === Setup AI Service ===

py -3.11 --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python 3.11 chua duoc cai. Tai tai: https://www.python.org/downloads/release/python-3119/
    pause
    exit /b 1
)

echo [1/3] Tao virtual environment voi Python 3.11...
py -3.11 -m venv venv

echo [2/3] Kich hoat venv va cai dependencies...
call venv\Scripts\activate.bat
pip install -r requirements.txt

echo [3/3] Done! Chay 'start.bat' de khoi dong server.
pause
