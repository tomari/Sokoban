package su.drsouko;

import java.text.NumberFormat;

import android.media.SoundPool;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SokoActivity extends Activity implements SokoView.SokoTouchListener,ValueAnimator.AnimatorUpdateListener {
	private SokoGameState state;
	private SokoView gameView;
	private NumberFormat numFormat=NumberFormat.getInstance();
	private ValueAnimator anim;
	private final String SAVELABEL_STATE="state";
	private HighscoreMgr highscores;
	public static final String STARTINTENT_FILENAME="stagesFile";
	private int backStack;
	private Toast toast;
	private static final String PREF_GAMESCALE="gamescale";
	private static final float default_scale=1.f;
	private SoundPool sp;
	private boolean sound=false;
	private int snd_walk1, snd_walk2, snd_walk3, snd_clearhs, 
				snd_clear, snd_retry, snd_undo;
	private class ViewPortAdjuster implements ViewTreeObserver.OnPreDrawListener {
		private boolean firsttime=true;
		@Override
		public boolean onPreDraw() {
			if(firsttime) {
				calculateViewPort();
				firsttime=false;
			}
			return true;
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final SharedPreferences shrP=PreferenceManager.getDefaultSharedPreferences(this);
		Thread snd_init=new Thread(new Runnable() {
			@Override
			public void run() {
				boolean snd_i=shrP.getBoolean(SettingsActivity.PREF_SOUND, true);
				if(snd_i) {
					initSound();
					sound = snd_i;
				}
			}
		});
		snd_init.run();
		setContentView(R.layout.activity_soko);
		final String path=this.getIntent().getExtras().getString(STARTINTENT_FILENAME);
		highscores=new HighscoreMgr(SokoActivity.this,path);
		highscores.load();
		getActionBar().setDisplayHomeAsUpEnabled(true);
		gameView=(SokoView)findViewById(R.id.gameView);
		if(savedInstanceState==null) {
			state=new SokoGameState(path);
			state.scale=shrP.getFloat(PREF_GAMESCALE, default_scale);
			if(!gotoStage(highscores.minUnclearedStage(),false)) {
				gotoStage(1,false);
			}
		} else {
			state=(SokoGameState)savedInstanceState.getSerializable(SAVELABEL_STATE);
			updateStatusDisplay();
		}
		gameView.setGameState(state);
		ViewTreeObserver vto=gameView.getViewTreeObserver();
		ViewPortAdjuster vpa=new ViewPortAdjuster();
		vto.addOnPreDrawListener(vpa);
		
		gameView.setSokoTouchListener(this);
		//
		anim=ValueAnimator.ofFloat(0.f,1.f);
		anim.setDuration(50);
		anim.addUpdateListener(this);
	}
	@Override
	public void onResume() {
		super.onResume();
		setCtrlPanelPlacement();
		backStack=0;
		toast=null;
	}
	private void setCtrlPanelPlacement() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		int ctrlPlacement;
		try {
			ctrlPlacement=Integer.valueOf(sharedPref.getString(SettingsActivity.PREF_CTRLPLACE,""));
		} catch (NumberFormatException e) {
			ctrlPlacement=0;
		}
		LinearLayout ctrlPanel=(LinearLayout)findViewById(R.id.controlPanel);
		RelativeLayout.LayoutParams lParams=(RelativeLayout.LayoutParams)ctrlPanel.getLayoutParams();
		ctrlPanel.setVisibility((ctrlPlacement==3)?View.INVISIBLE:View.VISIBLE);
		if(ctrlPlacement==1) { lParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT); }
		else if(ctrlPlacement==2) {lParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT); }
		else if(ctrlPlacement==0) {lParams.addRule(RelativeLayout.CENTER_HORIZONTAL);}
		ctrlPanel.setLayoutParams(lParams);
	}
	@Override
	public void onPause() {
		super.onPause();
		highscores.save();
		SharedPreferences shrP=PreferenceManager.getDefaultSharedPreferences(this);
		float scale=shrP.getFloat(PREF_GAMESCALE, default_scale);
		if(scale!=state.scale) {
			SharedPreferences.Editor e=shrP.edit();
			e.putFloat(PREF_GAMESCALE, state.scale);
			e.commit();
		}

	}
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putSerializable(SAVELABEL_STATE, state);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.soko, menu);
		return true;
	}
	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN || keyCode==KeyEvent.KEYCODE_J ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_8) {
			goDown(null);
		} else if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT || keyCode==KeyEvent.KEYCODE_H ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_4) {
			goLeft(null);
		} else if(keyCode==KeyEvent.KEYCODE_DPAD_UP || keyCode==KeyEvent.KEYCODE_K ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_2) {
			goUp(null);
		} else if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT || keyCode==KeyEvent.KEYCODE_L ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_6) {
			goRight(null);
		} else if(keyCode==KeyEvent.KEYCODE_R || keyCode==KeyEvent.KEYCODE_BUTTON_B) {
			retryThisStage();
		} else if(keyCode==KeyEvent.KEYCODE_Z || keyCode==KeyEvent.KEYCODE_BUTTON_A) {
			undoLastMove();
		} else if(keyCode==KeyEvent.KEYCODE_PAGE_DOWN || keyCode==KeyEvent.KEYCODE_BUTTON_R1) {
			gotoStage(state.stage+1);
		} else if(keyCode==KeyEvent.KEYCODE_PAGE_UP || keyCode==KeyEvent.KEYCODE_BUTTON_L1) {
			gotoStage(state.stage-1);
		} else if(keyCode==KeyEvent.KEYCODE_BUTTON_Y) {
			pickStage();
		} else {
			return super.onKeyDown(keyCode, event);
		}
		return true;
	}
	private void retryThisStage() {
		playSound(snd_retry);
		gotoStage(state.stage);
		gameView.invalidate();
	}
	@Override
	public boolean onMenuItemSelected(int featureId,MenuItem item) {
		int itemid=item.getItemId();
		if(itemid==android.R.id.home) {
			super.onBackPressed();
		} else if(itemid==R.id.action_retry) {
			retryThisStage();
		} else if(itemid==R.id.action_nextstage) {
			gotoStage(state.stage+1);
		} else if(itemid==R.id.action_prevstage) {
			gotoStage(state.stage-1);
		} else if(itemid==R.id.action_goto) {
			pickStage();
		} else if(itemid==R.id.action_undo) {
			undoLastMove();
		} else {
			return super.onMenuItemSelected(featureId, item);
		}
		return true;
	}
	public void goLeft(View view) {
		moveXY(-1,0);
	}
	public void goDown(View view) {
		moveXY(0, 1);
	}
	public void goUp(View view) {
		moveXY(0, -1);
	}
	public void goRight(View view) {
		moveXY(1, 0);
	}
	private boolean moveXY(int x,int y) {
		boolean success=state.moveXY(x, y);
		if(success) {
			if(state.isFinished()) {
				handleStageClear();
			} else {
				playWalkSound();
			}
			TextView stepsView=(TextView)findViewById(R.id.scoreLabel);
			stepsView.setText(numFormat.format(state.steps));
			calculateViewPort();
			state.animProgress=0.f;
			anim.start();
		}
		return success;
	}
	private void handleStageClear() {
		boolean newHS=highscores.putScore(state.stage, state.steps);
		playSound(newHS?snd_clearhs:snd_clear);
		String text=String.format(
				(String)getResources().getText(newHS?R.string.text_newhs:R.string.text_clear), 
				state.stageTitle);
		AlertDialog.Builder builder=new AlertDialog.Builder(this);
		builder.setTitle(R.string.title_clear)
		.setMessage(text)
		.setPositiveButton(R.string.button_clear, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				gotoStage(state.stage+1);
			}
		})
		.show();
	}
	private void calculateViewPort() {
		int chrDimX=gameView.getChrDimX();
		int chrDimY=gameView.getChrDimY();
		int chrDimU=(int) (chrDimX*state.scale);
		int chrDimV=(int) (chrDimY*state.scale);
		int vpDimU=gameView.getMeasuredWidth();
		int vpDimV=gameView.getMeasuredHeight();
		float roomDimU=(float)(state.roomWidth*chrDimU);
		float roomDimV=(float)(state.roomHeight*chrDimV);
		int viewOffsU, viewOffsV;
		if(roomDimU<vpDimU) {
			viewOffsU=(int)((vpDimU-roomDimU)/2.f);
		} else {
			int chrCenterU=chrDimU*state.chrX()+chrDimU/2;
			int primOffsetU=vpDimU/2-chrCenterU;
			int offsetUmin=vpDimU-(int)roomDimU;
			viewOffsU=Math.max(offsetUmin, Math.min(primOffsetU, 0));
		}
		if(roomDimV<vpDimV) {
			viewOffsV=(int)((vpDimV-roomDimV)/2.f);
		} else {
			int chrCenterV=chrDimV*state.chrY()+chrDimV/2;
			int primOffsetV=vpDimV/2-chrCenterV;
			int offsetVmin=vpDimV-chrDimV-(int)roomDimV;
			viewOffsV=Math.max(offsetVmin, Math.min(primOffsetV, chrDimV));
		}
		state.setViewOffs(viewOffsU, viewOffsV);
	}
	@Override
	public boolean onSokoTouch(SokoView v, int x, int y) {
		int dx=x-state.chrX();
		int dy=y-state.chrY();
		int adx=Math.abs(dx);
		int ady=Math.abs(dy);
		if(adx>ady) {
			return moveXY(dx/adx,0);
		} else if(ady>adx) {
			return moveXY(0,dy/ady);
		}
		return false;
	}
	@Override 
	public boolean onSokoDoubleTouch(SokoView v,int x, int y) {
		return false;
	}
	@Override
	public void onAnimationUpdate(ValueAnimator animation) {
		state.animProgress=(Float)animation.getAnimatedValue();
		gameView.invalidate();
	}
	private boolean gotoStage(int stage) { return gotoStage(stage,true); }
	private boolean gotoStage(int stage,boolean showToast) {
		boolean res;
		int oldStg=0;
		if(toast!=null) {
			toast.cancel();
			toast=null;
		}
		if(stage<1) {
			res=false;
		} else {
			oldStg=state.stage;
			res=state.loadStage(this,stage,false);
		}
		if(res) {
			backStack=oldStg;
			state.steps=0;
			calculateViewPort();
			gameView.invalidate();
		} else {
			boolean isFirst=stage<1;
			toast=Toast.makeText(this, isFirst?R.string.toast_fst:R.string.toast_last, Toast.LENGTH_SHORT);
			toast.show();
		}
		updateStatusDisplay();
		return res;
	}
	private void pickStage() {
		StagePicker sp=new StagePicker();
		sp.setFilepath(state.stagesFilename)
		.setOnStagePickListener(new StagePicker.OnStagePickListener() {
			@Override
			public void onStagePicked(int which) {
				gotoStage(which);
			}
		})
		.show(getFragmentManager(), "StagePicker");
	}
	@Override
	public void onBackPressed() {
		if(backStack>0) {
			gotoStage(backStack);
			backStack=0;
		} else {
			super.onBackPressed();
		}
	}
	private void updateStatusDisplay() {
		int highscore_num=highscores.scoreOfStage(state.stage);
		String hslabel;
		if(highscore_num==0) {
			hslabel=getResources().getString(R.string.highscore_never);
		} else {
			hslabel=numFormat.format(highscore_num);
		}
		getActionBar().setTitle(state.stageTitle);
		TextView lowstepsView=(TextView)findViewById(R.id.highscoreLabel);
		lowstepsView.setText(hslabel);
		TextView stepsView=(TextView)findViewById(R.id.scoreLabel);
		stepsView.setText(numFormat.format(state.steps));
	}
	private void undoLastMove() {
		if(state.undoLastMove()) {
			playSound(snd_undo);
			gameView.invalidate();
			updateStatusDisplay();
		}
	}
	private void initSound() {
		sp=new SoundPool(4, android.media.AudioManager.STREAM_MUSIC, 0);
		snd_walk1=sp.load(this, R.raw.walk1,1);
		snd_walk2=sp.load(this, R.raw.walk2,1);
		snd_walk3=sp.load(this, R.raw.walk3,1);
		snd_retry=sp.load(this, R.raw.retry,1);
		snd_clearhs=sp.load(this,  R.raw.clear_hs,1);
		snd_clear=sp.load(this, R.raw.clear,1);
		snd_undo=sp.load(this, R.raw.undo,1);
	}
	private void playWalkSound() {
		int sndid;
		if(state.isLastMoveParcel()) {
			sndid=snd_walk3;
		} else {
			boolean parity=((state.chrX()&1)>0)^((state.chrY()&1)>0);
			sndid=parity?snd_walk1:snd_walk2;
		}
		playSound(sndid);
	}
	private void playSound(int sndId) {
		if(sound) {
			sp.play(sndId, 1.f, 1.f, 0, 0, 1.f);
		}
	}
}
