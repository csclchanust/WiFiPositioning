package chan.eddie.wifipositioning;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class FloorPlanFragment extends Fragment implements
	OnClickListener, OnSeekBarChangeListener {

	private View view;
	private Button btnInfo, btnPlusUnit, btnMinusUnit;
	private EditText editGridUnit;
	private SeekBar barGridSize;
	protected ViewGroup planContainer;
	protected FloorPlanView viewFloorplan;
	protected int gridUnit, gridSize;
	protected WifiData wData;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        wData = WifiData.getInstance(getActivity());
	}
	
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
    	Log.d("FloorPlanFragment", "onCreateView");
    	
    	if (view == null) {
        	view = inflater.inflate(R.layout.floorplan_fragment, container, false);
        	
        	btnInfo = (Button)view.findViewById(R.id.btnInfo);
        	btnInfo.setOnClickListener(this);
        	btnPlusUnit = (Button)view.findViewById(R.id.btnPlusUnit);
        	btnPlusUnit.setOnClickListener(this);
        	btnMinusUnit = (Button)view.findViewById(R.id.btnMinusUnit);
        	btnMinusUnit.setOnClickListener(this);
        	
        	gridUnit = wData.getGridUnit();
        	editGridUnit = (EditText)view.findViewById(R.id.txtGridUnit);
        	editGridUnit.setText(""+gridUnit);
        	
        	gridSize = wData.getGridSize();
        	barGridSize = (SeekBar)view.findViewById(R.id.barGridSize);
        	barGridSize.setMax(WifiData.MAX_GRID_SIZE);
        	barGridSize.setProgress(gridSize - WifiData.MIN_GRID_SIZE);
        	barGridSize.setOnSeekBarChangeListener(this);
        	
        	Bitmap floorplanImg = wData.getFloorplanImage();
        	Log.d("FloorPlanFragment", "floorplan w="+floorplanImg.getWidth()+",h="+floorplanImg.getHeight());
        	viewFloorplan = (FloorPlanView)view.findViewById(R.id.viewFloorPlan);
        	viewFloorplan.setImageBitmap(floorplanImg);
        	//viewFloorplan.setMaxZoom(4f);
        	
        	viewFloorplan.setGrid(gridSize, gridUnit);
        	viewFloorplan.setLockGrid(true);
    	}
    	return view;
	}

    
	@Override 
	public void onDestroyView() { 
		super.onDestroyView(); 
		ViewGroup parentViewGroup = (ViewGroup) view.getParent(); 
		if( null != parentViewGroup ) { 
			parentViewGroup.removeView( view ); 
		} 
	}
	@Override
	public void onResume() {
		Log.d("FloorPlanFragment", "onResume");
		getActivity().getWindow().setTitle(MainActivity.TITLE_FLOORPLAN);
		super.onResume();
	}

	@Override
	public void onPause() {
		Log.d("FloorPlanFragment", "onPause");
		wData.setGrid((int)((float)gridSize / viewFloorplan.getSaveScale()), gridUnit);
		super.onPause();
	}

	public void onClick(View v) {
		if (v == btnInfo) {
			AlertDialogFragment dialog = AlertDialogFragment.newInstance(0, "Floor Plan Setup Help",
					"Scale the 'Grid Size' and adjust the 'Grid Unit' to match the floor plan scale in meter").setSingleButton();
			dialog.show(getFragmentManager(), MainActivity.TAG_INFO_DIALOG_FRAGMENT);
		} else if (v == btnPlusUnit) {
			gridUnit++;
			editGridUnit.setText(""+gridUnit);
			viewFloorplan.setGrid(gridSize, gridUnit);
			viewFloorplan.invalidate();
		} else if (v == btnMinusUnit) {
			gridUnit--;
			if (gridUnit <= WifiData.MIN_GRID_UNIT)
				gridUnit = WifiData.MIN_GRID_UNIT;
			editGridUnit.setText(""+gridUnit);
			viewFloorplan.setGrid(gridSize, gridUnit);
			viewFloorplan.invalidate();
		}
	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		gridSize = WifiData.MIN_GRID_SIZE + progress;
		viewFloorplan.setGrid(gridSize, gridUnit);
		viewFloorplan.invalidate();
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
	}
}
