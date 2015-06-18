/**
 * @author Yanyi Liang
 * @date Sep 2014
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.json.simple.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import filesync.*;

public class syncserver {
	@Option(name = "-file", usage = "filename", required = true)
	private String file;
	
	@Option(name = "-p", usage = "choose server port")
	private int serverport;
	static int counter = 0;
	
	public syncserver(String[] args){
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		Instruction receivedInst = null;
		CounterJSON cj = new CounterJSON();
		
		SynchronisedFile serverFile = null;
		
		DatagramSocket socket = null;
		int port = 4144;
		port = serverport;
		if(port==0)
			port = 4144;
		
		System.out.println("The server port is: " + port);

		try {
			socket = new DatagramSocket(port);
			System.out.println("Server listening for a connection.");
		
			byte[] buf = new byte[1028];
			DatagramPacket req = new DatagramPacket(buf, buf.length);
			socket.receive(req);

			String firstMsg = new String(req.getData()).trim();
			System.out.println(firstMsg);
			JSONObject firstObj = cj.getCounterJSON(firstMsg);
			if(cj.getCounterJSONType(firstObj).equals("negotiation")){
				int blocksize = ((Long) firstObj.get("blocksize")).intValue();
				try {
					serverFile=new SynchronisedFile(file,blocksize);// should use args[0] instead
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
				try{
					if(firstObj.get("direction").equals("push")){
						System.out.println("Server acts as receiver.");
						counter = 1;
						try{
							while(true) {
								System.out.println("Server listening for a connection.");
								
								byte[] bufPush = new byte[1024*64];//max packet size for udp
								DatagramPacket reqPush = new DatagramPacket(bufPush, bufPush.length);
								socket.receive(reqPush);
									
								String receivedMsg = new String(reqPush.getData()).trim();
								System.err.println("Received: "+receivedMsg);
								JSONObject obj = cj.getCounterJSON(receivedMsg);
								int c1 = cj.getJSONCounter(obj);
								if (c1 == counter){
								String receivedInstString = cj.getJSONInst(obj);	
								InstructionFactory instFact=new InstructionFactory();
								receivedInst = instFact.FromJSON(receivedInstString);
									
								try {
									// The Server processes the instruction		
									serverFile.ProcessInstruction(receivedInst);
								} catch (IOException e) {
									e.printStackTrace();
									System.exit(-1); // just die at the first sign of trouble
								} catch (BlockUnavailableException e) {
									System.out.println("throw a BlockUnavailableException");
									// The server does not have the bytes referred to by the block hash.
									try {
										/*
										 * At this point the Server needs to send a request back to the Client
										 * to obtain the actual bytes of the block.
										 */
										//ask client for actual bytes of the block											
										String exceptionString = cj.CounterExceptionJSON(counter);
											
										byte[] exceptionBuf = exceptionString.getBytes();
										DatagramPacket exceptionReply = 
												new DatagramPacket(exceptionBuf, exceptionBuf.length,reqPush.getAddress(), reqPush.getPort());
										socket.send(exceptionReply);
										// network delay
																							
										/*
										 * Server receives the NewBlock instruction.
										 */
										byte[] bufNewBlock = new byte[1024*64];
										DatagramPacket reqNewBlock = new DatagramPacket(bufNewBlock, bufNewBlock.length);
										socket.receive(reqNewBlock);
											
										String NewBlock = new String(reqNewBlock.getData()).trim();
										System.err.println("Received New Block: "+receivedMsg);
										JSONObject objNew = cj.getCounterJSON(NewBlock);
										String NewBlockString = cj.getJSONInst(objNew);
											
										Instruction receivedNewInst = instFact.FromJSON(NewBlockString);
										serverFile.ProcessInstruction(receivedNewInst);
									} catch (IOException e1) {
										e1.printStackTrace();
										System.exit(-1);
									} catch (BlockUnavailableException e1) {
										assert(false); // a NewBlockInstruction can never throw this exception
									}
								}
								}else {
									String expectingString = cj.CounterExpectJSON(counter);
									DatagramPacket Expect = new DatagramPacket(expectingString.getBytes(), expectingString.getBytes().length,reqPush.getAddress(),reqPush.getPort());
									socket.send(Expect);
								}
								
								//send ack to client
								String ack = cj.CounterAckJSON(counter);
								try{
									DatagramPacket ackReply = new DatagramPacket(ack.getBytes(), ack.getBytes().length,reqPush.getAddress(),reqPush.getPort());
									socket.send(ackReply);
								}catch (SocketException e) {
									e.printStackTrace();
								}
								if(receivedInst.Type().equals("EndUpdate")){
									counter = 1;
								}else{
									counter++;
								}
							}
						} catch (IOException e) {
							System.out.println("Listen socket:" + e.getMessage());
						}
					} else if (firstObj.get("direction").equals("pull")) {
						System.out.println("Server acts as sender");
						counter = 1;
						/*
						 * Start a thread to service the Instruction queue.
						 */
						Thread thread = new Thread(new fileSyncClientThread(req.getAddress().getHostName(),req.getPort(),serverFile,counter));
						thread.start();
						/*
						 * Continue forever, checking the fromFile every 5 seconds.
						 */
						  while(true){
							  try {
									// TODO: skip if the file is not modified
									System.err.println("syncserver: calling fromFile.CheckFileState()");
									serverFile.CheckFileState();
								} catch (IOException e) {
									e.printStackTrace();
									System.exit(-1);
								} catch (InterruptedException e) {
									e.printStackTrace();
									System.exit(-1);
								}
								try {
									Thread.sleep(3000);
								} catch (InterruptedException e) {
									e.printStackTrace();
									System.exit(-1);
								}
						  }
					}
				}finally {
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null)
				socket.close();
		}
	}

	public static void main(String[] args) {
		new syncserver(args);
	}
}
