# meli-climate-notifications

Sistema de notificações climáticas integrado com CPTEC.

## Arquitetura proposta

```plantuml
@startuml
skinparam monochrome true

[API Gateway] as gateway
[Notification Service] as service
[(MSSQL Server)] as db
[RabbitMQ] as mq
[Worker 1] as w1
[Worker N] as wn
[Redis] as cache
[CPTEC API] as cptec

gateway -> service : REST/HTTP
service -> db : Persistência
service -> mq : Eventos assíncronos
mq -> w1 : Consume
mq -> wn : Consume
service -> cache : Dados temporários
service -> cptec : Integração

@enduml
```

## Arquitetura e Design Decisions

### 1. Componentes Principais

| Componente | Tecnologia | Responsabilidade |
|------------|------------|------------------|
| API Gateway | Spring Cloud Gateway | Roteamento, Load Balancing, Auth |
| Notification Service | Spring Boot | Lógica de negócio, agendamentos |
| Workers | Spring + RabbitMQ | Processamento assíncrono |
| Banco de Dados | MSSQL Server | Dados persistentes (agendamentos, users) |
| Cache | Redis | Dados climáticos (TTL: 1h) |

### 2. Padrões de Comunicação

#### Síncrono (Frontend → API Gateway → Notification Service):
```mermaid
sequenceDiagram
    Frontend->>+Gateway: POST /notifications
    Gateway->>+Notification: Proxy request
    Notification-->>-Gateway: 202 Accepted
    Gateway-->>-Frontend: Response
```

#### Assíncrono (Worker → Frontend via Webhook/SSE):
```mermaid
sequenceDiagram
    Worker->>+Frontend: POST /webhook (dados climáticos)
    Frontend-->>-Worker: 200 OK
```

### 3. Fluxo de Agendamento

#### Frontend
- Solicita agendamento via API Gateway

#### Notification Service
- Valida regras (opt-out)
- Persiste no MSSQL Server
- Publica evento no RabbitMQ

#### Worker(s)
- Consome evento na hora agendada
- Busca dados atualizados (CPTEC + cache)
- Notifica frontend via Webhook/SSE

### 4. Resiliência

- Retry com backoff exponencial (RabbitMQ)
- Circuit Breaker para chamadas CPTEC

### 5. Escalabilidade

- Workers dinâmicos (docker-compose scale worker=5)


# Subir o backend (microserviços)

Na raiz do projeto rode o seguinte comando:

caso tenha o docker V2:
```bash
docker compose -f ./docker/docker-compose.yaml up --build
```

caso tenha o docker com a versão anterior da V2:
```bash
docker-compose -f ./docker/docker-compose.yaml up --build
```