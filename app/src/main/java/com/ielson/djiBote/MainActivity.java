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
    //private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    public final static boolean useSimulator = false;
    public static final Object TAKEOFFSTART = 1;
    public static final Object TAKEOFFCOMPLETE = 2;
    public static final Object TAKEOFFATTEMPTIG = 3;
    public static final Object VIRTUALSTICKSTART = 4;
    public static final Object VIRTUALSTICKATTEMPTING = 5;
    public static final Object VIRTUALSTICKCOMPLETE = 6;



    public enum State  {TAKEOFFSTART, TAKEOFFATTEMPTIG, TAKEOFFCOMPLETE, VIRTUALSTICKSTART, VIRTUALSTICKATTEMPTING, VIRTUALSTICKCOMPLETE;}
    public static State state = State.TAKEOFFSTART;

    protected DJICodecManager mCodecManager = null;

    // talker is the node that senses and sends the aircraft info, as position and others
    //private Talker talker;
    // RosDjiCameraPreviewView now is just setting the rawImageListener
    //private RosDjiCameraPreviewView rosDjiCameraPreviewView;
    // cmdVelListener shoudl listen for the messages sent by controller and make the drone fly
    //private CmdVelListener cmdVelListener;


    // Foi chamada depos de apertar no connectDroneButton, o que faz é:
    // 1-Chama a atividade do rosJava, com o nome DJIBote
       // 9(talvez2)-seta a tela para a da atividade principal (já era pra ser aqui? porque nao depois que o ros tiver conectado?) 10-cria cmdVelListener
    // 11-inicializa os botoes e seta os onClickListeners para ver qual o botao apertado e o que vai fazer
    // 12-Se tiver alteracao do produto, ele chama o initFlightController 
    // 13-cria um talker (por que aqui? - isso tem que ser executado antes do passo 6 entao)
    // 14-faz configuracoes do controlador de voo 15-Testa se é pra usar o simulador e se sim, configura pra ele
    // 16-Se ainda nao tiver, cria um timer para mandar os controles do virtualStick (controles na tela do drone)
    // 17-Se tiver mudanca no produto: 17a-Vê se o produto agora é nulo e escreve na tela que desconectou
    // 17b-Se nao for nulo, entao coemcou algum drone, verifica a mVideoSurface e setSurfaceTextureListener
    // As debaixo daqui só são chamadas depois que a tela do ROS der OK
    // 2-Chama o init 3-Init cria um socket com as configs escolhidas na tela do ros 4-cria um nodeConfiguration com o hostAddress do nó, e o MasterUri()
    // 6- inicializa as configs de um no para o talker (outro arquivo que manda msgs) 7-inicializa outro para rosDjiCameraPreviewView 8 - inicializa mais um para cmdVelListener

    //
    
    public MainActivity() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("DJIBote", "Communication DJI-ROS");
        // If you know the IP/Port of the ROS Master, you can set it as follows and avoid having the Master Chooser activity:
        //super("DJI-Ros Driver Activity", "DJI-Ros Driver Activity", URI.create("http://10.42.0.1:11311")
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("FLOW main", "MainActivity Started");
        super.onCreate(savedInstanceState);

        // it happens even before ROS has run
        //cmdVelListener = new CmdVelListener(MainActivity.this); // porque aqui?
        //Log.d("FLOW main", "CmdVelListener created");
        setContentView(R.layout.activity_main);
        initUI();
        // The callback for receiving the raw H264 video data for camera live view
        /* mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        }; */
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        // At this point, the user has already been prompted to either enter the URI
        // of a master to use or to start a master locally.
