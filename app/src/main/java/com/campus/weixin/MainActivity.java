package com.campus.weixin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;
import com.baidu.aip.asrwakeup3.core.params.CommonRecogParams;
import com.baidu.aip.asrwakeup3.core.params.OfflineRecogParams;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.baidu.aip.asrwakeup3.core.util.MyLogger;
import com.baidu.aip.asrwakeup3.core.wakeup.MyWakeup;
import com.baidu.aip.asrwakeup3.core.wakeup.listener.IWakeupListener;
import com.baidu.aip.asrwakeup3.core.wakeup.listener.SimpleWakeupListener;
import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.baidu.aip.face.AipFace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import androidx.core.app.ActivityCompat;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.mission.followme.FollowMeHeading;
import dji.common.mission.followme.FollowMeMission;
import dji.common.mission.followme.FollowMeMissionEvent;
import dji.common.mission.followme.FollowMeMissionState;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointMissionEvent;
import dji.common.mission.hotpoint.HotpointMissionState;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.model.LocationCoordinate2D;
import dji.common.product.Model;
import dji.common.realname.AircraftBindingState;
import dji.common.realname.AppActivationState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flighthub.FlightHubManager;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.activetrack.ActiveTrackOperator;
import dji.sdk.mission.followme.FollowMeMissionOperator;
import dji.sdk.mission.followme.FollowMeMissionOperatorListener;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.mission.hotpoint.HotpointMissionOperatorListener;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.TimelineMission;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.HotpointAction;
import dji.sdk.products.Aircraft;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {

    private static final String TAG = MainActivity.class.getName();
    protected Button loginBtn;
    protected Button logoutBtn;
    protected TextView bindingStateTV;
    protected TextView appActivationStateTV;
    private AppActivationManager appActivationManager;
    private AppActivationState.AppActivationStateListener activationStateListener;
    private AircraftBindingState.AircraftBindingStateListener bindingStateListener;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
    private ToggleButton mRecordBtn;
    private TextView recordingTime;

    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    private Handler handler;


    private FlightController mFlightController;
    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    private HotpointMissionOperator hotpointMissionOperator;

    /*
     * 本Activity中是否需要调用离线命令词功能。根据此参数，判断是否需要调用SDK的ASR_KWS_LOAD_ENGINE事件
     */
    protected boolean enableOffline;
    /**
     * 识别控制器，使用MyRecognizer控制识别的流程
     */
    protected MyRecognizer myRecognizer;

    /*
     * Api的参数类，仅仅用于生成调用START的json字符串，本身与SDK的调用无关
     */
    private  CommonRecogParams apiParams;

    Button takeoff;
    Button land;
    Button turn_left;
    Button turn_right;
    Button forward;
    Button backward;
    Button hotpoint;
    Button takephoto;
    Button enableVD;
    Button disableVD;
    Button followstart;
    Button followend;
    Button up;
    Button closegirl;

    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            mPitch = 0;
            mRoll = 0;
            mYaw = 0;
            mThrottle = 0;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initUI();
        initData();

        //语音识别监听开始
        wpspeach();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
        Camera camera = DemoApplication.getCameraInstance();

        if (camera != null) {

            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                recordingTime.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });

        }
        //人脸client
        client = new AipFace(APP_ID, API_KEY, SECRET_KEY);
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        handler = new Handler(Looper.getMainLooper());

        //开启定位 为了followme
        startLocationService();
        // 基于DEMO集成第1.1, 1.2, 1.3 步骤 初始化EventManager类并注册自定义输出事件
        // DEMO集成步骤 1.2 新建一个回调类，识别引擎会回调这个类告知重要状态和识别结果
        IRecogListener listener = new MessageStatusRecogListener(handler);
        // DEMO集成步骤 1.1 1.3 初始化：new一个IRecogListener示例 & new 一个 MyRecognizer 示例,并注册输出事件
        myRecognizer = new MyRecognizer(this, listener);
        if (enableOffline) {
            // 基于DEMO集成1.4 加载离线资源步骤(离线时使用)。offlineParams是固定值，复制到您的代码里即可
            Map<String, Object> offlineParams = OfflineRecogParams.fetchOfflineParams();
            myRecognizer.loadOfflineEngine(offlineParams);
        }
        apiParams = new OfflineRecogParams();
        apiParams.initSamplePath(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Log.e("图灵第二","thread start facetogirl " + facetogirl);
                while(true) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (facetogirl) {

                        Log.e("图灵第二", "facetogirl 1111" + facetogirl);
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (null == mSendVirtualStickDataTimer) {
//                                    Log.e("图灵第二","facetogirl " + " mSendVirtualStickDataTimer");
//
//                                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
//                                    mSendVirtualStickDataTimer = new Timer();
//                                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
//                                }
//                            }
//                        });

                        Log.e("图灵第二", "截取一下 1");

                        //4：3的摄像头。截取一下小的
                        Bitmap captureBitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888);
                        mVideoSurface.getBitmap(captureBitmap);
                        Log.e("图灵第二", "截取一下 2");

                        String img = bitmapToBase64(captureBitmap);
                        Log.e("图灵第二", "截取一下 3");

                        HashMap<String, String> options = new HashMap<String, String>();
                        options.put("face_field", "age");
                        options.put("max_face_num", "2");
                        options.put("face_type", "LIVE");
                        JSONObject res = client.detect(img, "BASE64", options);
                        Log.e("图灵第二", "detect " + res);

                        try {
                            //todo 人脸识别 判断最大的两个人脸是否找到女生人脸

                            //读取女生头像位置大小，计算距离
                            res = res.getJSONObject("result");
                            JSONArray face_list = (JSONArray) res.get("face_list");
                            //获取面积最大的人脸
                            JSONObject girl = (JSONObject) face_list.get(0);
                            JSONObject face_location = (JSONObject) girl.get("location");
                            double height = face_location.getDouble("height");
                            //操作飞机远近，调整到合适的距离
                            Log.e("图灵第二", "height " + height);

                            if (height < 20) {
                                //3m以外

                                mRoll = 2;

                            } else if (height >= 20 && height <= 31) {
                                //2m-3m
                                mRoll = 1;

                            } else if (height > 31 && height < 56) {
                                //1m-2m
                                mRoll = 0.5f;

                            } else if (height >= 56) {
                                //1m以内
                                mRoll = 0;
                                facetogirl = false;
                            } else {
                                facetogirl = false;
                                mRoll = 0;
                            }

                            //todo 根据三维角度控制无人机修正方向

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }).start();

    }


    public static final String APP_ID = "你的APP_ID";
    public static final String API_KEY = "你的API_KEY";
    public static final String SECRET_KEY = "你的SECRET_KEY";

    class LandTo170cmDataTask extends TimerTask {

        @Override
        public void run() {
            mThrottle = -1;
            if (mFlightController != null) {
                mFlightController.sendVirtualStickFlightControlData(
                        new FlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

//                                showToast("Mission Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                                float altitude = mFlightController.getState().getAircraftLocation().getAltitude();
                                if (altitude<=1.7){
                                    Log.e("图灵第二", "altitude " + altitude);

                                    //下降位置到了 停止下降
                                    mThrottle = 0;
                                    //开始靠近
                                    facetogirl = true;
                                    Log.e("图灵第二", "facetogirl " + facetogirl);

                                    //取消当前任务完成
                                    cancel();
                                }
                            }
                        }
                );
            }
        }
    }


    AipFace client;
    Timer to170cmTimer;

    boolean facetogirl = false;

    private void DynamicFindGirl  (){
        Log.e("图灵第二", "DynamicFindGirl call");
        if (to170cmTimer == null){
            to170cmTimer = new Timer();
        }
        LandTo170cmDataTask landTo170cmDataTask = new LandTo170cmDataTask();
        to170cmTimer.schedule(landTo170cmDataTask,100,200);

    }



    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            showmsg();
        }
    };
    public static Bitmap base64ToBitmap(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private void showmsg() {
        boolean ret = false;
        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null) {
            if(product.isConnected()) {
                //The product is connected
                showToast(DemoApplication.getProductInstance().getModel() + " Connected");
                ret = true;
            } else {
                if(product instanceof Aircraft) {
                    Aircraft aircraft = (Aircraft)product;
                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        // The product is not connected, but the remote controller is connected
                        showToast("only RC Connected");
                        ret = true;
                    }
                }
            }
        }

        if(!ret) {
            // The product or the remote controller are not connected.
            showToast("Disconnected");
        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        setUpListener();
        super.onResume();
        initPreviewer();
        onProductChange();
        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
        initFlightController();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        tearDownListener();
        uninitPreviewer();
        unregisterReceiver(mReceiver);
        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
        super.onDestroy();
    }

    private void initUI(){

        bindingStateTV = (TextView) findViewById(R.id.tv_binding_state_info);
        appActivationStateTV = (TextView) findViewById(R.id.tv_activation_state_info);
        loginBtn = (Button) findViewById(R.id.btn_login);
        logoutBtn = (Button) findViewById(R.id.btn_logout);
        loginBtn.setOnClickListener(this);
        logoutBtn.setOnClickListener(this);

         takeoff = findViewById(R.id.takeoff);
         takeoff.setOnClickListener(this);

         land = findViewById(R.id.land);
         land.setOnClickListener(this);

         turn_right = findViewById(R.id.turn_right);
        turn_right.setOnClickListener(this);

         turn_left = findViewById(R.id.turn_left);
        turn_left.setOnClickListener(this);

         forward = findViewById(R.id.forward);
        forward.setOnClickListener(this);

         backward = findViewById(R.id.backward);
        backward.setOnClickListener(this);

         hotpoint = findViewById(R.id.hotpoint);
        hotpoint.setOnClickListener(this);

         takephoto = findViewById(R.id.takephoto);
        takephoto.setOnClickListener(this);

         enableVD = findViewById(R.id.enableVD);
        enableVD.setOnClickListener(this);

         disableVD = findViewById(R.id.disableVD);
        disableVD.setOnClickListener(this);

        followstart = findViewById(R.id.followstart);
        followstart.setOnClickListener(this);

        followend = findViewById(R.id.followend);
        followend.setOnClickListener(this);

        up = findViewById(R.id.up);
        up.setOnClickListener(this);

        closegirl = findViewById(R.id.closegirl);
        closegirl.setOnClickListener(this);

        //video
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        recordingTime = (TextView) findViewById(R.id.timer);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);

        recordingTime.setVisibility(View.INVISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });

    }


    private void initPreviewer() {

        BaseProduct product = DemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = DemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    protected void onProductChange() {
        initPreviewer();
        loginAccount();
    }

    private void initData(){
        setUpListener();

        appActivationManager = DJISDKManager.getInstance().getAppActivationManager();

        if (appActivationManager != null) {
            appActivationManager.addAppActivationStateListener(activationStateListener);
            appActivationManager.addAircraftBindingStateListener(bindingStateListener);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appActivationStateTV.setText("" + appActivationManager.getAppActivationState());
                    bindingStateTV.setText("" + appActivationManager.getAircraftBindingState());

                }
            });
        }
    }

    private void setUpListener() {
        // Example of Listener
        activationStateListener = new AppActivationState.AppActivationStateListener() {
            @Override
            public void onUpdate(final AppActivationState appActivationState) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appActivationStateTV.setText("" + appActivationState);
                    }
                });
            }
        };

        bindingStateListener = new AircraftBindingState.AircraftBindingStateListener() {

            @Override
            public void onUpdate(final AircraftBindingState bindingState) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bindingStateTV.setText("" + bindingState);
                    }
                });
            }
        };
    }

    private void tearDownListener() {
        if (activationStateListener != null) {
            appActivationManager.removeAppActivationStateListener(activationStateListener);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appActivationStateTV.setText("Unknown");
                }
            });
        }
        if (bindingStateListener !=null) {
            appActivationManager.removeAircraftBindingStateListener(bindingStateListener);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bindingStateTV.setText("Unknown");
                }
            });
        }
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    int phonto = 0;
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.takeoff:{
                takeoff();
                break;
            }
            case R.id.land:{
                landing();
                break;
            }
            case R.id.up:{
                mThrottle = 2;

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }
                //之后还原 角速度需要测试具体时间
                handler.postDelayed(runnable,5000);

                break;
            }
            case R.id.turn_left:{
                mYaw = -20;

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }
                //之后还原 角速度需要测试具体时间
                handler.postDelayed(runnable,5000);

                break;
            }
            case R.id.turn_right:{
                mYaw = 20;

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }
                //1000ms之后还原
                handler.postDelayed(runnable,5000);
                break;
            }
            case R.id.forward:{

                mRoll = 1;
                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }
                //每秒1米 走1s
                handler.postDelayed(runnable,1000);

                break;
            }
            case R.id.backward:{
                mRoll = -1;

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }
                //每秒1米 走1s
                handler.postDelayed(runnable,1000);
                break;
            }
            case R.id.hotpoint:{
                hotpointMissionExample();
                break;
            }
            case R.id.takephoto:{
                captureAction();
                break;
            }
            case R.id.btn_login:{
                loginAccount();
                break;
            }
            case R.id.btn_logout:{
                mYaw = 100;

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
                }
                //之后还原 角速度需要测试具体时间
                handler.postDelayed(runnable,5000);
