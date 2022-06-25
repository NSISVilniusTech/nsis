import { Component } from '@angular/core';

import { StopsService } from './services/stops.service';
import { TransService } from './services/trans.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  constructor(private stpSrv: StopsService, private trSrv: TransService) {}

  ngOnInit(): void {
    this.stpSrv.initStops();
    this.trSrv.initTrans();
  }
}
