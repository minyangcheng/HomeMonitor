package com.min.home.monitor.ui;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.min.home.monitor.R;
import com.min.home.monitor.rtc.SignalClient;
import com.min.home.monitor.util.LogUtil;

import java.util.List;

import io.socket.emitter.Emitter;

public class MonitorActivity extends AppCompatActivity implements View.OnClickListener {

    private RadioGroup mTypeRg;
    private TextView mIdTv;
    private EditText mRoomNoEt;
    private TextView mOpenTv;
    private TextView mLookTv;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public static String[] permissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);
        getPermission();
        findView();
        initView();
        initSignal();
    }

    private void getPermission() {
        PermissionUtils.permission(permissions)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(List<String> permissionsGranted) {
                    }

                    @Override
                    public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                        ToastUtils.showShort("授权失败：" + permissionsDeniedForever.toString());
                    }
                })
                .request();
    }

    private void initSignal() {
        SignalClient.getInstance().on("connectedEvent", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final String idStr = args[0].toString();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mIdTv.setText(idStr);
                        SPUtils.getInstance().put("idStr", idStr);
                    }
                });
            }
        });
        SignalClient.getInstance().connect();
    }

    private void initView() {
        mTypeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                LogUtil.d("checkedId：" + checkedId);
                if (checkedId == R.id.rb_monitor) {
                    putUseType(1);
                } else if (checkedId == R.id.rb_look) {
                    putUseType(2);
                }
                changeUiByUseType();
            }
        });
        if (getUseType() == 1) {
            mTypeRg.check(R.id.rb_monitor);
        } else {
            mTypeRg.check(R.id.rb_look);
        }
        String idStr = SPUtils.getInstance().getString("idStr", "");
        mIdTv.setText(idStr);
        findViewById(R.id.ll_id).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyText(mIdTv.getText().toString());
                return false;
            }
        });
        findViewById(R.id.ll_room_no).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyText(mRoomNoEt.getText().toString());
                return false;
            }
        });
        mOpenTv.setOnClickListener(this);
        mLookTv.setOnClickListener(this);
    }

    private void changeUiByUseType() {
        if (getUseType() == 1) {
            mRoomNoEt.setEnabled(false);
            mRoomNoEt.setText(getRoomNoForMonitor());
            mOpenTv.setVisibility(View.VISIBLE);
            mLookTv.setVisibility(View.GONE);
        } else {
            mRoomNoEt.setEnabled(true);
            mRoomNoEt.setText(SPUtils.getInstance().getString("roomNo_look"));
            mOpenTv.setVisibility(View.GONE);
            mLookTv.setVisibility(View.VISIBLE);
        }
    }

    private void findView() {
        mTypeRg = findViewById(R.id.rg_type);
        mIdTv = findViewById(R.id.tv_id);
        mRoomNoEt = findViewById(R.id.et_room_no);
        mOpenTv = findViewById(R.id.tv_open);
        mLookTv = findViewById(R.id.tv_look);
    }

    private String getRoomNoForMonitor() {
        String roomNo = SPUtils.getInstance().getString("roomNo_monitor");
        if (TextUtils.isEmpty(roomNo)) {
            roomNo = System.currentTimeMillis() + "";
            SPUtils.getInstance().put("roomNo_monitor", roomNo);
        }
        return roomNo;
    }

    private void putUseType(int useType) {
        SPUtils.getInstance().put("useType", useType, true);
    }

    private int getUseType() {
        return SPUtils.getInstance().getInt("useType", 1);
    }

    private void startChat(String roomNo) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("roomNo", roomNo);
        intent.putExtra("useType", getUseType());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        SignalClient.getInstance().off("connectedEvent");
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        String roomNo = mRoomNoEt.getText().toString();
        if (!TextUtils.isEmpty(roomNo)) {
            if (v.getId() == R.id.tv_look) {
                SPUtils.getInstance().put("roomNo_look", roomNo);
            }
            startChat(roomNo);
        } else {
            ToastUtils.showShort("请输入房间号");
        }
    }

    public void copyText(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(text);
        ToastUtils.showShort("复制到剪切版成功!");
    }

}
