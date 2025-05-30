# 1. Atualizar os pacotes
sudo apt update && sudo apt upgrade -y

# 2. Instalar dependências necessárias
sudo apt install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# 3. Adicionar a chave GPG oficial do Docker
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 4. Adicionar o repositório do Docker
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 5. Atualizar a lista de pacotes novamente
sudo apt update

# 6. Instalar Docker Engine
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 7. Adicionar seu usuário ao grupo docker (para não precisar usar sudo)
sudo usermod -aG docker $USER

# 8. Iniciar o serviço do Docker
sudo service docker start

# 9. Verificar se a instalação foi bem sucedida
docker --version
docker compose version