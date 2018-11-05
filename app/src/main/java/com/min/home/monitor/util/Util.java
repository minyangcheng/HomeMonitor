package com.min.home.monitor.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * Created by minych on 18-11-3.
 */

public class Util {

    public static Handler handler = new Handler(Looper.getMainLooper());

    public static void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public static void toast(final Context context, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showMessageDialog(Context context, String message) {
        showAlertDialog(context, null, message, null, null, null, null);
    }

    public static void showAlertDialog(Context context, String title, String message, String sureStr, DialogInterface.OnClickListener sureListener,
                                       String cancelStr, DialogInterface.OnClickListener cancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }
        if (!TextUtils.isEmpty(message)) {
            builder.setMessage(message);
        }
        if (!TextUtils.isEmpty(sureStr)) {
            builder.setPositiveButton(sureStr, sureListener);
        }
        if (!TextUtils.isEmpty(cancelStr)) {
            builder.setNegativeButton(cancelStr, cancelListener);
        }
        builder.show();
    }

}
