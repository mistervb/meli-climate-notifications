import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, throwError, from } from 'rxjs';
import { tap, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { CookieService, CookieOptions } from './cookie.service';
import { BehaviorSubject } from 'rxjs';
import { jwtDecode } from 'jwt-decode';

export interface AuthResponse {
  status: string;
  statusCode: number;
  data: {
    token?: string;
    userId?: string;
    username?: string;
    createdAt?: string;
    updatedAt?: string;
  }
}

interface LoginResponse {
  status: string;
  statusCode: number;
  data: {
    token: string;
  };
}

interface DecodedToken {
  sub: string;
  userId: string;
  exp: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080';
  private readonly TOKEN_COOKIE = 'token';
  private platformId = inject(PLATFORM_ID);
  private cookieService = inject(CookieService);
  private isBrowser: boolean;
  private currentUserSubject: BehaviorSubject<string | null>;
  public currentUser: Observable<string | null>;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    this.currentUserSubject = new BehaviorSubject<string | null>(this.cookieService.get(this.TOKEN_COOKIE));
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): string | null {
    return this.currentUserSubject.value;
  }

  private getHttpOptions() {
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }),
      withCredentials: true
    };
  }

  private setToken(token: string): void {
    const cookieOptions: CookieOptions = {
      expires: new Date(Date.now() + 3600000), // 1 hora
      secure: true,
      sameSite: 'Strict',
      path: '/'
    };

    this.cookieService.set(this.TOKEN_COOKIE, token, cookieOptions);
    console.log('Token salvo no cookie:', this.TOKEN_COOKIE);
  }

  getToken(): string | null {
    const token = this.cookieService.get(this.TOKEN_COOKIE);
    console.log('Token recuperado do cookie:', token ? 'presente' : 'ausente');
    return token;
  }

  isAuthenticated(): boolean {
    if (!this.isBrowser) {
      return false;
    }
    const hasToken = this.cookieService.exists(this.TOKEN_COOKIE);
    console.log('Status de autenticação:', hasToken);
    return hasToken;
  }

  logout(): Promise<boolean> {
    console.log('Iniciando logout...');
    const cookieOptions: CookieOptions = {
      secure: true,
      sameSite: 'Strict',
      path: '/'
    };

    console.log('Removendo token do cookie');
    this.cookieService.remove(this.TOKEN_COOKIE, cookieOptions);
    this.currentUserSubject.next(null);
    return this.router.navigate(['/login']);
  }

  private handleError(error: HttpErrorResponse) {
    console.error('An error occurred:', error);
    let errorMessage = 'Ocorreu um erro no servidor. Por favor, tente novamente mais tarde.';

    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erro: ${error.error.message}`;
      return throwError(() => ({ message: errorMessage, status: error.status }));
    }

    if (error.status === 0) {
      errorMessage = 'Não foi possível conectar ao servidor. Por favor, verifique sua conexão.';
    } else if (error.status === 400) {
      errorMessage = error.error?.message || 'Dados inválidos. Verifique as informações e tente novamente.';
    } else if (error.status === 401) {
      errorMessage = 'Sessão expirada. Por favor, faça login novamente.';
      // Retorna uma Promise que será resolvida após o logout e navegação
      return from(this.logout()).pipe(
        switchMap(() => throwError(() => ({ message: errorMessage, status: error.status })))
      );
    } else if (error.status === 409) {
      errorMessage = 'Email ou nome de usuário já cadastrado.';
    }

    return throwError(() => ({ message: errorMessage, status: error.status }));
  }

  login(email: string, password: string): Observable<AuthResponse> {
    console.log('Tentando fazer login com:', { email });
    return this.http.post<AuthResponse>(
      `${this.API_URL}/user/login`,
      { email, password },
      this.getHttpOptions()
    ).pipe(
      tap(response => {
        console.log('Resposta do login:', response);
        if (response.data?.token) {
          this.setToken(response.data.token);
          this.currentUserSubject.next(response.data.token);
          this.router.navigate(['/home']);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  register(username: string, email: string, password: string): Observable<AuthResponse> {
    if (!this.isBrowser) {
      return throwError(() => ({ message: 'Registro não disponível durante SSR', status: 400 }));
    }

    console.log('Tentando registrar usuário:', { username, email });
    return this.http.post<AuthResponse>(
      `${this.API_URL}/user/register`,
      {
        username,
        email,
        passwordHashed: password
      },
      this.getHttpOptions()
    ).pipe(
      tap(response => {
        console.log('Resposta do registro:', response);
        if (response.statusCode === 201 && response.data?.token) {
          this.setToken(response.data.token);
          this.currentUserSubject.next(response.data.token);
          this.router.navigate(['/home']);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  getUserId(): string {
    const token = this.currentUserValue;
    if (!token) return '';

    try {
      const decoded = jwtDecode<DecodedToken>(token);
      return decoded.userId;
    } catch {
      return '';
    }
  }
}
