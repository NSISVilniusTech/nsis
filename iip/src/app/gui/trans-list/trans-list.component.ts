import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MatChip } from '@angular/material/chips';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Route } from 'src/app/interfaces/route';

@Component({
  selector: 'app-trans-list',
  templateUrl: './trans-list.component.html',
  styleUrls: ['./trans-list.component.scss'],
})
export class TransListComponent implements OnInit, OnDestroy {
  selectedRoutes: string[] = [];
  constructor(
    public dialogRef: MatDialogRef<TransListComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  ngOnInit(): void {
    
  }

  ngOnDestroy(): void {}

  toggle(chip: MatChip) {
    if (!chip.selected) {
      console.log(this.selectedRoutes);
      if (this.selectedRoutes.length == 0) {
        this.selectedRoutes.push(chip.value.rout_no);
      } else {
        if (
          this.selectedRoutes.find((r) => r == chip.value.rout_no) ===
          undefined
        ) {
          this.selectedRoutes.push(chip.value.rout_no);
        }
      }
    } else {
      this.selectedRoutes.forEach((r, i) => {
        if (r == chip.value.rout_no) {
          this.selectedRoutes.splice(i, 1);
        }
      });
    }
    chip.toggleSelected();
  }

  onConfirm() {
    console.log(this.selectedRoutes);
    this.dialogRef.close(this.selectedRoutes);
  }
}
