package sc.arc.comm.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import sc.arc.comm.Command;
import sc.arc.comm.CommandBuffer;
import sc.arc.comm.controller.Controller;
import sc.arc.comm.rc.Channel;

/**
 * @author stefanocottafavi
 *
 */
public class ProtocolMSP extends Protocol {

	public static final String ID = "MSP";
	
	private static final String MSP_HEADER 		= "$M<";	// header command
	private static final String MSP_HEADER_TM 	= "$M>";	// header response
	
	public static final int
	  MSP_IDENT                =100,
	  MSP_STATUS               =101,
	  MSP_RAW_IMU              =102,
	  MSP_SERVO                =103,
	  MSP_MOTOR                =104,
	  MSP_RC                   =105,
	  MSP_RAW_GPS              =106,
	  MSP_COMP_GPS             =107,
	  MSP_ATTITUDE             =108,
	  MSP_ALTITUDE             =109,
	  MSP_ANALOG               =110,
	  MSP_RC_TUNING            =111,
	  MSP_PID                  =112,
	  MSP_BOX                  =113,
	  MSP_MISC                 =114,
	  MSP_MOTOR_PINS           =115,
	  MSP_BOXNAMES             =116,
	  MSP_PIDNAMES             =117,
	  MSP_SERVO_CONF           =120,
	    
	  MSP_SET_RAW_RC           =200,
	  MSP_SET_RAW_GPS          =201,
	  MSP_SET_PID              =202,
	  MSP_SET_BOX              =203,
	  MSP_SET_RC_TUNING        =204,
	  MSP_ACC_CALIBRATION      =205,
	  MSP_MAG_CALIBRATION      =206,
	  MSP_SET_MISC             =207,
	  MSP_RESET_CONF           =208,
	  MSP_SELECT_SETTING       =210,
	  MSP_SET_HEAD             =211, // Not used
	  MSP_SET_SERVO_CONF       =212,
	  MSP_SET_MOTOR            =214,
	  
	  MSP_BIND                 =241,
	
	  MSP_EEPROM_WRITE         =250,
	  
	  MSP_DEBUGMSG             =253,
	  MSP_DEBUG                =254
	;
	
	public static final int
	  IDLE 			= 0,
	  HEADER_START 	= 1,
	  HEADER_M 		= 2,
	  HEADER_ARROW 	= 3,
	  HEADER_SIZE 	= 4,
	  HEADER_CMD 	= 5,
	  HEADER_ERR 	= 6;
	
	int c_state = IDLE;
	boolean err_rcvd = false;
	
	byte checksum=0;
	byte cmd;
	int offset=0, dataSize=0;
	
	byte[] inBuf = new byte[256];
	byte[] buffer = new byte[256];
	
	public byte[] getInBuf() {
		return inBuf;
	}

	public void setInBuf(byte[] inBuf) {
		this.inBuf = inBuf;
	}
	
	int p;
	
	int mode;
	boolean toggleRead = false,toggleReset = false,toggleCalibAcc = false,toggleCalibMag = false,toggleWrite = false,
	        toggleRXbind = false,toggleSetSetting = false,toggleVbat=true,toggleMotor=false,motorcheck=true;

	private int version;
	private int multiType;
	private int multiCapability;
	private int cycleTime;
	private int i2cError;
	private int present;

	private int ax;
	private int ay;
	private int az;
	private int gz;
	private int gy;
	private int gx;

	private int magx;
	private int magy;
	private int magz;

	Channel[] ch;
	Channel roll,pitch,yaw,throttle; 
	
	public String debug = new String();
	
	Controller controller;
	
	int ctrlMode = Controller.MODE2;
	
	// set accordingly to MultiWii config
	int CH_MIN = 1000;
	int CH_MAX = 2000;
	
	public ProtocolMSP(Controller controller, CommandBuffer commandBuffer,List<Channel> chs) {
		this.controller = controller;
		setCommandBuffer(commandBuffer);
		ch = chs.toArray(new Channel[chs.size()]);
		init();
	}
	
