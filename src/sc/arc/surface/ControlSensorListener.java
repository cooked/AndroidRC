package sc.arc.surface;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.TextView;

public class ControlSensorListener implements SensorEventListener {

	ControlSurface vl, vr;
	TextView label;
	
	private final float[] mRotationMatrix = new float[16];
	
	public ControlSensorListener(View vl, View vr, TextView myLabel) {
		this.vl = ((ControlSurface)vl);
		this.vr = ((ControlSurface)vr);
		label = myLabel;
		
		// initialize the rotation matrix to identity
        mRotationMatrix[ 0] = 1;
        mRotationMatrix[ 4] = 1;
        mRotationMatrix[ 8] = 1;
        mRotationMatrix[12] = 1;
        
	}

	float vert;
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()) {
			case Sensor.TYPE_PRESSURE:
				doAltitude(event);	break;
			//case Sensor.TYPE_GRAVITY:
			//	doGravity(event);
			case Sensor.TYPE_ROTATION_VECTOR: 
				doRotation(event);	break;
			default:
		}

	}
	
	private void doAltitude(SensorEvent event) {
		vert = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]);
		//label.setText(Float.toString(vert));
	}
	
	private void doRotation(SensorEvent event) {
		float[] lastRotVal 	= new float[5];
		float[] orientation = new float[3];
		
	    try{
	        System.arraycopy(event.values, 0, lastRotVal, 0, event.values.length); 
	    } catch (IllegalArgumentException e) {
	        //Hardcode the size to handle a bug on Samsung devices running Android 4.3
	        System.arraycopy(event.values, 0, lastRotVal, 0, 3); 
	    }

	    SensorManager.getRotationMatrixFromVector(mRotationMatrix, lastRotVal);
	    SensorManager.getOrientation(mRotationMatrix, orientation);

	    double azimuth = orientation[0];
	    double pitch = orientation[1];
	    double roll = orientation[2];
	    
	    //label.setText(Double.toString(pitch) + "/" + Double.toString(roll) + "/" + Double.toString(azimuth));
	    //label.setText(Double.toString( map(pitch,-Math.PI/2,Math.PI/2,1000,2000) ));
	    //vl.setStickNrm(0,map((float)pitch,-1.6f,1.6f,1000,2000));
	    
	}
	
	private double map(double x, double in_min, double in_max, double out_min, double out_max) {
		  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
		}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {	
	}

}
