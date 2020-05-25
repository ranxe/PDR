import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
 
public class ChattingServer {
 
    HashMap<String, DataOutputStream> clients;
    private ServerSocket ServerSocket = null;//소켓
    String name = null; //파일명
    boolean flagIPS = false;
 
    public static void main(String[] args) {
        new ChattingServer().start();
    }
 
    public ChattingServer() {
        // hashmap 생성자(Key, value) 선언
        clients = new HashMap<String, DataOutputStream>();
        // clients 동기화
        Collections.synchronizedMap(clients);
    }
 
    private void start() {
        
        int port = 5001;
        Socket socket = null;
 
        try {
            ServerSocket = new ServerSocket(port);
            while (true) {
            	//소켓 생성, 클라이언트 연결
                socket = ServerSocket.accept();
                //클라이언트의 주소를 읽어와서 서버에서만 출력
                InetAddress add = socket.getInetAddress();
                System.out.println(add + "  connected");
                //스레드 시작
                //여러 클라이언트가 동시에 접속하려면, 각 클라이언트마다의 연결이 하나의 스레드로 동작해야 한다.
                new MultiThread(socket).start();
            }
        } catch (IOException e) {
        }
    }
 
    class MultiThread extends Thread {
 
        Socket socket = null;
 
        String id = null;
        String msg = null;
 
        DataInputStream input;
        DataOutputStream output;
 
        public MultiThread(Socket socket) {
            this.socket = socket;
            try {
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
            }
        }
 
        public void run() {
 
            try {
            	//접속하면 우선 id를 받아온다. 그리고 모두에게 새로운 device가 접속했음을 알린다. 
                id = input.readUTF();
                if(id.equals("IPS"))
                	flagIPS = true;
                System.out.println(id + " 접속");
                clients.put(id, output);
                sendMsg(id + " 접속");

                
                while (input != null) {
                    try {
                    	//클라이언트가 실행되면 곧바로 클라이언트로부터 오는 데이터를 받는다.
                        String temp = input.readUTF();
                        System.out.println(temp);
                        //만약 클라이언트로부터 얻은 데이터가 특별한 의미를 갖고 있다면
                        //각 역할에 맞는 스레드 메소드를 호출한다.
                        //file은 클라이언트에서 '파일 전송'버튼을 누를 때 전달되는 메시지다.
                        if (temp.equals("file")){
                        	receiveFile(name);
                    	}
                        //get은 클라이언트에서 '파일 수신'버튼을 누를 때 전달되는 메시지다.
                        else if (temp.equals("get")){
                        	sendFile(name);
                        }
                        //클라이언트로부터 얻은 데이터가 파일 송수신에 관련된 문자열과 같지 않다면
                        //IPS가 접속한 상태이고 Where이라는 요청이 올 경우,
                        //얻은 메시지를 다시 Broadcasting한다.
                        else if (temp.equals("Where") && flagIPS!=true)
                			sendMsg("IPS 어플을 켜주세요!");
                        else if (temp.equals("Where") && flagIPS==true)
                			sendMsg(temp);
                        else{
                			sendMsg(temp);}
                    } catch (IOException e) {
                        sendMsg("No massege");
                        break;
                    }
                }
            } catch (IOException e) {
            }
        }
 
        //클라이언트들에게 메시지를 보내는 메소드
        private void sendMsg(String msg) {
 
            // clients의 Key값을 받아서 배열로선언
            Iterator<String> it = clients.keySet().iterator();
 
            while (it.hasNext()) {
                try {
                	//클라이언트로부터 key값을 모두 읽는다.
                    OutputStream dos = clients.get(it.next());
                    //각 key값에 맞는 데이터 출력 스트림을 생성한다.
                    DataOutputStream output = new DataOutputStream(dos);
                    //메시지를 모두 전송한다.
                    output.writeUTF(msg);
                } catch (IOException e) {
                }
            }
        }
        
        private void receiveFile(String str){
        	  try{
        		 DataInputStream Din = new DataInputStream(socket.getInputStream());
                 name = Din.readUTF();
                 System.out.println("수신중인 파일 이름 : " + name);
                 File f = new File("D://fip//"+name);
                 FileOutputStream output = new FileOutputStream(f);
                 byte[] buffer = new byte[8192];
                 int bytesRead=0;
                 while ((bytesRead = Din.read(buffer)) > 0) {
                     output.write(buffer, 0, bytesRead);
                 }

                 Din.close();
                 output.flush();
                 output.close();
                 System.out.println(name+"수신완료");
        	 }
             catch(Exception e){
                 System.out.println("수신 에러");
                 e.printStackTrace();
             }
        }
        
        private void sendFile(String str){
            try {
            	name = str;
            	if(name==null)
            		name = "WiFiScanner.txt";
                FileInputStream Fin = new FileInputStream(new File("D://fip//",name));
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//                dos.writeUTF(name);
                System.out.println("보낼 파일 이름 : " + name);
                byte[] buffer = new byte[8192];
                int bytesRead =0;
//                while (Fin.read(buffer) > 0) {
//                    dos.write(buffer);
//                    dos.flush();
//                }
                while ((bytesRead = Fin.read(buffer)) > 0) {
                    dos.write(buffer, 0, bytesRead);
                    dos.flush();
                }
//                dos.flush();
                dos.close();
                Fin.close();
                System.out.println(name+"전송완료");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println("송신에러:못찾겠다");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("송신에러:경로");
            }
        }      

    }
}

