package com.ielson.djiBote;

import com.google.common.base.Preconditions;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import sensor_msgs.CompressedImage;

/**
 * Publishes preview frames.
 *
 * @author damonkohler@google.com (Damon Kohler)
 */
class CompressedImagePublisher implements com.ielson.djiBote.RawImageListener {

    private final ConnectedNode connectedNode;
    private final Publisher<CompressedImage> imagePublisher;
    private final Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;

    private byte[] rawImageBuffer;
    private android.util.Size rawImageSize;
    private YuvImage yuvImage;
    private Rect rect;
    private ChannelBufferOutputStream stream;
    private Context context;

    public CompressedImagePublisher(Context context, ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
        this.context = context;
        NameResolver resolver = connectedNode.getResolver().newChild("camera");
        imagePublisher =
                connectedNode.newPublisher(resolver.resolve("image/compressed"),
                        sensor_msgs.CompressedImage._TYPE);
        cameraInfoPublisher =
                connectedNode.newPublisher(resolver.resolve("camera_info"), sensor_msgs.CameraInfo._TYPE);
        stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        Log.e("COMP IMG PUB", "ROS Compressed image publisher created");
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onNewRawImage(byte[] data, android.util.Size size) {
        Log.d("COMP IMG PUB", "onNewRaw YUV Image, size: " + size);
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(size);
        if (data != rawImageBuffer || !size.equals(rawImageSize)) {
            rawImageBuffer = data;
            rawImageSize = size;
            yuvImage = new YuvImage(rawImageBuffer, ImageFormat.NV21, size.getWidth(), size.getHeight(), null);
            rect = new Rect(0, 0, size.getWidth(), size.getHeight());
            Log.d("COMP IMG PUB", "jpg image created");
        }

            Time currentTime = connectedNode.getCurrentTime();
            String frameId = "camera";

        try {
            sensor_msgs.CompressedImage image = imagePublisher.newMessage();
            image.setFormat("jpeg");
            image.getHeader().setStamp(currentTime);
            image.getHeader().setFrameId(frameId);

            Preconditions.checkState(yuvImage.compressToJpeg(rect, 20, stream));
            image.setData(stream.buffer().copy());
            stream.buffer().clear();

//            screenShot(yuvImage, rect, context.getExternalFilesDir(null)  + "/bote2_screenshot");
//            Log.e("COMP IMG PUB", "dir: " + context.getExternalFilesDir(null) + "/bote_screenshot");
            imagePublisher.publish(image);
            Log.e("COMP IMG PUB", "Image Published");
        } catch (Exception e) {
            Log.e("COMP IMG PUB", "Exception: "+ e);
        }

        sensor_msgs.CameraInfo cameraInfo = cameraInfoPublisher.newMessage();
        cameraInfo.getHeader().setStamp(currentTime);
        cameraInfo.getHeader().setFrameId(frameId);

        cameraInfo.setWidth(size.getWidth());
        cameraInfo.setHeight(size.getHeight());
        Log.e("COMP IMG PUB", "Camera Info Published");
        cameraInfoPublisher.publish(cameraInfo);
    }

    private void screenShot(YuvImage yuvImage, Rect rect, String shotDir) {
        File dir = new File(shotDir);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        final String path = dir + "/ScreenShot_" + System.currentTimeMillis() + ".jpg";
        OutputStream outputFile = null;
        try {
            outputFile = new FileOutputStream(new File(path));
        }
        catch (FileNotFoundException e) {
            Log.e("COMP IMG PUB", "File not found" + e);
        }
        if (outputFile != null) {
            yuvImage.compressToJpeg(rect, 100, outputFile);
            Log.e("COMP IMG PUB", "output file: " + path);
        }
        try {
            outputFile.close();
        } catch (IOException e) {
            Log.e("COMP IMG PUB", "test screenShot: compress yuv image error: " + e);
            e.printStackTrace();
        }
    }
}