	@Override
	public void init() {
		// initialize control's scaling according to the protocol, here [1000,2000]
		for(int i=0;i<ch.length;i++) {
			ch[i].out_min = CH_MIN;
			ch[i].out_max = CH_MAX;
		}
		
		// MSP expects 	ROLL/PITCH/YAW/THROTTLE/AUX1/AUX2/AUX3AUX4
		//				AILERON/ELEVATOR/RUDDER/THROTTLE/AUX1/AUX2/AUX3AUX4
		// ch are 0:Lx 1:Ly 2:Rx 3:Ry 
		// so do order channels according to controller mode
		switch(ctrlMode) {
		case Controller.MODE1:
			roll=ch[2]; pitch=ch[1]; yaw=ch[0]; throttle=ch[3]; break;
		case Controller.MODE2:
			roll=ch[2]; pitch=ch[3]; yaw=ch[0]; throttle=ch[1]; break;
		case Controller.MODE3:
			roll=ch[0]; pitch=ch[1]; yaw=ch[2]; throttle=ch[3]; break;
		case Controller.MODE4:
			roll=ch[0]; pitch=ch[3]; yaw=ch[2]; throttle=ch[1]; break;
		default:
		}
		
	}
	
	/* (non-Javadoc)
	 * @see sc.arc.comm.protocol.Protocol#eval()
	 * Evaluate the buffer content. Parse the MSP message if header is "$M"
	 */
	@Override
	public void eval() {
		
	}
	
	@Override
	public void requestTM(int...tm) {
		for(int t:tm)
			getCommandBuffer().add( new Command(requestMSP(t,null)) );
	}
	
	@Override
	public void txChannels() {
		mspSetRawRc(roll.getValue(), pitch.getValue(), yaw.getValue(), throttle.getValue());	
	}

	public void mspSetRawRc(int roll, int pitch, int yaw, int throttle) {
		mspSetRawRc(roll,pitch,yaw,throttle,0,0,0,0);
	}
			
	public void mspSetRawRc(int roll, int pitch, int yaw, int throttle, int aux1, int aux2, int aux3, int aux4) {
		// 16x UINT16, Range [1000;2000]
		// ROLL/PITCH/YAW/THROTTLE/AUX1/AUX2/AUX3AUX4

		// old methods
		ArrayList<Character> ca = new ArrayList<Character>();
		ca.addAll(toCharArray(roll));
		ca.addAll(toCharArray(pitch));
		ca.addAll(toCharArray(yaw));
		ca.addAll(toCharArray(throttle));
		ca.addAll(toCharArray(aux1));
		ca.addAll(toCharArray(aux2));
		ca.addAll(toCharArray(aux3));
		ca.addAll(toCharArray(aux4));
		Character[] payload = ca.toArray(new Character[ca.size()]);
		getCommandBuffer().add(new Command(requestMSP(MSP_SET_RAW_RC, payload)));

		/*
		 * ByteBuffer payload = ByteBuffer.allocate(16);
		 * payload.put(toBytes(roll)); payload.put(toBytes(pitch));
		 * payload.put(toBytes(yaw)); payload.put(toBytes(throttle));
		 * payload.put(toBytes(0)); payload.put(toBytes(0));
		 * payload.put(toBytes(0)); payload.put(toBytes(0));
		 */
		
		/*CharBuffer payload = CharBuffer.allocate(16);
		payload.put(toChars(roll));
		payload.put(toChars(pitch));
		payload.put(toChars(yaw));
		payload.put(toChars(throttle));
		payload.put(toChars(0));
		payload.put(toChars(0));
		payload.put(toChars(0));
		payload.put(toChars(0));
		
		/*payload.putChar((char)pitch).putChar((char)yaw).putChar((char)throttle);
		payload.putChar((char)aux1).putChar((char)aux2).putChar((char)aux3).putChar((char)aux4);*/
		//char[] arr = payload.array();
		//getCommandBuffer().add( new Command(requestMSP(MSP_SET_RAW_RC,arr)) );
		
		/*ArrayList<Byte> ca = new ArrayList<Byte>();
		ca.addAll(toCharArray(roll));
		ca.addAll(toCharArray(pitch));
		ca.addAll(toCharArray(yaw));
		ca.addAll(toCharArray(throttle));
		ca.addAll(toCharArray(aux1));
		ca.addAll(toCharArray(aux2));
		ca.addAll(toCharArray(aux3));
		ca.addAll(toCharArray(aux4));
		Character[] payload = ca.toArray(new Character[ca.size()]);
		getCommandBuffer().add( new Command(requestMSP(MSP_SET_RAW_RC,payload)) );*/
		
	}
	
