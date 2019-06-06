package com.example.sm_pc.trial1;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaExtractor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {
    EditText id, pwd;
    String sid, spwd;
    Button login, join;
    CheckBox check;
    SharedPreferences loginPreferences;
    SharedPreferences.Editor loginPrefsEditor;
    Boolean saveLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.CAMERA}, 0);
        }

        id = (EditText)findViewById(R.id.emailinput);
        pwd = (EditText)findViewById(R.id.passwordInput);

        check = (CheckBox)findViewById(R.id.checkBox);
        loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEditor = loginPreferences.edit();

        saveLogin = loginPreferences.getBoolean("saveLogin", false);
        if (saveLogin == true) {
            id.setText(loginPreferences.getString("username", ""));
            pwd.setText(loginPreferences.getString("password", ""));
            check.setChecked(true);
        }
    }

    public void bt_login(View view)
    {
        Log.e("su","siu");
        sid = id.getText().toString();
        spwd = pwd.getText().toString();

        if (check.isChecked()) {
            loginPrefsEditor.putBoolean("saveLogin", true);
            loginPrefsEditor.putString("username", sid);
            loginPrefsEditor.putString("password", spwd);
            loginPrefsEditor.commit();
        } else {
            loginPrefsEditor.clear();
            loginPrefsEditor.commit();
        }

        loginDB ldb = new loginDB();
        ldb.execute();
    }

    public void bt_join(View view)
    {
        Log.e("su","siu");
        sid = id.getText().toString();
        spwd = pwd.getText().toString();

        registDB rdb = new registDB();
        rdb.execute();
    }

    public class loginDB extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... unused) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("u_id",sid);
                jsonObject.put("u_pw",spwd);

                URL url = new URL("http://203.153.147.28:8080/androidLogin/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                Log.e("su","wait");
                conn.connect();
                Log.e("su","connect");


                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsonObject.toString());
                os.flush();
                os.close();
                Log.e("su","out");


                /* 서버 -> 안드로이드 파라메터값 전달 */
                InputStream is = null;
                BufferedReader in = null;
                String data = "";

                is = conn.getInputStream();
                in = new BufferedReader(new InputStreamReader(is), 8 * 1024);
                String line = null;
                StringBuffer buff = new StringBuffer();
                while ( ( line = in.readLine() ) != null )
                {
                    Log.e("su","in");

                    buff.append(line + "\n");
                }
                data = buff.toString().trim();
                Log.e("RECV DATA",data);
                //1 : 비밀번호 오류  2:로그인 성공  3:아이디가 없다
                if(data.equals("2")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "비밀번호 오류", Toast.LENGTH_LONG).show();
                        }
                    });
                }else if(data.equals("1")){
                    Intent intent = new Intent(
                            getApplicationContext(), // 현재 화면의 제어권자
                            MainActivity.class); // 다음 넘어갈 클래스 지정
                    intent.putExtra("user_name",sid);
                    startActivity(intent);
                }
                else if(data.equals("3")){
                    Log.e("gg","SKSMS 3DLEK.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "아이디가 없습니다.", Toast.LENGTH_LONG).show();
                        }
                    });
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (JSONException e){
                e.printStackTrace();
            }

            return null;
        }

    }


    public class registDB extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... unused) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("u_id",sid);
                jsonObject.put("u_pw",spwd);

                URL url = new URL("http://203.153.147.28:8080/androidRegister/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                Log.e("su","wait");
                conn.connect();
                Log.e("su","connect");


                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsonObject.toString());
                os.flush();
                os.close();
                Log.e("su","out");


                /* 서버 -> 안드로이드 파라메터값 전달 */
                InputStream is = null;
                BufferedReader in = null;
                String data = "";

                is = conn.getInputStream();
                in = new BufferedReader(new InputStreamReader(is), 8 * 1024);
                String line = null;
                StringBuffer buff = new StringBuffer();
                while ( ( line = in.readLine() ) != null )
                {
                    Log.e("su","in");

                    buff.append(line + "\n");
                }
                data = buff.toString().trim();
                Log.e("RECV DATA",data);
                if (data == "register success"){
                    Log.e("RECV DATA","success");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "회원가입 성공", Toast.LENGTH_LONG).show();
                        }
                    });
//                    Toast.makeText(LoginActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                }else{
                    Log.e("RECV DATA","fail");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "회원가입 실패", Toast.LENGTH_LONG).show();
                        }
                    });
//                    Toast.makeText(LoginActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                }
                //아이디 중복되는 기능 후에 추가

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (JSONException e){
                e.printStackTrace();
            }

            return null;
        }

    }
}