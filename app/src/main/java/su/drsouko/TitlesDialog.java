package su.drsouko;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


public class TitlesDialog extends DialogFragment implements DialogInterface.OnClickListener {
	private SokoGameState gameState=null;
	private OnTitleChangedListener listener=null;
	private View rootView;
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View v=inflater.inflate(R.layout.titles_dialog, null);
		builder.setView(v)
		.setTitle(R.string.titledialog_title)
		.setPositiveButton(R.string.titledialog_setbutton, this)
		.setNegativeButton(android.R.string.cancel,null);
		if(gameState!=null) {
			EditText fileTitleEditText=(EditText) v.findViewById(R.id.fileTitleEdit);
			fileTitleEditText.setText(gameState.stagesTitle);
			EditText fileCopyEditText=(EditText) v.findViewById(R.id.fileCopyEdit);
			fileCopyEditText.setText(gameState.stagesCopyright);
			EditText stageTitleEditText=(EditText) v.findViewById(R.id.stageTitleEdit);
			stageTitleEditText.setText(gameState.stageTitle);
		}
		rootView=v;
		return builder.create();
	}
	public void setGameState(SokoGameState theState) { gameState=theState; }
	public void setOnTitleChangedListener(OnTitleChangedListener theListener) {
		listener=theListener;
	}
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(gameState!=null) {
			EditText fileTitleEditText=(EditText) rootView.findViewById(R.id.fileTitleEdit);
			Editable newTitleText=fileTitleEditText.getText();
			if(newTitleText.length()>0) {
				gameState.stagesTitle=newTitleText.toString();
			}
			EditText fileCopyEditText=(EditText) rootView.findViewById(R.id.fileCopyEdit);
			gameState.stagesCopyright=fileCopyEditText.getText().toString();
			EditText stageTitleEditText=(EditText) rootView.findViewById(R.id.stageTitleEdit);
			Editable newStageTitleText=stageTitleEditText.getText();
			if(newStageTitleText.length()>0) {
				gameState.stageTitle=newStageTitleText.toString();
			}
		}
		if(listener!=null) {
			listener.onTitleChanged();
		}
	}
	public interface OnTitleChangedListener {
		public void onTitleChanged();
	}
}
