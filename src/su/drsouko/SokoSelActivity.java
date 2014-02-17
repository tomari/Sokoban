package su.drsouko;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.http.protocol.HTTP;

import android.os.Bundle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
//import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class SokoSelActivity extends ListActivity {
	private LinkedList<Map<String,String>> files;
	private SokoStageLoader loader;
	private static final String COL_TITLE="title";
	private static final String COL_COPYRIGHT="copyright";
	private static final String COL_FILENAME="filename";
	private static final String STAGEFILE_SUFFIX=".txt";
	private enum SelState {Play, Export, Delete, ClearHS, Edit };
	private SelState state=SelState.Play;
	private static final String SAVELABEL_STATE="state";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_soko_sel);
		loader=new SokoStageLoader(this);
		Intent theIntent=getIntent();
		if(theIntent!=null) {
			String action=theIntent.getAction();
			String type=theIntent.getType();
			if(HTTP.PLAIN_TEXT_TYPE.equals(type)) {
				if(Intent.ACTION_VIEW.equals(action)|| Intent.ACTION_SEND.equals(action)) {
					final String str_in=theIntent.getStringExtra(Intent.EXTRA_TEXT);
					if(str_in!=null) {
						importFile(str_in);
					}
				}
			}
		}
	}
	@Override
	public void onResume() {
		super.onResume();
		loadFilelist();
	}
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putSerializable(SAVELABEL_STATE, state);
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		state=(SelState)savedInstanceState.getSerializable(SAVELABEL_STATE);
		if(state==null) {
			state=SelState.Play;
		}
		transitionStateTo(state);
	}
	private void loadFilelist() {
		files=new LinkedList<Map<String,String>>();
		File filesDir=getFilesDir();
		File[] dir=filesDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(STAGEFILE_SUFFIX);
			}
		});
		addFile(null); // add internal stages
		for(File f:dir) {
			addFile(f.getPath());
		}
		ListAdapter adapter=new SimpleAdapter(this,files,android.R.layout.simple_list_item_2,
				new String[] {COL_TITLE, COL_COPYRIGHT },
				new int[] {android.R.id.text1, android.R.id.text2});
		setListAdapter(adapter);
	}
	private boolean addFile(String path) {
		loader.setStagesFilename(path);
		boolean res=loader.loadStage(0);
		String stagesTitle,stagesCopyright;
		if(res) {
			stagesTitle=loader.stagesTitle();
			stagesCopyright=loader.stagesCopyright();
		} else {
			stagesTitle="(Error)";
			stagesCopyright="";
		}
		Map<String,String> row=new HashMap<String,String>();
		row.put(COL_TITLE, stagesTitle);
		row.put(COL_COPYRIGHT, stagesCopyright);
		row.put(COL_FILENAME,path);
		files.push(row);
		return res;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.soko_sel, menu);
		return true;
	}
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if(state==SelState.Play){
			Intent intent=new Intent(this,SokoActivity.class);
			intent.putExtra(SokoActivity.STARTINTENT_FILENAME, files.get(position).get(COL_FILENAME));
			startActivity(intent);
		} else if(state==SelState.Export) {
			exportFile(position);
		} else if(state==SelState.Delete) {
			deleteFile(position);
		} else if(state==SelState.ClearHS) {
			clearHighscores(position);
		} else if(state==SelState.Edit) {
			editStages(position);
		}
	}
	private void exportFile(int position) {
		Intent intent=new Intent(Intent.ACTION_SEND);
		intent.setType(HTTP.PLAIN_TEXT_TYPE);
		loader.setStagesFilename(files.get(position).get(COL_FILENAME));
		String entireFile=loader.getEntireFile();
		if(entireFile==null) { return; }
		intent.putExtra(Intent.EXTRA_TEXT, entireFile);
		String stgsTitle=files.get(position).get(COL_TITLE);
		String chooserTitle=String.format((String)getResources().getString(R.string.title_chooser), stgsTitle);
		Intent chooser=Intent.createChooser(intent, chooserTitle);
		if(intent.resolveActivity(getPackageManager())!=null) {
			startActivity(chooser);
		}
		transitionStateTo(SelState.Play);
	}
	private void deleteFile(int position) {
		final String stagesTitle=files.get(position).get(COL_TITLE);
		final String stagesPath=files.get(position).get(COL_FILENAME);
		AlertDialog.Builder confirm=new AlertDialog.Builder(this);
		if(stagesPath==null) {
			String alerttitle=String.format((String)(getResources().getString(R.string.title_nodelete)),
					stagesTitle);
			confirm.setMessage(R.string.text_nodelete)
			.setTitle(alerttitle)
			.setPositiveButton(android.R.string.cancel, null)
			.show();
		} else {
			final String confirmText=String.format((String) getResources().getText(R.string.text_delete), stagesTitle);
			final String hsPath=HighscoreMgr.scoreFilePath(this, stagesPath);
			confirm.setMessage(confirmText)
			.setTitle(R.string.title_deletedialog)
			.setPositiveButton(R.string.deletedialog_deletebutton, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					File stgFile=new File(stagesPath);
					stgFile.delete();
					//Log.println(Log.DEBUG, getPackageName(), "delete "+stagesPath);
					File hsFile=new File(hsPath);
					hsFile.delete();
					//Log.println(Log.DEBUG, getPackageName(), "delete "+hsPath);
					loadFilelist();
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
		}
		transitionStateTo(SelState.Play);
	}
	private void clearHighscores(int position) {
		final String stagesTitle=files.get(position).get(COL_TITLE);
		final String stagesPath=files.get(position).get(COL_FILENAME);
		final String hsPath=HighscoreMgr.scoreFilePath(this, stagesPath);
		AlertDialog.Builder confirm=new AlertDialog.Builder(this);
		final String msg=String.format((String)getResources().getText(R.string.text_clearhs),stagesTitle);
		confirm.setMessage(msg)
		.setTitle(R.string.title_clearhsdialog)
		.setPositiveButton(R.string.clearbutton, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				File hsFile=new File(hsPath);
				hsFile.delete();
				//Log.println(Log.DEBUG, "ClearHS", "Delete "+hsPath);
			}
		})
		.setNegativeButton(android.R.string.cancel, null)
		.show();
		transitionStateTo(SelState.Play);
	}
	private void importFile(final String input) {
		ImportDialog id=new ImportDialog();
		id.setImportDialogListener(new ImportDialog.ImportDialogListener() {
			@Override
			public void textEntered(String theText) {
				String newFilePath=findNextFilename();
				if(newFilePath==null) { return; }
				if(!loader.minimalSanityCheck(theText)) {
					return;
				}
				loader.setStagesFilename(newFilePath);
				boolean success=loader.writeEntireFile(theText);
				if(success) {
					loadFilelist();
					if(input!=null) {
						setResult(android.app.Activity.RESULT_OK);
						finish();
					}
				}
			}
			@Override
			public void importCancelled() {
				if(input!=null) {
					setResult(android.app.Activity.RESULT_CANCELED);
					finish();
				}
			}
		});
		id.setDefaultValue(input);
		id.show(getFragmentManager(), "ImportDialog");
	}
	private String findNextFilename() {
		int nextnum;
		String maxPath=files.peek().get(COL_FILENAME);
		if(maxPath==null) {
			nextnum=1;
		} else {
			File maxFile=new File(files.peek().get(COL_FILENAME));
			String maxName=maxFile.getName();
			String maxBasename=maxName.substring(0,maxName.length()-4);
			int maxNum=Integer.parseInt(maxBasename, 0x10);
			nextnum=maxNum+1;
		}
		String dir=getFilesDir()+File.separator;
		int max_try=128;
		do {
			String filenameCandidate=String.format("%s%08X"+STAGEFILE_SUFFIX, dir,nextnum);
			File fileCandidate=new File(filenameCandidate);
			if(fileCandidate.exists()) {
				nextnum++;
				continue;
			} else {
				//Log.println(Log.DEBUG, getPackageName(), "Create a file "+filenameCandidate);
				return filenameCandidate;
			}
		} while(max_try-->0);
		return null;
	}
	private void newFile() {
		String newPath=findNextFilename();
		if(newPath==null) {
			Toast.makeText(this, R.string.toast_fileslot_full, Toast.LENGTH_SHORT).show();
			return;
		}
		loader.setStagesFilename(newPath);
		if(!loader.writeEntireFile(getResources().getString(R.string.newfile_templ)+
				getResources().getString(R.string.newstage_templ))) {
			Toast.makeText(this, R.string.toast_io_error, Toast.LENGTH_SHORT).show();
		}
		loadFilelist();
	}
	@Override
	public boolean onMenuItemSelected(int featureId,MenuItem item) {
		int itemid=item.getItemId();
		if(itemid==R.id.action_settings) {
			Intent intent=new Intent(this,SettingsActivity.class);
			startActivity(intent);
		} else if(itemid==R.id.action_export) {
			transitionStateTo(SelState.Export);
		} else if(itemid==R.id.action_delete) {
			transitionStateTo(SelState.Delete);
		} else if(itemid==R.id.action_clearhs) {
			transitionStateTo(SelState.ClearHS);
		} else if(itemid==R.id.action_import) {
			importFile(null);
		} else if(itemid==R.id.action_new) {
			newFile();
		} else if(itemid==R.id.action_edit) {
			transitionStateTo(SelState.Edit);
		} else if(itemid==R.id.action_about) {
			handleAboutDialog();
		} else {
			return super.onMenuItemSelected(featureId,item);
		}
		return true;
	}
	private void editStages(int position) {
		String filePath=files.get(position).get(COL_FILENAME);
		if(filePath==null) {
			String alertTitle=String.format((String) (getResources().getString(R.string.title_cantedit)),
					files.get(position).get(COL_TITLE));
			AlertDialog.Builder alert=new AlertDialog.Builder(this);
			alert.setTitle(alertTitle)
			.setMessage(R.string.text_cantedit)
			.setPositiveButton(android.R.string.cancel, null)
			.show();
		} else {
			Intent intent=new Intent(this,EditActivity.class);
			intent.putExtra(EditActivity.EDIT_PATH, files.get(position).get(COL_FILENAME));
			startActivity(intent);
		}
		transitionStateTo(SelState.Play);
	}
	@Override
	public void onBackPressed() {
		if(state!=SelState.Play) {
			transitionStateTo(SelState.Play);
		} else {
			super.onBackPressed();
		}
	}
	private void transitionStateTo(SelState aState) {
		int newTitle;
		if(aState==SelState.Export) {
			newTitle=R.string.title_export;
		} else if(aState==SelState.Delete) {
			newTitle=R.string.title_delete;
		} else if(aState==SelState.ClearHS) {
			newTitle=R.string.title_clearhs;
		} else if(aState==SelState.Edit) {
			newTitle=R.string.title_edit;
		} else {
			newTitle=R.string.app_name;
		}
		getActionBar().setTitle(getResources().getString(newTitle));
		state=aState;
	}
	private void handleAboutDialog() {
		String title=String.format(getResources().getString(R.string.aboutdialog_title),
				getResources().getString(R.string.app_name));
		String versionName;
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			versionName="0";
		}
		String msg=String.format(getResources().getString(R.string.aboutdialog_text),
				versionName);
		AlertDialog.Builder about=new AlertDialog.Builder(this);
		about.setTitle(title).setMessage(msg).setPositiveButton(R.string.aboutdialog_dismiss, null)
		.show();
	}
}
