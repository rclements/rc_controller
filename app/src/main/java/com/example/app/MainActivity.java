package com.example.app;

import android.annotation.TargetApi;
import android.graphics.Color;
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
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VerticalSeekBar;

import java.util.Timer;
import java.util.TimerTask;



public class MainActivity extends ActionBarActivity {
    private TCPClient mTcpClient;

    double mLeft = 0;
    double mRight = 0;

    protected TextView mLeftValue;
    protected TextView mRightValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

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
                    if(stickValue != 0.0){
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

            new connectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            TimerTask task = new TimerTask() {
                public void run(){
                    TankSetter task = new TankSetter();
                    task.execute();
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 0, 100);

            return rootView;
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

    public class connectTask extends AsyncTask<String,String,TCPClient> {

        @Override
        protected TCPClient doInBackground(String... message) {
            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //Toast.makeText(getApplicationContext(), "TCP CLIENT", Toast.LENGTH_SHORT).show();
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                }
            });
            mTcpClient.run();

            return null;
        }
    }

    public class TankSetter extends AsyncTask<Object, Void, String> {
        @Override
        protected String doInBackground(Object... arg0) {
            double left = getLeft();
            double right = getRight();

            if(mTcpClient != null){
                String message = getMessage();
                Log.e("TankSetter", message);
                mTcpClient.sendMessage(message);
            }
            return "nope";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.e("RGBLedSetter", "onPostExecute");
        }

        protected String getMessage() {
            return "L" + Double.toString(mLeft) + "R" + Double.toString(mRight) + "\n";
        }
    }
}