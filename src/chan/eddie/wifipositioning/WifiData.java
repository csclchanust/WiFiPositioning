package chan.eddie.wifipositioning;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.util.Log;

public class WifiData {
	// preference key
	public static final String keyGridUnit = "pref_grid_unit";
	public static final String keyGridSize = "pref_grid_size";

	// default value for floor plan
	public static final int GRID_WIDTH_INIT = 200;
	public static final int GRID_UNIT_INIT = 3;
	public static final int MIN_GRID_UNIT = 1;
	public static final int MIN_GRID_SIZE = 50;
	public static final int MAX_GRID_SIZE = 500;
	// parameters for WiFi scanning
	public static final float WIFI_MAX_RSSI = 100;
	public static final int NUM_WIFI_CATPURE = 3;
	public static final long WIFI_SURVEY_INTERVAL = 5000;
	public static final long WIFI_SCAN_INTERVAL = 2000;
	// parameters for K-Means
	public static final int KMEANS_NUM_PT_POS = 4;
	// parameters for Orientation Filter
	public static final float OF_HALF_ANGLE = 60;
	// parameters for Trust Region
	public static final float TR_INIT_RADIUS = (float) 2.0;
	public static final float TR_MAX_RADIUS = (float) 20.0;
	public static final float TR_MIN_RADIUS = (float) 2.0;
	public static final float TR_LOW_RATIO = (float) 0.3;
	public static final float TR_HIGH_RATIO = (float) 0.95;
	public static final float TR_SHINK_RATIO = (float) 0.7;
	public static final float TR_GROW_RATIO = (float) 1.5;
	public static final float TR_ADJUST_RATIO = (float) 3.0;
	
	// single instance object
	private static WifiData instance = null;
	
	// member variable
	protected Activity activity;
	protected SharedPreferences sp = null;
	protected Bitmap imgFloorplan, imgRedDot, imgGreenDot, imgBlueDot, imgOrangeDot;
	protected int lastPointId = 0;
	protected WifiDBHelper wHelper;
	protected SQLiteDatabase wDB;
	protected DecimalFormat cf;
	protected SimpleDateFormat df;

	public class Point {
		public int pid;
		public float px;
		public float py;
		public int cnt_survey;
	}
	
	public class Survey {
		public int sid;
		public int pid;
		public Date sts;
		public int cnt_sample;
	}
	
	public class Sample {
		public int sid;
		public int pid;
		public String bssid;
		public String ssid;
		public float rssi;
		public int cnt_rssi;
		public float freq;
		public String desc;
	}
	
	public class PositionSurvey {
		public float px;
		public float py;
		public Map<String, Float> mapSample = new HashMap<String, Float>();
	}
	
	public class PositionTemp {
		public float px;
		public float pxScaled;
		public float py;
		public float pyScaled;
		public float pAdjX;
		public float pAdjY;
		public float radius = TR_INIT_RADIUS;
		public float degree;
		public float rssiDiff;
		public float rssiDiffTotal;
	}
	
	protected WifiData(Activity act) {
		activity = act;
		sp = PreferenceManager.getDefaultSharedPreferences(activity);
		wHelper = new WifiDBHelper(act);
		wDB = wHelper.getWritableDatabase();
        cf = new DecimalFormat("#.#");
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}
	
	public static WifiData getInstance(Activity act) {
		if (instance == null)
			instance = new WifiData(act);
		return instance;
	}
	
	public int getGridUnit() {
		return sp.getInt(keyGridUnit, GRID_UNIT_INIT);
	}
	
	public int getGridSize() {
		return sp.getInt(keyGridSize, GRID_WIDTH_INIT);
	}
	
	public void setGrid(int size, int unit) {
		SharedPreferences.Editor ed = sp.edit();
		ed.putInt(keyGridUnit, unit);
		ed.putInt(keyGridSize, size);
		ed.commit();
	}
	
