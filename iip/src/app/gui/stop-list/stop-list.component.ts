import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSelectionListChange } from '@angular/material/list';

import { Stop } from 'src/app/interfaces/stop';

@Component({
  selector: 'app-stop-list',
  templateUrl: './stop-list.component.html',
  styleUrls: ['./stop-list.component.scss'],
})
export class StopListComponent implements OnInit, OnDestroy {
  selectedStop: Stop = {} as Stop;
  constructor(
    public dialogRef: MatDialogRef<StopListComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  ngOnInit(): void {
    this.selectedStop = this.data.selectedStop;
  }

  ngOnDestroy(): void {}

  showDistance(val: number | undefined): string {
    if (val) {
      if (val < 1) {
        return String((val * 1000).toFixed()) + ' m';
      } else {
        return String(val.toFixed(2)) + ' km';
      }
    }
    return '';
  }

  onChange(change: MatSelectionListChange) {
    this.selectedStop = change.options[0].value;
    this.dialogRef.close(this.selectedStop);
  }
}
