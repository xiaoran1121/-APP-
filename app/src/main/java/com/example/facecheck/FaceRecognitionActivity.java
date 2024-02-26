package com.example.facecheck;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.facecheck.DBHelper.SqliteDBHelper;
import com.example.facecheck.Enity.Userinfo;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FaceRecognitionActivity extends AppCompatActivity implements View.OnClickListener {
    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private Handler handler;
    private SqliteDBHelper mHelper;
    private final String host = "tcp://ionmnFFCKzs.iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";     // TCP协议
    private final String userName = "adroid&ionmnFFCKzs";
    private final String passWord = "1d5d423e224eda6dff1cf4cab96c150a34d24e7a437fd0d74e017d1f3d76f1e8";

    private final String ClientId = "ionmnFFCKzs.adroid|securemode=2,signmethod=hmacsha256,timestamp=1684918933497|";
    private final String mqtt_sub_topic = "/ionmnFFCKzs/adroid/user/get";
    private final String mqtt_pub_topic = "/ionmnFFCKzs/adroid/user/update";

    private TextView m_name;
    private TextView m_pass;
    private TextView m_id;
    private TextView m_gender;
    private String Id;
    private MqttConnectOptions mqttConnectOptions;
    private Intent intent;
    private Boolean flag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();//这种方式默认式亮色主题
        setContentView(R.layout.activity_face_recognition);

        TextView m_mqtt = findViewById(R.id.m_mqtt);
        m_name = findViewById(R.id.m_name);
        m_pass = findViewById(R.id.m_pass);
        m_id = findViewById(R.id.m_id);
        m_gender = findViewById(R.id.m_gender);
        ImageView back = findViewById(R.id.face_back);
        back.setOnClickListener(this);

        mHelper = SqliteDBHelper.getInstance(this);

        mHelper.openReadLink();
        mHelper.openWriteLink();

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
                    case 4:
                        AlertDialog.Builder builder = new AlertDialog.Builder(FaceRecognitionActivity.this);
                        builder.setTitle("人脸信息到达");
                        builder.setMessage("身份Id为:  " + msg.obj);
                        builder.setPositiveButton("检测", (dialog, which) -> {
                            Userinfo info = mHelper.queryById(Id);
                            if (info != null) {
                                m_id.setText("用户Id: " + Id);
                                m_name.setText("姓    名：" + info.name);
                                m_gender.setText("性    别: " + info.gender);
                                if (info.pass) {
                                    m_pass.setText("是否具有通行资格：是");
                                    publish_message_plus(mqtt_pub_topic, "{\"flag\":\"03\"}\n");
                                    Toast.makeText(FaceRecognitionActivity.this, "有通行资格，通过", Toast.LENGTH_SHORT).show();
                                } else {
                                    m_pass.setText("是否具有通行资格：否");
                                    publish_message_plus(mqtt_pub_topic, "{\"flag\":\"04\"}\n");
                                    Toast.makeText(FaceRecognitionActivity.this, "无通行资格，禁止通过", Toast.LENGTH_SHORT).show();
                                }
                            }else {
                                Toast.makeText(FaceRecognitionActivity.this, "未匹配到人脸", Toast.LENGTH_SHORT).show();
                            }

                        });
                        builder.setNegativeButton("录入", (dialog, which) -> {
                            intent = new Intent(FaceRecognitionActivity.this,UserActivity.class);
                            intent.putExtra("Identity_Id", Id);
                            try {
                                client.disconnect();
                                flag = false;
                            } catch (MqttException e) {
                                throw new RuntimeException(e);
                            }
                            startActivity(intent);
                        });
                        builder.create().show();

                        break;


                    case 31:   //连接成功

                        //Toast.makeText(FaceRecognitionActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        m_mqtt.setText("连接成功");
                        try {
                            client.subscribe(mqtt_sub_topic, 1);//订阅
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;


                    case 30:  //连接失败
                        Toast.makeText(FaceRecognitionActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        m_mqtt.setText("连接失败");
                        break;

                    default:
                        break;
                }
            }
        };
        /* -------------------------------------------------------------------------------------- */
    }

    // MQTT初始化
    private void Mqtt_init() {
        try {
            //host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(host, ClientId, new MemoryPersistence());
            /* 创建MqttConnectOptions对象，并配置username和password。 */
            mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setCleanSession(true);
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setUserName(userName);
            mqttConnectOptions.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            mqttConnectOptions.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            //KeepAlive不能小于30s,不然会拒绝连接
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
                    Message msg = new Message();
                    msg.what = 4;
                    msg.obj = topicName + "---" + message.toString();
                    msg.obj = message.toString();
                    System.out.println(msg.obj);
                    try {
                        //json解析
                        JSONObject jsonObject = new JSONObject(message.toString());
                        Id = jsonObject.getString("Id");
                        Log.d("xiaofu", Id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    handler.sendMessage(msg);

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // MQTT连接函数
    private void Mqtt_connect() {
        new Thread(() -> {

            try {
                Log.d("fu", "run");

                if (!client.isConnected()&&flag)  //如果还未连接
                {
                    client.connect(mqttConnectOptions);
                    Message msg = new Message();
                    msg.what = 31;
                    Log.d("fu", "31");
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                Log.d("fu", "error");
                e.printStackTrace();
                e.getMessage();
                Message msg = new Message();
                msg.what = 30;
                Log.d("fu", "30");
                handler.sendMessage(msg);
            }

        }).start();
    }

    // MQTT重新连接函数
    private void startReconnect() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()&&flag) {
                    Mqtt_connect();
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
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
            //client.publish(topic,message2.getBytes(),0,true);
            client.publish(topic, message);
        } catch (MqttException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, MainDeskActivity.class);
        try {
            client.disconnect();
            flag = false;
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
        startActivity(intent);
    }



    /* ========================================================================================== */
}