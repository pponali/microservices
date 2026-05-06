@echo off
cd /d "%~dp0"

echo ==^> Stopping and removing containers...
docker-compose down

echo ==^> Done. Images are kept (next 'run.bat' will start instantly).
echo     To also remove images: docker-compose down --rmi local
