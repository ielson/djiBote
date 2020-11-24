package com.ielson.djiBote;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import net.sqlcipher.IContentObserver;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.util.CommonCallbacks;
import dji.midware.data.model.P3.DataFlycGetPushFlightRecord;
import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.Quaternion;
import std_msgs.Int32;
import sensor_msgs.Image;
import std_msgs.Header;


public class Talker extends AbstractNodeMain {
    private String topic_name;
    private ConnectedNode connectedNode;
    private Context context;
    Publisher<geometry_msgs.Point> posPublisher;
    Publisher<geometry_msgs.Point> rpyPublisher;
    Publisher<geometry_msgs.Point> velsPublisher;
    Publisher<std_msgs.Int32> headingPublisher;
    Publisher<std_msgs.Int32> flightTimePublisher;
    Publisher<std_msgs.Int32> goHomePublisher;

    // message type vai ter que estar aqui tambem
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

    public Talker(Context context) {
        topic_name = "chatter";
        this.context = context;
    }

    public Talker(String topic, Context context)
    {
        topic_name = topic;
        this.context = context;

    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("djiBote/SensorInfo");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        /*
        final Publisher<std_msgs.String> publisher =
                connectedNode.newPublisher(topic_name, std_msgs.String._TYPE);
        */
        this.connectedNode = connectedNode;
        NameResolver resolver = connectedNode.getResolver().newChild("sensorInfo");
        posPublisher = connectedNode.newPublisher(resolver.resolve("pose/position"), Point._TYPE);
        rpyPublisher = connectedNode.newPublisher(resolver.resolve("pose/orientation/rpy"), Point._TYPE);
        velsPublisher = connectedNode.newPublisher(resolver.resolve("twist/linear"), Point._TYPE);
        headingPublisher = connectedNode.newPublisher(resolver.resolve("twist/heading"), Int32._TYPE);
        flightTimePublisher = connectedNode.newPublisher(resolver.resolve("flightTimeRemaining"), Int32._TYPE);
        goHomePublisher = connectedNode.newPublisher(resolver.resolve("goHomePublisher"), Int32._TYPE);

        Log.d("Talker", "publishers created");
        /*if (!MainActivity.useSimulator) {
            MainActivity.mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull final FlightControllerState flightControllerState) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("Talker", "Updating var status");

                            if (!flightControllerState.isFlying()){
                                Log.e("STICK", "isn't flying");
                            }
                            else {
                                if (flightControllerState.getFlightMode() == FlightMode.AUTO_TAKEOFF) {
                                    Log.e("STICK", "Taking off");
                                } else {
                                    MainActivity.state = MainActivity.State.VIRTUALSTICKSTART;
                                }
                            }
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


                            MainActivity.mTextView.setText("Yaw : " + String.format("%.2f", yaw) + ", Pitch : " + String.format("%.2f", pitch) + ", Roll : " + String.format("%.2f", roll) + "\n" +
                                    ", PosX : " + String.format("%.2f", positionX) + ", PosY : " + String.format("%.2f", positionY) + ", PosZ : " + String.format("%.2f", positionZ));
                            Point pos = posPublisher.newMessage();
                            pos.setX(positionX);
                            pos.setY(positionY);
                            pos.setZ(positionZ);
                            posPublisher.publish(pos);
                            Log.d("Talker", "pos msg published");

                            Point rpy = rpyPublisher.newMessage();
                            rpy.setX(yaw);
                            rpy.setY(pitch);
                            rpy.setZ(roll);
                            rpyPublisher.publish(rpy);
                            Log.d("Talker", "rpy msg published");

                            Point vels = velsPublisher.newMessage();
                            vels.setX(xVelocity);
                            vels.setY(yVelocity);
                            vels.setZ(zVelocity);
                            velsPublisher.publish(vels);
                            Log.d("Talker", "vels msg published");

                            Int32 headMsg = headingPublisher.newMessage();
                            headMsg.setData(headDirection);
                            headingPublisher.publish(headMsg);
                            Log.d("Talker", "heading msg published");

                            Int32 flightTimeMsg = flightTimePublisher.newMessage();
                            flightTimeMsg.setData(flightTime);
                            flightTimePublisher.publish(flightTimeMsg);
                            Log.d("Talker", "flight time msg published");

                            Int32 goHomeHeightMsg = goHomePublisher.newMessage();
                            goHomeHeightMsg.setData(goHomeHeight);
                            goHomePublisher.publish(goHomeHeightMsg);
                            Log.d("Talker", "goHome msg published");


                        }
                    });
                }
            });

        }
        else {
            MainActivity.mFlightController.getSimulator().setStateCallback(new SimulatorState.Callback() {
                @Override
                public void onUpdate(final SimulatorState simulatorState) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (!simulatorState.isFlying()){
                                Log.e("STICK", "simulator isn't flying");
                            }
                            else {
                                MainActivity.state = MainActivity.State.VIRTUALSTICKSTART;
                            }
                            Log.d("Talker", "Updating var status");
                            yaw = simulatorState.getYaw();
                            pitch = simulatorState.getPitch();
                            roll = simulatorState.getRoll();
                            positionX = simulatorState.getPositionX();
                            positionY = simulatorState.getPositionY();
                            positionZ = simulatorState.getPositionZ();

                           MainActivity.mTextView.setText("Yaw : " + String.format("%.2f", yaw) + ", Pitch : " + String.format("%.2f", pitch) + ", Roll : " + String.format("%.2f", roll) + "\n" +
                                    ", PosX : " + String.format("%.2f", positionX) + ", PosY : " + String.format("%.2f", positionY) + ", PosZ : " + String.format("%.2f", positionZ));
                            Point pos = posPublisher.newMessage();
                            pos.setX(positionX);
                            pos.setY(positionY);
                            pos.setZ(positionZ);
                            posPublisher.publish(pos);
                            Log.d("Talker", "pos msg published");

                            Point rpy = rpyPublisher.newMessage();
                            rpy.setX(yaw);
                            rpy.setY(pitch);
                            rpy.setZ(roll);
                            rpyPublisher.publish(rpy);
                            Log.d("Talker", "rpy msg published");

                        }
                    });
                }
            });
        }*/
        MainActivity.mFlightController.setStateCallback(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                Log.e("controller", "received controller state callback");
                Log.e("controller", " altitude: " + flightControllerState.getAircraftLocation().getAltitude());
                Log.e("controller", "is flying? "+ flightControllerState.isFlying());
                Log.e("controller", "is still in take off mode? " + flightControllerState.getFlightMode());
                Log.e("controller", "isVirtualStick Available? " + MainActivity.mFlightController.isVirtualStickControlModeAvailable());

               updateSensorValues(flightControllerState);


                if (MainActivity.state == MainActivity.State.TAKEOFFCOMPLETE){
                    if (!flightControllerState.isFlying()){
                        Log.e("controller", "drone not flying");
                    }
                    else {
                        if (flightControllerState.getFlightMode() == FlightMode.AUTO_TAKEOFF){
                            Log.e("controller", "still taking off");
                        }
                        else {
                            Log.e("controller", "took off");
                            MainActivity.state = MainActivity.State.VIRTUALSTICKSTART;
                        }
                    }
                }
                }
            });
        MainActivity.mFlightController.getSimulator().setStateCallback(new SimulatorState.Callback() {
            @Override
            public void onUpdate(@NonNull SimulatorState simulatorState) {
                Log.e("controller", "received simulator state callback");
                Log.e("controller", "some values: altitude: " + simulatorState.getPositionZ());

            }
        });
    }

    public void updateSensorValues(FlightControllerState flightControllerState){
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


        Point pos = posPublisher.newMessage();
        pos.setX(positionX);
        pos.setY(positionY);
        pos.setZ(positionZ);
        posPublisher.publish(pos);
        Log.d("Talker", "pos msg published");

        Point rpy = rpyPublisher.newMessage();
        rpy.setX(yaw);
        rpy.setY(pitch);
        rpy.setZ(roll);
        rpyPublisher.publish(rpy);
        Log.d("Talker", "rpy msg published");

        Point vels = velsPublisher.newMessage();
        vels.setX(xVelocity);
        vels.setY(yVelocity);
        vels.setZ(zVelocity);
        velsPublisher.publish(vels);
        Log.d("Talker", "vels msg published");

        Int32 headMsg = headingPublisher.newMessage();
        headMsg.setData(headDirection);
        headingPublisher.publish(headMsg);
        Log.d("Talker", "heading msg published");

        Int32 flightTimeMsg = flightTimePublisher.newMessage();
        flightTimeMsg.setData(flightTime);
        flightTimePublisher.publish(flightTimeMsg);
        Log.d("Talker", "flight time msg published");

        Int32 goHomeHeightMsg = goHomePublisher.newMessage();
        goHomeHeightMsg.setData(goHomeHeight);
        goHomePublisher.publish(goHomeHeightMsg);
        Log.d("Talker", "goHome msg published");

    }
}