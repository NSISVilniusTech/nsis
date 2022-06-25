package com.example.nsis;

import static java.lang.Thread.sleep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.BuildConfig;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, WifiP2pManager.ConnectionInfoListener, SerialInputOutputManager.Listener {

    //Leidimu suteikimui
    private static final int PERMISSION_REQUEST_CODE = 1;

    //GUI elementai
    Button buttonDiscoveryStart;
    Button buttonDiscoveryStop;
    Button buttonConnect;
    Button buttonDisconnect;
    Button buttonServerStart;
    Button buttonClientStart;
    Button buttonClientStop;
    Button buttonServerStop;
    Button buttonConfigure;
    EditText editTextTextInput;
    ListView listViewDevices;
    TextView textViewDiscoveryStatus;
    TextView textViewWifiP2PStatus;
    TextView textViewConnectionStatus;
    TextView textViewReceivedData;
    TextView textViewReceivedDataStatus;
    IntentFilter mIntentFilter;

    TextView textDurys;
    TextView textPagalba;
    static String durys;

    //WiFi P2p klasės
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    WiFiDirectBroadcastReceiver mReceiver;

    //Pranešimų valdymas
    static boolean stateDiscovery = false;
    static boolean stateWifi = false;
    public static boolean stateConnection = false;
    public static final String TAG = "===MainActivity";

    // Įrenginiu paieska ir sąrašo sudarymas
    WifiP2pDevice[] deviceListItems;
    ArrayAdapter mAdapter;
    int j;

    //irenginys prie kurio jungiames
    WifiP2pDeviceList wifiP2pDeviceList;
    WifiP2pDevice device;
    public static String IP = null;
    public static boolean IS_OWNER = false;

    ServerSocketThread serverSocketThread;

    // USB-UART
    private final ArrayList<ListItem> listItems = new ArrayList<>();
    private int baudRate = 9600;
    private boolean withIoManager = true;

    static class ListItem {
        UsbDevice device;
        int port;
        UsbSerialDriver driver;

        ListItem(UsbDevice device, int port, UsbSerialDriver driver) {
            this.device = device;
            this.port = port;
            this.driver = driver;
        }
    }

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.LIBRARY_PACKAGE_NAME + ".GRANT_USB";
    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private int deviceId, portNum;
    private UsbSerialPort usbSerialPort;
    private BroadcastReceiver broadcastReceiver;
    private Handler mainLooper;
    private boolean connected = false;
    private SerialInputOutputManager usbIoManager;
    boolean Collect = false;
    String tx_buf = "";
    TextView texLatitude, texLongitude;
    static Double Latitude=0.0;
    static Double Longitude=0.0;
    // USB UART

    //SVS
    String postUrl = "http://5.20.228.20:8881/healthcheck/";
    protected RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestQueue = Volley.newRequestQueue(this);
        if(checkSelfPermission()){
        }
        else{
            requestPermission();
        }

        setUpUI();

        //pin 8
        activation("8");
        setDirection("8", "out");
        setValue("8", "0");

        // pin 7
        activation("81");
        setDirection("81", "out");
        setValue("81", "0");

        //pin 12
        activation("120");
        setDirection("120", "in");

        //GPS
        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
        UsbSerialProber usbDefaultProber = UsbSerialProber.getDefaultProber();
        UsbSerialProber usbCustomProber = CustomProber.getCustomProber();

        for(UsbDevice device : usbManager.getDeviceList().values()) {
            UsbSerialDriver driver = usbDefaultProber.probeDevice(device);
            if(driver == null) {
                driver = usbCustomProber.probeDevice(device);
            }
            if(driver != null) {
                //for(int port = 0; port < driver.getPorts().size(); port++) {
                //listItems.add(new ListItem(device, port, driver));
                listItems.add(new ListItem(device, 0, driver));
                //}
            } else {
                listItems.add(new ListItem(device, 0, null));
            }
        }

        ListItem item = listItems.get(0);

        if(item.driver == null) {
            Toast.makeText(MainActivity.this, "no driver", Toast.LENGTH_SHORT).show();
        } else {
            Bundle args = new Bundle();
            args.putInt("device", item.device.getDeviceId());
            args.putInt("port", item.port);
            args.putInt("baud", baudRate);
            args.putBoolean("withIoManager", withIoManager);

            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                        usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                                ? UsbPermission.Granted : UsbPermission.Denied;
                        connect();
                    }
                }
            };
            mainLooper = new Handler(Looper.getMainLooper());
        }

        deviceId = item.device.getDeviceId();
        portNum = item.port;
        baudRate = 9600;
        withIoManager = true;
        //GPS

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        stateDiscovery = true;
        discoverPeers();

        // Prisijungti galima 3 budais: 1 - pazymėjus 2 - pazymėjus ir nuspaudus connect mygtuką
        // ijungiame isrinnkta irengini
        listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                device = deviceListItems[i];
                Toast.makeText(MainActivity.this,"Selected device :"+ device.deviceName ,Toast.LENGTH_SHORT).show();
                // 1 budas nereikalaujantis connect mygtuko. su mygtuku reikėtu šią eilutę užkomentuoti
                connect(device);
            }
        });

        //serverSocketThread = new ServerSocketThread();

        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                durys = getValue("120");
