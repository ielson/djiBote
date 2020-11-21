package com.ielson.djiBote;

import android.content.Context;
import android.widget.Toast;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Twist;

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
        super.onStart(connectedNode);

        Subscriber<geometry_msgs.Twist> subscriber = connectedNode.newSubscriber("cmd_vel", Twist._TYPE);
        subscriber.addMessageListener(new MessageListener<Twist>() {
            @Override
            public void onNewMessage(Twist twist) {
                Toast.makeText(context, "New cmd_vel msg received ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
