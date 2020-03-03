package me.aflak.bluetoothterminal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.aflak.bluetooth.Bluetooth;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.lang.System.exit;

public class Chat extends AppCompatActivity implements Bluetooth.CommunicationCallback {
    private ArrayList<String> speed = new ArrayList<>();
    private ArrayList<String> charge = new ArrayList<>();
    private ArrayList<String> current = new ArrayList<>();
    private ArrayList<String> voltage = new ArrayList<>();
    private ArrayList<String> time = new ArrayList<>();
    Map<Integer, Map<String, String>> unsentData = new HashMap<>();
    ArrayList<Integer> unsentDataList = new ArrayList<>();
    Integer batteryCapacity;
    String url;
    String carId;
    Pair<Double, String> latestSpeed;
    Pair<Double, String> latestCurrent;
    Pair<Double, String> latestVoltage;
    Pair<Double, String> latestCharge;
    Integer unsentDataId;

    private Bluetooth b;
    private TextView text;
    private TextView speedText;
    private TextView chargeText;
    private TextView currentText;
    private TextView voltageText;
    private ScrollView scrollView;
    private boolean registered=false;

    final static String speedfileName = "speedData.txt";
    final static String currentfileName = "currentData.txt";
    final static String chargefileName = "chargeData.txt";
    final static String voltagefileName = "voltageData.txt";
    final static String timefileName = "timeData.txt";
    final static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/instinctcoder/readwrite/" ;

    Handler postHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        speed.add("0");
        charge.add("0");
        current.add("0");
        voltage.add("0");
        carId = "1";
        unsentDataId = 0;

        batteryCapacity = 3110400;

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        speedText = (TextView)findViewById(R.id.speedText);
        chargeText = (TextView)findViewById(R.id.chargeText);
        currentText = (TextView)findViewById(R.id.currentText);
        voltageText = (TextView)findViewById(R.id.voltageText);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        b = new Bluetooth(this);
        b.enableBluetooth();

        b.setCommunicationCallback(this);

        int pos = getIntent().getExtras().getInt("pos");

        Display1(String.valueOf((int) Double.parseDouble(speed.get(speed.size() - 1))), current.get(current.size() - 1), voltage.get(voltage.size() - 1),charge.get(charge.size() - 1));
        b.connectToDevice(b.getPairedDevices().get(pos));

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        registered=true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(registered) {
            unregisterReceiver(mReceiver);
            registered=false;
        }
    }

    //final static String TAG = FileHelper.class.getName();
    public static boolean saveToFile( String data, String fileName){
        try {
            new File("/sdcard/Download/data").mkdir();
            File file = new File("/sdcard/Download/data"+ fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file,true);
            fileOutputStream.write((data + System.getProperty("line.separator")).getBytes());

            fileOutputStream.close();
            return true;
        }  catch(FileNotFoundException ex) {
            exit(1);
        }  catch(IOException ex) {
            exit(1);
        }
        return  false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.close:
                b.removeCommunicationCallback();
                b.disconnect();
                Intent intent = new Intent(this, Select.class);
                startActivity(intent);
                finish();
                return true;

            case R.id.rate:
                Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void Display1(final String speedVal,final String discurrent,final String disvoltage,final String discharge  ){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chargeText.setText(discharge);
                speedText.setText(speedVal);
                currentText.setText(discurrent);
                voltageText.setText(disvoltage);
            }
        });
    }
    @Override
    public void onConnect(BluetoothDevice device) {    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        b.connectToDevice(device);
        Display1(String.valueOf((int) Double.parseDouble(speed.get(speed.size() - 1))), current.get(current.size() - 1), voltage.get(voltage.size() - 1),charge.get(charge.size() - 1));
    }

    @Override
    public void onMessage(String message) {
        String timestamp = String.valueOf(System.currentTimeMillis()/1000);
        String[] findNum = message.split(": ");
        if(findNum.length == 1){
            findNum = message.split("= ");
        }
        if(findNum[0].equals("MPH")){
            speed.add(findNum[1]);
            findNum[0] = "speed";
            saveToFile(String.valueOf((int) Double.parseDouble(speed.get(speed.size() - 1))) , speedfileName);
        } else if(findNum[0].equals("current")){
            current.add(findNum[1]);
            saveToFile(current.get(current.size() - 1), currentfileName);
        } else if(findNum[0].equals("INPUT V")){
            voltage.add(findNum[1]);
            findNum[0] = "voltage";
            saveToFile(voltage.get(voltage.size() - 1), voltagefileName);
        } else if(findNum[0].equals("SOC")){
            charge.add(findNum[1]);
            findNum[0] = "SOC";
            saveToFile(charge.get(charge.size() - 1), chargefileName);
        } else if(findNum[0].equals("milliseconds")){
            time.add(findNum[1]);
            findNum[0] = "time";
            saveToFile(time.get(time.size() - 1), timefileName);
        }
        Display1(String.valueOf((int) Double.parseDouble(speed.get(speed.size() - 1))), current.get(current.size() - 1), voltage.get(voltage.size() - 1),charge.get(charge.size() - 1));
    }

    @Override
    public void onError(String message) {
        Display1(String.valueOf((int) Double.parseDouble(speed.get(speed.size() - 1))), current.get(current.size() - 1), voltage.get(voltage.size() - 1),charge.get(charge.size() - 1));
    }

    @Override
    public void onConnectError(final BluetoothDevice device, String message) {
        Display1(String.valueOf((int) Double.parseDouble(speed.get(speed.size() - 1))), current.get(current.size() - 1), voltage.get(voltage.size() - 1),charge.get(charge.size() - 1));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        b.connectToDevice(device);
                    }
                }, 2000);
            }
        });
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Intent intent1 = new Intent(Chat.this, Select.class);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                }
            }
        }
    };
}
