package sc.arc.comm.rc;

import it.sephiroth.android.library.easing.EasingManager;
import it.sephiroth.android.library.easing.EasingManager.EasingCallback;
import it.sephiroth.android.library.easing.Elastic;

public class Channel implements EasingCallback {

	private boolean isSwitch 	= false;
	private boolean inverted 	= false;
	private boolean isSpring  	= false;
	private boolean isDirty  	= false;
			
	public int out_min 		= 0;
	public int out_max 		= 100;

	
	private float 	raw_v 	= 0;	// raw value [px]
	private float 	raw_c 	= 0;	// raw center of the slider
	private float 	raw_m 	= 0;	// raw max value (full excursion)
	private float 	raw_s 	= 50;	// raw stick radius
	private int 	nrm_v 	= 0;	// normalised value
	
	private int 	tol_c 	= 20;	// tolerance of the center position
	
	public Channel() {
	}
		
	public void init() {
		raw_c = raw_m/2;	// calculate central position
		setRaw(raw_c);		// move the stick there
		setDirty(true);
	}
	
	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}

	public boolean isCatched(float v) {
		return Math.abs(v-raw_v)<raw_s;
	}
	
	public boolean isCentered() {
		return Math.abs(raw_v-raw_c)<tol_c;
	}
	
	public boolean isSwitch() {
		return isSwitch;
	}

	public boolean isSpring() {
		return isSpring;
	}
	
	public Channel setSpring(boolean spring) {
		isSpring = spring;
		return this;
	}
	
	public void setSwitch(boolean isSwitch) {
		this.isSwitch = isSwitch;
	}
	
	public void setRaw(float raw) {
		raw_v = Math.max( Math.min(raw,raw_m-raw_s), raw_s);
		//nrm_v = Math.round(map(raw_v,raw_s,raw_m-raw_s,0,norm));
		nrm_v = Math.round(map(raw_v,raw_s,raw_m-raw_s,out_min,out_max));
		nrm_v = inverted?(out_max-nrm_v)+out_min:nrm_v;
		//setDirty(true);
	}
	public void setNrm(float nrm_v) {
		//nrm_v = Math.max( Math.min(raw,out-raw_s), raw_s);
		//nrm_v = Math.round(map(raw_v,raw_s,raw_m-raw_s,0,norm));
		//nrm_v = Math.round(map(raw_v,raw_s,raw_m-raw_s,out_min,out_max));
		nrm_v = inverted?(out_max-nrm_v)+out_min:nrm_v;
		//setDirty(true);
	}

	private float map(float x, float in_min, float in_max, float out_min, float out_max) {
	  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}
	
	public float getValueRaw() {
		return raw_v;
	}
	
	public byte getByte() {
		return (byte)nrm_v;
	}
	public byte getByte2c() {
		return (byte)~(nrm_v);
	}
	
	/**
	 * @return normalised value [min,max]
	 */
	public int getValue() {
		return nrm_v;
	}
	
	public float getRawM() {
		return raw_m;
	}
	public Channel setRawM(float value) {
		raw_m = value;
		return this;
	}
	public float getRawC() {
		return raw_c;
	}
	
	public float getRawS() {
		return raw_s;
	}
	
	public String getNrmS() {
		return Integer.toString(nrm_v);
	}
	public boolean isInverted() {
		return inverted;
	}
	
	public Channel setInverted(boolean inverted) {
		this.inverted = inverted;
		init(); // to refresh current value
		return this;
	}
	
	// easing management
	EasingManager.EaseType mEaseType = EasingManager.EaseType.EaseInOut;
	EasingManager manager = new EasingManager(this);
	private boolean complete = true;
	
	public void makeEasing() {
		if(complete) {
			manager.start(Elastic.class, mEaseType, raw_c, raw_v, 500, 50);
			complete = false;
		}
	}
	
	@Override
	public void onEasingValueChanged(double value, double oldValue) {
		if(!isCentered())
			setRaw((float)value);
	}

	@Override
	public void onEasingStarted(double value) {
		if(!isCentered())
			setRaw((float) value);
	}

	@Override
	public void onEasingFinished(double value) {
		setRaw((float)raw_c);
		complete  = true;
		manager.stop();
	}
}
