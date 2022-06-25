import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

import { StopListComponent } from '../stop-list/stop-list.component';
import { TransListComponent } from '../trans-list/trans-list.component';

import { GeolocationService } from 'src/app/services/geolocation.service';
import { SettingsService } from 'src/app/services/settings.service';
import { RoutesService } from 'src/app/services/routes.service';
import { StopsService } from 'src/app/services/stops.service';
import { TransService } from 'src/app/services/trans.service';
import { CordovaService } from 'src/app/services/cordova.service';
import { AuthService } from 'src/app/services/auth.service';

import { Stop } from 'src/app/interfaces/stop';
import { Route } from 'src/app/interfaces/route';
import { map, Subscription, timer } from 'rxjs';


@Component({
  selector: 'app-main',
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.scss'],
})
export class MainComponent implements OnInit, OnDestroy {
  private _geoSubs: any;
  public isLoggedIn: boolean = false;
  private timerSubscription: Subscription = {} as Subscription;
  public coords: GeolocationCoordinates = {} as GeolocationCoordinates;
  public stopList: Stop[] = [];
  public selectedStop: Stop = {} as Stop;
  public routes: Route[] = this.routeSrv.possibleRoutes;
  public selectedRoutes: string[] = [];
  public notShownRotes: string[] = [];
  public audio = new Audio();
  public isTpmConnected: boolean = false;
  public isStopSelected = false;
  public isRouteSelected = true;
  public foundTrans: any[] = [];
  public peerList: any[] = [];
  public tpm: any = null;
  public doorOpened = false;

  constructor(
    public dialog: MatDialog,
    private geoSrv: GeolocationService,
    private matIconRegistry: MatIconRegistry,
    private domSanitzer: DomSanitizer,
    private stopSrv: StopsService,
    private routeSrv: RoutesService,
    private trSrv: TransService,
    private stgSrv: SettingsService,
    private authSrv: AuthService,
    private cSrv: CordovaService
  ) {
    this.matIconRegistry.addSvgIcon(
      'bus_stop',
      this.domSanitzer.bypassSecurityTrustResourceUrl(
        '/assets/img/bus-stop.svg'
      )
    );
  }

  ngOnInit(): void {
    this._geoSubs = this.geoSrv.position.subscribe(
      (res: GeolocationPosition) => {
        this.coords = res.coords;

        //zaliasis link centro
        // let coords: GeolocationCoordinates = {
        //   latitude: 54.692221,
        //   longitude: 25.279892,
        //   accuracy: 16,
        //   altitude: null,
        //   altitudeAccuracy: null,
        //   heading: null,
        //   speed: null,
        // };

        //zaliasis is centro
        // let coords: GeolocationCoordinates = {
        //   latitude: 54.6925139,
        //   longitude: 25.2804525,
        //   accuracy: 16,
        //   altitude: null,
        //   altitudeAccuracy: null,
        //   heading: null,
        //   speed: null,
        // };

        // let coords: GeolocationCoordinates = {
        //   latitude: 54.718597,
        //   longitude: 25.25766,
        //   accuracy: 16,
        //   altitude: null,
        //   altitudeAccuracy: null,
        //   heading: null,
        //   speed: null,
        // };
        // this.coords = coords;
        this.updateList();
        if (!this.isStopSelected) {
          this.isStopSelected = true;
          this.stopSrv.selectedStop = this.stopList[0];
          this.selectedStop = this.stopList[0];
          this.timerSubscription = timer(0, 22000)
            .pipe(
              map(() => {
                this.getRoutes();
                this.trSrv.initTrans();
              })
            )
            .subscribe();
          if (this.stgSrv.isAudioEnabled) {
            this.playAutoStop(this.stopList[0]);
          }
        }
      }
    );

    const wd = this.cSrv.cordova.plugins.wifi_direct;
    let node: any;
    new Promise((accept, reject) => {
      wd.getInstance(1, 1, 1, 1, 1, accept, reject);
    }).then((n) => {
      // console.log(n);
      node = n;
      console.log('start discovering');
      node.startDiscovering((peers: any) => {
        // console.log("this.peerList")
        // filter and save the robot peers
        this.peerList = peers.filter(() => true);
        console.log('found');
        console.log(peers);
        console.log(this.routes);
        this.peerList.forEach((el) => {
          if (el.address == '22:4e:f6:c2:02:11') {
            // console.log('found you');
            // console.log(this.trSrv.trans);
            this.foundTrans = [];
            this.trSrv.trans.forEach((trans) => {
              if (trans.tpm !== null) {
                if (trans.tpm.phy_address == el.address) {
                  this.foundTrans.push(trans);
                  this.trSrv.getTpm(trans.tpm.id).subscribe((res) => {
                    if (res.door) {
                      this.audio.pause();
                      this.tpm = res;
                      this.stgSrv.isAudioEnabled = false;
                      if (!this.doorOpened) {
                        this.audio.src =
                          'http://localhost/assets/sounds/intro/door_open.mp3';
                        this.audio.play();
                      }
                      this.doorOpened = true;
                      if(this.authSrv.isLoggedIn){
                        this.isTpmConnected = true;
                      }
                    } else {
                      if (this.doorOpened) {
                        this.audio.src =
                          'http://localhost/assets/sounds/intro/door_closed.mp3';
                        this.audio.play();
                        this.doorOpened = false;
                        this.isTpmConnected = false;
                        this.stgSrv.isAudioEnabled = true;
                        this.needHelp(false);
                      }
                    }
                  });
                }
              }
            });
            node.connect(
              el,
              (success: any) => {
                console.log('connection success.');
                // node.stopDiscovering();
              },
              (error: any) => {
                console.log(error);
              }
            );
          }
        });
      }, console.log('error'));

      console.log('The peeers');
      console.log(this.peerList);
    });
  }

