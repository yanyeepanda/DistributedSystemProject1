package SyncThread;

import java.io.IOException;

import filesync.*;

public class ThreadFromFlie implements Runnable {

	Instruction inst;
	InstructionFactory instFact = new InstructionFactory();

	// The Client reads instructions to send to the Server
	public String readIns(SynchronisedFile fromFile) {
		String msg = inst.ToJSON();
		System.err.println("Sending: " + msg);
		return msg;
	}

	// Client upgrades the CopyBlock to a NewBlock instruction and sends it.
	public String upgradIns(String msg) {
		msg = inst.ToJSON();
		Instruction upgraded = new NewBlockInstruction(
				(CopyBlockInstruction) inst);
		String msg2 = upgraded.ToJSON();
		System.err.println("Sending: " + msg2);
		return msg2;
	}

	@Override
	public void run() {
		
	}

}
