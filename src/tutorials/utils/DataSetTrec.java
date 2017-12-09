package tutorials.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataSetTrec {

	private String filter;
	private float map;
	private Map<String, Float> pages = new LinkedHashMap<String, Float>();
	private Map<String, Float> recalls = new LinkedHashMap<String, Float>();

	public DataSetTrec(String filter) {
		this.filter = filter;
	}

	public void setMap(float map) {
		this.map = map;
	}

	public String getFilter() {
		return filter;
	}

	public float getMap() {
		return map;
	}

	public Map<String, Float> getPages() {
		return pages;
	}

	public Map<String, Float> getRecalls() {
		return recalls;
	}

	public void addPage(String k, float v) {
		pages.put(k, v);
	}

	public void addRecall(String k, float v) {
		recalls.put(k, v);
	}

}
