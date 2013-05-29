package RoadPattern;

import java.io.*;
import java.util.*;
import Objects.*;

public class RoadPatternGeneration {

	/**
	 * @param args
	 */
	static String street = "FIGUEROA";
	static String root = "/Users/Sun/Documents/workspace/CleanPath/GeneratedFile";
	static HashMap<String, PatternPairInfo> patternMap = new HashMap<String, PatternPairInfo>();
	static ArrayList<LinkInfo> linkList = new ArrayList<LinkInfo>();
	static ArrayList<LinkInfo> searchList = new ArrayList<LinkInfo>();

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		readFileToMemory();

		searchStreet(street);

		createPattern();
	}

	private static void createPattern() {
		FileWriter fstream = null;
		BufferedWriter out = null;
		try {
			System.out.println("create pattern for " + street + " start");
			fstream = new FileWriter("Pattern_" + street + ".txt");
			out = new BufferedWriter(fstream);
			int sum = searchList.size();
			for(int i = 0; i < sum; i++) {
				LinkInfo link = searchList.get(i);
				String patternId = link.getStart_node() + link.getEnd_node();
				PatternPairInfo patternInfo = patternMap.get(patternId);
				String link_id = link.getPureLinkId();
				String st_node = link.getStart_node();
				String ed_node = link.getEnd_node();
				System.out.println("Link_ID:" + link_id + " St_Node:" + st_node + " Ed_Node:" + ed_node);
				out.write("Link_ID:" + link_id + " St_Node:" + st_node + " Ed_Node:" + ed_node + "\n");
				double distance = DistanceCalculator.CalculationByDistance(link.getNodes()[0], link.getNodes()[1]);
				int[] intervals = patternInfo.getIntervals();
				for(int j = 0; j < 60 && j < intervals.length; j++) {
					double speed = (double)Math.round(distance / intervals[j] * 1000000 * 100) / 100;
					System.out.print(UtilClass.getStartTime(j) + "-" + UtilClass.getEndTime(j) + 
						":" + speed + ", ");
					out.write(UtilClass.getStartTime(j) + "-" + UtilClass.getEndTime(j) + 
						":" + speed + ", ");
				}
				System.out.println();
				out.write("\n");
			}
			
			out.close();
			fstream.close();
			System.out.println("create pattern for " + street + " finished!");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void searchStreet(String street) {
		System.out.println("search " + street + " start");
		int sum = linkList.size();
		for(int i = 0; i < sum; i++) {
			LinkInfo link = linkList.get(i);
			if(link.getSt_name().contains(street)) {
				// if(link.getLinkId().contains("24009477"))
					// System.out.println(link.toString());
				searchList.add(link);
			}
			if(i % 10000 == 0)
				System.out.println((double)i / sum * 100 + "%");
		}
		System.out.println("search " + street + " finished! find " + searchList.size() + " links");
	}

	private static void readFileToMemory() {
		FileInputStream fstream = null;
		DataInputStream in = null;
		BufferedReader br = null;
		
		try {
			System.out.println("read from AdjList_Monday.txt start");
			fstream = new FileInputStream(root + "/AdjList_Monday.txt");
			in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in));
			int lineNum = 0;
			String strLine;
			while ((strLine = br.readLine()) != null) {
				if (strLine.equals("NA")) {
					lineNum++;
					continue;
				}
				String[] nodes = strLine.split(";");
				for (int i = 0; i < nodes.length; i++) {
					// info[0]: n1(V)	info[1]: 7931,7931,7931,7931......
					String[] info = nodes[i].split(":");
					// info2[0]: n1		info2[1]: V)
					String[] info2 = info[0].split("\\(");
					int node1 = lineNum;
					int node2 = Integer.parseInt(info2[0].substring(1));
					int[] intervals = new int[60];
					// String flag = info2[1].substring(0, 1);
					// if(flag == "V") { } else { }
					String[] intervalS = info[1].split(",");
					int[] interval = new int[60];
					int intervalNum = 0;
					while(intervalNum < 60 && intervalNum < intervalS.length) {
						interval[intervalNum] = Integer.parseInt(intervalS[intervalNum]);
						intervalNum++;
					}
					PatternPairInfo pattern = new PatternPairInfo(node1, node2, interval);
					String patternId = "n" + node1 + "n" + node2;
					patternMap.put(patternId, pattern);
				}
				lineNum++;
				if(lineNum % 10000 == 0)
					System.out.print("read line " + lineNum + "\r");
			}
			br.close();
			in.close();
			fstream.close();
			System.out.println("read " + lineNum + " lines from AdjList_Monday.txt finished!");
			
			System.out.println("read from Edges.csv start");
			fstream = new FileInputStream(root + "/Edges.csv");
			in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in));
			lineNum = 0;
			while ((strLine = br.readLine()) != null) {
				String[] nodes = strLine.split(",");
				String link_id = nodes[0];
				int func_class = Integer.parseInt(nodes[1]);
				String st_name = nodes[2];
				String st_node = nodes[3];
				String ed_node = nodes[4];
				String linkIndexId = link_id + st_node.substring(1) + ed_node.substring(1);
				PairInfo[] pairs = new PairInfo[2];
				pairs[0] = new PairInfo(Double.parseDouble(nodes[5]), Double.parseDouble(nodes[6]));
				pairs[1] = new PairInfo(Double.parseDouble(nodes[7]), Double.parseDouble(nodes[8]));
				LinkInfo link = new LinkInfo(linkIndexId, link_id, func_class, st_name, st_node, ed_node, pairs, 2);
				linkList.add(link);
				
				lineNum++;
				if(lineNum % 10000 == 0)
					System.out.print("read line " + lineNum + "\r");
			}
			br.close();
			in.close();
			fstream.close();
			System.out.println("read " + lineNum + " lines from Edges.csv finished!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}