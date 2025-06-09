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

@Component({
  selector: 'app-register',
  imports: [
    CommonModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    ReactiveFormsModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
private fb = inject(NonNullableFormBuilder);
  registerForm: FormGroup;

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
}