	public Bitmap getFloorplanImage() {
		if (imgFloorplan == null)
			imgFloorplan = ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.floorplan)).getBitmap();
		return imgFloorplan;
	}
	
	public Bitmap getImgRedDot() {
		if (imgRedDot == null)
			imgRedDot = ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.reddot)).getBitmap();
		return imgRedDot;
	}

	public Bitmap getImgBlueDot() {
		if (imgBlueDot == null)
			imgBlueDot = ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.bluedot)).getBitmap();
		return imgBlueDot;
	}
	
	public Bitmap getImgGreenDot() {
		if (imgGreenDot == null)
			imgGreenDot = ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.greendot)).getBitmap();
		return imgGreenDot;
	}
	
	public Bitmap getImgOrangeDot() {
		if (imgOrangeDot == null)
			imgOrangeDot = ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.orangedot)).getBitmap();
		return imgOrangeDot;
	}
	
	public String formatDecimal(float f) {
		return cf.format(f);
	}
	
	public String formatDatetime(Date d) {
		return df.format(d);
	}

	// SQLite functions / objects
	public Point newPointObj() {
		return new Point();
	}
	
	public Survey newSurveyObj() {
		return new Survey();
	}
	
	public Sample newSampleObj() {
		return new Sample();
	}
	
	public PositionTemp newPositionTemp() {
		return new PositionTemp();
	}

	public int insertPoint(float px, float py) {
		ContentValues values = new ContentValues();
        values.put("px", px);
        values.put("py", py);
        return (int)wDB.insert(WifiDBHelper.TABLE_POINT, null, values);
	}
	
	public ArrayList<Point> getPoints() {
		String sqlCnt = "SELECT COUNT(*) FROM " + WifiDBHelper.TABLE_SURVEY
				+ " s WHERE s.pid=p.pid";
		Cursor cursor = wDB.rawQuery("SELECT pid,px,py,(" + sqlCnt
				+ ") AS cnt_survey FROM " + WifiDBHelper.TABLE_POINT + " p",
				null);
		Log.d("WifiData", "getPoints # of points="+cursor.getCount());
		cursor.moveToFirst();
		ArrayList<Point> pointList = new ArrayList<Point>();
		while (!cursor.isAfterLast()) {
			Point p = new Point();
			p.pid = cursor.getInt(0);
			p.px = cursor.getFloat(1);
			p.py = cursor.getFloat(2);
			p.cnt_survey = cursor.getInt(3);
			Log.d("WifiData", "getPoints ID="+p.pid+", cnt="+p.cnt_survey);
			
			pointList.add(p);
			cursor.moveToNext();
		}
		cursor.close();
		return pointList;
	}
	
	public Point getPoint(int pid) {
		String sqlCnt = "SELECT COUNT(*) FROM " + WifiDBHelper.TABLE_SURVEY
				+ " s WHERE s.pid=p.pid";
		Cursor cursor = wDB.rawQuery("SELECT pid,px,py,(" + sqlCnt
				+ ") AS cnt_survey FROM " + WifiDBHelper.TABLE_POINT + " p WHERE p.pid="+pid,
				null);
		cursor.moveToFirst();
		Point p = new Point();
		while (!cursor.isAfterLast()) {
			p.pid = cursor.getInt(0);
			p.px = cursor.getFloat(1);
			p.py = cursor.getFloat(2);
			p.cnt_survey = cursor.getInt(3);
			cursor.moveToNext();
		}
		cursor.close();
		return p;
	}
	
	public void removePoint(int pid) {
		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_SAMPLE+" WHERE pid="+pid);
		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_SURVEY+" WHERE pid="+pid);
		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_POINT+" WHERE pid="+pid);
	}
	
	public int insertSurvey(int pid, ArrayList<Sample> listSample) {
        int sid = 0;
        
		ContentValues values = new ContentValues();
        values.put("pid", pid);
        values.put("sts", formatDatetime(new Date()));
        sid = (int) wDB.insert(WifiDBHelper.TABLE_SURVEY, null, values);
        
        if (sid != -1) {
    		for (Sample s : listSample) {
    			values = new ContentValues();
    			values.put("pid", pid);
    			values.put("sid", sid);
    			values.put("bssid", s.bssid);
    			values.put("ssid", s.ssid);
    			values.put("rssi", s.rssi);
    			values.put("freq", s.freq);
    			values.put("desc", s.desc);
    			if (wDB.insert(WifiDBHelper.TABLE_SAMPLE, null, values) == -1)
    				Log.d("WifiData", "insertSurvey: fail to insert sample pid="+pid+" sid="+sid);
    		}
        }
		return sid;
	}
	
	public Survey getSurvey(int sid) {
		String sqlCnt = "SELECT COUNT(*) FROM " + WifiDBHelper.TABLE_SAMPLE
				+ " a WHERE a.sid=s.sid";
		Cursor cursor = wDB.rawQuery("SELECT pid,sts,(" + sqlCnt
				+ ") AS cnt_sample FROM " + WifiDBHelper.TABLE_SURVEY + " s WHERE s.sid="+sid,
				null);
		cursor.moveToFirst();
		Survey s = new Survey();
		while (!cursor.isAfterLast()) {
			s.pid = cursor.getInt(0);
			s.sid = sid;
			s.sts = Timestamp.valueOf(cursor.getString(1));
			s.cnt_sample = cursor.getInt(2);
			cursor.moveToNext();
		}
		cursor.close();
		return s;
	}
	
	public ArrayList<Survey> getSurveysByPoint(int pid) {
		String sqlCnt = "SELECT COUNT(*) FROM "+WifiDBHelper.TABLE_SAMPLE+" a WHERE a.sid=s.sid";
		Cursor cursor = wDB.rawQuery("SELECT pid,sid,sts,(" + sqlCnt
				+ ") AS cnt_sample FROM " + WifiDBHelper.TABLE_SURVEY + " s WHERE s.pid="+pid,
				null);
		Log.d("WifiData", "getSurveysByPoint pid="+pid+" # of survey="+cursor.getCount());
		
		ArrayList<Survey> surveyList = new ArrayList<Survey>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Survey s = new Survey();
			s.pid = cursor.getInt(0);
			s.sid = cursor.getInt(1);
			s.sts = Timestamp.valueOf(cursor.getString(2));
			s.cnt_sample = cursor.getInt(3);
			surveyList.add(s);
			cursor.moveToNext();
		}
		cursor.close();
		return surveyList;
	}
	
	public void removeSurvey(int sid) {
		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_SAMPLE+" WHERE sid="+sid);
		wDB.execSQL("DELETE FROM "+WifiDBHelper.TABLE_SURVEY+" WHERE sid="+sid);
	}
	
	public ArrayList<Sample> getSamplesBySurvey(int sid) {
		Cursor cursor = wDB.rawQuery("SELECT pid,bssid,ssid,rssi,freq,desc FROM "+WifiDBHelper.TABLE_SAMPLE+
				" WHERE sid="+sid, null);
		Log.d("WifiData", "getSampleBySurvey sid="+sid+" # of sample="+cursor.getCount());
		
		ArrayList<Sample> sampleList = new ArrayList<Sample>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Sample s = new Sample();
			s.sid = sid;
			s.pid = cursor.getInt(0);
			s.bssid = cursor.getString(1);
			s.ssid = cursor.getString(2);
			s.rssi = cursor.getFloat(3);
			s.freq = cursor.getInt(4);
			s.desc = cursor.getString(5);
			sampleList.add(s);
			cursor.moveToNext();
		}
		cursor.close();
		return sampleList;
	}

	public ArrayList<PositionSurvey> getPositionSurveys() {
		ArrayList<PositionSurvey> list = new ArrayList<PositionSurvey>();
		for (Point p : getPoints()) {
			for (Survey s : getSurveysByPoint(p.pid)) {
				PositionSurvey ps = new PositionSurvey();
				ps.px = p.px;
				ps.py = p.py;
				for (Sample a : getSamplesBySurvey(s.sid))
					ps.mapSample.put(a.bssid, WIFI_MAX_RSSI + a.rssi);
				list.add(ps);
			}
		}
		return list;
	}
}
