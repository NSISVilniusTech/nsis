import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import jwtDecode from 'jwt-decode';

import { User } from 'src/app/interfaces/user';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private currentUserSubject: BehaviorSubject<User> =
    {} as BehaviorSubject<User>;
  public currentUser: Observable<User> = {} as Observable<User>;
  public isLoggedIn = false;

  constructor(private http: HttpClient) {
    this.currentUserSubject = new BehaviorSubject<User>(
      JSON.parse(localStorage.getItem('currentUser') || '{}')
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): User {
    return this.currentUserSubject.value;
  }

  login(username: string, password: string) {
    return this.http
      .post<any>(`${environment.apiRoot}${environment.jwtLogin}`, {
        username,
        password,
      })
      .pipe(
        map((response) => {
          let currentUser: User = {} as User;
          if (response.access) {
            currentUser = jwtDecode(response.access);
            currentUser.token = response.access;
            currentUser.refreshToken = response.refresh;
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            this.currentUserSubject.next(currentUser);
          }
          return currentUser;
        })
      );
  }

  refreshToken() {
    const refreshToken = this.currentUserValue.refreshToken;
    return this.http
      .post<any>(`${environment.apiRoot}${environment.jwtRefresh}`, {
        refresh: refreshToken,
      })
      .pipe(
        map((response) => {
          let currentUser: User = {} as User;
          if (response.access) {
            currentUser = jwtDecode(response.access);
            currentUser.token = response.access;
            currentUser.refreshToken = response.refresh;
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            this.currentUserSubject.next(currentUser);
          }
          return currentUser;
        })
      );
  }

  logout() {
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next({} as User);
  }
}
