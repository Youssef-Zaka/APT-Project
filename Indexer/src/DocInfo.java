import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DocInfo {
	String document;
	int TF;
	List<String> tags;
	int score;
	Set<String> headers;
	public DocInfo(String doc, String tag) {
		// TODO Auto-generated constructor stub
		document = doc;
		TF = 1;
		tags = new ArrayList<String>();
		tags.add(tag);
		score = 0;
		headers = new HashSet<String>(Arrays.asList("h", "h1", "h2", "h3", "h4", "h5","h6","h7", "header"));
		if(tag.toLowerCase() == "title"){
			score += 10;
		}
		else if (headers.contains(tag)){
			score += 3;
		}
		else {
			score += 1;
		}
	}
	
	public DocInfo incrementTF(String tag) {
		tags.add(tag);
		TF += 1;
		if(tag.toLowerCase() == "title"){
			score += 10;
		}
		else if (headers.contains(tag)){
			score += 3;
		}
		else {
			score += 1;
		}
		return this;
	}
	
	@Override
	public String toString() {
		return "(" + document + " "+ score +" " + TF + " " + String.join(" ", tags) +")";
	}
}
