@echo off
REM One-click runner for the microservices LB demo (Windows).
REM Requires Docker Desktop running.

cd /d "%~dp0"

where docker >nul 2>&1
if errorlevel 1 (
    echo ERROR: 'docker' is not on PATH. Install Docker Desktop first.
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker daemon is not running.
    echo        Start Docker Desktop and re-run this script.
    exit /b 1
)

echo ==^> Building images and starting the stack (first run takes a few minutes)...
docker-compose up --build -d
if errorlevel 1 (
    echo Compose failed. See output above.
    exit /b 1
)

echo ==^> Waiting ~60s for Spring Boot apps to register with Eureka...
timeout /t 60 /nobreak >nul

echo.
echo ============================================================
echo   STACK IS READY
echo ============================================================
echo.
echo   Eureka dashboard      http://localhost:8761
echo   Server-side LB demo   http://localhost:8080/api/users/whoami
echo   Feign client-side LB  http://localhost:8082/catalog/with-user-feign
echo   RestTemplate LB       http://localhost:8082/catalog/with-user-rest
echo.
echo   Hit those URLs MULTIPLE times in a row — the 'host' field
echo   in the JSON should rotate between the two user-service
echo   containers. That's the load balancing in action.
echo.
echo   See TESTING.md for the full test plan.
echo.
echo   Tear down with:  stop.bat   (or: docker-compose down)
echo ============================================================
