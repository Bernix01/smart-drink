package ec.bernix01.m;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by gbern on 3/31/2016.
 */
public class BTThread extends Thread {
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE_FAILED = -1;
    public static final int MESSAGE_WRITE = 2;
    public static final String DEVICE_NAME = "deviceName";
    public static final int MESSAGE_SNACK = 3;
    public static final String SNACK = "snack";
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_DISCONNECTED = 5;
    public static final String DEVICE_ADDRESS = "deviceAddr";
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final BufferedReader bufferedReaderIn;
    private Handler mHandler;

    public BTThread(BluetoothSocket socket, Handler mHandler) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        BufferedReader tmpBIn = null;
        this.mHandler = mHandler;
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {

            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
            tmpBIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        bufferedReaderIn = tmpBIn;
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, mmSocket.getRemoteDevice().getName());
        bundle.putString(DEVICE_ADDRESS, mmSocket.getRemoteDevice().getAddress());
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        this.write("Hello");
    }

    public void run() {
        String line;

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream

                line = bufferedReaderIn.readLine();
                if (line != null)
                    Log.i("btThread", "read!");
                // Send the obtained bytes to the UI activity
                mHandler.obtainMessage(MESSAGE_READ, line)
                        .sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

//        byte[] buffer = new byte[1024];  // buffer store for the stream
//        int bytes; // bytes returned from read()
//
//        // Keep listening to the InputStream until an exception occurs
//        while (true) {
//            try {
//                // Read from the InputStream
//                bytes = mmInStream.read(buffer);
//                Log.i("btThread",new String(buffer, StandardCharsets.UTF_8));
//                // Send the obtained bytes to the UI activity
//                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
//                        .sendToTarget();
//            } catch (IOException e) {
//                break;
//            }
//        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String data) {
        try {
            byte[] bytes = (data+"\n").getBytes();
            mmOutStream.write(bytes);
            mHandler.obtainMessage(MESSAGE_WRITE, bytes);
            Log.i("btThread", "writing...");
        } catch (IOException e) {
            Message msg = mHandler.obtainMessage(MESSAGE_SNACK);
            Bundle bundle = new Bundle();
            bundle.putString(SNACK, "Unable to write.");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmInStream.close();
            mmOutStream.close();
            mmSocket.close();
            mHandler.obtainMessage(MESSAGE_DISCONNECTED).sendToTarget();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}