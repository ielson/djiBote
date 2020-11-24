package com.ielson.djiBote;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.dji.videostreamdecodingsample.media.DJIVideoStreamDecoder;
import com.dji.videostreamdecodingsample.media.NativeHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.common.error.DJIError;
import geometry_msgs.Point;


import org.ros.EnvironmentVariables;
import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosTextView;
import org.ros.internal.node.server.master.MasterRegistrationListener;
import org.ros.master.client.MasterStateClient;
import org.ros.master.client.SystemState;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;



public class MainActivity extends RosActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    protected TextureView mVideoSurface = null;
    private Button mLandBtn, mTakeOffBtn, mStickBtn;
    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    private static BaseProduct product;
    public static FlightController mFlightController;
    private OnScreenJoystick mScreenJoystickRight;
    private OnScreenJoystick mScreenJoystickLeft;

    public static TextView mTextView;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    public final static boolean useSimulator = true;
    public static final Object TAKEOFFSTART = 1;
    public static final Object TAKEOFFCOMPLETE = 2;
    public static final Object TAKEOFFATTEMPTIG = 3;
    public static final Object VIRTUALSTICKSTART = 4;
    public static final Object VIRTUALSTICKATTEMPTING = 5;
    public static final Object VIRTUALSTICKCOMPLETE = 6;


    public enum State  {TAKEOFFSTART, TAKEOFFATTEMPTIG, TAKEOFFCOMPLETE, VIRTUALSTICKSTART, VIRTUALSTICKATTEMPTING, VIRTUALSTICKCOMPLETE;}
    public static State state = State.TAKEOFFSTART;

    protected DJICodecManager mCodecManager = null;

    private Talker talker;
    private RosDjiCameraPreviewView rosDjiCameraPreviewView;
    private CmdVelListener cmdVelListener;



    public MainActivity() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("DJIBote", "Communication DJI-ROS");
        // If you know the IP/Port of the ROS Master, you can set it as follows and avoid having the Master Chooser activity:
        //super("DJI-Ros Driver Activity", "DJI-Ros Driver Activity", URI.create("http://10.42.0.1:11311")
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cmdVelListener = new CmdVelListener(MainActivity.this);
        Log.e("CMDVEL", "cmdVel Created");
        initUI();
        // The callback for receiving the raw H264 video data for camera live view
        /*mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };*/
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        // At this point, the user has already been prompted to either enter the URI
        // of a master to use or to start a master locally.
//        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
//        nodeConfiguration.setMasterUri(getMasterUri());
        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            Log.d("configuracao Master URI", getMasterUri().toString());
            Log.d("configuracao ROS HOSTN", getRosHostname());
            Log.d("configuracao ROS IP", EnvironmentVariables.ROS_IP);
            Log.d("configuracao ROS M URI", EnvironmentVariables.ROS_MASTER_URI);
            Log.d("configuracao ROS HNAME", EnvironmentVariables.ROS_ROOT);
            //Log.d("Node name", nodeConfiguration.getNodeName().toString());
            Log.e(TAG, "node configuration: " + nodeConfiguration);
            nodeMainExecutor.execute(talker, nodeConfiguration);
            nodeMainExecutor.execute(rosDjiCameraPreviewView, nodeConfiguration);
            nodeMainExecutor.execute(cmdVelListener, nodeConfiguration);
            Log.e("CMDVEL", " executed");

        }
        catch (IOException e) {
            Log.e("Camera Tutorial", "socket error trying to get networking information from the master uri");
        }



//        Log.e(TAG, "node configuration: " + nodeConfiguration);
//        nodeMainExecutor.execute(talker, nodeConfiguration);
//        nodeMainExecutor.execute(rosDjiCameraPreviewView, nodeConfiguration);
        //nodeMainExecutor.execute(rosTextView, nodeConfiguration);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
    }

    private void initFlightController() {
        talker = new Talker("position", MainActivity.this);
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }
        if (mFlightController != null) {
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            mFlightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        Log.e("STICK orientation", "Flight orientation set to aricraft heading");
                    }
                    else {
                        Log.e("STICK orientation", "error : " + djiError.getDescription());
                    }
                }
            });
