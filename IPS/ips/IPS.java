package com.example.ryu_10.ips;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mycontacts.db";

    private static final int DATABASE_VERSION = 2;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE contacts ( _id INTEGER PRIMARY KEY" +
                " AUTOINCREMENT, x NUMBER, y NUMBER, hot NUMBER, hot2 NUMBER, point NUMBER);");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS contacts");
        onCreate(db);
    }
}

class RSSdata{
    double x=0, y=0;
    int hot1=0, hot2=0, hot3=0;
    int point=0;

    public void saveRSS(double x, double y, int hot, int hot2, int hot3, int point) {
        this.x = x;
        this.y = y;
        this.hot1 = hot;
        this.hot2 = hot2;
        this.hot3 = hot3;
        this.point = point;
    }

    void getXY(double x, double y){
        x = this.x;
        y = this.y;
    }
    int gethot1(){
        return hot1;
    }
    int gethot2(){
        return hot2;
    }

    void reset(){
        this.x = this.y = 0;
        this.hot1=this.hot2=point=0;
    }
}

public class IPS extends AppCompatActivity implements SensorEventListener {
    //뷰 들을 위한 것
    private TextView wifiview;
    private TextView orientview;
    private TextView aziview;
    private TextView stepsview;
    private TextView locview;
    private EditText pointview;
    private EditText editIP;
    private TextView ptview;

    private Button resetbtn;
    private Button rssbtn1;
    private Button rssbtn;
    private Button servbtn;

    //소켓
    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    String sendS;
    boolean isConnect = false;

    //데이터베이스 변수
    DBHelper helper;
    SQLiteDatabase db;

//  DrawView drawing;

    RSSdata[] data = new RSSdata[4]; //RSS 데이터들

    //센서
    private SensorManager mSensorManager;

    //for orientation
    private Sensor mAccelerometer, mMagnetometer;
    private float[] m_acc_data = null;
    private float[] m_mag_data = null;
    private float[] m_rotation = new float[9];
    private float[] m_orient_data = new float[3];

    String[] orientS = new String[]{"South", "West", "North", "East"};
    int orientN; //0부터 3까지 차례대로 "South", "West", "North", "East"을 의미

    //for PDR
    private float previousA = 0, currentA = 0; // 가속도 변화량을 구할 변수들
    private int steps = 0; //걸음수를 담을 정수형 변수
    private int threshold = 3;      // threshold 값 담을 정수형 변수
    private double stepSize = 0.5; //보폭은 50cm로 지정
    private int mAzimuth = 0; //방위각
    private double x = 0, y = -0.5; //현 위치값

    //forRSS
    private int[] rss = new int[3]; //거리측정에 쓰이는 WiFi의 RSS값을 담을 변수
    private  String[] mac = new String[]{"","",""};

    private int point = 0;
    private int[] hotrange = new int[4];
    // private int[] hot2range = new int[4];
    boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        helper = new DBHelper(this);
        try {
            db = helper.getWritableDatabase();
        } catch (SQLiteException ex) {
            db = helper.getReadableDatabase();
        }

        //for views
        editIP = (EditText)findViewById( R.id.editIp);
        wifiview = (TextView) findViewById(R.id.wifi);
        pointview = (EditText) findViewById(R.id.RSSpt);
        orientview = (TextView) findViewById(R.id.orientation);
        aziview = (TextView) findViewById(R.id.azimuth);
        stepsview = (TextView) findViewById(R.id.steps);
        locview = (TextView) findViewById(R.id.location);
        ptview = (TextView) findViewById(R.id.Point);
        resetbtn = (Button) findViewById(R.id.reset);
        rssbtn1 = (Button) findViewById(R.id.RSSbt1);
        rssbtn = (Button) findViewById(R.id.RSSbt);
        servbtn = (Button) findViewById(R.id.Serverbtn);

//        drawing = (DrawView) findViewById(R.id.myview);
        //for graphic
//        drawing = new DrawView(this);

        //sensor
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //for orientation
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //for RSS
        // Setup WIFI
        wifimanager = (WifiManager) getSystemService(WIFI_SERVICE);

        // if WIFIEnabled
        if (wifimanager.isWifiEnabled() == false)
            wifimanager.setWifiEnabled(true);

        initWIFIScan(); // WIFI Scan을 시작한다!

