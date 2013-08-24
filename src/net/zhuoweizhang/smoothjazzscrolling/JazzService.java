package net.zhuoweizhang.smoothjazzscrolling;

import java.util.*;

import android.app.*;
import android.content.*;
import android.accessibilityservice.*;
import android.media.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.*;

public class JazzService extends AccessibilityService {

	public static final String TAG = "JazzService";

	private final Handler stopTheMusicHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//STOP! hammertime.
			if (msg.what == MESSAGE_TYPE_STOP_MUSIC) {
				stopTheMusic();
			} else if (msg.what == MESSAGE_TYPE_FADE_TO) {
				doFadeIncrement(msg);
			}
			msg.recycle();
		}
	};

	private static final int MESSAGE_TYPE_STOP_MUSIC = 0x53544F50;
	private static final int MESSAGE_TYPE_FADE_TO = 0xfade;
	private static final int VOLUME_FULL = 1;
	private static final int VOLUME_SILENT = 0;
	private float curVolume = 0.0f;
	private int lastItemCount = -99999;

	private MediaPlayer mediaPlayer;
	private Message fadeMessage = new Message();

	@Override
	public void onAccessibilityEvent(AccessibilityEvent e) {
		Log.i(TAG, e.toString());
		int eventType = e.getEventType();
		if (eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return;
		int newItemCount = e.getItemCount();
		int oldItemCount = lastItemCount;
		lastItemCount = newItemCount;
		if (oldItemCount != newItemCount) {
			//We've just entered a new view - sometimes the framework auto-scrolls here, so drop the event
			return;
		}
		if (mediaPlayer == null) createMediaPlayer();
		mediaPlayer.start();
		stopTheMusicHandler.removeMessages(MESSAGE_TYPE_STOP_MUSIC);
		stopTheMusicHandler.removeMessages(MESSAGE_TYPE_FADE_TO);
		//stopTheMusicHandler.sendEmptyMessageDelayed(MESSAGE_TYPE_STOP_MUSIC, 500);
		Message blank = new Message();
		blank.what = MESSAGE_TYPE_STOP_MUSIC;
		stopTheMusicHandler.sendMessageDelayed(blank, 500);
		Message msg = new Message();
		msg.what = MESSAGE_TYPE_FADE_TO;
		msg.arg1 = VOLUME_FULL;
		msg.arg2 = 20;
		stopTheMusicHandler.sendMessage(msg);
		
	}

	@Override
	public void onInterrupt() {
		Log.i(TAG, "BAWK!");
		stopTheMusic();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		createMediaPlayer();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mediaPlayer != null) mediaPlayer.release();
		mediaPlayer = null;
	}

	@Override
	public void onLowMemory() {
		if (mediaPlayer != null) mediaPlayer.release();
		mediaPlayer = null;
		super.onLowMemory();
	}


	private void createMediaPlayer() {
		mediaPlayer = MediaPlayer.create(this, R.raw.carelesswhisper_loop);
		mediaPlayer.setLooping(true);
		mediaPlayer.setVolume(0, 0);
		curVolume = 0;
	}

	public void stopTheMusic() {
		//TODO gradually fade out the music playing
		//mediaPlayer.pause();
		stopTheMusicHandler.removeMessages(MESSAGE_TYPE_FADE_TO);
		stopTheMusicHandler.removeMessages(MESSAGE_TYPE_STOP_MUSIC);
		Message msg = new Message();
		msg.what = MESSAGE_TYPE_FADE_TO;
		msg.arg1 = VOLUME_SILENT;
		msg.arg2 = 20;
		stopTheMusicHandler.sendMessage(msg);
	}

	public void doFadeIncrement(Message msg) {
		if (mediaPlayer == null) return;
		float newVolume = curVolume + (msg.arg1 == VOLUME_FULL? 0.05f : -0.05f);
		boolean needSendMsg = false;
		if (newVolume > 1) {
			newVolume = 1;
		} else if (newVolume < 0) {
			newVolume = 0;
			mediaPlayer.pause();
		} else {
			needSendMsg = true;
		}
		mediaPlayer.setVolume(newVolume, newVolume);
		curVolume = newVolume;
		if (needSendMsg) {
			Message newMsg = new Message();
			newMsg.copyFrom(msg);
			stopTheMusicHandler.sendMessageDelayed(newMsg, msg.arg2);
		}
	}

}
