import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 	Author Wenxuan Wang
 * 	Date 2/12/ 2017
 */

public class Apriori {
	private static long timer;
	private static int MIN_SUPPORT;

	private static List<List<Integer>> buffer = new ArrayList<List<Integer>>();
	private static List<Map<String, Candidate>> list = new ArrayList<>();
	private static ConcurrentHashMap<String, Candidate> singleItem = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, Candidate> twoItem = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, Candidate> threeItem = new ConcurrentHashMap<>();
	
	public static void main(String[] args) {
		String inputFile = args[0];
		String outputFile = args[2];
		MIN_SUPPORT = Integer.parseInt(args[1]);
		
		try{
			PrintStream terminal = System.out;
			PrintStream redirect = new PrintStream(new FileOutputStream(outputFile));
			System.setOut(redirect);
			timer = new Date().getTime();
			AprioriPruning(inputFile);
			System.setOut(terminal);
			System.out.printf("t = %f", (new Date().getTime()-timer) / 1000.0);
			outputHandler(outputFile, terminal);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

	}

	public static void outputHandler(String inputFile, PrintStream terminal) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		List<String> outputBuffer = new ArrayList<String>();

		String line;
		while((line = br.readLine()) != null)
			outputBuffer.add(line);

		sortOutput(outputBuffer);

		PrintStream redirect = new PrintStream(new FileOutputStream(inputFile));
		System.setOut(redirect);
		for(int i = 0; i < outputBuffer.size(); i++) {
			System.out.println(outputBuffer.get(i));
		}
		System.setOut(terminal);
	}

	public static void sortOutput(List<String> outputBuffer) throws Exception{
		Collections.sort(outputBuffer, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				List<Integer> list1 = parseLine(s1);
				List<Integer> list2 = parseLine(s2);
				int m = 0, n = 0;
				int val1 , val2 ;
				while(m < list1.size() || n < list2.size()) {
					val1 = m < list1.size() ? list1.get(m) : 0;
					val2 = n < list2.size() ? list2.get(n) : 0;
					if(val1 != val2)
						return val1 - val2;
					m++;
					n++;
				}
				return 0;
			}
		});
	}

	public static List<Integer> parseLine(String line) {
		line = line.trim().substring(0, line.lastIndexOf(" "));
		List<Integer> list = new ArrayList<Integer>();
		for(String s : line.split(" "))
			list.add(Integer.parseInt(s));
		return list;
	}

	public static void AprioriPruning(String inputFile) throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		String line;
		while((line = br.readLine()) != null) {
			List<Integer> temp = new ArrayList<Integer>();
			for(String s : line.trim().split(" "))
				temp.add(Integer.parseInt(s));
			generateSingle(temp);
			generateTuple(temp);
			//generateTriple(temp);
			buffer.add(temp);
		}
		list.add(clean(singleItem));
		list.add(clean(twoItem));
		//list.add(clean(threeItem));
		//result.add(clean(threeItem));

		Map<String, Candidate> nextSet = getCandidateSet(list.get(list.size() - 1));
		list.add(nextSet);
		while(nextSet.size() > 0) {
			for(List<Integer> entry : buffer) {
				Set<Integer> set = new HashSet<Integer>();
				for(Integer n : entry)
					set.add(n);
				for(Map.Entry<String, Candidate> m : nextSet.entrySet()) {
					boolean contain = true;
					int[] itemList = m.getValue().set;
					for(int i : itemList) {
						if(!set.contains(i))
							contain = false;
					}
					if(contain)	m.getValue().support++;
				}
			}
			clean(nextSet);
			nextSet = getCandidateSet(list.get(list.size() - 1));
			list.add(nextSet);
		}
	}

	public static Map<String, Candidate> clean(Map<String, Candidate> map) {
		for(Map.Entry<String, Candidate> e : map.entrySet() ) {
			if(e.getValue().support < MIN_SUPPORT)
				map.remove(e.getKey());
			else {
				for(int i : e.getValue().set)
					System.out.printf("%d ",i);
				System.out.printf("(%d)\n",e.getValue().support);
			}
		}
		return map;
	}

	public static Map<String,Candidate> getCandidateSet(Map<String, Candidate> lastSet) {
		ConcurrentHashMap<String, Candidate> nextSet = new ConcurrentHashMap<>();
		boolean valid;
		for(Candidate c1 : lastSet.values()) {
			int[] item1 = c1.set;
			for(Candidate c2 : lastSet.values()) {
				if(c1 == c2)	continue;
				int[] item2 = c2.set;
				valid = true;

				for(int i = 0; i < item2.length - 1; i++) {
					if(item1[i] != item2[i]){
						valid = false;
						break;
					}
				}
				if(valid) {
					boolean isNext = true;
					int[] nextCandidate = new int[item1.length + 1];
					int k = 0;
					while(k < item1.length) {
						nextCandidate[k] = item1[k++];
					}
					nextCandidate[k] = item2[item2.length - 1];
					Arrays.sort(nextCandidate);

					List<List<Integer>> subset = getSubsets(nextCandidate);
					for (int i = 0; i < subset.size(); i++) {
						int[] tempoararyList = subset.get(i).stream().mapToInt(x -> x).toArray();
						if (!lastSet.containsKey(Arrays.toString(tempoararyList)))
							isNext = false;
					}
					if (isNext)
						nextSet.put(Arrays.toString(nextCandidate), new Candidate(0, nextCandidate));
				}
			}
		}
		return nextSet;
	}

	public static List<List<Integer>> getSubsets(int[] item) {
		List<List<Integer>> list = new ArrayList<List<Integer>>();
		helper(list, new ArrayList<>(), item, item.length - 1,0);
		return list;
	}

	public static void helper(List<List<Integer>> list, List<Integer> temp, int[] item, int length, int start) {
		if(temp.size() == length)	list.add(new ArrayList<Integer>(temp));
		else {
			for(int i = start; i < item.length; i++) {
				temp.add(item[i]);
				helper(list,temp,item,length,i+1);
				temp.remove(temp.size() - 1);
			}
		}
	}

	public static void buildMap(Map<String, Candidate> map, int[] set) {
		String key = Arrays.toString(set);
		if(!map.containsKey(key))
			map.put(key, new Candidate(1, set));
		else
			map.get(key).support++;
	}

	public static void generateSingle(List<Integer> temp) {
		for(int i = 0; i < temp.size(); i++)
			buildMap(singleItem, new int[]{temp.get(i)});
	}
	public static void generateTuple(List<Integer> temp) {
		for(int i = 0; i < temp.size() - 1; i++) {
			for(int j = i+1; j < temp.size(); j++) {
				buildMap(twoItem, new int[]{temp.get(i), temp.get(j)});
			}
		}
	}
	public static void generateTriple(List<Integer> temp) {
		int[] arr = temp.stream().mapToInt(x -> x).toArray();
		tripleHelper(new ArrayList<>(), arr, 3, 0);
	}

	public static void tripleHelper(List<Integer> temp, int[] item, int length, int start) {
		if(temp.size() == length)	buildMap(threeItem, temp.stream().mapToInt(x->x).toArray());
		else {
			for(int i = start; i < item.length; i++) {
				temp.add(item[i]);
				tripleHelper(temp,item,length,i+1);
				temp.remove(temp.size() - 1);
			}
		}
	}
}

class Candidate {
	int support;
	int[] set;

	Candidate(int support, int[] set) {
		this.support = support;
		this.set = set;
	}
}