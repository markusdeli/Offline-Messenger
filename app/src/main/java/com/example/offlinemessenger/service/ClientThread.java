package com.example.offlinemessenger.service;

import android.bluetooth.BluetoothDevice;
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

public class ClientThread extends Thread {

    private static final String TAG = "ClientThread";

    private Handler mInHandler;
    private Handler mOutHandler;

    private BluetoothSocket mBtSocket;
    private ObjectOutputStream mOut;
    private ObjectInputStream mIn;

    private final List<Sendable> mQueue;
    private final Object queueLock = new Object();

    public ClientThread(BluetoothDevice device, Handler handler) {
        Log.d(TAG, "ClientThread started");
        mOutHandler = handler;
        ObjectOutputStream tmpO = null;
        ObjectInputStream tmpI = null;
        mQueue = new LinkedList<>();
        try {
            mBtSocket = device.createRfcommSocketToServiceRecord(UUID.randomUUID());
            Log.d(TAG, "Connected Thread: trying to get output and input streams of the socket");
            tmpO = new ObjectOutputStream(mBtSocket.getOutputStream());
            tmpI = new ObjectInputStream(mBtSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mOut = tmpO;
        mIn = tmpI;
    }

    public void run() {
        mInHandler = new Handler(this::onMessageReceived);
        Message msg = new Message();
        msg.obj = mInHandler;
        mOutHandler.dispatchMessage(msg);

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

    private void addToQueue(Sendable s) {
        synchronized (queueLock) {
            mQueue.add(s);
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
