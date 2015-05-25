package sc.arc.comm.controller;

import sc.arc.comm.CommandBuffer;
import sc.arc.comm.protocol.IProtocol;

public interface IController {
		
	public boolean discover();
	public boolean connect();
	public boolean disconnect();
	public boolean start();
	public boolean stop();
	
	public boolean isConnected();
	
	public int setMode(int mode);
	public int getMode();
	
	public void setProtocol(IProtocol protcol);
	
	public CommandBuffer getCommandBuffer();
	
}