//                logoutAccount();
                break;
            }
            case R.id.btn_capture:{
                captureAction();
                break;
            }
            case R.id.enableVD:{
                showToast("enable VD");
                setViryualStickStatus(true);
                break;
            }
            case R.id.disableVD:{
                showToast("disable VD");
                setViryualStickStatus(false);
                break;
            }

            case R.id.followstart:{
                followMeStart();
                break;
            }
            case R.id.followend:{

                followMeStop();
                break;
            }
            case R.id.btn_shoot_photo_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            }
            case R.id.btn_record_video_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            }
            case R.id.closegirl:{
                DynamicFindGirl();
                break;
            }
            default:
                break;
        }
    }

    /** 保存方法 */
    public void saveBitmap(Bitmap bm,String picName) {
        Log.e(TAG, "保存图片");
        File f = new File("/mnt/sdcard/baidu/", picName);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            Log.i(TAG, "已经保存");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

        Camera camera = DemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
        }
    }

    // Method for taking photo
    private void captureAction(){

        final Camera camera = DemoApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null) {
                                            showToast("take photo: success");
                                        } else {
                                            showToast(djiError.getDescription());
                                        }
                                    }
                                });
                            }
                        }, 2000);
                    }
                }
            });
        }
    }

    // Method for starting recording
    private void startRecord(){

        final Camera camera = DemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if (djiError == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        Camera camera = DemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){

                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        showToast("Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });

    }

    private void logoutAccount(){
        UserAccountManager.getInstance().logoutOfDJIUserAccount(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (null == error) {
                    showToast("Logout Success");
                } else {
                    showToast("Logout Error:"
                            + error.getDescription());
                }
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }


    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void setViryualStickStatus(boolean enable){
        if (mFlightController != null){

            mFlightController.setVirtualStickModeEnabled(enable, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null){
                        showToast(djiError.getDescription());
                    }else
                    {
                        showToast("set Virtual Stick Success : " + enable);
                    }
                }
            });

        }
    }

    private FollowMeMissionOperator followMeMissionOperator;
    private LocationManager locationManager;
    private LocationListener listener;
    private LocationCoordinate2D movingObjectLocation;



    private void startLocationService(){
        if(null == locationManager) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }
        if(null == listener) {
            listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    movingObjectLocation = new LocationCoordinate2D(location.getLatitude(), location.getLongitude());
                }


                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {
                }

                @Override
                public void onProviderEnabled(String s) {
                }

                @Override
                public void onProviderDisabled(String s) {
                    showToast("onProviderDisabled");
                    Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                }
            };

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showToast("Location Permission missing");
                return;
            }
            locationManager.requestLocationUpdates("gps", 1000, 0, listener);
        }
    }


    private void takeoff(){
        showToast("take off call");
        if (mFlightController != null){
            mFlightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    showToast("TakeOFF Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                }
            });
        }

    }

    private void landing(){
        if (mFlightController != null){
            mFlightController.startLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    showToast("landing Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                }
            });
        }

    }


    private void followMeStart(){
        if(null == locationManager) {
            startLocationService();
        }


        if(followMeMissionOperator == null) {
            followMeMissionOperator = DJISDKManager.getInstance().getMissionControl().getFollowMeMissionOperator();
        }
        showToast("Follow State:"+followMeMissionOperator.getCurrentState().toString());
        if (followMeMissionOperator.getCurrentState() == FollowMeMissionState.READY_TO_EXECUTE) {

            float height = (float) KeyManager.getInstance().getValue(FlightControllerKey.create(FlightControllerKey.ALTITUDE));
            double la = (double) KeyManager.getInstance().getValue(FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE));
            double lo = (double) KeyManager.getInstance().getValue(FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE));

            FollowMeMission ffm = new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION, la, lo, 15f);

            followMeMissionOperator.addListener(new FollowMeMissionOperatorListener() {
                @Override
                public void onExecutionUpdate(@NonNull FollowMeMissionEvent followMeMissionEvent) {
                    Log.e("图灵第二", "onExecutionUpdate" + followMeMissionEvent.toString());

                }

                @Override
                public void onExecutionStart() {
                    Log.e("图灵第二", "onExecutionStart");

                }

                @Override
                public void onExecutionFinish(@Nullable DJIError djiError) {
                    Log.e("图灵第二", "onExecutionFinish");

                }
            });
            followMeMissionOperator.startMission(ffm, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.e("图灵第二 ", "Mission Start: onResult " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    startFollowMeTimer();

                }
            });

        }
    }
    private void followMeStop(){
        if(followMeMissionOperator == null) {
            followMeMissionOperator = DJISDKManager.getInstance().getMissionControl().getFollowMeMissionOperator();
        }
        followMeMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                showToast("Mission Stop: " + (djiError == null ? "Successfully" : djiError.getDescription()));
            }
        });

    }

    private void startFollowMeTimer() {
        Timer updateFollowMeTaskTimer;
        updateFollowMe updateFollowMeTask;
        updateFollowMeTask = new updateFollowMe();
        updateFollowMeTaskTimer = new Timer();
        updateFollowMeTaskTimer.schedule(updateFollowMeTask, 1000, 200);
    }
    private class updateFollowMe extends TimerTask {
        @Override
        public void run() {
            if (followMeMissionOperator.getCurrentState() == FollowMeMissionState.EXECUTING) {


                double la = followMeMissionOperator.getFollowingTarget().getLatitude();
                double lo = followMeMissionOperator.getFollowingTarget().getLongitude();

                if (movingObjectLocation != null){
                    la = movingObjectLocation.getLatitude();
                    lo = movingObjectLocation.getLongitude();
                }
                followMeMissionOperator.updateFollowingTarget(new LocationCoordinate2D(la, lo), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        Log.e("图灵第二","updateFollowMe onResult: " + (djiError == null ? "Successfully" : djiError.getDescription()));

                    }
                });
            }
        }
    }
    private void initFlightController() {

        Aircraft aircraft = DemoApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            showToast("Disconnected");
            mFlightController = null;
            return;
        } else {
            mFlightController = aircraft.getFlightController();
            showToast("mFlightController init " + mFlightController);
            mFlightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    showToast("setFlightOrientationMode Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                }
            });
            //RollPitchControlModeAngle 如果是机身坐标系，角度就是机身X、Y轴的旋转角度。取值范围是 - 30 到 30 度。
            //RollPitchControlModeVelocity X、Y 轴的移动速度，取值范围 -15 到 15 米每秒。根据坐标系不同 X、Y 轴的方向不同，前面已经说明过。
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            //AngularVelocity旋转时的角速度，取值范围是 -100 到 100。负数表示逆时针方向旋转。如果你想要飞行器以个指定的角速度匀速旋转，那么应该选择这个模式。
            //Angle 相对于机头的旋转角度，取值范围注意不是 0 - 360，而是 -180 到 180。正的角度表示顺时针旋转，负的角度值表示逆时针。
            mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            //Velocity 调整高度的速度，取值范围是 -4 到 4。正的值表示往高处飞，负数表示下降时的速度。
            //Position 直接设置想要到达的高度值，取值范围是 0 到 500 米。也就是说最高可以让飞行器飞到 500 米的高度。
            mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            mFlightController.getSimulator().setStateCallback(new SimulatorState.Callback() {
                @Override
                public void onUpdate(final SimulatorState stateData) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            String yaw = String.format("%.2f", stateData.getYaw());
                            String pitch = String.format("%.2f", stateData.getPitch());
                            String roll = String.format("%.2f", stateData.getRoll());
                            String positionX = String.format("%.2f", stateData.getPositionX());
                            String positionY = String.format("%.2f", stateData.getPositionY());
                            String positionZ = String.format("%.2f", stateData.getPositionZ());

                            showToast("Yaw : " + yaw + ", Pitch : " + pitch + ", Roll : " + roll + "\n" + ", PosX : " + positionX +
                                    ", PosY : " + positionY +
                                    ", PosZ : " + positionZ);
                        }
                    });
                }
            });
        }
    }



    class SendVirtualStickDataTask extends TimerTask {

        @Override
        public void run() {

            if (mFlightController != null) {
                mFlightController.sendVirtualStickFlightControlData(
                        new FlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
//                                showToast("Mission Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));

                            }
                        }
                );
            }
        }
    }
    public void  hotpointMissionExample(){

        try {
//            FlightHubManager flightHubManager = DJISDKManager.getInstance().getFlightHubManager();

//            flightHubManager.setUploadFlightDataEnabled(true);
            missionControl = DJISDKManager.getInstance().getMissionControl();
            hotpointMissionOperator = DJISDKManager.getInstance().getMissionControl().getHotpointMissionOperator();
            showToast(hotpointMissionOperator.getCurrentState() + "");
            MyLogger.error(TAG, hotpointMissionOperator.getCurrentState() + "");
            if (HotpointMissionState.READY_TO_EXECUTE.equals(hotpointMissionOperator.getCurrentState())) {

                hotpointMissionOperator.addListener(new HotpointMissionOperatorListener() {

                    @Override
                    public void onExecutionUpdate(@NonNull HotpointMissionEvent hotpointMissionEvent) {
                        //TODO
                    }

                    @Override
                    public void onExecutionStart() {
                        //TODO
                    }

                    @Override
                    public void onExecutionFinish(@Nullable DJIError djiError) {
                        //T 悬停
                    }
                });
            }

            //如果没有定位位置，则以飞机当前位置为默认hotpoint
            double la = (double) KeyManager.getInstance().getValue(FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE));
            double lo = (double) KeyManager.getInstance().getValue(FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE));

            if (movingObjectLocation == null){
                movingObjectLocation = new LocationCoordinate2D(la, lo);

            }

            HotpointMission hotpointMission = new HotpointMission();

            //热点位置。
            hotpointMission.setHotpoint(movingObjectLocation);
            //起始点方向
            /**
             *   NORTH(0),
             *     SOUTH(1),
             *     WEST(2),
             *     EAST(3),
             *     NEAREST(4);
             *     开始在轨道上的初始位置
             */

            hotpointMission.setStartPoint(HotpointStartPoint.EAST);
            //半径
            hotpointMission.setRadius(10);
            //飞机角速度（度/秒） 角度
            hotpointMission.setAngularVelocity(10);
            hotpointMission.setAltitude(10);
            //true 飞机是否应围绕热点沿顺时针方向移动。
            hotpointMission.setClockwise(true);
            hotpointMission.setHeading(HotpointHeading.TOWARDS_HOT_POINT);

            List<TimelineElement> elements = new ArrayList<>();
            elements.add(new HotpointAction(hotpointMission, 360));//一圈
