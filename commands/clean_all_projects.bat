@echo off
setlocal enabledelayedexpansion

cd ..

if not exist "meli-climatehub-backend" (
    echo "ERRO: Diretorio meli-climatehub-backend nao encontrado"
    exit /b 1
)

cd meli-climatehub-backend

set "falhas="

echo "=== Instalando Service Discovery ==="
if exist "climatehub-service-discovery" (
    cd climatehub-service-discovery
    call mvn clean install -DskipTests
    if !errorlevel! neq 0 (
        echo "ERRO: Falha ao instalar Service Discovery"
        set "falhas=!falhas!Service Discovery; "
    )
    cd ..
) else (
    echo "ERRO: Diretorio climatehub-service-discovery nao encontrado"
    set "falhas=!falhas!Service Discovery; "
)

echo "=== Instalando API Gateway ==="
if exist "climatehub-api-gateway" (
    cd climatehub-api-gateway
    call mvn clean install -DskipTests
    if !errorlevel! neq 0 (
        echo "ERRO: Falha ao instalar API Gateway"
        set "falhas=!falhas!API Gateway; "
    )
    cd ..
) else (
    echo "ERRO: Diretorio climatehub-api-gateway nao encontrado"
    set "falhas=!falhas!API Gateway; "
)

echo "=== Instalando MS User ==="
if exist "climatehub-ms-user" (
    cd climatehub-ms-user
    call mvn clean install -DskipTests
    if !errorlevel! neq 0 (
        echo "ERRO: Falha ao instalar MS User"
        set "falhas=!falhas!MS User; "
    )
    cd ..
) else (
    echo "ERRO: Diretorio climatehub-ms-user nao encontrado"
    set "falhas=!falhas!MS User; "
)

echo "=== Instalando MS Notification ==="
if exist "climatehub-ms-notification" (
    cd climatehub-ms-notification
    call mvn clean install -DskipTests
    if !errorlevel! neq 0 (
        echo "ERRO: Falha ao instalar MS Notification"
        set "falhas=!falhas!MS Notification; "
    )
    cd ..
) else (
    echo "ERRO: Diretorio climatehub-ms-notification nao encontrado"
    set "falhas=!falhas!MS Notification; "
)

echo "=== Instalando MS Notification Worker ==="
if exist "climatehub-ms-notification-worker" (
    cd climatehub-ms-notification-worker
    call mvn clean install -DskipTests
    if !errorlevel! neq 0 (
        echo "ERRO: Falha ao instalar MS Notification Worker"
        set "falhas=!falhas!MS Notification Worker; "
    )
    cd ..
) else (
    echo "ERRO: Diretorio climatehub-ms-notification-worker nao encontrado"
    set "falhas=!falhas!MS Notification Worker; "
)

echo "=== Resumo da Instalacao ==="
if defined falhas (
    echo "Os seguintes projetos falharam: !falhas!"
    echo "Por favor, verifique os erros acima"
) else (
    echo "Todos os projetos foram instalados com sucesso!"
)

pause


