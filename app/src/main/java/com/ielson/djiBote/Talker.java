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


    public Talker(Context context) {
        topic_name = "chatter";
        this.context = context;
        Log.d("FLOW talker", "talker"+topic_name);
    }

    public Talker(String topic, Context context)
    {
        topic_name = topic;
        this.context = context;
        Log.d("FLOW talker", "talker"+topic_name);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("djiBote/SensorInfo");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        Log.d("FLOW talker", "onStat");
        this.connectedNode = connectedNode;
        NameResolver resolver = connectedNode.getResolver().newChild("sensorInfo");
        posPublisher = connectedNode.newPublisher(resolver.resolve("pose/position"), Point._TYPE);
        rpyPublisher = connectedNode.newPublisher(resolver.resolve("pose/orientation/rpy"), Point._TYPE);
        velsPublisher = connectedNode.newPublisher(resolver.resolve("twist/linear"), Point._TYPE);
        headingPublisher = connectedNode.newPublisher(resolver.resolve("twist/heading"), Int32._TYPE);
        flightTimePublisher = connectedNode.newPublisher(resolver.resolve("flightTimeRemaining"), Int32._TYPE);
        goHomePublisher = connectedNode.newPublisher(resolver.resolve("goHomePublisher"), Int32._TYPE);

        Log.d("FLOW talker", "publishers created");

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void loop() throws InterruptedException {
                Log.d("FLOW talker", "inside Cancellable Loop");
                Point rpy = rpyPublisher.newMessage();
                rpy.setX(droneController.yaw);
                rpy.setY(droneController.pitch);
                rpy.setZ(droneController.roll);
                rpyPublisher.publish(rpy);
                Log.d("talker", "rpy msg published Yaw : " + String.format("%.2f", droneController.yaw) + ", Pitch : " + String.format("%.2f", droneController.pitch) + ", Roll: " + String.format("%.2f", droneController.roll));

                Point pos = posPublisher.newMessage();
                pos.setX(droneController.positionX);
                pos.setY(droneController.positionY);
                pos.setZ(droneController.positionZ);
                posPublisher.publish(pos);
                Log.d("talker", "pos msg published PosX : " + String.format("%.2f", droneController.positionX) + ", posY : " + String.format("%.2f", droneController.positionY) + ", posZ: " + String.format("%.2f", droneController.positionZ));


                Point vels = velsPublisher.newMessage();
                vels.setX(droneController.xVelocity);
                vels.setY(droneController.yVelocity);
                vels.setZ(droneController.zVelocity);
                velsPublisher.publish(vels);
                Log.d("talker", "vels msg published velX : " + String.format("%.2f", droneController.xVelocity) + ", velY : " + String.format("%.2f", droneController.yVelocity) + ", velZ: " + String.format("%.2f", droneController.zVelocity));


                Thread.sleep(1000);
            }
        });




        /*
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



    }
}