//                                textDurys.setText(durys);
                                if(getValue("120").equals("1")){
                                    textDurys.setText("Durys atidarytos");
                                }
                                else {
                                    textDurys.setText("Durys uždarytos");
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        thread.start();

        Thread thread1 = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(3000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                JSONObject postData = new JSONObject();
                                try {
                                    postData.put("phy_address", "22:4e:f6:c2:02:11");
                                    postData.put("lat", Double.toString(MainActivity.Latitude));
                                    postData.put("lng", Double.toString(MainActivity.Longitude));
                                    postData.put("door", false);
                                    if(getValue("120").equals("1")){
                                        postData.put("door", true);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl, postData, new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            String help = response.getString("help");
                                            if(help == "false"){
                                                textPagalba.setText("");
                                            }
                                            if(help == "true"){
                                                textPagalba.setText("Reikalinga pagalba");
                                            }
//                                            Toast.makeText(getApplicationContext(), "Response: "+ help, Toast.LENGTH_LONG).show();
                                        }
                                        catch (JSONException e){
                                            e.printStackTrace();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        error.printStackTrace();
                                    }
                                });

                                requestQueue.add(jsonObjectRequest);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        thread1.start();
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
        if(usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted)
            mainLooper.post(this::connect);
        registerReceiver(mReceiver, mIntentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        if(connected) {
            Log.i("Info", "disconnected");
        }
        unregisterReceiver(broadcastReceiver);
    }

    private void setUpUI() {
        buttonDiscoveryStart = findViewById(R.id.main_activity_button_discover_start);
        buttonDiscoveryStop = findViewById(R.id.main_activity_button_discover_stop);
        buttonConnect = findViewById(R.id.main_activity_button_connect);
        buttonDisconnect = findViewById(R.id.main_activity_button_disconnect);
        buttonServerStart = findViewById(R.id.main_activity_button_server_start);
        buttonServerStop = findViewById(R.id.main_activity_button_server_stop);
        buttonClientStart = findViewById(R.id.main_activity_button_client_start);
        buttonClientStop = findViewById(R.id.main_activity_button_client_stop);
        buttonConfigure = findViewById(R.id.main_activity_button_configure);
        listViewDevices = findViewById(R.id.main_activity_list_view_devices);
        textViewConnectionStatus = findViewById(R.id.main_activiy_textView_connection_status);
        textViewDiscoveryStatus = findViewById(R.id.main_activiy_textView_dicovery_status);
        textViewWifiP2PStatus = findViewById(R.id.main_activiy_textView_wifi_p2p_status);
        textViewReceivedData = findViewById(R.id.main_acitivity_data);
        textViewReceivedDataStatus = findViewById(R.id.main_acitivity_received_data);

        editTextTextInput = findViewById(R.id.main_acitivity_input_text);

        texLatitude = (TextView) findViewById(R.id.text5);
        texLongitude = (TextView) findViewById(R.id.text6);

        textDurys = (TextView) findViewById(R.id.text7);
        textPagalba = (TextView) findViewById(R.id.text3);

        //irenginiu paieskai
        buttonDiscoveryStop.setOnClickListener(this);
        buttonDiscoveryStart.setOnClickListener(this);
        //irenginio ijungimas/isjungimas
        buttonConnect.setOnClickListener(this);
        buttonDisconnect.setOnClickListener(this);
        //Client
        buttonConfigure.setOnClickListener(this);
        buttonClientStart.setOnClickListener(this);
        buttonClientStop.setOnClickListener(this);
        buttonServerStart.setOnClickListener(this);
        buttonServerStop.setOnClickListener(this);

        buttonClientStop.setVisibility(View.INVISIBLE);
        buttonClientStart.setVisibility(View.INVISIBLE);
        buttonServerStop.setVisibility(View.INVISIBLE);
        buttonServerStart.setVisibility(View.INVISIBLE);
        editTextTextInput.setVisibility(View.INVISIBLE);
        textViewReceivedDataStatus.setVisibility(View.INVISIBLE);
        textViewReceivedData.setVisibility(View.INVISIBLE);

        buttonConfigure.setVisibility(View.INVISIBLE);

    }

    public void setStatusView(int status) {

        switch (status)
        {
            case Constants.DISCOVERY_INITATITED:
                stateDiscovery = true;
                textViewDiscoveryStatus.setText("DISCOVERY_INITIATED");
                break;
            case Constants.DISCOVERY_STOPPED:
                stateDiscovery = false;
                textViewDiscoveryStatus.setText("DISCOVERY_STOPPED");
                break;
            case Constants.P2P_WIFI_DISABLED:
                stateWifi = false;
                textViewWifiP2PStatus.setText("P2P_WIFI_DISABLED");
                buttonDiscoveryStart.setEnabled(false);
                buttonDiscoveryStop.setEnabled(false);
                break;
            case Constants.P2P_WIFI_ENABLED:
                stateWifi = true;
                textViewWifiP2PStatus.setText("P2P_WIFI_ENABLED");
                buttonDiscoveryStart.setEnabled(true);
                buttonDiscoveryStop.setEnabled(true);
                break;
            case Constants.NETWORK_CONNECT:
                stateConnection = true;
                makeToast("It's a connect");
                textViewConnectionStatus.setText("Connected");

                //Ijungiame LED
                setValue("8", "1");
                setValue("81", "1");

                //Aktyvuojame serverį be mygtuko
                mManager.requestConnectionInfo(mChannel,MainActivity.this);
                serverSocketThread = new ServerSocketThread();
                serverSocketThread. setUpdateListener(new ServerSocketThread.OnUpdateListener() {
                    public void onUpdate(String obj) {
                        setReceivedText(obj);
                    }
                });
                serverSocketThread.execute();

                //ijungiama sustiprinta paieska po atsijungimo nuo įrenginio
                stateDiscovery = true;
                discoverPeers();

                break;
            case Constants.NETWORK_DISCONNECT:
                stateConnection = false;
                textViewConnectionStatus.setText("Disconnected");
                makeToast("State is disconnected");

                //Išjungiame LED
                setValue("8", "0");
                setValue("81", "0");

                break;

            default:
                Log.d(MainActivity.TAG,"Unknown status");
                break;
        }
    }

    public void makeToast(String msg) {
        Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show();
    }

    //Mygtuku paspaudimai
    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.main_activity_button_discover_start:
                if(!stateDiscovery) {
                    discoverPeers();
                }
                break;
            case R.id.main_activity_button_discover_stop:
                if(stateDiscovery){
                    stopPeerDiscover();
                }
                break;
            //Bandome pasijungti prie pasirinkto irenginiu sy connect mygtuku
            case R.id.main_activity_button_connect:
                Toast.makeText(MainActivity.this,"Connecting",Toast.LENGTH_SHORT).show();
                if(device == null) {
                    Toast.makeText(MainActivity.this,"Please discover and select a device",Toast.LENGTH_SHORT).show();
                    return;
                }
                connect(device);
                break;
            case R.id.main_activity_button_disconnect:
                Toast.makeText(MainActivity.this,"Disconnecting",Toast.LENGTH_SHORT).show();
                disconnect();
                break;
            case R.id.main_activity_button_server_start:
                serverSocketThread = new ServerSocketThread();
                serverSocketThread. setUpdateListener(new ServerSocketThread.OnUpdateListener() {
                    public void onUpdate(String obj) {
                        setReceivedText(obj);
                    }
                });
                serverSocketThread.execute();
                break;
            case R.id.main_activity_button_server_stop:
                if(serverSocketThread != null) {
                    serverSocketThread.setInterrupted(true);
                } else {
                    Log.d(MainActivity.TAG,"serverSocketThread is null");
                }
                //makeToast("Yet to do...");
                break;
            case R.id.main_activity_button_client_start:
                //serviceDisvcoery.startRegistrationAndDiscovery(mManager,mChannel);
                String dataToSend = "Labas";
                ClientSocket clientSocket = new ClientSocket(MainActivity.this,this,dataToSend);
                clientSocket.execute();
                break;
            case R.id.main_activity_button_configure:
                mManager.requestConnectionInfo(mChannel,this);
                break;
            case R.id.main_activity_button_client_stop:
                makeToast("Yet to do");
                break;
            default:
                break;
        }
    }

    public void setReceivedText(final String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewReceivedData.setText(data);
            }
        });
    }

    //įrenginių paieška arba apieškos sustabdymas

    @SuppressLint("MissingPermission")
    public void discoverPeers()
    {
        Log.d(MainActivity.TAG,"discoverPeers()");
        setDeviceList(new ArrayList<WifiP2pDevice>());
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                stateDiscovery = true;
                Log.d(MainActivity.TAG,"peer discovery started");
                makeToast("peer discovery started");
                MyPeerListener myPeerListener = new MyPeerListener(MainActivity.this);
                mManager.requestPeers(mChannel,myPeerListener);
            }

            @Override
            public void onFailure(int i) {
                stateDiscovery = false;
                if (i == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(MainActivity.TAG," peer discovery failed :" + "P2P_UNSUPPORTED");
                    makeToast(" peer discovery failed :" + "P2P_UNSUPPORTED");

                } else if (i == WifiP2pManager.ERROR) {
                    Log.d(MainActivity.TAG," peer discovery failed :" + "ERROR");
                    makeToast(" peer discovery failed :" + "ERROR");

                } else if (i == WifiP2pManager.BUSY) {
                    Log.d(MainActivity.TAG," peer discovery failed :" + "BUSY");
                    makeToast(" peer discovery failed :" + "BUSY");
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    public class DiscoveryUpdater implements Runnable {
        @Override
        public void run() {
            while(true) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        stateDiscovery = true;
                        Log.d(MainActivity.TAG,"peer discovery started");
                        makeToast("peer discovery started");
                        MyPeerListener myPeerListener = new MyPeerListener(MainActivity.this);
                        mManager.requestPeers(mChannel,myPeerListener);
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        stateDiscovery = false;
                        if (reasonCode == WifiP2pManager.P2P_UNSUPPORTED) {
                            Log.d(MainActivity.TAG," peer discovery failed :" + "P2P_UNSUPPORTED");
                            makeToast(" peer discovery failed :" + "P2P_UNSUPPORTED");

                        } else if (reasonCode == WifiP2pManager.ERROR) {
                            Log.d(MainActivity.TAG," peer discovery failed :" + "ERROR");
                            makeToast(" peer discovery failed :" + "ERROR");

                        } else if (reasonCode == WifiP2pManager.BUSY) {
                            Log.d(MainActivity.TAG," peer discovery failed :" + "BUSY");
                            makeToast(" ANDROID OS :" + "BUSY");
                        }
                    }
                });
            }
        }
    }

    private void stopPeerDiscover() {
        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                stateDiscovery = false;
                Log.d(MainActivity.TAG,"Peer Discovery stopped");
                makeToast("Peer Discovery stopped" );
                buttonDiscoveryStop.setEnabled(true);
            }

            @Override
            public void onFailure(int i) {
                Log.d(MainActivity.TAG,"Stopping Peer Discovery failed");
                makeToast("Stopping Peer Discovery failed" );
                buttonDiscoveryStop.setEnabled(false);
            }
        });
    }

    public void setDeviceList(ArrayList<WifiP2pDevice> deviceDetails) {

        deviceListItems = new WifiP2pDevice[deviceDetails.size()];
        String[] deviceNames = new String[deviceDetails.size()];
        for(int i=0 ;i< deviceDetails.size(); i++){
            deviceNames[i] = deviceDetails.get(i).deviceName;
            deviceListItems[i] = deviceDetails.get(i);
        }
        mAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,android.R.id.text1,deviceNames);
        listViewDevices.setAdapter(mAdapter);
