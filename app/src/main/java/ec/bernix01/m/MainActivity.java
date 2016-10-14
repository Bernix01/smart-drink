package ec.bernix01.m;

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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularfillableloaders.CircularFillableLoaders;

import java.lang.ref.WeakReference;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    /*
        * Notifications from UsbService will be received here.
        */

    static StringBuilder data = new StringBuilder();
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private double val = 0.0d;
    private UsbService usbService;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };
    private TextView metertxt;
    private ImageView m1;
    private ImageView m2;
    private ImageView m3;
    private CircularFillableLoaders meter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new MyHandler(this);
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
    }


    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
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

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
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

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String s = msg.obj.toString();
                    if (s.length() > 1) {
                        char a[] = s.toCharArray();
                        for (int i = 0; i < s.length(); i++) {

                            Log.e("message", s + "  " + String.valueOf(s.equals("|")));
                            if (a[i] == '|') {
                                mActivity.get().display(data.toString());
                                data = new StringBuilder();
                            } else
                                data.append(a[i]);
                        }
                    } else {
                        if (s.equals("|")) {
                            mActivity.get().display(data.toString());
                            data = new StringBuilder();
                        } else
                            data.append(s);
                    }
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

}