	byte[] toBytes(int in) {
		// low byte first
		return new byte[]{(byte)in,(byte)(in>>8)};
	}
	
	char[] toChars(int in) {
		//l.add((char)(in%256));
		//l.add((char)(in/256));
		return new char[]{(char)(in%256),(char)(in/256)};
		//return new char[]{(char)in,(char)(in>>8)};
	}
	
	
	
	List<Character> toCharArray(int in) {
		ArrayList<Character> l = new ArrayList<Character>();
		//l.add((char)(in%256));
		//l.add((char)(in/256));
		l.add((char)in);
		l.add((char)(in>>8));
		return l;
	}
	
	/*void sendRequestMSP(List<Byte> msp) {
		byte[] arr = new byte[msp.size()];
		int i = 0;
		for (byte b: msp)
			arr[i++] = b;
		
		//g_serial.write(arr); // send the complete byte sequence in one go
	
	}*/

	
	//send msp without payload
	/*private List<Byte> requestMSP(int msp) {
		return  requestMSP( msp, null);
	}

	//send multiple msp without payload
	private List<Byte> requestMSP (int[] msps) {
		List<Byte> s = new LinkedList<Byte>();
		for (int m : msps) {
			s.addAll(requestMSP(m, null));
		}
		return s;
	}*/

	//send msp with payload
	/*private ByteBuffer requestMSP (int msp, byte[] payload) {
		
		if(msp < 0)
			return null;
		
		byte pl_size = (byte)((payload != null ? payload.length : 0)&0xFF);
		
		ByteBuffer bf = ByteBuffer.allocate(pl_size+6);		
		bf.put(MSP_HEADER.getBytes());
		bf.put((byte)(msp&0xFF));
		bf.put(pl_size);
		bf.put(payload);
		
		byte checksum = 0;
		checksum ^= (pl_size&0xFF);
		checksum ^= (msp&0xFF);
		if (payload != null)
			checksum ^= getCRC(payload);
		
		return bf.put(checksum);
	}*/
	
	byte getCRC(byte[] data) {
        byte[] crc = Arrays.copyOf(data, data.length);
        for(int i = 1; i<crc.length; i++)
            crc[0] ^= crc[i];
        return crc[0];
    }
	
	
	private List<Byte> requestMSP(int msp) {
		return  requestMSP( msp, null);
	}

	//send multiple msp without payload
	private List<Byte> requestMSP (int[] msps) {
		List<Byte> s = new LinkedList<Byte>();
		for (int m : msps) {
			s.addAll(requestMSP(m, null));
		}
		return s;
	}
	
	private List<Byte> requestMSP (int msp, Character[] payload) {
		if(msp < 0) {
			return null;
		}
		List<Byte> bf = new LinkedList<Byte>();
		for (byte c : MSP_HEADER.getBytes()) {
			bf.add( c );
		}
		
		byte checksum=0;
		byte pl_size = (byte)((payload != null ? payload.length : 0)&0xFF);
		bf.add(pl_size);
		checksum ^= (pl_size&0xFF);

		bf.add((byte)(msp & 0xFF));
		checksum ^= (msp&0xFF);

		if (payload != null) {
			for (char c :payload){
				bf.add((byte)(c&0xFF));
				checksum ^= (c&0xFF);
			}
		}
		bf.add(checksum);
		return (bf);
	}