//            elements.add(new GoHomeAction());
            missionControl.scheduleElements(elements);
            if (MissionControl.getInstance().scheduledCount() > 0) {
                MissionControl.getInstance().startTimeline();
            } else {
                showToast("Init the timeline first by clicking the Init button");
            }
//            missionControl.addListener(new MissionControl.Listener() {
//                @Override
//                public void onEvent(@Nullable TimelineElement timelineElement, TimelineEvent timelineEvent, @Nullable DJIError djiError) {
//                    updateTimelineStatus(timelineElement, timelineEvent, djiError);
//                    showToast("Trigger " + trigger.getClass().getSimpleName() + " event is " + timelineEvent.name() + (djiError==null? " ":djiError.getDescription()));
//                }
//            });
//
//            hotpointMissionOperator.startMission(hotpointMission, new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onResult(DJIError djiError) {
//                    showToast("Mission Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
//                    Log.d("hotpointMissionExample","Mission Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
//                }
//            });
        } catch(Exception e){
            showToast(e.getMessage());

            e.printStackTrace();
        }


    }
    private MissionControl missionControl;
    private FlightController flightController;
    private TimelineEvent preEvent;
    private TimelineElement preElement;
    private DJIError preError;

    private void updateTimelineStatus(@Nullable TimelineElement element, TimelineEvent event, DJIError error) {

        if (element == preElement && event == preEvent && error == preError) {
            return;
        }

        if (element != null) {
            if (element instanceof TimelineMission) {
                showToast(((TimelineMission) element).getMissionObject().getClass().getSimpleName()
                        + " event is "
                        + event.toString()
                        + " "
                        + (error == null ? "" : error.getDescription()));
            } else {
                showToast(element.getClass().getSimpleName()
                        + " event is "
                        + event.toString()
                        + " "
                        + (error == null ? "" : error.getDescription()));
            }
        } else {
            showToast("Timeline Event is " + event.toString() + " " + (error == null
                    ? ""
                    : "Failed:"
                    + error.getDescription()));
        }

        preEvent = event;
        preElement = element;
        preError = error;
    }
    IWakeupListener iWakeupListener = null;
    MyWakeup myWakeup = null;
    public void  wpspeach(){
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        params.put(com.baidu.speech.asr.SpeechConstant.APP_ID, "你的APP_ID");

        params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");
        if (iWakeupListener==null) {
            iWakeupListener = new SimpleWakeupListener(this);
            myWakeup = new MyWakeup(this, iWakeupListener);
        }
        myWakeup.start(params);

    }
    /**
     * 开始录音，点击“开始”按钮后调用。
     * 基于DEMO集成2.1, 2.2 设置识别参数并发送开始事件
     */
    public void start() {
        // DEMO集成步骤2.1 拼接识别参数： 此处params可以打印出来，直接写到你的代码里去，最终的json一致即可。
        final Map<String, Object> params = fetchParams();
        // params 也可以根据文档此处手动修改，参数会以json的格式在界面和logcat日志中打印
        Log.i(TAG, "设置的start输入参数：" + params);
        // 复制此段可以自动检测常规错误
        (new AutoCheck(getApplicationContext(), new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
                        Log.d("start",message + "\n");
                        ; // 可以用下面一行替代，在logcat中查看代码
                        // Log.w("AutoCheckMessage", message);
                    }
                }
            }
        }, enableOffline)).checkAsr(params);

        // 这里打印出params， 填写至您自己的app中，直接调用下面这行代码即可。
        // DEMO集成步骤2.2 开始识别
        myRecognizer.start(params);
    }
    /**
     * 开始录音后，手动点击“停止”按钮。
     * SDK会识别不会再识别停止后的录音。
     * 基于DEMO集成4.1 发送停止事件 停止录音
     */
    protected void stop() {

        myRecognizer.stop();
    }
    /**
     * 开始录音后，手动点击“取消”按钮。
     * SDK会取消本次识别，回到原始状态。
     * 基于DEMO集成4.2 发送取消事件 取消本次识别
     */
    protected void cancel() {

        myRecognizer.cancel();
    }
    protected Map<String, Object> fetchParams() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        //  上面的获取是为了生成下面的Map， 自己集成时可以忽略
        Map<String, Object> params = apiParams.fetch(sp);
        //  集成时不需要上面的代码，只需要params参数。
        return params;
    }
}
