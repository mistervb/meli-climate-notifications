export interface NotificationRequest {
  cityName: string;
  uf: string;
  scheduleType: 'ONCE' | 'DAILY' | 'WEEKLY';
  time: string;
  dayOfWeek?: number;
  executeAt?: string;
  endDate?: string;
}

export interface NotificationResponse {
  notificationId: string;
  nextExecutionTime: string;
}

export interface NotificationStatusUpdate {
  status: 'ACTIVE' | 'PAUSED' | 'CANCELLED';
}

export interface WeatherAlert {
  cityName: string;
  uf: string;
  temperature: string;
  humidity: string;
  description: string;
  timestamp: string;
}

export interface NotificationDetailsResponse {
  notificationId: string;
  scheduleType: 'ONCE' | 'DAILY' | 'WEEKLY';
  status: 'ACTIVE' | 'PAUSED' | 'CANCELLED';
  cityName: string;
  uf: string;
  time: string;
  nextExecution: string;
}
