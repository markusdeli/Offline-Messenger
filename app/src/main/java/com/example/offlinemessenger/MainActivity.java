package com.example.offlinemessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.room.Room;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.offlinemessenger.data.ChatMessage;
import com.example.offlinemessenger.data.ChatUser;
import com.example.offlinemessenger.data.ChatViewModel;
import com.example.offlinemessenger.data.ChatViewModelFactory;
import com.example.offlinemessenger.data.Sendable;
import com.example.offlinemessenger.db.AppDatabase;
import com.example.offlinemessenger.service.BluetoothService;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * The app's main (starting) activity.
 */
public class MainActivity extends AppCompatActivity {

    BluetoothConnectionService mBtConnectionService;

    private static final UUID UUID_INSECURE =
            UUID.fromString("c199f1da-5634-44bf-ad17-394d1c186a24");
    private static final String TAG = "Main Activity";
    private static final int REQUEST_ENABLE_BLUETOOTH = 110;
    private static final int REQUEST_DISCOVER_DEVICES = 111;

    private List<ChatMessage> mMessageList = new LinkedList<>();
    private ChatUser user = new ChatUser(Sendable.Action.NONE, "Heinz");

    private String mBtDeviceAddress;
    private ListView mConversationsListView;
    private EditText mOutput;
    private BluetoothAdapter mBluetoothAdapter;
    private MediaPlayer mp;

    /** The view model for this activity. */
    ChatViewModel mViewModel;

    private final BroadcastReceiver receiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                onBtConnectionStateChanged(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        instantiateElements();

        AppDatabase appDB = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "offline-chat"
        ).build();

        mp = MediaPlayer.create(this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sound11));

        mViewModel = ViewModelProviders.of(this, new ChatViewModelFactory(appDB))
                .get(ChatViewModel.class);
        mViewModel.getChatMessages().observe(this, chatMessages -> {
            mMessageList = chatMessages;
            mConversationsListView.invalidateViews();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.app_bar_switch:
                if (mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(
                            this,
                            "Bluetooth is already enabled!",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                }
                break;

            case R.id.discover_item:
                Intent intent = new Intent(getApplicationContext(), DeviceManager.class);
                startActivityForResult(intent, REQUEST_DISCOVER_DEVICES);
                break;

            case R.id.secure_connect_button:
                setupMediaPlayer();
                startConnection();
                break;

            case R.id.discoverable_button:
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                        300
                );
                startActivity(discoverableIntent);
                IntentFilter intentFilter =
                        new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                registerReceiver(receiver1,intentFilter);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, BluetoothService.class);
        // TODO: call bindService() here
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        mp.release();

        // Unregister broadcast listeners
        this.unregisterReceiver(receiver1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_DISCOVER_DEVICES && resultCode == Activity.RESULT_OK) {
            mBtDeviceAddress = data.getExtras().getString(DeviceManager.EXTRA_ADDRESS);
            if (mBtDeviceAddress != null) {
                //mBtConnectionService = new BluetoothConnectionService(mHandler);
            }
        }
    }

    /**
     * Initiate a connection to another bluetooth device.
     */
    public void startConnection() {
        if (mBtDeviceAddress != null) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mBtDeviceAddress);
            Toast toast = Toast.makeText(this, "Connecting to " +
                    device.toString(), Toast.LENGTH_SHORT);
            toast.show();
            mBtConnectionService.startClient(device, UUID_INSECURE);
        } else {
            Toast toast = Toast.makeText(this, "Choose a device first!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * Instantiate and set up all required UI elements.
     */
    private void instantiateElements() {
        mOutput = findViewById(R.id.edit_text_out);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mConversationsListView = findViewById(R.id.conversation_list_view);
        mConversationsListView.setAdapter(new ConversationListAdapter(mMessageList, getLayoutInflater()));
    }

    /**
     * Callback when the send button has been clicked.
     *
     * @param v The button that has been clicked.
     */
    public void sendButtonClicked(View v) {
        if (mBtConnectionService == null) {
            Toast.makeText(
                    this,
                    "You need to connect to a device first!",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        mViewModel.sendMessage(mOutput.getText().toString());
        mOutput.setText("");
    }

    /**
     * instantiate media player.
     */
    private void setupMediaPlayer(){
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.start();

    }
    /**
     * Callback for changes to the bluetooth connection state.
     *
     * @param intent The intent carrying information about the new state.
     */
    private void onBtConnectionStateChanged(Intent intent) {
        int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

        switch (mode) {
            //Device is in Discoverable Mode
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                break;

            //Device not in discoverable mode
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                break;

            case BluetoothAdapter.SCAN_MODE_NONE:
                Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                break;

            case BluetoothAdapter.STATE_CONNECTING:
                Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                break;

            case BluetoothAdapter.STATE_CONNECTED:
                Log.d(TAG, "mBroadcastReceiver2: Connected.");
                break;
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     *
     * TODO: finish this
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

}
