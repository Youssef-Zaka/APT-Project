import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DocInfo {
	String document;
	int TF;
	List<WordLocation> tags;
	Double score;
	Set<String> headers;
	public DocInfo(String doc, String tag, String n, int i, Double pageRank) {
		// TODO Auto-generated constructor stub
		document = doc;
		TF = 1;
		tags = new ArrayList<WordLocation>();
		tags.add(new WordLocation(tag, n, i));
		score = pageRank != null ? pageRank * 5 : 0.0;
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
	
	public DocInfo(List<String> valuesArray) {
		// TODO Auto-generated constructor stub
		List<String> initial = Arrays.asList(valuesArray.get(0).split(" "));
		if(initial.size() > 3) {
			initial = initial.subList(1, 4);
		}
		document = initial.get(0);
		score = Double.parseDouble(initial.get(1));
		TF = Integer.parseInt(initial.get(2));
		
		tags = new ArrayList<WordLocation>();
		List<String> docInfo;
		for (int i = 1; i < valuesArray.size(); i++) {
			docInfo = Arrays.asList(valuesArray.get(i).split(" "));
			tags.add(new WordLocation(docInfo.get(0), docInfo.get(1), Integer.parseInt(docInfo.get(2))));
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
