package com.example.facecheck;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.facecheck.DBHelper.SqliteDBHelper;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainDeskActivity extends AppCompatActivity implements View.OnClickListener {
    private Handler handler;
    private SqliteDBHelper mHelper;
    private MqttClient client;
    private final String host = "tcp://ionmnFFCKzs.iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";
    private final String userName = "adroid&ionmnFFCKzs";
    private final String passWord = "1d5d423e224eda6dff1cf4cab96c150a34d24e7a437fd0d74e017d1f3d76f1e8";
    private final String ClientId = "ionmnFFCKzs.adroid|securemode=2,signmethod=hmacsha256,timestamp=1684918933497|";
    private final String mqtt_sub_topic = "/ionmnFFCKzs/adroid/user/get";
    private final String mqtt_pub_topic = "/ionmnFFCKzs/adroid/user/update";
    private MqttConnectOptions mqttConnectOptions;
    private Boolean flag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();//这种方式默认式亮色主题
        setContentView(R.layout.activity_main_desk);
        TextView m_mqtt = findViewById(R.id.m_mqtt);
        Button Rfid = findViewById(R.id.Rfid);
        Button face_recognition = findViewById(R.id.face_recognition);
        Button password_check = findViewById(R.id.password_check);
        Button exit = findViewById(R.id.exit);
        mHelper = SqliteDBHelper.getInstance(this);
        mHelper.openReadLink();
        mHelper.openWriteLink();
//        mHelper.save(new Userinfo("寇旺琴",1,"女",20,"1",true));
//        mHelper.save(new Userinfo("付诗琴",2,"女",21,"2",true));
//        mHelper.save(new Userinfo("许文萱",3,"女",21,"3",true));

        Mqtt_init();
        startReconnect();

        handler = new Handler(Looper.myLooper()) {
            @SuppressLint("SetTextI18n")
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.d("fu", msg.what + "");
                switch (msg.what) {

                    case 1: //开机校验更新回传
                        break;
                    case 2:  // 反馈回传

                        break;
                    case 3:  //MQTT 收到消息回传
                        System.out.println(msg.obj.toString());   // 显示MQTT数据
                        break;
                    case 31:   //连接成功
                        m_mqtt.setText("连接成功");
                        Rfid.setOnClickListener(MainDeskActivity.this);
                        face_recognition.setOnClickListener(MainDeskActivity.this);
                        password_check.setOnClickListener(MainDeskActivity.this);
                        exit.setOnClickListener(MainDeskActivity.this);

                        try {
                            client.subscribe(mqtt_sub_topic, 1);//订阅
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;

                    case 30:  //连接失败
                        Toast.makeText(MainDeskActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        m_mqtt.setText("连接失败");
                        break;

                    default:
                        break;
                }
            }
        };
    }

    // MQTT初始化
    private void Mqtt_init() {
        try {
            client = new MqttClient(host, ClientId, new MemoryPersistence());
            mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setCleanSession(false);
            mqttConnectOptions.setUserName(userName);
            mqttConnectOptions.setPassword(passWord.toCharArray());
            mqttConnectOptions.setConnectionTimeout(10);
            mqttConnectOptions.setKeepAliveInterval(100);
            //设置回调
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    //连接丢失后，一般在这里面进行重连
                    System.out.println("connectionLost----------");
                    startReconnect();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //publish后会执行到这里
                    System.out.println("deliveryComplete---------"
                            + token.isComplete());
                }

                @Override
                public void messageArrived(String topicName, MqttMessage message)
                        throws Exception {
                    //subscribe后得到的消息会执行到这里面
                    System.out.println("messageArrived----------");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // MQTT连接函数
    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (!client.isConnected() && flag)  //如果还未连接
                    {
                        client.connect(mqttConnectOptions);
                        Message msg = new Message();
                        msg.what = 31;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    e.getMessage();
                    Message msg = new Message();
                    msg.what = 30;
                    handler.sendMessage(msg);
                }

            }
        }).start();
    }

    // MQTT重新连接函数
    private void startReconnect() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected() && flag) {
                    Mqtt_connect();
                }
            }
        }, 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    // 订阅函数    (下发任务/命令)
    private void publish_message_plus(String topic, String message2) {
        if (client == null || !client.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(message2.getBytes());
        message.setQos(1);
        try {
            client.publish(topic, message);
        } catch (MqttException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.Rfid:
                publish_message_plus(mqtt_pub_topic, "{\"flag\":\"01\"}\n");
                break;
            case R.id.face_recognition:
                publish_message_plus(mqtt_pub_topic, "{\"flag\":\"02\"}\n");
                Intent intent = new Intent(this, FaceRecognitionActivity.class);
                //主动
                try {
                    client.disconnect();
                    flag = false;
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
                startActivity(intent);
                break;
            case R.id.password_check:
                intent = new Intent(this, PasswordCheckActivity.class);
                try {
                    client.disconnect();
                    flag = false;
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
                startActivity(intent);
                break;
            case R.id.exit:
                publish_message_plus(mqtt_pub_topic, "{\"flag\":\"04\"}\n");
                Toast.makeText(MainDeskActivity.this, "退出", Toast.LENGTH_SHORT).show();
                break;
        }

    }
}
