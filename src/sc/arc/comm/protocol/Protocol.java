package sc.arc.comm.protocol;

import sc.arc.comm.CommandBuffer;
import sc.arc.comm.rc.IChannels;

public abstract class Protocol implements IProtocol,IChannels {

	public String ID = "NULL";
	
	CommandBuffer commandBuffer;
	
	public Protocol() {
	}

	@Override
	abstract public void init();
	
	@Override
	abstract public void eval();
	
	public CommandBuffer getCommandBuffer() {
		return commandBuffer;
	}

	public void setCommandBuffer(CommandBuffer commandBuffer) {
		this.commandBuffer = commandBuffer;
	}
	
	abstract public void requestTM(int...tm);
	
}


