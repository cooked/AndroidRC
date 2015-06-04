package sc.arc.comm.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;
import sc.arc.R;
import sc.arc.comm.CommandBuffer;
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
	
	public String preferredDevice = "HC";
	
	public ControllerBT(Activity main, TextView myLabel, List<Channel> ch) {
		super(new CommandBuffer());
		setContext(main.getApplicationContext());
		this.myLabel = myLabel;
		activity = main;
		
		// initialise the protocol (currently only MSP)
		protocol = new ProtocolMSP(this, commandBuffer, ch);
	}
	
	@Override
	public boolean discover() {
		
		// get the default adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// check if the adapter exists
		if (mBluetoothAdapter == null) {
			Toast.makeText(getContext(), R.string.conn_bt_no_adapter, Toast.LENGTH_LONG).show();
			return false;
		// if exists and is disabled take note and start the activity to enable it
		} else if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableBluetooth, 0);
			Toast.makeText(getContext(), R.string.conn_bt_enabled, Toast.LENGTH_LONG).show();
			// TODO check for the actual results and DO NOT assume everything's fine
		}
		
		// then get the list of the known devices
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for(BluetoothDevice device:pairedDevices) {
				if(device.getName().contains(preferredDevice)) {
					mmDevice = device;
					Toast.makeText(getContext(), R.string.conn_bt_enabled, Toast.LENGTH_LONG).show();
					return true;
				}
			}
		// if none inform the user about it 
		} else
			Toast.makeText(getContext(), R.string.conn_bt_enabled, Toast.LENGTH_LONG).show();
		
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
		} catch (IOException e) {
			Toast.makeText(getContext(), R.string.conn_bt_connect_err, Toast.LENGTH_LONG).show();
			return false;
		}
		setConnected(true);
		Toast.makeText(getContext(), R.string.conn_bt_connect, Toast.LENGTH_LONG).show();
		return true;
	}
	
	@Override
	public boolean disconnect() {
		try {
			if(mmOutputStream!=null) mmOutputStream.close();
			if(mmInputStream!=null) mmInputStream.close();
			if(mmSocket!=null) mmSocket.close();
			if (mBluetoothAdapter.isEnabled())
				mBluetoothAdapter.disable();
		} catch (IOException e) {
			Toast.makeText(getContext(), R.string.conn_bt_disconnect_err, Toast.LENGTH_LONG).show();
			return false;
		}
		setConnected(false);
		Toast.makeText(getContext(), R.string.conn_bt_disconnect, Toast.LENGTH_LONG).show();
		return true;
	}
	
	@Override
	protected void txTask() {
		new AsyncTask<Void, Void, Object>() {
			
			@Override
			protected Object doInBackground(Void... v) {

				protocol.txChannels();

				if(!commandBuffer.isEmpty()) {
					try {
						if(mmOutputStream!=null)
							mmOutputStream.write(commandBuffer.poll().getBytes());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		}.execute();

	}
	
	@Override
	protected
	void rxTask() {
			new AsyncTask<Void, Void, Object>() {
				
				int bytesRead = 0;
				int BUFSIZE = 256;
				int state = ProtocolMSP.IDLE;
				byte[] buffer = new byte[BUFSIZE];
				
				@Override
				protected Object doInBackground(Void... v) {
					
					protocol.requestTM(ProtocolMSP.MSP_RC);

					if(!commandBuffer.isEmpty()) {
						try {
							if(mmOutputStream!=null)
								mmOutputStream.write(commandBuffer.poll().getBytes());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					try {
						while(mmInputStream.available()>0) {
							bytesRead = mmInputStream.read(buffer);
							if (bytesRead == -1) break;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					return null;
				}

				@Override
				protected void onPostExecute(Object result) {
					
				
					    myLabel.setText("reading: "+bytesRead+" / ");
					    int dataSize = 0;
					    int checksum = 0;
					    int offset 	 = 0;
					    byte inBuf[] = {};
					    byte cmdMSP = 0;
					    
					    for(int i=0;i<bytesRead;i++) {
					    	byte c = buffer[i];

							if(state==ProtocolMSP.IDLE) {
						    	if (c=='$') state = ProtocolMSP.HEADER_START;
						    } else if (state == ProtocolMSP.HEADER_START) {
						        state = (c=='M') ? ProtocolMSP.HEADER_M : ProtocolMSP.IDLE;
						    } else if (state == ProtocolMSP.HEADER_M) {
						    	state = (c=='>') ? ProtocolMSP.HEADER_ARROW : ProtocolMSP.IDLE;
						    } else if (state == ProtocolMSP.HEADER_ARROW) {
						    	
						    	/*if (c > INBUF_SIZE) {  // now we are expecting the payload size
						    		state = ProtocolMSP.IDLE;
						            continue;
						    	}*/
						
						    	dataSize = c;		myLabel.append(Integer.toString(dataSize)+",");
						        checksum = c;
						        offset	 = 0;
						        inBuf = new byte[dataSize];
						        
						        state = ProtocolMSP.HEADER_SIZE;  // the command is to follow
						        
						    } else if (state == ProtocolMSP.HEADER_SIZE) {
						    	cmdMSP = c;		myLabel.append(Integer.toString(cmdMSP)+" ");
						        checksum ^= c;
						            
						        state = ProtocolMSP.HEADER_CMD;
						            
						    } else if (state == ProtocolMSP.HEADER_CMD) {
						           if (offset < dataSize) {
						        	  inBuf[offset++] = c;
						              checksum ^= c;
						            } else {
							              if (checksum == c) {// compare calculated and transferred checksum
							            	  ((ProtocolMSP)protocol).setInBuf(inBuf);
							            	  ((ProtocolMSP)protocol).evaluateCommand(cmdMSP,dataSize); // we got a valid packet, evaluate it
							            	  myLabel.append(((ProtocolMSP)protocol).debug);
							              }
						             
							              state = ProtocolMSP.IDLE;
						            }
						           
						    }
					    	
					    }
					    
					super.onPostExecute(result);
				}
				
			}.execute();
		
	}

	

}
