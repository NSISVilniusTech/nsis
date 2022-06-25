import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SettingsService {
  private _isTTSEnabled: boolean = false; 
  private _isAudioEnabled: boolean = true;
  private _isAutoStopEnabled: boolean = true;

  constructor() { }

  get isTTSEnabled(){
    return this._isTTSEnabled;
  }

  set isTTSEnabled(val: boolean){
    this._isTTSEnabled = val;
  }

  get isAudioEnabled(){
    return this._isAudioEnabled;
  }

  set isAudioEnabled(val: boolean){
    this._isAudioEnabled = val;
  }

  get isAutoStopEnabled(){
    return this._isAutoStopEnabled;
  }

  set isAutoStopEnabled(val: boolean){
    this._isAutoStopEnabled = val;
  }

}
