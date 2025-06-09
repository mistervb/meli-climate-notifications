import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SettingsService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  optIn(userId: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/user/${userId}/opt-in`, {});
  }

  optOut(userId: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/user/${userId}/opt-out`, {});
  }
} 