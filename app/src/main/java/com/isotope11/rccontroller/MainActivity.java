package com.isotope11.rccontroller;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VerticalSeekBar;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangFloat;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity {
    double mLeft = 0;
    double mRight = 0;

    boolean mIsReady = false;

    protected TextView mLeftValue;
    protected TextView mRightValue;

    static final String TAG = "RCController";
    static final String COOKIE = "test";
    static Context context;
    static OtpNode self;
    static OtpMbox mbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.context = getApplicationContext();

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    public float getMapped(int val) {
        float newVal = Float.valueOf(val) / Float.valueOf(255);
        Log.d("UI", "getMapped: " + Float.toString(newVal));
        return newVal;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setRetainInstance(true); // Don't throw this guy away on rotate
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            final Button copyButton = (Button) rootView.findViewById(R.id.copyButton);
            final Button launchErlangButton = (Button) rootView.findViewById(R.id.launchErlangButton);


            copyButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    copyErlangOntoFs();
                    makeExecutable("/erlang/bin/epmd");
                    makeExecutable("/erlang/bin/erl");
                }
            });

            launchErlangButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchEpmd();
                    launchErlangNode();
                    mIsReady = true;
                }
            });

            final VerticalSeekBar leftStick = (VerticalSeekBar) rootView.findViewById(R.id.leftStick);
            final VerticalSeekBar rightStick = (VerticalSeekBar) rootView.findViewById(R.id.rightStick);
            mLeftValue = (TextView) rootView.findViewById(R.id.leftValue);
            mRightValue = (TextView) rootView.findViewById(R.id.rightValue);

            leftStick.setProgress(50);
            mLeftValue.setText(Double.toString(0));
            leftStick.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    double stickValue = mapStickValueToMotorValue(i);
                    if(stickValue != 0.0){
                        mLeft = stickValue;
                    }
                    mLeftValue.setText(Double.toString(stickValue));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // NOTE: This should happen, but it moves the thumb to the bottom for some inexplicable reason
                    leftStick.setProgress(50);
                    mLeft = 0.01;
                    mLeftValue.setText(Double.toString(mapStickValueToMotorValue(50)));
                }
            });

            rightStick.setProgress(50);
            mRightValue.setText(Double.toString(0));
            rightStick.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    double stickValue = mapStickValueToMotorValue(i);
                    if (stickValue != 0.0) {
                        mRight = stickValue;
                    }
                    mRightValue.setText(Double.toString(stickValue));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // NOTE: This should happen, but it moves the thumb to the bottom for some inexplicable reason
                    rightStick.setProgress(50);
                    mRight = 0.01;
                    mRightValue.setText(Double.toString(mapStickValueToMotorValue(50)));
                }
            });

            TimerTask task = new TimerTask() {
                public void run(){
                    if(mIsReady){
                        TankSetter task = new TankSetter();
                        task.execute();
                    }
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 0, 100);

            return rootView;
        }

        public void copyErlangOntoFs() {
            Log.d(TAG, "copyErlangOntoFs start");

            InputStream erlangZipFileInputStream = null;
            try {
                erlangZipFileInputStream = getActivity().getApplicationContext().getAssets().open("erlang_R16B.zip");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, erlangZipFileInputStream.toString());
            Log.d(TAG, MainActivity.context.getFilesDir().getPath());
            Decompress unzipper = new Decompress(erlangZipFileInputStream, MainActivity.context.getFilesDir().getPath() + "/");
            unzipper.unzip();

            Log.d(TAG, "copyErlangOntoFs done");
        }

        public void makeExecutable(String path) {
            this.doCommand("/system/bin/chmod 777 " + MainActivity.context.getFilesDir().getPath() + path);
        }

        public void doCommand(String command) {
            try {
                // Executes the command.
                Process process = Runtime.getRuntime().exec(command);

                // Reads stdout.
                // NOTE: You can write to stdin of the command using
                //       process.getOutputStream().
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                int read;
                char[] buffer = new char[4096];
                StringBuffer output = new StringBuffer();
                while ((read = reader.read(buffer)) > 0) {
                    output.append(buffer, 0, read);
                }
                reader.close();

                // Waits for the command to finish.
                process.waitFor();

                // send output to the log
                Log.d(TAG, output.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void launchEpmd() {
            this.doCommand(MainActivity.context.getFilesDir().getPath() + "/erlang/bin/epmd -daemon");
        }

        public void launchErlangNode() {
            String ip = Utils.getIPAddress(true);
            this.doCommand(MainActivity.context.getFilesDir().getPath() + "/erlang/bin/erl -name foo@" + ip + " -setcookie " + COOKIE);
        }
    }



    protected double mapStickValueToMotorValue(int stickValue) {
        double middle = 50;
        double middleMapped = (double) stickValue - middle;
        return middleMapped / middle;
    }

    protected double getLeft(){
        return mLeft;
    }

    protected double getRight(){
        return mRight;
    }

    protected void setLeft(double val){
        mLeft = val;
    }

    protected void setRight(double val){
        mRight = val;
    }


    public class TankSetter extends AsyncTask<Object, Void, String> {
        final String remoteNodeName = "server@192.168.1.10";

        @Override
        protected String doInBackground(Object... arg0) {
            prepareNode();
            updateTank();
            return "whatevs...";
        }

        public void prepareNode(){
            if(self == null){
                try {
                    self = new OtpNode("mynode", COOKIE);
                    mbox = self.createMbox("tanksetter");
                    if (self.ping(remoteNodeName, 2000)) {
                        System.out.println("remote is up");
                    } else {
                        System.out.println("remote is not up");
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            //Log.e(TAG, "onPostExecute");
        }

        public void updateTank(){
            castUpdate((float) mLeft, (float) mRight);
            castBlast();
        }

        public void castUpdate(Float left, Float right){
            OtpErlangObject[] message = new OtpErlangObject[3];
            message[0] = new OtpErlangAtom("update");
            message[1] = new OtpErlangFloat(left);
            message[2] = new OtpErlangFloat(right);

            cast(new OtpErlangTuple(message));
        }

        public void castBlast(){
            cast(new OtpErlangAtom("blast"));
        }

        public void cast(OtpErlangObject message){
            //Log.e(TAG, "updateLed");
            OtpErlangObject[] castMsg = new OtpErlangObject[2];
            castMsg[0] = new OtpErlangAtom("$gen_cast");
            castMsg[1] = message;

            mbox.send("raspi_tank", remoteNodeName, new OtpErlangTuple(castMsg));
            Log.d(TAG, "cast completed");
        }
    }
}