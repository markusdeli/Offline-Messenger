package com.example.offlinemessenger.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

import com.example.offlinemessenger.IBluetoothService;
import com.example.offlinemessenger.data.ChatMessage;
import com.example.offlinemessenger.data.ChatUser;
import com.example.offlinemessenger.data.Sendable;

import java.util.LinkedList;
import java.util.List;

public class BluetoothService extends Service {

    private BluetoothAdapter mBtAdapter;

    private ServerThread mServerThread;
    private ClientThread mClientThread;

    private Handler mOutHandler;
    private Handler mInHandler;

    private List<ChatMessage> mIncomingQueue;
    private ChatUser mChatPartner;
    private ChatUser mUser;

    private final IBluetoothService.Stub mBinder = new IBluetoothService.Stub() {

        @Override
        public void sendMessage(ChatMessage msg) throws RemoteException {
            Message m = new Message();
            m.obj = msg;
            mOutHandler.dispatchMessage(m);
        }

        @Override
        public void setUser(ChatUser user) throws RemoteException {
            mUser = user;

            Message m = new Message();
            m.obj = user;
            mOutHandler.dispatchMessage(m);
        }

        @Override
        public List<String> getAvailableDevices() throws RemoteException {
            return null;
        }

        @Override
        public List<ChatMessage> getMessageQueue() throws RemoteException {
            List<ChatMessage> queue = mIncomingQueue;
            mIncomingQueue = new LinkedList<>();
            return queue;
        }

        @Override
        public ChatUser getChatPartner() throws RemoteException {
            return mChatPartner;
        }

        @Override
        public void connect(BluetoothDevice device) {
            mClientThread = new ClientThread(device, mInHandler);
        }

        @Override
        public void startListener() {
            mServerThread = new ServerThread(mBtAdapter, mInHandler);
        }

    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        mIncomingQueue = new LinkedList<>();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        mInHandler = new Handler(msg -> {
            if (msg.obj instanceof Handler) {
                mOutHandler = (Handler) msg.obj;
                return true;
            }

            if (msg.obj instanceof Sendable) {
                onSendableReceived((Sendable) msg.obj);
                return true;
            }

            return false;
        });
    }

    @Override
    public void onDestroy() {
        if (mServerThread != null) {
            mServerThread.interrupt();
        } else if (mClientThread != null) {
            mClientThread.interrupt();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void onSendableReceived(Sendable s) {
        if (s instanceof ChatMessage) {
            mIncomingQueue.add((ChatMessage) s);
        } else if (s instanceof ChatUser) {
            mChatPartner = (ChatUser) s;
        }
    }

}
