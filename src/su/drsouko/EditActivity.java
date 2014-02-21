package su.drsouko;

import android.os.Bundle;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
//import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class EditActivity extends Activity implements SokoView.SokoTouchListener, ValueAnimator.AnimatorUpdateListener {
	public static final String EDIT_PATH="Path";
	private static final String SAVELABEL_STATE="state";
	private static enum Tool {Move, Wall, Floor, Parcel, Target, Player, Rubout}
	private static final String SAVELABEL_TOOL="selected_sokotool";
	private SokoGameState state;
	private Tool currentToolSelection=Tool.Wall;
	private SokoView gameView;
	private boolean dirty;
	private Toast toast;
	private ValueAnimator anim=null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);
		dirty=false;
		getActionBar().setDisplayHomeAsUpEnabled(true);
		gameView=(SokoView)findViewById(R.id.editorSokoView);
		gameView.setSokoTouchListener(this);
		String path=this.getIntent().getExtras().getString(EDIT_PATH);
		if(savedInstanceState==null) {
			state=new SokoGameState(path);
			state.editMode=true;
			gotoStage(1);
			onSelectTool(Tool.Move);
		} else {
			state=(SokoGameState)savedInstanceState.getSerializable(SAVELABEL_STATE);
			Tool lastTool=(Tool)savedInstanceState.getSerializable(SAVELABEL_TOOL);
			if(lastTool!=null) {
				onSelectTool(lastTool);
			}
		}
		gameView.setGameState(state);
	}
	@Override
	public void onResume() {
		super.onResume();
		toast=null;
	}
	@Override
	public void onPause() {
		super.onPause();
		saveStage();
	}
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putSerializable(SAVELABEL_STATE, state);
		savedInstanceState.putSerializable(SAVELABEL_TOOL, currentToolSelection);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.edit, menu);
		return true;
	}
	public void onSelectWallTool(View v) { onSelectTool(Tool.Wall); }
	public void onSelectFloorTool(View v) { onSelectTool(Tool.Floor); }
	public void onSelectParcelTool(View v) { onSelectTool(Tool.Parcel); }
	public void onSelectTargetTool(View v) { onSelectTool(Tool.Target); }
	public void onSelectPlayerTool(View v) { onSelectTool(Tool.Player); }
	public void onSelectRuboutTool(View v) { onSelectTool(Tool.Rubout); }
	public void onSelectMoveTool(View v) { onSelectTool(Tool.Move); }
	private void onSelectTool(Tool whichTool) {
		currentToolSelection=whichTool;
		int bitmap=R.drawable.move;
		boolean enableScroll=false;
		if(currentToolSelection==Tool.Wall) { bitmap=R.drawable.kabe; }
		else if(currentToolSelection==Tool.Floor) { bitmap=R.drawable.yuka; }
		else if(currentToolSelection==Tool.Parcel) { bitmap=R.drawable.nimotsu; }
		else if(currentToolSelection==Tool.Target) { bitmap=R.drawable.target; }
		else if(currentToolSelection==Tool.Player) {bitmap=R.drawable.player_right1; }
		else if(currentToolSelection==Tool.Rubout) {bitmap=R.drawable.blanktile; }
		else if(currentToolSelection==Tool.Move) {
			enableScroll=true;
		}
		ImageView cselView=(ImageView)findViewById(R.id.currentSelectionView);
		cselView.setImageResource(bitmap);
		gameView.setScrollEnable(enableScroll);
	}
	public boolean onMenuItemSelected(int featureId,MenuItem item) {
		if(toast!=null) {
			toast.cancel();
			toast=null;
		}
		int itemid=item.getItemId();
		if(itemid==android.R.id.home) {
			super.onBackPressed();
		} else if(itemid==R.id.action_settitle) {
			callTitleDialog();
		} else if(itemid==R.id.action_editorgoto) {
			pickStage();
		} else if(itemid==R.id.action_addstage) {
			addStage();
		} else if(itemid==R.id.action_rmstage) {
			rmStage();
		} else if(itemid==R.id.action_swap) {
			swapStage();
		} else if(itemid==R.id.action_duplicate) {
			dupStage();
		} else if(itemid==R.id.action_editor_prevstage) {
			action_gotoStage(-1);
		} else if(itemid==R.id.action_editor_nextstage) {
			action_gotoStage(1);
		} else {
			return super.onMenuItemSelected(featureId, item);
		}
		return true;
	}
	private boolean gotoStage(int stage) {
		if(stage<1) { return false; }
		saveStage();
		boolean success=state.loadStage(this, stage, false);
		if(success) {
			getActionBar().setTitle(state.stageTitle);
			state.setViewOffs(0, 0);
			gameView.invalidate();
		}
		return success;
	}
	private char mapStateToSokoChar() {
		char res;
		if(currentToolSelection==Tool.Wall) { res='#'; }
		else if(currentToolSelection==Tool.Floor) { res='.'; }
		else if(currentToolSelection==Tool.Parcel) { res='o'; }
		else if(currentToolSelection==Tool.Target) { res='x'; }
		else { res=' '; }
		return res;
	}
	@Override
	public boolean onSokoTouch(SokoView v, int x, int y) {
		if(currentToolSelection==Tool.Move) { return false; }
		int extend_x_by=0, extend_y_by=0;
		if(x<0) {
			extend_x_by=calcExtendSize(x,gameView.numHorizontalChrs());
			x-=extend_x_by;
			state.setChrXY(state.chrX()-extend_x_by, state.chrY(),true);
			state.setViewOffs(state.viewOffsU()+extend_x_by*(int)(gameView.getChrDimX()*state.scale),
					state.viewOffsV());
		} else if(state.roomWidth<=x) {
			extend_x_by=calcExtendSize(1+x-state.roomWidth,gameView.numHorizontalChrs());
		}
		if(y<0) {
			extend_y_by=calcExtendSize(y,gameView.numVerticalChrs());
			y-=extend_y_by;
			state.setChrXY(state.chrX(), state.chrY()-extend_y_by,true);
			state.setViewOffs(state.viewOffsU(),
					state.viewOffsV()+extend_y_by*(int) (gameView.getChrDimY()*state.scale));
		} else if(state.roomHeight<=y) {
			extend_y_by=calcExtendSize(1+y-state.roomHeight,gameView.numVerticalChrs());
		}
		if(0<Math.abs(extend_x_by)+Math.abs(extend_y_by)) {
			//Log.println(Log.DEBUG, "onSokoTouch", "extend x="+Integer.toString(extend_x_by)+
			//		" extend y="+Integer.toString(extend_y_by));
			if(!state.extendRoom(extend_x_by,extend_y_by)) {
				return false;
			}
		}
		//
		dirty=true;
		if(currentToolSelection==Tool.Player) {
			if(state.room[y][x]=='.') {
				state.setChrXY(x, y, true);
			}
		} else {
			char c=mapStateToSokoChar();
			state.room[y][x]=c;
		}
		gameView.invalidate();
		return true;
	}
	private int calcExtendSize(int delta, int screenSz) {
		int adelta=Math.abs(delta);
		int multiplier=(int) Math.ceil(((float)adelta)/((float)screenSz));
		int sz=screenSz*multiplier*(delta>=0?1:-1);
		return sz;
	}
	@Override
	public boolean onSokoDoubleTouch(SokoView v, int x, int y) {
		return false;
	}
	private boolean saveStage() {
		if(dirty) {
			SokoFileWriter sfw=new SokoFileWriter(state);
			sfw.saveStage(getFilesDir());
			dirty=false;
		}
		return true;
	}
	private void callTitleDialog() {
		TitlesDialog dialog=new TitlesDialog();
		dialog.setGameState(state);
		dialog.setOnTitleChangedListener(new TitlesDialog.OnTitleChangedListener() {
			@Override
			public void onTitleChanged() {
				dirty=true;
				getActionBar().setTitle(state.stageTitle);
			}
		});
		dialog.show(getFragmentManager(), "TITLES_DIALOG");
	}
	private void pickStage() {
		saveStage();
		StagePicker sp=new StagePicker();
		sp.setFilepath(state.stagesFilename)
		.setOnStagePickListener(new StagePicker.OnStagePickListener() {
			@Override
			public void onStagePicked(int which) {
				gotoStage(which);
			}
		})
		.show(getFragmentManager(), "Editor_StagePicker");
	}
	private void addStage() {
		SokoFileWriter sfw=new SokoFileWriter(state);
		sfw.appendOneStage(getResources().getString(R.string.newstage_templ));
		state.loadStage(this, SokoGameState.MAX_STAGE, true);
		gotoStage(state.loaded_stages);
	}
	private void rmStage() {
		AlertDialog.Builder confirm=new AlertDialog.Builder(this);
		String text=String.format(getResources().getString(R.string.dialog_remove_text),
				state.stageTitle);
		confirm.setTitle(R.string.dialog_remove_title)
		.setMessage(text)
		.setPositiveButton(R.string.dialog_remove_go, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				SokoFileWriter sfw=new SokoFileWriter(state);
				sfw.removeCurrentStage(getFilesDir(), getResources().getString(R.string.newstage_templ));
				gotoStage(Math.max(1,state.stage-1));
			}
		})
		.setNegativeButton(android.R.string.cancel, null)
		.show();
	}
	private void swapStage() {
		saveStage();
		String title=String.format(getResources().getString(R.string.dialog_swap_title),
				state.stageTitle);
		StagePicker sp=new StagePicker();
		sp.setTitle(title)
		.setFilepath(state.stagesFilename)
		.setOnStagePickListener(new StagePicker.OnStagePickListener() {
			@Override
			public void onStagePicked(int which) {
				SokoFileWriter sfw=new SokoFileWriter(state);
				if(sfw.moveCurrentStageTo(getFilesDir(), which)) {
					int movedto;
					if(which<=state.stage) {
						movedto=which;
					} else {
						movedto=which-1;
					}
					gotoStage(movedto);
				}
			}
		})
		.show(getFragmentManager(), "SwapStagePicker");
	}
	private void dupStage() {
		saveStage();
		SokoFileWriter sfw=new SokoFileWriter(state);
		if(sfw.dupCurrentStage(getFilesDir())) {
			if(toast!=null) { toast.cancel(); }
			toast=Toast.makeText(this, R.string.toast_dupcomplete, Toast.LENGTH_SHORT);
			toast.show();
		}
	}
	private boolean action_gotoStage(int delta) {
		boolean success=gotoStage(state.stage+delta);
		if(!success) {
			int msg;
			if(delta>0) {
				msg=R.string.toast_last;
			} else {
				msg=R.string.toast_fst;
			}
			toast=Toast.makeText(this, msg, Toast.LENGTH_SHORT);
			toast.show();
		}
		return success;
	}
	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN || keyCode==KeyEvent.KEYCODE_J ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_8) {
			scrollHalfScreen(0,1);
		} else if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT || keyCode==KeyEvent.KEYCODE_H ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_4) {
			scrollHalfScreen(-1,0);
		} else if(keyCode==KeyEvent.KEYCODE_DPAD_UP || keyCode==KeyEvent.KEYCODE_K ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_2) {
			scrollHalfScreen(0,-1);
		} else if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT || keyCode==KeyEvent.KEYCODE_L ||
				keyCode==KeyEvent.KEYCODE_NUMPAD_6) {
			scrollHalfScreen(1,0);
		} else if(keyCode==KeyEvent.KEYCODE_Q) {
			onSelectMoveTool(null);
		} else if(keyCode==KeyEvent.KEYCODE_W) {
			onSelectWallTool(null);
		} else if(keyCode==KeyEvent.KEYCODE_E) {
			onSelectFloorTool(null);
		} else if(keyCode==KeyEvent.KEYCODE_R) {
			onSelectParcelTool(null);
		} else if(keyCode==KeyEvent.KEYCODE_T) {
			onSelectPlayerTool(null);
		} else if(keyCode==KeyEvent.KEYCODE_Y) {
			onSelectRuboutTool(null);
		} else if(keyCode==KeyEvent.KEYCODE_PAGE_DOWN) {
			action_gotoStage(+1);
		} else if(keyCode==KeyEvent.KEYCODE_PAGE_UP) {
			action_gotoStage(-1);
		} else {
			return super.onKeyDown(keyCode, event);
		}
		return true;
	}
	private void scrollHalfScreen(int diffx, int diffy) {
		int halfOffsU=gameView.getWidth()/2;
		int halfOffsV=gameView.getHeight()/2;
		int newOffsU=state.viewOffsU()-diffx*halfOffsU;
		int newOffsV=state.viewOffsV()-diffy*halfOffsV;
		int limOffsU=Math.max(gameView.minOffsU(), Math.min(gameView.maxOffsU(), newOffsU));
		int limOffsV=Math.max(gameView.minOffsV(), Math.min(gameView.maxOffsV(), newOffsV));
		state.setViewOffs(limOffsU, limOffsV);
		if(anim==null) { initAnimator(); }
		anim.start();
	}
	private void initAnimator() {
		anim=ValueAnimator.ofFloat(0.f,1.f);
		anim.setDuration(50);
		anim.addUpdateListener(this);
	}
	@Override
	public void onAnimationUpdate(ValueAnimator animation) {
		state.animProgress=(Float)animation.getAnimatedValue();
		gameView.invalidate();
	}
}