//        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
//        nodeConfiguration.setMasterUri(getMasterUri());
        try {
            // aqui a conexao não já foi feita na tela principal do ROS? porque preciso usar os sockets?
            Log.d("FLOW main", "On Init Method");
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress(); // ver o que retorna nessa funcao
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            Log.d("configuracao Master URI", getMasterUri().toString());
            Log.d("configuracao ROS HOSTN", getRosHostname());
            Log.d("configuracao ROS IP", EnvironmentVariables.ROS_IP);
            Log.d("configuracao ROS M URI", EnvironmentVariables.ROS_MASTER_URI);
            Log.d("configuracao ROS HNAME", EnvironmentVariables.ROS_ROOT);
            //Log.d("Node name", nodeConfiguration.getNodeName().toString());
            Log.d("FLOW main", "node configuration done, with this parameters: " + nodeConfiguration);
            //nodeMainExecutor.execute(talker, nodeConfiguration); // podem todos os bis terem a mesma config?
            //nodeMainExecutor.execute(rosDjiCameraPreviewView, nodeConfiguration);
            //nodeMainExecutor.execute(cmdVelListener, nodeConfiguration);
            Log.d("FLOW main", "1 nodes executed");

            product = ConnectionActivity.mProduct;
            //initPreviewer(); // Precisa pra mostrar a camera
            //Log.d("FLOW main", "Previewer Init");
            initFlightController();
            Log.d("FLOW main", "Flight Controller Init");
            //onProductChange(); // por que??????? vou tirar daqui a pouco
            //DJIVideoStreamDecoder.getInstance().resume();
            //Log.d("FLOW main", "VideoStreamDecoder resumed");
            mFlightController.setStateCallback(new FlightControllerState.Callback(){
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState){
                    Log.d("FLOW main", "onMFlightController Update");
                    droneController.updateSensorValues(flightControllerState);
                }

            });

        }
        catch (IOException e) {
            // ta errado a tag
            // porque so pego esse erro?
            Log.d("FLOW main", "Socket error in rosjava");
            Log.e(TAG, "Socket error trying to get networking information from the master uri");
        }


//        Log.e(TAG, "node configuration: " + nodeConfiguration);
//        nodeMainExecutor.execute(talker, nodeConfiguration);
//        nodeMainExecutor.execute(rosDjiCameraPreviewView, nodeConfiguration);
        //nodeMainExecutor.execute(rosTextView, nodeConfiguration);
    }

    // por que preciso dos broadcast receivers aqui? Provavelmente pra receber o notifyStatusChange da connection Activity
    /*protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("FLOW main", "Broadcast Received");
            onProductConnectionChange();
        }
    };*/

    private void onProductConnectionChange()
    {
        //assim ele vai tentar iniciar o flightController mesmo se tiver sido uma desconexao, nao?
        Log.d("FLOW main", "onProductConnectionChange");
        initFlightController();
    }

    private void initFlightController() {
        Log.d("FLOW main", "init Flight Controller");
        //talker = new Talker("position", MainActivity.this);
        //Log.d("Flow main", "Talker created");
        Log.d("FLOW debug", "product: " +product + " connected? " +product.isConnected());
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
                Log.d("FLOW main", "got product flight controller");
            }
            // colocar mensagem de erro se nao for, dizer qual o erro e tal 
        }
        if (mFlightController != null) {
            Log.d("FLOW main", "setting flight controller modes");
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            mFlightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        Log.d("FLOW main", "Flight orientation set to aircraft heading");
                    }
                    else {
                        Log.e("STICK orientation", "error : " + djiError.getDescription());
                    }
                }
            });
