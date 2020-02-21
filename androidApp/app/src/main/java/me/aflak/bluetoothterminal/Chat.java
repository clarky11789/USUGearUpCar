package me.aflak.bluetoothterminal;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
//import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
//import android.widget.Button;
//import android.widget.EditText;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.aflak.bluetooth.Bluetooth;

public class Chat extends AppCompatActivity implements Bluetooth.CommunicationCallback {
    private ArrayList<String> speed = new ArrayList<>();
    private ArrayList<String> charge = new ArrayList<>();
    private ArrayList<String> current = new ArrayList<>();
    private ArrayList<String> voltage = new ArrayList<>();
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
    //private String name;
    private Bluetooth b;
    //private EditText message;
    //private Button send;
    private TextView text;
    private TextView speedText;
    private TextView chargeText;
    private TextView currentText;
    private TextView voltageText;
    private ScrollView scrollView;
    private boolean registered=false;

    Handler postHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        speed.add("0");
        charge.add("0");
        current.add("0");
        voltage.add("0");
        carId = "1";
        unsentDataId = 0;
        latestVoltage = null;
        latestCurrent = null;
        latestCharge = null;
        latestSpeed = null;
        batteryCapacity = 3110400;
        //Bundle bundle = getIntent().getExtras();
        //url = "http://"+bundle.getString("ipAddress")+":3000";
        url = "http://ec2-54-187-254-25.us-west-2.compute.amazonaws.com:3000";
        //url = "http://localhost:3000";

        postHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                postData();
                postHandler.postDelayed(this, 250);
            }
        }, 250);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //text = (TextView)findViewById(R.id.speedTextDesc);
        speedText = (TextView)findViewById(R.id.speedText);
        chargeText = (TextView)findViewById(R.id.chargeText);
        currentText = (TextView)findViewById(R.id.currentText);
        voltageText = (TextView)findViewById(R.id.voltageText);
        //message = (EditText)findViewById(R.id.message);
        //send = (Button)findViewById(R.id.send);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        //text.setMovementMethod(new ScrollingMovementMethod());
        //send.setEnabled(false);

        b = new Bluetooth(this);
        b.enableBluetooth();

        b.setCommunicationCallback(this);

        int pos = getIntent().getExtras().getInt("pos");
        //name = b.getPairedDevices().get(pos).getName();

        Display("Connecting...");
        b.connectToDevice(b.getPairedDevices().get(pos));
        /*
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = message.getText().toString();
                message.setText("");
                b.send(msg);
                Display("You: "+msg);
            }
        });
        */
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

    public void postData(){
        Display("this is a message");
        if(latestSpeed != null){
            postSpeed();
            latestSpeed = new Pair<>(0.0, String.valueOf(System.currentTimeMillis()/1000));;
        }
        if(latestCharge != null){
            postCharge();
            latestCharge = null;
        }
        if(latestVoltage != null){
            postVoltage();
            latestVoltage = null;
        }
        if(latestCurrent != null){
            postCurrent();
            latestCurrent = null;
        }
    }

    private void postSpeed(){
        Map<String, String> postData = new HashMap<>();
        postData.put("carId", carId);
        postData.put("indicator", "spe");
        postData.put("val", String.valueOf(latestSpeed.first));
        postData.put("timeStamp", latestSpeed.second);
        HttpPostAsyncTask task = new HttpPostAsyncTask(postData);
        task.execute( url + "/update");
    }

    private void postCharge(){
        Map<String, String> postData = new HashMap<>();
        postData.put("carId", carId);
        postData.put("indicator", "cha");
        postData.put("val", String.valueOf(latestCharge.first));
        postData.put("timeStamp", latestCharge.second);
        HttpPostAsyncTask task = new HttpPostAsyncTask(postData);
        task.execute( url + "/update");
    }

    private void postCurrent(){
        Map<String, String> postData = new HashMap<>();
        postData.put("carId", carId);
        postData.put("indicator", "cur");
        postData.put("val", String.valueOf(latestCurrent.first));
        postData.put("timeStamp", latestCurrent.second);
        HttpPostAsyncTask task = new HttpPostAsyncTask(postData);
        task.execute( url + "/update");
    }

    private void postVoltage(){
        Map<String, String> postData = new HashMap<>();
        postData.put("carId", carId);
        postData.put("indicator", "vol");
        postData.put("val", String.valueOf(latestVoltage.first));
        postData.put("timeStamp", latestVoltage.second);
        HttpPostAsyncTask task = new HttpPostAsyncTask(postData);
        task.execute( url + "/update");
    }

    private class HttpPostAsyncTask extends AsyncTask<String, Void, String> {
        // This is the JSON body of the post
        JSONObject postData;
        Integer postDataId;

        // This is a constructor that allows you to pass in the JSON body
        public HttpPostAsyncTask(Map<String, String> postData) {
            if (postData != null) {
                this.postData = new JSONObject(postData);
            }
        }

        private String convertInputStreamToString(InputStream inputStream) {
            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                while((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }
        // This is a function that we are overriding from AsyncTask. It takes Strings as parameters because that is what we defined for the parameters of our async task
        @Override
        protected String doInBackground(String... params) {

            try {
                // This is getting the url from the string we passed in
                URL url = new URL(params[0]);

                // Create the urlConnection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();


                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                urlConnection.setRequestProperty("Content-Type", "application/json");

                urlConnection.setRequestMethod("POST");


                // OPTIONAL - Sets an authorization header
                urlConnection.setRequestProperty("Authorization", "someAuthString");

                // Send the post body
                if (this.postData != null) {
                    OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                    writer.write(postData.toString());
                    writer.flush();
                }

                int statusCode = urlConnection.getResponseCode();

                if (statusCode ==  200) {

                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                    String response = convertInputStreamToString(inputStream);

                    return "good";
                } else {
                    return "not";
                }

            } catch (Exception e) {
                //Log.d("sendData", e.getLocalizedMessage());
                return "not";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // Call activity method with results
            if(result == "good"){ //if it went well, remove data from the map
                Log.i("post", "sending data worked");
                //unsentData.remove(this.postDataId);
            } else if (result == "not") {// if not, add id back to list to be tried again
                Log.i("post", "sending data did not work");
                //unsentDataList.add(0, this.postDataId);
            }
        }
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

    public void Display(final String s){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //text.setText("just got" + s + "\n");
                String speedVal =  String.valueOf((int) Double.parseDouble(speed.get(speed.size() - 1)));
                if(latestCharge != null){
                    String chargeVal =  String.valueOf( (int) ((latestCharge.first/batteryCapacity)*100));
                    chargeText.setText(chargeVal);
                }
                speedText.setText(speedVal);
                currentText.setText(current.get(current.size() - 1));
                voltageText.setText(voltage.get(voltage.size() - 1));
            }
        });
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        Display("Connected to "+device.getName()+" - "+device.getAddress());
        /*this.runOnUiThread(new Runnable() {
            //@Override
            //public void run() {
            //    send.setEnabled(true);
            //}
        });*/
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        Display("Disconnected!");
        Display("Connecting again...");
        b.connectToDevice(device);
    }

    @Override
    public void onMessage(String message) {
        String timestamp = String.valueOf(System.currentTimeMillis()/1000);
        String[] findNum = message.split(": ");
        if(findNum.length == 1){
            findNum = message.split("= ");
        }
        if(findNum[0].equals("MPH")){
            latestSpeed = new Pair<>(Double.parseDouble(findNum[1]), timestamp);
            speed.add(findNum[1]);
            findNum[0] = "speed";
        } else if(findNum[0].equals("current")){
            latestCurrent = new Pair<>(Double.parseDouble(findNum[1]), timestamp);
            current.add(findNum[1]);
        } else if(findNum[0].equals("INPUT V")){
            latestVoltage = new Pair<>(Double.parseDouble(findNum[1]), timestamp);
            voltage.add(findNum[1]);
            findNum[0] = "voltage";
        } else if(findNum[0].equals("charge")){
            if(Double.parseDouble(findNum[1]) > batteryCapacity){
                latestCharge = new Pair<>(Double.parseDouble(String.valueOf(batteryCapacity)), timestamp);
            } else {
                latestCharge = new Pair<>(Double.parseDouble(findNum[1]), timestamp);
            }
            findNum[1] = String.valueOf(latestCharge);
            charge.add(findNum[1]);
        }
    }

    @Override
    public void onError(String message) {
        Display("Error: "+message);
    }

    @Override
    public void onConnectError(final BluetoothDevice device, String message) {
        Display("Error: "+message);
        Display("Trying again in 3 sec.");
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
