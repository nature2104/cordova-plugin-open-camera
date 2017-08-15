package com.cordova.mediacapture.OpenCamera.UI;

import com.cordova.mediacapture.OpenCamera.CaptureActivity;
import com.cordova.mediacapture.OpenCamera.MyDebug;
import com.cordova.mediacapture.OpenCamera.PreferenceKeys;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.ZoomControls;

/** This contains functionality related to the main UI.
 */
public class MainUI {
	private static final String TAG = "MainUI";

	private final CaptureActivity main_activity;

	private volatile boolean popup_view_is_open; // must be volatile for test project reading the state
    private PopupView popup_view;
	private final static boolean cache_popup = true; // if false, we recreate the popup each time

    private int current_orientation;
	private boolean ui_placement_right = true;

	private boolean immersive_mode;
    private boolean show_gui = true; // result of call to showGUI() - false means a "reduced" GUI is displayed, whilst taking photo or video

	private boolean keydown_volume_up;
	private boolean keydown_volume_down;

	public MainUI(CaptureActivity main_activity) {
		if( MyDebug.LOG )
			Log.d(TAG, "MainUI");
		this.main_activity = main_activity;
		
		this.setSeekbarColors();

		this.setIcon(main_activity.getResources().getIdentifier("gallery", "id", main_activity.getPackageName()));
		this.setIcon(main_activity.getResources().getIdentifier("settings", "id", main_activity.getPackageName()));
		this.setIcon(main_activity.getResources().getIdentifier("popup", "id", main_activity.getPackageName()));
		this.setIcon(main_activity.getResources().getIdentifier("exposure_lock", "id", main_activity.getPackageName()));
		this.setIcon(main_activity.getResources().getIdentifier("exposure", "id", main_activity.getPackageName()));
		this.setIcon(main_activity.getResources().getIdentifier("switch_video", "id", main_activity.getPackageName()));
		this.setIcon(main_activity.getResources().getIdentifier("switch_camera", "id", main_activity.getPackageName()));
		this.setIcon(main_activity.getResources().getIdentifier("audio_control", "id", main_activity.getPackageName()));
		this.setIcon(main_activity.getResources().getIdentifier("trash", "id", main_activity.getPackageName()));
		this.setIcon(main_activity.getResources().getIdentifier("share", "id", main_activity.getPackageName()));
	}
	
	private void setIcon(int id) {
		if( MyDebug.LOG )
			Log.d(TAG, "setIcon: " + id);
	    ImageButton button = (ImageButton)main_activity.findViewById(id);
	    button.setBackgroundColor(Color.argb(63, 63, 63, 63)); // n.b., rgb color seems to be ignored for Android 6 onwards, but still relevant for older versions
	}
	