	public void evaluateCommand(byte cmd, int dataSize) {
		
		debug = "";
		
		int i, icmd = (int)(cmd&0xFF);
		
		switch(icmd) {
		case MSP_IDENT:
			/*version = read8();
			multiType = read8();
			read8(); // MSP version
			multiCapability = read32();// capability
			if ((multiCapability&1)>0) {buttonRXbind = controlP5.addButton("bRXbind",1,10,yGraph+205-10,55,10); buttonRXbind.setColorBackground(blue_);buttonRXbind.setLabel("RX Bind");}
			if ((multiCapability&4)>0) controlP5.addTab("Motors").show();
			if ((multiCapability&8)>0) flaps=true;
			if (!GraphicsInited)  create_ServoGraphics();*/
			break;

		case MSP_STATUS:
			/*cycleTime = read16();
			i2cError = read16();
			present = read16();
			mode = read32();
			if ((present&1) >0) {buttonAcc.setColorBackground(green_);} else {buttonAcc.setColorBackground(red_);tACC_ROLL.setState(false); tACC_PITCH.setState(false); tACC_Z.setState(false);}
			if ((present&2) >0) {buttonBaro.setColorBackground(green_);} else {buttonBaro.setColorBackground(red_); tBARO.setState(false); }
			if ((present&4) >0) {buttonMag.setColorBackground(green_); Mag_=true;} else {buttonMag.setColorBackground(red_); tMAGX.setState(false); tMAGY.setState(false); tMAGZ.setState(false);}
			if ((present&8) >0) {buttonGPS.setColorBackground(green_);} else {buttonGPS.setColorBackground(red_); tHEAD.setState(false);}
			if ((present&16)>0) {buttonSonar.setColorBackground(green_);} else {buttonSonar.setColorBackground(red_);}

			for(i=0;i<CHECKBOXITEMS;i++) {if ((mode&(1<<i))>0) buttonCheckbox[i].setColorBackground(green_); else buttonCheckbox[i].setColorBackground(red_);}
			confSetting.setValue(read8());
			confSetting.setColorBackground(green_);*/
			break;
		case MSP_RAW_IMU:
			/*ax = read16();ay = read16();az = read16();
			if (ActiveTab=="Motors"){ // Show unfilterd values in graph.
				gx = read16();gy = read16();gz = read16();
				magx = read16();magy = read16();magz = read16(); 
			}else{
				gx = read16()/8;gy = read16()/8;gz = read16()/8;
				magx = read16()/3;magy = read16()/3;magz = read16()/3; 
			}*/
			break;
		case MSP_SERVO:
			/*for(i=0;i<8;i++) servo[i] = read16();*/ 
			break;
		case MSP_MOTOR: 
			/*for(i=0;i<8;i++){ mot[i] = read16();} 
			if (multiType == SINGLECOPTER)servo[7]=mot[0];
			if (multiType == DUALCOPTER){servo[7]=mot[0];servo[6]=mot[1];}*/
			break; 
		case MSP_RC:
			p=0;
			for(i=0;i<8;i++) {
				debug = debug.concat( Integer.toString(read16())).concat(",");
				//RCChan[i]=read16();
				//TX_StickSlider[i].setValue(RCChan[i]);
			}
			
			break;
		case MSP_RAW_GPS:
			/*GPS_fix = read8();
			GPS_numSat = read8();
			GPS_latitude = read32();
			GPS_longitude = read32();
			GPS_altitude = read16();
			GPS_speed = read16(); */
			break;
		case MSP_COMP_GPS:
			/*GPS_distanceToHome = read16();
			GPS_directionToHome = read16();
			GPS_update = read8(); 
			*/
			break;
		case MSP_ATTITUDE:
			/*angx = read16()/10;angy = read16()/10;
			head = read16(); 
			*/
			break;
		case MSP_ALTITUDE: 
			/*alt = read32();*/ break;
		case MSP_ANALOG:
			/*bytevbat = read8();
			pMeterSum = read16();
			rssi = read16(); if(rssi!=0)VBat[5].setValue(rssi).show();  // rssi
			amperage = read16(); // amperage
			VBat[4].setValue(bytevbat/10.0);    // Volt */
			break;
		case MSP_RC_TUNING:
			/*byteRC_RATE = read8();byteRC_EXPO = read8();byteRollPitchRate = read8();
			byteYawRate = read8();byteDynThrPID = read8();
			byteThrottle_MID = read8();byteThrottle_EXPO = read8();
			confRC_RATE.setValue(byteRC_RATE/100.0);
			confRC_EXPO.setValue(byteRC_EXPO/100.0);
			rollPitchRate.setValue(byteRollPitchRate/100.0);
			yawRate.setValue(byteYawRate/100.0);
			dynamic_THR_PID.setValue(byteDynThrPID/100.0);
			throttle_MID.setValue(byteThrottle_MID/100.0);
			throttle_EXPO.setValue(byteThrottle_EXPO/100.0);
			confRC_RATE.setColorBackground(green_);confRC_EXPO.setColorBackground(green_);rollPitchRate.setColorBackground(green_);
			yawRate.setColorBackground(green_);dynamic_THR_PID.setColorBackground(green_);
			throttle_MID.setColorBackground(green_);throttle_EXPO.setColorBackground(green_);
			updateModelMSP_SET_RC_TUNING();*/
			break;
		case MSP_ACC_CALIBRATION:break;
		case MSP_MAG_CALIBRATION:break;
		case MSP_PID:
			/*for(i=0;i<PIDITEMS;i++) {
				byteP[i] = read8();byteI[i] = read8();byteD[i] = read8();
				switch (i) {
				case 0:confP[i].setValue(byteP[i]/10.0);confI[i].setValue(byteI[i]/1000.0);confD[i].setValue(byteD[i]);break;
				case 1:confP[i].setValue(byteP[i]/10.0);confI[i].setValue(byteI[i]/1000.0);confD[i].setValue(byteD[i]);break;
				case 2:confP[i].setValue(byteP[i]/10.0);confI[i].setValue(byteI[i]/1000.0);confD[i].setValue(byteD[i]);break;
				case 3:confP[i].setValue(byteP[i]/10.0);confI[i].setValue(byteI[i]/1000.0);confD[i].setValue(byteD[i]);break;
				case 7:confP[i].setValue(byteP[i]/10.0);confI[i].setValue(byteI[i]/1000.0);confD[i].setValue(byteD[i]);break;
				case 8:confP[i].setValue(byteP[i]/10.0);confI[i].setValue(byteI[i]/1000.0);confD[i].setValue(byteD[i]);break;
				case 9:confP[i].setValue(byteP[i]/10.0);confI[i].setValue(byteI[i]/1000.0);confD[i].setValue(byteD[i]);break;
				//Different rates fot POS-4 POSR-5 NAVR-6
				case 4:confP[i].setValue(byteP[i]/100.0);confI[i].setValue(byteI[i]/100.0);confD[i].setValue(byteD[i]/1000.0);break;
				case 5:confP[i].setValue(byteP[i]/10.0);confI[i].setValue(byteI[i]/100.0);confD[i].setValue(byteD[i]/1000.0);break;
				case 6:confP[i].setValue(byteP[i]/10.0);confI[i].setValue(byteI[i]/100.0);confD[i].setValue(byteD[i]/1000.0);break;
				}
				confP[i].setColorBackground(green_);confI[i].setColorBackground(green_);confD[i].setColorBackground(green_);
			}
			updateModelMSP_SET_PID();*/
			break;
		case MSP_BOX:
			/*for( i=0;i<CHECKBOXITEMS;i++) {
				activation[i] = read16();
				for(int aa=0;aa<12;aa++) {
					if ((activation[i]&(1<<aa))>0) {checkbox[i].activate(aa);}else {checkbox[i].deactivate(aa);}}}*/ 
			break;
		case MSP_BOXNAMES:
			/*create_checkboxes(new String(inBuf, 0, dataSize).split(";"));*/
			break;
		case MSP_PIDNAMES:
			// TODO create GUI elements from this message 
			//System.out.println("Got PIDNAMES: "+new String(inBuf, 0, dataSize));
			break;
		case MSP_SERVO_CONF:
			/*Bbox.deactivateAll();
			// min:2 / max:2 / middle:2 / rate:1 
			for( i=0;i<8;i++){
				ServoMIN[i]   = read16(); 
				ServoMAX[i]   = read16(); 
				ServoMID[i]   = read16(); 
				servoRATE[i]  = read8() ;
			}
			if (multiType == AIRPLANE ) { // Airplane OK
				if(flaps) {
					//ServoSliderC[2].setMin(4).setMax(10);
					if(ServoMID[2]==4) {Cau0();}else if(ServoMID[2]==5) {Cau1();}else if(ServoMID[2]==6) {Cau2();}else if(ServoMID[2]==7){Cau3();}else{CauClear();}
				}
				for( i=0;i<8;i++){
					ServoSliderMIN[i].setValue(ServoMIN[i]); //Update sliders
					ServoSliderMAX[i].setValue(ServoMAX[i]);
					ServoSliderC[i].setValue(ServoMID[i]);
					if (servoRATE[i]>127){ // Reverse/Rate servos
						Bbox.deactivate(i); RateSlider[i].setValue(abs(servoRATE[i]-256));
					}else{
						Bbox.activate(i); RateSlider[i].setValue(abs(servoRATE[i]));
					}
				}

			} else if (multiType == FLYING_WING || multiType == TRI || multiType == BI  || multiType == DUALCOPTER  || multiType == SINGLECOPTER) { // FlyingWing & TRI & BI
				int nBoxes;
				for( i=0;i<8;i++){ //Update sliders
					ServoSliderMIN[i].setValue(ServoMIN[i]);
					ServoSliderMAX[i].setValue(ServoMAX[i]);
					ServoSliderC[i].setValue(ServoMID[i]);
					if (servoRATE[i]>127){ // Reverse/Rate servos
						wingDir[i]=-1; RateSlider[i].setValue((servoRATE[i]-256));
					}else{ wingDir[i]=1; RateSlider[i].setValue(abs(servoRATE[i])); } // Servo Direction
				}

				if(multiType == FLYING_WING) { //OK
					if ((servoRATE[3]&1)<1) {Wbox.deactivate(2);}else{Wbox.activate(2);} //
					if ((servoRATE[3]&2)<1) {Wbox.deactivate(0);}else{Wbox.activate(0);} //
					if ((servoRATE[4]&1)<1) {Wbox.deactivate(3);}else{Wbox.activate(3);} //
					if ((servoRATE[4]&2)<1) {Wbox.deactivate(1);}else{Wbox.activate(1);} //


				} else if(multiType == SINGLECOPTER) { //
					if ((servoRATE[3]&1)<1) {Wbox.deactivate(0);}else{Wbox.activate(0);} //
					if ((servoRATE[3]&2)<1) {Wbox.deactivate(1);}else{Wbox.activate(1);} //
					if ((servoRATE[4]&1)<1) {Wbox.deactivate(2);}else{Wbox.activate(2);} //
					if ((servoRATE[4]&2)<1) {Wbox.deactivate(3);}else{Wbox.activate(3);} //
					if ((servoRATE[5]&1)<1) {Wbox.deactivate(4);}else{Wbox.activate(4);} //
					if ((servoRATE[5]&2)<1) {Wbox.deactivate(5);}else{Wbox.activate(5);} //
					if ((servoRATE[6]&1)<1) {Wbox.deactivate(6);}else{Wbox.activate(6);} //
					if ((servoRATE[6]&2)<1) {Wbox.deactivate(7);}else{Wbox.activate(7);} //


				} else if(multiType == DUALCOPTER) { // OK
					if ((servoRATE[4]&1)<1) {Wbox.deactivate(0);}else{Wbox.activate(0);}
					if ((servoRATE[5]&1)<1) {Wbox.deactivate(1);}else{Wbox.activate(1);}


				}  else if (multiType == TRI) {// OK
					if ((servoRATE[5]&1)<1) {Wbox.deactivate(0);}else{Wbox.activate(0);}

				} else if( multiType == BI) {// OK
					if ((servoRATE[4]&2)<1) {Wbox.deactivate(0);}else{Wbox.activate(0);} // L
					if ((servoRATE[5]&2)<1) {Wbox.deactivate(1);}else{Wbox.activate(1);}
					if ((servoRATE[4]&1)<1) {Wbox.deactivate(2);}else{Wbox.activate(2);} // R
					if ((servoRATE[5]&1)<1) {Wbox.deactivate(3);}else{Wbox.activate(3);}
				}

			}else if (multiType == HELI_120_CCPM ||  multiType == HELI_90_DEG) {
				for( i=0;i<8;i++) { //Update sliders
					ServoSliderMIN[i].setValue(ServoMIN[i]);
					ServoSliderMAX[i].setValue(ServoMAX[i]);
					ServoSliderC[i].setValue(ServoMID[i]);

					if (servoRATE[i]>127){ // Reverse/Rate servos
						Bbox.deactivate(i); RateSlider[i].setValue((servoRATE[i]-256));
					}else{ Bbox.activate(i); RateSlider[i].setValue(abs(servoRATE[i]));}
				}
				if ((servoRATE[5]&1)<1) {Bbox.deactivate(5);}else{Bbox.activate(5);} // YawReverse
				if(multiType == HELI_120_CCPM) { //bbb
					if ((servoRATE[3]&1)<1) {Mbox.deactivate(2);}else{Mbox.activate(2);} // roll
					if ((servoRATE[3]&2)<1) {Mbox.deactivate(1);}else{Mbox.activate(1);} // nick
					if ((servoRATE[3]&4)<1) {Mbox.deactivate(0);}else{Mbox.activate(0);} // coll
					if ((servoRATE[4]&1)<1) {Mbox.deactivate(5);}else{Mbox.activate(5);} //
					if ((servoRATE[4]&2)<1) {Mbox.deactivate(4);}else{Mbox.activate(4);} //
					if ((servoRATE[4]&4)<1) {Mbox.deactivate(3);}else{Mbox.activate(3);} //
					if ((servoRATE[6]&1)<1) {Mbox.deactivate(8);}else{Mbox.activate(8);} //
					if ((servoRATE[6]&2)<1) {Mbox.deactivate(7);}else{Mbox.activate(7);} //
					if ((servoRATE[6]&4)<1) {Mbox.deactivate(6);}else{Mbox.activate(6);} //
				}

			}else if (multiType == PPM_TO_SERVO ) { // PPM_TO_SERVO
				for( i=0;i<8;i++){
					ServoSliderMIN[i].setValue(ServoMIN[i]); //Update sliders
					ServoSliderMAX[i].setValue(ServoMAX[i]);
					ServoSliderC[i]  .setValue(ServoMID[i]);
					// Reverse/Rate servos
					if (servoRATE[i]>127){
						Bbox.deactivate(i); RateSlider[i].setValue(abs(servoRATE[i]-256));
					}else{Bbox.activate(i); RateSlider[i].setValue(abs(servoRATE[i]));}
				}
			}

			if (gimbal){
				if(!gimbalConfig)create_GimbalGraphics(); 
				// Switch beween Channels or Centerpos.
				if(ServoMID[0]>1200) {GimbalSlider[2] .setMin(1200).setMax(1700); }else{GimbalSlider[2] .setMin(0).setMax(12);}
				if(ServoMID[1]>1200) {GimbalSlider[6] .setMin(1200).setMax(1700); }else{GimbalSlider[6] .setMin(0).setMax(12);}
				if(ServoMID[2]>1000) {GimbalSlider[10].setMin(1000).setMax(30000);}else{GimbalSlider[10].setMin(0).setMax(12);}


				i=0;
				GimbalSlider[0] .setValue((int)ServoMIN[i]);
				GimbalSlider[1] .setValue((int)ServoMAX[i]);
				GimbalSlider[2] .setValue((int)ServoMID[i]);
				if (servoRATE[i]>127){ GimbalSlider[3].setValue((servoRATE[i]-256));
				}else{ GimbalSlider[3].setValue(abs(servoRATE[i]));}

				i=1;
				GimbalSlider[4] .setValue((int)ServoMIN[i]);
				GimbalSlider[5] .setValue((int)ServoMAX[i]);
				GimbalSlider[6] .setValue((int)ServoMID[i]);         
				if (servoRATE[i]>127){ GimbalSlider[7].setValue((servoRATE[i]-256));
				}else{GimbalSlider[7].setValue(abs(servoRATE[i]));}

				i=2;
				GimbalSlider[8] .setValue((int)ServoMIN[i]);
				GimbalSlider[9] .setValue((int)ServoMAX[i]);
				GimbalSlider[10].setValue((int)ServoMID[i]);
				GimbalSlider[11].setValue((int)servoRATE[i]);
			}
			if (camTrigger){
				if(ServoMID[2]>1200) {ServoSliderC[2].setMin(Centerlimits[0]).setMax(Centerlimits[1]);}else{ServoSliderC[2].setMin(0).setMax(12);}       
			}

			if(ExportServo) SAVE_SERVO_CONFIG(); // ServoConfig to file */
			break;
		case MSP_MISC:
			/*intPowerTrigger = read16(); // a

			//int minthrottle,maxthrottle,mincommand,FSthrottle,armedNum,lifetime,mag_decliniation ;
			for (i=0;i<4;i++) { MConf[i]= read16(); 
			confINF[i].setValue((int)MConf[i]).show(); 
			}
			if(MConf[3]<1000)confINF[3].hide();          

			// LOG_PERMANENT        
			MConf[4]= read16(); confINF[4].setValue((int)MConf[4]);//f
			MConf[5]= read32(); confINF[5].setValue((int)MConf[5]);//g        
			for (i=1;i<3;i++){confINF[i].setColorBackground(grey_).setMin((int)MConf[i]).setMax((int)MConf[i]);} //?

			// hide LOG_PERMANENT
			if(MConf[4]<1){confINF[5].hide();confINF[4].hide();}else{confINF[5].show();confINF[4].show();}

			//mag_decliniation
			MConf[6]= read16(); confINF[6].setValue((float)MConf[6]/10).show(); //h
			if(!Mag_)confINF[6].hide();        

			// VBAT
			int q = read8();if(toggleVbat){VBat[0].setValue(q).setColorBackground(green_);toggleVbat=false; // i
			for( i=1;i<4;i++) VBat[i].setValue(read8()/10.0).setColorBackground(green_);}  // j,k,l
			if(q > 1) for( i=0;i<5;i++) VBat[i].show();

			controlP5.addTab("Config").show();

			confPowerTrigger.setValue(intPowerTrigger);
			updateModelMSP_SET_MISC();*/
			break;
		case MSP_MOTOR_PINS:
			/*for( i=0;i<8;i++) {byteMP[i] = read8();}*/
			break;
		case MSP_DEBUGMSG:
			/*while(dataSize-- > 0) {
				char c = (char)read8();
				if (c != 0) {System.out.print( c );}
			}*/
			break;
		case MSP_DEBUG:
			/*debug1 = read16();debug2 = read16();debug3 = read16();debug4 = read16();*/ 
			break;
		default:
			//println("Don't know how to handle reply "+icmd);
		}
	}

	int read32() {
		return (inBuf[p++]&0xff) + ((inBuf[p++]&0xff)<<8) + ((inBuf[p++]&0xff)<<16) + ((inBuf[p++]&0xff)<<24); 
	}
	int read16() {
		return (inBuf[p++]&0xff) + ((inBuf[p++])<<8); 
	}
	int read8() {
		return  inBuf[p++]&0xff;
	}

}