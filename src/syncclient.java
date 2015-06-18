/**
 * @author Yanyi Liang
 * @date Sep 2014
 */


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.json.simple.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import filesync.*;

public class syncclient {
	@Option(name = "-file", usage = "filename", required = true)
	private String file;
	
	@Option(name = "-host", usage = "hostname", required = true)
	private String host;
	
	@Option(name = "-p", usage = "choose server port")
	private int serverport;
	
	@Option(name = "-b", usage = "choose block size")
	private int bs;
	
	@Option(name = "-d", usage = "choose direction")
	private String d;
	
	static int counter = 0;
	
	public syncclient(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		CounterJSON cj = new CounterJSON();
		Instruction receivedInst = null;
		String hostname = host;
		SynchronisedFile clientFile = null;
		
		
		DatagramSocket socket = null;
		int port = 4144;
		port = serverport;
		if(port==0)
			port = 4144;
		
		try{
			socket = new DatagramSocket();
			InetAddress host = InetAddress.getByName(hostname);//localhost
			int blocksize = 1024;
			blocksize = bs;
			String direction = d;// pull or push
			
			try {
				clientFile=new SynchronisedFile(file,blocksize);// input file name
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			//send the negotiation
			JSONObject objN = new JSONObject();
			objN.put("type", "negotiation");
			objN.put("direction", direction);
			objN.put("blocksize", blocksize);
			
			String reqN = objN.toJSONString();
			
			byte[] bufN = reqN.getBytes();
			DatagramPacket requestN = new DatagramPacket(bufN, bufN.length, host, port);
			socket.send(requestN);
						
			if (direction.equals("push")) {
				System.out.println("Client acts as sender");
				counter = 1;
				/*
				 * Start a thread to service the Instruction queue.
				 */
				Thread thread = new Thread(new fileSyncClientThread("localhost", port, clientFile, counter));
				thread.start();
				/*
				 * Continue forever, checking the fromFile every 5 seconds.
				 */
				while (true) {
					try {
						// TODO: skip if the file is not modified
						System.err
								.println("syncclient: calling clientFile.CheckFileState()");
						clientFile.CheckFileState();
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
			} else if (direction.equals("pull")) {
				// client acts as receiver
				System.out.println("Client acts as receiver");
				counter = 1;
				try {
					while(true){
						byte[] bufPull = new byte[1024*64];
						DatagramPacket reqPull = new DatagramPacket(bufPull, bufPull.length);
						socket.receive(reqPull);
						
						String receivedMsg = new String(reqPull.getData()).trim();
						System.err.println("Received: "+receivedMsg);
						JSONObject obj = cj.getCounterJSON(receivedMsg);
						int c1 = cj.getJSONCounter(obj);
						if (c1 == counter){
						String receivedInstString = cj.getJSONInst(obj);
						InstructionFactory instFact=new InstructionFactory();
						receivedInst = instFact.FromJSON(receivedInstString);
						
						try {
							// The Client processes the instruction		
							clientFile.ProcessInstruction(receivedInst);
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
								//ask server for actual bytes of the block
								String exceptionString = cj.CounterExceptionJSON(counter);
								
								byte[] exceptionBuf = exceptionString.getBytes();
								DatagramPacket exceptionReply = 
										new DatagramPacket(exceptionBuf, exceptionBuf.length, reqPull.getAddress(), reqPull.getPort());
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
								clientFile.ProcessInstruction(receivedNewInst);
							} catch (IOException e1) {
								e1.printStackTrace();
								System.exit(-1);
							} catch (BlockUnavailableException e1) {
								assert(false); // a NewBlockInstruction can never throw this exception
							}
						}
					}else {
						String expectingString = cj.CounterExpectJSON(counter);
						DatagramPacket Expect = new DatagramPacket(expectingString.getBytes(), expectingString.getBytes().length,reqPull.getAddress(),reqPull.getPort());
						socket.send(Expect);
					}
					
					//send ack to client
					String ack = cj.CounterAckJSON(counter);
					try{
						DatagramPacket ackReply = new DatagramPacket(ack.getBytes(), ack.getBytes().length,reqPull.getAddress(),reqPull.getPort());
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
			}
		} catch (SocketException e){
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		} finally {
			if (socket!=null){
				socket.close();
			}
		}
	}

	public static void main(String[] args) {
		new syncclient(args);
	}
}
