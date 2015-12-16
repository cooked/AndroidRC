package sc.arc.surface;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.widget.TextView;

public class ControlGravity {

	//moving linearly up/down => move drone up/down
	//rotate left/right => move drone left/right (with altitude hold, that means we need to augment thrust as well - done by PID?boh )
	//rotate fore/aft 
	//ratate above Z => spin the drone until rotation is released (back to initial hand position)
	
	SensorManager 	mSensorManager;
	Context 		ctx;
	
	ControlSensorListener csl;
	private Sensor mRotationVectorSensor;
	
	public ControlGravity(Context ctx, ControlSurface sx, ControlSurface dx, TextView myLabel) {

	}
	
}
