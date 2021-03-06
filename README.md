> 分享诸葛亮临终前写给他儿子诸葛瞻的一封家书《诫子书》
夫君子之行，静以修身，俭以养德。非澹泊无以明志，非宁静无以致远。夫学须静也，才须学也，非学无以广才，非志无以成学。慆慢则不能励精，险躁则不能冶性。年与时驰，意与日去，遂成枯落，多不接世，悲守穷庐，将复何及！

最近公司需要做银行视频面签相关的东西，其中有一块涉及到了视频聊天相关的技术，我在研究此技术的时候，突然想到，可不可以用视频聊天技术实现一个手机家庭监控应用，于是周末花时间写了以下应用，大家可以扫码体验。
![](http://phpk3imnj.bkt.clouddn.com/0005.png)

### 用法

如果你有两台安卓手机，下载安装此应用，一台手机作为监控端（家里不用的废弃手机），一台手机作为查看端（自己平时用的手机），即可实现家庭监控。
1. 监控端操作：在监控端手机上打开此应用，在客户端类型栏目选择监控端，然后记下房间号栏目上的数字，点击开启监控按钮，即可进入监控状态。如果你需要持久监控，请保持应用摄像视频界面一直处于打开状态。
2. 查看端操作：在监控端手机上打开此应用，在客户端类型栏目选择查看端，然则在房间号栏目输入上一步记下来的数字，点击查看监控，即可查看家里的情况。点击挂断图标即可停止查看。

### 相关技术

#### WebRtc（点对点通讯）
我们都知道浏览器本身不支持相互之间建立信道进行通信，都需要通过服务器进行中转。比如现在有两个客户端—甲、乙，他俩想要进行通信，首先需要甲和服务器、乙和服务器之间建立信道。甲给乙发送消息时，甲先将消息发送到服务器上，服务器对甲的消息进行中转，发送到乙处，反过来也是一样。这样甲与乙之间的一次消息要通过两段信道，通信的效率同时受制于这两段信道的带宽。同时这样的信道并不适合数据流的传输，如何建立浏览器之间的点对点传输，一直困扰着开发者。因此WebRTC应运而生。
WebRTC是一个开源项目，旨在使得浏览器能为实时通信（RTC）提供简单的JavaScript接口。说的简单明了一点就是让浏览器提供JS的即时通信接口。这个接口所创立的信道并不是像WebSocket一样，打通一个浏览器与WebSocket服务器之间的通信，而是通过一系列的信令，建立一个浏览器与浏览器之间（peer-to-peer）的信道，这个信道可以发送任何数据，而不需要经过服务器。并且WebRTC通过实现MediaStream，通过浏览器调用设备的摄像头、话筒，使得浏览器之间可以传递音频和视频。
目前此开源项目也支持Android、IOS了，使得Android和IOS设备作为终端设备能够像浏览器一样，进行即时通信。
>https://webrtc.org/native-code/android/

##### 信令服务器
WebRTC使用RTCPeerConnection在浏览器之间传递流数据，但也需要一种协调通信和发送控制消息的机制，这一过程称为信令。 WebRTC没有指定信令实现的方法和协议。这里采用ndoe平台的socket.io实现信令服务器功能.

##### 打洞服务器和中继服务器
WebRTC被设计为点对点通信，因此用户可以通过最直接的路线进行连接。 但是，WebRTC是为了应对真实世界的网络而构建的：客户端应用程序需要穿越NAT网关和防火墙，并且在直接连接失败的情况下需要对等网络需求回退。 作为该过程的一部分，WebRTC API使用STUN服务器来获取您的计算机的IP地址，并且在对等通信失败的情况下使TURN服务器充当中继服务器。这里采用coturn开源项目实现打洞服务器和中继服务器.

#### socket.io（信令服务器）
由于HTTP是无状态的协议，要实现即时通讯非常困难。因为当对方发送一条消息时，服务器并不知道当前有哪些用户等着接收消息，当前实现即时通讯功能最为普遍的方式就是轮询机制。即客户端定期发起一个请求，看看有没有人发送消息到服务器，如果有服务端就将消息发给客户端。这种做法的缺点显而易见，那么多的请求将消耗大量资源，大量的请求其实是浪费的。
现在，我们有了WebSocket，它是HTML5的新API。WebSocket连接本质上就是建立一个TCP连接，WebSocket会通过HTTP请求建立，建立后的WebSocket会在客户端和服务端建立一个持久的连接，直到有一方主动关闭该连接。所以，现在服务器就知道有哪些用户正在连接了，这样通讯就变得相对容易了。
socket.io是一个跨浏览器支持WebSocket的实时通讯的JS。实际上它是WebSocket的父集，Socket.io封装了WebSocket和轮询等方法，会根据情况选择方法来进行通讯。
目前socket.io也支持Android、IOS平台了。
>https://socket.io/

#### 打洞服务器和中继服务器
coturn是作为一个STUN/TURN来使用，其中STUN是用于P2P，而TURN是用于中继转发，用来穿透虚拟网络架构用的。我是在云服务器上ubuntu系统上搭建的coturn
> http://blog.51yip.com/server/1946.html

### 聊天交互流程

这里直接看图:
![](http://phpk3imnj.bkt.clouddn.com/0006.jpg)

### 关键代码

#### 服务器端代码

源码分享
> talk is cheap , show me code
服务器端:https://github.com/minyangcheng/webrtc-server
客户端:https://github.com/minyangcheng/HomeMonitor

* 用express和socket.io搭建信令服务器
```
const express = require('express');
const fs = require('fs');
const path = require('path');
// const http = require('http');
const https = require('https');
const socketIo = require('socket.io');

const app = express();
var privateKey = fs.readFileSync(path.resolve(__dirname, '../certificate/private.pem'), 'utf8');
var certificate = fs.readFileSync(path.resolve(__dirname, '../certificate/file.crt'), 'utf8');
var credentials = {key: privateKey, cert: certificate};


// const httpServer = http.createServer(app);
var httpsServer = https.createServer(credentials, app);
// const io = socketIo(httpServer);
const ios = socketIo(httpsServer);

app.use(express.static(path.resolve(__dirname, '../public')));

// httpServer.listen(8000, function () {
//   console.log('listening on *:8000');
// });
httpsServer.listen(8001, function () {
  console.log('listening on *:8001');
});

// require('./socketHandler.js')(io);
require('./socketHandler.js')(ios);

```
* socket事件处理
```
const util = require('util');
let io;
let userList = [];

module.exports = function (ioServer) {
  io = ioServer;
  io.on('connection', socket => {
    addUser(socket);
    handleEvent(socket);
    socket.on('disconnect', () => {
      deleteUser(socket);
    });
  });
}

function getUserList() {
  return userList;
}

function addUser(socket) {
  console.log('add user ', socket.id);
  userList.push(socket.id);
  socket.emit('connectedEvent', socket.id);
  io.emit('userListEvent', getUserList(socket));
}

function deleteUser(socket) {
  console.log('deleteUser user ', socket.id)
  let index = userList.indexOf(socket.id);
  if (index > -1) {
    userList.splice(index, 1);
  }
  io.emit('userListEvent', getUserList());
}

function handleEvent(socket) {

  socket.on('videoChatEvent', data => {
    data = JSON.parse(data)
    let roomNo = Date.now();
    socket.emit('generateRoomNoEvent', roomNo);
    io.to(data.toUser).emit('videoChatEvent', {roomNo, fromUser: data.fromUser});
  });

  socket.on('joinEvent', roomNo => {
    socket.join(roomNo);
    socket.to(roomNo).emit('agreeEvent', socket.id);
  })

  socket.on('leaveEvent', roomNo => {
    socket.leave(roomNo);
  })

  socket.on('rtcEvent', event => {
    event = JSON.parse(event);
    socket.to(event.roomNo).emit('rtcEvent', event);
  })

  socket.on('rejectEvent', roomNo => {
    socket.to(roomNo).emit('rejectEvent');
  })

  socket.on('hangUpEvent', roomNo => {
    io.to(roomNo).emit('hangUpEvent');
  })

}

```

#### 客户端代码

* 信令客户端
```
public class SignalClient {

    private static SignalClient signalClient;
    private Socket client;
    private UserBean user;

    private SignalClient() {
        try {
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
            IO.setDefaultOkHttpCallFactory(okHttpClient);
            IO.Options opts = new IO.Options();
            opts.callFactory = okHttpClient;
            opts.webSocketFactory = okHttpClient;
            client = IO.socket(BuildConfig.SIGNAL_SERVER_HOST, opts);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("创建信令客户端失败");
        }
    }

    public static SignalClient getInstance() {
        if (signalClient == null) {
            synchronized (SignalClient.class) {
                if (signalClient == null) {
                    signalClient = new SignalClient();
                }
            }
        }
        return signalClient;
    }

    public void connect() {
        if (!client.connected()) {
            client.on("connectedEvent", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    String name = args[0].toString();
                    LogUtil.d("用户上线id-connectedEvent:" + name);
                    user = new UserBean(name, "");
                }
            });
            client.connect();
        }
    }

    public void disConnect() {
        client.disconnect();
    }

    public void on(String event, Emitter.Listener listener) {
        client.on(event, listener);
    }

    public void off(String event) {
        client.off(event);
    }

    public void emit(String event, Object... args) {
        client.emit(event, args);
    }

    public UserBean getUser() {
        return user;
    }

}

```

* webrtc功能实现
```
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
```