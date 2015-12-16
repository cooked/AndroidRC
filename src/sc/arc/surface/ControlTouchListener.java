package sc.arc.surface;

import java.util.ArrayList;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import sc.arc.comm.rc.Channel;

public class ControlTouchListener implements OnTouchListener {

	boolean grabbed = false;
	boolean move	= false;
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		ArrayList<Channel> chs = ((ControlSurface)v).getChs();
		float vx=0,vy=0, x=0,y=0;
		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			vx = event.getX() - v.getLeft();
			vy = event.getY() - v.getTop();
			if(chs.get(0).isCatched(vx) && chs.get(1).isCatched(vy))
				grabbed = true;
			
		case MotionEvent.ACTION_MOVE:
			vx = event.getX() - v.getLeft();
			vy = event.getY() - v.getTop();
			if(grabbed)		
				((ControlSurface)v).setStick(vx,vy);
            return true;
            
		case MotionEvent.ACTION_UP:

			if(grabbed)	{	
				//((ControlSurface)v).setStick(vx,vy);
				for(Channel ch:chs)
					if(ch.isSpring()) ch.makeEasing();
				v.performClick();
			}
			grabbed = false;
			move	= false;
			return true;
			
		}
        return false;
	}
	

}
