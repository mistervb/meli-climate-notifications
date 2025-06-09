import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { passwordMatchValidator } from '../../shared/password-match.directive';
import { AuthResponse, AuthService } from '../../services/auth.service';
import { catchError, delay, finalize, of } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-register',
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
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private fb = inject(NonNullableFormBuilder);
  private authService: AuthService = inject(AuthService);
  private messageService = inject(MessageService);
  protected registerForm: FormGroup;
  protected isLoading = false;

  constructor() {
    this.registerForm = this.initializeRegisterForm();
  }

  private initializeRegisterForm(): FormGroup {
    return this.fb.group({
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
      confirmPassword: ['', Validators.required]
    }, {
      validators: passwordMatchValidator
    });
  }

  protected getUsernameControl(): AbstractControl<any, any> {
    return this.registerForm.controls['username'];
  }

  protected getEmailControl(): AbstractControl<any, any> {
    return this.registerForm.controls['email'];
  }

  protected getPasswordControl(): AbstractControl<any, any> {
    return this.registerForm.controls['password'];
  }

  protected getConfirmPasswordControl(): AbstractControl<any, any> {
    return this.registerForm.controls['confirmPassword'];
  }

  protected registerUser(): void {
    if (this.isLoading) return;
    
    this.isLoading = true;
    console.log('Iniciando registro com os dados:', this.registerForm.value);

    this.authService.register(
      this.registerForm.controls['username'].value,
      this.registerForm.controls['email'].value,
      this.registerForm.controls['password'].value
    ).pipe(
      delay(2000), // Delay artificial de 2 segundos para testar o loading
      catchError(error => {
        console.error('Erro durante o registro:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erro no Registro',
          detail: error.message || 'Ocorreu um erro inesperado. Por favor, tente novamente.'
        });
        return of(null);
      }),
      finalize(() => {
        this.isLoading = false;
        console.log('Registro finalizado');
      })
    ).subscribe({
      next: (response: AuthResponse | null) => {
        if (response) {
          console.log('Registro realizado com sucesso:', response);
          this.messageService.add({
            severity: 'success',
            summary: 'Sucesso',
            detail: 'Registro realizado com sucesso!'
          });
        }
      }
    });
  }
}
