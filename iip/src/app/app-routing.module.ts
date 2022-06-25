import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MainComponent } from './gui/main/main.component';
import { MenuComponent } from './gui/menu/menu.component';

import { StopListComponent } from './gui/stop-list/stop-list.component';
import { TransListComponent } from './gui/trans-list/trans-list.component';

const routes: Routes = [
  { path: 'o', component: MenuComponent, children: [
    { path: 'main', component: MainComponent},
    { path: 'stop-list', component: StopListComponent },
    { path: 'trans-list', component: TransListComponent },
  ]},
  { path: '', redirectTo: '/o/main', pathMatch: 'full' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
