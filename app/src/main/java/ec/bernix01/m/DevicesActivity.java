package ec.bernix01.m;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static ec.bernix01.m.MainActivity.REQUEST_ENABLE_BT;

public class DevicesActivity extends AppCompatActivity {

    private CoordinatorLayout coordinatorLayout;
    private MyAdapter adapterP;
    private MyAdapter adapterA;
    private FloatingActionButton fab;
    public final static String TAG = DevicesActivity.class.getCanonicalName();

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "Found a device!");
                // Add the name and address to an array adapter to show in a ListView
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                    adapterA.add(device.getName() + "\n" + device.getAddress());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "Discovery finished.");
                invalidateOptionsMenu();
            }

        }
    };
    private BluetoothAdapter bt;
    private String mConnectedDeviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        RecyclerView recyclerViewP = (RecyclerView) findViewById(R.id.recyclerViewPaired);
        RecyclerView recyclerViewA = (RecyclerView) findViewById(R.id.recyclerViewAvailable);
        assert recyclerViewP != null;
        recyclerViewP.setLayoutManager(new LinearLayoutManager(this));
        assert recyclerViewA != null;
        recyclerViewA.setLayoutManager(new LinearLayoutManager(this));
        assert fab != null;
        adapterA = new MyAdapter(new LinkedList<String>());
        adapterP = new MyAdapter(new LinkedList<String>());
        recyclerViewP.setAdapter(adapterP);
        recyclerViewA.setAdapter(adapterA);
        fab.hide();
        bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null) {
            Snackbar.make(coordinatorLayout, "Bluetooth not available.", Snackbar.LENGTH_INDEFINITE);
        } else {
            if (!bt.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                getDevices();
            }
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(mConnectedDeviceAddress != null){
                    Intent i = new Intent();
                    i.putExtra("dir",mConnectedDeviceAddress);
                    setResult(Activity.RESULT_OK,i);
                }else{
                    setResult(Activity.RESULT_CANCELED);
                }
                finish();
            }
        });
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy, done :)
        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setEnabled(!bt.isDiscovering());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                getDevices();
            }
        }
    }

    private void getDevices() {
        Set<BluetoothDevice> pairedDevices = bt.getBondedDevices();
// If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                adapterP.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MainActivity.LOCATION_BECAUSE_MARSHMALLOWS_FUCKING_NEEDS_IT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doDiscovery();
                } else {
                    Snackbar.make(coordinatorLayout, "C'mon human, Android wants it to be this way...", Snackbar.LENGTH_LONG).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search_devices:
                doDiscovery();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void doDiscovery() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                Snackbar.make(coordinatorLayout, "Because you want to discover new devices...", Snackbar.LENGTH_LONG).show();

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MainActivity.LOCATION_BECAUSE_MARSHMALLOWS_FUCKING_NEEDS_IT);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            if (!bt.isDiscovering())
                bt.startDiscovery();
            invalidateOptionsMenu();
        }
    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<String> mDataset;

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(List<String> myDataset) {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            // create a new view
            LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_device, parent, false);
            // set the view's size, margins, paddings and layout parameters

            ViewHolder vh = new ViewHolder(v);
            vh.textView = (TextView) v.findViewById(R.id.deviceName);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final String dAddress = mDataset.get(position).substring(mDataset.get(position).length() - 17);
            holder.textView.setText(mDataset.get(position));
            if(dAddress.equals(mConnectedDeviceAddress))
                holder.textView.setTypeface(null, Typeface.BOLD);
            else
                holder.textView.setTypeface(null,Typeface.NORMAL);
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mConnectedDeviceAddress = dAddress;
                    fab.show();
                    notifyDataSetChanged();
                }
            });

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }

        public void add(String device) {
            mDataset.add(device);
            notifyDataSetChanged();
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public LinearLayout mTextView;
            public TextView textView;

            public ViewHolder(LinearLayout v) {
                super(v);
                mTextView = v;
            }
        }
    }
}

