package com.ielson.djiBote;

import android.hardware.Camera;
import android.util.Size;

interface RawImageListener {

    void onNewRawImage(byte[] data, Size size);

}