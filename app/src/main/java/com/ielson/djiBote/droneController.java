package com.ielson.djiBote;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.util.CommonCallbacks;
import dji.midware.data.model.P3.DataFlycGetPushFlightRecord;


public class droneController {

    public static double positionX;
    public static double positionY;
    public static double positionZ;
    public static double roll;
    public static double pitch;
    public static double yaw;
    public static int headDirection;
    public static int flightTime;
    public static int goHomeHeight;
    public static double xVelocity;
    public static double yVelocity;
    public static double zVelocity;





        /*if (MainActivity.state == MainActivity.State.TAKEOFFCOMPLETE) {
            if (!flightControllerState.isFlying()) {
                Log.d("controller", "drone not flying");
            } else {
                if (flightControllerState.getFlightMode() == FlightMode.AUTO_TAKEOFF) {
                    Log.d("controller", "still taking off");
                } else {
                    Log.d("controller", "took off");
                    MainActivity.state = MainActivity.State.VIRTUALSTICKSTART;
                }
            }
        }*/

    /*MainActivity.mFlightController.getSimulator().setStateCallback(new SimulatorState.Callback() {
        @Override
        public void onUpdate(@NonNull SimulatorState simulatorState) {
            Log.d("controller simulator", "received simulator state callback");
            Log.d("controller simulator", "some values: altitude: " + simulatorState.getPositionZ());

        }
    });*/


    // gets all new values at the same time and send them to the screen
    public static void updateSensorValues(FlightControllerState flightControllerState) {

        Log.d("FLOW talker", "onUpdateSensorValues");
        yaw = flightControllerState.getAttitude().yaw;
        pitch = flightControllerState.getAttitude().pitch;
        roll = flightControllerState.getAttitude().roll;
        positionX = flightControllerState.getAircraftLocation().getLatitude();
        positionY = flightControllerState.getAircraftLocation().getLongitude();
        positionZ = flightControllerState.getAircraftLocation().getAltitude();


        headDirection = flightControllerState.getAircraftHeadDirection();
        flightTime = flightControllerState.getFlightTimeInSeconds();
        goHomeHeight = flightControllerState.getGoHomeHeight();
        xVelocity = flightControllerState.getVelocityX();
        yVelocity = flightControllerState.getVelocityY();
        zVelocity = flightControllerState.getVelocityZ();

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                MainActivity.mTextView.setText("xvel : " + String.format("%.2f", xVelocity) + ", yvel : " + String.format("%.2f", yVelocity) + ", zvel : " + String.format("%.2f", zVelocity) + "\n" +
                        ", PosX : " + String.format("%.2f", positionX) + ", PosY : " + String.format("%.2f", positionY) + ", PosZ : " + String.format("%.2f", positionZ));
            }
        });
    }
}
