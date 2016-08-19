//*************************************************************************************************
// Developer: Tyler Weiss
// Last Update: 8/18/2016
//
// Summary: The Main Activity supports the text and button inputs for "Mr. Robot".
//          Commands to the raspberry pi(aka Mr. Robot) are sent through a socket.
//          Response to the command is shown on the bottom of the screen in plain text
//          and a text-to-speech object reads the command to the user.
//
//*************************************************************************************************


package com.example.rec0nrat.socketserver;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // UI objects
    private TextView textResponse;
    private EditText editTextAddress, editTextPort;
    private Button buttonConnect, buttonRight, buttonLeft, buttonStop, buttonKill, buttonForward, buttonMovie;

    // message buffers and tts
    private PrintWriter out;
    private BufferedReader in;
    private TextToSpeech t1;
    private String response;

    // DEBUG TAG
    private static final String TAG = "TCP-DEBUG-CONNECTION:";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // GET reference to UI elements
        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        buttonConnect = (Button) findViewById(R.id.connect);
        buttonRight = (Button)findViewById(R.id.right);
        textResponse = (TextView) findViewById(R.id.response);
        buttonLeft = (Button)findViewById(R.id.left);
        buttonStop = (Button)findViewById(R.id.stop);
        buttonKill = (Button)findViewById(R.id.kill);
        buttonForward = (Button)findViewById(R.id.forward);
        buttonMovie = (Button)findViewById(R.id.movie);

        // SET button listeners
        buttonConnect.setOnClickListener(this);
        buttonForward.setOnClickListener(this);
        buttonKill.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        buttonLeft.setOnClickListener(this);
        buttonRight.setOnClickListener(this);
        buttonMovie.setOnClickListener(this);

        // ttf setup - modify rate/pitch for a more robot-like voice
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                    t1.setSpeechRate(0.7f);
                    t1.setPitch(0.3f);
                }
            }
        });

    }

    // Handle tts object on pause
    public void onPause(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }

    /**
     * Choose which command to send to RPi on button click
     * @param view  - The button clicked
     */
    @Override
    public void onClick(View view) {

        // Create an async task to send command
        MyClientTask myClientTask;

        // Default values for quick debugging
        if(editTextAddress.getText().toString().equals("") || editTextPort.getText().toString().equals("")) {
            editTextAddress.setText("172.17.2.96");
            editTextPort.setText("666");
        }

        // ID the button and send corresponding command through async task
        if(view == buttonConnect){
            myClientTask = new MyClientTask(
                    editTextAddress.getText().toString(),
                    Integer.parseInt(editTextPort.getText().toString()),
                    "CONNECT");
            myClientTask.execute();

        }
       else if(view == buttonForward){
            myClientTask = new MyClientTask(
                    editTextAddress.getText().toString(),
                    Integer.parseInt(editTextPort.getText().toString()),
                    "FORWARD");
            myClientTask.execute();

        }
        else if(view == buttonRight){
            myClientTask = new MyClientTask(
                    editTextAddress.getText().toString(),
                    Integer.parseInt(editTextPort.getText().toString()),
                    "TURN RIGHT");
            myClientTask.execute();

        }
        else if(view == buttonLeft){
            myClientTask = new MyClientTask(
                    editTextAddress.getText().toString(),
                    Integer.parseInt(editTextPort.getText().toString()),
                    "TURN LEFT");
            myClientTask.execute();

        }
        else if(view == buttonKill){
            myClientTask = new MyClientTask(
                    editTextAddress.getText().toString(),
                    Integer.parseInt(editTextPort.getText().toString()),
                    "KILL");
            myClientTask.execute();

        }
        else if(view == buttonStop){
            myClientTask = new MyClientTask(
                    editTextAddress.getText().toString(),
                    Integer.parseInt(editTextPort.getText().toString()),
                    "STOP");
            myClientTask.execute();
        }
        else if(view == buttonMovie){
            myClientTask = new MyClientTask(
                    editTextAddress.getText().toString(),
                    Integer.parseInt(editTextPort.getText().toString()),
                    "MOVIE");
            myClientTask.execute();
        }

    }


    /**
     * Async task to facilitate communication with RPi without halting main thread execution
     */
    public class MyClientTask extends AsyncTask<Void, Void, Void> {

        // Message and Address of command destination
        String dstAddress;
        int dstPort;
        String message;

        MyClientTask(String addr, int port, String cmd) {
            dstAddress = addr;
            dstPort = port;
            message = cmd;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            try {

                Socket socket = new Socket(dstAddress, dstPort) ; // setup socket connection to RPi

                // input/output buffers for communication
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                in = new BufferedReader( new InputStreamReader(socket.getInputStream()));

                // send message over socket
                out.flush();
                out.print(message);
                out.flush();

                //Log.i(TAG, "Before Response");

                char[] buffer = new char[1024];   // stores response

                // retrieve response
                in.read(buffer);
                response = new String(buffer);

                //Log.i(TAG, response);

                socket.close();     // cleanup socket connection

            } catch (UnknownHostException e) {
                Toast.makeText(MainActivity.this, "Can't Find Host", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Having Trouble Communicating", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            return null;
        }

        /**
         * Communication thread post execution
         * @param result return value of async task
         */
        @Override
        protected void onPostExecute(Void result) {
            //show response on the bottom of the screen and repeat response with tts
            textResponse.setText(response);
            super.onPostExecute(result);
            t1.speak(textResponse.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
        }

    }

}