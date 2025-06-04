# Meli Climate Hub

<div align="center">
  <img src="https://http2.mlstatic.com/frontend-assets/ml-web-navigation/ui-navigation/5.21.22/mercadolibre/logo__large_plus.png" alt="Mercado Livre Logo" width="300"/>

  <p align="center">
    Sistema de notificações climáticas desenvolvido como parte do desafio técnico para o Mercado Livre.
  </p>

  <p align="center">
    <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java" alt="Java 21"/>
    <img src="https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?style=flat-square&logo=spring" alt="Spring Boot"/>
    <img src="https://img.shields.io/badge/Spring%20Cloud-2023.0.5-brightgreen?style=flat-square&logo=spring" alt="Spring Cloud"/>
    <img src="https://img.shields.io/badge/Docker-Latest-blue?style=flat-square&logo=docker" alt="Docker"/>
    <br/>
    <img src="https://img.shields.io/badge/RabbitMQ-Latest-orange?style=flat-square&logo=rabbitmq" alt="RabbitMQ"/>
    <img src="https://img.shields.io/badge/Redis-Latest-red?style=flat-square&logo=redis" alt="Redis"/>
    <img src="https://img.shields.io/badge/H2-Database-darkblue?style=flat-square&logo=h2" alt="H2"/>
    <img src="https://img.shields.io/badge/Maven-3.9.6-red?style=flat-square&logo=apache-maven" alt="Maven"/>
  </p>
</div>

## Sobre o Desafio

O desafio proposto pelo Mercado Livre consistia em desenvolver uma solução de microserviços para gerenciamento de notificações climáticas. O objetivo era demonstrar conhecimentos em arquitetura distribuída, práticas de desenvolvimento e padrões de projeto.

> "Apesar do prazo desafiador, mergulhei de cabeça no projeto. Foi uma experiência extremamente enriquecedora que me permitiu aplicar e expandir meus conhecimentos em arquitetura de microserviços."

## Arquitetura

```mermaid
graph TB
    Client((Client))
    Gateway[API Gateway]
    User[User Service]
    Notification[Notification Service]
    Worker[Notification Worker]
    Discovery[Service Discovery]
    MQ[(RabbitMQ)]
    DB[(H2 Database)]
    Redis[(Redis)]
    CPTEC[CPTEC API]

    Client -->|REST/HTTP| Gateway
    Gateway -->|Auth/JWT| User
    Gateway -->|Notificações| Notification
    Gateway -->|Service Registry| Discovery
    
    User -->|Persistência| DB
    User -->|Registry/Discovery| Discovery
    
    Notification -->|Persistência| DB
    Notification -->|Eventos| MQ
    Notification -->|Registry/Discovery| Discovery
    Notification -->|Cache| Redis
    Notification -.->|SSE| Client
    
    Worker -->|Consume| MQ
    Worker -->|Cache| Redis
    Worker -->|Integração| CPTEC
    Worker -->|Registry/Discovery| Discovery
    Worker -.->|SSE| Client

    style Client fill:#f9f,stroke:#333,stroke-width:2px
    style Gateway fill:#bbf,stroke:#333,stroke-width:2px
    style Discovery fill:#bfb,stroke:#333,stroke-width:2px
    style MQ fill:#fbf,stroke:#333,stroke-width:2px
    style Redis fill:#fbb,stroke:#333,stroke-width:2px
    style DB fill:#bff,stroke:#333,stroke-width:2px
```

## 🔍 Componentes Principais

| Componente | Tecnologia | Responsabilidade |
|------------|------------|------------------|
| API Gateway | Spring Cloud Gateway | Roteamento, Load Balancing, Autenticação JWT |
| User Service | Spring Boot | Autenticação, Gestão de Usuários, JWT |
| Notification Service | Spring Boot | Agendamentos, Eventos, Persistência |
| Notification Worker | Spring Boot | Processamento Assíncrono, Integração CPTEC |
| Service Discovery | Netflix Eureka | Service Registry, Load Balancing |
| Message Broker | RabbitMQ | Comunicação Assíncrona |
| Cache | Redis | Cache de Dados Climáticos |
| Database | H2 Database | Persistência de Dados |

## ⭐ Destaques Técnicos

- **Arquitetura Distribuída**: Microserviços independentes com Service Discovery
- **Comunicação em Tempo Real**:
  - Server-Sent Events (SSE) para notificações em tempo real
  - Conexões persistentes para atualizações climáticas
  - Baixa latência na entrega de notificações
- **Segurança**: 
  - Autenticação JWT implementada no API Gateway
  - Filtros de segurança em cada serviço
  - Tokens com expiração e renovação automática
- **Resiliência**: 
  - Circuit breaker configurado para chamadas externas
  - Retry policies no RabbitMQ
  - Healthchecks em todos os serviços
- **Observabilidade**: 
  - Actuator endpoints configurados
  - Logging estruturado com níveis apropriados
- **Clean Code**: 
  - Uso de DTOs e Mappers (MapStruct)
  - Injeção de dependências
  - Princípios SOLID

## 🚀 Executando o Projeto

### Pré-requisitos
- Docker v2+ e Docker Compose
- Java 21
- Maven

### Inicialização

```bash
# Build e execução dos serviços
docker compose -f ./docker/docker-compose.yaml up --build

# Versões antigas do Docker
docker-compose -f ./docker/docker-compose.yaml up --build
```

### 🌐 Endpoints Principais

- Eureka Dashboard: http://localhost:8761
- API Gateway: http://localhost:8080
- SSE Notifications: http://localhost:8080/notification/stream

---
<div align="center">
  <sub>Construido con ❤️ para el desafío técnico de Mercado Livre.</sub>
</div>