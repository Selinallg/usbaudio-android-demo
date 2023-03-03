package com.luoxi.uacdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.luoxi.uac.AudioPlayback;
import com.luoxi.uac.UsbAudio;

import org.libusb.UsbHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String ACTION_USB_PERMISSION = "com.minelab.droidspleen.USB_PERMISSION";
    private final Logger mylog = Logger.getLogger(TAG);
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator;
    public static final String DIRECTORY_NAME = "LUOXI/debug";

    private PendingIntent mPermissionIntent = null;
    private UsbManager mUsbManager = null;
    private UsbDevice mAudioDevice = null;

    private UsbAudio mUsbAudio = null;

    private Thread mUsbThread = null;

    private UsbReciever mUsbPermissionReciever;

    private String mShortMsg = "正在处理中";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FileHandler fileHandler = null;
        try {
            String fileName = ROOT_PATH + DIRECTORY_NAME + File.separator + "log.txt";
            File file = new File(fileName);
            file.setWritable(true);
            FileOutputStream outputStream =new FileOutputStream(file);

            outputStream.write(fileName.getBytes());
            outputStream.flush();
            outputStream.close();


            if(file.exists()){
                fileHandler = new FileHandler(fileName, true);
                fileHandler.setLevel(Level.INFO);
                mylog.addHandler(fileHandler);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("onCreate",e.getMessage());
        }

        // Grab the USB Device so we can get permission
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        int count = 0;
        mShortMsg = "USB设备：";
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            count ++;
            UsbInterface intf = device.getInterface(0);
            if (intf.getInterfaceClass() == UsbConstants.USB_CLASS_AUDIO) {
                mAudioDevice = device;
            }

            mShortMsg += "No." + count;
            mShortMsg += "Audio class device: " + device;

        }

        mylog.info(mShortMsg);

        // Load native lib
        System.loadLibrary("usb-1.0");
        UsbHelper.useContext(getApplicationContext());

        mUsbAudio = new UsbAudio();

        AudioPlayback.setup();

        // Buttons
        final Button startButton = (Button) findViewById(R.id.start_button);
        final Button stopButton = (Button) findViewById(R.id.stop_button);

        startButton.setEnabled(true);
        stopButton.setEnabled(false);

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mShortMsg = "Start pressed";
                Log.d(TAG, mShortMsg);
                try{
                    boolean ret = mUsbAudio.setup();
                    if (ret == true) {
                        startButton.setEnabled(false);
                        stopButton.setEnabled(true);
                    }
                    else
                    {
                        mylog.info("设备绑定出错");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    mShortMsg = e.getMessage();
                    Log.d(TAG, mShortMsg);
                }

                new Thread(new Runnable() {
                    public void run() {
                        while (true) {
                            try {
                                mUsbAudio.loop();
                            }catch (Exception e){
                                e.printStackTrace();
                                Log.d(TAG, e.getMessage());
                            }
                        }
                    }
                }).start();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mShortMsg = "Stop pressed";
                Log.d(TAG, mShortMsg);
                mUsbAudio.stop();
                mUsbAudio.close();

                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });

        // Register for permission
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        mUsbPermissionReciever = new UsbReciever();
        registerReceiver(mUsbPermissionReciever, filter);

        // Request permission from user
        if (mAudioDevice != null && mPermissionIntent != null) {
            mUsbManager.requestPermission(mAudioDevice, mPermissionIntent);
        } else {
            Log.e(TAG, "Device not present? Can't request peremission");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbPermissionReciever);
        if (mUsbAudio != null) {
            mUsbAudio.stop();
            mUsbAudio.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    private void setDevice(UsbDevice device) {
        // Set button to enabled when permission is obtained
        ((Button) findViewById(R.id.start_button)).setEnabled(device != null);
    }
    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    private class UsbReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (ACTION_USB_PERMISSION.equals(action)) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    setDevice(device);
                } else {
                    String msg = "Permission denied for device " + device;
                    showShortMsg(msg);
                    Log.d(TAG, msg);
                }
            }
        }
    }
}