  ngOnDestroy(): void {
    this._geoSubs.unsubscribe();
    this.timerSubscription.unsubscribe();
  }

  openStops() {
    const highestId = window.setTimeout(() => {
      for (let i = highestId; i >= 0; i--) {
        window.clearInterval(i);
      }
    }, 0);
    this.audio.pause();
    const dialogRef = this.dialog.open(StopListComponent, {
      data: {
        stops: this.stopList,
        selectedStop: this.selectedStop,
      },
    });

    dialogRef.afterClosed().subscribe((res) => {
      if (res) {
        if (this.selectedStop.stop_id != res.stop_id) {
          this.selectedStop = res;
          this.stopSrv.selectedStop = this.selectedStop;
          this.selectedRoutes = [];
          this.timerSubscription.unsubscribe();
          this.timerSubscription = timer(0, 20000)
            .pipe(
              //reset time again
              map(() => {
                this.getRoutes();
                this.trSrv.initTrans();
              })
            )
            .subscribe();
          if (this.stgSrv.isAudioEnabled) {
            this.playSelectedStop(this.selectedStop);
          }
        }
      }
    });
  }

  openRoutes() {
    const highestId = window.setTimeout(() => {
      for (let i = highestId; i >= 0; i--) {
        window.clearInterval(i);
      }
    }, 0);
    this.audio.pause();
    const dialogRef = this.dialog.open(TransListComponent, {
      data: {
        routes: this.routeSrv.possibleRoutes,
        selectedRoutes: this.selectedRoutes,
      },
    });

    dialogRef.afterClosed().subscribe((res) => {
      if (res) {
        this.selectedRoutes = [];
        res.forEach((r: string) => {
          this.selectedRoutes.push(r);
        });
        this.timerSubscription.unsubscribe();
        this.timerSubscription = timer(0, 22000)
          .pipe(
            map(() => {
              this.getSelectedRoutes();
            })
          )
          .subscribe();
      }
    });
  }

  private updateList() {
    let stops: Stop[] = this.stopSrv.stops;

    stops.forEach((elm) => {
      let dist = this.calculateDistance(elm);
      if (dist != -1) {
        elm.distance = dist;
      }
    });

    stops = stops.sort(function (a: Stop, b: Stop) {
      if (a.distance && b.distance) {
        return a.distance - b.distance;
      }
      return 0;
    });
    if (stops.length > 10) {
      this.stopList = stops.slice(0, 6);
    }
  }

  private toRad(Value: number): number {
    return (Value * Math.PI) / 180;
  }

  private calculateDistance(stop: Stop): number {
    if (stop.lat && stop.lng) {
      var R = 6371; // km
      var dLat = this.toRad(this.coords.latitude - stop.lat);
      var dLon = this.toRad(this.coords.longitude - stop.lng);
      var lat1 = this.toRad(stop.lat);
      var mylat = this.toRad(this.coords.latitude);

      var a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.sin(dLon / 2) *
          Math.sin(dLon / 2) *
          Math.cos(lat1) *
          Math.cos(mylat);
      var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      return R * c;
    }
    return 100;
  }

  preventMultipleRouteArrivals() {
    this.routes.forEach((r) => {
      if (r.arrival_time! == 0) {
        if (!this.notShownRotes.includes(r.rout_no!)) {
          this.notShownRotes.push(r.rout_no!);
        }
      }
    });
    let routes = this.routeSrv.possibleRoutes;
    this.routes = [];
    routes.forEach((r) => {
      if (this.notShownRotes.includes(r.rout_no!)) {
        if (r.arrival_time! > 0) {
          let index = this.notShownRotes.indexOf(r.rout_no!, 0);
          this.notShownRotes.splice(index, 1);
          this.routes.push(r);
        }
      } else {
        this.routes.push(r);
      }
    });
  }

  getRoutes() {
    this.routeSrv.initPublicTrans(this.selectedStop.stop_id);
    setTimeout(() => {
      this.preventMultipleRouteArrivals();

      // this.routes = this.routeSrv.possibleRoutes;
      setTimeout(() => {
        if (this.stgSrv.isAudioEnabled) {
          this.playRoutesAudio(this.routes);
        }
      }, 5000);
    }, 1000);
  }

