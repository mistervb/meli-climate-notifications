import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../../services/settings.service';
import { AuthService } from '../../services/auth.service';
import { MessageService } from 'primeng/api';

// PrimeNG Imports
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ToastModule } from 'primeng/toast';
import { InputSwitchModule } from 'primeng/inputswitch';
import { AccordionModule } from 'primeng/accordion';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    CardModule,
    ToastModule,
    InputSwitchModule,
    AccordionModule
  ],
  providers: [MessageService],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {
  notificationsEnabled: boolean = true;
  isLoading: boolean = false;
  hasUnsavedChanges: boolean = false;
  appVersion: string = '1.0.0';
  userId: string = '';

  constructor(
    private settingsService: SettingsService,
    private authService: AuthService,
    private messageService: MessageService
  ) {}

  ngOnInit() {
    this.userId = this.authService.getUserId();
  }

  onNotificationToggleChange() {
    this.hasUnsavedChanges = true;
  }

  async saveChanges() {
    if (!this.hasUnsavedChanges) return;

    this.isLoading = true;
    try {
      if (this.notificationsEnabled) {
        await this.settingsService.optIn(this.userId).toPromise();
      } else {
        await this.settingsService.optOut(this.userId).toPromise();
      }
      
      this.messageService.add({
        severity: 'success',
        summary: 'Sucesso',
        detail: 'Configurações salvas com sucesso!'
      });
      
      this.hasUnsavedChanges = false;
    } catch (error) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erro',
        detail: 'Erro ao salvar as configurações. Tente novamente.'
      });
    } finally {
      this.isLoading = false;
    }
  }
} 