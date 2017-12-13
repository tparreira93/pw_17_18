package tutorials.clustering;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Jaccard {
	
	static List<String> s1 = new LinkedList<String>(Arrays.asList("teste","documents"));
    static List<String> s2 = new LinkedList<String>(Arrays.asList("teste2","documents"));
    
 
	public static float similarity(List<String> s1, List<String> s2){
			float intersections = 0;
		
		   if (s1 == null) {
	            throw new NullPointerException("s1 must not be null");
	        }

	        if (s2 == null) {
	            throw new NullPointerException("s2 must not be null");
	        }
	     
	        Set<String> shingles = new HashSet<String>();
	        shingles.addAll(s1);
	        shingles.addAll(s2);
	        
	        
	        for (String t : s1) {
	            if(s2.contains(t)) {
	               intersections++;
	            }
	        }
	        
	        return intersections/shingles.size();
	       
	}
	
	
	public static float distance(List<String> s1, List<String> s2){
	
		return 1 - similarity(s1, s2);
	}
	
	public static void main(String[] args) {
		
		System.out.println(distance(s1,s2));
			
	}
}
