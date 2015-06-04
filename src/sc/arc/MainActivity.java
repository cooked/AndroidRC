package sc.arc;

import java.util.Arrays;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import sc.arc.comm.controller.ControllerBT;
import sc.arc.comm.controller.ControllerTCP;
import sc.arc.comm.controller.IController;
import sc.arc.settings.SettingsFragmentNetwork;
import sc.arc.surface.ControlSurface;
import sc.arc.util.SystemUiHider;

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

	SharedPreferences sharedPref;
	int connType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		final View contentView = findViewById(R.id.fullscreen_content);
		final TextView myLabel = (TextView) findViewById(R.id.myLabel);	
		
		// Getting reference to the control surfaces
		ctrlSrfSx = (ControlSurface) findViewById(R.id.surf_sx);
		ctrlSrfSx.chy.setInverted(true);
		ctrlSrfSx.setColorBack(getResources().getColor(R.color.black_overlay));
		ctrlSrfDx = (ControlSurface) findViewById(R.id.surf_dx);
		ctrlSrfDx.chy.setInverted(true);
		ctrlSrfDx.setColorBack(getResources().getColor(R.color.black_overlay));
		
		PreferenceManager.setDefaultValues(this, R.xml.pref_connection, false);
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		
		loadPref();
		if(connType==2)
			ctrl = new ControllerBT(this, myLabel, Arrays.asList(ctrlSrfSx.chx, ctrlSrfSx.chy, ctrlSrfDx.chx, ctrlSrfDx.chy));
		else if (connType==1)
			ctrl = new ControllerTCP(this, myLabel, Arrays.asList(ctrlSrfSx.chx, ctrlSrfSx.chy, ctrlSrfDx.chx, ctrlSrfDx.chy));
		else
			ctrl=null;
		
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
					//	mControlsHeight = controlsView.getHeight();
					}
					if (mShortAnimTime == 0) {
						//mShortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
					}
					//controlsView.animate().translationY(visible ? 0 : mControlsHeight).setDuration(mShortAnimTime);
				} else {
					// If the ViewPropertyAnimator APIs aren't
					// available, simply show or hide the in-layout UI
					// controls.
					//controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
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

	MenuInflater inflater;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		inflater = getMenuInflater();
		inflater.inflate(R.menu.action_bar, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// action with ID action_refresh was selected
			// action with ID action_settings was selected
		case R.id.action_connect:
			if(ctrl==null) {
				Toast.makeText(getApplicationContext(), "No controller!", Toast.LENGTH_LONG).show();
				return false;
			}
			if(!ctrl.isConnected()) {
				if(ctrl.discover()) {
					if(ctrl.connect()) {
						Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
						while(!ctrl.isConnected()){};
						item.setIcon(R.drawable.ic_conn_green);
						Toast.makeText(getApplicationContext(), R.string.conn_wifi_connect, Toast.LENGTH_LONG).show();
					} else {
						item.setIcon(R.drawable.ic_conn_grey);
					}
				}
			} else {
				if(ctrl.isRunning())
					Toast.makeText(getApplicationContext(), "ERROR! stop control first", Toast.LENGTH_LONG).show();
				else {
					ctrl.disconnect();
					while(ctrl.isConnected()){};
					Toast.makeText(getApplicationContext(), "disconnected", Toast.LENGTH_LONG).show();
					item.setIcon(R.drawable.ic_conn_grey);
				}
			}
			
			break;
		case R.id.action_rtx:
			if(ctrl==null) {
				Toast.makeText(getApplicationContext(), "No controller!", Toast.LENGTH_LONG).show();
				return false;
			}
			if(ctrl.isConnected()) {
				if(ctrl.isRunning()) {
					ctrl.stop();
					item.setIcon(R.drawable.ic_action_stop);
				} else {
					ctrl.start();
					item.setIcon(R.drawable.ic_action_play);
				}
			}
			break;
		case R.id.action_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivityForResult(intent, 99);
			break;
		default:
		}

		return true;

	}

	
	@Override
	 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//super.onActivityResult(requestCode, resultCode, data);
		//if(requestCode==99)
		//	loadPref();
	 }
	    
	 private void loadPref(){
		 connType = Integer.parseInt( sharedPref.getString(SettingsFragmentNetwork.KEY_CONN_TYPE, "None") );
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
