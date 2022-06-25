import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { environment } from 'src/environments/environment';
import { User } from '../interfaces/user';

@Injectable({
  providedIn: 'root',
})
export class ApiService {

  constructor(private http: HttpClient) {}

  getUser(id:number){
    return this.http.get<any>(environment.apiRoot.concat('usr/', id.toString(), '/'));
  }
}
