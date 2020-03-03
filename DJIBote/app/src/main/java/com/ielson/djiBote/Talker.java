package com.ielson.djiBote;

/*
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

public class Talker extends AbstractNodeMain {
    private String topic_name;

    public Talker() {
        this.topic_name = "chatter";
    }

    public Talker(String topic) {
        this.topic_name = topic;
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava_tutorial_pubsub/talker");
    }

    public void onStart(ConnectedNode connectedNode) {
        final Publisher<std_msgs.String> publisher = connectedNode.newPublisher(this.topic_name, "std_msgs/String");
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private int sequenceNumber;

            protected void setup() {
                this.sequenceNumber = 0;
            }

            protected void loop() throws InterruptedException {
                std_msgs.String str = (std_msgs.String)publisher.newMessage();
                str.setData("Hello world! " + this.sequenceNumber);
                publisher.publish(str);
                ++this.sequenceNumber;
                Thread.sleep(1000L);
            }
        });
    }
}

*/

import android.util.Log;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

/**
 * A simple {@link Publisher} {@link NodeMain}.
 *
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Talker extends AbstractNodeMain {
    private String topic_name;

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
        final Publisher<std_msgs.String> publisher =
                connectedNode.newPublisher(topic_name, std_msgs.String._TYPE);
        // This CancellableLoop will be canceled automatically when the node shuts
        // down.
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private int sequenceNumber;

            @Override
            protected void setup() {
                sequenceNumber = 0;
            }

            @Override
            protected void loop() throws InterruptedException {
                Log.d("Loop","ok");
                std_msgs.String str = publisher.newMessage();
                str.setData("Hello Binu " + sequenceNumber);
                publisher.publish(str);
                sequenceNumber++;
                Thread.sleep(1000);
            }
        });
    }
}