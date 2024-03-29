package com.dji.videostreamdecodingsample.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.ielson.djiBote.R;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import dji.common.product.Model;
import dji.log.DJILog;
import dji.midware.data.model.P3.DataCameraGetPushStateInfo;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * This class is a helper class for hardware decoding. Please follow the following steps to use it:
 *
 * 1. Initialize and set the instance as a listener of NativeDataListener to receive the frame data.
 *
 * 2. Send the raw data from camera to ffmpeg for frame parsing.
 *
 * 3. Get the parsed frame data from ffmpeg parsing frame callback and cache the parsed framed data into the frameQueue.
 *
 * 4. Initialize the MediaCodec as a decoder and then check whether there is any i-frame in the MediaCodec. If not, get
 * the default i-frame from sdk resource and insert it at the head of frameQueue. Then dequeue the framed data from the
 * frameQueue and feed it(which is Byte buffer) into the MediaCodec.
 *
 * 5. Get the output byte buffer from MediaCodec, if a surface(Video Previewing View) is configured in the MediaCodec,
 * the output byte buffer is only need to be released. If not, the output yuv data should invoke the callback and pass
 * it out to external listener, it should also be released too.
 *
 * 6. Release the ffmpeg and the MediaCodec, stop the decoding thread.
 */
public class DJIVideoStreamDecoder implements NativeHelper.NativeDataListener {
    private static final String TAG = DJIVideoStreamDecoder.class.getSimpleName();
    private static final int BUF_QUEUE_SIZE = 30;
    private static final int MSG_INIT_CODEC = 0;
    private static final int MSG_FRAME_QUEUE_IN = 1;
    private static final int MSG_DECODE_FRAME = 2;
    private static final int MSG_CHANGE_SURFACE = 3;
    private static final int CODEC_DEQUEUE_INPUT_QUEUE_RETRY = 20;
    public static final String VIDEO_ENCODING_FORMAT = "video/avc";
    private static HandlerThread handlerThreadNew = new HandlerThread("native parser thread");
    private static Handler handlerNew;

    private final boolean DEBUG = false;

    private static DJIVideoStreamDecoder instance;

    private Queue<DJIFrame> frameQueue;
    private HandlerThread dataHandlerThread;
    private Handler dataHandler;
    private HandlerThread callbackHandlerThread;
    private Handler callbackHandler;
    private Context context;
    private MediaCodec codec;
    private Surface surface;

    public int frameIndex = -1;
    private long currentTime;
    public int width;
    public int height;
    private boolean hasIFrameInQueue = false;
    private boolean hasIFrameInCodec;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    LinkedList<Long> bufferChangedQueue=new LinkedList<Long>();

    private long createTime;

    public interface IYuvDataListener {
        /**
         * Callback method for processing the yuv frame data from hardware decoder.
         * @param yuvFrame
         * @param width
         * @param height
         */
        void onYuvDataReceived(byte[] yuvFrame, int width, int height);
    }

    /**
     * Set the yuv frame data receiving callback. The callback method will be invoked when the decoder
     * output yuv frame data. What should be noted here is that the hardware decoder would not output
     * any yuv data if a surface is configured into, which mean that if you want the yuv frames, you
     * should set "null" surface when calling the "configure" method of MediaCodec.
     * @param yuvDataListener
     */
    public void setYuvDataListener(IYuvDataListener yuvDataListener) {
        this.yuvDataListener = yuvDataListener;
    }

    private IYuvDataListener yuvDataListener;

    /**
     * A data structure for containing the frames.
     */
    private static class DJIFrame {
        public byte[] videoBuffer;
        public int size;
        public long pts;
        public long incomingTimeMs;
        public long fedIntoCodecTime;
        public long codecOutputTime;
        public boolean isKeyFrame;
        public int frameNum;
        public long frameIndex;
        public int width;
        public int height;

        public DJIFrame(byte[] videoBuffer, int size, long pts, long incomingTimeUs, boolean isKeyFrame,
                        int frameNum, long frameIndex, int width, int height){
            this.videoBuffer=videoBuffer;
            this.size=size;
            this.pts =pts;
            this.incomingTimeMs=incomingTimeUs;
            this.isKeyFrame=isKeyFrame;
            this.frameNum=frameNum;
            this.frameIndex=frameIndex;
            this.width=width;
            this.height=height;
        }

