package sc.arc.comm.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.widget.TextView;
import sc.arc.comm.Command;
import sc.arc.comm.CommandBuffer;
import sc.arc.comm.protocol.IProtocol;
import sc.arc.comm.protocol.Protocol;
import sc.arc.comm.protocol.ProtocolMSP;
import sc.arc.comm.rc.Channel;

public class ControllerBT extends Controller {

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothSocket mmSocket;
	private BluetoothDevice mmDevice;
	private OutputStream mmOutputStream;
	private InputStream mmInputStream;
	private TextView myLabel;
	private Activity activity;
	
	public ControllerBT(Activity main, TextView myLabel, List<Channel> ch) {
		super(new CommandBuffer());
		this.myLabel = myLabel;
		activity = main;
		//channels = ch;
		
		// init protocol // TODO move in a better position
		protocol = new ProtocolMSP(this, commandBuffer, ch);
		
	}
	
	@Override
	public boolean discover() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null)
			myLabel.setText("No bluetooth adapter available");

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableBluetooth, 0);
		}

		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				if (device.getName().equals("BTBee")) {
					mmDevice = device;
					myLabel.setText("Bluetooth Device Found");
					return true;
				}
			}
		}
		myLabel.setText("Bluetooth NO Device");
		return false;
	}
	
	@Override
	public boolean connect() {
		// Standard SerialPortService ID
		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); 
		try {
			mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
			mmSocket.connect();
			mmOutputStream = mmSocket.getOutputStream();
			mmInputStream = mmSocket.getInputStream();
			myLabel.setText("Bluetooth Opened");
		} catch (IOException e) {
			e.printStackTrace();
			myLabel.setText("Bluetooth Error");
			return false;
		}
		// beginListenForData();
		return true;
	}
	
	@Override
	public boolean disconnect() {
		//stopWorker = true;
		try {
			if(mmOutputStream!=null) mmOutputStream.close();
			if(mmInputStream!=null) mmInputStream.close();
			if(mmSocket!=null) mmSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		myLabel.setText("Bluetooth Closed");
		return true;
	}

	/*public boolean sendData(List<ControlCh> channels) {
		
		String msg = "*";
		for(ControlCh channel:channels)
	        msg += channel.getNrmS()+" ";
	    msg = msg.trim()+"\n";
		return sendData(msg.getBytes());
		
	}*/
	
	/*public boolean sendData(byte[] bytes) {
	
	try {
		mmOutputStream.write(bytes);
		return true;
	} catch (IOException e) {
		e.printStackTrace();
	}
	return false;
}*/
	
	public void sendData() {
		
		// send command to the buffer
		protocol.txChannels();

		if(!commandBuffer.isEmpty()) {
			try {
				mmOutputStream.write(commandBuffer.poll().getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	

	//@Override
	public void run() {
		sendData();
		//H.postDelayed(this, ms_tx);
	}
	
	

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setProtocol(IProtocol protcol) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected
	void txTask() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected
	void rxTask() {
		// TODO Auto-generated method stub
		
	}

	

}