//       rosDjiCameraPreviewView = new RosDjiCameraPreviewView(this.getApplicationContext());
            if (useSimulator) {
                mFlightController.getSimulator().start(InitializationData.createInstance(new LocationCoordinate2D(-12.97, -38.51), 10, 10), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Toast.makeText(MainActivity.this, djiError.getDescription(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Started Simulator", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            if (null == mSendVirtualStickDataTimer) {
                Log.e("STICK", "mSendVirtualStickDataTask created and scheduled");
                mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                mSendVirtualStickDataTimer = new Timer();
                mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
            }
        }
    }

    protected void onProductChange() {
    }
    private void initPreviewer() {
        product = ConnectionActivity.mProduct;
        if (product == null || !product.isConnected()) {
            Toast.makeText(this, getString(R.string.disconnected), Toast.LENGTH_SHORT).show();
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
//                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
                Log.d(TAG, "Drone model: " + product.getModel());
            }
        }
    }
    private void uninitPreviewer() {
        Camera camera = product.getCamera();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);

        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        initFlightController();
        onProductChange();
        DJIVideoStreamDecoder.getInstance().resume();
        /*if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }*/
    }


    class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (mFlightController != null) {
                Log.e("controller", "virtualStickControle available : " + mFlightController.isVirtualStickControlModeAvailable());
                Log.e("controller", "state: " + state);
                switch (state){
                    case VIRTUALSTICKSTART:
                        state = State.VIRTUALSTICKATTEMPTING;
                        mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null ){
                                    Log.e("STICK", "error setting virtual stick: " + djiError.getDescription());
                                    Toast.makeText(MainActivity.this, "error setting virtual stick: " + djiError.getDescription(), Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    Log.e("STICK", "Virtual stick set");
                                    state = State.VIRTUALSTICKCOMPLETE;
                                }
                            }
                        });
                    case VIRTUALSTICKATTEMPTING:
                        // just wait for it to finish
                        break;
                    case VIRTUALSTICKCOMPLETE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "It's ok to send cmds to the drone now", Toast.LENGTH_SHORT).show();
                            }
                        });
                        mSendVirtualStickDataTimer.cancel();

                }


            }
        }
    }

    @Override
    public void onPause() {
        DJIVideoStreamDecoder.getInstance().stop();
        super.onPause();
    }
    @Override
    public void onStop() {
        super.onStop();

    }
    public void onReturn(View view){
        this.finish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        DJIVideoStreamDecoder.getInstance().destroy();
        NativeHelper.getInstance().release();
    }
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }







    private void initUI() {
        // init mVideoSurface
//        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
        mTakeOffBtn = (Button) findViewById(R.id.btn_take_off);
        mLandBtn = (Button) findViewById(R.id.btn_land);
        mStickBtn = (Button) findViewById(R.id.btn_stick);
//        mScreenJoystickRight = (OnScreenJoystick)findViewById(R.id.directionJoystickRight);
//        mScreenJoystickLeft = (OnScreenJoystick)findViewById(R.id.directionJoystickLeft);
        mTextView = (TextView) findViewById(R.id.flightControllerData_tv);
        rosDjiCameraPreviewView = (RosDjiCameraPreviewView) findViewById(R.id.ros_dji_camera_preview_view);

/*
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }*/

        mTakeOffBtn.setOnClickListener(this);
        mLandBtn.setOnClickListener(this);
        mStickBtn.setOnClickListener(this);
        /*mScreenJoystickLeft.setJoystickListener(new OnScreenJoystickListener(){

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }

                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float pitchJoyControlMaxSpeed = 10;
                float rollJoyControlMaxSpeed = 10;

                mPitch = (float)(pitchJoyControlMaxSpeed * pX);

                mRoll = (float)(rollJoyControlMaxSpeed * pY);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }

            }

        });

        mScreenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }

                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float verticalJoyControlMaxSpeed = 2;
                float yawJoyControlMaxSpeed = 30;

                mYaw = (float)(yawJoyControlMaxSpeed * pX);
                mThrottle = (float)(verticalJoyControlMaxSpeed * pY);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                }

            }
        });*/
        /*mScreenJoystickLeft.setJoystickListener(new OnScreenJoystickListener(){

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
            }
        });

        mScreenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
            }
        });*/
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_take_off:
                Toast.makeText(this, "Take off pressed", Toast.LENGTH_SHORT).show();
                if (mFlightController != null){
                    Toast.makeText(this, "Flight controller not null", Toast.LENGTH_SHORT).show();
                    mFlightController.startTakeoff(
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        Toast.makeText(MainActivity.this, djiError.getDescription(), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Takeoff success", Toast.LENGTH_SHORT).show();
                                        state = State.TAKEOFFCOMPLETE;
                                    }
                                }
                            }
                    );
                }
                break;
            case R.id.btn_land:
                Toast.makeText(this, "Land pressed", Toast.LENGTH_SHORT).show();
                if (mFlightController != null){
                    mFlightController.startLanding(
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        Toast.makeText(MainActivity.this, djiError.getDescription(), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Start Landing", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                    );
                }
                break;
            case R.id.btn_stick:
                Toast.makeText(this, "Stick Button", Toast.LENGTH_SHORT).show();
                mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if(djiError != null){
                            Log.e("controller", "Error setting virtualStick: " + djiError.getDescription());
                        }
                        else {
                            Log.e("controller", "VirtualStickMode Enabled");
                            Toast.makeText(MainActivity.this, "VirtualStickMode Enabled", Toast.LENGTH_SHORT).show();
                            state = State.VIRTUALSTICKCOMPLETE;
                        }
                    }
                });
                break;
            default:
                break;
        }
    }

}
