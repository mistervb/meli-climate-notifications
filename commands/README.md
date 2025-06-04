# Scripts de Desenvolvimento

Este diretório contém scripts úteis para auxiliar no desenvolvimento e implantação do projeto.

## 🪟 Windows

### clean_all_projects.bat

Script para compilar todos os microserviços em sequência.

```bash
./clean_all_projects.bat
```

**Funcionalidades:**
- ✅ Compila os projetos na ordem correta de dependência
- 📊 Exibe progresso em tempo real
- 🔍 Detecta e reporta falhas automaticamente
- 📝 Gera relatório final de status

**Ordem de Execução:**
1. Service Discovery
2. API Gateway
3. User Service
4. Notification Service

## 🐧 Linux/WSL

### install_docker_on_linux.bash

Script para instalação automatizada do Docker e Docker Compose no ambiente Linux/WSL.

```bash
chmod +x install_docker_on_linux.bash
./install_docker_on_linux.bash
```

**Funcionalidades:**
- 🔄 Atualiza os pacotes do sistema
- 📦 Instala todas as dependências necessárias
- 🔑 Configura chaves GPG e repositórios
- 👥 Configura permissões de usuário
- ✅ Verifica a instalação

**Passos Executados:**
1. Atualização do sistema
2. Instalação de dependências
3. Configuração do repositório Docker
4. Instalação do Docker Engine
5. Configuração de permissões
6. Verificação da instalação

## ⚠️ Observações

- Execute os scripts do diretório raiz do projeto
- No Windows, execute em um terminal com permissões de administrador
- No Linux, pode ser necessário fornecer senha para comandos sudo