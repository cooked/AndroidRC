package sc.arc.comm.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;
import sc.arc.comm.Command;
import sc.arc.comm.CommandBuffer;
import sc.arc.comm.protocol.IProtocol;

public class ControllerUDP extends Controller {
	
    public ControllerUDP(CommandBuffer commandBuffer) {
		super(commandBuffer);
	}

	final Handler toastHandler = new Handler();
    
    public void SendTo(final Context context, final Uri uri) {

        if (uri == null) return;
        String msg = Uri.decode(uri.getLastPathSegment());
        if(msg == null) return;
        byte[] msgBytes = msg.getBytes();
        if (msg.startsWith("\\0x")) {
            msg = msg.replace("\\0x", "0x");
            msgBytes = msg.getBytes();
        } else if (msg.startsWith("0x")) {
            msg = msg.replace("0x", "");
            if(!msg.matches("[a-fA-F0-9]+")) {
            	Toast.makeText(context, "ERROR: Invalid hex values", Toast.LENGTH_LONG).show();
            	return;
            }
            //msgBytes = hexStringToBytes(msg);
        }

        final byte[] buf = msgBytes;

        //String appName = context.getString(R.string.app_name);

        new Thread(new Runnable() {
            public void run() {
                try {
                    InetAddress serverAddress = InetAddress.getByName(uri.getHost());
                    DatagramSocket socket = new DatagramSocket();
                    if (!socket.getBroadcast()) socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddress, uri.getPort());
                    socket.send(packet);
                    socket.close();
                } catch (final UnknownHostException e) {
                    toastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, e.toString(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                } catch (final SocketException e) {
                    toastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, e.toString(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                } catch (final IOException e) {
                    toastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, e.toString(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }

	//@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean discover() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean connect() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean disconnect() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
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