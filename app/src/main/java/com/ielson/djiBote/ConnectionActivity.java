package com.ielson.djiBote;

import static android.app.PendingIntent.getActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;

import java.util.List;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;


// Done following this version of dji tutorials
// https://web.archive.org/web/20171025211037/https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-integrate.html
// Primeira atividade a ser chamada, basicamente o que faz é:
// 1-verifica se as permissões estão concedidadas (#TODO corrigir app fechado quando a permissao ainda nao foi dada)
// 2-inicializa UI da tela de conexao 3-inicializa o handler 4-acontece productChange 5-NotifYStatusChange é chamada 6-faz o registro do app na SDK da DJI
// 7-tenta conectar com o drone com o startConnectionToProduct(); 8-espera conexao do produto na onProductChange
// 9-espera por alteracoes nos em conexao ou produtos com o BaseProductListener 10-chama o notifyStatusChange
// 11-cria o updateRunnable que manda um broadcast de intent de que a conexao foi alterada. 12-ConnectButtonPressed 13-startConnectionToProduct de novo
// 14 - Outro productChange 15-OutroBaseProductListenerSet 16-Outro notifyStatusChange
// 17 - chama a MainActivicity.class
// 18-Se mesmo em outra tela desconectar: Connectivity changed, e várias vezes de: notifyStatusChange,ComponentChange. (deve ser uma pra cada componente)

public class ConnectionActivity extends AppCompatActivity {

    private static final String TAG = ConnectionActivity.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    public static BaseProduct mProduct;
    private Handler mHandler;
    boolean connectionResult;

    ProgressBar registerProgressBar;
    TextView registerTextView;
    Button connectButton;
    TextView modelTextView;
    Handler handler = new Handler(Looper.getMainLooper());




    public void connectDrone(View view){
        // TODO mudar o nome do botao, pois o drone já tá conectado.
        // TODO o botão só pode ficar clicável depois da conexão, não tá assim
        Log.d("FLOW", "Connect Button Pressed");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        Log.d("FLOW", "onCreate");
        // TODO colocar um try aqui e fazer algo se a permissao nao for concedida
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }
        Log.d("FLOW", "Permissions answered");

        setContentView(R.layout.activity_connection);
        Log.d("FLOW", "connectionActivity set");
        connectButton = (Button) findViewById(R.id.connectButton);
        registerProgressBar = (ProgressBar) findViewById(R.id.registerProgressBar);
        registerTextView = (TextView) findViewById(R.id.registerTextView);
        connectButton.setEnabled(false);

        mHandler = new Handler(Looper.getMainLooper());
        Log.d("FLOW", "Handler started");
        DJISDKManager.getInstance().registerApp(this, mDJISDKManagerCallback);
    }

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(final DJIError error) {
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct(); // This method should be called after successful registration of the app and
                                                                        // once there is a data connection between the mobile device and DJI product.
                                                                        // This data connection is either a USB cable connection, a WiFi connection (that needs to be established outside of the SDK)
                                                                        // or a Bluetooth connection (that needs to be established with getBluetoothProductConnector).
                                                                        // If the connection succeeds, onProductConnect will be called if the connection succeeded.
                                                                        // Returns true if the connection is started successfully.
                //handler = new Handler(Looper.getMainLooper());
                // handler is needed so the UI is updated at the right thread
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register Success", Toast.LENGTH_LONG).show();
                        registerProgressBar.setVisibility(View.GONE);
                        registerTextView.setText("Successful registration");
                        Log.d("FLOW", "Registered to DJISDK");
                    }
                });
            } else {
                //handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "register sdk failed, check if network is available", Toast.LENGTH_LONG).show();
                        registerTextView.setText("Could not register, error found: " + error.toString());
                        Log.e(TAG, "Could not register, error found: " + error.toString());
                    }
                });

            }
        }


        public void onProductConnect(BaseProduct product){
            // ISSO AQUI NÃO TÁ SENDO CHAMADO NUNCA
            Log.d("FLOW", "Product Connected");
            modelTextView = (TextView) findViewById(R.id.modelTextView);
            connectButton.setEnabled(true);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Connection succeeded: "+ connectionResult, Toast.LENGTH_SHORT).show();
                    modelTextView.setText(mProduct.getModel().getDisplayName() + " is connected"); // escreve no textView qual o drone que conectou

                }
            });
        }

        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            Log.d("FLOW", "Product Change");

            mProduct = DJISDKManager.getInstance().getProduct();
            modelTextView = (TextView) findViewById(R.id.modelTextView);
            if (mProduct != null && mProduct.getModel() != null) {
                // se algum produto conectado:
                // deveria verificar modelo do drone
                // so liberar o botao de conexao se for o spark
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectButton.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "ProductChange", Toast.LENGTH_SHORT).show();
                        modelTextView.setText(mProduct.getModel().getDisplayName() + " is connected"); // escreve no textView qual o drone que conectou

                    }
                });
            } else {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        modelTextView.setText("No model connected");
                    }
                });
            }

            // independente de ter conectado ou desconectado vai chamar essa parte
            mProduct = newProduct;
            // porque nao colocar esse listener dentro do if do produto conectado?
            if(mProduct != null) {
                mProduct.setBaseProductListener(mDJIBaseProductListener); // Receives notifications of component and product connectivity changes.
                                                                        // Interface Methods: onConnectivityChange; onComponentChange
                                                                        // A component can be a camera, gimbal, remote controller, etc. A DJI product consists of several components. All components of a product are subclasses of BaseComponent and can be accessed directly from the product objects (Aircraft or HandHeld).
                Log.d("FLOW", "BaseProductListener set");
            }

            notifyStatusChange();
        }
    };

    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            Log.d("FLOW", "Component Change");
            if(newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener); // entrou uma novo drone, camera, gimbal ou algo do tipo vamos esperar por alteracoes dele
                Toast.makeText(getApplicationContext(), "Component Changed", Toast.LENGTH_LONG).show();
            }
            notifyStatusChange();
        }

        @Override
        public void onConnectivityChange(boolean isConnected) {
            Log.d("FLOW", "Connectivity Changed");
            Toast.makeText(getApplicationContext(), "Connectivity Changed", Toast.LENGTH_SHORT).show(); // alteracao de conectividade de algum produto ou componente (camera, gimbal, controle remoto)
            notifyStatusChange();
        }

    };

    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }

    };

    private void notifyStatusChange() { // tenho que entender o que ela faz direito.
        Log.d("FLOW", "on Notify Status Change");
        mHandler.removeCallbacks(updateRunnable); // Remove any pending posts of Runnable r that are in the message queue.
        mHandler.postDelayed(updateRunnable, 500); // Causes the Runnable r to be added to the message queue, to be run after the specified amount of time elapses.

    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE); // This Intent is then broadcast to all receivers that have registered to receive broadcasts with this action.
            sendBroadcast(intent);

        }
    };

}