package ec.bernix01.m;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularfillableloaders.CircularFillableLoaders;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    /*
        * Notifications from UsbService will be received here.
        */

    //private Toolbar toolbar;
    public static final int LOCATION_BECAUSE_MARSHMALLOWS_FUCKING_NEEDS_IT = 42;

    public static final int REQUEST_GET_DEVICE = 24;
    public static final int REQUEST_ENABLE_BT = 12;
    public final static String TAG = MainActivity.class.getCanonicalName();
    private ScrollView root;
    private final Handler mHandler = new Handler() {


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BTThread.MESSAGE_WRITE:
                    Log.i(TAG, "wrote some bytes...");
                    break;
                case BTThread.MESSAGE_READ:
                    String readMessage = (String) msg.obj;
                    Log.i(TAG, "received some bytes... " + readMessage);
                    display(readMessage);

                    break;
                case BTThread.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    //toolbar.setTitle(msg.getData().getString(BTThread.DEVICE_NAME));
                    //toolbar.setSubtitle(msg.getData().getString(BTThread.DEVICE_ADDRESS));
                    invalidateOptionsMenu();
                    break;
                case BTThread.MESSAGE_SNACK:
                    String msgstr = msg.getData().getString(BTThread.SNACK);
                    if(msgstr != null)
                        Snackbar.make(root, msgstr,
                                Snackbar.LENGTH_SHORT).show();
                    break;
                case BTThread.MESSAGE_DISCONNECTED:
                    Snackbar.make(root,"Disconnected",Snackbar.LENGTH_SHORT).show();
                    //toolbar.setTitle(getString(R.string.title_not_connected));
                    break;
            }
        }
    };
    private BluetoothAdapter bt;
    private BluetoothSocket socket;
    private BTThread btThread;
    private String deviceAddr;
    private double val = 0.0d;
    private TextView metertxt;
    private ImageView m1;
    private ImageView m2;
    private ImageView m3;
    private CircularFillableLoaders meter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                btThread = null;
                invalidateOptionsMenu();
//                toolbar.setTitle(getResources().getString(R.string.title_not_connected));
//                toolbar.setSubtitle(null);
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        meter = (CircularFillableLoaders) findViewById(R.id.circularFillableLoaders);
        meter.setProgress(50);
        metertxt = (TextView) findViewById(R.id.level_txt);
        Button b1 = (Button) findViewById(R.id.button2);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                val = 0;
                display("0.0mg/L");
            }
        });
        m1 = (ImageView) findViewById(R.id.imageView);
        m2 = (ImageView) findViewById(R.id.imageView2);
        m3 = (ImageView) findViewById(R.id.imageView3);
        m2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), CallMeATaxiActivity.class));
            }
        });
        ImageView m4 = (ImageView) findViewById(R.id.imageView4);
        m4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), InfoActivity.class));
            }
        });
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                display("2.9mg/L");
            }
        }, 100);

        root = (ScrollView) findViewById(R.id.root);

        //toolbar = (Toolbar) findViewById(R.id.toolbar);
        bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null) {
            Snackbar.make(root, "Why are you using this if you don't even have a bluetooth adapter?", Snackbar.LENGTH_INDEFINITE).show();
        } else {
            if (!bt.isEnabled()) {
                requestBluetoothEnalbed();
            }
        }
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy, done :)
    }
    private void requestBluetoothEnalbed() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (btThread != null)
            btThread.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainActivity.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Snackbar.make(root, "Without bluetooth this won't work. C'mon human...", Snackbar.LENGTH_INDEFINITE).setAction("enable", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestBluetoothEnalbed();
                    }
                }).show();
            }
        } else if (requestCode == MainActivity.REQUEST_GET_DEVICE) {
            if (resultCode == RESULT_OK)
                deviceAddr = data.getStringExtra("dir");
            else if(resultCode == RESULT_CANCELED)
                return;
            if (deviceAddr != null)
                connect();
            else
                Snackbar.make(root, "Could not get a device address. Please try again.", Snackbar.LENGTH_SHORT).show();
            invalidateOptionsMenu();
        }
    }
    private void requestDeviceAddress() {
        Intent getDevice = new Intent(getApplicationContext(), DevicesActivity.class);
        startActivityForResult(getDevice, MainActivity.REQUEST_GET_DEVICE);
    }

    private void connect() {
        try {
            if (bt.isDiscovering())
                bt.cancelDiscovery();
            BluetoothDevice device = bt.getRemoteDevice(deviceAddr);
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService ID
            socket = device.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            btThread = new BTThread(socket, mHandler);
            btThread.start();
        } catch (IOException e) {
            Snackbar.make(root,"Could not connect '" + e.getLocalizedMessage() + "'",Snackbar.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_connect:
                requestDeviceAddress();
                return true;
            case R.id.action_disconnect:
                disconnect();
                return true;
            case R.id.action_beep:
                btThread.write("Beep!");
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.getItem(0).setVisible(btThread == null);
        menu.getItem(1).setVisible(btThread != null);
        menu.getItem(2).setVisible(btThread != null);
        return true;
    }

    private void disconnect() {
        btThread.cancel();
        btThread = null;
        socket = null;
        invalidateOptionsMenu();
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }



    private void display(String data) {
        Log.i("loooool", data);
        if (data.length() < 5)
            return;
        String mg_L = data.substring(0, data.length() - 4);
        Log.i("md_L", mg_L);
        double tmpval = Double.parseDouble(mg_L);
        if (tmpval < val)
            return;
        val = tmpval;
        metertxt.setText(data.substring(0, data.length() - 4));
        int progress = 100 - (int) ((val / 3.1) * 100);
        Log.i("progress", (val / 3.1) * 100 + "  " + progress);
        meter.setProgress(progress);
        if (val < 0.3) {
            meter.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGood));
            meter.setAmplitudeRatio(0.02f);
            m1.setVisibility(View.GONE);
            m2.setVisibility(View.GONE);
            m3.setVisibility(View.VISIBLE);
            meter.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.cocktail));

            metertxt.setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.black));
        } else if (val >= 0.3 && val < 0.8) {
            meter.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorWarning));
            meter.setAmplitudeRatio(0.02f);
            m1.setVisibility(View.VISIBLE);
            m2.setVisibility(View.VISIBLE);
            m3.setVisibility(View.GONE);
            meter.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.cocktail));
            metertxt.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorx_x));
        } else if (val >= 0.8 && val < 1.2) {
            meter.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorquefalta));
            meter.setAmplitudeRatio(0.04f);
            m1.setVisibility(View.VISIBLE);
            m2.setVisibility(View.VISIBLE);
            m3.setVisibility(View.GONE);
            meter.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.cocktail));
            metertxt.setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.white));
        } else if (val >= 1.2 && val < 2.6) {
            meter.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorDanger));
            meter.setAmplitudeRatio(0.06f);
            m1.setVisibility(View.VISIBLE);
            m2.setVisibility(View.VISIBLE);
            m3.setVisibility(View.GONE);
            metertxt.setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.white));
            meter.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.cocktail));
        } else if (val > 2.5) {
            meter.setProgress(90);
            meter.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorx_x));
            m1.setVisibility(View.VISIBLE);
            m2.setVisibility(View.VISIBLE);
            m3.setVisibility(View.GONE);
            meter.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.skull));

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_joystick, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }



}
