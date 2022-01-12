package kr.co.kimsubin.raspberry_pi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    Button send_button, ok_button;
    TextView send_textView;
    TextView read_textView;

    private Socket client;
    private DataOutputStream dataOutput;
    private DataInputStream dataInput;
    private static String SERVER_IP = "192.168.137.102";
    private static String CONNECT_MSG = "connect";
    private static String STOP_MSG = "stop";

    private static int BUF_SIZE = 1024;
    static final int SMS_RECEIVE_PERMISSON=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ok_button = findViewById(R.id.ok_button);
        send_button = findViewById(R.id.send_button);
        send_textView = findViewById(R.id.send_textView);
        read_textView = findViewById(R.id.read_textView);

        sms();  // 문자전송 권한 요청
        call();  // 전화걸기 권한 요청

        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Connect connect = new Connect();
                connect.execute(CONNECT_MSG);
            }
        });

    }

    private class Connect extends AsyncTask<String, String, Void> {
        private String output_message;
        private String input_message;

        @Override
        protected Void doInBackground(String... strings) {
            try {
                client = new Socket(SERVER_IP, 9995);
                dataOutput = new DataOutputStream(client.getOutputStream());
                dataInput = new DataInputStream(client.getInputStream());
                output_message = strings[0];
                dataOutput.writeUTF(output_message);

            } catch (UnknownHostException e) {
                String str = e.getMessage().toString();
                Log.w("discnt", str + " 1");
            } catch (IOException e) {
                String str = e.getMessage().toString();
                Log.w("discnt", str + " 2");
            }

            while (true) {
                try {
                    byte[] buf = new byte[BUF_SIZE];
                    int read_Byte = dataInput.read(buf);
                    input_message = new String(buf, 0, read_Byte);

                    ok_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                client.close();  // 클라이언트 종료
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(getApplicationContext(), "보호자 확인 되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });

                    if (input_message.equals("1")) {  // 서버에서 전송받은 값이 1일 경우 : 아이가 있을 경우
                        Handler mHandler = new Handler(Looper.getMainLooper());
                        mHandler.postDelayed(new Runnable() {  // 일정한 시간 뒤에 실행시키고 싶을때
                            @Override
                            public void run() {
                                // 문자전송
                                Toast.makeText(getApplicationContext(), "보호자가 확인되지 않았습니다.", Toast.LENGTH_SHORT).show();
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage("555-521-5554", null, "차량에 아이가 남아있습니다!", null, null);
                                Toast.makeText(getApplicationContext(), "문자전송 완료", Toast.LENGTH_SHORT).show();
                            }
                        }, 0);  // 바로 실행

                    } else if (input_message.equals("8")) {  // 서버에서 전송받은 값이 8일 경우 : 아이가 아직 차안에 있을 경우
                        try {
                            Handler mHandler = new Handler(Looper.getMainLooper());
                            mHandler.postDelayed(new Runnable() {  // 일정한 시간 뒤에 실행시키고 싶을때
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "보호자가 확인 되지 않았습니다.", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:119"));
                                    startActivity(intent);
                                    Toast.makeText(getApplicationContext(), "긴급센터에 연락합니다.", Toast.LENGTH_SHORT).show();
                                    try {
                                        client.close();  // 클라이언트 종료
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 2000);  // 2초뒤 실행
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (!input_message.equals(STOP_MSG)) {
                        publishProgress(input_message);
                    } else {
                        break;
                    }

                    Thread.sleep(1);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... params) {
            send_textView.setText(""); // Clear the chat box
            send_textView.append("보낸 메세지 : " + output_message);
            read_textView.setText(""); // Clear the chat box
            read_textView.append("받은 메세지 : " + params[0]);
        }
    }

    ////////////////////문자////////////////////
    public void sms(){
        int permissonCheck= ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);

        //AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        if (permissonCheck == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "SMS권한 있음", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "SMS권한 없음", Toast.LENGTH_SHORT).show();
        /* 권한설정 dialog에서 거부를 누르면 ActivityCompat.shouldShowRequestPermissionRationale 메소드의 반환값이 true가 됨
        단, 사용자가 "Don't ask again"을 체크한 경우 거부하더라도 false를 반환하여, 직접 사용자가 권한을 부여하지 않는 이상, 권한을 요청할 수 없게 됨 */
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_RECEIVE_PERMISSON);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_RECEIVE_PERMISSON);
            }
        }
    }

    ////////////////////전화////////////////////
    public void call(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionResult = checkSelfPermission(Manifest.permission.CALL_PHONE);

            if (permissionResult == PackageManager.PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("권한 요청").setMessage("이 앱을 사용하기 위해서는 \"전화걸기\" 권한이 필요합니다. 계속 하시겠습니까?").setPositiveButton("네", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // CALL_PHONE 권한을 Android OS에 요청한다.
                                requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1000);
                            }
                        }
                    }).setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            moveTaskToBack(true);  // 태스크를 백그라운드로 이동
                            finishAndRemoveTask();  // 액티비티 종료 + 태스크 리스트에서 지우기
                            android.os.Process.killProcess(android.os.Process.myPid());	 // 앱 프로세스 종료
                        }
                    }).create().show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1000);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // 문자
        switch (requestCode) {
            case SMS_RECEIVE_PERMISSON:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "SMS권한 승인함", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "SMS권한 거부함", Toast.LENGTH_SHORT).show();
                    moveTaskToBack(true);  // 태스크를 백그라운드로 이동
                    finishAndRemoveTask();  // 액티비티 종료 + 태스크 리스트에서 지우기
                    android.os.Process.killProcess(android.os.Process.myPid());	 // 앱 프로세스 종료
                }
                break;
        }

        // 전화
        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:01074626013")); // Add Check Permission
                Toast.makeText(getApplicationContext(), "CALL권한 승인함", Toast.LENGTH_SHORT).show();
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    startActivity(intent);
                }
            } else {
                Toast.makeText(MainActivity.this, "CALL권한 거부함", Toast.LENGTH_SHORT).show();
                moveTaskToBack(true);  // 태스크를 백그라운드로 이동
                finishAndRemoveTask();  // 액티비티 종료 + 태스크 리스트에서 지우기
                android.os.Process.killProcess(android.os.Process.myPid());	 // 앱 프로세스 종료
            }
        }
    }

}
