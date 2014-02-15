package su.drsouko;

import java.text.NumberFormat;

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
	private TextView lowstepsView, stepsView;
	private NumberFormat numFormat=NumberFormat.getInstance();
	private ValueAnimator anim;
	private final String SAVELABEL_STATE="state";
	private HighscoreMgr highscores;
	public static final String STARTINTENT_FILENAME="stagesFile";
	private int backStack;
	private Toast toast;
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
		setContentView(R.layout.activity_soko);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		//View controlPanel=findViewById(R.id.controlPanel);
		gameView=(SokoView)findViewById(R.id.gameView);
		lowstepsView=(TextView)findViewById(R.id.highscoreLabel);
		stepsView=(TextView)findViewById(R.id.scoreLabel);
		String path=this.getIntent().getExtras().getString(STARTINTENT_FILENAME);
		highscores=new HighscoreMgr(this,path);
		highscores.load();
		if(savedInstanceState==null) {
			state=new SokoGameState(this,path);
			if(!gotoStage(highscores.minUnclearedStage(),false)) {
				gotoStage(1,false);
			}
		} else {
			state=(SokoGameState)savedInstanceState.getSerializable(SAVELABEL_STATE);
		}
		gameView.setGameState(state);
		if(savedInstanceState==null) {
			ViewTreeObserver vto=gameView.getViewTreeObserver();
			ViewPortAdjuster vpa=new ViewPortAdjuster();
			vto.addOnPreDrawListener(vpa);
		}
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
			return true;
		} else if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT || keyCode==KeyEvent.KEYCODE_H ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_4) {
			goLeft(null);
			return true;
		} else if(keyCode==KeyEvent.KEYCODE_DPAD_UP || keyCode==KeyEvent.KEYCODE_K ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_2) {
			goUp(null);
			return true;
		} else if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT || keyCode==KeyEvent.KEYCODE_L ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_6) {
			goRight(null);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	@Override
	public boolean onMenuItemSelected(int featureId,MenuItem item) {
		int itemid=item.getItemId();
		if(itemid==android.R.id.home) {
			super.onBackPressed();
			return true;
		} else if(itemid==R.id.action_retry) {
			gotoStage(state.stage);
			stepsView.setText(numFormat.format(state.steps));
			gameView.invalidate();
		} else if(itemid==R.id.action_nextstage) {
			gotoStage(state.stage+1);
		} else if(itemid==R.id.action_prevstage) {
			gotoStage(state.stage-1);
		} else if(itemid==R.id.action_goto) {
			pickStage();
		}
		return super.onMenuItemSelected(featureId, item);
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
			}
			stepsView.setText(numFormat.format(state.steps));
			calculateViewPort();
			state.animProgress=0.f;
			anim.start();
		}
		return success;
	}
	private void handleStageClear() {
		boolean newHS=highscores.putScore(state.stage, state.steps);
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
			getActionBar().setTitle(state.stageTitle);
			state.steps=0;
			calculateViewPort();
			int highscore_num=highscores.scoreOfStage(stage);
			String hslabel;
			if(highscore_num==0) {
				hslabel=getResources().getString(R.string.highscore_never);
			} else {
				hslabel=numFormat.format(highscore_num);
			}
			lowstepsView.setText(hslabel);
			stepsView.setText(numFormat.format(state.steps));
			gameView.invalidate();
		} else {
			boolean isFirst=stage<1;
			toast=Toast.makeText(this, isFirst?R.string.toast_fst:R.string.toast_last, Toast.LENGTH_SHORT);
			toast.show();
		}
		
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
}
