@echo off
setlocal enabledelayedexpansion

cd meli-climatehub-backend

set "falhas="

echo "=== Instalando Service Discovery ==="
cd climatehub-service-discovery
call mvn clean install
if !errorlevel! neq 0 (
    echo "ERRO: Falha ao instalar Service Discovery"
    set "falhas=!falhas!Service Discovery; "
)
cd ..

echo "=== Instalando API Gateway ==="
cd climatehub-api-gateway
call mvn clean install
if !errorlevel! neq 0 (
    echo "ERRO: Falha ao instalar API Gateway"
    set "falhas=!falhas!API Gateway; "
)
cd ..

echo "=== Instalando MS User ==="
cd climatehub-ms-user
call mvn clean install
if !errorlevel! neq 0 (
    echo "ERRO: Falha ao instalar MS User"
    set "falhas=!falhas!MS User; "
)
cd ..

echo "=== Instalando MS Notification ==="
cd climatehub-ms-notification
call mvn clean install
if !errorlevel! neq 0 (
    echo "ERRO: Falha ao instalar MS Notification"
    set "falhas=!falhas!MS Notification; "
)
cd ..

echo "=== Resumo da Instalacao ==="
if defined falhas (
    echo "Os seguintes projetos falharam: !falhas!"
    echo "Por favor, verifique os erros acima"
) else (
    echo "Todos os projetos foram instalados com sucesso!"
)

pause


