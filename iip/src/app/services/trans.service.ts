import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root',
})
export class TransService {
  private _transData: any[] = [];

  constructor(private http: HttpClient) {}

  private getTrans() {
    return this.http.get<any[]>(environment.apiTrans);
  }

  public getTpm(id: number) {
    return this.http.get<any>(environment.apiRoot + "tpm1/"+ id.toString() + "/");
  }

  public needHelp(data: any, helpNeeded: boolean) {
    data.help = helpNeeded;
    console.log("pushed help")
    console.log(data);
    return this.http.post(environment.apiRoot.concat('tpmhelp/'), data);
  }

  public initTrans() {
    this.getTrans().subscribe({
      next: (data) => {
        this._transData = data;
      },
      error: (er) => {},
    });
  }

  get trans(): any[] {
    return this._transData;
  }
}
