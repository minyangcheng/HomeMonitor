package com.min.home.monitor.rtc;

import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

/**
 * Created by minych on 18-11-2.
 */

public interface RtcListener {

    void onStatusChanged(PeerConnection.IceConnectionState iceConnectionState);

    void onLocalStream(MediaStream localStream);

    void onAddRemoteStream(MediaStream remoteStream);

    void onRemoveRemoteStream();
}