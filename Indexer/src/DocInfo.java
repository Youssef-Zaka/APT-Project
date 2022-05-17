import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DocInfo {
	String document;
	int TF;
	List<WordLocation> tags;
	int score;
	Set<String> headers;
	public DocInfo(String doc, String tag, String n, int i) {
		// TODO Auto-generated constructor stub
		document = doc;
		TF = 1;
		tags = new ArrayList<WordLocation>();
		tags.add(new WordLocation(tag, n, i));
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
	
	public DocInfo incrementTF(String tag, String n, int i) {
		tags.add(new WordLocation(tag, n, i));
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
		return document + " "+ score +" " + TF + " " + tags.toString().replaceAll("[\\[\\]]", "").replaceAll(",", " ") + "|";
	}
}

class WordLocation{
	public String tag;
	public String unstemmed;
	public int index;
	
	public WordLocation(String t, String n, int i) {
		tag = t;
		unstemmed = n;
		index = i;
	}

	@Override
	public String toString() {
		return "|" + tag + " " + unstemmed + " " + index;
	}
	

}
