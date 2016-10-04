package chan.eddie.wifipositioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.FloatMath;
import android.util.Log;

public class WifiScanner {
	// single instance object
	private static WifiScanner instance = null;
	private WifiManager wm;
	protected WifiData wData;
	
	public WifiScanner(Activity act) {
		wm = (WifiManager) act.getSystemService(Context.WIFI_SERVICE);
		wData = WifiData.getInstance(act);
	}
	
	public static WifiScanner getInstance(Activity act) {
		if (instance == null)
			instance = new WifiScanner(act);
		return instance;
	}
	
	public boolean isWifiEnabled() {
		return wm.isWifiEnabled();
	}
	
	public List<ScanResult> scanWifi() {
		if (!wm.startScan())
			return null;
		return wm.getScanResults();
	}
	
	public ArrayList<WifiData.Sample> getMeanSample(ArrayList<List<ScanResult>> scanLists) {
		Map<String, WifiData.Sample> item = new HashMap<String, WifiData.Sample>();
		WifiData.Sample s = null;
		for (List<ScanResult> list : scanLists) {
			for (ScanResult sr : list) {
				s = item.get(sr.BSSID);
				if (s == null) {
					s = wData.newSampleObj();
					s.bssid = sr.BSSID;
					s.ssid = sr.SSID;
					s.rssi = sr.level;
					s.cnt_rssi = 1;
					s.freq = sr.frequency;
					s.desc = sr.capabilities;
					item.put(sr.BSSID, s);
				} else {
					s.rssi += sr.level;
					s.cnt_rssi++;
				}
			}
		}

		// calculate the mean RSSI of each BSSID
		for (Map.Entry<String, WifiData.Sample> entry : item.entrySet()) {
			s = entry.getValue();
			s.rssi = s.rssi / s.cnt_rssi;
		}
		
		return new ArrayList<WifiData.Sample>(item.values());
	}
	
	public void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public WifiData.PositionTemp calcKMeansPoint(ArrayList<WifiData.PositionSurvey> listSurvey, List<ScanResult> listScan) {
		ArrayList<WifiData.PositionTemp> listTemp = new ArrayList<WifiData.PositionTemp>();
		WifiData.PositionTemp pt = null;
		for (WifiData.PositionSurvey ps : listSurvey) {
			for (ScanResult sr : listScan) {
				Float rssi = ps.mapSample.get(sr.BSSID);
				if (rssi != null) {
					// create a temp point if BSSID if found in the survey point
					if (pt == null) {
						pt = wData.newPositionTemp();
						pt.px = ps.px;
						pt.py = ps.py;
					}
					pt.rssiDiffTotal += Math.pow(rssi - (WifiData.WIFI_MAX_RSSI + sr.level), 2);
				}
			}
			
			if (pt != null) {
				// calculate difference by Euclidean distance
				pt.rssiDiff = FloatMath.sqrt(pt.rssiDiffTotal);
				listTemp.add(pt);
				pt = null;
			}
		}
		
		if (listTemp.size() > 0) {
			// sort the temp point list in ascending order
			Collections.sort(listTemp, new Comparator<WifiData.PositionTemp>() {
				public int compare(WifiData.PositionTemp lhs, WifiData.PositionTemp rhs) {
					if (lhs.rssiDiff < rhs.rssiDiff)
						return -1;
					else if (lhs.rssiDiff > rhs.rssiDiff)
						return 1;
					return 0;
				}
			});
			// pick out N smallest distance point and calculate the mean point 
			int numPt = listTemp.size() > WifiData.KMEANS_NUM_PT_POS ? WifiData.KMEANS_NUM_PT_POS : listTemp.size();
			WifiData.PositionTemp p = wData.newPositionTemp();
			for (int i=0; i<numPt; i++) {
				p.px += listTemp.get(i).px;
				p.py += listTemp.get(i).py;
			}
			p.px /= numPt;
			p.py /= numPt;
			p.pAdjX = p.px;
			p.pAdjY = p.py;
			return p;
		}
		Log.d("WifiScanner", "calcKMeansPoint No reference point found for positioning...");
		return null;
	}
	
