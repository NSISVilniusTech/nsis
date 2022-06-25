import { Component, OnInit } from '@angular/core';
import { CordovaService } from 'src/app/services/cordova.service';

declare var WifiDirect: any;
declare var WifiDirectNode: any;
@Component({
  selector: 'app-info',
  templateUrl: './info.component.html',
  styleUrls: ['./info.component.scss'],
})
export class InfoComponent implements OnInit {
  public isStopSelected = false;
  public isRouteSelected = false;

  peerList: any[] = [];
  constructor(private cSrv: CordovaService) {}

  ngOnInit(): void {
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

        this.peerList.forEach((el) => {
          if (el.address == '22:4e:f6:c2:02:11') {
            console.log('found you');
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
}
