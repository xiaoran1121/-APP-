package com.example.facecheck;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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

public class UserActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private Handler handler;
    private SqliteDBHelper mHelper;
    private final String host = "tcp://ionmnFFCKzs.iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";     // TCP协议
    private final String userName = "adroid&ionmnFFCKzs";
    private final String passWord = "1d5d423e224eda6dff1cf4cab96c150a34d24e7a437fd0d74e017d1f3d76f1e8";

    private final String ClientId = "ionmnFFCKzs.adroid|securemode=2,signmethod=hmacsha256,timestamp=1684918933497|";
    private final String mqtt_sub_topic = "/ionmnFFCKzs/adroid/user/get";
    private final String mqtt_pub_topic = "/ionmnFFCKzs/adroid/user/update";
    final int btn_backgrounds = R.drawable.button_selector;

    private String Id;
    private MqttConnectOptions mqttConnectOptions;
    private Intent intent;
    private EditText et_name;
    private EditText et_age;
    private RadioGroup rg_gender;
    private RadioGroup rg_pass;
    private RadioButton rb_male;
    private RadioButton rb_female;
    private RadioButton rb_true;
    private RadioButton rb_false;
    private Button save;
    private Button update;
    private String gender;
    private Boolean flag = true;
    private CheckBox ck_pass;
    private Userinfo info;
    private EditText et_password;
    private final String Password = "10086";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();//这种方式默认式亮色主题
        setContentView(R.layout.activity_user);
        TextView m_mqtt = findViewById(R.id.m_mqtt);
        et_name = findViewById(R.id.et_name);
        et_age = findViewById(R.id.et_age);
        et_password = findViewById(R.id.et_password);
        rg_gender = findViewById(R.id.rg_gender);
        rb_male = findViewById(R.id.rb_male);
        rb_female = findViewById(R.id.rb_female);
        ck_pass = findViewById(R.id.ck_pass);
        ImageView back = findViewById(R.id.user_back);
        save = findViewById(R.id.save);
        update = findViewById(R.id.update);
        Button btn_confirm = findViewById(R.id.btn_confirm);

        update.setOnClickListener(this);
        btn_confirm.setOnClickListener(this);

        back.setOnClickListener(this);
        rg_gender.setOnCheckedChangeListener(this);
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
                    case 3:  //MQTT 收到消息回传   UTF8Buffer msg=new UTF8Buffer(object.toString());

                        System.out.println(msg.obj.toString());   // 显示MQTT数据
                        break;
                    case 4:
                        info = mHelper.queryById(Id);
                        if (info!=null){
                            mHelper.delete(info);
                            Userinfo info1 = new Userinfo();
                            info1.identity_id = Id;
                            info1.age = Integer.parseInt(et_age.getText().toString());
                            info1.name = et_name.getText().toString();
                            info1.pass = ck_pass.isChecked();
                            info1.gender = gender;
                            mHelper.save(info1);
                            Toast.makeText(UserActivity.this, "更新成功", Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(UserActivity.this, "未查找到用户", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 5:
                        save.setBackgroundResource(btn_backgrounds);
                        update.setBackgroundResource(btn_backgrounds);
                        save.setOnClickListener(UserActivity.this);
                        update.setOnClickListener(UserActivity.this);
                        break;
                    case 31:   //连接成功
                        //Toast.makeText(MainDeskActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        m_mqtt.setText("连接成功");
                        try {
                            client.subscribe(mqtt_sub_topic, 1);//订阅
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;

                    case 30:  //连接失败
                        //Toast.makeText(UserActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
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
            //host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(host, ClientId, new MemoryPersistence());
            /* 创建MqttConnectOptions对象，并配置username和password。 */
            mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setCleanSession(false);
            mqttConnectOptions.setUserName(userName);
            mqttConnectOptions.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            mqttConnectOptions.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            //KeepAlive不能小于30s,不然会拒绝连接
            mqttConnectOptions.setKeepAliveInterval(200);
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
                    msg.what = 4;   //收到消息标志位
                    msg.obj = topicName + "---" + message.toString();
                    msg.obj = message.toString();
                    System.out.println(msg.obj);
                    try {
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
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
                Looper.loop();
            }
        }).start();
    }

    // MQTT重新连接函数
    private void startReconnect() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
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
            client.publish(topic, message);
        } catch (MqttException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.save:
                Id = getIntent().getStringExtra("Identity_Id");
                info = new Userinfo();
                info.identity_id = Id;
                info.age = Integer.parseInt(et_age.getText().toString());
                info.name = et_name.getText().toString();
                info.pass = ck_pass.isChecked();
                info.gender = gender;
                mHelper.save(info);
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                break;
            case R.id.update:
                Id = getIntent().getStringExtra("Identity_Id");
                Message msg = new Message();
                msg.what = 4;
                handler.sendMessage(msg);
                break;

            case R.id.user_back:
                Intent intent = new Intent(this, MainDeskActivity.class);
                try {
                    client.disconnect();
                    flag = false;
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
                startActivity(intent);
                break;
            case R.id.btn_confirm:
                if (TextUtils.isEmpty(et_password.getText())) {
                    Toast.makeText(this, "请输入正确的数字", Toast.LENGTH_SHORT).show();
                } else {
                    if (et_password.getText().toString().equals(Password)) {
                        publish_message_plus(mqtt_pub_topic, "{\"flag\":\"03\"}\n");
                        Toast.makeText(this, "密码正确", Toast.LENGTH_SHORT).show();
                        Message message = new Message();
                        message.what = 5;
                        handler.sendMessage(message);
                    } else {
                        publish_message_plus(mqtt_pub_topic, "{\"flag\":\"04\"}\n");
                        Toast.makeText(this, "密码错误，请重新输入", Toast.LENGTH_SHORT).show();
                    }
                }
                et_password.clearFocus();
                et_password.getText().clear();
                break;

        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        gender = "男";
        switch (i){
            case R.id.rb_male:
                gender = "男";
                break;
            case R.id.rb_female:
                gender = "女";
                break;
        }
    }
}