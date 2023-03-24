package com.ielson.djiBote;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import geometry_msgs.Twist;

import static com.ielson.djiBote.droneController.mFlightController;

public class CmdVelListener extends AbstractNodeMain {
    private Context context;

    public CmdVelListener(Context context) {
        this.context = context;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("djiBote/cmdVelListener");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Log.e("CMDVEL", "on Start");
        Subscriber<geometry_msgs.Twist> subscriber = connectedNode.newSubscriber("/cmd_vel", Twist._TYPE);
        subscriber.addMessageListener(new MessageListener<Twist>() {
            @Override
            public void onNewMessage(Twist twist) {
                Log.e("CMDVEL", "new msg: " + twist.getAngular().getY() + " " + twist.getAngular().getX() +  " " +twist.getAngular().getZ() +  " " +twist.getLinear().getZ());
                if (mFlightController != null) {
                    Log.e("CMDVEL", "sending virtual stick control data");
                    if (droneController.state == droneController.State.VIRTUALSTICKCOMPLETE) {
                        mFlightController.sendVirtualStickFlightControlData(
                                new FlightControlData(
                                        (float) twist.getLinear().getY(), (float) twist.getLinear().getX(), (float) twist.getAngular().getZ(), (float) twist.getLinear().getZ()
                                ), new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError != null) {
                                            Log.e("CMDVEL", "djiError: " + djiError.getDescription());
                                        } else {
                                            Log.e("CMDVEL", "cmd sent");
                                        }
                                    }
                                }
                        );
                    }
                }
            }
        });
    }
}
