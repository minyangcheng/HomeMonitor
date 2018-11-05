package com.min.home.monitor.rtc;

import android.util.Log;

import org.webrtc.VideoRenderer;

public class ProxyRenderer implements VideoRenderer.Callbacks {
    private static final String TAG = "ProxyRenderer";
    private VideoRenderer.Callbacks target;

    synchronized public void renderFrame(VideoRenderer.I420Frame frame) {
        if (target == null) {
            Log.d(TAG, "Dropping frame in proxy because target is null.");
            VideoRenderer.renderFrameDone(frame);
            return;
        }

        target.renderFrame(frame);
    }

    synchronized public void setTarget(VideoRenderer.Callbacks target) {
        this.target = target;
    }
}