/*
        //jeigu aptinkame žinoma irengini iskarto bandome prie jo jungtis ir dar neesame susijunge
        if (stateConnection == false) {
            for (int i = 0; i < deviceDetails.size(); i++) {
                deviceNames[i] = deviceDetails.get(i).deviceName;
                if (deviceNames[i].equals("ASUS_I006D_64e7")) {
                    device = deviceListItems[i];
                    connect(device);
                }
            }
        }

 */
    }

    //įrenginiu paieškos funkciju pabaiga


    //Pridijungimo prie irenginio funkcija
    @SuppressLint("MissingPermission")
    public void connect (final WifiP2pDevice device) {
        // Picking the first device found on the network.

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 15;


        Log.d(MainActivity.TAG,"Trying to connect : " +device.deviceName);
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(MainActivity.TAG, "Connected to :" + device.deviceName);
                Toast.makeText(getApplication(),"Connection successful with " + device.deviceName,Toast.LENGTH_SHORT).show();
                setDeviceList(new ArrayList<WifiP2pDevice>());
            }

            @Override
            public void onFailure(int reason) {
                if(reason == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.d(MainActivity.TAG, "P2P_UNSUPPORTED");
                    makeToast("Failed establishing connection: " + "P2P_UNSUPPORTED");
                }
                else if( reason == WifiP2pManager.ERROR) {
                    Log.d(MainActivity.TAG, "Conneciton falied : ERROR");
                    makeToast("Failed establishing connection: " + "ERROR");

                }
                else if( reason == WifiP2pManager.BUSY) {
                    Log.d(MainActivity.TAG, "Conneciton falied : BUSY");
                    makeToast("Failed establishing connection: " + "BUSY");

                }
            }
        });
    }

    /*
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

        }
    };
    */
    //itraukta klasė į MainActivity implements todėl taip galima
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        String hostAddress= wifiP2pInfo.groupOwnerAddress.getHostAddress();
        if (hostAddress == null) hostAddress= "host is null";

        //makeToast("Am I group owner : " + String.valueOf(wifiP2pInfo.isGroupOwner));
        //makeToast(hostAddress);
        Log.d(MainActivity.TAG,"wifiP2pInfo.groupOwnerAddress.getHostAddress() " + wifiP2pInfo.groupOwnerAddress.getHostAddress());
        IP = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        IS_OWNER = wifiP2pInfo.isGroupOwner;

        if(IS_OWNER) {
            makeToast("Owner");
            buttonClientStop.setVisibility(View.GONE);
            buttonClientStart.setVisibility(View.GONE);
            editTextTextInput.setVisibility(View.GONE);
            //buttonServerStop.setVisibility(View.VISIBLE);
            //buttonServerStart.setVisibility(View.VISIBLE);
            //textViewReceivedData.setVisibility(View.VISIBLE);
            //textViewReceivedDataStatus.setVisibility(View.VISIBLE);


        } else {
            makeToast("Slave");
            buttonClientStop.setVisibility(View.VISIBLE);
            buttonClientStart.setVisibility(View.VISIBLE);
            editTextTextInput.setVisibility(View.VISIBLE);
            buttonServerStop.setVisibility(View.GONE);
            buttonServerStart.setVisibility(View.GONE);
            textViewReceivedData.setVisibility(View.GONE);
            textViewReceivedDataStatus.setVisibility(View.GONE);
      }

        makeToast("Configuration Completed");
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {

        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null) {
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });

        }
    }

    //prisijungimo pabaiga


    /*
     * GPIO
     */

    void activation(String port){
        String command = String.format("echo %s > /sys/class/gpio/export", port);
        @SuppressLint("WrongThread") CommandResult result = Shell.SH.run(command);
        if (result.isSuccessful()) {
            //Toast.makeText(getApplicationContext(),result.getStdout(),Toast.LENGTH_SHORT).show();
        }
    }
    void deactivation(String port){
        String command = String.format("echo %s > /sys/class/gpio/unexport", port);
        @SuppressLint("WrongThread") CommandResult result = Shell.SH.run(command);
        if (result.isSuccessful()) {
            //Toast.makeText(getApplicationContext(),result.getStdout(),Toast.LENGTH_SHORT).show();
        }
    }

    void setDirection(String port, String direction){
        String command = null;
        if (direction == "out")
            command = String.format("echo out > /sys/class/gpio/gpio%s/direction", port);
        if (direction == "in")
            command = String.format("echo in > /sys/class/gpio/gpio%s/direction", port);
        @SuppressLint("WrongThread") CommandResult result = Shell.SH.run(command);
        if (result.isSuccessful()) {
            //Toast.makeText(getApplicationContext(),result.getStdout(),Toast.LENGTH_SHORT).show();
        }
    }

    void setValue(String port, String value){
        String command = String.format("echo %s > /sys/class/gpio/gpio%s/value", value, port);
        @SuppressLint("WrongThread") CommandResult result = Shell.SH.run(command);
        if (result.isSuccessful()) {
            //Toast.makeText(getApplicationContext(),result.getStdout(),Toast.LENGTH_SHORT).show();
        }
    }


    String getValue(String port){
        String command = String.format("cat < /sys/class/gpio/gpio%s/value", port);
        @SuppressLint("WrongThread") CommandResult result = Shell.SH.run(command);
        if (result.isSuccessful()) {
            //Toast.makeText(getApplicationContext(),result.getStdout(),Toast.LENGTH_SHORT).show();
        }
        return result.getStdout();
    }

    String getDirection(String port){
        String command = String.format("cat < /sys/class/gpio/gpio%s/direction", port);
        @SuppressLint("WrongThread") CommandResult result = Shell.SH.run(command);
        if (result.isSuccessful()) {
            //Toast.makeText(getApplicationContext(),result.getStdout(),Toast.LENGTH_SHORT).show();
        }
        return result.getStdout();
    }

    /*
     * Serial
     */
    @Override
    public void onNewData(byte[] data) {

        mainLooper.post(() -> {
            String str = "";
            try {
                str = new String(data, "ASCII");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String[] newDesc=str.split("");

            for (int i=0; i < newDesc.length; i++) {
                Log.i("Info", newDesc[i]);  //i6trinus ka=kod4l neveikia visi6ka mistika
                if (newDesc[i].equals("$") || Collect == true) {
                    Collect = true;
                    tx_buf += newDesc[i];
                    if (newDesc[i].equals("\r")) {
                        Collect = false;
                        //Log.i("Value", String.valueOf(tx_buf));
                        String[] row = tx_buf.split(",");
                        tx_buf = "";
                        Latitude = parseCoordinate(row[2], row[3]);
                        Longitude = parseCoordinate(row[4], row[5]);
                        texLatitude.setText(Double.toString(Latitude));
                        texLongitude.setText(Double.toString(Longitude));
                    }
                }

            }
        });
    }

    @Override
    public void onRunError(Exception e) {
        mainLooper.post(() -> {
            Log.i("Info", "connection lost: " + e.getMessage());
        });
    }

    /*
     * Serial + UI
     */
    private void connect() {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId)
                device = v;
        if(device == null) {
            Log.i("Info", "connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            Log.i("Info", "connection failed: no driver for device");
            return;
        }
        if(driver.getPorts().size() < portNum) {
            Log.i("Info", "connection failed: not enough ports at device");
            return;
        }

        usbSerialPort = driver.getPorts().get(portNum);

        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent(INTENT_ACTION_GRANT_USB), PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                Log.i("Info", "connection failed: permission denied");
            else
                Log.i("Info", "connection failed: open failed");

            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            if(withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            Log.i("Info", "connected");
            connected = true;
        } catch (Exception e) {
            Log.i("Info", "connection failed: " + e.getMessage());
        }
    }

    /**
     * Convert NMEA absolute position to decimal degrees
     * "ddmm.mmmm" or "dddmm.mmmm" really is D+M/60,
     * then negated if quadrant is 'W' or 'S'
     */
    /**
     * Combine an NMEA coordinate tuple into a decimal value in degrees.
     * @param degreeString Coordinate in degrees, minutes, seconds.
     * @param hemisphere Hemisphere designation (N,S,E,W)
     * @return Decimal value of the coordinate.
     */
    private double parseCoordinate(String degreeString, String hemisphere) {
        if (degreeString.isEmpty() || hemisphere.isEmpty()) {
            // No data
            return -1;
        }

        // Two digits left of decimal to the end are the minutes
        int index = degreeString.indexOf('.') - 2;
        if (index < 0) {
            // Invalid string
            return -1;
        }

        // Parse full degrees
        try {
            double value = Double.parseDouble(degreeString.substring(0, index));
            // Append the minutes
            value += Double.parseDouble(degreeString.substring(index)) / 60.0;

            // Compensate for the hemisphere
            if (hemisphere.contains("W") || hemisphere.contains("S")) {
                value *= -1;
            }

            return value;
        } catch (NumberFormatException e) {
            // Invalid value
            return -1;
        }
    }

    /*
     * End of GPIO
     */

    //------------ CHECK PERMISSIONS ------------------

    private void requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(MainActivity.this, "Please Give Permission to Access Location", Toast.LENGTH_SHORT).show();
        }
        else{
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_REQUEST_CODE);
        }
    }

    private boolean checkSelfPermission(){
        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(result == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_REQUEST_CODE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this, "Permission Successfull", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this, "Permission Failed", Toast.LENGTH_SHORT).show();
                }
        }
    }
    //____________________________________________________________________________________

}