        public long getQueueDelay()
        {
            return fedIntoCodecTime-incomingTimeMs;
        }

        public long getDecodingDelay()
        {
            return codecOutputTime-fedIntoCodecTime;
        }

        public long getTotalDelay()
        {
            return codecOutputTime-fedIntoCodecTime;
        }
    }

    private void logd(String tag, String log) {
        if (!DEBUG) {
            return;
        }
        Log.e(tag, log);
    }
    private void loge(String tag, String log) {
        if (!DEBUG) {
            return;
        }
        Log.e(tag, log);
    }

    private void logd(String log) {
        logd(TAG, log);
    }
    private void loge(String log) {
        loge(TAG, log);
    }

    private DJIVideoStreamDecoder() {
        Log.e(TAG, "onDJIVideoStreamDecoder constructor");
        createTime = System.currentTimeMillis();
        frameQueue = new ArrayBlockingQueue<DJIFrame>(BUF_QUEUE_SIZE);
        startDataHandler();
        callbackHandlerThread = new HandlerThread("callback handler");
        callbackHandlerThread.start();
        callbackHandler = new Handler(callbackHandlerThread.getLooper());
        handlerThreadNew.start();
        handlerNew = new Handler(handlerThreadNew.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                byte[] buf = (byte[])msg.obj;
                NativeHelper.getInstance().parse(buf, msg.arg1);
                return false;
            }
        });
    }


    public static DJIVideoStreamDecoder getInstance() {
        if (instance == null) {
            instance = new DJIVideoStreamDecoder();
        }
//        Log.e(TAG, "onDJIVideoStreamDecoder.getInstance");
        return instance;
    }

    /**
     * Initialize the decoder
     * @param context The application context
     * @param surface The displaying surface for the video stream. What should be noted here is that the hardware decoder would not output
     * any yuv data if a surface is configured into, which mean that if you want the yuv frames, you
     * should set "null" surface when calling the "configure" method of MediaCodec.
     */
    public void init(Context context, Surface surface) {
        Log.e(TAG, "onDJIVideoStreamDecoder init");
        Log.e(TAG, "Decoder initialized, surface: " + surface);
        this.context = context;
        this.surface = surface;
        NativeHelper.getInstance().setDataListener(this);
        if (dataHandler != null && !dataHandler.hasMessages(MSG_INIT_CODEC)) {
            dataHandler.sendEmptyMessage(MSG_INIT_CODEC);
        }
    }

    /**
     * Framing the raw data from the camera.
     * @param buf Raw data from camera.
     * @param size Data length
     */
    public void parse(byte[] buf, int size) {
//        Log.e(TAG, "onDJIVideoStreamDecoder parse");
        logd( "parse data size: " + size);
        Message message =handlerNew.obtainMessage();
        message.obj = buf;
        message.arg1 = size;
        handlerNew.sendMessage(message);

    }

    /**
     * Get the resource ID of the IDR frame.
     * @param pModel Product model of connecting DJI product.
     * @param width Width of current video stream.
     * @return Resource ID of the IDR frame
     */
    public int getIframeRawId(Model pModel, int width) {
//        Log.d(TAG, "onDJIVideoStreamDecoder getIFrameRawId");
        int iframeId = R.raw.iframe_1280x720_ins;

        switch(pModel) {
            case PHANTOM_3_ADVANCED:
            case PHANTOM_3_STANDARD:
                if (width==960) {
                    //for photo mode, 960x720, GDR
                    iframeId = R.raw.iframe_960x720_3s;
                } else {
                    //for record mode, 1280x720, GDR
                    iframeId = R.raw.iframe_1280x720_3s;
                }
                break;

            case Phantom_3_4K:
                switch(width) {
                    case 640:
                        //for P3-4K with resolution 640*480
                        iframeId = R.raw.iframe_640x480;
                        break;
                    case 848:
                        //for P3-4K with resolution 848*480
                        iframeId = R.raw.iframe_848x480;
                        break;
                    default:
                        iframeId = R.raw.iframe_1280x720_3s;
                        break;
                }
                break;

            case OSMO_PRO:
            case OSMO:
                iframeId = -1;
                break;

            case PHANTOM_4:
                iframeId = R.raw.iframe_1280x720_p4;
                break;
            case PHANTOM_4_PRO: // p4p
                switch (width) {
                    case 1280:
                        iframeId = R.raw.iframe_p4p_720_16x9;
                        break;
                    case 960:
                        iframeId = R.raw.iframe_p4p_720_4x3;
                        break;
                    case 1088:
                        iframeId = R.raw.iframe_p4p_720_3x2;
                        break;
                    case 1344:
                        iframeId = R.raw.iframe_p4p_1344x720;
                        break;
                    default:
                        iframeId = R.raw.iframe_p4p_720_16x9;
                        break;
                }
                break;
            case INSPIRE_2: //inspire2
                DataCameraGetPushStateInfo.CameraType cameraType = DataCameraGetPushStateInfo.getInstance().getCameraType();
                if(cameraType == DataCameraGetPushStateInfo.CameraType.DJICameraTypeGD600) {
                    iframeId = R.raw.iframe_1080x720_gd600;
                } else {
                    if (width == 640 && height == 368) {
                        DJILog.i(TAG, "Selected Iframe=iframe_640x368_wm620");
                        iframeId = R.raw.iframe_640x368_wm620;
                    }
                    if (width == 608 && height == 448) {
                        DJILog.i(TAG, "Selected Iframe=iframe_608x448_wm620");
                        iframeId = R.raw.iframe_608x448_wm620;
                    } else if (width == 720 && height == 480) {
                        DJILog.i(TAG, "Selected Iframe=iframe_720x480_wm620");
                        iframeId = R.raw.iframe_720x480_wm620;
                    } else if (width == 1280 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1280x720_wm620");
                        iframeId = R.raw.iframe_1280x720_wm620;
                    } else if (width == 1080 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1080x720_wm620");
                        iframeId = R.raw.iframe_1080x720_wm620;
                    } else if (width == 960 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_960x720_wm620");
                        iframeId = R.raw.iframe_960x720_wm620;
                    } else if (width == 1360 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1360x720_wm620");
                        iframeId = R.raw.iframe_1360x720_wm620;
                    } else if (width == 1344 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1344x720_wm620");
                        iframeId = R.raw.iframe_1344x720_wm620;
                    } else if (width == 1760 && height == 720) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1760x720_wm620");
                        iframeId = R.raw.iframe_1760x720_wm620;
                    } else if (width == 1920 && height == 800) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1920x800_wm620");
                        iframeId = R.raw.iframe_1920x800_wm620;
                    } else if (width == 1920 && height == 1024) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1920x1024_wm620");
                        iframeId = R.raw.iframe_1920x1024_wm620;
                    } else if (width == 1920 && height == 1088) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1920x1080_wm620");
                        iframeId = R.raw.iframe_1920x1088_wm620;
                    } else if (width == 1920 && height == 1440) {
                        DJILog.i(TAG, "Selected Iframe=iframe_1920x1440_wm620");
                        iframeId = R.raw.iframe_1920x1440_wm620;
                    }
                }
                break;
            default: //for P3P, Inspire1, etc/
                iframeId = R.raw.iframe_1280x720_ins;
                break;
        }
        return iframeId;
    }

    /** Get default black IDR frame.
     * @param width Width of current video stream.
     * @return IDR frame data
     * @throws IOException
     */
    private byte[] getDefaultKeyFrame(int width) throws IOException {
        Log.e(TAG, "onDJIVideoStreamDecoder getDefaultKeyFrame");
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product == null || product.getModel() == null) {
            return null;
        }
        int iframeId=getIframeRawId(product.getModel(), width);
        if (iframeId >= 0){

            InputStream inputStream = context.getResources().openRawResource(iframeId);
            int length = inputStream.available();
            logd("iframeId length=" + length);
            byte[] buffer = new byte[length];
            inputStream.read(buffer);
            inputStream.close();

            return buffer;
        }
        return null;
    }


    /**
     * Initialize the hardware decoder.
     */
    private void initCodec() {
        Log.d(TAG, "onDJIVideoStreamDecoder initCodec");
        if (width == 0 || height == 0) {
            return;
        }
        if (codec != null) {
            releaseCodec();
        }
        loge("initVideoDecoder----------------------------------------------------------");
        loge("initVideoDecoder video width = " + width + "  height = " + height);
        // create the media format
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_ENCODING_FORMAT, width, height);
        if (surface == null) {
            logd("initVideoDecoder: yuv output");
            // The surface is null, which means that the yuv data is needed, so the color format should
            // be set to YUV420.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        } else {
            logd("initVideoDecoder: display");
            logd("init video decoder surface: " + surface);
            // The surface is set, so the color format should be set to format surface.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        }
//        Log.e("TESTE", "TESTE");
        try {
//            Log.e("TESTE2", "TESTE2");
            // Create the codec instance.
            codec = MediaCodec.createDecoderByType(VIDEO_ENCODING_FORMAT);
            logd( "initVideoDecoder create: " + (codec == null));
            // Configure the codec. What should be noted here is that the hardware decoder would not output
            // any yuv data if a surface is configured into, which mean that if you want the yuv frames, you
            // should set "null" surface when calling the "configure" method of MediaCodec.
            codec.configure(format, surface, null, 0);
            logd( "initVideoDecoder configure!");
            //            codec.configure(format, null, null, 0);
            if (codec == null) {
                loge("Can't find video info!");
                return;
            }
            // Start the codec
            codec.start();
            logd( "initVideoDecoder start");
            // Get the input and output buffers of hardware decoder
            inputBuffers = codec.getInputBuffers();
            outputBuffers = codec.getOutputBuffers();
            logd( "initVideoDecoder get buffers");


        } catch (Exception e) {
            loge("init codec failed, do it again: " + e);
            if (e instanceof MediaCodec.CodecException) {
                MediaCodec.CodecException ce = (MediaCodec.CodecException) e;
            }
            e.printStackTrace();
        }
    }

    private void startDataHandler() {
        Log.d(TAG, "onDJIVideoStreamDecoder startDataHandler");
        if (dataHandlerThread != null && dataHandlerThread.isAlive()) {
            return;
        }
        dataHandlerThread = new HandlerThread("frame data handler thread");
        dataHandlerThread.start();
        dataHandler = new Handler(dataHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_INIT_CODEC:
                        try {
                            initCodec();
                        } catch (Exception e) {
                            loge("init codec error: " + e.getMessage());
                            e.printStackTrace();
                        }

                        removeCallbacksAndMessages(null);
                        sendEmptyMessageDelayed(MSG_DECODE_FRAME, 1);
                        break;
                    case MSG_FRAME_QUEUE_IN:
                        try {
                            onFrameQueueIn(msg);
                        } catch (Exception e) {
                            loge("queue in frame error: " + e);
                            e.printStackTrace();
                        }

                        if (!hasMessages(MSG_DECODE_FRAME)) {
                            sendEmptyMessage(MSG_DECODE_FRAME);
                        }
                        break;
                    case MSG_DECODE_FRAME:
                        try {
                            decodeFrame();
                        } catch (Exception e) {
                            loge("handle frame error: " + e);
                            if (e instanceof MediaCodec.CodecException) {
                            }
                            e.printStackTrace();
                            initCodec();
                        }finally {
                            if (frameQueue.size() > 0) {
                                sendEmptyMessage(MSG_DECODE_FRAME);
                            }
                        }
                        break;
                    case MSG_CHANGE_SURFACE:

                        break;
                    default:
                        break;
                }
            }
        };
        dataHandler.sendEmptyMessage(MSG_DECODE_FRAME);
    }

    /**
     * Stop the data processing thread
     */
    private void stopDataHandler() {
        Log.d(TAG, "onDJIVideoStreamDecoder stop Data Handler");
        if (dataHandlerThread == null || !dataHandlerThread.isAlive()) {
            return;
        }
        if (dataHandler != null) {
            dataHandler.removeCallbacksAndMessages(null);
        }
        if (Build.VERSION.SDK_INT >= 18) {
            dataHandlerThread.quitSafely();
        } else {
            dataHandlerThread.quit();
        }

        try {
            dataHandlerThread.join(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        releaseCodec();
        dataHandler = null;
    }

    /**
     * Change the displaying surface of the decoder. What should be noted here is that the hardware decoder would not output
     * any yuv data if a surface is configured into, which mean that if you want the yuv frames, you
     * should set "null" surface when calling the "configure" method of MediaCodec.
     * @param surface
     */
    public void changeSurface(Surface surface) {
        Log.d(TAG, "onDJIVideoStreamDecoder changeSurface");
        this.surface = surface;
        if (dataHandler != null && !dataHandler.hasMessages(MSG_INIT_CODEC)) {
            dataHandler.sendEmptyMessage(MSG_INIT_CODEC);
        }
    }

    /**
     * Release and close the codec.
     */
    private void releaseCodec() {
        Log.d(TAG, "onDJIVideoStreamDecoder releaseCodec");
        if (frameQueue!=null){
            frameQueue.clear();
            hasIFrameInQueue = false;
            hasIFrameInCodec = false;
        }
        if (codec != null) {
            try {
                codec.flush();
            } catch (Exception e) {
                loge("flush codec error: " + e.getMessage());
                codec = null;
            }

            try {
                codec.release();
            } catch (Exception e) {
                loge("close codec error: " + e.getMessage());
            } finally {
                codec = null;
            }
        }
    }

    /**
     * Queue in the frame.
     * @param msg
     */
    private void onFrameQueueIn(Message msg) {
        Log.d(TAG, "onDJIVideoStreamDecoder onFrameQueueIn");
        DJIFrame inputFrame = (DJIFrame)msg.obj;
        if (inputFrame == null) {
            return;
        }
        if (!hasIFrameInQueue) { // check the I frame flag
            if (inputFrame.frameNum !=1 && !inputFrame.isKeyFrame) {
                loge("the timing for setting iframe has not yet come.");
                return;
            }
            byte[] defaultKeyFrame = null;
            try {
                defaultKeyFrame = getDefaultKeyFrame(inputFrame.width); // Get I frame data
            } catch (IOException e) {
                loge("get default key frame error: " + e.getMessage());
            }
            if (defaultKeyFrame != null) {
                DJIFrame iFrame = new DJIFrame(
                        defaultKeyFrame,
                        defaultKeyFrame.length,
                        inputFrame.pts,
                        System.currentTimeMillis(),
                        inputFrame.isKeyFrame,
                        0,
                        inputFrame.frameIndex - 1,
                        inputFrame.width,
                        inputFrame.height
                );
                frameQueue.clear();
                frameQueue.offer(iFrame); // Queue in the I frame.
                logd("add iframe success!!!!");
                hasIFrameInQueue = true;
            } else if (inputFrame.isKeyFrame) {
                logd("onFrameQueueIn no need add i frame!!!!");
                hasIFrameInQueue = true;
            } else {
                loge("input key frame failed");
            }
        }
        if (inputFrame.width!=0 && inputFrame.height != 0 &&
                inputFrame.width != this.width &&
                inputFrame.height != this.height) {
            this.width = inputFrame.width;
            this.height = inputFrame.height;
    	   /*
    	    * On some devices, the codec supports changing of resolution during the fly
    	    * However, on some devices, that is not the case.
    	    * So, reset the codec in order to fix this issue.
    	    */
            loge("init decoder for the 1st time or when resolution changes");
            if (dataHandler != null && !dataHandler.hasMessages(MSG_INIT_CODEC)) {
                dataHandler.sendEmptyMessage(MSG_INIT_CODEC);
            }
        }
        // Queue in the input frame.
        if (this.frameQueue.offer(inputFrame)){
            logd("put a frame into the Extended-Queue with index=" + inputFrame.frameIndex);
        } else {
            // If the queue is full, drop a frame.
            DJIFrame dropFrame = frameQueue.poll();
            this.frameQueue.offer(inputFrame);
            loge("Drop a frame with index=" + dropFrame.frameIndex+" and append a frame with index=" + inputFrame.frameIndex);
        }
    }

    /**
     * Dequeue the frames from the queue and decode them using the hardware decoder.
     * @throws Exception
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void decodeFrame() throws Exception {
        Log.d(TAG, "onDJIVideoStreamDecoder decodeFrame");
        DJIFrame inputFrame = frameQueue.poll();
        if (inputFrame == null) {
            return;
        }
        if (codec == null) {
            if (dataHandler != null && !dataHandler.hasMessages(MSG_INIT_CODEC)) {
                dataHandler.sendEmptyMessage(MSG_INIT_CODEC);
            }
        }
        int inIndex = -1;

        // Get input buffer index of the MediaCodec.
        for (int i = 0; i < CODEC_DEQUEUE_INPUT_QUEUE_RETRY && inIndex < 0; i ++) {
            try {
                inIndex = codec.dequeueInputBuffer(0);
            } catch (IllegalStateException e) {
                logd(TAG, "decodeFrame: dequeue input: " + e);
                codec.stop();
                codec.reset();
                initCodec();
                e.printStackTrace();
            }
        }
        logd(TAG, "decodeFrame: index=" + inIndex);

        // Decode the frame using MediaCodec
        if (inIndex >= 0) {
            ByteBuffer buffer = inputBuffers[inIndex];
            buffer.clear();
            buffer.rewind();
            buffer.put(inputFrame.videoBuffer);

            inputFrame.fedIntoCodecTime = System.currentTimeMillis();
            long queueingDelay = inputFrame.getQueueDelay();
            logd("input frame delay: " + queueingDelay);
            // Feed the frame data to the decoder.
            codec.queueInputBuffer(inIndex, 0, inputFrame.size, inputFrame.pts, 0);
            hasIFrameInCodec = true;

            // Get the output data from the decoder.
            int outIndex = -1;
            outIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
            logd(TAG, "decodeFrame: outIndex: " + outIndex);
            if (outIndex >= 0) {
                if (surface == null && yuvDataListener != null) {
                    // If the surface is null, the yuv data should be get from the buffer and invoke the callback.
                    logd("decodeFrame: need callback");
                    ByteBuffer yuvDataBuf = outputBuffers[outIndex];
                    yuvDataBuf.position(bufferInfo.offset);
                    yuvDataBuf.limit(bufferInfo.size - bufferInfo.offset);
                    final byte[] bytes = new byte[bufferInfo.size - bufferInfo.offset];
                    yuvDataBuf.get(bytes);
                    callbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            yuvDataListener.onYuvDataReceived(bytes, width, height);
                        }
                    });
                }
                // All the output buffer must be release no matter whether the yuv data is output or
                // not, so that the codec can reuse the buffer.
                codec.releaseOutputBuffer(outIndex, true);
            } else if (outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // The output buffer set is changed. So the decoder should be reinitialized and the
                // output buffers should be retrieved.
                long curTime = System.currentTimeMillis();
                bufferChangedQueue.addLast(curTime);
                if (bufferChangedQueue.size() >= 10) {
                    long headTime = bufferChangedQueue.pollFirst();
                    if (curTime - headTime < 1000) {
                        // reset decoder
                        loge("Reset decoder. Get INFO_OUTPUT_BUFFERS_CHANGED more than 10 times within a second.");
                        bufferChangedQueue.clear();
                        dataHandler.removeCallbacksAndMessages(null);
                        dataHandler.sendEmptyMessage(MSG_INIT_CODEC);
                        return;
                    }
                }
                if (outputBuffers == null) {
                    return;
                }
                outputBuffers = codec.getOutputBuffers();
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                loge("format changed, color: " + codec.getOutputFormat().getInteger(MediaFormat.KEY_COLOR_FORMAT));
            }
        }
    }

    /**
     * Stop the decoding process.
     */
    public void stop() {
        Log.d(TAG, "onDJIVideoStreamDecoder stop");
        dataHandler.removeCallbacksAndMessages(null);
        frameQueue.clear();
        hasIFrameInQueue = false;
        hasIFrameInCodec = false;
        if (codec != null) {
            try {
                codec.flush();
            } catch (IllegalStateException e) {
            }
        }
        stopDataHandler();
    }

    public void resume() {
        startDataHandler();
    }

    public void destroy() {
    }

    @Override
    public void onDataRecv(byte[] data, int size, int frameNum, boolean isKeyFrame, int width, int height) {
        Log.d(TAG, "onDJIVideoStreamDecoder onDataRecv");
        if (dataHandler == null || dataHandlerThread == null || !dataHandlerThread.isAlive()) {
            return;
        }
        if (data.length != size) {
            loge( "recv data size: " + size + ", data lenght: " + data.length);
        } else {
            logd( "recv data size: " + size + ", frameNum: "+frameNum+", isKeyframe: "+isKeyFrame+"," +
                    " width: "+width+", height: " + height);
            currentTime = System.currentTimeMillis();
            frameIndex ++;
            DJIFrame newFrame = new DJIFrame(data, size, currentTime, currentTime, isKeyFrame,
                    frameNum, frameIndex, width, height);
            dataHandler.obtainMessage(MSG_FRAME_QUEUE_IN, newFrame).sendToTarget();

        }
    }
}