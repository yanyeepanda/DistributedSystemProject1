package SyncThread;

import java.io.IOException;

import filesync.*;

public class ThreadToFile implements Runnable{
	
	Instruction inst;
	InstructionFactory instFact=new InstructionFactory();
	
	//The Server receives the string and convert it to instruction here.
	public Instruction receiveIns(SynchronisedFile toFile, String msg){
		Instruction receivedInst = instFact.FromJSON(msg);
		try {
			// The Server processes the instruction
			toFile.ProcessInstruction(receivedInst);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1); // just die at the first sign of trouble
		} catch (BlockUnavailableException e){
			return null;
		}
		return receivedInst;
	}
	
	//Server receives the NewBlock instruction.
	public void receiveUpgradedIns(SynchronisedFile toFile, String msg2){
		Instruction receivedInst2 = instFact.FromJSON(msg2);
		try {
			toFile.ProcessInstruction(receivedInst2);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (BlockUnavailableException e) {
			assert(false);
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
