import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { Stop } from '../interfaces/stop';
import { environment } from 'src/environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class StopsService {
  private _publicURL = 'https://www.stops.lt/vilnius/vilnius/stops.txt';
  private _stopsData: Stop[] = [];
  private _selectedStop: Stop = {} as Stop;

  constructor(private http: HttpClient) {}

  private getPublicStops() {
    return this.http.get<any>(this._publicURL, {
      responseType: 'text' as 'json',
    });
  }

  private getStops() {
    return this.http.get<Stop[]>(environment.apiStops);
  }

  private addStop(arr: Array<string>): Stop {
    let stop: Stop = {} as Stop;

    for (let i = 0; i < arr.length; i++) {
      switch (i) {
        case 0:
          stop.stop_id = arr[i];
          break;
        case 1:
          stop.direction = arr[i];
          break;
        case 2:
          if (arr[i] !== '0') {
            stop.lat = Number(arr[i]) / 1e5;
          }
          break;
        case 3:
          if (arr[i] !== '0') {
            stop.lng = Number(arr[i]) / 1e5;
          }
          break;
        case 4:
          stop.stops = arr[i].split(',');
          break;
        case 5:
          stop.name = arr[i];
          break;
        case 6:
          stop.street = arr[i];
          break;
        default:
          break;
      }
    }
    return stop;
  }

  public initStops() {
    if (localStorage.getItem('stops') === null) {
      this.getStops().subscribe({
        next: (data) => {
          localStorage.setItem('stops', JSON.stringify(data));
          this._stopsData = data;
        },
        error: (er) => {},
      });
    } else {
      //try tu update and check differences
      this._stopsData = JSON.parse(localStorage.getItem('stops') || '{}');
    }
  }

  public initPublicStops() {
    this.getPublicStops().subscribe((data) => {
      let arr: Array<string> = data.split('\n');

      arr = arr.slice(1);

      let tmp = Array(10).fill('');

      arr.forEach((elm) => {
        let data = elm.split(';');

        for (let i = 0; i < data.length; i++) {
          if (i == 1 || i == 4) {
            if (data[i] === '') {
              tmp[i] = '';
              continue;
            }
          }
          if (data[i] === '0') {
            tmp[i] = '';
            continue;
          }
          if (data[i] !== '') {
            tmp[i] = data[i];
            continue;
          }
        }
        this._stopsData.push(this.addStop(tmp));
      });
    });
  }

  get stops(): Stop[] {
    return this._stopsData;
  }

  get selectedStop(): Stop {
    return this._selectedStop;
  }

  set selectedStop(st: Stop){
    this._selectedStop = st;
  }
}
