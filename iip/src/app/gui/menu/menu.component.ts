import { Component, OnInit, ViewChild } from '@angular/core';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';

import { MatSidenav } from '@angular/material/sidenav';
import { MatDialog } from '@angular/material/dialog';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';

import { LoginComponent } from '../login/login.component';

import { AuthService } from 'src/app/services/auth.service';
import { ApiService } from 'src/app/services/api.service';
import { SettingsService } from 'src/app/services/settings.service';

@Component({
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.scss'],
})
export class MenuComponent implements OnInit {
  @ViewChild('sidenav') sidenav?: MatSidenav;
  public isLoggedIn: boolean = false;
  public userName: string = '';

  public isAudioEnabled: boolean = true;

  isHandset$: Observable<boolean> = this.breakpointObserver
    .observe(Breakpoints.Handset)
    .pipe(
      map((result) => result.matches),
      shareReplay()
    );

  constructor(
    private breakpointObserver: BreakpointObserver,
    private authSrv: AuthService,
    private apiSrv: ApiService,
    private stgSrv: SettingsService,
    public dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.isAudioEnabled = this.stgSrv.isAudioEnabled;
    if (Object.keys(this.authSrv.currentUserValue).length != 0) {
      this.isLoggedIn = true;
      this.authSrv.isLoggedIn = true;
      this.apiSrv.getUser(this.authSrv.currentUserValue.user_id!).subscribe({
        next: (data) => {
          this.userName = data.username;
        },
        error: (er) => {},
      });
    }
  }

  onLogin() {
    const dialogRef = this.dialog.open(LoginComponent);

    dialogRef.afterClosed().subscribe((res) => {
      if (Object.keys(this.authSrv.currentUserValue).length != 0) {
        this.isLoggedIn = true;
        this.authSrv.isLoggedIn = true;
        this.apiSrv.getUser(this.authSrv.currentUserValue.user_id!).subscribe({
          next: (data) => {
            this.userName = data.username;
          },
          error: (er) => {},
        });
      }
    });
  }

  onLogout() {
    this.authSrv.logout();
    this.isLoggedIn = false;
    this.authSrv.isLoggedIn = false;
    this.userName = '';
  }

  onAudioChanged($event: MatSlideToggleChange) {
    this.stgSrv.isAudioEnabled = $event.checked;
  }
}
