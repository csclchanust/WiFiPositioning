package chan.eddie.wifipositioning;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class AlertDialogFragment extends DialogFragment {
	
	// callback of activity on intent action
	private OnClickListener mCallback = null;

	public static final String ARG_TITLE = "title";
	public static final String ARG_OK = "OK";
	public static final String ARG_CANCEL = "Cancel";
	public static final String ARG_MSG = "msg";
	public static final String ARG_OPTIONS = "options";
	public static final int CLICK_OK = 1;
	public static final int CLICK_CANCEL = 2;
	
	protected int dialogId = 0;
	protected int selectedIndex = 0;
	protected boolean okOnly = false;
	
    // Container Activity must implement this interface
    public interface OnClickListener {
    	public void onAlertDialogClick(int dialogId, int result, int option);
    }

    public static AlertDialogFragment newInstance(int id, String title, String msg) {
    	return newInstance(id, title, ARG_OK, ARG_CANCEL, msg, null);
    }

    public static AlertDialogFragment newInstance(int id, String title, String[] options) {
    	return newInstance(id, title, ARG_OK, ARG_CANCEL, null, options);
    }
    
	public static AlertDialogFragment newInstance(int id, String title, String ok, String cancel, String msg, String[] options) {
    	AlertDialogFragment frag = new AlertDialogFragment();
    	frag.dialogId = id;
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_OK, ok);
        args.putString(ARG_CANCEL, cancel);
        args.putString(ARG_MSG, msg);
        args.putStringArray(ARG_OPTIONS, options);
        frag.setArguments(args);
        return frag;
	}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(getArguments().getString(ARG_TITLE));
    	String msg = getArguments().getString(ARG_MSG);
    	String[] items = getArguments().getStringArray(ARG_OPTIONS);
    	
    	if (msg != null) {
    		builder.setMessage(msg);
    	} else if (items != null) {
	    	builder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int item) {
	    	    	// save the selected item index for callback
	    	    	selectedIndex = item;
	    	    }
	    	});
    	}
    	
    	builder.setPositiveButton(getArguments().getString(ARG_OK), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int id) {
    			if (mCallback != null)
    				mCallback.onAlertDialogClick(dialogId, CLICK_OK, selectedIndex);
    			dialog.cancel();
    		}
    	});
    	
    	if (!okOnly) {
	    	builder.setNegativeButton(getArguments().getString(ARG_CANCEL), new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int id) {
	    			if (mCallback != null)
	    				mCallback.onAlertDialogClick(dialogId, CLICK_CANCEL, selectedIndex);
	    			dialog.cancel();
	    		}
	    	});
    	}
    	
    	return builder.create();
    }
    
    public void setOnClickListener(OnClickListener listener) {
    	mCallback = listener;
    }
    
    public AlertDialogFragment setSingleButton() {
    	okOnly = true;
    	return this;
    }
}
