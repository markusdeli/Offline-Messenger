package com.example.offlinemessenger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.offlinemessenger.data.ChatMessage;
import com.example.offlinemessenger.data.Sendable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class BluetoothConnectionService {
    //Tag used for logging
    private static final String TAG = "BluetoothConnectService";
    private final String APP_NAME = "MY_APP";

    //UUID to confirm both devices use the same service in this case the app OfflineMessenger
    private static final UUID UUID_INSECURE =
            UUID.fromString("c199f1da-5634-44bf-ad17-394d1c186a24");

    private final BluetoothAdapter mBluetoothAdapter;

    private ServerListenThread mInsecureServerListenThread;
    private ClientConnectThread mConnectThread;
    private IOThread mIOThread;

    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    private final Handler mUIHandler;

    public BluetoothConnectionService(Handler handler) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mUIHandler = handler;
        start();
    }

    // Listening for connections and accepting incoming calls
    private class ServerListenThread extends Thread {
        private final BluetoothServerSocket mBtServerSocket;
        public ServerListenThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, UUID_INSECURE);
                Log.d(TAG, "ServerListenThread setting up Server using: " + UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "Unable to accept client connection", e);
            }
            mBtServerSocket = tmp;
        }
        public void run(){
            Log.d(TAG, "run: ServerListenThread running!");
            BluetoothSocket mBtSocket = null;
            try {
                mBtSocket = mBtServerSocket.accept();

                Log.d(TAG, "run: server socket accepted connection");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(mBtSocket != null) {
                connected(mBtSocket, mDevice);
            }
            Log.d(TAG, "End ServerListenThread");
        }

        public void cancel(){
            Log.d(TAG, "cancel: cancelling ServerListenThread");
            try {
                mBtServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancelling of ServerListenThread failed!");
            }
        }
    }
    
    private class ClientConnectThread extends Thread {
        private BluetoothSocket mBluetoothSocket;

        private ClientConnectThread(BluetoothDevice device, UUID uuid) {
            mDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            Log.i(TAG, "run: mConnectThread");
            tryBtConnection();
            connected(mBluetoothSocket, mDevice);
        }

        public void cancel(){
            try {
                Log.d(TAG, "cancel: Closing client socket");
                mBluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancelling of client socket failed!" + e.getMessage());
            }
        }

        private void tryBtConnection() {
            BluetoothSocket tmp;
            try {
                Log.d(TAG, "ClientConnectThread: Trying to create socket to service record using UUID: " + deviceUUID);
                tmp = mDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "Failed to create socket to service record!" + e.getMessage());
                return;
            }
            mBluetoothSocket = tmp;
            mBluetoothAdapter.cancelDiscovery();
            try {
                mBluetoothSocket.connect();
                Log.d(TAG, "ClientConnectThread: Socket is connected!");
            } catch (IOException e) {
                try {
                    mBluetoothSocket.close();
                    Log.d(TAG, "Socket closed!");
                } catch (Exception e1) {
                    Log.e(TAG, "Unable to close connection!" + e1.getMessage());
                }
                Log.e(TAG, "ClientConnectThread: Failed connecting to UUID: " + deviceUUID + "! " + e.getMessage());
            }
        }
    }

    public synchronized void start(){
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if(mInsecureServerListenThread == null){
            mInsecureServerListenThread = new ServerListenThread();
            mInsecureServerListenThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient started!");
        mConnectThread = new ClientConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class IOThread extends Thread {
        private final BluetoothSocket mBluetoothSocket;
        private final ObjectOutputStream outputStream;
        private final ObjectInputStream inputStream;

        private final List<Sendable> queue;
        private final Object queueLock = new Object();

        public IOThread(BluetoothSocket bluetoothSocket){
            Log.d(TAG, "ClientThread started");
            mBluetoothSocket = bluetoothSocket;
            ObjectOutputStream tmpO = null;
            ObjectInputStream tmpI = null;
            queue = new LinkedList<>();
            try {
                Log.d(TAG, "Connected Thread: trying to get output and input streams of the socket");
                tmpO = new ObjectOutputStream(mBluetoothSocket.getOutputStream());
                tmpI = new ObjectInputStream(mBluetoothSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = tmpO;
            inputStream = tmpI;
        }

        public void run(){
            while (true){
                try {
                    Object o = inputStream.readObject();
                    if (o instanceof ChatMessage) {
                        Message m = new Message();
                        m.obj = o;
                        mUIHandler.dispatchMessage(m);
                    }
                    Log.d(TAG, "Inputstream = " + o.toString());
                } catch (IOException e) {
                    break;
                } catch (ClassNotFoundException e) {
                    Log.wtf(TAG, e);
                }
                synchronized (queueLock) {
                    while (!queue.isEmpty()) {
                        write(queue.remove(0));
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        private void addToQueue(Sendable s) {
            synchronized (queueLock) {
                queue.add(s);
            }
        }

        public int write(Sendable data){
            Log.d(TAG, "Write: writing to outputstream :" + data.toString());
            try {
                outputStream.writeObject(data);
                return 1; // if writing was successful
            } catch (IOException e) {
                Log.e(TAG, "Write: Error writing to outputstream :" + data.toString());
                return 0;
            }
        }

        public void cancel(){
            try {
                Log.d(TAG, "cancel: Closing client socket");
                mBluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancelling of client socket failed!" + e.getMessage());
            }
        }
    }

    private void connected(BluetoothSocket socket, BluetoothDevice device){
        Log.d(TAG, "connected: Starting!");

        mIOThread = new IOThread(socket);
        mIOThread.start();
    }

    public void write(Sendable data){
        Log.d(TAG, "write: Write called");
        mIOThread.addToQueue(data);
    }
}
