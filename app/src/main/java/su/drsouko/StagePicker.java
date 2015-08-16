package su.drsouko;

import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.SimpleAdapter;

public class StagePicker extends DialogFragment implements DialogInterface.OnClickListener {
	private String filepath=null;
	private OnStagePickListener listener=null;
	private AlertDialog.Builder builder=null;
	private String title=null;
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		builder=new AlertDialog.Builder(getActivity());
		setAdapter();
		if(title!=null) {
			builder.setTitle(title);
		}
		return builder.create();
	}
	private void setAdapter() {
		SokoStageLoader loader=new SokoStageLoader(getActivity(),filepath);
		List<Map<String,String>> stages=loader.getStageTitles();
		builder.setAdapter(new SimpleAdapter(getActivity(), stages, android.R.layout.simple_list_item_1,
						new String[] {SokoStageLoader.STAGE_COL},
						new int[] {android.R.id.text1}),
						this);
	}
	public StagePicker setFilepath(String thePath) {
		filepath=thePath;
		return this;
	}
	public StagePicker setOnStagePickListener(OnStagePickListener theListener) {
		listener=theListener;
		return this;
	}
	public interface OnStagePickListener {
		public void onStagePicked(int which);
	}
	public StagePicker setTitle(String theTitle) {
		title=theTitle;
		return this;
	}
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(listener!=null) {
			listener.onStagePicked(which+1);
		}
	}
}
