package com.example.offlinemessenger.service;

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

public class ServerThread extends Thread {

    private final BluetoothAdapter mBtAdapter;
    private final BluetoothServerSocket mBtServerSocket;

    private static final String TAG = "asdf";
    private final String APP_NAME = "MY_APP";
    private final UUID UUID_INSECURE = UUID.randomUUID();

    private BluetoothSocket mBtSocket;
    private ObjectOutputStream mOut;
    private ObjectInputStream mIn;

    private final List<Sendable> mQueue;
    private final Object queueLock = new Object();

    private Handler mInHandler;
    private final Handler mOutHandler;

    public ServerThread(BluetoothAdapter adapter, Handler handler) {
        mBtAdapter = adapter;
        mOutHandler = handler;
        mQueue = new LinkedList<>();
        BluetoothServerSocket tmp = null;
        try {
            tmp = mBtAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, UUID_INSECURE);
            Log.d(TAG, "ServerListenThread setting up Server using: " + UUID_INSECURE);
        } catch (IOException e) {
            Log.e(TAG, "Unable to accept client connection", e);
        }
        mBtServerSocket = tmp;
    }

    public void run() {
        Log.d(TAG, "run: ServerListenThread running!");
        mInHandler = new Handler(this::onMessageReceived);
        Message msg = new Message();
        msg.obj = mInHandler;
        mOutHandler.dispatchMessage(msg);

        BluetoothSocket socket = null;
        try {
            socket = mBtServerSocket.accept();

            Log.d(TAG, "run: server socket accepted connection");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (socket == null) {
            Log.e(TAG, "Could not open a bluetooth socket");
            return;
        }
        Log.d(TAG, "End ServerListenThread");

        while (true) {
            try {
                Object o = mIn.readObject();
                if (o instanceof ChatMessage) {
                    Message m = new Message();
                    m.obj = o;
                    mOutHandler.dispatchMessage(m);
                }
                Log.d(TAG, "Inputstream = " + o.toString());
            } catch (IOException e) {
                break;
            } catch (ClassNotFoundException e) {
                Log.wtf(TAG, e);
                break;
            }

            synchronized (queueLock) {
                while (!mQueue.isEmpty()) {
                    write(mQueue.remove(0));
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return;
            }
        }

        try {
            mBtSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close socket", e);
        }
    }

    public static class ListenResult {

        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        private ListenResult(BluetoothSocket socket, BluetoothDevice device) {
            mSocket = socket;
            mDevice = device;
        }

        public BluetoothSocket getSocket() {
            return mSocket;
        }

        public BluetoothDevice getDevice() {
            return mDevice;
        }

    }

    private boolean onMessageReceived(Message msg) {
        if (msg.obj instanceof Sendable) {
            return write((Sendable) msg.obj);
        }

        return false;
    }

    private boolean write(Sendable data) {
        Log.d(TAG, "Write: writing to outputstream :" + data.toString());
        try {
            mOut.writeObject(data);
            return true; // if writing was successful
        } catch (IOException e) {
            Log.e(TAG, "Write: Error writing to outputstream :" + data.toString());
            return false;
        }
    }

}
