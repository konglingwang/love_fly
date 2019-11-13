package com.baidu.aip.asrwakeup3.core.wakeup.listener;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;

import com.baidu.aip.asrwakeup3.core.util.MyLogger;
import com.baidu.aip.asrwakeup3.core.wakeup.WakeUpResult;
import com.campus.weixin.ConnectionActivity;
import com.campus.weixin.MainActivity;

import static android.content.Context.ACTIVITY_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

/**
 * Created by fujiayi on 2017/6/21.
 */

public class SimpleWakeupListener implements IWakeupListener {

    private static final String TAG = "SimpleWakeupListener";
    private MainActivity mainActivity ;
    public SimpleWakeupListener(MainActivity mainActivity) {
        this.mainActivity= mainActivity;
    }

    public SimpleWakeupListener() {

    }

    @Override
    public void onSuccess(String word, WakeUpResult result) {
        MyLogger.info(TAG, "唤醒成功，唤醒词：" + word);
        //TODO 判断唤醒词
        if ("小度你好".equals(word)){
            System.out.println("shang xia ");
            mainActivity.start();
//            mainActivity.landing();

        }else if("百度一下".equals(word)){
            System.out.println("ZUO YOU ---------------");
        }else if ("向左转向".equals(word)){
//            mainActivity.landing();
        }else if ("向右转向".equals(word)){

        }else if ("向左转向".equals(word)){

        }else if ("向前飞行".equals(word)){

        }else if ("向后飞行".equals(word)){

        }else if ("最美的女生".equals(word)){

        }else if ("礼物出现".equals(word)){

        }else if ("立刻降落".equals(word)){

        }else if ("拍照模式".equals(word)){

        }
        //TODO
    }

    @Override
    public void onStop() {
        MyLogger.info(TAG, "唤醒词识别结束：");
    }

    @Override
    public void onError(int errorCode, String errorMessge, WakeUpResult result) {
        MyLogger.info(TAG, "唤醒错误：" + errorCode + ";错误消息：" + errorMessge + "; 原始返回" + result.getOrigalJson());
    }

    @Override
    public void onASrAudio(byte[] data, int offset, int length) {
        MyLogger.error(TAG, "audio data： " + data.length);
    }

}
