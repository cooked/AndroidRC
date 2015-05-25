package sc.arc.surface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup.LayoutParams;
import it.sephiroth.android.library.easing.Easing;
import it.sephiroth.android.library.easing.EasingManager;
import it.sephiroth.android.library.easing.Linear;
import sc.arc.comm.rc.Channel;

public class ControlSurface extends SurfaceView implements SurfaceHolder.Callback {

	// Surface holder allows to control and monitor the surface
	private SurfaceHolder mHolder;

	// A thread where the painting activities are taking place
	private DrawThread mThread;

	// A flag which controls the start and stop of the repainting of the SurfaceView
	private boolean mFlag = false;

	public Channel chx,chy;
	
	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		setOnTouchListener(new ControlTouchListener());
		
		chx.setRawM(getWidth()).init();
		chy.setRawM(getHeight()).init();
		
		mThread = new DrawThread(getContext());
		mThread.setRunning(true);
		mThread.start();
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		//mThread.setSurfaceSize(width, height);
		
		//mThread.setRunning(false);
		
		chx.setRawM(getWidth()).init();
		chy.setRawM(getHeight()).init();
		
		//mThread.setRunning(true);
		//mThread.start();
		
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
		
		// Initializing the paint object mPaint
		mPaint.setTextSize(40);
		mPaint.setColor(Color.RED);
		mPaint.setStyle(Style.FILL);
		
		chx = new Channel();
		chy = new Channel();

	}

	public void resume() {
		// Instantiating the thread
		//if(mThread==null)
		//	mThread = new BubbleThread(getContext());
		//mThread.setRunning(true);
		//mThread.start();
	}

	public void pause() {
		mThread.setRunning(false);
	}
	
	public void setStick(float x, float y) {
		chx.setRaw(x);
		chy.setRaw(y);
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

	
	class DrawThread extends Thread {

		private boolean run = false;

		public DrawThread(Context context) {
			//ctx = context;
		}

		public void run() {
			while (run) {
				Canvas c = null;
				try {
					c = mHolder.lockCanvas(null);
					synchronized (mHolder) {
						doDraw(c);
					}
				} finally {
					if (c != null) {
						mHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}

		public void setRunning(boolean b) { 
			run = b;
		}

		private void doDraw(Canvas canvas) {
			//canvas.restore();
			canvas.drawARGB(255, 0, 0, 0);
			canvas.drawCircle(chx.getValueRaw(), chy.getValueRaw(), chy.getRawS(), mPaint);
			canvas.drawLine(chx.getValueRaw(), 0, chx.getValueRaw(), chy.getRawM(), mPaint); // vertical
			canvas.drawLine(0, chy.getValueRaw(), chx.getRawM(), chy.getValueRaw(), mPaint); // horizontal

			//canvas.drawText(String.format("%02X ", chy.getByte()), 0, chy.getRaw(), mPaint);
			canvas.drawText(chy.getNrmS(), 0, chy.getValueRaw(), mPaint);
			canvas.drawText(chx.getNrmS(), chx.getValueRaw(), 32, mPaint);
			//canvas.save();

		}
	}
	
}