//       rosDjiCameraPreviewView = new RosDjiCameraPreviewView(this.getApplicationContext());
            /*if (useSimulator) {
                Log.d("Flow main", "using simulator");
                mFlightController.getSimulator().start(InitializationData.createInstance(new LocationCoordinate2D(-12.97, -38.51), 10, 10), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Toast.makeText(MainActivity.this, djiError.getDescription(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error starting simulator: " + djiError.getDescription());
                        } else {
                            Log.d("FLOW main", "Simulator created");
                            Toast.makeText(MainActivity.this, "Started Simulator", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }*/
            // cria um timer para mandar comandos para o virtualStickData(botoes de controle na tela) de tempos em tempos
            /*if (null == mSendVirtualStickDataTimer) {

                mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                mSendVirtualStickDataTimer = new Timer();

                // tirando os envios do VirtualStick por enquanto
                //mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                Log.d("FLOW main", "Created mSendVirtualDataTimer");
            }*/
        }
    }

    // pelo que eu entendi vai usar o da outra activity
    protected void onProductChange() {
        Log.d("FLOW main", "onProductChange");
    }
    // por quem que esse initPreviewer e uninit são chamados?
    private void initPreviewer() {
        // faz a camera aparecer na tela do cel
        Log.d("FLOW main", "onInitPreviewer");
        if (product == null || !product.isConnected()) {
            Toast.makeText(this, getString(R.string.disconnected), Toast.LENGTH_SHORT).show(); // por que faz  esse teste aqui?
        } else {
            // se existir uma mVideoSurface, ela comeca a escutar por textures chegando
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this); //
                Log.d("FLOW main", "mVIdeoSurface surfaceTextureListener set"); //nao foi chamada pelo que eu vi
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
//                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
                Log.d(TAG, "Drone model: " + product.getModel()); // nao preciso disso aqui
            }
        }
    }
    private void uninitPreviewer() {
        // se nao precisar do init nao vai precisar do uninit tambem, tenho que testar
        Camera camera = product.getCamera();
        if (camera != null){
            // Reset the callback
            Log.d("FLOW main", "Resetting VideoFeed Callback"); // nao sei onde foi setado
            VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);

        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    /*class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            Log.d("FLOW main", "onSendVirtualStickDataTask");
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
                                    Log.d("Flow main", "Virtual stick set");
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
                                Log.d("FLOW main", "Virtual Stick Complete");
                                Toast.makeText(MainActivity.this, "It's ok to send cmds to the drone now", Toast.LENGTH_SHORT).show();
                            }
                        });
                        mSendVirtualStickDataTimer.cancel(); // por que cancela????????

                }


            }
        }
    }*/

    @Override
    public void onPause() {
        DJIVideoStreamDecoder.getInstance().stop();
        Log.d("FLOW main", "onPause");
        super.onPause();
    }
    @Override
    public void onStop() {
        Log.d("FLOW main", "onStop");
        super.onStop();

    }
    public void onReturn(View view){
        Log.d("FLOW main", "onReturn");
        this.finish(); // por que????
    }
    @Override
    protected void onDestroy() {
        Log.d("FLOW main", "onDestroy");
        super.onDestroy();
        DJIVideoStreamDecoder.getInstance().destroy();
        NativeHelper.getInstance().release();
    }
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d("FLOW main", "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d("FLOW main", "onSurfaceTextureSizeChanged");
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d("FLOW main","onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.d("FLOW main", "onSurfaceTextureUpdated");
    }







    private void initUI() {
        Log.d("FLOW main", "onInitUI");
        // init mVideoSurface
//        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
        mTakeOffBtn = (Button) findViewById(R.id.btn_take_off);
        mLandBtn = (Button) findViewById(R.id.btn_land);
        mStickBtn = (Button) findViewById(R.id.btn_stick);
//        mScreenJoystickRight = (OnScreenJoystick)findViewById(R.id.directionJoystickRight);
//        mScreenJoystickLeft = (OnScreenJoystick)findViewById(R.id.directionJoystickLeft);
        // mTextView is the button of the flightControllerData_tv name from here
        mTextView = (TextView) findViewById(R.id.flightControllerData_tv);
        //rosDjiCameraPreviewView = (RosDjiCameraPreviewView) findViewById(R.id.ros_dji_camera_preview_view);
        Log.d("FLOW main", "buttons found");

/*
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }*/

        mTakeOffBtn.setOnClickListener(this); // nao lembro bem o que setar para this faz.
        mLandBtn.setOnClickListener(this);
        mStickBtn.setOnClickListener(this);
        Log.d("FLOW main", "onClickListeners for mTakeOff, mLan and mStickBtn set");
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
        Log.d("FLOW main", "some button pressed");
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
                                        Log.d("FLOW main", "Takeoff success");
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
                                        Toast.makeText(MainActivity.this, "Landing Complete", Toast.LENGTH_SHORT).show();
                                        Log.d("FLOW main", "Landing Complete");
                                    }
                                }
                            }
                    );
                }
                break;
            /*case R.id.btn_stick:
                Toast.makeText(this, "Stick Button", Toast.LENGTH_SHORT).show();
                Log.d("FLOW main", "Setting virtual stick mode");
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
                break;*/
            default:
                break;
        }
    }

}
