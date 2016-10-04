package chan.eddie.wifipositioning;

import java.util.ArrayList;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class SurveyFragment extends Fragment
	implements FloorPlanView.OnFloorPlanClickListener, OnClickListener {
	
	private static final String kEmptyCoordText = "P0(---- , ----)M  Record=0";
	
	private View view;
	protected WifiData wData;
	protected FloorPlanView viewFloorplan;
	protected ToggleButton toggleGrid;
	protected Button btnSurvey, btnList, btnInfo;
	protected ArrayList<Integer> pointList;
	protected TextView txtCoord;
	protected int gridSize, gridUnit;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        wData = WifiData.getInstance(getActivity());
        pointList = new ArrayList<Integer>();
	}
	
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
    	Log.d("SurveyFragment", "onCreateView");
    	
    	if (view == null) {
        	view = inflater.inflate(R.layout.survey_fragment, container, false);
        	
        	gridUnit = wData.getGridUnit();
        	gridSize = wData.getGridSize();
        	Bitmap floorplanImg = wData.getFloorplanImage();
        	Log.d("FloorPlanFragment", "floorplan w="+floorplanImg.getWidth()+",h="+floorplanImg.getHeight());
        	viewFloorplan = (FloorPlanView)view.findViewById(R.id.viewFloorPlan);
        	viewFloorplan.setImageBitmap(floorplanImg);
        	viewFloorplan.setGrid(wData.getGridSize(), wData.getGridUnit());
        	viewFloorplan.setLockGrid(false);
        	viewFloorplan.showGrid(true);
        	viewFloorplan.showPoint(true);
        	viewFloorplan.setOnFloorPlanClickListener(this);
        	
        	btnSurvey = (Button)view.findViewById(R.id.btnSurvey);
        	btnSurvey.setOnClickListener(this);
        	
        	btnList = (Button)view.findViewById(R.id.btnList);
        	btnList.setOnClickListener(this);
        	
        	toggleGrid = (ToggleButton)view.findViewById(R.id.toggleGrid);
        	toggleGrid.setChecked(true);
        	toggleGrid.setOnClickListener(this);
        	
        	txtCoord = (TextView)view.findViewById(R.id.txtCoord);
        	txtCoord.setText(kEmptyCoordText);
        	
        	btnInfo = (Button)view.findViewById(R.id.btnInfo);
        	btnInfo.setOnClickListener(this);

        	// load point list from wifi DB
        	ArrayList<WifiData.Point> pList = wData.getPoints();
        	for (WifiData.Point p: pList) {
    			viewFloorplan.addPoint(p.pid, p.px, p.py, 0, wData.getImgGreenDot(), wData.getImgRedDot(), wData.getImgGreenDot());
    			pointList.add(p.pid);
        	}
    	}
    	return view;
    }

	@Override
	public void onResume() {
		Log.d("SurveyFragment", "onResume");
		getActivity().getWindow().setTitle(MainActivity.TITLE_SURVEY);

		// renew the selected point text
		for (int i=0; i<pointList.size(); i++) {
			FloorPlanView.PointObj obj = viewFloorplan.getPointObj(pointList.get(i));
			if (obj.state == 1) {
				txtCoord.setText("P"+obj.id+"("+wData.formatDecimal(obj.pX*gridUnit/gridSize)+" , "+wData.formatDecimal(obj.pY*gridUnit/gridSize)+")M  Record="+wData.getPoint(obj.id).cnt_survey);
				break;
			}
		}
		
		super.onResume();
	}

	@Override
	public void onPause() {
		Log.d("SurveyFragment", "onPause");
		super.onPause();
	}
	
	public void OnFloorPlanClick(int pointId, float posX, float posY) {
		Log.d("SurveyFragment", "OnFloorPlanClick id="+pointId+", X="+posX+", Y="+posY);
		// check empty survey point and set no select other point
		// For point state, 1 = selected, 0 = normal
		for (int i=0; i<pointList.size(); i++) {
			if (pointId == pointList.get(i))
				continue; // skip the selected point
			if (wData.getPoint(pointList.get(i)).cnt_survey == 0) {
				viewFloorplan.removePoint(pointList.get(i));
				wData.removePoint(pointList.get(i));
				pointList.remove(i);
				--i; // current item is removed, so the index is step back
			} else {
				viewFloorplan.setPointState(pointList.get(i), 0);
			}
		}
		
		if (pointId == 0) { // no point clicked
			int newPointId = wData.insertPoint(posX, posY);
			// add new point to the floor plan
			viewFloorplan.addPoint(newPointId, posX, posY, 1, wData.getImgGreenDot(), wData.getImgRedDot(), wData.getImgGreenDot());
			pointList.add(newPointId);
			txtCoord.setText("P"+newPointId+"("+wData.formatDecimal(posX*gridUnit/gridSize)+" , "+wData.formatDecimal(posY*gridUnit/gridSize)+")M  Record=0");
			Log.d("SurveyFragment", "Point("+posX+","+posY+") id="+newPointId+" created");
		} else {
			// select the selected point state
			viewFloorplan.setPointState(pointId, 1);
			FloorPlanView.PointObj obj = viewFloorplan.getPointObj(pointId);
			txtCoord.setText("P"+pointId+"("+wData.formatDecimal(obj.pX*gridUnit/gridSize)+" , "+wData.formatDecimal(obj.pY*gridUnit/gridSize)+")M  Record="+wData.getPoint(pointId).cnt_survey);
		}
	}

	public void onClick(View v) {
		if (v == btnSurvey) {
			int pointId = 0;
			for (int i=0; i<pointList.size(); i++) {
				FloorPlanView.PointObj obj = viewFloorplan.getPointObj(pointList.get(i));
				if (obj.state == 1) {
					pointId = obj.id;
					break;
				}
			}
			
			if (pointId == 0) {
				Toast.makeText(getActivity(), "Please select a point for WiFi survey in the map first", Toast.LENGTH_LONG).show();
			} else {
				showScanFragment(pointId);
			}
		} else if (v == btnList) {
			showScanFragment(0);
		} else if (v == toggleGrid) {
			viewFloorplan.showGrid(toggleGrid.isChecked());
			viewFloorplan.invalidate();
		} else if (v == btnInfo) {
			AlertDialogFragment dialog = AlertDialogFragment.newInstance(0, "WiFi Surveying Help",
				"Please select a point for WiFi survey in the map first\n"+
				"Click 'Survey' to show the WiFi scanning screen\n"+
				"Click 'List' to show all the collected survey\n"+
				"Click 'Grid' to toggle the grid line on floor plan\n\n"+
				"In the WiFi scanning screen:\n"+
				"Click 'Scan' to carry out the survey and collect the WiFi fingerprint\n"+
				"Click 'Select' to toggle the selection mode for deletion on survey record\n"+
				"Click 'Del' to delete the selected survey record\n"+
				"Click on the survey record to view the detail information in survey").setSingleButton();
			dialog.show(getFragmentManager(), MainActivity.TAG_INFO_DIALOG_FRAGMENT);
		}
	}
	
	public void showScanFragment(int pid) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SurveyScanFragment scanFrag = new SurveyScanFragment();
        scanFrag.setCurPointId(pid);
        ft.replace(R.id.container, scanFrag, MainActivity.TAG_SURVEY_SCAN_FRAGMENT);
        ft.addToBackStack(null);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
	}
}
