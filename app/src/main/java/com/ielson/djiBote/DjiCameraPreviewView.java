package com.ielson.djiBote;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

public class DjiCameraPreviewView extends ViewGroup {
    private static final String TAG = DjiCameraPreviewView.class.getName();
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallback = null;
    protected DJICodecManager mCodecManager = null;
    private Camera.Size previewSize;

    private final class ReceivedVideoDataCallback implements VideoFeeder.VideoDataCallback {
        @Override
        public void onReceive(byte[] bytes, int size) {
            if (mCodecManager!=null) {
                mCodecManager.sendDataToDecoder(bytes, size);
            }
        }
    }

    private final class TextureViewCallback implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height){
            Log.d(TAG, "onSurfaceTextureAvailable");
            if (mCodecManager == null) {
                mCodecManager = new DJICodecManager(DjiCameraPreviewView.this.getContext(), surface, width, height);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG, "onSurfaceTextureDestroyed");
            if (mCodecManager != null) {
                mCodecManager.cleanSurface();
                mCodecManager = null;
            }
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }

    }

    private void init(Context context){
        TextureView mVideoSurface = new TextureView(context);
        addView(mVideoSurface);
        mVideoSurface.setSurfaceTextureListener(new TextureViewCallback());
        mReceivedVideoDataCallback = new ReceivedVideoDataCallback();
    }

    public DjiCameraPreviewView(Context context) {
        super(context);
        init(context);
    }

    public DjiCameraPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DjiCameraPreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void releaseCamera() {}

    //vou ter que criar uma interface pra o rawImageListener
    //add o rawImageListener

    /*
        public Camera.Size getPreviewSize() {

        }

        public void setCamera(){
            BaseProduct product = ConnectionActivity.mProduct
            product.getCamera().
        }


        private void setupCameraParameters() {

        }


        private Camera.Size getOptimalPreviewSize(){

        }

        private void setupBufferingPreviewCallback() {

        }
    */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);
            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
//            previewSize.width = previewWidth;
//            previewSize.height = previewHeight;


            /*// Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2);
            }*/
            child.layout(0,0, width, height);
        }
    }
}
