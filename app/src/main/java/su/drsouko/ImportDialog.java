package su.drsouko;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;

public class ImportDialog extends DialogFragment implements DialogInterface.OnClickListener {
	private ImportDialogListener listener=null;
	private EditText editText=null;
	CharSequence inputStr=null;
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		editText=(EditText)inflater.inflate(R.layout.import_dialog, null);
		builder.setView(editText)
		.setTitle(R.string.text_paste_here)
		.setPositiveButton(R.string.importbutton, this)
		.setNegativeButton(android.R.string.cancel, this);
		if(inputStr!=null) {
			editText.setText(inputStr);
			inputStr=null;
		}
		return builder.create();
	}
	public void setImportDialogListener(ImportDialogListener theListener) {
		listener=theListener;
	}
	public void onClick(DialogInterface dialog, int which) {
		if(listener!=null) {
			if(which==DialogInterface.BUTTON_POSITIVE) {
				String stages=editText.getText().toString();
				listener.textEntered(stages);
			} else {
				listener.importCancelled();
			}
		}
		return;
	}
	public interface ImportDialogListener {
		public void textEntered(String theText);
		public void importCancelled();
	}
	public void setDefaultValue(CharSequence defaultString) {
		inputStr=defaultString;
	}
}
