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
    private ServerSocket ServerSocket = null;//����
    String name = null; //���ϸ�
    boolean flagIPS = false;
 
    public static void main(String[] args) {
        new ChattingServer().start();
    }
 
    public ChattingServer() {
        // hashmap ������(Key, value) ����
        clients = new HashMap<String, DataOutputStream>();
        // clients ����ȭ
        Collections.synchronizedMap(clients);
    }
 
    private void start() {
        
        int port = 5001;
        Socket socket = null;
 
        try {
            ServerSocket = new ServerSocket(port);
            while (true) {
            	//���� ����, Ŭ���̾�Ʈ ����
                socket = ServerSocket.accept();
                //Ŭ���̾�Ʈ�� �ּҸ� �о�ͼ� ���������� ���
                InetAddress add = socket.getInetAddress();
                System.out.println(add + "  connected");
                //������ ����
                //���� Ŭ���̾�Ʈ�� ���ÿ� �����Ϸ���, �� Ŭ���̾�Ʈ������ ������ �ϳ��� ������� �����ؾ� �Ѵ�.
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
            	//�����ϸ� �켱 id�� �޾ƿ´�. �׸��� ��ο��� ���ο� device�� ���������� �˸���. 
                id = input.readUTF();
                if(id.equals("IPS"))
                	flagIPS = true;
                System.out.println(id + " ����");
                clients.put(id, output);
                sendMsg(id + " ����");

                
                while (input != null) {
                    try {
                    	//Ŭ���̾�Ʈ�� ����Ǹ� ��ٷ� Ŭ���̾�Ʈ�κ��� ���� �����͸� �޴´�.
                        String temp = input.readUTF();
                        System.out.println(temp);
                        //���� Ŭ���̾�Ʈ�κ��� ���� �����Ͱ� Ư���� �ǹ̸� ���� �ִٸ�
                        //�� ���ҿ� �´� ������ �޼ҵ带 ȣ���Ѵ�.
                        //file�� Ŭ���̾�Ʈ���� '���� ����'��ư�� ���� �� ���޵Ǵ� �޽�����.
                        if (temp.equals("file")){
                        	receiveFile(name);
                    	}
                        //get�� Ŭ���̾�Ʈ���� '���� ����'��ư�� ���� �� ���޵Ǵ� �޽�����.
                        else if (temp.equals("get")){
                        	sendFile(name);
                        }
                        //Ŭ���̾�Ʈ�κ��� ���� �����Ͱ� ���� �ۼ��ſ� ���õ� ���ڿ��� ���� �ʴٸ�
                        //IPS�� ������ �����̰� Where�̶�� ��û�� �� ���,
                        //���� �޽����� �ٽ� Broadcasting�Ѵ�.
                        else if (temp.equals("Where") && flagIPS!=true)
                			sendMsg("IPS ������ ���ּ���!");
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
 
        //Ŭ���̾�Ʈ�鿡�� �޽����� ������ �޼ҵ�
        private void sendMsg(String msg) {
 
            // clients�� Key���� �޾Ƽ� �迭�μ���
            Iterator<String> it = clients.keySet().iterator();
 
            while (it.hasNext()) {
                try {
                	//Ŭ���̾�Ʈ�κ��� key���� ��� �д´�.
                    OutputStream dos = clients.get(it.next());
                    //�� key���� �´� ������ ��� ��Ʈ���� �����Ѵ�.
                    DataOutputStream output = new DataOutputStream(dos);
                    //�޽����� ��� �����Ѵ�.
                    output.writeUTF(msg);
                } catch (IOException e) {
                }
            }
        }
        
        private void receiveFile(String str){
        	  try{
        		 DataInputStream Din = new DataInputStream(socket.getInputStream());
                 name = Din.readUTF();
                 System.out.println("�������� ���� �̸� : " + name);
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
                 System.out.println(name+"���ſϷ�");
        	 }
             catch(Exception e){
                 System.out.println("���� ����");
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
                System.out.println("���� ���� �̸� : " + name);
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
                System.out.println(name+"���ۿϷ�");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println("�۽ſ���:��ã�ڴ�");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("�۽ſ���:���");
            }
        }      

    }
}

