import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export interface CookieOptions {
  expires?: Date;
  path?: string;
  secure?: boolean;
  sameSite?: 'Strict' | 'Lax' | 'None';
}

@Injectable({
  providedIn: 'root'
})
export class CookieService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser: boolean;

  constructor() {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  get(name: string): string | null {
    if (!this.isBrowser) return null;
    
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop()?.split(';').shift() || null;
    }
    return null;
  }

  set(name: string, value: string, options: CookieOptions = {}): void {
    if (!this.isBrowser) return;

    const defaultOptions: CookieOptions = {
      path: '/',
      secure: true,
      sameSite: 'Strict'
    };

    const finalOptions = { ...defaultOptions, ...options };
    let cookieString = `${name}=${value}`;

    if (finalOptions.expires) {
      cookieString += `;expires=${finalOptions.expires.toUTCString()}`;
    }

    if (finalOptions.path) {
      cookieString += `;path=${finalOptions.path}`;
    }

    if (finalOptions.secure) {
      cookieString += ';secure';
    }

    if (finalOptions.sameSite) {
      cookieString += `;SameSite=${finalOptions.sameSite}`;
    }

    document.cookie = cookieString;
  }

  remove(name: string, options: CookieOptions = {}): void {
    if (!this.isBrowser) return;

    const defaultOptions: CookieOptions = {
      path: '/',
      secure: true,
      sameSite: 'Strict'
    };

    const finalOptions = { ...defaultOptions, ...options };
    let cookieString = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT`;

    if (finalOptions.path) {
      cookieString += `;path=${finalOptions.path}`;
    }

    if (finalOptions.secure) {
      cookieString += ';secure';
    }

    if (finalOptions.sameSite) {
      cookieString += `;SameSite=${finalOptions.sameSite}`;
    }

    document.cookie = cookieString;
  }

  exists(name: string): boolean {
    return !!this.get(name);
  }
} 