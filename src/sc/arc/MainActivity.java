package sc.arc;

import java.util.Arrays;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import sc.arc.comm.controller.ControllerBT;
import sc.arc.comm.controller.ControllerTCP;
import sc.arc.comm.controller.IController;
import sc.arc.surface.ControlSurface;
import sc.arc.surface.ControlTouchListener;
import sc.arc.util.SystemUiHider;
import sc.arc.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends Activity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = false;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	// controls
	private ControlSurface ctrlSrfSx;
	private ControlSurface ctrlSrfDx;

	// comm
	private IController ctrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);
		final TextView myLabel = (TextView) findViewById(R.id.myLabel);

		// Getting reference to the control surfaces
		ctrlSrfSx = (ControlSurface) findViewById(R.id.surf_sx);
		//ctrlSrfSx.chx.setSpring(true);
		ctrlSrfSx.chy.setInverted(true);
		ctrlSrfDx = (ControlSurface) findViewById(R.id.surf_dx);
		ctrlSrfDx.chy.setInverted(true);

		// initialise comm channel
		//ctrl = new ControllerBT(this, myLabel, Arrays.asList(ctrlSrfSx.chx, ctrlSrfSx.chy, ctrlSrfDx.chx, ctrlSrfDx.chy));
		ctrl = new ControllerTCP(this, myLabel, Arrays.asList(ctrlSrfSx.chx, ctrlSrfSx.chy, ctrlSrfDx.chx, ctrlSrfDx.chy));

		
		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
			// Cached values.
			int mControlsHeight;
			int mShortAnimTime;

			@Override
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
			public void onVisibilityChange(boolean visible) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
					// If the ViewPropertyAnimator API is available
					// (Honeycomb MR2 and later), use it to animate the
					// in-layout UI controls at the bottom of the
					// screen.
					if (mControlsHeight == 0) {
						mControlsHeight = controlsView.getHeight();
					}
					if (mShortAnimTime == 0) {
						mShortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
					}
					controlsView.animate().translationY(visible ? 0 : mControlsHeight).setDuration(mShortAnimTime);
				} else {
					// If the ViewPropertyAnimator APIs aren't
					// available, simply show or hide the in-layout UI
					// controls.
					controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
				}
			}
		});

		// Set up the user interaction to manually show or hide the system UI.
		/*
		 * contentView.setOnClickListener(new View.OnClickListener() {
		 * 
		 * @Override public void onClick(View view) { if (TOGGLE_ON_CLICK) {
		 * mSystemUiHider.toggle(); } else { mSystemUiHider.show(); } } });
		 */

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		// b_send.setOnTouchListener(mDelayHideTouchListener);

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		// delayedHide(100);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.action_bar, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		return super.onPrepareOptionsMenu(menu);
	}

	boolean isStarted = false;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// action with ID action_refresh was selected
			// action with ID action_settings was selected
		case R.id.action_connect:
			if (ctrl.discover())
				ctrl.connect();
			break;
		case R.id.action_rtx:
			if (isStarted)
				isStarted = ctrl.stop();
			else
				isStarted = ctrl.start();
			break;
		case R.id.action_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			//EditText editText = (EditText) findViewById(R.id.edit_message);
		    //String message = editText.getText().toString();
		    //intent.putExtra(EXTRA_MESSAGE, message);
			startActivity(intent);
			break;
		default:
			break;
		}

		return true;
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	/*
	 * View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener()
	 * {
	 * 
	 * @Override public boolean onTouch(View view, MotionEvent motionEvent) { if
	 * (AUTO_HIDE) { delayedHide(AUTO_HIDE_DELAY_MILLIS); } return false; } };
	 */

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			// mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	@Override
	protected void onResume() {
		super.onResume();
		ctrlSrfSx.resume();
		ctrlSrfDx.resume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		ctrlSrfSx.pause();
		ctrlSrfDx.pause();
	}

	@Override
	public void finish() {
		ctrl.disconnect();
		super.finish();
	}

}
