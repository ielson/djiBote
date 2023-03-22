package com.ielson.djiBote;

import android.Manifest;
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

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;


// Done following this version of dji tutorials
// https://web.archive.org/web/20171025211037/https://developer.dji.com/mobile-sdk/documentation/application-development-workflow/workflow-integrate.html
// Primeira atividade a ser chamada, basicamente o que faz é:
// 1-verifica se as permissões estão concedidadas (#TODO corrigir app fechado quando a permissao ainda nao foi dada)
// 2-inicializa UI da tela de conexao 3-inicializa o handler (reler) 4-faz o registro do app na SDK da DJI 4.1 DJISDKManager.getInstance().startConnectionToProduct();
// 5-espera conexao do produto na onProductChange 6-espera por alteracoes nos componentes ou conexao no mDJIBaseProductListener
// 7-chama o notifyStatusChange (tenho que ver o que faz) 8-cria o updateRunnable que manda um broadcast de intent de que a conexao foi alterada.
// 9-nao sei como o connectDrone ta sendo chamado (acho que é o callback do botao) 10 - ele salva em connectionResult se deu certo a conexao, e nao faz nada com ela 
// 11 - chama a MainActivicity.class

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
        Toast.makeText(getApplicationContext(), "Starting Connection", Toast.LENGTH_SHORT).show();
        connectionResult = DJISDKManager.getInstance().startConnectionToProduct(); //tenho que ver se faz a conexao, ou se comeca a mandar os dados

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
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

        setContentView(R.layout.activity_connection);
        registerProgressBar = (ProgressBar) findViewById(R.id.registerProgressBar);
        registerTextView = (TextView) findViewById(R.id.registerTextView);

        //Initialize DJI SDK Manager
        mHandler = new Handler(Looper.getMainLooper());
        DJISDKManager.getInstance().registerApp(this, mDJISDKManagerCallback);
    }

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(final DJIError error) {
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct(); // ver o que ta fazendo aqui, quem ele chama, pra onde vai depois daqui
                                                                        // se nao chamar em mais nenhum lugar, ele so reconhece o drone se já tiver 
                                                                        // conectado na wifi na hora que fizer o teste do registro.
                //handler = new Handler(Looper.getMainLooper());
                // handler is needed so the UI is updated at the right thread
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register Success", Toast.LENGTH_LONG).show();
                        registerProgressBar.setVisibility(View.GONE);
                        registerTextView.setText("Successful registration"); // era pra habilitar o botao de connect só agora, né nao?
                                                                            // na verdade é depois de reconhecer o drone, mas aqui precisava mudar algo?
                    }
                });
            } else {
                //handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "register sdk failed, check if network is available", Toast.LENGTH_LONG).show();
                        registerTextView.setText("Could not register, error found: " + error.toString());
                    }
                });

            }
            Log.e("TAG", error.toString()); // Esse log aqui ta meio sem sentido 
        }

        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            modelTextView = (TextView) findViewById(R.id.modelTextView);
            mProduct = DJISDKManager.getInstance().getProduct();
            if (mProduct != null && mProduct.getModel() != null) {
                // se algum produto conectado:
                // deveria verificar modelo do drone
                // so liberar o botao de conexao se for o spark 
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "ProductChange", Toast.LENGTH_SHORT).show();
                        modelTextView.setText(mProduct.getModel().getDisplayName() + " is connected"); // escreve no textView qual o drone que conectou
                        Toast.makeText(getApplicationContext(), "Connection succeeded: "+ connectionResult, Toast.LENGTH_SHORT).show(); // nao faz muito sentido os dois toasts
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
                mProduct.setBaseProductListener(mDJIBaseProductListener); // espera por mudancas no componente ou conectividade. Componente sao as placas do drone?
            }

            notifyStatusChange();
        }
    };

    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener); // tenho que ver o que o component listener faz
                Toast.makeText(getApplicationContext(), "Component Changed", Toast.LENGTH_LONG).show();
            }
            notifyStatusChange();
        }

        @Override
        public void onConnectivityChange(boolean isConnected) {

            Toast.makeText(getApplicationContext(), "Connectivity Changed", Toast.LENGTH_SHORT).show(); // seria o certo tratar alguma coisa aqui sobre uma alteracao na conectividade?
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
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };

}