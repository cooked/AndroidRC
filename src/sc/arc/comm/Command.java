package sc.arc.comm;

import java.nio.ByteBuffer;
import java.util.List;

public class Command {

	byte[] bytes;
	
	public Command(ByteBuffer bytes) {
		this.bytes = bytes.array();
	}
	
	public Command(List<Byte> b) {
		bytes = new byte[b.size()];
		for(int i=0;i<b.size();i++)
			bytes[i] = b.get(i);
	}
	
	public Command(byte[] bytes) {
		this.bytes = bytes;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
}