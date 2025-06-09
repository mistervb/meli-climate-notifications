# API Documentation

## 🔑 User Service (Authentication)

### Register User
```http
POST /user/register
```

**Payload:**
```json
{
  "username": "string",
  "email": "string",
  "passwordHashed": "string"
}
```

> "passwordHashed" porque ela será criptografada durante o processo de criação de conta, mas não deve ser informa-la criptografada.

**Success Response (201 Created):**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "data": {
    "userId": "uuid",
    "username": "string",
    "createdAt": "datetime",
    "updatedAt": "datetime"
  }
}
```

### Login
```http
POST /user/login
```

**Payload:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Success Response (200 OK):**
```json
{
  "status": "success",
  "statusCode": 200,
  "data": {
    "token": "string"
  }
}
```

### Opt-out de Notificações
```http
PUT /user/{userId}/opt-out
```

**Success Response (204 No Content)**

### Opt-in de Notificações
```http
PUT /user/{userId}/opt-in
```

**Success Response (204 No Content)**

## 📅 Notification Service

### Create Notification Schedule
```http
POST /notification/schedule
```

**Payload:**
```json
{
  "cityName": "string",
  "uf": "string",
  "scheduleType": "ONCE | DAILY | WEEKLY",
  "time": "HH:mm",
  "dayOfWeek": 1,
  "executeAt": "2024-03-20T10:00:00",
  "endDate": "2024-12-31T23:59:59"
}
```

**Success Response (200 OK):**
```json
{
  "notificationId": "uuid",
  "nextExecutionTime": "datetime"
}
```

### Get All Notifications
```http
GET /notification/all
```

**Success Response (200 OK):**
```json
[
  {
    "notificationId": "uuid",
    "type": "ONCE | DAILY | WEEKLY",
    "status": "ACTIVE | PAUSED | CANCELLED",
    "cityName": "string",
    "uf": "string",
    "nextExecution": "datetime"
  }
]
```

### Get Notification by ID
```http
GET /notification/{notificationId}
```

**Success Response (200 OK):**
```json
{
  "notificationId": "uuid",
  "type": "ONCE | DAILY | WEEKLY",
  "status": "ACTIVE | PAUSED | CANCELLED",
  "cityName": "string",
  "uf": "string",
  "nextExecution": "datetime"
}
```

### Update Notification Status
```http
PUT /notification/{notificationId}/status
```

**Payload:**
```json
{
  "status": "ACTIVE | PAUSED | CANCELLED"
}
```

**Success Response (200 OK)**

### Subscribe to Weather Notifications
```http
GET /notification/subscribe
```
Server-Sent Events (SSE) endpoint que envia notificações em tempo real.

**Event Data Format:**
```json
{
  "event": "weather_alert",
  "data": {
    "cityName": "string",
    "uf": "string",
    "temperature": 25.5,
    "humidity": 80,
    "description": "string",
    "timestamp": "datetime"
  }
}
```

## 📋 Regras de Negócio

### Tipos de Agendamento

1. **ONCE**
   - Execução única em data/hora específica
   - Requer: executeAt
   - Não requer: time, dayOfWeek
   - Opcional: endDate

2. **DAILY**
   - Executa todos os dias no horário especificado
   - Requer: time
   - Não requer: executeAt, dayOfWeek
   - Opcional: endDate

3. **WEEKLY**
   - Executa em um dia específico da semana
   - Requer: time, dayOfWeek (1-7, onde 1 = Segunda-feira)
   - Não requer: executeAt
   - Opcional: endDate

### Validações

1. **Datas e Horários**
   - time: formato HH:mm (24h), entre 06:00 e 22:00
   - executeAt: data/hora futura
   - endDate: posterior a data atual e executeAt (se presente)
   - dayOfWeek: 1-7 (Segunda-Domingo)

2. **Localização**
   - cityName: nome da cidade válido (letras, espaços e hífens)
   - uf: sigla do estado válida (2 caracteres maiúsculos)

3. **Agendamento**
   - Não permite sobreposição de horários para mesma cidade
   - Máximo de 5 agendamentos ativos por usuário
   - Status possíveis: ACTIVE, PAUSED, CANCELLED

### Notificações em Tempo Real (SSE)

1. **Conexão**
   - Requer autenticação via token JWT no header Authorization
   - Reconexão automática em caso de queda
   - Timeout de 30 segundos

2. **Eventos**
   - connect: confirmação de conexão estabelecida
   - weather_alert: alertas de condições climáticas
   - error: notificação de erros

3. **Tratamento de Erros**
   - Reconexão exponencial (1s, 2s, 4s, 8s...)
   - Máximo de 3 tentativas por conexão
   - Notificação de falha após tentativas

### Códigos de Erro

1. **400 Bad Request**
   - Dados inválidos no payload
   - Validações de negócio não atendidas
   - Formato de data/hora inválido

2. **401 Unauthorized**
   - Token ausente ou inválido
   - Sessão expirada

3. **403 Forbidden**
   - Tentativa de acesso a recurso de outro usuário
   - Limite de agendamentos ativos excedido

4. **404 Not Found**
   - Notificação não encontrada
   - Cidade não encontrada

5. **409 Conflict**
   - Sobreposição de horários para mesma cidade
   - Email já registrado (no cadastro)
