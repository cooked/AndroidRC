package sc.arc.comm.controller;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import sc.arc.comm.CommandBuffer;
import sc.arc.comm.protocol.Protocol;

import static java.util.concurrent.TimeUnit.*;

public abstract class Controller implements IController {

	public static final int MODE1 = 1; 	// LxLyRxRy : YAW,PITCH,ROLL,THROTTLE
	public static final int MODE2 = 2; 	// LxLyRxRy : YAW,THROTTLE,ROLL,PITCH
	public static final int MODE3 = 3; 	// LxLyRxRy : ROLL,PITCH,YAW,THROTTLE
	public static final int MODE4 = 4; 	// LxLyRxRy : ROLL,THROTTLE,YAW,PITCH
	
	public int 	mode = 	MODE2;			// default to MODE2
	
	public int 	ms_tx = 100;
	public int 	ms_rx = 500;
	
	CommandBuffer 	commandBuffer;
	Protocol		protocol;

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
	ScheduledFuture<?> txHandle,rxHandle; 
	
	public Controller(CommandBuffer commandBuffer) {
		this.commandBuffer = commandBuffer;
	}
	
	protected void txTask(){};
	protected void rxTask(){};
	
	public boolean start() {
		
		txHandle = scheduler.scheduleAtFixedRate(
				new Runnable() {
			       public void run() { txTask(); }
			     }, 0, ms_tx, MILLISECONDS);
		rxHandle = scheduler.scheduleAtFixedRate(
				new Runnable() {
			       public void run() { rxTask(); }
			     }, 1000, ms_rx, MILLISECONDS);
		return true;
	}
	
	public boolean stop() {
		// http://stackoverflow.com/questions/4205327/scheduledexecutorservice-start-stop-several-times
		txHandle.cancel(true);
		rxHandle.cancel(true);
		return false;
	}
	
	public CommandBuffer getCommandBuffer() {
		return commandBuffer;
	}
	
	public int getMode() {
		return mode;
	}
	
	public int setMode(int mode) {
		return this.mode = mode;
	}

}
