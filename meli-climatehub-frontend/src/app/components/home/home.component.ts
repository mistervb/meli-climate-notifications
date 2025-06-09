import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { AuthService } from '../../services/auth.service';
import { FormGroup, FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { MultiSelectModule } from 'primeng/multiselect';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { TableModule } from 'primeng/table';
import { LocalDatePipe } from '../../pipes/local-date.pipe';
import { NotificationService } from '../../services/notification.service';
import { NotificationRequest, WeatherAlert, NotificationDetailsResponse } from '../../interfaces/notification';
import { Subscription } from 'rxjs';
import { NotificationValidators } from '../../shared/notification-validators';
import { Router } from '@angular/router';
import { BrowserNotificationService } from '../../services/browser-notification.service';
import { NotificationHistoryService } from '../../services/notification-history.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    RippleModule,
    CardModule,
    DialogModule,
    ReactiveFormsModule,
    DropdownModule,
    InputTextModule,
    CalendarModule,
    MultiSelectModule,
    ToastModule,
    TableModule,
    LocalDatePipe
  ],
  providers: [MessageService],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, OnDestroy {
  sidebarVisible: boolean = true;
  showLogoutDialog: boolean = false;
  showNewNotificationDialog: boolean = false;
  showHistoryDialog: boolean = false;
  private weatherSubscription?: Subscription;
  private historySubscription?: Subscription;
  minDate: Date = new Date();
  maxActiveSchedules: number = 5;
  notificationHistory: WeatherAlert[] = [];
  isLoading: boolean = false;

  get hasReachedMaxActiveSchedules(): boolean {
    return this.notifications.filter(n => n.status === 'ACTIVE').length >= this.maxActiveSchedules;
  }

  notificationForm!: FormGroup;

  scheduleTypes: any[] = [
    { label: 'Once', value: 'ONCE' },
    { label: 'Daily', value: 'DAILY' },
    { label: 'Weekly', value: 'WEEKLY' }
  ];

  daysOfWeek: any[] = [
    { label: 'Monday', value: 1 },
    { label: 'Tuesday', value: 2 },
    { label: 'Wednesday', value: 3 },
    { label: 'Thursday', value: 4 },
    { label: 'Friday', value: 5 },
    { label: 'Saturday', value: 6 },
    { label: 'Sunday', value: 7 }
  ];

  notifications: NotificationDetailsResponse[] = [];

  constructor(
    private authService: AuthService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private notificationService: NotificationService,
    private browserNotificationService: BrowserNotificationService,
    private notificationHistoryService: NotificationHistoryService,
    private router: Router
  ) {
    this.initForm();
  }

  async ngOnInit() {
    this.loadNotifications();
    this.subscribeToWeatherAlerts();
    this.subscribeToHistory();
    await this.browserNotificationService.requestPermission();
  }

  ngOnDestroy() {
    if (this.weatherSubscription) {
      this.weatherSubscription.unsubscribe();
    }
    if (this.historySubscription) {
      this.historySubscription.unsubscribe();
    }
  }

  private subscribeToHistory() {
    this.historySubscription = this.notificationHistoryService.getNotifications()
      .subscribe(notifications => {
        this.notificationHistory = notifications;
      });
  }

  private subscribeToWeatherAlerts() {
    this.weatherSubscription = this.notificationService.subscribeToWeatherAlerts()
      .subscribe({
        next: (alert: WeatherAlert) => {
          // Add to history
          this.notificationHistoryService.addNotification(alert);

          // Show toast
          this.messageService.add({
            severity: 'info',
            summary: `Weather Alert - ${alert.cityName}/${alert.uf}`,
            detail: `${alert.temperature}\n${alert.description}`,
            life: 10000 // 10 seconds
          });

          // Show browser notification
          this.browserNotificationService.showNotification(
            `Weather Alert - ${alert.cityName}/${alert.uf}`,
            {
              body: `${alert.temperature}\n${alert.description}`,
              icon: '/assets/weather-icon.png',
              tag: 'weather-alert',
              requireInteraction: true
            }
          );
        },
        error: (error) => {
          console.error('SSE connection error:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to connect to notification service.'
          });
        }
      });
  }

  clearHistory() {
    this.notificationHistoryService.clearHistory();
    this.messageService.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Notification history cleared successfully!'
    });
  }

  private initForm(): void {
    this.notificationForm = this.fb.group({
      cityName: ['', [Validators.required, Validators.pattern(/^[a-zA-ZÀ-ÿ\s\-]+$/)]],
      uf: ['', [Validators.required, Validators.pattern(/^[A-Z]{2}$/)]],
      scheduleType: ['', Validators.required],
      time: ['', [Validators.required, NotificationValidators.validTimeFormat()]],
      dayOfWeek: [null, NotificationValidators.validDayOfWeek()],
      executeAt: [null, NotificationValidators.futureDate()],
      endDate: [null]
    }, {
      validators: [
        NotificationValidators.validLocation(),
        NotificationValidators.noOverlappingSchedule(this.notifications)
      ]
    });

    // Adiciona validadores condicionais baseados no tipo de agendamento
    this.notificationForm.get('scheduleType')?.valueChanges.subscribe(type => {
      const dayOfWeekControl = this.notificationForm.get('dayOfWeek');
      const executeAtControl = this.notificationForm.get('executeAt');
      const endDateControl = this.notificationForm.get('endDate');

      // Reseta os validadores
      dayOfWeekControl?.clearValidators();
      executeAtControl?.clearValidators();
      endDateControl?.clearValidators();

      // Aplica os validadores apropriados
      if (type === 'WEEKLY') {
        dayOfWeekControl?.setValidators([
          Validators.required,
          NotificationValidators.validDayOfWeek()
        ]);
      } else if (type === 'ONCE') {
        executeAtControl?.setValidators([
          Validators.required,
          NotificationValidators.futureDate()
        ]);
      }

      // Validador de data final
      if (type === 'ONCE' && executeAtControl?.value) {
        endDateControl?.setValidators([
          NotificationValidators.endDateAfterExecuteAt('executeAt')
        ]);
      }

      // Atualiza o estado dos controles
      dayOfWeekControl?.updateValueAndValidity();
      executeAtControl?.updateValueAndValidity();
      endDateControl?.updateValueAndValidity();
    });
  }

  getErrorMessage(controlName: string): string {
    const control = this.notificationForm.get(controlName);
    if (!control || !control.errors || !control.touched) {
      return '';
    }

    const errors = control.errors;

    if (errors['required']) {
      return 'This field is required';
    }
    if (errors['pattern']) {
      switch (controlName) {
        case 'cityName':
          return 'Invalid city name';
        case 'uf':
          return 'State code must contain 2 uppercase letters';
        default:
          return 'Invalid format';
      }
    }
    if (errors['invalidTimeFormat']) {
      return 'Invalid time format (HH:mm)';
    }
    if (errors['invalidTimeRange']) {
      return 'Time must be between 06:00 and 22:00';
    }
    if (errors['invalidDayOfWeek']) {
      return 'Invalid day of week';
    }
    if (errors['futureDate']) {
      return 'Date must be in the future';
    }
    if (errors['endDateAfterExecuteAt']) {
      return 'End date must be after execution date';
    }
    if (errors['overlappingSchedule']) {
      return 'Already exists a schedule for this city at the same time';
    }
    if (errors['maxActiveSchedulesExceeded']) {
      return `Maximum of ${this.maxActiveSchedules} active schedules allowed`;
    }

    return 'Invalid field';
  }

  private loadNotifications() {
    this.notificationService.getAllNotifications().subscribe({
      next: (notifications) => {
        console.log('Raw notifications data:', notifications);
        // Ensure nextExecutionTime is properly formatted
        this.notifications = notifications.map(notification => {
          console.log('Processing notification:', notification);
          if (notification.nextExecution) {
            console.log('Original nextExecutionTime:', notification.nextExecution);
            // Ensure the date is in the correct format for the pipe
            notification.nextExecution = new Date(notification.nextExecution).toISOString();
            console.log('Processed nextExecutionTime:', notification.nextExecution);
          }
          return notification;
        });
      },
      error: (error) => {
        console.error('Erro ao carregar notificações:', error);
        if (error.status === 401) {
          this.messageService.add({
            severity: 'error',
            summary: 'Erro',
            detail: 'Sua sessão expirou. Por favor, faça login novamente.'
          });
          this.authService.logout();
          this.router.navigate(['/login']);
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Erro',
            detail: 'Não foi possível carregar as notificações.'
          });
        }
      }
    });
  }

  saveNotification() {
    if (this.notificationForm.valid && !this.hasReachedMaxActiveSchedules) {
      this.isLoading = true;
      const formValue = this.notificationForm.value;
      const request: NotificationRequest = {
        cityName: formValue.cityName,
        uf: formValue.uf,
        scheduleType: formValue.scheduleType,
        time: this.formatTime(formValue.time),
        dayOfWeek: formValue.dayOfWeek || undefined,
        executeAt: formValue.executeAt ? new Date(formValue.executeAt).toISOString() : undefined,
        endDate: formValue.endDate ? new Date(formValue.endDate).toISOString() : undefined
      };

      // Remove campos undefined antes de enviar
      Object.keys(request).forEach(key => {
        if (request[key as keyof NotificationRequest] === undefined) {
          delete request[key as keyof NotificationRequest];
        }
      });

      console.log('Enviando requisição:', request);

      this.notificationService.scheduleNotification(request).subscribe({
        next: (response) => {
          console.log('Resposta do servidor:', response);
          this.showNewNotificationDialog = false;
          this.messageService.add({
            severity: 'success',
            summary: 'Sucesso',
            detail: 'Notificação agendada com sucesso!'
          });
          // Recarrega a lista de notificações após criar uma nova
          this.loadNotifications();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Erro detalhado:', error);
          let errorMessage = 'Não foi possível agendar a notificação. ';

          if (error.status === 401) {
            errorMessage = 'Sua sessão expirou. Por favor, faça login novamente.';
            this.authService.logout();
            this.router.navigate(['/login']);
          } else if (error.error?.message) {
            errorMessage += error.error.message;
          } else if (error.status === 0) {
            errorMessage += 'Servidor indisponível. Verifique sua conexão.';
          } else if (error.status === 400) {
            errorMessage += 'Dados inválidos. Verifique os campos e tente novamente.';
          } else if (error.status === 409) {
            errorMessage += 'Conflito de horário detectado.';
          } else {
            errorMessage += 'Por favor, tente novamente mais tarde.';
          }

          this.messageService.add({
            severity: 'error',
            summary: 'Erro',
            detail: errorMessage
          });
          this.isLoading = false;
        }
      });
    } else if (this.hasReachedMaxActiveSchedules) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erro',
        detail: `Você já atingiu o limite de ${this.maxActiveSchedules} agendamentos ativos.`
      });
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Erro',
        detail: 'Por favor, corrija os erros no formulário antes de salvar.'
      });
    }
  }

  toggleNotificationStatus(notification: NotificationDetailsResponse) {
    const newStatus = notification.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';

    // Verifica se atingiu o limite de notificações ativas
    if (newStatus === 'ACTIVE' && this.hasReachedMaxActiveSchedules) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erro',
        detail: `Você já atingiu o limite de ${this.maxActiveSchedules} agendamentos ativos.`
      });
      return;
    }

    this.notificationService.updateNotificationStatus(notification.notificationId, { status: newStatus }).subscribe({
      next: () => {
        notification.status = newStatus;
        this.messageService.add({
          severity: 'info',
          summary: 'Status Atualizado',
          detail: `Notificação ${newStatus === 'ACTIVE' ? 'ativada' : 'pausada'}`
        });
      },
      error: (error) => {
        console.error('Erro ao atualizar status:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erro',
          detail: 'Não foi possível atualizar o status da notificação.'
        });
      }
    });
  }

  deleteNotification(notification: NotificationDetailsResponse) {
    this.notificationService.updateNotificationStatus(notification.notificationId, { status: 'CANCELLED' }).subscribe({
      next: () => {
        this.notifications = this.notifications.filter(n => n.notificationId !== notification.notificationId);
        this.messageService.add({
          severity: 'success',
          summary: 'Sucesso',
          detail: 'Notificação removida com sucesso!'
        });
      },
      error: (error) => {
        console.error('Erro ao remover notificação:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erro',
          detail: 'Não foi possível remover a notificação.'
        });
      }
    });
  }

  private formatTime(date: Date): string {
    return date.toTimeString().slice(0, 5); // Retorna apenas HH:mm
  }

  toggleSidebar() {
    this.sidebarVisible = !this.sidebarVisible;
    // Aqui você pode adicionar lógica adicional para manipular a visibilidade do sidebar em dispositivos móveis
  }

  confirmLogout() {
    this.showLogoutDialog = true;
  }

  logout() {
    this.authService.logout().then(() => {
      this.showLogoutDialog = false;
    });
  }

  hideLogoutDialog() {
    this.showLogoutDialog = false;
  }

  openNewNotificationDialog() {
    this.showNewNotificationDialog = true;
    this.initForm();
  }
}
