package com.bj.awake;

import java.util.Timer;

import android.Manifest;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.CallLog.Calls;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class AutoRedialService extends Service {
    private int mRetryCount = 0;
    private int mDialedCount = 0;
    private String mPhoneNumber = "10086";
    private String mDebugLog = "";
    private boolean mJustCall = false;

    public void Log(String text) {
        //mDebugLog += text;
    }

    @Override
    public IBinder onBind(Intent msg) {
        // 使用 bind的办法，可以方便的在service和
        //activity两个不同的进程直接交互，不过看代码很多啊

        return null;

    }

    private void PhoneCall() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mJustCall = false;
        Uri localUri = Uri.parse("tel:" + mPhoneNumber);
        Intent call = new Intent(Intent.ACTION_CALL, localUri);
        call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startActivity(call);
        //android.util.Log.v("TeleListener", "start to call");
        mDialedCount++;
    }

    private boolean ShouldStop() {
        return mDialedCount > mRetryCount;
    }

    private boolean LastCallSucceed() {

        if (mJustCall == false) {
            return false;
        }

        String[] projection = new String[]{
                Calls.NUMBER,
                Calls.DURATION
        };

        ContentResolver cr = getContentResolver();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return mJustCall;
        }
        final Cursor cur = cr.query(android.provider.CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                Calls.DEFAULT_SORT_ORDER);
        if (cur.moveToFirst()) {
            int duration = cur.getInt(1);
            //上次通话时间
            if (duration > 0 )
            {
                //android.util.Log.v("TeleListener", "|"+ String.valueOf(duration) + "|");
                //Log( "|"+ String.valueOf(duration) + "|");
                return true;
            }
        }

        return false;
    }
    public void onCreate()
    {
        super.onCreate();
    }

    public void onStart(Intent intent, int startID) {
        super.onStart(intent, startID);
        //android.util.Log.v("TeleListener", "starting haha");
        // 获取电话管理的一个类实例
        mRetryCount = intent.getIntExtra("RetryCount", 0);
        String tmp  = intent.getStringExtra("PhoneNumber");
        if (tmp != null)
        {
            mPhoneNumber = tmp;
        }

        TelephonyManager telephonyMgr = (TelephonyManager) this
                .getSystemService(Context.TELEPHONY_SERVICE);

        // 建立一个监听器来实时监听电话的通话状态
        telephonyMgr.listen(new TeleListener(this),
                PhoneStateListener.LISTEN_CALL_STATE);


        mDialedCount = 0;
        //PhoneCall();

    }

   public void onDestroy()
    {
        TelephonyManager telephonyMgr = (TelephonyManager)getSystemService("");
        TeleListener teleListener = new TeleListener(this);
        telephonyMgr.listen(teleListener, 0);
        mDialedCount = 0;
        mRetryCount = 0;
        mPhoneNumber= "18773221877";
        super.onDestroy();
    }

    class TeleListener extends PhoneStateListener {
        private AutoRedialService manager;
        public TeleListener(AutoRedialService a)
        {
            this.manager = a;
        }
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                // 当处于待机状态中
                case TelephonyManager.CALL_STATE_IDLE: {
                    manager.Log("IDLE");
                    //android.util.Log.v("TeleListener", "IDLE");
                    if (manager.ShouldStop() || manager.LastCallSucceed()) {
                        manager.stopSelf();
                        break;
                    }
                    PhoneCall();
                    break;
                }
                // 当处于正在拨号出去，或者正在通话中
                case TelephonyManager.CALL_STATE_OFFHOOK: {
                    manager.Log("OFFHOOK");
                    //android.util.Log.v("TeleListener", "OFFHOOK");
                    mJustCall = true;
                    //Timer t = new Timer();
                    break;
                }
                // 外面拨进来，好没有接拨号状态中..
                case TelephonyManager.CALL_STATE_RINGING: {
                    manager.Log("RINGING");
                    //android.util.Log.v("TeleListener", "RINGING");
                    break;
                }
                default:
                    break;
            }
        }

    }


}

