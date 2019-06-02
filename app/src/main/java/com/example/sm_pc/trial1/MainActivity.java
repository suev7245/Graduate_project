package com.example.sm_pc.trial1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.location.LocationManager;
import android.media.MediaCodec;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;
import android.widget.ToggleButton;
import org.ankit.gpslibrary.ADLocation;
import org.ankit.gpslibrary.MyTracker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class Box_point{
    Float box_x1, box_y1, box_x2, box_y2;
}

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
    private Activity activity;
    private final String TAG = "LiveFeed";
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    ServerSocket serverSocket;
    AvcEncoder encoder;

    int height, width=0;
    int real_width, real_height;
    float width_ratio, height_ratio;

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
    boolean removeline = false;
    boolean name_send = true;

    String lane_change = null;
    Float left_x1,left_y1,left_x2,left_y2;
    Float right_x1, right_y1, right_x2,right_y2;
    Box_point bp[] = new Box_point[10];
    Box boxes[] = new Box[10];
    Line line, line2;
    String user;
    String weather;

    Double lati, longi;
    String address;
    JSONObject data = null;

    int frame_cnt = 0;
    byte[] frame_array;
    int arraysize = (int)Math.pow(2, 32); //3200만

    int preview_cnt=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        startActivity(new Intent(this, SplashActivity.class));
        Intent intent = getIntent();
        user = intent.getStringExtra("user_name");

        activity = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        real_height = size.x;
        real_width = size.y;

        line = new Line(this);
        line2 = new Line(this);
        for(int i=0;i<10;i++){
            boxes[i] = new Box(this);
            bp[i] = new Box_point();
        }

        togglebtn = (ToggleButton)findViewById(R.id.lane_toggle_btn);
        togglebtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true){
                    togglebtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.toggle1));
                    Toast.makeText(MainActivity.this, "고속도로 주행 모드", Toast.LENGTH_SHORT).show();
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
                    togglebtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.toggle2));
                    Toast.makeText(MainActivity.this, "시내 주행 모드", Toast.LENGTH_SHORT).show();
                    lane_flag = "false";
                    changed = true;
                    removeline = true;
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

        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( MainActivity.this, new String[] {  android.Manifest.permission.ACCESS_FINE_LOCATION  },
                    0 );
        }else{
            GPSThread gpsThread = new GPSThread();
            gpsThread.start();
        }

        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(this, mCamera);
        camPreview.addView(mCameraPreview);

        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//        params.setPictureSize(1920,1080);
        //params.setPreviewSize(320, 240);
        params.setPreviewFrameRate(45);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewCallback(mPreview);

    }

    public void getWeather(final double lati, final double longi){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    URL url = new URL("http://api.openweathermap.org/data/2.5/weather?lat="+Double.toString(lati)+"&lon="+Double.toString(longi)+"&APPID=782aedbfe88ac08f3d018b654179e903");

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    StringBuffer json = new StringBuffer(1024);
                    String tmp = "";

                    while((tmp = reader.readLine()) != null)
                        json.append(tmp).append("\n");
                    reader.close();

                    data = new JSONObject(json.toString());

                    if(data.getInt("cod") != 200) {
                        Log.e("my weather received","Cancelled");
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void Void) {
                try {
                    if(data!=null){
                        JSONArray w = data.getJSONArray("weather");
                        Log.e("weather", w.toString());
                        String wt = w.getJSONObject(0).getString("main");
                        weather = "weather="+wt+"=weatherend";
                        Log.e("weather", weather);

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private class GPSThread extends Thread {
        final LocationManager lm;
        public GPSThread() {
            lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        }

        public void run() {
            Log.i("스레드","시작했다");
            while(true){
                try {
                    Log.e("jeesangcamp","while try");
                    new MyTracker(getApplicationContext(),gpsLocationListener).track();
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Log.e("jeesangcamp","GPSThread error catch");
                    e.printStackTrace();
                }
            }
        }
    }
    final MyTracker.ADLocationListener gpsLocationListener = new MyTracker.ADLocationListener() {
        @Override
        public void whereIAM(ADLocation loc) {
            lati = loc.lat;
            longi = loc.longi;
            getWeather(lati, longi);

            address = "address=" + Double.toString(loc.lat) + "," + Double.toString(loc.longi) + "=addressend";
            Log.e("jeesangcamp",address);
        }
    };

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
                if(width==0){
                    width = mCamera.getParameters().getPreviewSize().width;
                    height = mCamera.getParameters().getPreviewSize().height;
                    Log.e("dd",String.valueOf(width));
                    Log.e("dd",String.valueOf(height));

                    width_ratio = (float)real_width / width;
                    height_ratio = (float)real_height/height;
                    Log.d("ImageFormat?1", String.valueOf(ImageFormat.NV21));
                    Log.d("ImageFormat?2", String.valueOf(format));
                }

//               encoder = new AvcEncoder();
//                encoder.init(width, height, mCamera.getParameters().getPreviewFrameRate(), 125000);
//                imageData = encoder.inputEncoder(bytes);

//                System.arraycopy(imageData,0,encoder.offerEncoder(bytes),0, encoder.offerEncoder(bytes).length);
//                encoder.close();
//                imageData = encoder.offerEncoder(bytes);
//                int len =encData.length
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
                        if(name_send == true){
                            oStream.writeUTF("user="+user+"=userend");
                            oStream.flush();
                            name_send = false;
                        }
                        if(weather!= null) {
                            oStream.writeUTF(address);
                            oStream.writeUTF(weather);
                            Log.e("jeesangcampgps", address);
//                        oStream.writeDouble(lati);
//                        oStream.writeDouble(longi);
                            oStream.flush();
                        }
                        oStream.write(imageData);
                        oStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };


    public class ServerSocketThread extends Thread {
        @Override
        public void run() {
            try {
                mSocket = new Socket("203.153.147.28", 80);
//                serverSocket = new ServerSocket(SOCKET_SERVER_PORT);
//                mSocket = new Socket("172.30.119.42", 8080);

                while (mSocket != null) {
                    oStream = new DataOutputStream(mSocket.getOutputStream());
                    hasConnection = true;
                    in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    final int bp_num;

                    String temp = in.readLine();
                    Log.e("jeesangcamp", temp);
                    if(temp.equals("lane")) {
                        Log.e("jeesangcamp", "레인그릴");
                        Log.e("jeesangcamp", lane_change = in.readLine());
                        left_x1 = Float.valueOf(in.readLine());
                        left_y1 = Float.valueOf(in.readLine());
                        left_x2 = Float.valueOf(in.readLine());
                        left_y2 = Float.valueOf(in.readLine());
                        right_x1 = Float.valueOf(in.readLine());
                        right_y1 = Float.valueOf(in.readLine());
                        right_x2 = Float.valueOf(in.readLine());
                        right_y2 = Float.valueOf(in.readLine());
                        Log.e("jeesangcamp", Float.toString(left_x1) + Float.toString(right_y2));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (lane_change.equals("change")) {
                                    Toast.makeText(MainActivity.this, "차선변경", Toast.LENGTH_LONG).show();
                                    MediaPlayer m = MediaPlayer.create(MainActivity.this, R.raw.lane_beep);
                                    m.start();
                                }
                                if (line.getParent() != null) {
                                    ((ViewGroup) line.getParent()).removeView(line);
                                    ((ViewGroup) line2.getParent()).removeView(line2);
                                }
                                line.getLocation((height - left_y1) * height_ratio, left_x1 * width_ratio, (height - left_y2) * height_ratio, left_x2 * width_ratio);
                                addContentView(line, new WindowManager.LayoutParams(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.FILL_PARENT));
                                line2.getLocation((height - right_y1) * height_ratio, right_x1 * width_ratio, (height - right_y2) * height_ratio, right_x2 * width_ratio);
                                addContentView(line2, new WindowManager.LayoutParams(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.FILL_PARENT));
                            }
                        });
                    }else if(temp.equals("noobject")){
                        if (boxes[0].getParent() != null){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for(int i=0; i<10 ; i++){
                                        if (boxes[i].getParent() != null) {
                                            ((ViewGroup) boxes[i].getParent()).removeView(boxes[i]);
                                        }
                                    }
                                }
                            });
                        }
                    }else{
                        Log.e("jeesangcamp","박스그릴");
                        bp_num = Integer.valueOf(temp);
                        for(int i =0; i<bp_num; i++){
                            String str = in.readLine();
                            String boxes[] = str.split(",");
                            bp[i].box_x1 = Float.valueOf(boxes[0]);
                            bp[i].box_y1 = Float.valueOf(boxes[1]);
                            bp[i].box_x2 = Float.valueOf(boxes[2]);
                            bp[i].box_y2 = Float.valueOf(boxes[3]);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                for(int i=0; i<10 ; i++){
                                    if (boxes[i].getParent() != null) {
                                        ((ViewGroup) boxes[i].getParent()).removeView(boxes[i]);
                                    }
                                }
                                for(int i=0; i<bp_num; i++){
                                    boxes[i].getLocation(height_ratio*(height - bp[i].box_x2),  width_ratio*bp[i].box_y1, height_ratio*(height - bp[i].box_x1), width_ratio*bp[i].box_y2);
//                                        boxes[i].getLocation((height - bp[i].box_y1)*height_ratio, bp[i].box_x1*width_ratio, (height - bp[i].box_y2)*height_ratio, bp[i].box_x2*width_ratio);
//                                        boxes[i].getLocation(1521.0f,33.0f,15.0f,1845.0f);
                                    addContentView(boxes[i], new WindowManager.LayoutParams(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.FILL_PARENT));
                                }
                            }
                        });
                    }
                    if(removeline==true){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (line.getParent() != null) {
                                    ((ViewGroup) line.getParent()).removeView(line);
                                    ((ViewGroup) line2.getParent()).removeView(line2);
                                }
                                removeline = false;
                            }
                        });
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}