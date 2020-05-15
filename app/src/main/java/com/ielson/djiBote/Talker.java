package com.ielson.djiBote;


import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import dji.common.flightcontroller.FlightControllerState;
import dji.midware.data.model.P3.DataFlycGetPushFlightRecord;
import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.Quaternion;
import std_msgs.Int32;
import sensor_msgs.Image;
import std_msgs.Header;


public class Talker extends AbstractNodeMain {
    private String topic_name;
    Publisher<geometry_msgs.Point> posPublisher;
    Publisher<geometry_msgs.Point> rpyPublisher;
    Publisher<geometry_msgs.Point> velsPublisher;
    Publisher<std_msgs.Int32> headingPublisher;
    Publisher<std_msgs.Int32> flightTimePublisher;
    Publisher<std_msgs.Int32> goHomePublisher;
    Publisher<sensor_msgs.Image> videoPublisher;


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
    public static Image videoFeed;

    public Talker() {
        topic_name = "chatter";
    }

    public Talker(String topic)
    {
        topic_name = topic;

    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava_tutorial_pubsub/talker");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        /*
        final Publisher<std_msgs.String> publisher =
                connectedNode.newPublisher(topic_name, std_msgs.String._TYPE);
        */
        posPublisher = connectedNode.newPublisher(topic_name, Point._TYPE);
        // This CancellableLoop will be canceled automatically when the node shuts
        // down.
        rpyPublisher = connectedNode.newPublisher(GraphName.of("rpy"), Point._TYPE);
        velsPublisher = connectedNode.newPublisher(GraphName.of("vels"), Point._TYPE);
        headingPublisher = connectedNode.newPublisher(GraphName.of("heading"), Int32._TYPE);
        flightTimePublisher = connectedNode.newPublisher(GraphName.of("flightTime"), Int32._TYPE);
        goHomePublisher = connectedNode.newPublisher(GraphName.of("goHomeHeight"), Int32._TYPE);

        videoPublisher = connectedNode.newPublisher(GraphName.of("videoFeed"), Image._TYPE);

        final Image videoFeedMsg = videoPublisher.newMessage();
        Header header = videoFeedMsg.getHeader();
        header.setStamp(connectedNode.getCurrentTime());
//        videoFeedMsg.setHeight();
//        videoFeedMsg.setWidth();
//        videoFeedMsg.setEncoding();
//        videoFeedMsg.setIsBigendian();
//        videoFeedMsg.setStep();
//        videoFeedMsg.setData();
        
        MainActivity.mFlightController.setStateCallback(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(@NonNull final FlightControllerState flightControllerState) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
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

                        Point rpy = rpyPublisher.newMessage();
                        rpy.setX(yaw);
                        rpy.setY(pitch);
                        rpy.setZ(roll);
                        rpyPublisher.publish(rpy);

                        Point vels = velsPublisher.newMessage();
                        vels.setX(xVelocity);
                        vels.setY(yVelocity);
                        vels.setZ(zVelocity);
                        velsPublisher.publish(vels);

                        Int32 headMsg = headingPublisher.newMessage();
                        headMsg.setData(headDirection);
                        headingPublisher.publish(headMsg);

                        Int32 flightTimeMsg = flightTimePublisher.newMessage();
                        flightTimeMsg.setData(flightTime);
                        flightTimePublisher.publish(flightTimeMsg);

                        Int32 goHomeHeightMsg = goHomePublisher.newMessage();
                        goHomeHeightMsg.setData(goHomeHeight);
                        goHomePublisher.publish(goHomeHeightMsg);
                        

                    }
                });
            }
        });


    }
}