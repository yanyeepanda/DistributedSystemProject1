/**
 * @author Yanyi Liang
 * @date Sep 2014
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.simple.JSONObject;
import filesync.*;

public class fileSyncClientThread implements Runnable{
	
	int counter;
	String host;
	int serverPort;
	SynchronisedFile fromFile;
	private CounterJSON newJson = new CounterJSON();
	
	fileSyncClientThread(String host, int serverPort, SynchronisedFile fromFile, int counter){
		this.host = host;
		this.serverPort = serverPort;
		this.fromFile = fromFile;
		this.counter = counter;
	}

	@Override
	public void run() {
		Instruction inst;
		// The Client reads instructions to send to the Server
		while((inst=fromFile.NextInstruction())!=null){
			String msg = inst.ToJSON();
			String msg2 = newJson.CounterInstJSON(msg, counter);
			
			System.err.println("Sending: "+msg2);
			
			DatagramSocket s = null;
			try{
				s = new DatagramSocket();
				InetAddress hostAdd = InetAddress.getByName(host);
				String req = msg2;
				byte[] buf = req.getBytes();
				DatagramPacket request = new DatagramPacket(buf, buf.length, hostAdd, serverPort);
				s.send(request);
				
				byte[] rbuf = new byte[1024];
				DatagramPacket reply = new DatagramPacket(rbuf, rbuf.length);
				s.receive(reply);
				
				String returned = new String(reply.getData()).trim();
				System.err.println("Received: "+returned);
				JSONObject returnedObj = newJson.getCounterJSON(returned);
				
				
				/*
				 * Client upgrades the CopyBlock to a NewBlock instruction and sends it.
				 */
				
				// network delay
				if(newJson.getCounterJSONType(returnedObj).equals("exception")){
					Instruction upgraded=new NewBlockInstruction((CopyBlockInstruction)inst);
					String msg3 = upgraded.ToJSON();
					String msg4 = newJson.CounterInstJSON(msg3, counter);
					System.err.println("Sending: "+msg4);
					
					byte[] buf2 = msg4.getBytes();
					DatagramPacket request2 = new DatagramPacket(buf2, buf2.length, hostAdd, serverPort);
					s.send(request2);
					
					rbuf = new byte[1024];
					reply = new DatagramPacket(rbuf, rbuf.length);
					s.receive(reply);
					
				}else if(newJson.getCounterJSONType(returnedObj).equals("expecting")){
					int c = newJson.getJSONCounter(returnedObj);
					s.send(request);
				}else if(newJson.getCounterJSONType(returnedObj).equals("ack")){
					
					System.out.println("counter: " + counter);
				}
				if(inst.Type().equals("EndUpdate")){
					counter = 1;
				}else{
					counter++;
				}
			} catch(UnknownHostException e){
				System.out.println("Socket:"+e.getMessage());
			} catch (IOException e) {
				System.out.println("readline:"+e.getMessage());
			} 
		}
		
	}

}