        for(int i=0; i<4 ; i++){
            data[i] = new RSSdata();

        }
    }

    //서버
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //접속버튼
    public void connectServer(View v) {
        StrictMode.enableDefaults();
        String sIP = editIP.getText().toString();
        try {
            socket = new Socket(sIP, 5001);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("IPS"); // 최초 전송(id)는 IPS로.
        } catch (IOException e) {
            e.printStackTrace();
        }
        isConnect = true;
    }



    //리셋버튼
    public void resetAll(View v) {
        previousA = currentA = 0; // 가속도 변화량을 구할 변수들
        steps = 0; //걸음수를 담을 정수형 변수
        x = 0;
        y = -0.5; //현 위치값0; //현 위치값

        wifiview.setText("SSID : null , RSS : null");
        pointview.setText(null);
        pointview.setHint("RSS Point");
        stepsview.setText("Current Steps : 0");
        locview.setText("( x, y )");

        flag =false;


        unregisterReceiver(mReceiver); // stop WIFISCan
        initWIFIScan(); // start WIFIScan
    }

    //RSS기반 위치기반서비스 활성화 버튼
    public void basedRSS(View v) {
        flag = true;
        Toast.makeText(this,"Find the location by RSS", Toast.LENGTH_SHORT).show();
    }

    //RSS값 저장 버튼
    public void saveRSS(View v) {
        String tmp = pointview.getText().toString();
        point = Integer.valueOf(tmp);
        for(int i=0;i<4;i++){
        if(point==(i+1)){
            data[i].x=x; data[i].y=y; data[i].hot1=rss[0]; data[i].hot2=rss[1]; data[i].hot3 = rss[2]; data[i].point=point;}
        }
       /* db.execSQL("INSERT INTO contacts VALUES (null, '" + x + "', '" + y
               + "', '" + hot + "', '" + hot2 +"', '" + point+"');");
        Toast.makeText(getApplicationContext(), "성공적으로 추가되었음",
                Toast.LENGTH_SHORT).show(); */
    }

    //for orientation, accellerometer
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer,
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];

            currentA = (float) Math.sqrt(ax*ax+ay*ay+az*az);

            //값의 변화량이 기준점(허용치, threshold)보다 크다면
            if (Math.abs(currentA - previousA) > threshold) {
                steps++; //걸었는지 여부 판단
                stepsview.setText("Current Steps : " + String.valueOf(steps));

                switch (orientN) {
                    case 0:
                        y = y + stepSize;
                        break;
                    case 1:
                        x = x + stepSize;
                        break;
                    case 2:
                        y = y - stepSize;
                        break;
                    case 3:
                        x = x - stepSize;
                        break;
                }

//                drawing.setX((float) x);
//                drawing.setY((float) y);
//                drawing.invalidate();
                if(isConnect){
                    //toServer
                    String dataS = locview.getText().toString() + "영역 : "+ String.valueOf(point);
                    try {
                        out.writeUTF(dataS);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            locview.setText("( "+ String.valueOf(x) + " , " + String.valueOf(y) + ")");
            previousA = currentA;

            m_acc_data = event.values.clone();




        } else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // 자기장 센서가 전달한 데이터인 경우
            // 수치 데이터를 복사한다.
            m_mag_data = event.values.clone();
        }
        if(m_acc_data != null && m_mag_data != null) {
            // 가속 데이터와 자기장 데이터로 회전 매트릭스를 얻는다.
            SensorManager.getRotationMatrix(m_rotation, null, m_acc_data, m_mag_data);
            // 회전 매트릭스로 방향 데이터를 얻는다.
            SensorManager.getOrientation(m_rotation, m_orient_data);

            // Radian 값을 Degree 값으로 변환한다.
            m_orient_data[0] = (float) Math.toDegrees(m_orient_data[0]);

            // 0 이하의 값인 경우 360을 더한다.
            if (m_orient_data[0] < 0) m_orient_data[0] += 360;

            // 첫번째 데이터인 방위값
            mAzimuth = (int) m_orient_data[0];

            if (mAzimuth >= 0 && mAzimuth < 90)
                orientN = 0;
            else if (mAzimuth >= 90 && mAzimuth < 180)
                orientN = 1;
            else if (mAzimuth >= 180 && mAzimuth < 270)
                orientN = 2;
            else
                orientN = 3;

            aziview.setText("Current Angle : " + mAzimuth);
            orientview.setText("Orientation : " + orientS[orientN]);
            ptview.setText("Point :" + point );
        }
    }

    //RSS
    // WifiManager variable
    WifiManager wifimanager;

    private List<ScanResult> mScanResult; // ScanResult List

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                getWIFIScanResult(); // get WIFISCanResult
                wifimanager.startScan(); // for refresh
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
            }
        }
    };

    //RSS : received s'ignal strength
    public void getWIFIScanResult() {
        mScanResult = wifimanager.getScanResults(); // ScanResult
        rss[0] = rss[1] = rss[2] = -80;

// Scan count
        for (int i = 0; i < mScanResult.size(); i++) {
            ScanResult result = mScanResult.get(i);
//            String ssid = result.SSID.toString();
            String mac_temp = result.BSSID.toString();
            int rss_temp = result.level;

            if(rss_temp > rss[0]){
                rss[0] = rss_temp;
                mac[0] = mac_temp;
            }
            else if(rss_temp > rss[1]) {
                rss[1] = rss_temp;
                mac[1] = mac_temp;
            }
            else if(rss_temp > rss[2]) {
                rss[2] = rss_temp;
                mac[2] = mac_temp;
            }

//            if (ssid.equals("G41")) {
//                hot2 = result.level;
//            }
//            if (ssid.equals("AndroidHotspot6670")) {
//                hot = result.level;
//            }
        }

        wifiview.setText(""+ mac[0] +":"+""+rss[0]+"\n"+""+ mac[1] +":"+""+rss[1]+"\n"+""+ mac[2] +":"+""+rss[2]+"\n");

          for(int i=0; i<4; i++) {
            if( (data[i].hot1 + 10 > rss[0]) && (data[i].hot1 - 10 < rss[0]) && (data[i].hot2 + 10 > rss[1])
                    && (data[i].hot2 - 10 < rss[1]) && (data[i].hot3 + 10 > rss[2]) && (data[i].hot3 - 10 < rss[2])) {
                    hotrange[i] = 1;
            }
        }

        if(flag) {
            for(int i=0;i<4;i++) {
                if (hotrange[i] == 1) {
                    // Toast.makeText(getApplicationContext(), "1-point", Toast.LENGTH_SHORT).show();
                    x=data[i].x;
                    y=data[i].y;
                    point=data[i].point;
                    hotrange[i] = 0;
                }
            }
        }
    }

    public void initWIFIScan() {
        // init WIFISCAN
        final IntentFilter filter = new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        wifimanager.startScan();
    }
}