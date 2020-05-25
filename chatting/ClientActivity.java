package com.example.ryu_10.chatting;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androidclient.R;

//import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientActivity extends Activity {
    //view 객체들 선언
    EditText inText;
    Button button, ftp1bt, ftp2bt, wherebt;
    TextView outText, locText;

    //소켓과 데이터 송수신에 관련된 스레드 객체 선언
    Socket socket;
    SocketThread SocT;
    ReceiveThread recT;
    SendThread sendT;

    boolean flagIPS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client_activity);

        //for view
        inText = (EditText) findViewById(R.id.input);
        button = (Button) findViewById(R.id.button);
        ftp1bt = (Button) findViewById(R.id.ftp1);
        ftp2bt = (Button) findViewById(R.id.ftp2);
        wherebt = (Button) findViewById(R.id.where);
        outText = (TextView) findViewById(R.id.output);
        locText = (TextView) findViewById(R.id.locate);

        //intent로부터 값을 받아온다.
        Intent it = getIntent();
        final String sIP = it.getStringExtra("ip");
        final String sID = it.getStringExtra("id");

        //소켓 스레드 생성 및 시작
        SocT = new SocketThread(sIP, 5001, sID);
        SocT.start();

        //메시지 전송 버튼
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String data = inText.getText().toString();
                if (data != null) {
                    sendT = new SendThread(socket, data, sID);
                    sendT.start();
                    inText.setText("");
                }
            }
        });

        //위치 좌표 수신 버튼
        wherebt.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(flagIPS==true){
                    flagIPS = false;
                }
                else
                    flagIPS = true;
            }
        });

        //파일 전송 버튼
        ftp1bt.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
//              inText.setText("i.txt");
                String name = inText.getText().toString();
                if (name != null) {
                    try {
                        DataInputStream Fin = new DataInputStream(new FileInputStream(new File("/mnt/sdcard/ftp/" + name)));
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeUTF("file");
                        dos.writeUTF(name);
                        byte[] buf = new byte[1024];
                        while (Fin.read(buf) > 0) {
                            dos.write(buf);
                            dos.flush();
                        }
                        outText.post(new Runnable() {
                            public void run() {
                                outText.append("\n");
                                outText.append("파일전송");
                            }
                        });
                        dos.close();
                        socket.close();
                        SocT = new SocketThread(sIP, 5001, sID);
                        SocT.start();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    inText.setText("");

                }
            }
        });

        //파일 수신 버튼
        ftp2bt.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    ReceiveFileThread ReceiveF = new ReceiveFileThread(socket,sIP,sID);
                    ReceiveF.start();
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeUTF("get");
                } catch (IOException e) {
                    e.printStackTrace();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }

//                try{
//                    DataInputStream Din = new DataInputStream(socket.getInputStream());
//                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//
//                    dos.writeUTF("get");
//                    String name = Din.readUTF();
//
//                    File fp = new File("/mnt/sdcard/ftp/");
//                    if(!fp.exists()){
//                        fp.mkdir();
//                    }
//
//                    File f = new File("/mnt/sdcard/ftp/",name);
//                    FileOutputStream fout = new FileOutputStream(f);
//                    byte[] buffer = new byte[8192];
//                    int bytesRead=0;
//                    dos.writeUTF("get");
//                    while ((bytesRead = Din.read(buffer)) > 0) {
//                        fout.write(buffer, 0, bytesRead);
//                        fout.flush();
//                    }
//                    Din.close();
//                    fout.close();
//                }
//                catch(Exception e){
//                    e.printStackTrace();
//                }
            }
        });
    }
    //소켓 스레드
    class SocketThread extends Thread {
        boolean Flag;
        String ip, id;
        int port;

        DataOutputStream output;

        //생성자로 필요한 값들을 얻는다.
        public SocketThread(String ip, int port, String id) {
            Flag = true;
            this.ip = ip;
            this.port = port;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                //소켓을 연결하고 서버로 아이디를 보낸다.
                socket = new Socket(ip, port);
                output = new DataOutputStream(socket.getOutputStream());
                output.writeUTF(id);
                //소켓이 연결되면 항상 값을 수신하는 스레드를 생성하여 실행한다.
                recT = new ReceiveThread(socket);
                recT.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //값을 수신하는 스레드
    class ReceiveThread extends Thread {
        private Socket socket = null;
        DataInputStream input;

        public ReceiveThread(Socket socket) {
            this.socket = socket;
            try {
                input = new DataInputStream(socket.getInputStream());
            } catch (Exception e) {
            }
        }

        public void run() {
            try {
                while (input != null) {
                    final String msg = input.readUTF();
                    if (flagIPS)
                        locText.setText(msg);
                    else if (msg != null) {
                        Log.d(ACTIVITY_SERVICE, "test");
                        //수신한 내용을 곧바로 TextView로 전달한다.
                        outText.post(new Runnable() {
                            public void run() {
                                outText.append("\n");
                                outText.append(msg);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //값을 보내는 스레드
    class SendThread extends Thread {
        private Socket socket;
        String sendData = inText.getText().toString();
        DataOutputStream output;
        String sID;

        //보내는 자의 이름이 누구인지 모두가 알 수 있도록 생성자에서 id도 함께 수신
        public SendThread(Socket socket, String data, String id) {
            this.socket = socket;
            try {
                output = new DataOutputStream(socket.getOutputStream());
                sID = id;
            } catch (Exception e) {
            }
        }

        public void run() {
            try {
                if (output != null) {
                    if (sendData != null) {
                        output.writeUTF(sID +"  :  " + sendData);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException npe) {
                npe.printStackTrace();
            }
        }
    }

    //파일을 수신하는 스레드
    class ReceiveFileThread extends Thread {
        private Socket socket = null;
        DataInputStream Din;
        DataOutputStream dos;
        String sIP, sID;

        public ReceiveFileThread(Socket socket, String sIP, String sID) {
            this.socket = socket;
            this.sIP = sIP;
            this.sID = sID;
            try {
                Din = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

            } catch (Exception e) {
            }
        }

        String name = "";



        public void run() {
            try{
//                String name = Din.readUTF();
                name = inText.getText().toString();
                if (name.equals(""))
                    name = "k.txt";
                File f = new File("/mnt/sdcard/ftp/",name);
                FileOutputStream fout = new FileOutputStream(f);
                byte[] buffer = new byte[8192];

                int bytesRead=0;
//                bytesRead = Din.read(buffer,0,buffer.length);
//                int current = bytesRead;
//                do {
//                    bytesRead =
//                    Din.read(buffer, current, (buffer.length-current));
//                    if(bytesRead >= 0) current += bytesRead;
//                	} while(bytesRead > -1);
//                BufferedOutputStream bos = new BufferedOutputStream(fout);
//                bos.write(buffer, 0 , current);
                while ((bytesRead = Din.read(buffer)) > 0) {
                    fout.write(buffer, 0, bytesRead);
                }

                outText.post(new Runnable() {
                    public void run() {
                        outText.append("\n");
                        outText.append("파일수신");
                    }
                });
                fout.flush();
                Din.close();
                fout.close();
                socket.close();
                SocT = new SocketThread(sIP, 5001, sID);
                SocT.start();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}