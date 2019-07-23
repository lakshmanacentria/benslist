package com.acentria.benslist;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

public class YoutubeActivity extends AppCompatActivity {

	private static ActionBar actionBar;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Lang.setDirection(this);

		Intent intent = getIntent();
		final String videoID = intent.getStringExtra("id");
		final String videoType = intent.getStringExtra("type");
		
		setTitle(Lang.get("android_title_activity_video"));
		setContentView(videoType.equals("youtube") ? R.layout.activity_youtube : R.layout.activity_video);
		
		/* enable back action */
		actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		if ( videoType.equals("youtube") ) {
			FragmentManager fragmentManager = getSupportFragmentManager();
		    FragmentTransaction fragmentTransaction = fragmentManager
		            .beginTransaction();

		    YouTubePlayerSupportFragment fragment = new YouTubePlayerSupportFragment();
		    fragmentTransaction.add(R.id.fragment, fragment);
		    fragmentTransaction.commit();

		    fragment.initialize(getResources().getString(R.string.google_key), new OnInitializedListener() {
				@Override
				public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
				    if ( !wasRestored ) {
				    	player.loadVideo(videoID);
				    }
				}

				@Override
				public void onInitializationFailure(Provider arg0, YouTubeInitializationResult arg1) {}
		    });
		}
		else {
			VideoView myVideoView = (VideoView) findViewById(R.id.video);
			myVideoView.setVideoURI(Uri.parse(intent.getStringExtra("video")));
			myVideoView.setMediaController(new MediaController(this));
			myVideoView.requestFocus();
			myVideoView.start();
		}
	}
	
	@Override	
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		if ( newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ) {
			actionBar.show();
		}
		else {
			actionBar.hide();
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {     
	        case android.R.id.home:
	        	super.onBackPressed();
				return true;
				
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}
}
