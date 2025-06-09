import { Component, inject } from '@angular/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import {
  AbstractControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthResponse, AuthService } from '../../services/auth.service';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { catchError, delay, finalize, of } from 'rxjs';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    ReactiveFormsModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private messageService = inject(MessageService);
  private router = inject(Router);
  protected loginForm: FormGroup;
  protected isLoading = false;

  constructor() {
    this.loginForm = this.initializeLoginForm();
  }

  private initializeLoginForm(): FormGroup {
    return this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  protected getEmailControl(): AbstractControl<any, any> {
    return this.loginForm.controls['email'];
  }

  protected getPasswordControl(): AbstractControl<any, any> {
    return this.loginForm.controls['password'];
  }

  protected login(): void {
    if (this.isLoading) return;
    
    this.isLoading = true;
    console.log('Iniciando login com os dados:', this.loginForm.value);

    this.authService.login(
      this.loginForm.controls['email'].value,
      this.loginForm.controls['password'].value
    ).pipe(
      delay(2000), // Delay artificial de 2 segundos para testar o loading
      catchError(error => {
        console.error('Erro durante o login:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erro no Login',
          detail: error.message || 'Ocorreu um erro inesperado. Por favor, tente novamente.'
        });
        return of(null);
      }),
      finalize(() => {
        this.isLoading = false;
        console.log('Login finalizado');
      })
    ).subscribe({
      next: (response: AuthResponse | null) => {
        if (response) {
          console.log('Login realizado com sucesso:', response);
          this.messageService.add({
            severity: 'success',
            summary: 'Sucesso',
            detail: 'Login realizado com sucesso!'
          });
          this.router.navigate(['/home']);
        }
      }
    });
  }
}
