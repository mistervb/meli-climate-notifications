import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class BrowserNotificationService {
  private permission: NotificationPermission = 'default';

  constructor() {
    this.checkPermission();
  }

  private checkPermission() {
    if (!('Notification' in window)) {
      console.warn('Este navegador não suporta notificações desktop');
      return;
    }

    this.permission = Notification.permission;
  }

  async requestPermission(): Promise<boolean> {
    if (!('Notification' in window)) {
      return false;
    }

    try {
      const permission = await Notification.requestPermission();
      this.permission = permission;
      return permission === 'granted';
    } catch (error) {
      console.error('Erro ao solicitar permissão:', error);
      return false;
    }
  }

  showNotification(title: string, options?: NotificationOptions) {
    if (!('Notification' in window)) {
      return;
    }

    if (this.permission !== 'granted') {
      console.warn('Permissão para notificações não concedida');
      return;
    }

    try {
      new Notification(title, options);
    } catch (error) {
      console.error('Erro ao mostrar notificação:', error);
    }
  }
} 