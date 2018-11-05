package com.min.home.monitor.rtc;

import android.app.Activity;
import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.min.home.monitor.util.LogUtil;
import com.min.home.monitor.util.Util;

import org.webrtc.AudioSource;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import io.socket.emitter.Emitter;

public class RtcClient {

    private Context context;
    private RtcListener rtcListener;
    private PeerConnectionParameters pcParams;
    private PeerConnectionFactory factory;
    private VideoCapturer videoCapturer;
    private VideoSource videoSource;
    private MediaStream localMS;
    private EglBase.Context eglContext;

    public RemotePeer remotePeer;
    private String roomNo;

    public RtcClient(Context context, RtcListener listener, PeerConnectionParameters params, String roomNo) {
        this.context = context;
        rtcListener = listener;
        pcParams = params;
        this.roomNo = roomNo;
        eglContext = EglBase.create().getEglBaseContext();

        PeerConnectionFactory.initializeAndroidGlobals(context.getApplicationContext(), params.videoCodecHwAcceleration);
        PeerConnectionFactory.Options opt = null;
        if (pcParams.loopback) {
            opt = new PeerConnectionFactory.Options();
            opt.networkIgnoreMask = 0;
        }
        factory = new PeerConnectionFactory(opt);
        factory.setVideoHwAccelerationOptions(eglContext, eglContext);
        addSignalListener();
    }

    public EglBase.Context getEglContext() {
        return eglContext;
    }

    public void onPause() {
        try {
            if (videoCapturer != null) {
                videoCapturer.stopCapture();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onResume() {
        try {
            if (videoCapturer != null) {
                videoCapturer.startCapture(pcParams.videoWidth, pcParams.videoHeight, pcParams.videoFps);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        removeSignalListener();
        if (remotePeer != null) {
            remotePeer.getPeerConnection().dispose();
        }
        if (videoSource != null) {
            videoSource.dispose();
        }
        factory.dispose();
    }

    public void initLocalStream() {
        localMS = factory.createLocalMediaStream("ARDAMS");
        if (pcParams.videoCallEnabled) {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
            videoSource = factory.createVideoSource(videoCapturer);
            videoCapturer.startCapture(pcParams.videoWidth, pcParams.videoHeight, pcParams.videoFps);
            VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
            localMS.addTrack(videoTrack);
        }
        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
        localMS.addTrack(factory.createAudioTrack("ARDAMSa0", audioSource));
        rtcListener.onLocalStream(localMS);
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    public void switchCamera() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            LogUtil.d("switchCamera");
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
            cameraVideoCapturer.switchCamera(null);
        }
    }

    private void addSignalListener() {
        SignalClient.getInstance().on("agreeEvent", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LogUtil.d("对方同意开启聊天-agreeEvent:" + args[0].toString());
                offer();
            }
        });
        SignalClient.getInstance().on("rejectEvent", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LogUtil.d("视频聊天被拒绝");
                Util.toast(context, "视频聊天被拒绝");
                SignalClient.getInstance().emit("leaveEvent", roomNo);
                exitChat();
            }
        });
        SignalClient.getInstance().on("hangUpEvent", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LogUtil.d("视频聊天被挂断");
                Util.toast(context, "视频聊天被挂断");
                SignalClient.getInstance().emit("leaveEvent", roomNo);
                exitChat();
            }
        });
        SignalClient.getInstance().on("rtcEvent", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = JSON.parseObject(args[0].toString());
                String type = data.getString("type");
                if (type.equals("offer")) {
                    LogUtil.d("收到offer sessionDescription-->" + data.toJSONString());
                    answer();
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(data.getString("type")),
                            data.getString("sdp")
                    );
                    remotePeer.getPeerConnection().setRemoteDescription(remotePeer, sdp);
                    remotePeer.getPeerConnection().createAnswer(remotePeer, remotePeer.getPcConstraints());
                } else if (type.equals("answer")) {
                    LogUtil.d("收到answer sessionDescription-->" + data.toJSONString());
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(data.getString("type")),
                            data.getString("sdp")
                    );
                    remotePeer.getPeerConnection().setRemoteDescription(remotePeer, sdp);
                } else if (type.equals("candidate")) {
                    LogUtil.d("收到candidate-->" + data.toJSONString());
                    if (remotePeer != null) {
                        IceCandidate candidate = new IceCandidate(
                                data.getString("id"),
                                data.getInteger("label"),
                                data.getString("candidate")
                        );
                        remotePeer.getPeerConnection().addIceCandidate(candidate);
                    }
                }
            }
        });
    }

    public void offer() {
        LogUtil.d("创建RemotePeer--offer");
        remotePeer = new RemotePeer(this, rtcListener, localMS);
        remotePeer.getPeerConnection().createOffer(remotePeer, remotePeer.getPcConstraints());
    }

    public void answer() {
        LogUtil.d("创建RemotePeer--answer");
        remotePeer = new RemotePeer(this, rtcListener, localMS);
    }

    public void doHangUp(String roomNo) {
        LogUtil.d("挂断视频聊天");
        SignalClient.getInstance().emit("hangUpEvent", roomNo);
    }

    private void removeSignalListener() {
        SignalClient.getInstance().off("agreeEvent");
        SignalClient.getInstance().off("rejectEvent");
        SignalClient.getInstance().off("hangUpEvent");
        SignalClient.getInstance().off("rtcEvent");
    }

    private void exitChat() {
        Util.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = (Activity) context;
                activity.finish();
            }
        });
    }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return factory;
    }

    public String getRoomNo() {
        return roomNo;
    }

}