	public ArrayList<WifiData.PositionSurvey> getOrientationFilteredPositionSurveys(ArrayList<WifiData.PositionSurvey> listSurvey, WifiData.PositionTemp lastPt, float curDegree, float halfAngle) {
		if (lastPt == null)
			return null;
		
		ArrayList<WifiData.PositionSurvey> filteredList = new ArrayList<WifiData.PositionSurvey>();
		double halfRadian = halfAngle / 180.0 * Math.PI;
		double curRadian = (curDegree - 90) / 180.0 * Math.PI;
		for (WifiData.PositionSurvey ps : listSurvey) {
			double compRadian = Math.atan2(ps.py - lastPt.py, ps.px - lastPt.px);
			double diffrandian = Math.abs(curRadian - compRadian);
			Log.d("WifiScanner", "compRadian="+compRadian+", diffrandian="+diffrandian+", halfRadian="+halfRadian);
			// if the angle difference is within our orientation filter angle, add this point to the survey list 
		    if (diffrandian <= halfRadian || (diffrandian >= (2*Math.PI - halfRadian) && diffrandian <= (2*Math.PI + halfRadian)))
		    	filteredList.add(ps);
		}
		return filteredList;
	}
	
	public void calcTrustRegion(WifiData.PositionTemp curPt, WifiData.PositionTemp lastPt) {
		if (curPt == null || lastPt == null)
			return;
		
		// work on the scale of Meter
		curPt.pxScaled = curPt.px * wData.getGridUnit() / wData.getGridSize();
		curPt.pyScaled = curPt.py * wData.getGridUnit() / wData.getGridSize();
		lastPt.pxScaled = lastPt.px * wData.getGridUnit() / wData.getGridSize();
		lastPt.pyScaled = lastPt.py * wData.getGridUnit() / wData.getGridSize();
		
		double distance = Math.sqrt(Math.pow(curPt.pxScaled - lastPt.pxScaled, 2) + Math.pow(curPt.pyScaled - lastPt.pyScaled, 2));
		float distRatio = (float) (distance / lastPt.radius);
		Log.d("WifiScanner", "calcTrustRegion distance="+distance+", ratio="+distRatio);
		
		if (distRatio < WifiData.TR_LOW_RATIO) {
			curPt.radius = (float) (lastPt.radius * WifiData.TR_SHINK_RATIO);
			if (curPt.radius < WifiData.TR_MIN_RADIUS)
				curPt.radius = (float) WifiData.TR_MIN_RADIUS;
		} else if (distRatio >= WifiData.TR_LOW_RATIO && distRatio < WifiData.TR_HIGH_RATIO) {
			// no change on trust region radius
			curPt.radius = lastPt.radius;
		} else {
			curPt.radius = lastPt.radius * WifiData.TR_GROW_RATIO;
			if (curPt.radius > WifiData.TR_MAX_RADIUS)
				curPt.radius = WifiData.TR_MAX_RADIUS;
			if (distRatio > WifiData.TR_ADJUST_RATIO) {
				calcAdjustedIntersectPoint(lastPt, curPt);
			}
		}
	}
	
	// find the intersection point of line and circle
	public void calcAdjustedIntersectPoint(WifiData.PositionTemp p1, WifiData.PositionTemp p2) {
		WifiData.PositionTemp c1 = p1;
		double r = p1.radius * wData.getGridSize() / wData.getGridUnit(); // work on the scale of Meter
	    double a, b, c;
	    double bb4ac;
	    double ix, iy;
	    ix = p2.px - p1.px;
	    iy = p2.py - p1.py;
	    a = ix * ix + iy * iy;
	    b = 2 * (ix * (p1.px - c1.px)+ iy * (p1.py - c1.py));
	    c = c1.px * c1.px + c1.py * c1.py;
	    c += p1.px * p1.px + p1.py * p1.py;
	    c -= 2 * (c1.px * p1.px + c1.py * p1.py);
	    c -= r * r;
	    bb4ac = b * b - 4 * a * c;
	    double mu1 = (-b + Math.sqrt(bb4ac)) / (2 * a);
	    double newX = p1.px + mu1 * (p2.px - p1.px);
	    double newY = p1.py + mu1 * (p2.py - p1.py);
	    p2.px = (float) newX;
	    p2.py = (float) newY;
	    Log.d("WifiScanner", "calcAdjustedIntersectPoint x="+p2.px+",y="+p2.py+" orgX="+p2.pAdjX+",orgY="+p2.pAdjY);
	}
}
