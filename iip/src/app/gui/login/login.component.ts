import { Input, Component, Output, EventEmitter, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';

import { ApiService } from 'src/app/services/api.service';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit {
  @Input() error: string | null = '';
  @Output() submitEM = new EventEmitter();

  constructor(
    public dialogRef: MatDialogRef<LoginComponent>,
    private authSrv: AuthService,
    private apiSrv: ApiService
  ) {}

  ngOnInit(): void {
    this.authSrv.logout();
  }

  form: FormGroup = new FormGroup({
    username: new FormControl('', [Validators.required]),
    password: new FormControl('', [Validators.required]),
  });

  onSubmit() {
    if (this.form.value['username'] == '') {
      this.form.controls['username'].markAsTouched();
    }
    if (this.form.value['password'] == '') {
      this.form.controls['password'].markAsTouched();
    }

    if (this.form.valid) {
      this.authSrv
        .login(this.form.value['username'], this.form.value['password'])
        .subscribe({
          next: (data) => {
            this.dialogRef.close();
          },
          error: (er) => {
            console.log(er);
            if (er.status > 401 || er.status == 0) {
              this.form.controls['username'].setErrors({ incorrect: true });
              this.form.controls['password'].setErrors({ incorrect: true });
            }
          },
        });
    }
  }
}