	private void setSeekbarColors() {
		if( MyDebug.LOG )
			Log.d(TAG, "setSeekbarColors");
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
			ColorStateList progress_color = ColorStateList.valueOf( Color.argb(255, 240, 240, 240) );
			ColorStateList thumb_color = ColorStateList.valueOf( Color.argb(255, 255, 255, 255) );

			SeekBar seekBar = (SeekBar)main_activity.findViewById(main_activity.getResources().getIdentifier("zoom_seekbar", "id", main_activity.getPackageName()));
			seekBar.setProgressTintList(progress_color);
			seekBar.setThumbTintList(thumb_color);

			seekBar = (SeekBar)main_activity.findViewById(main_activity.getResources().getIdentifier("focus_seekbar", "id", main_activity.getPackageName()));
			seekBar.setProgressTintList(progress_color);
			seekBar.setThumbTintList(thumb_color);

			seekBar = (SeekBar)main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_seekbar", "id", main_activity.getPackageName()));
			seekBar.setProgressTintList(progress_color);
			seekBar.setThumbTintList(thumb_color);

			seekBar = (SeekBar)main_activity.findViewById(main_activity.getResources().getIdentifier("iso_seekbar", "id", main_activity.getPackageName()));
			seekBar.setProgressTintList(progress_color);
			seekBar.setThumbTintList(thumb_color);

			seekBar = (SeekBar)main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_time_seekbar", "id", main_activity.getPackageName()));
			seekBar.setProgressTintList(progress_color);
			seekBar.setThumbTintList(thumb_color);

			seekBar = (SeekBar)main_activity.findViewById(main_activity.getResources().getIdentifier("white_balance_seekbar", "id", main_activity.getPackageName()));
			seekBar.setProgressTintList(progress_color);
			seekBar.setThumbTintList(thumb_color);
		}
	}

	/** Similar view.setRotation(ui_rotation), but achieves this via an animation.
	 */
	private void setViewRotation(View view, float ui_rotation) {
		//view.setRotation(ui_rotation);
		float rotate_by = ui_rotation - view.getRotation();
		if( rotate_by > 181.0f )
			rotate_by -= 360.0f;
		else if( rotate_by < -181.0f )
			rotate_by += 360.0f;
		// view.animate() modifies the view's rotation attribute, so it ends up equivalent to view.setRotation()
		// we use rotationBy() instead of rotation(), so we get the minimal rotation for clockwise vs anti-clockwise
		view.animate().rotationBy(rotate_by).setDuration(100).setInterpolator(new AccelerateDecelerateInterpolator()).start();
	}

    public void layoutUI() {
		layoutUI(false);
	}

    private void layoutUI(boolean popup_container_only) {
		long debug_time = 0;
		if( MyDebug.LOG ) {
			Log.d(TAG, "layoutUI");
			debug_time = System.currentTimeMillis();
		}
		//main_activity.getPreview().updateUIPlacement();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
		String ui_placement = sharedPreferences.getString(PreferenceKeys.getUIPlacementPreferenceKey(), "ui_right");
    	// we cache the preference_ui_placement to save having to check it in the draw() method
		this.ui_placement_right = ui_placement.equals("ui_right");
		if( MyDebug.LOG )
			Log.d(TAG, "ui_placement: " + ui_placement);
		// new code for orientation fixed to landscape	
		// the display orientation should be locked to landscape, but how many degrees is that?
	    int rotation = main_activity.getWindowManager().getDefaultDisplay().getRotation();
	    int degrees = 0;
	    switch (rotation) {
	    	case Surface.ROTATION_0: degrees = 0; break;
	        case Surface.ROTATION_90: degrees = 90; break;
	        case Surface.ROTATION_180: degrees = 180; break;
	        case Surface.ROTATION_270: degrees = 270; break;
    		default:
    			break;
	    }
	    // getRotation is anti-clockwise, but current_orientation is clockwise, so we add rather than subtract
	    // relative_orientation is clockwise from landscape-left
    	//int relative_orientation = (current_orientation + 360 - degrees) % 360;
    	int relative_orientation = (current_orientation + degrees) % 360;
		if( MyDebug.LOG ) {
			Log.d(TAG, "    current_orientation = " + current_orientation);
			Log.d(TAG, "    degrees = " + degrees);
			Log.d(TAG, "    relative_orientation = " + relative_orientation);
		}
		int ui_rotation = (360 - relative_orientation) % 360;
		main_activity.getPreview().setUIRotation(ui_rotation);
		int align_left = RelativeLayout.ALIGN_LEFT;
		int align_right = RelativeLayout.ALIGN_RIGHT;
		//int align_top = RelativeLayout.ALIGN_TOP;
		//int align_bottom = RelativeLayout.ALIGN_BOTTOM;
		int left_of = RelativeLayout.LEFT_OF;
		int right_of = RelativeLayout.RIGHT_OF;
		int above = RelativeLayout.ABOVE;
		int below = RelativeLayout.BELOW;
		int align_parent_left = RelativeLayout.ALIGN_PARENT_LEFT;
		int align_parent_right = RelativeLayout.ALIGN_PARENT_RIGHT;
		int align_parent_top = RelativeLayout.ALIGN_PARENT_TOP;
		int align_parent_bottom = RelativeLayout.ALIGN_PARENT_BOTTOM;
		if( !ui_placement_right ) {
			//align_top = RelativeLayout.ALIGN_BOTTOM;
			//align_bottom = RelativeLayout.ALIGN_TOP;
			above = RelativeLayout.BELOW;
			below = RelativeLayout.ABOVE;
			align_parent_top = RelativeLayout.ALIGN_PARENT_BOTTOM;
			align_parent_bottom = RelativeLayout.ALIGN_PARENT_TOP;
		}

		if( !popup_container_only )
		{
			// we use a dummy button, so that the GUI buttons keep their positioning even if the Settings button is hidden (visibility set to View.GONE)
			View view = main_activity.findViewById(main_activity.getResources().getIdentifier("gui_anchor", "id", main_activity.getPackageName()));
			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, 0);
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);
	
			view = main_activity.findViewById(main_activity.getResources().getIdentifier("gallery", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("gui_anchor", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);
	
			view = main_activity.findViewById(main_activity.getResources().getIdentifier("settings", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("gallery", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);
	
			view = main_activity.findViewById(main_activity.getResources().getIdentifier("popup", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("settings", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);
	
			view = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_lock", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("popup", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);
	
			view = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("exposure_lock", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);
	
			view = main_activity.findViewById(main_activity.getResources().getIdentifier("switch_video", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("exposure", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);
	
			view = main_activity.findViewById(main_activity.getResources().getIdentifier("switch_camera", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, 0);
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("switch_video", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("audio_control", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, 0);
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("switch_camera", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("trash", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("audio_control", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);
	
			view = main_activity.findViewById(main_activity.getResources().getIdentifier("share", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_top, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_bottom, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("trash", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("take_photo", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, RelativeLayout.TRUE);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("pause_video", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, RelativeLayout.TRUE);
			view.setLayoutParams(layoutParams);
			setViewRotation(view, ui_rotation);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("zoom", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_parent_left, 0);
			layoutParams.addRule(align_parent_right, RelativeLayout.TRUE);
			layoutParams.addRule(align_parent_top, 0);
			layoutParams.addRule(align_parent_bottom, RelativeLayout.TRUE);
			view.setLayoutParams(layoutParams);
			view.setRotation(180.0f); // should always match the zoom_seekbar, so that zoom in and out are in the same directions
	
			view = main_activity.findViewById(main_activity.getResources().getIdentifier("zoom_seekbar", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			// if we are showing the zoom control, the align next to that; otherwise have it aligned close to the edge of screen
			if( sharedPreferences.getBoolean(PreferenceKeys.getShowZoomControlsPreferenceKey(), false) ) {
				layoutParams.addRule(align_left, 0);
				layoutParams.addRule(align_right, main_activity.getResources().getIdentifier("zoom", "id", main_activity.getPackageName()));
				layoutParams.addRule(above, main_activity.getResources().getIdentifier("zoom", "id", main_activity.getPackageName()));
				layoutParams.addRule(below, 0);
				// need to clear the others, in case we turn zoom controls on/off
				layoutParams.addRule(align_parent_left, 0);
				layoutParams.addRule(align_parent_right, 0);
				layoutParams.addRule(align_parent_top, 0);
				layoutParams.addRule(align_parent_bottom, 0);
			}
			else {
				layoutParams.addRule(align_parent_left, 0);
				layoutParams.addRule(align_parent_right, RelativeLayout.TRUE);
				layoutParams.addRule(align_parent_top, 0);
				layoutParams.addRule(align_parent_bottom, RelativeLayout.TRUE);
				// need to clear the others, in case we turn zoom controls on/off
				layoutParams.addRule(align_left, 0);
				layoutParams.addRule(align_right, 0);
				layoutParams.addRule(above, 0);
				layoutParams.addRule(below, 0);
			}
			view.setLayoutParams(layoutParams);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("focus_seekbar", "id", main_activity.getPackageName()));
			layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			layoutParams.addRule(align_left, main_activity.getResources().getIdentifier("preview", "id", main_activity.getPackageName()));
			layoutParams.addRule(align_right, 0);
			layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("zoom_seekbar", "id", main_activity.getPackageName()));
			layoutParams.addRule(right_of, 0);
			layoutParams.addRule(align_parent_top, 0);
			layoutParams.addRule(align_parent_bottom, RelativeLayout.TRUE);
			view.setLayoutParams(layoutParams);
		}

		if( !popup_container_only )
		{
			// set seekbar info
			int width_dp;
			if( ui_rotation == 0 || ui_rotation == 180 ) {
				width_dp = 300;
			}
			else {
				width_dp = 200;
			}
			int height_dp = 50;
			final float scale = main_activity.getResources().getDisplayMetrics().density;
			int width_pixels = (int) (width_dp * scale + 0.5f); // convert dps to pixels
			int height_pixels = (int) (height_dp * scale + 0.5f); // convert dps to pixels
			int exposure_zoom_gap = (int) (4 * scale + 0.5f); // convert dps to pixels

			View view = main_activity.findViewById(main_activity.getResources().getIdentifier("sliders_container", "id", main_activity.getPackageName()));
			setViewRotation(view, ui_rotation);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_seekbar", "id", main_activity.getPackageName()));
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)view.getLayoutParams();
			lp.width = width_pixels;
			lp.height = height_pixels;
			view.setLayoutParams(lp);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_seekbar_zoom", "id", main_activity.getPackageName()));
			view.setAlpha(0.5f);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("iso_seekbar", "id", main_activity.getPackageName()));
			lp = (RelativeLayout.LayoutParams)view.getLayoutParams();
			lp.width = width_pixels;
			lp.height = height_pixels;
			view.setLayoutParams(lp);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_time_seekbar", "id", main_activity.getPackageName()));
			lp = (RelativeLayout.LayoutParams)view.getLayoutParams();
			lp.width = width_pixels;
			lp.height = height_pixels;
			view.setLayoutParams(lp);

			view = main_activity.findViewById(main_activity.getResources().getIdentifier("white_balance_seekbar", "id", main_activity.getPackageName()));
			lp = (RelativeLayout.LayoutParams)view.getLayoutParams();
			lp.width = width_pixels;
			lp.height = height_pixels;
			view.setLayoutParams(lp);
		}

		{
			View view = main_activity.findViewById(main_activity.getResources().getIdentifier("popup_container", "id", main_activity.getPackageName()));
			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
			//layoutParams.addRule(left_of, main_activity.getResources().getIdentifier("popup", "id", main_activity.getPackageName()));
			layoutParams.addRule(align_right, main_activity.getResources().getIdentifier("popup", "id", main_activity.getPackageName()));
			layoutParams.addRule(below, main_activity.getResources().getIdentifier("popup", "id", main_activity.getPackageName()));
			layoutParams.addRule(align_parent_bottom, RelativeLayout.TRUE);
			layoutParams.addRule(above, 0);
			layoutParams.addRule(align_parent_top, 0);
			view.setLayoutParams(layoutParams);

			setViewRotation(view, ui_rotation);
			// reset:
			view.setTranslationX(0.0f);
			view.setTranslationY(0.0f);
			if( MyDebug.LOG ) {
				Log.d(TAG, "popup view width: " + view.getWidth());
				Log.d(TAG, "popup view height: " + view.getHeight());
			}
			if( ui_rotation == 0 || ui_rotation == 180 ) {
				view.setPivotX(view.getWidth()/2.0f);
				view.setPivotY(view.getHeight()/2.0f);
			}
			else {
				view.setPivotX(view.getWidth());
				view.setPivotY(ui_placement_right ? 0.0f : view.getHeight());
				if( ui_placement_right ) {
					if( ui_rotation == 90 )
						view.setTranslationY( view.getWidth() );
					else if( ui_rotation == 270 )
						view.setTranslationX( - view.getHeight() );
				}
				else {
					if( ui_rotation == 90 )
						view.setTranslationX( - view.getHeight() );
					else if( ui_rotation == 270 )
						view.setTranslationY( - view.getWidth() );
				}
			}
		}

		if( !popup_container_only ) {
			setTakePhotoIcon();
			// no need to call setSwitchCameraContentDescription()
		}

		if( MyDebug.LOG ) {
			Log.d(TAG, "layoutUI: total time: " + (System.currentTimeMillis() - debug_time));
		}
    }

    /** Set icon for taking photos vs videos.
	 *  Also handles content descriptions for the take photo button and switch video button.
     */
    public void setTakePhotoIcon() {
		if( MyDebug.LOG )
			Log.d(TAG, "setTakePhotoIcon()");
		if( main_activity.getPreview() != null ) {
			ImageButton view = (ImageButton)main_activity.findViewById(main_activity.getResources().getIdentifier("take_photo", "id", main_activity.getPackageName()));
			int resource;
			int content_description;
			int switch_video_content_description;
			if( main_activity.getPreview().isVideo() ) {
				if( MyDebug.LOG )
					Log.d(TAG, "set icon to video");
				resource = main_activity.getPreview().isVideoRecording() ? main_activity.getResources().getIdentifier("take_video_recording", "drawable", main_activity.getPackageName()) : main_activity.getResources().getIdentifier("take_video_selector", "drawable", main_activity.getPackageName());
				content_description = main_activity.getPreview().isVideoRecording() ? main_activity.getResources().getIdentifier("stop_video", "string", main_activity.getPackageName()) : main_activity.getResources().getIdentifier("start_video", "string", main_activity.getPackageName());
				switch_video_content_description = main_activity.getResources().getIdentifier("switch_to_photo", "string", main_activity.getPackageName());
			}
			else {
				if( MyDebug.LOG )
					Log.d(TAG, "set icon to photo");
				resource = main_activity.getResources().getIdentifier("take_photo_selector", "drawable", main_activity.getPackageName());
				content_description = main_activity.getResources().getIdentifier("take_photo", "string", main_activity.getPackageName());
				switch_video_content_description = main_activity.getResources().getIdentifier("switch_to_video", "string", main_activity.getPackageName());
			}
			view.setImageResource(resource);
			view.setContentDescription( main_activity.getResources().getString(content_description) );
			view.setTag(resource); // for testing

			view = (ImageButton)main_activity.findViewById(main_activity.getResources().getIdentifier("switch_video", "id", main_activity.getPackageName()));
			view.setContentDescription( main_activity.getResources().getString(switch_video_content_description) );
		}
    }

    /** Set content description for switch camera button.
     */
    public void setSwitchCameraContentDescription() {
		if( MyDebug.LOG )
			Log.d(TAG, "setSwitchCameraContentDescription()");
		if( main_activity.getPreview() != null && main_activity.getPreview().canSwitchCamera() ) {
			ImageButton view = (ImageButton)main_activity.findViewById(main_activity.getResources().getIdentifier("switch_camera", "id", main_activity.getPackageName()));
			int content_description;
			int cameraId = main_activity.getNextCameraId();
		    if( main_activity.getPreview().getCameraControllerManager().isFrontFacing( cameraId ) ) {
				content_description = main_activity.getResources().getIdentifier("switch_to_front_camera", "string", main_activity.getPackageName());
		    }
		    else {
				content_description = main_activity.getResources().getIdentifier("switch_to_back_camera", "string", main_activity.getPackageName());
		    }
			if( MyDebug.LOG )
				Log.d(TAG, "content_description: " + main_activity.getResources().getString(content_description));
			view.setContentDescription( main_activity.getResources().getString(content_description) );
		}
    }

	/** Set content description for pause video button.
	 */
	public void setPauseVideoContentDescription() {
		if (MyDebug.LOG)
			Log.d(TAG, "setPauseVideoContentDescription()");
		View pauseVideoButton = main_activity.findViewById(main_activity.getResources().getIdentifier("pause_video", "id", main_activity.getPackageName()));
		ImageButton pauseButton =(ImageButton)main_activity.findViewById(main_activity.getResources().getIdentifier("pause_video", "id", main_activity.getPackageName()));
		int content_description;
		if( main_activity.getPreview().isVideoRecordingPaused() ) {
			content_description = main_activity.getResources().getIdentifier("resume_video", "string", main_activity.getPackageName());
			pauseButton.setImageResource(main_activity.getResources().getIdentifier("ic_play_circle_outline_white", "drawable", main_activity.getPackageName()));
		}
		else {
			content_description = main_activity.getResources().getIdentifier("pause_video", "string", main_activity.getPackageName());
			pauseButton.setImageResource(main_activity.getResources().getIdentifier("ic_pause_circle_outline_white_48dp", "drawable", main_activity.getPackageName()));
		}
		if( MyDebug.LOG )
			Log.d(TAG, "content_description: " + main_activity.getResources().getString(content_description));
		pauseVideoButton.setContentDescription(main_activity.getResources().getString(content_description));
	}

    public boolean getUIPlacementRight() {
    	return this.ui_placement_right;
    }

    public void onOrientationChanged(int orientation) {
		/*if( MyDebug.LOG ) {
			Log.d(TAG, "onOrientationChanged()");
			Log.d(TAG, "orientation: " + orientation);
			Log.d(TAG, "current_orientation: " + current_orientation);
		}*/
		if( orientation == OrientationEventListener.ORIENTATION_UNKNOWN )
			return;
		int diff = Math.abs(orientation - current_orientation);
		if( diff > 180 )
			diff = 360 - diff;
		// only change orientation when sufficiently changed
		if( diff > 60 ) {
		    orientation = (orientation + 45) / 90 * 90;
		    orientation = orientation % 360;
		    if( orientation != current_orientation ) {
			    this.current_orientation = orientation;
				if( MyDebug.LOG ) {
					Log.d(TAG, "current_orientation is now: " + current_orientation);
				}
			    layoutUI();
			}
		}
	}

    public void setImmersiveMode(final boolean immersive_mode) {
		if( MyDebug.LOG )
			Log.d(TAG, "setImmersiveMode: " + immersive_mode);
    	this.immersive_mode = immersive_mode;
		main_activity.runOnUiThread(new Runnable() {
			public void run() {
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
				// if going into immersive mode, the we should set GONE the ones that are set GONE in showGUI(false)
		    	//final int visibility_gone = immersive_mode ? View.GONE : View.VISIBLE;
		    	final int visibility = immersive_mode ? View.GONE : View.VISIBLE;
				if( MyDebug.LOG )
					Log.d(TAG, "setImmersiveMode: set visibility: " + visibility);
		    	// n.b., don't hide share and trash buttons, as they require immediate user input for us to continue
			    View switchCameraButton = main_activity.findViewById(main_activity.getResources().getIdentifier("switch_camera", "id", main_activity.getPackageName()));
			    View switchVideoButton = main_activity.findViewById(main_activity.getResources().getIdentifier("switch_video", "id", main_activity.getPackageName()));
			    View exposureButton = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure", "id", main_activity.getPackageName()));
			    View exposureLockButton = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_lock", "id", main_activity.getPackageName()));
			    View audioControlButton = main_activity.findViewById(main_activity.getResources().getIdentifier("audio_control", "id", main_activity.getPackageName()));
			    View popupButton = main_activity.findViewById(main_activity.getResources().getIdentifier("popup", "id", main_activity.getPackageName()));
			    View galleryButton = main_activity.findViewById(main_activity.getResources().getIdentifier("gallery", "id", main_activity.getPackageName()));
			    View settingsButton = main_activity.findViewById(main_activity.getResources().getIdentifier("settings", "id", main_activity.getPackageName()));
			    View zoomControls = main_activity.findViewById(main_activity.getResources().getIdentifier("zoom", "id", main_activity.getPackageName()));
			    View zoomSeekBar = main_activity.findViewById(main_activity.getResources().getIdentifier("zoom_seekbar", "id", main_activity.getPackageName()));
			    if( main_activity.getPreview().getCameraControllerManager().getNumberOfCameras() > 1 )
			    	switchCameraButton.setVisibility(visibility);
		    	switchVideoButton.setVisibility(visibility);
			    if( main_activity.supportsExposureButton() )
			    	exposureButton.setVisibility(visibility);
			    if( main_activity.getPreview().supportsExposureLock() )
			    	exposureLockButton.setVisibility(visibility);
			    if( main_activity.hasAudioControl() )
			    	audioControlButton.setVisibility(visibility);
		    	popupButton.setVisibility(visibility);
			    galleryButton.setVisibility(visibility);
			    settingsButton.setVisibility(visibility);
				if( MyDebug.LOG ) {
					Log.d(TAG, "has_zoom: " + main_activity.getPreview().supportsZoom());
				}
				if( main_activity.getPreview().supportsZoom() && sharedPreferences.getBoolean(PreferenceKeys.getShowZoomControlsPreferenceKey(), false) ) {
					zoomControls.setVisibility(visibility);
				}
				if( main_activity.getPreview().supportsZoom() && sharedPreferences.getBoolean(PreferenceKeys.getShowZoomSliderControlsPreferenceKey(), true) ) {
					zoomSeekBar.setVisibility(visibility);
				}
        		String pref_immersive_mode = sharedPreferences.getString(PreferenceKeys.getImmersiveModePreferenceKey(), "immersive_mode_low_profile");
        		if( pref_immersive_mode.equals("immersive_mode_everything") ) {
					if( sharedPreferences.getBoolean(PreferenceKeys.getShowTakePhotoPreferenceKey(), true) ) {
						View takePhotoButton = main_activity.findViewById(main_activity.getResources().getIdentifier("take_photo", "id", main_activity.getPackageName()));
						takePhotoButton.setVisibility(visibility);
					}
					if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && main_activity.getPreview().isVideoRecording() ) {
						View pauseVideoButton = main_activity.findViewById(main_activity.getResources().getIdentifier("pause_video", "id", main_activity.getPackageName()));
						pauseVideoButton.setVisibility(visibility);
					}
        		}
				if( !immersive_mode ) {
					// make sure the GUI is set up as expected
					showGUI(show_gui);
				}
			}
		});
    }
    
    public boolean inImmersiveMode() {
    	return immersive_mode;
    }

    public void showGUI(final boolean show) {
		if( MyDebug.LOG )
			Log.d(TAG, "showGUI: " + show);
		this.show_gui = show;
		if( inImmersiveMode() )
			return;
		if( show && main_activity.usingKitKatImmersiveMode() ) {
			// call to reset the timer
			main_activity.initImmersiveMode();
		}
		main_activity.runOnUiThread(new Runnable() {
			public void run() {
		    	final int visibility = show ? View.VISIBLE : View.GONE;
			    View switchCameraButton = main_activity.findViewById(main_activity.getResources().getIdentifier("switch_camera", "id", main_activity.getPackageName()));
			    View switchVideoButton = main_activity.findViewById(main_activity.getResources().getIdentifier("switch_video", "id", main_activity.getPackageName()));
			    View exposureButton = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure", "id", main_activity.getPackageName()));
			    View exposureLockButton = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_lock", "id", main_activity.getPackageName()));
			    View audioControlButton = main_activity.findViewById(main_activity.getResources().getIdentifier("audio_control", "id", main_activity.getPackageName()));
			    View popupButton = main_activity.findViewById(main_activity.getResources().getIdentifier("popup", "id", main_activity.getPackageName()));
			    if( main_activity.getPreview().getCameraControllerManager().getNumberOfCameras() > 1 )
			    	switchCameraButton.setVisibility(visibility);
			    if( !main_activity.getPreview().isVideo() )
			    	switchVideoButton.setVisibility(visibility); // still allow switch video when recording video
			    if( main_activity.supportsExposureButton() && !main_activity.getPreview().isVideo() ) // still allow exposure when recording video
			    	exposureButton.setVisibility(visibility);
			    if( main_activity.getPreview().supportsExposureLock() && !main_activity.getPreview().isVideo() ) // still allow exposure lock when recording video
			    	exposureLockButton.setVisibility(visibility);
			    if( main_activity.hasAudioControl() )
			    	audioControlButton.setVisibility(visibility);
			    if( !show ) {
			    	closePopup(); // we still allow the popup when recording video, but need to update the UI (so it only shows flash options), so easiest to just close
			    }
			    if( !main_activity.getPreview().isVideo() || !main_activity.getPreview().supportsFlash() )
			    	popupButton.setVisibility(visibility); // still allow popup in order to change flash mode when recording video
			}
		});
    }

    public void audioControlStarted() {
		ImageButton view = (ImageButton)main_activity.findViewById(main_activity.getResources().getIdentifier("audio_control", "id", main_activity.getPackageName()));
		view.setImageResource(main_activity.getResources().getIdentifier("ic_mic_red_48dp", "drawable", main_activity.getPackageName()));
		view.setContentDescription( main_activity.getResources().getString(main_activity.getResources().getIdentifier("audio_control_stop", "string", main_activity.getPackageName())) );
    }

    public void audioControlStopped() {
		ImageButton view = (ImageButton)main_activity.findViewById(main_activity.getResources().getIdentifier("audio_control", "id", main_activity.getPackageName()));
		view.setImageResource(main_activity.getResources().getIdentifier("ic_mic_white_48dp", "drawable", main_activity.getPackageName()));
		view.setContentDescription( main_activity.getResources().getString(main_activity.getResources().getIdentifier("audio_control_start", "string", main_activity.getPackageName())) );
    }

    public void toggleExposureUI() {
		if( MyDebug.LOG )
			Log.d(TAG, "toggleExposureUI");
		closePopup();
		View exposure_seek_bar = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_container", "id", main_activity.getPackageName()));
		int exposure_visibility = exposure_seek_bar.getVisibility();
		View manual_exposure_seek_bar = main_activity.findViewById(main_activity.getResources().getIdentifier("manual_exposure_container", "id", main_activity.getPackageName()));
		int manual_exposure_visibility = manual_exposure_seek_bar.getVisibility();
		boolean is_open = exposure_visibility == View.VISIBLE || manual_exposure_visibility == View.VISIBLE;
		if( is_open ) {
			clearSeekBar();
		}
		else if( main_activity.getPreview().getCameraController() != null ) {
			String iso_value = main_activity.getApplicationInterface().getISOPref();
			if( main_activity.getPreview().usingCamera2API() && !iso_value.equals("auto") ) {
				// with Camera2 API, when using manual ISO we instead show sliders for ISO range and exposure time
				if( main_activity.getPreview().supportsISORange()) {
					manual_exposure_seek_bar.setVisibility(View.VISIBLE);
					SeekBar exposure_time_seek_bar = ((SeekBar)main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_time_seekbar", "id", main_activity.getPackageName())));
					if( main_activity.getPreview().supportsExposureTime() ) {
						exposure_time_seek_bar.setVisibility(View.VISIBLE);
					}
					else {
						exposure_time_seek_bar.setVisibility(View.GONE);
					}
				}
			}
			else {
				if( main_activity.getPreview().supportsExposures() ) {
					exposure_seek_bar.setVisibility(View.VISIBLE);
					ZoomControls seek_bar_zoom = (ZoomControls)main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_seekbar_zoom", "id", main_activity.getPackageName()));
					seek_bar_zoom.setVisibility(View.VISIBLE);
				}
			}

			if( main_activity.getPreview().supportsWhiteBalanceTemperature()) {
				// we also show slider for manual white balance, if in that mode
				String white_balance_value = main_activity.getApplicationInterface().getWhiteBalancePref();
				View manual_white_balance_seek_bar = main_activity.findViewById(main_activity.getResources().getIdentifier("manual_white_balance_container", "id", main_activity.getPackageName()));
				if (main_activity.getPreview().usingCamera2API() && white_balance_value.equals("manual")) {
					manual_white_balance_seek_bar.setVisibility(View.VISIBLE);
				} else {
					manual_white_balance_seek_bar.setVisibility(View.GONE);
				}
			}
		}
    }

	public void setSeekbarZoom(int new_zoom) {
		if( MyDebug.LOG )
			Log.d(TAG, "setSeekbarZoom: " + new_zoom);
	    SeekBar zoomSeekBar = (SeekBar) main_activity.findViewById(main_activity.getResources().getIdentifier("zoom_seekbar", "id", main_activity.getPackageName()));
		if( MyDebug.LOG )
			Log.d(TAG, "progress was: " + zoomSeekBar.getProgress());
		zoomSeekBar.setProgress(main_activity.getPreview().getMaxZoom()-new_zoom);
		if( MyDebug.LOG )
			Log.d(TAG, "progress is now: " + zoomSeekBar.getProgress());
	}
	
	public void changeSeekbar(int seekBarId, int change) {
		if( MyDebug.LOG )
			Log.d(TAG, "changeSeekbar: " + change);
		SeekBar seekBar = (SeekBar)main_activity.findViewById(seekBarId);
	    int value = seekBar.getProgress();
	    int new_value = value + change;
	    if( new_value < 0 )
	    	new_value = 0;
	    else if( new_value > seekBar.getMax() )
	    	new_value = seekBar.getMax();
		if( MyDebug.LOG ) {
			Log.d(TAG, "value: " + value);
			Log.d(TAG, "new_value: " + new_value);
			Log.d(TAG, "max: " + seekBar.getMax());
		}
	    if( new_value != value ) {
		    seekBar.setProgress(new_value);
	    }
	}

    public void clearSeekBar() {
		View view = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_container", "id", main_activity.getPackageName()));
		view.setVisibility(View.GONE);
		view = main_activity.findViewById(main_activity.getResources().getIdentifier("exposure_seekbar_zoom", "id", main_activity.getPackageName()));
		view.setVisibility(View.GONE);
		view = main_activity.findViewById(main_activity.getResources().getIdentifier("manual_exposure_container", "id", main_activity.getPackageName()));
		view.setVisibility(View.GONE);
		view = main_activity.findViewById(main_activity.getResources().getIdentifier("manual_white_balance_container", "id", main_activity.getPackageName()));
		view.setVisibility(View.GONE);
    }
    
    public void setPopupIcon() {
		if( MyDebug.LOG )
			Log.d(TAG, "setPopupIcon");
		ImageButton popup = (ImageButton)main_activity.findViewById(main_activity.getResources().getIdentifier("popup", "id", main_activity.getPackageName()));
		String flash_value = main_activity.getPreview().getCurrentFlashValue();
		if( MyDebug.LOG )
			Log.d(TAG, "flash_value: " + flash_value);
    	if( flash_value != null && flash_value.equals("flash_off") ) {
    		popup.setImageResource(main_activity.getResources().getIdentifier("popup_flash_off", "drawable", main_activity.getPackageName()));
    	}
    	else if( flash_value != null && flash_value.equals("flash_torch") ) {
    		popup.setImageResource(main_activity.getResources().getIdentifier("popup_flash_torch", "drawable", main_activity.getPackageName()));
    	}
		else if( flash_value != null && ( flash_value.equals("flash_auto") || flash_value.equals("flash_frontscreen_auto") ) ) {
    		popup.setImageResource(main_activity.getResources().getIdentifier("popup_flash_auto", "drawable", main_activity.getPackageName()));
    	}
		else if( flash_value != null && ( flash_value.equals("flash_on") || flash_value.equals("flash_frontscreen_on") ) ) {
    		popup.setImageResource(main_activity.getResources().getIdentifier("popup_flash_on", "drawable", main_activity.getPackageName()));
    	}
    	else if( flash_value != null && flash_value.equals("flash_red_eye") ) {
    		popup.setImageResource(main_activity.getResources().getIdentifier("popup_flash_red_eye", "drawable", main_activity.getPackageName()));
    	}
    	else {
    		popup.setImageResource(main_activity.getResources().getIdentifier("popup", "drawable", main_activity.getPackageName()));
    	}
    }

    public void closePopup() {
		if( MyDebug.LOG )
			Log.d(TAG, "close popup");
		if( popupIsOpen() ) {
			popup_view_is_open = false;
			/* Not destroying the popup doesn't really gain any performance.
			 * Also there are still outstanding bugs to fix if we wanted to do this:
			 *   - Not resetting the popup menu when switching between photo and video mode. See test testVideoPopup().
			 *   - When changing options like flash/focus, the new option isn't selected when reopening the popup menu. See test
			 *     testPopup().
			 *   - Changing settings potentially means we have to recreate the popup, so the natural place to do this is in
			 *     CaptureActivity.updateForSettings(), but doing so makes the popup close when checking photo or video resolutions!
			 *     See test testSwitchResolution().
			 */
			if( cache_popup ) {
				popup_view.setVisibility(View.GONE);
			}
			else {
				destroyPopup();
			}
			main_activity.initImmersiveMode(); // to reset the timer when closing the popup
		}
    }

    public boolean popupIsOpen() {
    	return popup_view_is_open;
    }
    
    public void destroyPopup() {
		if( popupIsOpen() ) {
			closePopup();
		}
		ViewGroup popup_container = (ViewGroup)main_activity.findViewById(main_activity.getResources().getIdentifier("popup_container", "id", main_activity.getPackageName()));
		popup_container.removeAllViews();
		popup_view = null;
    }

    public void togglePopupSettings() {
		final ViewGroup popup_container = (ViewGroup)main_activity.findViewById(main_activity.getResources().getIdentifier("popup_container", "id", main_activity.getPackageName()));
		if( popupIsOpen() ) {
			closePopup();
			return;
		}
		if( main_activity.getPreview().getCameraController() == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "camera not opened!");
			return;
		}

		if( MyDebug.LOG )
			Log.d(TAG, "open popup");

		clearSeekBar();
		main_activity.getPreview().cancelTimer(); // best to cancel any timer, in case we take a photo while settings window is open, or when changing settings
		main_activity.stopAudioListeners();

    	final long time_s = System.currentTimeMillis();

    	{
			// prevent popup being transparent
			popup_container.setBackgroundColor(Color.BLACK);
			popup_container.setAlpha(0.9f);
		}

    	if( popup_view == null ) {
			if( MyDebug.LOG )
				Log.d(TAG, "create new popup_view");
    		popup_view = new PopupView(main_activity);
			popup_container.addView(popup_view);
    	}
    	else {
			if( MyDebug.LOG )
				Log.d(TAG, "use cached popup_view");
			popup_view.setVisibility(View.VISIBLE);
    	}
		popup_view_is_open = true;
		
        // need to call layoutUI to make sure the new popup is oriented correctly
		// but need to do after the layout has been done, so we have a valid width/height to use
		// n.b., even though we only need the portion of layoutUI for the popup container, there
		// doesn't seem to be any performance benefit in only calling that part
		popup_container.getViewTreeObserver().addOnGlobalLayoutListener(
			new OnGlobalLayoutListener() {
				@SuppressWarnings("deprecation")
				@Override
			    public void onGlobalLayout() {
					if( MyDebug.LOG )
						Log.d(TAG, "onGlobalLayout()");
					if( MyDebug.LOG )
						Log.d(TAG, "time after global layout: " + (System.currentTimeMillis() - time_s));
					layoutUI(true);
					if( MyDebug.LOG )
						Log.d(TAG, "time after layoutUI: " + (System.currentTimeMillis() - time_s));
		    		// stop listening - only want to call this once!
		            if( Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 ) {
		            	popup_container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		            }
		            else {
		            	popup_container.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		            }

		    		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
		    		String ui_placement = sharedPreferences.getString(PreferenceKeys.getUIPlacementPreferenceKey(), "ui_right");
		    		boolean ui_placement_right = ui_placement.equals("ui_right");
		            ScaleAnimation animation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, ui_placement_right ? 0.0f : 1.0f);
		    		animation.setDuration(100);
		    		popup_container.setAnimation(animation);
		        }
			}
		);

		if( MyDebug.LOG )
			Log.d(TAG, "time to create popup: " + (System.currentTimeMillis() - time_s));
    }

	@SuppressWarnings("deprecation")
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if( MyDebug.LOG )
			Log.d(TAG, "onKeyDown: " + keyCode);
		switch( keyCode ) {
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS: // media codes are for "selfie sticks" buttons
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			case KeyEvent.KEYCODE_MEDIA_STOP:
			{
				if( keyCode == KeyEvent.KEYCODE_VOLUME_UP )
					keydown_volume_up = true;
				else if( keyCode == KeyEvent.KEYCODE_VOLUME_DOWN )
					keydown_volume_down = true;

				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
				String volume_keys = sharedPreferences.getString(PreferenceKeys.getVolumeKeysPreferenceKey(), "volume_take_photo");

				if((keyCode==KeyEvent.KEYCODE_MEDIA_PREVIOUS
						||keyCode==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
						||keyCode==KeyEvent.KEYCODE_MEDIA_STOP)
						&&!(volume_keys.equals("volume_take_photo"))) {
					AudioManager audioManager = (AudioManager) main_activity.getSystemService(Context.AUDIO_SERVICE);
					if(audioManager==null) break;
					if(!audioManager.isWiredHeadsetOn()) break; // isWiredHeadsetOn() is deprecated, but comment says "Use only to check is a headset is connected or not."
				}

				if( volume_keys=="volume_take_photo") {
					main_activity.takePicture();
					return true;
				} else if( volume_keys=="volume_focus"){
						if(keydown_volume_up && keydown_volume_down) {
							if (MyDebug.LOG)
								Log.d(TAG, "take photo rather than focus, as both volume keys are down");
							main_activity.takePicture();
						}
						else if (main_activity.getPreview().getCurrentFocusValue() != null && main_activity.getPreview().getCurrentFocusValue().equals("focus_mode_manual2")) {
							if(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
								main_activity.changeFocusDistance(-1);
							else
								main_activity.changeFocusDistance(1);
						}
						else {
							// important not to repeatedly request focus, even though main_activity.getPreview().requestAutoFocus() will cancel, as causes problem if key is held down (e.g., flash gets stuck on)
							// also check DownTime vs EventTime to prevent repeated focusing whilst the key is held down
							if(event.getDownTime() == event.getEventTime() && !main_activity.getPreview().isFocusWaiting()) {
								if(MyDebug.LOG)
									Log.d(TAG, "request focus due to volume key");
								main_activity.getPreview().requestAutoFocus();
							}
						}
						return true;
				} else if( volume_keys=="volume_zoom"){
						if(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
							main_activity.zoomIn();
						else
							main_activity.zoomOut();
						return true;
				} else if( volume_keys=="volume_exposure"){
						if(main_activity.getPreview().getCameraController() != null) {
							String value = sharedPreferences.getString(PreferenceKeys.getISOPreferenceKey(), main_activity.getPreview().getCameraController().getDefaultISO());
							boolean manual_iso = !value.equals("auto");
							if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
								if(manual_iso) {
									if(main_activity.getPreview().supportsISORange())
										main_activity.changeISO(1);
								}
								else
									main_activity.changeExposure(1);
							}
							else {
								if(manual_iso) {
									if(main_activity.getPreview().supportsISORange())
										main_activity.changeISO(-1);
								}
								else
									main_activity.changeExposure(-1);
							}
						}
						return true;
				} else if( volume_keys=="volume_auto_stabilise"){
						if(main_activity.supportsAutoStabilise()) {
							boolean auto_stabilise = sharedPreferences.getBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), false);
							auto_stabilise = !auto_stabilise;
							SharedPreferences.Editor editor = sharedPreferences.edit();
							editor.putBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), auto_stabilise);
							editor.apply();
							String message = main_activity.getResources().getString(main_activity.getResources().getIdentifier("preference_auto_stabilise", "string", main_activity.getPackageName())) + ": " + main_activity.getResources().getString(auto_stabilise ? main_activity.getResources().getIdentifier("on", "string", main_activity.getPackageName()) : main_activity.getResources().getIdentifier("off", "string", main_activity.getPackageName()));
							main_activity.getPreview().showToast(main_activity.getChangedAutoStabiliseToastBoxer(), message);
						}
						else {
							main_activity.getPreview().showToast(main_activity.getChangedAutoStabiliseToastBoxer(), main_activity.getResources().getIdentifier("auto_stabilise_not_supported", "string", main_activity.getPackageName()));
						}
						return true;
				} else if( volume_keys=="volume_really_nothing"){
						// do nothing, but still return true so we don't change volume either
						return true;
				}
				// else do nothing here, but still allow changing of volume (i.e., the default behaviour)
				break;
			}
			case KeyEvent.KEYCODE_MENU:
			{
				// needed to support hardware menu button
				// tested successfully on Samsung S3 (via RTL)
				// see http://stackoverflow.com/questions/8264611/how-to-detect-when-user-presses-menu-key-on-their-android-device
				main_activity.openSettings();
				return true;
			}
			case KeyEvent.KEYCODE_CAMERA:
			{
				if( event.getRepeatCount() == 0 ) {
					main_activity.takePicture();
					return true;
				}
			}
			case KeyEvent.KEYCODE_FOCUS:
			{
				// important not to repeatedly request focus, even though main_activity.getPreview().requestAutoFocus() will cancel - causes problem with hardware camera key where a half-press means to focus
				// also check DownTime vs EventTime to prevent repeated focusing whilst the key is held down - see https://sourceforge.net/p/opencamera/tickets/174/ ,
				// or same issue above for volume key focus
				if( event.getDownTime() == event.getEventTime() && !main_activity.getPreview().isFocusWaiting() ) {
					if( MyDebug.LOG )
						Log.d(TAG, "request focus due to focus key");
					main_activity.getPreview().requestAutoFocus();
				}
				return true;
			}
			case KeyEvent.KEYCODE_ZOOM_IN:
			{
				main_activity.zoomIn();
				return true;
			}
			case KeyEvent.KEYCODE_ZOOM_OUT:
			{
				main_activity.zoomOut();
				return true;
			}
		}
		return false;
	}

	public void onKeyUp(int keyCode, KeyEvent event) {
		if( MyDebug.LOG )
			Log.d(TAG, "onKeyUp: " + keyCode);
		if( keyCode == KeyEvent.KEYCODE_VOLUME_UP )
			keydown_volume_up = false;
		else if( keyCode == KeyEvent.KEYCODE_VOLUME_DOWN )
			keydown_volume_down = false;
	}

	/** Returns a (possibly translated) user readable string for a white balance preference value.
	 *  If the value is not recognised (this can happen for the old Camera API, some devices can
	 *  have device-specific options), then the received value is returned.
	 */
	public String getEntryForWhiteBalance(String value) {
		int id = -1;
		if( value=="auto")
			id = main_activity.getResources().getIdentifier("white_balance_auto", "string", main_activity.getPackageName());
		else if (value=="cloudy-daylight")
			id = main_activity.getResources().getIdentifier("white_balance_cloudy", "string", main_activity.getPackageName());
		else if (value=="daylight")
			id = main_activity.getResources().getIdentifier("white_balance_daylight", "string", main_activity.getPackageName());
		else if (value=="fluorescent")
			id = main_activity.getResources().getIdentifier("white_balance_fluorescent", "string", main_activity.getPackageName());
		else if (value=="incandescent")
			id = main_activity.getResources().getIdentifier("white_balance_incandescent", "string", main_activity.getPackageName());
		else if (value=="shade")
			id = main_activity.getResources().getIdentifier("white_balance_shade", "string", main_activity.getPackageName());
		else if (value=="twilight")
			id = main_activity.getResources().getIdentifier("white_balance_twilight", "string", main_activity.getPackageName());
		else if (value=="warm-fluorescent")
			id = main_activity.getResources().getIdentifier("white_balance_warm", "string", main_activity.getPackageName());
		else if (value=="manual")
			id = main_activity.getResources().getIdentifier("white_balance_manual", "string", main_activity.getPackageName());

		String entry;
		if( id != -1 ) {
			entry = main_activity.getResources().getString(id);
		}
		else {
			entry = value;
		}
		return entry;
	}

	/** Returns a (possibly translated) user readable string for a scene mode preference value.
	 *  If the value is not recognised (this can happen for the old Camera API, some devices can
	 *  have device-specific options), then the received value is returned.
	 */
	public String getEntryForSceneMode(String value) {
		int id = -1;
		if( value == "action")
			id = main_activity.getResources().getIdentifier("scene_mode_action", "string", main_activity.getPackageName());
		else if( value == "barcode")
			id = main_activity.getResources().getIdentifier("scene_mode_barcode", "string", main_activity.getPackageName());
		else if( value == "beach")
			id = main_activity.getResources().getIdentifier("scene_mode_beach", "string", main_activity.getPackageName());
		else if( value == "candlelight")
			id = main_activity.getResources().getIdentifier("scene_mode_candlelight", "string", main_activity.getPackageName());
		else if( value == "auto")
			id = main_activity.getResources().getIdentifier("scene_mode_auto", "string", main_activity.getPackageName());
		else if( value == "fireworks")
			id = main_activity.getResources().getIdentifier("scene_mode_fireworks", "string", main_activity.getPackageName());
		else if( value == "landscape")
			id = main_activity.getResources().getIdentifier("scene_mode_landscape", "string", main_activity.getPackageName());
		else if( value == "night")
			id = main_activity.getResources().getIdentifier("scene_mode_night", "string", main_activity.getPackageName());
		else if( value == "night-portrait")
			id = main_activity.getResources().getIdentifier("scene_mode_night_portrait", "string", main_activity.getPackageName());
		else if( value == "party")
			id = main_activity.getResources().getIdentifier("scene_mode_party", "string", main_activity.getPackageName());
		else if( value == "portrait")
			id = main_activity.getResources().getIdentifier("scene_mode_portrait", "string", main_activity.getPackageName());
		else if( value == "snow")
			id = main_activity.getResources().getIdentifier("scene_mode_snow", "string", main_activity.getPackageName());
		else if( value == "sports")
			id = main_activity.getResources().getIdentifier("scene_mode_sports", "string", main_activity.getPackageName());
		else if( value == "steadyphoto")
			id = main_activity.getResources().getIdentifier("scene_mode_steady_photo", "string", main_activity.getPackageName());
		else if( value == "sunset")
			id = main_activity.getResources().getIdentifier("scene_mode_sunset", "string", main_activity.getPackageName());
		else if( value == "theatre")
			id = main_activity.getResources().getIdentifier("scene_mode_theatre", "string", main_activity.getPackageName());


		String entry;
		if( id != -1 ) {
			entry = main_activity.getResources().getString(id);
		}
		else {
			entry = value;
		}
		return entry;
	}

	/** Returns a (possibly translated) user readable string for a color effect preference value.
	 *  If the value is not recognised (this can happen for the old Camera API, some devices can
	 *  have device-specific options), then the received value is returned.
	 */
	public String getEntryForColorEffect(String value) {
		int id = -1;
		if( value  == "aqua")
			id = main_activity.getResources().getIdentifier("color_effect_aqua", "string", main_activity.getPackageName());
		if( value  == "blackboard")
			id = main_activity.getResources().getIdentifier("color_effect_blackboard", "string", main_activity.getPackageName());
		if( value  == "mono")
			id = main_activity.getResources().getIdentifier("color_effect_mono", "string", main_activity.getPackageName());
		if( value  == "negative")
			id = main_activity.getResources().getIdentifier("color_effect_negative", "string", main_activity.getPackageName());
		if( value  == "none")
			id = main_activity.getResources().getIdentifier("color_effect_none", "string", main_activity.getPackageName());
		if( value  == "posterize")
			id = main_activity.getResources().getIdentifier("color_effect_posterize", "string", main_activity.getPackageName());
		if( value  == "sepia")
			id = main_activity.getResources().getIdentifier("color_effect_sepia", "string", main_activity.getPackageName());
		if( value  == "solarize")
			id = main_activity.getResources().getIdentifier("color_effect_solarize", "string", main_activity.getPackageName());
		if( value  == "whiteboard")
			id = main_activity.getResources().getIdentifier("color_effect_whiteboard", "string", main_activity.getPackageName());


		String entry;
		if( id != -1 ) {
			entry = main_activity.getResources().getString(id);
		}
		else {
			entry = value;
		}
		return entry;
	}

    // for testing
    public View getPopupButton(String key) {
    	return popup_view.getPopupButton(key);
    }

    public PopupView getPopupView() {
		return popup_view;
	}
}
