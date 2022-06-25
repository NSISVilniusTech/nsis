package com.example.nsis;

import static java.lang.Thread.sleep;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, MainActivity mActivity) {
        super();
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.mActivity = mActivity;

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                mActivity.setStatusView(Constants.P2P_WIFI_ENABLED);
            } else {
                mActivity.setStatusView(Constants.P2P_WIFI_DISABLED);
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (mManager != null) {
                MyPeerListener myPeerListener = new MyPeerListener(mActivity);
                mManager.requestPeers(mChannel, myPeerListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                mActivity.setStatusView(Constants.NETWORK_CONNECT);
            } else {
                // It's a disconnect
                mActivity.setStatusView(Constants.NETWORK_DISCONNECT);

                //ijungiama sustiprinta paieska po atsijungimo nuo įrenginio
                mActivity.stateDiscovery = true;
                mActivity.discoverPeers();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

        }  else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 10000);
            if( state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED ) {
                mActivity.setStatusView(Constants.DISCOVERY_INITATITED);
            } else if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                mActivity.setStatusView(Constants.DISCOVERY_STOPPED);

                //ijungiama sustiprinta paieska po atsijungimo nuo įrenginio
                mActivity.stateDiscovery = true;
                mActivity.discoverPeers();
            }
        }

    }
}
