package sc.arc.comm.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.widget.TextView;
import sc.arc.comm.CommandBuffer;
import sc.arc.comm.protocol.IProtocol;
import sc.arc.comm.protocol.ProtocolMSP;
import sc.arc.comm.rc.Channel;

public class ControllerTCP extends Controller {
	
	private Activity activity;
	private TextView myLabel;
	
	Socket socket;
	private WifiManager mWifiManager;
	private ScanResult mmNet;
	private static final int SERVERPORT = 23;
	private static final String SERVER_IP = "192.168.4.1";
	
	private OutputStream mmOutputStream;
	private InputStream mmInputStream;
	
    public ControllerTCP(CommandBuffer commandBuffer) {
		super(commandBuffer);
	}

    public ControllerTCP(Activity main, TextView myLabel, List<Channel> ch) {
		super(new CommandBuffer());
		this.myLabel = myLabel;
		activity = main;

		// define protocol
		protocol = new ProtocolMSP(this, commandBuffer, ch);
		
	}
    
    @Override
	public boolean discover() {
		mWifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
		if (mWifiManager == null) {
			myLabel.setText("No Wifi adapter available");
			return false;
		}
		
		if(!mWifiManager.isWifiEnabled()) {
			Intent enableWifi = new Intent(Settings.ACTION_WIFI_SETTINGS);
			activity.startActivity(enableWifi);
		}

		List<ScanResult> wifiNetworks = mWifiManager.getScanResults();
		if (wifiNetworks.size() > 0) {
			for (ScanResult net : wifiNetworks) {
				if (net.SSID.contains("ESP_")) {
					mmNet = net;
					myLabel.setText("ESP_ network Found");
					// TODO store the current NET if any
					return true;
				}
			}
		}
		myLabel.setText("NO ESP_ network");
		
		return false;
	}
    
    @Override
	public boolean connect() {
  		new NetworkTask().execute();
        return true;
	}
    
    @Override
	public boolean disconnect() {
		//socket.close();
		return true;
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

	private class NetworkTask extends AsyncTask<Void, Void, Object>{

    	InetAddress serverAddr = null;

    	@Override
    	protected Object doInBackground(Void... v) {

    		try {
    			InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
    			socket = new Socket(serverAddr, SERVERPORT);
    			mmInputStream = socket.getInputStream();
    			mmOutputStream = socket.getOutputStream();
    		} catch (UnknownHostException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}

    		return serverAddr;


    	}

    	// onPostExecute displays the results of the AsyncTask.
    	@Override
    	protected void onPostExecute(Object result) {

    	}
    }
	
	

	@Override
	protected
	void txTask() {
		new AsyncTask<Void, Void, Object>() {
			
			@Override
			protected Object doInBackground(Void... v) {

				// send command to the buffer
				protocol.txChannels();
				//protocol.requestTM(ProtocolMSP.MSP_RC);

				if(!commandBuffer.isEmpty()) {
					try {
						if(mmOutputStream!=null)
							mmOutputStream.write(commandBuffer.poll().getBytes());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				return commandBuffer;

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
						if(mmOutputStream!=null) {
							mmOutputStream.write(commandBuffer.poll().getBytes());
						}
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return null;
			}

			@Override
			protected void onPostExecute(Object result) {
				
			
				    myLabel.setText("reading: "+bytesRead+" / ");
				   // out.write(buffer,0,bytesRead);
				    
				    //ByteBuffer buf = ByteBuffer.wrap(out.toByteArray());
				    int dataSize = 0;
				    int checksum = 0;
				    int offset 	 = 0;
				    int MSP 	 = 0;
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