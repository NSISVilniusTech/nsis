import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Route } from '../interfaces/route';

@Injectable({
  providedIn: 'root',
})
export class RoutesService {
  private _publicURL = 'https://www.stops.lt/vilnius/departures2.php?stopid=';
  private _routesData: Route[] = [];
  private _possibleRoutes: Route[] = [];
  private _routes: Route[] = [];

  constructor(private http: HttpClient) {}

  private getPublicTrans(stopId: number) {
    let id: string = "";
    if(stopId < 1000){
      id = '0' + stopId.toString()
    }
    else{
      id = stopId.toString();
    }
    return this.http.get<any>(this._publicURL + id, {
      responseType: 'text' as 'json',
    });
  }

  private addTrans(arr: string[]): Route {
    let route: Route = {} as Route;

    route.trans_type = arr[0];
    route.rout_no = arr[1];
    route.rout_direction = arr[2];
    route.arrival_time = Math.round(+arr[3] / 60);
    route.trans_no = arr[4];
    route.trans_no = route.trans_no.replace('K', '');
    route.trans_no = route.trans_no.replace('W', '');
    route.trans_no = route.trans_no.replace('N', '');
    route.trans_no = route.trans_no.replace('Z', '');
    route.trans_no = route.trans_no.replace('D', '');
    route.last_stop = arr[5];

    return route;
  }

  public initPublicTrans(stopId: any) {
    this.getPublicTrans(stopId).subscribe((data) => {
      this.processRoutes(data);
    });
  }

  private timerCalculation(timeNumber: number): number {
    // const now = new Date();
    // let seconds = new Date().getTime() / 1000;

    let d = new Date(),
      e = new Date(d);
    let sTimeSinceMidnight: number = Math.round(
      (+d - +d.setHours(0, 0, 0, 0)) / 1000
    );

    if (timeNumber > 86400) {
      timeNumber = timeNumber - 86400;
    }
    let sTimeDiffTillArrive: number = timeNumber - sTimeSinceMidnight;

    return sTimeDiffTillArrive;
  }

  private processRoutes(data: any) {
    let arr: string[] = data.split('\n');

    arr = arr.slice(1);
    this._routesData = [];
    this._possibleRoutes = [];

    arr.forEach((elm) => {
      if (!elm && elm.length == 0) {
      } else {
        let lin_arr: string[] = elm.split(',');
        let tmp = Array(6).fill('');

        let i = 0;

        lin_arr.forEach((lin) => {
          if (i < 6) {
            if (i == 3) {
              if (this.timerCalculation(+lin) < 0) {
                tmp[i] = 0;
              } else {
                tmp[i] = this.timerCalculation(+lin);
              }
            } else {
              tmp[i] = lin;
            }
            i++;
          }
        });

        this._routesData.push(this.addTrans(tmp));
      }
    });

    this._routesData = this._routesData.sort((a, b) =>
      a.arrival_time! < b.arrival_time! ? -1 : 1
    );

    this._routesData.forEach((route) => {
      if (this._possibleRoutes.length == 0) {
        this._possibleRoutes.push(route);
      } else {
        if (this._possibleRoutes.find(r => r.rout_no === route.rout_no) === undefined) {
          this._possibleRoutes.push(route);
        }
      }
    });
  }


  get possibleRoutes(): Route[] {
    return this._possibleRoutes;
  }


}
