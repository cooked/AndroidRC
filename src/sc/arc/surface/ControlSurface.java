package sc.arc.surface;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import sc.arc.comm.rc.Channel;

public class ControlSurface extends SurfaceView implements SurfaceHolder.Callback {

	// Surface holder allows to control and monitor the surface
	private SurfaceHolder mHolder;

	// A thread where the painting activities are taking place
	private DrawThread mThread;

	// A flag which controls the start and stop of the repainting of the SurfaceView
	private boolean mFlag = false;

	public Channel chx,chy;
	
	int color 		= Color.RED;
	int background	= Color.BLACK;
	
	// colors palette
	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);			
 		
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		// setup touch controller
		setOnTouchListener(new ControlTouchListener());
		
		chx.setRawM(getWidth()).init();
		chy.setRawM(getHeight()).init();
		
		int front = Color.parseColor("#cddc39"); // green Android
		
		//ctx = context;
		mPaint.setStyle(Style.FILL);
		mPaint.setColor(front);
		mPaint.setTextSize(40);
		
		mThread = new DrawThread(getContext());
		mThread.setRunning(true);
		mThread.start();
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
		chx.setRawM(getWidth()).init();
		chy.setRawM(getHeight()).init();
		
		mThread.setRunning(false);
		mThread = new DrawThread(getContext());
		mThread.setRunning(true);
		mThread.start();
		
	}

	public void setColor(int color) {
		this.color = color; 
	}
	
	public void setColorBack(int color) {
		this.background = color; 
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		mThread.setRunning(false);
		while (retry) {
			try {
				mThread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	public ControlSurface(Context context, AttributeSet set) {
		// TODO create a map of config params to be passed: key:value		
		super(context,set);

		// make sure we get key events
		setFocusable(true); 
		
		// Getting the holder
		mHolder = getHolder();
		mHolder.addCallback(this);
		
		chx = new Channel();
		chy = new Channel();
		
		// creating new gesture detector
        //gestureDetector = new GestureDetector(context, new GestureListener());
	}

	public void resume() {
		// Instantiating the thread
		if(mThread!=null && mThread.isAlive()) {
			mThread.setRunning(false);
			mThread = new DrawThread(getContext());
			mThread.setRunning(true);
			mThread.start();
		}
	}

	public void pause() {	
		mThread.setRunning(false);
	}
	
	public void setStick(float x, float y) {
		chx.setRaw(x);
		chy.setRaw(y);
	}
	public void setStickNrm(float x, float y) {
		chx.setNrm(x);
		chy.setNrm(y);
	}
	
	public ArrayList<Channel> getChs() {
		return new ArrayList<Channel>(Arrays.asList(chx,chy));
	}
	
	/*public void resetStick() {
		setStick(chx.getRawC(), chy.getRawC());
	}*/
	
	private void clearDirty() {
		chx.setDirty(false);
		chy.setDirty(false);
	}

	boolean kill = false;
	
	class DrawThread extends Thread {

		private boolean run = false;
		
		public DrawThread(Context context) {

		}

		public void run() {
			
			while(run) {
					Canvas c = null;
					try {
						c = mHolder.lockCanvas(null);
						synchronized (mHolder) {
							doDraw(c);
						}
					} finally {
						if (c != null)
							mHolder.unlockCanvasAndPost(c);
					}
			}
		}

		public void setRunning(boolean b) { 
			run = b;
		}

		private void doDraw(Canvas canvas) {
			//canvas.restore();
			canvas.drawColor(background);
			canvas.drawCircle(chx.getValueRaw(), chy.getValueRaw(), chy.getRawS(), mPaint);
			canvas.drawLine(chx.getValueRaw(), 0, chx.getValueRaw(), chy.getRawM(), mPaint); // vertical
			canvas.drawLine(0, chy.getValueRaw(), chx.getRawM(), chy.getValueRaw(), mPaint); // horizontal

			//canvas.drawText(String.format("%02X ", chy.getByte()), 0, chy.getRaw(), mPaint);
			canvas.drawText(chx.getNrmS(), 0, chy.getValueRaw(), mPaint);
			canvas.drawText(chy.getNrmS(), chx.getValueRaw(), 32, mPaint);
			//canvas.save();

		}
	}
	
}