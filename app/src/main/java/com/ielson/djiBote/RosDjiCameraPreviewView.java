package com.ielson.djiBote;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;

public class RosDjiCameraPreviewView extends DjiCameraPreviewView implements NodeMain {
    private static final String TAG = RosDjiCameraPreviewView.class.getName();

    public RosDjiCameraPreviewView(Context context) {
        super(context);
        Log.e(TAG, "Starting ROS");
    }

    public RosDjiCameraPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.e(TAG, "Starting ROS");
    }

    public RosDjiCameraPreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Log.e(TAG, "Starting ROS");
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("ros_dji_camera_preview_view");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Log.e(TAG, "Ros onStart");
        setRawImageListener(new CompressedImagePublisher(connectedNode));
    }

    @Override
    public void onShutdown(Node node) {
        Log.e(TAG, "Ros onShutdown");
    }

    @Override
    public void onShutdownComplete(Node node) {
        Log.e(TAG, "Ros onShutdownComplete");
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        Log.e(TAG, "Ros onError");
    }
}
