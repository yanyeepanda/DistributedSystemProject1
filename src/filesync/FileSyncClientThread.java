package filesync;

/**
 * @author Ye Hua, Yi Lu
 * @date 18th April 2013
 */

/*
 * This file sync client thread is created by the sender side,
 * and helps the sender to send the instructions to the receiver.
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class FileSyncClientThread implements Runnable {
	String host;
	int serverPort;
	SynchronisedFile fromFile;
	
	FileSyncClientThread(String host, int serverPort, SynchronisedFile fromFile){
		this.host = host;
		this.serverPort = serverPort;
		this.fromFile=fromFile;
	}

	@Override
	public void run() {
		Instruction inst;
		// The Client reads instructions to send to the Server
		while((inst=fromFile.NextInstruction())!=null){
			String msg=inst.ToJSON();
			System.err.println("Sending: "+msg);
	  
			Socket s = null;
			try{
				s = new Socket(host, serverPort);
				DataInputStream in = new DataInputStream( s.getInputStream());
				DataOutputStream out =new DataOutputStream( s.getOutputStream());
				out.writeUTF(msg);     // UTF is a string encoding see Sn. 4.4
				String returned = in.readUTF();   // read returned message from server
				
				/*
				 * Client upgrades the CopyBlock to a NewBlock instruction and sends it.
				 */
				
				// network delay
				
				if(returned.equals("new")){//check whether server ask for NewBlockInstruction
					Instruction upgraded=new NewBlockInstruction((CopyBlockInstruction)inst);
					String msg2 = upgraded.ToJSON();
					System.err.println("Sending: "+msg2);
					out.writeUTF(msg2);
				}
			}catch (UnknownHostException e) {
				System.out.println("Socket:"+e.getMessage());
			}catch (EOFException e){
				System.out.println("EOF:"+e.getMessage());
			}catch (IOException e){
				System.out.println("readline:"+e.getMessage());
			}finally {
				if(s!=null) try {
					s.close();
				}catch (IOException e){
					System.out.println("close:"+e.getMessage());
				}
			}
		}		  
	}
}
