// IBluetoothService.aidl
package com.example.offlinemessenger;

// Declare any non-default types here with import statements
import android.bluetooth.BluetoothDevice;

import com.example.offlinemessenger.data.ChatMessage;
import com.example.offlinemessenger.data.ChatUser;

interface IBluetoothService {

    /**
     * Send a message to the currently connected device.
     *
     * @param msg The message.
     */
    void sendMessage(in ChatMessage msg);

    /**
     * Set the chat user to interact with.
     *
     * @param user The user.
     */
    void setUser(in ChatUser user);

    /**
     * Get a list of all available bluetooth devices by scanning for them.
     */
    List<String> getAvailableDevices();

    List<ChatMessage> getMessageQueue();

    ChatUser getChatPartner();

    void connect(in BluetoothDevice device);

    void startListener();

}
