package com.min.home.monitor.rtc;

import com.alibaba.fastjson.JSONObject;
import com.min.home.monitor.util.LogUtil;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.LinkedList;

/**
 * Created by minych on 18-11-2.
 */

public class RemotePeer implements SdpObserver, PeerConnection.Observer {

    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private MediaConstraints pcConstraints = new MediaConstraints();

    private RtcClient rtcClient;
    private RtcListener rtcListener;
    private PeerConnection pc;

    public RemotePeer(RtcClient rtcClient, RtcListener rtcListener, MediaStream localMS) {
        initParams();
        this.rtcClient = rtcClient;
        this.rtcListener = rtcListener;
        this.pc = rtcClient.getPeerConnectionFactory().createPeerConnection(iceServers, pcConstraints, this);
        pc.addStream(localMS);
    }

    private void initParams() {
        iceServers.add(new PeerConnection.IceServer("turn:39.107.240.238:3478?transport=udp","mytest","12345678"));
        iceServers.add(new PeerConnection.IceServer("turn:39.107.240.238:3478?transport=tcp","mytest","12345678"));
        iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
    }

    @Override
    public void onCreateSuccess(final SessionDescription sdp) {
        LogUtil.d("create sdp -->" + sdp.description);
        JSONObject data = new JSONObject();
        data.put("type", sdp.type.canonicalForm());
        data.put("roomNo", rtcClient.getRoomNo());
        data.put("sdp", sdp.description);
        SignalClient.getInstance().emit("rtcEvent", data);
        pc.setLocalDescription(this, sdp);
    }

    @Override
    public void onSetSuccess() {
    }

    @Override
    public void onCreateFailure(String s) {
    }

    @Override
    public void onSetFailure(String s) {
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            removeRemotePeer();
        }
        rtcListener.onStatusChanged(iceConnectionState);
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        LogUtil.d("create candidate -->" + candidate.toString());
        JSONObject data = new JSONObject();
        data.put("type", "candidate");
        data.put("roomNo", rtcClient.getRoomNo());
        data.put("label", candidate.sdpMLineIndex);
        data.put("id", candidate.sdpMid);
        data.put("candidate", candidate.sdp);
        SignalClient.getInstance().emit("rtcEvent", data);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        rtcListener.onAddRemoteStream(mediaStream);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        removeRemotePeer();
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

    }

    private void removeRemotePeer() {
        rtcListener.onRemoveRemoteStream();
        pc.close();
    }

    public PeerConnection getPeerConnection() {
        return pc;
    }

    public MediaConstraints getPcConstraints() {
        return pcConstraints;
    }

}
