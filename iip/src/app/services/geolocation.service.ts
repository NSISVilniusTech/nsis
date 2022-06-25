import { Injectable, NgZone } from '@angular/core';

import { Observable, Subscription } from 'rxjs';
// import { mergeMap } from 'rxjs/operators';

// import { Cordova, ZoneObservable } from './cordova_bak';

@Injectable({
  providedIn: 'root',
})
export class GeolocationService {
  private _position$ = new Observable((observer:any) => {
    let watchId: number;

    watchId = navigator.geolocation.watchPosition(
      (position: GeolocationPosition) => {
        observer.next(position);
      },
      (error: GeolocationPositionError) => {
        observer.error(error);
      }
    );
    return {
      unsubscribe() {
        navigator.geolocation.clearWatch(watchId);
      },
    };
  });

  constructor() {}

  get position(): Observable<any> {
    return this._position$;
  }
}
