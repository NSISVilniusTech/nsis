package com.example.nsis;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketThread extends AsyncTask {

    OnUpdateListener listener;
    private boolean interrupted = false;
    private static final String TAG = "===ServerSocketThread";
    String receivedData = "null";

    ServerSocket serverSocket;

    public ServerSocketThread() {
    }

    public interface OnUpdateListener {
        public void onUpdate(String data);
    }

    public void setUpdateListener(OnUpdateListener listener) {
        this.listener = listener;
    }


    @Override
    protected Void doInBackground(Object[] objects) {
        int len;
        try {
            Log.d(ServerSocketThread.TAG," started DoInBackground");
            serverSocket = new ServerSocket(8888);

            while (!interrupted) {
                Socket client = serverSocket.accept();
                Log.d(ServerSocketThread.TAG,"Accepted Connection");

                OutputStream outputStream = client.getOutputStream();
                String data = Double.toString(MainActivity.Latitude) + "," + Double.toString(MainActivity.Longitude) + "," + MainActivity.durys +"\r\n";
                byte[] theByteArray = data.getBytes();
                outputStream.write(theByteArray);
                outputStream.flush();
                outputStream.close();

            }
            serverSocket.close();

            return null;

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(ServerSocketThread.TAG,"IOException occurred");
        }
        return null;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
