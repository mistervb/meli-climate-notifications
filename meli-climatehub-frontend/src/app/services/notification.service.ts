import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, Subject, throwError } from 'rxjs';
import { NotificationRequest, NotificationResponse, NotificationStatusUpdate, WeatherAlert, NotificationDetailsResponse } from '../interfaces/notification';
import { environment } from '../../environments/environment';
import { CookieService } from './cookie.service';
import { EventSourcePolyfill } from 'event-source-polyfill';
import { AuthService } from './auth.service';
import { catchError, retry, debounceTime } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = `${environment.apiUrl}/notification`;
  private weatherAlerts = new Subject<WeatherAlert>();
  private eventSource: EventSourcePolyfill | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000; // 3 segundos
  private lastHeartbeat: number = Date.now();
  private heartbeatTimeout: number = 30000; // 30 segundos
  private heartbeatCheckInterval: any;
  private statusUpdateSubject = new Subject<{notificationId: string, status: NotificationStatusUpdate}>();

  constructor(
    private http: HttpClient,
    private cookieService: CookieService,
    private authService: AuthService
  ) {
    // Configura o debounce para atualizações de status
    this.statusUpdateSubject.pipe(
      debounceTime(300) // Aguarda 300ms antes de processar a próxima atualização
    ).subscribe(update => {
      this.processStatusUpdate(update.notificationId, update.status);
    });
  }

  getAllNotifications(): Observable<NotificationDetailsResponse[]> {
    return this.http.get<NotificationDetailsResponse[]>(`${this.apiUrl}/all`);
  }

  scheduleNotification(request: NotificationRequest): Observable<NotificationResponse> {
    return this.http.post<NotificationResponse>(`${this.apiUrl}/schedule`, request);
  }

  updateNotificationStatus(notificationId: string, status: NotificationStatusUpdate): Observable<void> {
    // Envia a atualização para o subject que irá fazer o debounce
    this.statusUpdateSubject.next({notificationId, status});
    return new Observable(subscriber => {
      subscriber.next();
      subscriber.complete();
    });
  }

  private processStatusUpdate(notificationId: string, status: NotificationStatusUpdate): void {
    this.http.put<void>(`${this.apiUrl}/${notificationId}/status`, status)
      .pipe(
        retry(2), // Tenta a requisição até 2 vezes em caso de erro
        catchError((error: HttpErrorResponse) => {
          console.error('Erro ao atualizar status:', error);
          return throwError(() => error);
        })
      )
      .subscribe({
        error: (error) => {
          console.error('Erro após tentativas de retry:', error);
        }
      });
  }

  subscribeToWeatherAlerts(): Observable<WeatherAlert> {
    this.setupSSEConnection();
    return this.weatherAlerts.asObservable();
  }

  private setupSSEConnection() {
    this.cleanupConnection();

    const token = this.cookieService.get('token');
    if (!token) {
      console.error('Token não encontrado');
      return;
    }

    // Constrói a URL de forma segura
    const baseUrl = this.apiUrl.replace(/\/+$/, ''); // Remove trailing slashes
    const subscribeUrl = `${baseUrl}/subscribe?token=${encodeURIComponent(token)}`;
    
    this.eventSource = new EventSourcePolyfill(subscribeUrl, {
      withCredentials: true,
      heartbeatTimeout: 60000 // 60 segundos
    });

    this.setupEventListeners();
    this.startHeartbeatCheck();
  }

  private setupEventListeners() {
    if (!this.eventSource) return;

    this.eventSource.addEventListener('open', () => {
      console.log('SSE Connection established');
      this.reconnectAttempts = 0;
      this.lastHeartbeat = Date.now();
    });

    this.eventSource.addEventListener('heartbeat', () => {
      this.lastHeartbeat = Date.now();
      console.log('Heartbeat received');
    });

    this.eventSource.addEventListener('weather-notification', (event: any) => {
      try {
        const data = JSON.parse(event.data);
        const weatherAlert: WeatherAlert = {
          cityName: data.cityName || 'Cidade não informada',
          uf: data.uf || 'UF não informada',
          temperature: data.minTemp !== undefined && data.maxTemp !== undefined 
            ? `Min: ${data.minTemp}°C, Max: ${data.maxTemp}°C`
            : 'Temperatura não disponível',
          humidity: data.humidity !== undefined ? data.humidity : 'N/A',
          description: data.message || 'Sem descrição disponível',
          timestamp: data.date || new Date().toISOString()
        };
        this.weatherAlerts.next(weatherAlert);
      } catch (error) {
        console.error('Error parsing weather alert:', error);
      }
    });

    this.eventSource.addEventListener('error', (error) => {
      console.error('SSE Connection error:', error);
      this.handleConnectionError();
    });
  }

  private startHeartbeatCheck() {
    if (this.heartbeatCheckInterval) {
      clearInterval(this.heartbeatCheckInterval);
    }

    this.heartbeatCheckInterval = setInterval(() => {
      const now = Date.now();
      if (now - this.lastHeartbeat > this.heartbeatTimeout) {
        console.warn('No heartbeat received for too long, reconnecting...');
        this.handleConnectionError();
      }
    }, 5000);
  }

  private handleConnectionError() {
    this.cleanupConnection();

    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      
      setTimeout(() => {
        this.setupSSEConnection();
      }, this.reconnectDelay * this.reconnectAttempts);
    } else {
      console.error('Max reconnection attempts reached');
    }
  }

  private cleanupConnection() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    if (this.heartbeatCheckInterval) {
      clearInterval(this.heartbeatCheckInterval);
      this.heartbeatCheckInterval = null;
    }
  }
} 