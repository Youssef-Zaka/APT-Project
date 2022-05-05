import java.util.ArrayList;
import java.util.List;

public class DocInfo {
	String document;
	int TF;
	List<String> tags;
	
	public DocInfo(String doc, String tag) {
		// TODO Auto-generated constructor stub
		document = doc;
		TF = 1;
		tags = new ArrayList<String>();
		tags.add(tag);
	}
	
	public DocInfo incrementTF(String tag) {
		tags.add(tag);
		TF += 1;
		return this;
	}
	
	@Override
	public String toString() {
		return "(" + document + " " + TF + " " + String.join(" ", tags) +")";
	}
}
