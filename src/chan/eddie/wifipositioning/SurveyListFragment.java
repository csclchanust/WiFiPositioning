package chan.eddie.wifipositioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SurveyListFragment extends Fragment {

	public final static String ITEM_TITLE = "title";
	public final static String ITEM_CAPTION = "caption";
	
	protected WifiData wData;
	protected int sid, gridSize, gridUnit;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		wData = WifiData.getInstance(getActivity());
		gridUnit = wData.getGridUnit();
    	gridSize = wData.getGridSize();
	}
	
	public void setSurvey(int surveyId) {
		sid = surveyId;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d("SurveyListFragment", "onCreateView");
		
		WifiData.Survey s = wData.getSurvey(sid);
		WifiData.Point p = wData.getPoint(s.pid);
		ArrayList<WifiData.Sample> listSample = wData.getSamplesBySurvey(sid);
		
		String sHeader = "P"+p.pid+"("+wData.formatDecimal(p.px*gridUnit/gridSize)+" , "+wData.formatDecimal(p.py*gridUnit/gridSize)+")M";
		String sHeader2 = "S"+s.sid+" "+wData.formatDatetime(s.sts)+" #SID:"+s.cnt_sample;
		List<Map<String, ?>> strListMapSample = new LinkedList<Map<String, ?>>();
		for (int i=0; i<listSample.size(); i++) {
			WifiData.Sample a = listSample.get(i);
			strListMapSample.add(createItem("#"+(i+1)+" SID:"+a.ssid+" RSSI:"+a.rssi,
					"BSSID:"+a.bssid+"\n"+a.desc));
		}
		
		SeparatedListAdapter adapter = new SeparatedListAdapter(getActivity());
		adapter.addSection(sHeader, new ArrayAdapter<String>(getActivity(),
				R.layout.list_item, new String[] {}));
		adapter.addSection(sHeader2, new SimpleAdapter(getActivity(), strListMapSample,
				R.layout.list_complex,
				new String[] { ITEM_TITLE, ITEM_CAPTION }, new int[] {
						R.id.list_complex_title, R.id.list_complex_caption }));
		
		ListView list = new ListView(getActivity());
		list.setAdapter(adapter);
		return list;
	}
    
	public Map<String, ?> createItem(String title, String caption) {
		Map<String, String> item = new HashMap<String, String>();
		item.put(ITEM_TITLE, title);
		item.put(ITEM_CAPTION, caption);
		return item;
	}
}
