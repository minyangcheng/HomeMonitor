package com.min.home.monitor.ui;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

import com.blankj.utilcode.util.ToastUtils;
import com.min.home.monitor.R;
import com.min.home.monitor.rtc.PeerConnectionParameters;
import com.min.home.monitor.rtc.ProxyRenderer;
import com.min.home.monitor.rtc.RtcClient;
import com.min.home.monitor.rtc.RtcListener;
import com.min.home.monitor.rtc.SignalClient;
import com.min.home.monitor.util.LogUtil;
import com.min.home.monitor.util.Util;

import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

public class ChatActivity extends Activity implements RtcListener, View.OnClickListener {

    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";

    private ImageView switchCameraIv;
    private ImageView switchMuteIv;
    private ImageView hangupIv;
    private ImageView switchSpeakeIv;


    private SurfaceViewRenderer pipRenderer;
    private SurfaceViewRenderer fullscreenRenderer;

    private RtcClient client;
    private final ProxyRenderer remoteProxyRenderer = new ProxyRenderer();
    private final ProxyRenderer localProxyRenderer = new ProxyRenderer();

    private String roomNo;
    private int useType; //1监控端 2查看端

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_chat);
        getDataFromIntent();
        initRtcClient();
        initViews();
        startChat();
    }

    private void startChat() {
        client.initLocalStream();
        LogUtil.d("加入聊天室：" + roomNo);
        SignalClient.getInstance().emit("joinEvent", roomNo);
    }

    private void getDataFromIntent() {
        useType = getIntent().getIntExtra("useType", 1);
        roomNo = getIntent().getStringExtra("roomNo");
    }

    private void initViews() {
        switchCameraIv = findViewById(R.id.iv_switch_camear);
        switchCameraIv.setOnClickListener(this);
        switchMuteIv = findViewById(R.id.iv_switch_mute);
        switchMuteIv.setOnClickListener(this);
        hangupIv = findViewById(R.id.iv_hangup);
        hangupIv.setOnClickListener(this);
        switchSpeakeIv = findViewById(R.id.iv_switch_speak);
        switchSpeakeIv.setOnClickListener(this);

        pipRenderer = findViewById(R.id.pip_video_view);
        fullscreenRenderer = findViewById(R.id.fullscreen_video_view);
        pipRenderer.init(client.getEglContext(), null);
        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        fullscreenRenderer.init(client.getEglContext(), null);
        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        pipRenderer.setZOrderMediaOverlay(true);
        pipRenderer.setEnableHardwareScaler(true);
        fullscreenRenderer.setEnableHardwareScaler(true);
        setSwappedFeeds();
    }

    private void initRtcClient() {
        LogUtil.d("初始化RtcClient");
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30,
                1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);
        client = new RtcClient(this, this, params, roomNo);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (client != null) {
            client.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (client != null) {
            client.onResume();
        }
    }

    @Override
    public void onDestroy() {
        if (client != null) {
            client.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onStatusChanged(final PeerConnection.IceConnectionState iceConnectionState) {
        LogUtil.d("onStatusChanged...." + iceConnectionState);
        Util.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (iceConnectionState) {
                    case CONNECTED:
                        ToastUtils.showShort("远程连接成功");
                        break;
                    case DISCONNECTED:
                    case CLOSED:
                        pipRenderer.clearImage();
                        remoteProxyRenderer.setTarget(null);
                        ToastUtils.showShort("远程连接断开");
                        break;
                }
            }
        });

    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        LogUtil.d("onLocalStream....");
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localProxyRenderer));
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream) {
        LogUtil.d("onAddRemoteStream....");
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteProxyRenderer));
    }

    @Override
    public void onRemoveRemoteStream() {
        LogUtil.d("onRemoveRemoteStream....");
        pipRenderer.clearImage();
        remoteProxyRenderer.setTarget(null);
    }

    private void setSwappedFeeds() {
        boolean isSwappedFeeds = useType == 1;
        localProxyRenderer.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
        fullscreenRenderer.setMirror(isSwappedFeeds);
        pipRenderer.setMirror(!isSwappedFeeds);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_hangup) {
            if (useType == 1) {
                client.doHangUp(roomNo);
            } else if (useType == 2) {
                SignalClient.getInstance().emit("leaveEvent", roomNo);
                finish();
            }
        } else if (id == R.id.iv_switch_camear) {
            client.switchCamera();
        }
    }

    @Override
    public void onBackPressed() {
    }
}