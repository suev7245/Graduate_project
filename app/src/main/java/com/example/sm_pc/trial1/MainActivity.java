package com.example.sm_pc.trial1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import butterknife.BindView;
import butterknife.ButterKnife;

import android.speech.tts.TextToSpeech;
import android.widget.ToggleButton;

import java.util.HashMap;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
    private Activity activity;
    private final String TAG = "LiveFeed";
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    ServerSocket serverSocket;

    boolean hasConnection = false;
    //    private decodeYUV420SP yuvtorgb;
    static int[] myPixels;
    static int[] rgb;
    @BindView(R.id.camera_preview)
    FrameLayout camPreview;
    BufferedReader in;
    String fromMsg;
    DataOutputStream oStream = null;
    Socket mSocket = null;
    byte[] imageData;
    MediaCodec codec;
    String example = "Convert Java String";
    ToggleButton togglebtn;
    byte[] d = example.getBytes();

    String lane_flag = "false";
    boolean changed = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        startActivity(new Intent(this, SplashActivity.class));
        activity = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        togglebtn = (ToggleButton)findViewById(R.id.lane_toggle_btn);

        togglebtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true){
                    Toast.makeText(MainActivity.this, "Lane Detection-ON", Toast.LENGTH_SHORT).show();
                    lane_flag = "true";
                    changed = true;
//                    if ((oStream != null) && (hasConnection)) {
//                        try {
//                            oStream.writeUTF(lane_flag);
//                            oStream.flush();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }

                } else {
                    Toast.makeText(MainActivity.this, "Lane Detection-OFF", Toast.LENGTH_SHORT).show();
                    lane_flag = "false";
                    changed = true;
//                    if ((oStream != null) && (hasConnection)) {
//                        try {
//                            oStream.writeUTF(lane_flag);
//                            oStream.flush();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
                }
            }
        });

        ServerSocketThread startServer = new ServerSocketThread();
        startServer.start();

        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(this, mCamera);
        camPreview.addView(mCameraPreview);

        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        //params.setPictureSize(320, 240);
        //params.setPreviewSize(320, 240);
        params.setPreviewFrameRate(15);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewCallback(mPreview);

    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            try {
                oStream.close();
                mSocket.close();
//                encoder.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
    }

    public Camera.PreviewCallback mPreview = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            int format = mCamera.getParameters().getPreviewFormat();
            if (format == ImageFormat.NV21 || format == ImageFormat.NV16 || format == ImageFormat.YUY2) {
                int width = mCamera.getParameters().getPreviewSize().width;
                int height = mCamera.getParameters().getPreviewSize().height;
                Log.d("ImageFormat?1", String.valueOf(ImageFormat.NV21));
                Log.d("ImageFormat?2", String.valueOf(format));

//                encoder = new AvcEncoder();
//                encoder.init(width, height, mCamera.getParameters().getPreviewFrameRate(), 125000);
//                imageData = encoder.offerEncoder(bytes);
//                System.arraycopy(imageData,0,encoder.offerEncoder(bytes),0, encoder.offerEncoder(bytes).length);
//                encoder.close();
//                imageData = encoder.offerEncoder(bytes);
//                int len =encData.length;

//                compreeToJpeg 이용
                YuvImage yuvImage = new YuvImage(bytes, format, width, height, null);
                Rect rect = new Rect(0, 0, width, height);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(rect, 85, outputStream);
                imageData = outputStream.toByteArray();
                if ((oStream != null) && (hasConnection)) {
                    try {
                        //oStream.write(encData);
                        //oStream.writeBytes(String.valueOf(imageData.length));
                        //oStream.writeBytes(" ");
                        if(changed==true) {
                            oStream.writeUTF(lane_flag);
                            oStream.flush();
                            changed = false;
                        }
                        oStream.write(imageData);
                        oStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }



//                Log.d("success", "success");


            }
        }
    };


    public class ServerSocketThread extends Thread {
        @Override
        public void run() {
            try {
//                mSocket = new Socket("203.153.146.92", 8080);
//                serverSocket = new ServerSocket(SOCKET_SERVER_PORT);
                mSocket = new Socket("192.168.0.13", 8080);

                while (mSocket != null) {
                    oStream = new DataOutputStream(mSocket.getOutputStream());
                    hasConnection = true;

                    in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    Log.d("message",in.readLine());

//                    InputStream inputStream = mSocket.getInputStream();
//                    byte[] buffer = new byte[1024];
//                    ByteArrayOutputStream byteArrayOutputStream =
//                            new ByteArrayOutputStream(1024);
//                    int bytesRead;
//                    String response="";
//
//                    Log.d("ready","ready");
//
//                    if((bytesRead = inputStream.read(buffer)) != -1){
//                        byteArrayOutputStream.write(buffer, 0, bytesRead);
//                        response = byteArrayOutputStream.toString("UTF-8");
//                    }
//
//                    Log.d("message",response);

//                    try{
//                        while(true){
//                            final String data = in.readLine();
//                            Log.d("message",data);
////                            int bytesRead= inputStream.read(buffer);
////                            Log.d("ready","Ready");
//
//                        }
//                    }catch (Exception e){
//
//                    }
//                    if ((oStream != null) && (hasConnection)) {
//                        try {
//                            //oStream.write(encData);
//                            //oStream.writeBytes(String.valueOf(imageData.length));
//                            //oStream.writeBytes(" ");
//                            oStream.write(imageData);
//                            oStream.flush();
//
////                            in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
////                            String data = in.readLine();
////                            Log.d("readfrom", data);
//
////                            Log.d("readfrom", in.readLine());
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }


//                    in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
//
//                    Log.d("message**", in.readLine());
//                    if ((oStream != null) && (hasConnection)) {
//                        try {
//                            //oStream.write(encData);
//                            //oStream.writeBytes(String.valueOf(imageData.length));
//                            //oStream.writeBytes(" ");
//                            oStream.write(imageData);
//                            oStream.flush();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
                }

            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}