  getSelectedRoutes() {
    this.routeSrv.initPublicTrans(this.selectedStop.stop_id);
    setTimeout(() => {
      this.preventMultipleRouteArrivals();

      let routes: Route[] = [];
      if (this.selectedRoutes.length > 0) {
        this.selectedRoutes.forEach((el) => {
          this.routes.forEach((r) => {
            if (r.rout_no! == el) {
              routes.push(r);
              return;
            }
          });
        });
        this.routes = routes;
      }
      if (this.stgSrv.isAudioEnabled) {
        this.playRoutesAudio(this.routes);
      }
    }, 1000);
  }

  replaceText(val: string) {
    let rVal: string = val.toLowerCase();
    rVal = rVal.replace('ą', 'a');
    rVal = rVal.replace('č', 'c');
    rVal = rVal.replace('ę', 'e');
    rVal = rVal.replace('ė', 'e');
    rVal = rVal.replace('į', 'i');
    rVal = rVal.replace('š', 's');
    rVal = rVal.replace('ū', 'u');
    rVal = rVal.replace('ų', 'u');
    rVal = rVal.replace('ž', 'z');
    rVal = rVal.replace('st.', 'st');
    rVal = rVal.replace('g.', 'g');

    return rVal;
  }

  playAutoStop(stop: Stop) {
    let sequence = 1;
    this.audio.src = 'http://localhost/assets/sounds/intro/aptikta_stotele.mp3';
    this.audio.onended = () => {
      if (sequence == 2) {
        sequence = 3;
        this.audio.src =
          'http://localhost/assets/sounds/destinations/' +
          this.replaceText(this.selectedStop.direction + '.mp3');
        if (this.stgSrv.isAudioEnabled) {
          this.audio.play();
        }
      }
      if (sequence == 1) {
        this.audio.src =
          'http://localhost/assets/sounds/stops/' +
          this.replaceText(this.selectedStop.name + '.mp3');
        if (this.stgSrv.isAudioEnabled) {
          this.audio.play();
        }
        sequence = 2;
      }
    };

    this.audio.preload = 'metadata';
    if (this.stgSrv.isAudioEnabled) {
      this.audio.play();
    }
  }

  playSelectedStop(stop: Stop) {
    let sequence = 1;
    this.audio.src =
      'http://localhost/assets/sounds/stops/' +
      this.replaceText(this.selectedStop.name + '.mp3');
    this.audio.onended = () => {
      if (sequence == 1) {
        this.audio.src =
          'http://localhost/assets/sounds/destinations/' +
          this.replaceText(this.selectedStop.direction + '.mp3');
        this.audio.play();
        sequence = 2;
      }
    };

    this.audio.preload = 'metadata';
    this.audio.play();
  }

  playRoutesAudio(routes: Route[]) {
    routes.forEach((r, i) => {
      if (i == 0) {
        this.playRouteAudio(r);
      } else {
        if (i < 5) {
          //play only first five
          setTimeout(() => {
            this.playRouteAudio(r);
          }, i * 5000);
        }
      }
    });
  }

  playRouteAudio(r: Route) {
    let sequence = 1;
    let val: string = r.rout_no!.toUpperCase();
    if (r.trans_type.includes('bus')) {
      val = 'A' + val;
    }
    if (r.trans_type.includes('trol')) {
      val = 'T' + val;
    }

    this.audio.src = 'http://localhost/assets/sounds/routes/' + val + '.mp3';
    this.audio.onended = () => {
      if (sequence == 2) {
        sequence = 3;
        this.audio.src =
          'http://localhost/assets/sounds/arrival_times/' +
          r.arrival_time!.toString() +
          'm.mp3';
        this.audio.play();
      }
      if (sequence == 1) {
        if (r.arrival_time! < 1 && r.arrival_time! >= 0) {
          this.audio.src =
            'http://localhost/assets/sounds/intro/atvyksta_i_stotele.mp3';
        } else {
          this.audio.src = 'http://localhost/assets/sounds/intro/atvyksta.mp3';
        }
        this.audio.play();
        sequence = 2;
      }
    };
    this.audio.play();
  }

  needHelp(helpNeeded: boolean){
    console.log(this.tpm);
    if(this.tpm != null){
      this.trSrv.needHelp(this.tpm, helpNeeded).subscribe((res) => {
        console.log("need help");
        console.log(res);
        console.log("---------");
      });
    }
  }

  getTransTypeStyle(trans_type: string) {
    if (trans_type.includes('expressbus')) {
      return 'number trans-bus-g';
    }

    if (trans_type.includes('bus')) {
      return 'number trans-bus';
    }

    if (trans_type.includes('trol')) {
      return 'number trans-trolley';
    }
    return 'number';
  }
}
