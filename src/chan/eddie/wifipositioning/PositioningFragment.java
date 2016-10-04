package chan.eddie.wifipositioning;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.net.wifi.ScanResult;
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

public class PositioningFragment extends Fragment
	implements FloorPlanView.OnFloorPlanClickListener,
	OrientationSensor.OnSensorChangeListener, OnClickListener {
	
	public static final String kEmptyEstCoordStr = "P(---- , ----)M  Dir:---¢X";
	public static final long kRotateInterval = 200;
	public static final float kRotateAdjust = 20;
	
	private View view;
	protected WifiData wData;
	protected WifiScanner wScanner;
	protected FloorPlanView viewFloorplan;
	protected int gridSize, gridUnit;
	protected OrientationSensor sensor;
	protected TextView txtEstCoord;
	protected float curDirection;
	protected ToggleButton toggleScan, toggleGrid, togglePoint, toggleRotate;
	protected ToggleButton toggleTrustRegion, toggleOrientationFilter;
	protected Button btnInfo;
	protected Timer scanTimer;
	protected Activity activity;
	protected ArrayList<WifiData.PositionSurvey> listSurvey;
	protected WifiData.PositionTemp curPoint, lastPoint, newPoint;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        // this fragment has own option menu
        setHasOptionsMenu(true);
        
        activity = getActivity();
        wData = WifiData.getInstance(activity);
        wScanner = WifiScanner.getInstance(activity);
    	sensor = new OrientationSensor(activity);
    	sensor.setOnSensorChangeListener(this);
		gridUnit = wData.getGridUnit();
    	gridSize = wData.getGridSize();
	}

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
    	Log.d("PositioningFragment", "onCreateView");
    	
    	if (view == null) {
        	view = inflater.inflate(R.layout.position_fragment, container, false);
        	
        	gridUnit = wData.getGridUnit();
        	gridSize = wData.getGridSize();
        	Bitmap floorplanImg = wData.getFloorplanImage();
        	Log.d("PositioningFragment", "floorplan w="+floorplanImg.getWidth()+",h="+floorplanImg.getHeight());
        	viewFloorplan = (FloorPlanView)view.findViewById(R.id.viewFloorPlan);
        	viewFloorplan.setImageBitmap(floorplanImg);
        	viewFloorplan.setGrid(gridSize, gridUnit);
        	viewFloorplan.setLockGrid(false);
        	viewFloorplan.showTrack(true);
        	viewFloorplan.setOnFloorPlanClickListener(this);
        	
        	txtEstCoord = (TextView)view.findViewById(R.id.txtEstCoord);
        	txtEstCoord.setText(kEmptyEstCoordStr);
        	
        	toggleScan = (ToggleButton)view.findViewById(R.id.toggleScan);
        	toggleScan.setChecked(false);
        	toggleScan.setOnClickListener(this);
        	
        	toggleRotate = (ToggleButton)view.findViewById(R.id.toggleRotate);
        	toggleRotate.setChecked(false);
        	toggleRotate.setOnClickListener(this);
        	viewFloorplan.showRotate(toggleRotate.isChecked());
        	
        	toggleTrustRegion = (ToggleButton)view.findViewById(R.id.toggleTrustRegion);
        	toggleTrustRegion.setChecked(true);
        	toggleTrustRegion.setOnClickListener(this);
        	viewFloorplan.showTrustRegion(toggleTrustRegion.isChecked());
        	
        	toggleOrientationFilter = (ToggleButton)view.findViewById(R.id.toggleOritentationFilter);
        	toggleOrientationFilter.setChecked(true);
        	toggleOrientationFilter.setOnClickListener(this);
        	viewFloorplan.showOrientationFilter(toggleOrientationFilter.isChecked());
        	
        	togglePoint = (ToggleButton)view.findViewById(R.id.togglePoint);
        	togglePoint.setChecked(true);
        	togglePoint.setOnClickListener(this);
        	viewFloorplan.showPoint(togglePoint.isChecked());
        	
        	toggleGrid = (ToggleButton)view.findViewById(R.id.toggleGrid);
        	toggleGrid.setChecked(true);
        	toggleGrid.setOnClickListener(this);
        	viewFloorplan.showGrid(toggleGrid.isChecked());
        	
        	btnInfo = (Button)view.findViewById(R.id.btnInfo);
        	btnInfo.setOnClickListener(this);
        	
        	// load survey point list from WiFi DB, and add to floor plan
        	ArrayList<WifiData.Point> pList = wData.getPoints();
        	for (WifiData.Point p: pList)
    			viewFloorplan.addPoint(p.pid, p.px, p.py, 0, wData.getImgGreenDot(), wData.getImgGreenDot(), wData.getImgGreenDot());
    	}
    	return view;
    }

	@Override
	public void onResume() {
		super.onResume();
		Log.d("PositioningFragment", "onResume");
		
		activity.getWindow().setTitle(MainActivity.TITLE_POSITIONING);
		Toast.makeText(activity, "Click 'Start' to do WiFi positioning\n\n"+
				"Click 'Direction' to show the real time direction view on the floor plan\n\n"+
				"Tag on the floor plan to make an estimated WiFi positioning point", Toast.LENGTH_LONG).show();
		
		if (listSurvey == null)
			listSurvey = wData.getPositionSurveys();
		
		sensor.startSensor();
	}

	@Override
	public void onPause() {
		Log.d("PositioningFragment", "onPause");
		sensor.stopSensor();
		
		if (toggleScan.isChecked())
			stopWifiScan();
		
		super.onPause();
	}

	public void OnFloorPlanClick(int pointId, float posX, float posY) {
		Log.d("PositioningFragment", "OnFloorPlanClick id="+pointId+", X="+posX+", Y="+posY);
		
		// make a manual estimated WiFi positioning point for testing
		float curDegree = curDirection;
		lastPoint = curPoint;
		curPoint = null;
		
		if (toggleOrientationFilter.isChecked()) {
			ArrayList<WifiData.PositionSurvey> listFilter = wScanner.getOrientationFilteredPositionSurveys(listSurvey, lastPoint, curDegree, WifiData.OF_HALF_ANGLE);
			// if orientation filter has survey return, here is for checking only
			if (listFilter != null && listFilter.size() > 0)
				Log.d("PositioningFragment", "OnFloorPlanClick: OrientationFiltered survey size="+listFilter.size());
			else
				Log.d("PositioningFragment", "OnFloorPlanClick: full survey size="+listSurvey.size());
		}

		// since it is a manual point, skip the k-means step
		// and make a custom point as estimated point
		curPoint = wData.newPositionTemp();
		curPoint.px = posX;
		curPoint.py = posY;
		curPoint.pAdjX = posX;
		curPoint.pAdjY = posY;
		curPoint.degree = curDegree;
		
		if (toggleTrustRegion.isChecked())
			wScanner.calcTrustRegion(curPoint, lastPoint);
		
		updateEstCoordText(curPoint.degree);
		viewFloorplan.addTrack(curPoint.px, curPoint.py, curPoint.pAdjX, curPoint.pAdjY, curPoint.radius, curPoint.degree);
		viewFloorplan.invalidate();
	}

	public synchronized void updateEstCoordText(float degree) {
		if (curPoint != null)
			txtEstCoord.setText("P("+wData.formatDecimal(curPoint.pAdjX*gridUnit/gridSize)+" , "+wData.formatDecimal(curPoint.pAdjY*gridUnit/gridSize)+")M  Dir:"+wData.formatDecimal(degree)+"¢X");
		else
			txtEstCoord.setText("P(---- , ----)M  Dir:"+wData.formatDecimal(degree)+"¢X");
		txtEstCoord.invalidate();
	}

	public void onClick(View v) {
		if (v == toggleScan) {
			if (!wScanner.isWifiEnabled()) {
				Toast.makeText(activity, "WiFi scan is available on real device only", Toast.LENGTH_LONG).show();
				return;
			}
			
			if (toggleScan.isChecked()) {
				startWifiScan();
			} else {
				stopWifiScan();
			}
		} else if (v == toggleRotate) {
			viewFloorplan.showRotate(toggleRotate.isChecked());
			viewFloorplan.invalidate();
		} else if (v == toggleTrustRegion) {
			viewFloorplan.showTrustRegion(toggleTrustRegion.isChecked());
			viewFloorplan.invalidate();
		} else if (v == toggleOrientationFilter) {
			viewFloorplan.showOrientationFilter(toggleOrientationFilter.isChecked());
			viewFloorplan.invalidate();
		} else if (v == toggleGrid) {
			viewFloorplan.showGrid(toggleGrid.isChecked());
			viewFloorplan.invalidate();
		} else if (v == togglePoint) {
			viewFloorplan.showPoint(togglePoint.isChecked());
			viewFloorplan.invalidate();
		} else if (v == btnInfo) {
			AlertDialogFragment dialog = AlertDialogFragment.newInstance(0, "WiFi Positioning Help",
				"Click 'Start' to do WiFi positioning\n"+
				"Click 'Direction' to show the real time direction view on the floor plan\n"+
				"Click 'TR' to toggle the function of Trust Region during positioning\n"+
				"Click 'OR' to toggle the function of Orientation Filter during positioning\n"+
				"Click 'Point' to toggle the survey point\n"+
				"Click 'Grid' to toggle the grid line on floor plan\n\n"+
				"Tag on the floor plan to make a new estimated WiFi positioning point for testing").setSingleButton();
			dialog.show(getFragmentManager(), MainActivity.TAG_INFO_DIALOG_FRAGMENT);
		}
	}
	
	public void startWifiScan() {
		toggleScan.setChecked(true);
		scanTimer = new Timer(true);
		scanTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				float curDegree = curDirection;
				newPoint = null;
				ArrayList<WifiData.PositionSurvey> listFilter = null;
				List<ScanResult> listScan = wScanner.scanWifi();
				
				if (toggleOrientationFilter.isChecked())
					listFilter = wScanner.getOrientationFilteredPositionSurveys(listSurvey, curPoint, curDegree, WifiData.OF_HALF_ANGLE);
				if (listFilter != null && listFilter.size() > 0) {
					// if orientation filter has survey return
					Log.d("startWifiScan", "OrientationFiltered survey size="+listFilter.size());
					newPoint = wScanner.calcKMeansPoint(listFilter, listScan);
				}
				
				if (newPoint == null) {
					// do full list scan if orientation filter without points
					Log.d("startWifiScan", "full survey size="+listSurvey.size());
					newPoint = wScanner.calcKMeansPoint(listSurvey, listScan);
				}

				if (newPoint != null) {
					newPoint.degree = curDegree;
					if (toggleTrustRegion.isChecked())
						wScanner.calcTrustRegion(newPoint, curPoint);

					// update UI in UI thread to prevent exception
					activity.runOnUiThread(new Runnable() {public void run() {
						lastPoint = curPoint; // swap points
						curPoint = newPoint;

						// run UI update function
						//Toast.makeText(activity, "WiFi scan @"+(new Date()), Toast.LENGTH_SHORT).show();
						if (curPoint != null) {
							updateEstCoordText(curPoint.degree);
							viewFloorplan.addTrack(curPoint.px, curPoint.py, curPoint.pAdjX, curPoint.pAdjY, curPoint.radius, curPoint.degree);
							viewFloorplan.invalidate();
						}
					}});
				}
			}
		}, 0, WifiData.WIFI_SCAN_INTERVAL);
	}
	
	public void stopWifiScan() {
		scanTimer.cancel();
		toggleScan.setChecked(false);
	}
	
	public void OnSensorChange(float degree, float gradient) {
		curDirection = degree > 0 ? degree : 360 + degree;
		updateEstCoordText(curDirection);
		if (toggleRotate.isChecked()) {
			viewFloorplan.setCurDirection(curDirection);
			viewFloorplan.invalidate();
		}
	}
}
