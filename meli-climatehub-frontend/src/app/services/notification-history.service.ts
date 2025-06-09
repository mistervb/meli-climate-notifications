import { Injectable } from '@angular/core';
import { WeatherAlert } from '../interfaces/notification';
import { BehaviorSubject, Observable } from 'rxjs';
import { CookieService } from './cookie.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationHistoryService {
  private readonly STORAGE_KEY = 'weather_notifications';
  private readonly MAX_HISTORY = 50;
  private notifications = new BehaviorSubject<WeatherAlert[]>([]);

  constructor(private cookieService: CookieService) {
    this.loadFromStorage();
  }

  private loadFromStorage() {
    try {
      const stored = this.cookieService.get(this.STORAGE_KEY);
      if (stored) {
        const notifications = JSON.parse(stored);
        this.notifications.next(notifications);
      }
    } catch (error) {
      console.error('Erro ao carregar notificações do storage:', error);
    }
  }

  private saveToStorage(notifications: WeatherAlert[]) {
    try {
      this.cookieService.set(this.STORAGE_KEY, JSON.stringify(notifications));
    } catch (error) {
      console.error('Erro ao salvar notificações no storage:', error);
    }
  }

  addNotification(notification: WeatherAlert) {
    const current = this.notifications.value;
    const updated = [notification, ...current].slice(0, this.MAX_HISTORY);
    this.notifications.next(updated);
    this.saveToStorage(updated);
  }

  getNotifications(): Observable<WeatherAlert[]> {
    return this.notifications.asObservable();
  }

  clearHistory() {
    this.notifications.next([]);
    this.cookieService.remove(this.STORAGE_KEY);
  }
} 