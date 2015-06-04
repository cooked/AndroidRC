package sc.arc.comm.controller;

import sc.arc.comm.CommandBuffer;
import sc.arc.comm.protocol.Protocol;

public interface IController {
		
	public boolean discover();
	public boolean connect();
	public boolean disconnect();
	public void start();
	public boolean stop();
	
	public boolean isConnected();
	public void setConnected(boolean connected);
	public boolean isRunning();
	
	public int setMode(int mode);
	public int getMode();
	
	public void setProtocol(Protocol protcol);
	
	public CommandBuffer getCommandBuffer();
	
}
