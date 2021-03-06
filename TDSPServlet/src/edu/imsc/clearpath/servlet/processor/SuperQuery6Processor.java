package edu.imsc.clearpath.servlet.processor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletOutputStream;

import library.FibonacciHeap;
import library.FibonacciHeapNode;

import edu.imsc.clearpath.bean.RequestObject;
import Objects.DistanceCalculator;
import Objects.EndingNode;
import Objects.Node;
import Objects.NodeConnectionValues;
import Objects.NodeValues;
import Objects.PairInfo;
import Objects.resultset;

/**
 * @author jiayun
 * 
 * @modified skannan processor for TDSPQuerySuper6.java
 * 
 */
public class SuperQuery6Processor {

	RequestObject request; // bean that contains all the request parameters
	public double[] gpsSt;
	public double[] gpsEnd;
	public static String[] nodesMap;
	public static double[][] nodes;
	public static HashMap<String, String> streets2;
	public static NodeConnectionValues[][] graphTDSP;
	public static HashMap<String, String> exitDataWithLocation;
	public HashMap<Integer, String> lookup;
	public static HashMap<String, Integer> CarpoolLU;
	public static InputStream realtimeTableStream;

	int link_count = 0;
	private static String[] days = { "Monday", "Tuesday", "Wednesday",
			"Thursday", "Friday", "Saturday", "Sunday" };
	private String day;
	private int dayIndex;
	static StringBuffer nodesInfo = new StringBuffer("");
	static ArrayList<String> pathInfo = new ArrayList<String>();
	public static HashMap<String, Integer> accidentdisplay = new HashMap<String, Integer>();
	static public double Radius;
	public static HashMap<String, String> eventLU = new HashMap<String, String>();;
	public static double singleDistance;
	public static int NNcount = 0;
	public int starthitIn = 0;
	public int endhitIn = 0;
	public int starthitOut = 0;
	public int endhitOut = 0;
	private int timeIndex;
	public static HashMap<String, Integer> accidentTable;
	public static HashMap<String, String> streets;
	public static HashMap<Integer, ArrayList<Integer>> reverseList;
	public static HashMap<Integer, ArrayList<Integer>> forwardList;

	static String url_home = "jdbc:oracle:thin:@gd.usc.edu:1521/adms";
	static String userName = "clearp";
	static String password = "clearp";
	static Connection connHome = null;

	// cannot instantiate an object of this class without instance of
	// RequestObject class
	private SuperQuery6Processor() {

	}

	public SuperQuery6Processor(RequestObject pRequest, double[] pGpsSt,
			double[] pGpsEnd, String[] pNodesMap, double[][] pNodes,
			HashMap<String, String> pStreets2,
			NodeConnectionValues[][] pGraphTDSP,
			HashMap<String, String> pExitdataWithLocation,
			HashMap<Integer, String> pLookup,
			HashMap<String, Integer> pCarpoolLU,
			InputStream pRealtimeTableStream) {
//		System.out.println("Inside constructor SuperQuery6Processor");
		request = pRequest;
		gpsSt = pGpsSt;
		gpsEnd = pGpsEnd;
		nodesMap = pNodesMap;
		nodes = pNodes;
		streets2 = pStreets2;
		graphTDSP = pGraphTDSP;
		exitDataWithLocation = pExitdataWithLocation;
		lookup = pLookup;
		CarpoolLU = pCarpoolLU;
		realtimeTableStream = pRealtimeTableStream;
//		System.out.println("Exit constructor SuperQuery6Processor");
	}

	public static int findNN1(double lat, double lon, EndingNode startEnd,
			EndingNode endEnd) {
		try {

			// NNcount++; // indicate startNode or endNode

			// out.print("im inside FindNN1:\n");//////

			int NNID = 1;
			double maxdistance = Double.MIN_VALUE;
			int maxIndex = -1;
			double[][] array = new double[8][2];
			// find the nearest 5 nodes
			for (int i = 0; i < 8; i++) {
				double distance = (lat - nodes[i][0]) * (lat - nodes[i][0])
						+ (lon - nodes[i][1]) * (lon - nodes[i][1]);
				if (distance > maxdistance) {
					maxdistance = distance;
					maxIndex = i;
				}
				array[i][0] = i;
				array[i][1] = distance;
			}

			for (int i = 8; i < nodes.length; i++) {
				double dist = (lat - nodes[i][0]) * (lat - nodes[i][0])
						+ (lon - nodes[i][1]) * (lon - nodes[i][1]);
				if (dist < maxdistance) {
					array[maxIndex][0] = i;
					array[maxIndex][1] = dist;

					// get the new maxIndex and new MaxVlaue
					maxdistance = Double.MIN_VALUE;
					maxIndex = -1;
					for (int k = 0; k < 8; k++) {
						if (array[k][1] > maxdistance) {
							maxdistance = array[k][1];
							maxIndex = k;
						}
					}
				}

			}

			// out.print("found the nearest 5 nodes"); //////

			/*
			 * //use our edgesGrid to get the searching region HashMap<String,
			 * Integer> NEdges = new HashMap<String, Integer>();
			 * 
			 * 
			 * int index1 = getIndex1(lat); int index2 = getIndex2(lon);
			 * 
			 * //out.print("index1: "+index1+"\n");//////
			 * //out.print("index2: "+index2+"\n");//////
			 * 
			 * 
			 * String[] Sarea = edgesGrid[index1][index2].split(","); int
			 * edgenum = Integer.parseInt(Sarea[2]); int curstID = 3; int
			 * curendID = 4;
			 * 
			 * //out.print("Grid Info: "+edgesGrid[index1][index2]+"\n");//////
			 * 
			 * 
			 * 
			 * for(int en = 0; en < edgenum; en++){ int stID =
			 * Integer.parseInt(Sarea[curstID].substring(1)); int endID =
			 * Integer.parseInt(Sarea[curendID].substring(1)); double stlat1 =
			 * nodes[stID][0]; double stlon1 = nodes[stID][1]; double stlat2 =
			 * nodes[endID][0]; double stlon2 = nodes[endID][1];
			 * 
			 * //check match for(int k=0;k<5;k++){
			 * if((stlat1==nodes[(int)array[k
			 * ][0]][0]&&stlon1==nodes[(int)array[k
			 * ][0]][1])||(stlat2==nodes[(int
			 * )array[k][0]][0]&&stlon2==nodes[(int)array[k][0]][1])){
			 * NEdges.put(streets2.get(stlat1+","+stlon1+","+stlat2+","+stlon2),
			 * 1); break; } }
			 * 
			 * curstID+=2; curendID+=2; }
			 */

			// now we have already get the nearest 10 nodes, then lets get the
			// nearest candidate edges to evaluate

			// out.print("size of NEdges: "+NEdges.size()+"\n");//////
			HashMap<String, Integer> NEdges = new HashMap<String, Integer>();

			Set<String> key = streets2.keySet();
			Iterator<String> iter = key.iterator();
			int i = 0;
			/*
			 * while(i<key.size()){ String st = iter.next(); String[] stInfo =
			 * st.split(","); double stlat1 = Double.parseDouble(stInfo[0]);
			 * double stlon1 = Double.parseDouble(stInfo[1]); double stlat2 =
			 * Double.parseDouble(stInfo[2]); double stlon2 =
			 * Double.parseDouble(stInfo[3]);
			 * 
			 * //check match for(int k=0;k<5;k++){
			 * if((stlat1==nodes[(int)array[k
			 * ][0]][0]&&stlon1==nodes[(int)array[k
			 * ][0]][1])||(stlat2==nodes[(int
			 * )array[k][0]][0]&&stlon2==nodes[(int)array[k][0]][1])){
			 * NEdges.put(streets2.get(st), 1); break; } }
			 * 
			 * i++; }
			 */

			// look up the nodesMap to get the Nearest Edges
			for (int k = 0; k < 8; k++) {
				int nodeID = (int) array[k][0];
				String[] mapNodes = nodesMap[nodeID].split(",");
				for (int m = 0; m < mapNodes.length;) {
					int stID = Integer.parseInt(mapNodes[m]);
					int endID = Integer.parseInt(mapNodes[m + 1]);
					NEdges.put(
							streets2.get(nodes[stID][0] + "," + nodes[stID][1]
									+ "," + nodes[endID][0] + ","
									+ nodes[endID][1]), 1);
					m += 2;
				}

			}

			// calculate the perpendicular distance of each nearest edges
			double[] inter = new double[2];
			double mindist = Double.MAX_VALUE;
			String minst = "";
			key = NEdges.keySet();
			iter = key.iterator();
			i = 0;
			int hitflag = 0;
			while (i < key.size()) {
				String st = iter.next();
				String[] stInfo = st.split(",");
				double x0 = Double.parseDouble(stInfo[6]) * 100.;
				double y0 = Double.parseDouble(stInfo[5]) * 100.;
				double x1 = Double.parseDouble(stInfo[8]) * 100.;
				double y1 = Double.parseDouble(stInfo[7]) * 100.;

				double deltax = x1 - x0;
				double deltay = y1 - y0;

				double dirx = deltay;
				double diry = -deltax;

				double xs = lon * 100.;
				double ys = lat * 100.;

				double t = 0.;
				double fenmu = diry * deltax - dirx * deltay;
				if (fenmu == 0)
					continue;
				else
					t = (diry * (xs - x0) + dirx * (y0 - ys)) / fenmu;

				double xt = x0 + t * deltax;
				double yt = y0 + t * deltay;

				double dist = ((xs - xt) * (xs - xt) + (ys - yt) * (ys - yt));
				if (dist < mindist && t >= 0. && t <= 1.) {
					minst = st;
					mindist = dist;
					hitflag = 1;

					// calculating the current intersection point and store it
					// into the minloc
					double x3 = Double.parseDouble(stInfo[6]);
					double y3 = Double.parseDouble(stInfo[5]);
					double x4 = Double.parseDouble(stInfo[8]);
					double y4 = Double.parseDouble(stInfo[7]);

					// inter[0] = x3+t*(x4-x3);
					// inter[1] = y3+t*(y4-y3);
					inter[0] = y3 + t * (y4 - y3);
					inter[1] = x3 + t * (x4 - x3);
				}

				i++;
			}

			if (hitflag == 1) {
				String[] minstInfo = minst.split(",");
				int n1 = Integer.parseInt(minstInfo[3].substring(1));
				int n2 = Integer.parseInt(minstInfo[4].substring(1));

				double dist1 = (lat - nodes[n1][0]) * (lat - nodes[n1][0])
						+ (lon - nodes[n1][1]) * (lon - nodes[n1][1]);
				double dist2 = (lat - nodes[n2][0]) * (lat - nodes[n2][0])
						+ (lon - nodes[n2][1]) * (lon - nodes[n2][1]);

				// check if it is a one way street
				// skannan-change: commented if-else part to avoid the confusion
				// for 3761 S hope to
				// if(minstInfo[9].equals("1")){
				// NNID = Integer.parseInt(minstInfo[4].substring(1));
				// }
				// //check ends
				// else
				NNID = dist1 > dist2 ? n2 : n1;

			}

			else {
				double mindistance = Double.MAX_VALUE;
				int minID = -1;
				for (int k = 0; k < 10; k++) {
					if (array[k][1] < mindistance) {
						mindistance = array[k][1];
						minID = (int) array[k][0];
					}
				}

				NNID = minID;
			}

			// set up the startEnd or endEnd
			// if(NNcount==1){
			startEnd.setLat(inter[0]);
			startEnd.setLon(inter[1]);
			startEnd.setstInfo(minst);
			// }
			// if(NNcount==2){
			// endEnd.setLat(inter[0]);
			// endEnd.setLon(inter[1]);
			// endEnd.setstInfo(minst);

			// NNcount = 0;
			// }

			// add output the nearest roads find

			/*
			 * String filename = ""; String filename1 = "";
			 * 
			 * filename = "H:\\jiayunge\\NearestEdges_St_new2.kml"; filename1 =
			 * "H:\\jiayunge\\NearestEdge_St_new2.kml";
			 * 
			 * FileWriter fstream_out = new FileWriter(filename); BufferedWriter
			 * out_test = new BufferedWriter(fstream_out);
			 * 
			 * out_test.write("<kml><Document>"); i=0; key = NEdges.keySet();
			 * iter = key.iterator(); while(i<key.size()){ String str =
			 * iter.next(); String[] strInfo = str.split(","); out_test.write(
			 * "<Placemark><name>roads</name><description>roads</description><LineString><tessellate>1</tessellate><coordinates>"
			 * +strInfo[6]+","+strInfo[5]+",0 "+strInfo[8]+","+strInfo[7]+
			 * ",0 </coordinates></LineString></Placemark>\n");
			 * 
			 * i++; }
			 * 
			 * out_test.write("</Document></kml>"); out_test.close();
			 * fstream_out.close();
			 * 
			 * fstream_out = new FileWriter(filename1); out_test = new
			 * BufferedWriter(fstream_out); String[] strInfo = minst.split(",");
			 * out_test.write("<kml><Document>"); out_test.write(
			 * "<Placemark><name>roads</name><description>roads</description><LineString><tessellate>1</tessellate><coordinates>"
			 * +strInfo[6]+","+strInfo[5]+",0 "+strInfo[8]+","+strInfo[7]+
			 * ",0 </coordinates></LineString></Placemark>\n");
			 * out_test.write("</Document></kml>"); out_test.close();
			 * fstream_out.close();
			 */
			// add ends

			NEdges.clear();

			return NNID;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static int findNN2(double lat, double lon, EndingNode startEnd,
			EndingNode endEnd) {
		try {

			// NNcount++; // indicate startNode or endNode

			// out.print("im inside findNN2:\n");//////

			int NNID = 1;
			double maxdistance = Double.MIN_VALUE;
			int maxIndex = -1;
			double[][] array = new double[8][2];
			// find the nearest 10 nodes
			for (int i = 0; i < 8; i++) {
				double distance = (lat - nodes[i][0]) * (lat - nodes[i][0])
						+ (lon - nodes[i][1]) * (lon - nodes[i][1]);
				if (distance > maxdistance) {
					maxdistance = distance;
					maxIndex = i;
				}
				array[i][0] = i;
				array[i][1] = distance;
			}

			for (int i = 8; i < nodes.length; i++) {
				double dist = (lat - nodes[i][0]) * (lat - nodes[i][0])
						+ (lon - nodes[i][1]) * (lon - nodes[i][1]);
				if (dist < maxdistance) {
					array[maxIndex][0] = i;
					array[maxIndex][1] = dist;

					// get the new maxIndex and new MaxVlaue
					maxdistance = Double.MIN_VALUE;
					maxIndex = -1;
					for (int k = 0; k < 8; k++) {
						if (array[k][1] > maxdistance) {
							maxdistance = array[k][1];
							maxIndex = k;
						}
					}
				}

			}

			// out.print("found nearest 5 nodes\n");//////

			/*
			 * //find the nearest edges using the edgesGrid HashMap<String,
			 * Integer> NEdges = new HashMap<String, Integer>();
			 * 
			 * 
			 * int index1 = getIndex1(lat); int index2 = getIndex2(lon);
			 * 
			 * //out.print("index1: "+index1+"\n");//////
			 * //out.print("index2: "+index2+"\n");//////
			 * 
			 * String[] Sarea = edgesGrid[index1][index2].split(","); int
			 * edgenum = Integer.parseInt(Sarea[2]); int curstID = 3; int
			 * curendID = 4;
			 * 
			 * //out.print("Gird Info: "+edgesGrid[index1][index2]+"\n");//////
			 * 
			 * 
			 * for(int en = 0; en < edgenum; en++){ int stID =
			 * Integer.parseInt(Sarea[curstID].substring(1)); int endID =
			 * Integer.parseInt(Sarea[curendID].substring(1)); double stlat1 =
			 * nodes[stID][0]; double stlon1 = nodes[stID][1]; double stlat2 =
			 * nodes[endID][0]; double stlon2 = nodes[endID][1];
			 * 
			 * //check match for(int k=0;k<5;k++){
			 * if((stlat1==nodes[(int)array[k
			 * ][0]][0]&&stlon1==nodes[(int)array[k
			 * ][0]][1])||(stlat2==nodes[(int
			 * )array[k][0]][0]&&stlon2==nodes[(int)array[k][0]][1])){
			 * NEdges.put(streets2.get(stlat1+","+stlon1+","+stlat2+","+stlon2),
			 * 1); break; } }
			 * 
			 * curstID+=2; curendID+=2; }
			 */

			// now we have already get the nearest 10 nodes, then lets get the
			// nearest candidate edges to evaluate
			// out.print("Edges Size: "+NEdges.size()+"\n");//////

			HashMap<String, Integer> NEdges = new HashMap<String, Integer>();

			Set<String> key = streets2.keySet();
			Iterator<String> iter = key.iterator();
			int i = 0;
			/*
			 * while(i<key.size()){ String st = iter.next(); String[] stInfo =
			 * st.split(","); double stlat1 = Double.parseDouble(stInfo[0]);
			 * double stlon1 = Double.parseDouble(stInfo[1]); double stlat2 =
			 * Double.parseDouble(stInfo[2]); double stlon2 =
			 * Double.parseDouble(stInfo[3]);
			 * 
			 * //check match for(int k=0;k<5;k++){
			 * if((stlat1==nodes[(int)array[k
			 * ][0]][0]&&stlon1==nodes[(int)array[k
			 * ][0]][1])||(stlat2==nodes[(int
			 * )array[k][0]][0]&&stlon2==nodes[(int)array[k][0]][1])){
			 * NEdges.put(streets2.get(st), 1); break; } }
			 * 
			 * i++; }
			 */

			// look up the nodesMap to get the Nearest Edges
			for (int k = 0; k < 8; k++) {
				int nodeID = (int) array[k][0];
				String[] mapNodes = nodesMap[nodeID].split(",");
				for (int m = 0; m < mapNodes.length;) {
					int stID = Integer.parseInt(mapNodes[m]);
					int endID = Integer.parseInt(mapNodes[m + 1]);
					NEdges.put(
							streets2.get(nodes[stID][0] + "," + nodes[stID][1]
									+ "," + nodes[endID][0] + ","
									+ nodes[endID][1]), 1);
					m += 2;
				}

			}

			// calculate the perpendicular distance of each nearest edges
			double[] inter = new double[2];
			double mindist = Double.MAX_VALUE;
			String minst = "";
			key = NEdges.keySet();
			iter = key.iterator();
			i = 0;
			int hitflag = 0;
			while (i < key.size()) {
				String st = iter.next();
				String[] stInfo = st.split(",");
				double x0 = Double.parseDouble(stInfo[6]) * 100.;
				double y0 = Double.parseDouble(stInfo[5]) * 100.;
				double x1 = Double.parseDouble(stInfo[8]) * 100.;
				double y1 = Double.parseDouble(stInfo[7]) * 100.;

				double deltax = x1 - x0;
				double deltay = y1 - y0;

				double dirx = deltay;
				double diry = -deltax;

				double xs = lon * 100.;
				double ys = lat * 100.;

				double t = 0.;
				double fenmu = diry * deltax - dirx * deltay;
				if (fenmu == 0)
					continue;
				else
					t = (diry * (xs - x0) + dirx * (y0 - ys)) / fenmu;

				double xt = x0 + t * deltax;
				double yt = y0 + t * deltay;

				double dist = ((xs - xt) * (xs - xt) + (ys - yt) * (ys - yt));
				if (dist < mindist && t >= 0. && t <= 1.) {
					minst = st;
					mindist = dist;
					hitflag = 1;

					// calculating the current intersection point and store it
					// into the minloc
					double x3 = Double.parseDouble(stInfo[6]);
					double y3 = Double.parseDouble(stInfo[5]);
					double x4 = Double.parseDouble(stInfo[8]);
					double y4 = Double.parseDouble(stInfo[7]);

					// inter[0] = x3+t*(x4-x3);
					// inter[1] = y3+t*(y4-y3);
					inter[0] = y3 + t * (y4 - y3);
					inter[1] = x3 + t * (x4 - x3);
				}

				i++;
			}

			if (hitflag == 1) {
				String[] minstInfo = minst.split(",");
				int n1 = Integer.parseInt(minstInfo[3].substring(1));
				int n2 = Integer.parseInt(minstInfo[4].substring(1));

				double dist1 = (lat - nodes[n1][0]) * (lat - nodes[n1][0])
						+ (lon - nodes[n1][1]) * (lon - nodes[n1][1]);
				double dist2 = (lat - nodes[n2][0]) * (lat - nodes[n2][0])
						+ (lon - nodes[n2][1]) * (lon - nodes[n2][1]);

				// //check if it is a one way street
				// if(minstInfo[9].equals("1")){
				// NNID = Integer.parseInt(minstInfo[4].substring(1));
				// }
				// check ends
				// else
				NNID = dist1 > dist2 ? n2 : n1;

			}

			else {
				double mindistance = Double.MAX_VALUE;
				int minID = -1;
				for (int k = 0; k < 10; k++) {
					if (array[k][1] < mindistance) {
						mindistance = array[k][1];
						minID = (int) array[k][0];
					}
				}

				NNID = minID;
			}

			// set up the startEnd or endEnd
			// if(NNcount==1){
			// startEnd.setLat(inter[0]);
			// startEnd.setLon(inter[1]);
			// startEnd.setstInfo(minst);
			// }
			// if(NNcount==2){
			endEnd.setLat(inter[0]);
			endEnd.setLon(inter[1]);
			endEnd.setstInfo(minst);

			// NNcount = 0;
			// }

			// add output the nearest roads find
			/*
			 * 
			 * String filename = ""; String filename1 = ""; if(NNcount==1){
			 * filename = "H:\\jiayunge\\NearestEdges_St_new2.kml"; filename1 =
			 * "H:\\jiayunge\\NearestEdge_St_new2.kml"; } else if(NNcount == 0){
			 * filename = "H:\\jiayunge\\NearestEdges_End.kml"; filename1 =
			 * "H:\\jiayunge\\NearestEdge_End.kml";
			 * 
			 * } FileWriter fstream_out = new FileWriter(filename);
			 * BufferedWriter out_test = new BufferedWriter(fstream_out);
			 * 
			 * out_test.write("<kml><Document>"); i=0; key = NEdges.keySet();
			 * iter = key.iterator(); while(i<key.size()){ String str =
			 * iter.next(); String[] strInfo = str.split(","); out_test.write(
			 * "<Placemark><name>roads</name><description>roads</description><LineString><tessellate>1</tessellate><coordinates>"
			 * +strInfo[6]+","+strInfo[5]+",0 "+strInfo[8]+","+strInfo[7]+
			 * ",0 </coordinates></LineString></Placemark>\n");
			 * 
			 * i++; }
			 * 
			 * out_test.write("</Document></kml>"); out_test.close();
			 * fstream_out.close();
			 * 
			 * fstream_out = new FileWriter(filename1); out_test = new
			 * BufferedWriter(fstream_out); String[] strInfo = minst.split(",");
			 * out_test.write("<kml><Document>"); out_test.write(
			 * "<Placemark><name>roads</name><description>roads</description><LineString><tessellate>1</tessellate><coordinates>"
			 * +strInfo[6]+","+strInfo[5]+",0 "+strInfo[8]+","+strInfo[7]+
			 * ",0 </coordinates></LineString></Placemark>\n");
			 * out_test.write("</Document></kml>"); out_test.close();
			 * fstream_out.close();
			 */
			// add ends

			NEdges.clear();

			return NNID;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static double distance(double stlat, double stlon, double endlat,
			double endlon) {
		Radius = 6371 * 0.621371192;
		// double lat1 = StartP.getLati();
		double lat1 = stlat;
		// double lat2 = EndP.getLati();
		double lat2 = endlat;
		// double lon1 = StartP.getLongi();
		double lon1 = stlon;
		// double lon2 = EndP.getLongi();
		double lon2 = endlon;

		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
				* Math.sin(dLon / 2);
		double c = 2 * Math.asin(Math.sqrt(a));
		return Radius * c;
	}

	private int getTime(int arrTime) {
		int minutesTime = (int) (arrTime / 60000.0);
		if (minutesTime >= 0 && minutesTime < 7)
			return 0;
		else if (minutesTime >= 7 && minutesTime < 22)
			return 1;
		else if (minutesTime >= 22 && minutesTime < 37)
			return 2;
		else if (minutesTime >= 37 && minutesTime < 52)
			return 3;
		else
			return 4;

	}

	private String tdspA(int start, int end, int time, EndingNode startEnd,
			EndingNode endEnd) {
//		System.out.println("Inside tdspA");
		String servletOutput = "";
		// System.out.println("TDSP CALLED WITH PARAMETERS:"+start+","+end+","+time+","+maxTime);
		StringBuffer outTemp = new StringBuffer();
		StringBuffer NodesTemp = new StringBuffer();

		FibonacciHeap<Node> priorityQ = new FibonacciHeap<Node>();
		HashMap<Integer, Node> priorityQMap1 = new HashMap<Integer, Node>();
		HashMap<Node, FibonacciHeapNode<Node>> priorityQMap2 = new HashMap<Node, FibonacciHeapNode<Node>>();

		int leftcount = 0;

		int i, len = graphTDSP.length, j, id;
		// System.out.println("len="+len);
		int w, arrTime;
		int[] c = new int[len];
		int[] parent = new int[len];
		// double shortestTime = 8000000;

		int[] nextNode = new int[len];

		double hdistance = distance(nodes[start][0], nodes[start][1],
				nodes[end][0], nodes[end][1]);
		int htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);

		for (i = 0; i < len; i++) {
			nextNode[i] = -1;
			parent[i] = -1;
			if (i == start)
				c[i] = 0 + htime; // starting node
			else
				c[i] = 8000000; // indicating infinity
		}

		Node n, s = new Node(start, c[start], 0); // creating the starting
		// node with nodeId =
		// start and cost = heuristic cost
		// and arrival time =
		// 0
		FibonacciHeapNode<Node> new_node = new FibonacciHeapNode<Node>(s);
		priorityQ.insert(new_node, s.getNodeCost()); // inserting s into the
														// priority queue
		priorityQMap1.put(s.getNodeId(), s);
		priorityQMap2.put(s, new_node);

		//skannan-comment: block that calculates shortest path
		while (!(priorityQ.isEmpty())) { // while Q is not empty

			// System.out.println("Size of queue="+priorityQ.size());
			FibonacciHeapNode<Node> node = priorityQ.min();
			priorityQ.removeMin();
			n = node.getData();

			n = priorityQMap1.get(n.getNodeId());
			priorityQMap1.remove(n.getNodeId());
			priorityQMap2.remove(n);

			int updTime = time;
			id = n.getNodeId();
			if (graphTDSP[id][dayIndex] == null)
				continue;

			if (n.getNodeId() == end) {
				// shortestTime = n.getArrTime();
				break;
			}

			if (n.getNodeId() != start) {
				updTime = time + getTime(n.getArrTime());

				if (updTime > 59)
					updTime = 59;
			}

			if (graphTDSP[id][dayIndex].nodes != null) {
				HashMap<Integer, NodeValues> hmap = graphTDSP[id][dayIndex].nodes;// check
																					// graphTDSP

				Set<Integer> keys = hmap.keySet();
				Iterator<Integer> iter = keys.iterator();
				// System.out.println("size->"+keys.size());
				int i2 = 0;
				while (i2 < keys.size()) {
					int key = iter.next();
					NodeValues val = hmap.get(key);
					arrTime = n.getArrTime();
					if (eventLU.containsKey(id + "," + key + "," + dayIndex
							+ "," + updTime)) {
						// double location0 =
						// Double.parseDouble(eventLU.get(id+","+key+","+dayIndex+","+updTime).split(",")[0]);
						// double location1 =
						// Double.parseDouble(eventLU.get(id+","+key+","+dayIndex+","+updTime).split(",")[1]);
						accidentdisplay.put(
								eventLU.get(id + "," + key + "," + dayIndex
										+ "," + updTime), 1);
					}
					w = val.getValues()[updTime];

					// left turn penalty test
					// int tail_id = 0;
					if (parent[id] != -1) {
						int id0 = parent[id];
						int id1 = id;
						int id2 = key;

						// tail_id = parent[id];
						double tail_st_x = nodes[id0][1] * 100.;
						double tail_st_y = nodes[id0][0] * 100.;
						double tail_end_x = nodes[id1][1] * 100.;
						double tail_end_y = nodes[id1][0] * 100.;

						double A = tail_end_y - tail_st_y;
						double B = -(tail_end_x - tail_st_x);
						double C = (tail_end_x - tail_st_x) * tail_st_y
								- (tail_end_y - tail_st_y) * tail_st_x;

						double x = nodes[id2][1] * 100.;
						double y = nodes[id2][0] * 100.;

						double result = A * x + B * y + C;

						if (result < 0) {

							double x1 = tail_end_x - tail_st_x;
							double y1 = tail_end_y - tail_st_y;
							double x2 = x - tail_end_x;
							double y2 = y - tail_end_y;

							double cosTheta = (x1 * x2 + y1 * y2)
									/ (Math.sqrt(x1 * x1 + y1 * y1) * Math
											.sqrt(x2 * x2 + y2 * y2));

							if (cosTheta < 0.5)
								w = w + 1 * 60000;
						}

					}
					// left turn penalty ends

					// g
					hdistance = distance(nodes[key][0], nodes[key][1],
							nodes[end][0], nodes[end][1]);
					htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);
					// g ends

					// System.out.println("cost="+w);
					if (arrTime + w + htime < c[key]) {
						c[key] = arrTime + w + htime;
						parent[key] = id;
						// it = priorityQ.iterator();

						// System.out.println("Size of queue="+priorityQ.size());

						if (priorityQMap1.containsKey(key)) {
							Node node_ = priorityQMap1.get(key);
							node_.setArrTime(c[key] - htime);
							node_.setNodeCost(c[key]);
							priorityQMap1.put(key, node_);
							// priorityQ.
							priorityQ.decreaseKey(priorityQMap2.get(node_),
									node_.getNodeCost());
						} else {
							Node node2 = new Node(key, c[key], c[key] - htime);
							FibonacciHeapNode<Node> new_node2 = new FibonacciHeapNode<Node>(
									node2);
							priorityQ.insert(new_node2, c[key]); // arrival time
																	// = c[i]
							priorityQMap1.put(key, node2);
							priorityQMap2.put(node2, new_node2);
						}

					}

					i2++;

				}
			}
		}

		int temp;
		temp = end;
		while (temp != -1) {
			if (parent[temp] != -1)
				nextNode[parent[temp]] = temp;
			temp = parent[temp];
		}

		// calculate the total distance
		singleDistance = 0.;
		double[][] pair = new double[2][2];
		int cur = 1;

		if (start == end) {
			// out.println("Your starting node is the same as your ending node.");
			// out.println(""+nodes[start][0]+","+nodes[start][1]+";0");
			servletOutput += "" + nodes[start][0] + "," + nodes[start][1]
					+ ";0";

			nodesInfo.append("" + nodes[nextNode[i]][0] + ","
					+ nodes[nextNode[i]][1] + ";");
			singleDistance = 0.;
			return servletOutput;
		} else {

			// handle special start part, this may cause heading on the wrong
			// direction
			int checki = start;
			int checkj = nextNode[checki];

			String street = startEnd.getstInfo();
			String[] stInfo = street.split(",");
			double lat1 = Double.parseDouble(stInfo[5]);
			double lon1 = Double.parseDouble(stInfo[6]);
			double lat2 = Double.parseDouble(stInfo[7]);
			double lon2 = Double.parseDouble(stInfo[8]);

			if ((nodes[checki][0] == lat1 && nodes[checki][1] == lon1
					&& nodes[checkj][0] == lat2 && nodes[checkj][1] == lon2)
					|| (nodes[checkj][0] == lat1 && nodes[checkj][1] == lon1
							&& nodes[checki][0] == lat2 && nodes[checki][1] == lon2)) {
				starthitIn = 1;
				starthitOut = 0;
			} else {
				starthitIn = 0;
				starthitOut = 1;
			}

			// handle special start part ends

			i = start;
			j = 1;

			if (starthitIn == 1)
				//sk out.print(""+startEnd.getLat()+","+startEnd.getLon()+";");
				servletOutput += "" + startEnd.getLat() + ","
						+ startEnd.getLon() + ";";
			else {
				//sk out.print(""+startEnd.getLat()+","+startEnd.getLon()+";");
				servletOutput += "" + startEnd.getLat() + ","
						+ startEnd.getLon() + ";";
				//sk out.print(""+nodes[i][0]+","+nodes[i][1]+";");
				servletOutput += "" + nodes[i][0] + "," + nodes[i][1] + ";";
			}
			// out.print(""+gpsSt[0]+","+gpsSt[1]+";");
			nodesInfo.append("" + nodes[i][0] + "," + nodes[i][1] + ";");
			pair[0][0] = nodes[i][0];
			pair[0][1] = nodes[i][1];

			while (i != end && nextNode[i] != -1) {
				if (nextNode[i] == end) {
					// out.print(""+gpsEnd[0]+","+gpsEnd[1] + ";");
					checki = i;
					checkj = end;

					street = endEnd.getstInfo();
					stInfo = street.split(",");
					lat1 = Double.parseDouble(stInfo[5]);
					lon1 = Double.parseDouble(stInfo[6]);
					lat2 = Double.parseDouble(stInfo[7]);
					lon2 = Double.parseDouble(stInfo[8]);

					if ((nodes[checki][0] == lat1 && nodes[checki][1] == lon1
							&& nodes[checkj][0] == lat2 && nodes[checkj][1] == lon2)
							|| (nodes[checkj][0] == lat1
									&& nodes[checkj][1] == lon1
									&& nodes[checki][0] == lat2 && nodes[checki][1] == lon2)) {
						endhitIn = 1;
						endhitOut = 0;
					} else {
						endhitIn = 0;
						endhitOut = 1;
					}

					if (endhitIn == 1) {
						// out.print(""+endEnd.getLat()+","+endEnd.getLon() +
						// ";");
						servletOutput += "" + endEnd.getLat() + ","
								+ endEnd.getLon() + ";";
					} else {
						// out.print(""+nodes[ nextNode[i]][0]+","+nodes[
						// nextNode[i]][1] + ";");
						servletOutput += "" + nodes[nextNode[i]][0] + ","
								+ nodes[nextNode[i]][1] + ";";
						// out.print(""+endEnd.getLat()+","+endEnd.getLon() +
						// ";");
						servletOutput += "" + endEnd.getLat() + ","
								+ endEnd.getLon() + ";";
					}
				} else {
					// out.print(""+nodes[ nextNode[i]][0]+","+nodes[
					// nextNode[i]][1] + ";");
					servletOutput += "" + nodes[nextNode[i]][0] + ","
							+ nodes[nextNode[i]][1] + ";";
				}

				nodesInfo.append("" + nodes[nextNode[i]][0] + ","
						+ nodes[nextNode[i]][1] + ";");

				pair[cur][0] = nodes[nextNode[i]][0];
				pair[cur][1] = nodes[nextNode[i]][1];
				singleDistance += DistanceCalculator.CalculationByDistance(
						new PairInfo(pair[0][0], pair[0][1]), new PairInfo(
								pair[1][0], pair[1][1]));
				// cur = 1-cur;
				if (cur == 1)
					cur = 0;
				else
					cur = 1;

				i = nextNode[i];
			}
			// out.print(""+((double)c[end]/60000.0+leftcount));
			servletOutput += "" + ((double) c[end] / 60000.0 + leftcount);
			nodesInfo.append("" + (double) c[end] / 60000.0);

		}
//		System.out.println("output generated for servlet "+servletOutput);
//		System.out.println("Exit tdspA");
		return servletOutput;
	}

	public static int findNN(double lat, double lon) {
		try {

			NNcount++; // indicate startNode or endNode

			int NNID = 1;
			double maxdistance = Double.MIN_VALUE;
			int maxIndex = -1;
			double[][] array = new double[10][2];
			// find the nearest 10 nodes
			for (int i = 0; i < 10; i++) {
				double distance = (lat - nodes[i][0]) * (lat - nodes[i][0])
						+ (lon - nodes[i][1]) * (lon - nodes[i][1]);
				if (distance > maxdistance) {
					maxdistance = distance;
					maxIndex = i;
				}
				array[i][0] = i;
				array[i][1] = distance;
			}

			for (int i = 10; i < nodes.length; i++) {
				double dist = (lat - nodes[i][0]) * (lat - nodes[i][0])
						+ (lon - nodes[i][1]) * (lon - nodes[i][1]);
				if (dist < maxdistance) {
					array[maxIndex][0] = i;
					array[maxIndex][1] = dist;

					// get the new maxIndex and new MaxVlaue
					maxdistance = Double.MIN_VALUE;
					maxIndex = -1;
					for (int k = 0; k < 10; k++) {
						if (array[k][1] > maxdistance) {
							maxdistance = array[k][1];
							maxIndex = k;
						}
					}
				}

			}

			// now we have already get the nearest 10 nodes, then lets get the
			// nearest candidate edges to evaluate
			HashMap<String, Integer> NEdges = new HashMap<String, Integer>();
			Set<String> key = streets2.keySet();
			Iterator<String> iter = key.iterator();
			int i = 0;
			while (i < key.size()) {
				String st = iter.next();
				String[] stInfo = st.split(",");
				double stlat1 = Double.parseDouble(stInfo[0]);
				double stlon1 = Double.parseDouble(stInfo[1]);
				double stlat2 = Double.parseDouble(stInfo[2]);
				double stlon2 = Double.parseDouble(stInfo[3]);

				// check match
				for (int k = 0; k < 10; k++) {
					if ((stlat1 == nodes[(int) array[k][0]][0] && stlon1 == nodes[(int) array[k][0]][1])
							|| (stlat2 == nodes[(int) array[k][0]][0] && stlon2 == nodes[(int) array[k][0]][1])) {
						NEdges.put(streets2.get(st), 1);
						break;
					}
				}

				i++;
			}

			// calculate the perpendicular distance of each nearest edges
			double[] inter = new double[2];
			double mindist = Double.MAX_VALUE;
			String minst = "";
			key = NEdges.keySet();
			iter = key.iterator();
			i = 0;
			int hitflag = 0;
			while (i < key.size()) {
				String st = iter.next();
				String[] stInfo = st.split(",");
				double x0 = Double.parseDouble(stInfo[6]) * 100.;
				double y0 = Double.parseDouble(stInfo[5]) * 100.;
				double x1 = Double.parseDouble(stInfo[8]) * 100.;
				double y1 = Double.parseDouble(stInfo[7]) * 100.;

				double deltax = x1 - x0;
				double deltay = y1 - y0;

				double dirx = deltay;
				double diry = -deltax;

				double xs = lon * 100.;
				double ys = lat * 100.;

				double t = 0.;
				double fenmu = diry * deltax - dirx * deltay;
				if (fenmu == 0)
					continue;
				else
					t = (diry * (xs - x0) + dirx * (y0 - ys)) / fenmu;

				double xt = x0 + t * deltax;
				double yt = y0 + t * deltay;

				double dist = ((xs - xt) * (xs - xt) + (ys - yt) * (ys - yt));
				if (dist < mindist) {
					minst = st;
					mindist = dist;
					hitflag = 1;

					// calculating the current intersection point and store it
					// into the minloc
					double x3 = Double.parseDouble(stInfo[6]);
					double y3 = Double.parseDouble(stInfo[5]);
					double x4 = Double.parseDouble(stInfo[8]);
					double y4 = Double.parseDouble(stInfo[7]);

					// inter[0] = x3+t*(x4-x3);
					// inter[1] = y3+t*(y4-y3);
					inter[0] = y3 + t * (y4 - y3);
					inter[1] = x3 + t * (x4 - x3);
				}

				i++;
			}

			if (hitflag == 1) {
				String[] minstInfo = minst.split(",");
				int n1 = Integer.parseInt(minstInfo[3].substring(1));
				int n2 = Integer.parseInt(minstInfo[4].substring(1));

				double dist1 = (lat - nodes[n1][0]) * (lat - nodes[n1][0])
						+ (lon - nodes[n1][1]) * (lon - nodes[n1][1]);
				double dist2 = (lat - nodes[n2][0]) * (lat - nodes[n2][0])
						+ (lon - nodes[n2][1]) * (lon - nodes[n2][1]);

				NNID = dist1 > dist2 ? n2 : n1;

			}

			else {
				double mindistance = Double.MAX_VALUE;
				int minID = -1;
				for (int k = 0; k < 10; k++) {
					if (array[k][1] < mindistance) {
						mindistance = array[k][1];
						minID = (int) array[k][0];
					}
				}

				NNID = minID;
			}

			/*
			 * //set up the startEnd or endEnd if(NNcount==1){
			 * startEnd.setLat(inter[0]); startEnd.setLon(inter[1]);
			 * startEnd.setstInfo(minst); } if(NNcount==2){
			 * endEnd.setLat(inter[0]); endEnd.setLon(inter[1]);
			 * endEnd.setstInfo(minst);
			 * 
			 * NNcount = 0; }
			 * 
			 * 
			 * 
			 * //add output the nearest roads find
			 * 
			 * 
			 * String filename = ""; String filename1 = ""; if(NNcount==1){
			 * filename = "H:\\jiayunge\\NearestEdges_St_new2.kml"; filename1 =
			 * "H:\\jiayunge\\NearestEdge_St_new2.kml"; } else if(NNcount == 0){
			 * filename = "H:\\jiayunge\\NearestEdges_End.kml"; filename1 =
			 * "H:\\jiayunge\\NearestEdge_End.kml";
			 * 
			 * } FileWriter fstream_out = new FileWriter(filename);
			 * BufferedWriter out = new BufferedWriter(fstream_out);
			 * 
			 * out.write("<kml><Document>"); i=0; key = NEdges.keySet(); iter =
			 * key.iterator(); while(i<key.size()){ String str = iter.next();
			 * String[] strInfo = str.split(","); out.write(
			 * "<Placemark><name>roads</name><description>roads</description><LineString><tessellate>1</tessellate><coordinates>"
			 * +strInfo[6]+","+strInfo[5]+",0 "+strInfo[8]+","+strInfo[7]+
			 * ",0 </coordinates></LineString></Placemark>\n");
			 * 
			 * i++; }
			 * 
			 * out.write("</Document></kml>"); out.close(); fstream_out.close();
			 * 
			 * fstream_out = new FileWriter(filename1); out = new
			 * BufferedWriter(fstream_out); String[] strInfo = minst.split(",");
			 * out.write("<kml><Document>"); out.write(
			 * "<Placemark><name>roads</name><description>roads</description><LineString><tessellate>1</tessellate><coordinates>"
			 * +strInfo[6]+","+strInfo[5]+",0 "+strInfo[8]+","+strInfo[7]+
			 * ",0 </coordinates></LineString></Placemark>\n");
			 * out.write("</Document></kml>"); out.close(); fstream_out.close();
			 * 
			 * // add ends
			 */

			NEdges.clear();

			return NNID;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	public void findPathInfo() {
		try {
			String[] nodes = nodesInfo.toString().split(";");

			for (int i = 0; i < nodes.length - 2; i++) {
				String query = nodes[i] + "," + nodes[i + 1];
				// InputStream is =
				// getServletContext().getResourceAsStream("\\WEB-INF\\classes\\TDSP Files1234\\Edges.csv");
				// DataInputStream in2 = new DataInputStream(is);
				// BufferedReader br2 = new BufferedReader(new
				// InputStreamReader(in2));

				// String strLine2;
				// while ((strLine2 = br2.readLine()) != null){
				// String[] nodes2 = strLine2.split(",");
				// String test =
				// nodes2[5]+","+nodes2[6]+","+nodes2[7]+","+nodes2[8];

				// if(query.equals(test)){

				pathInfo.add(streets2.get(query));

				// break;
				// }

				// }

				// br2.close();
				// in2.close();
				// is.close();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void findPathInfo(EndingNode startEnd, EndingNode endEnd) {
//		System.out.println("Inside findPathInfo");
		try {

			// test the variable values inside the findPathInfo
			// findbuf = new StringBuffer("");
			// findbuf.append("starthitIn = "+starthitIn+"\n");
			// findbuf.append("starthitOut = "+starthitOut+"\n");
			// findbuf.append("endhitIn = "+endhitIn+"\n");
			// findbuf.append("endhitOut = "+endhitOut+"\n");
			// findbuf.append("startRoadInfo: "+startEnd.getstInfo()+"\n");
			// findbuf.append("endRoadInfo: "+endEnd.getstInfo()+"\n");
			// test ends

			String[] nodes = nodesInfo.toString().split(";");

			int istart = 0;
			// handle the very beginning
			if (starthitIn == 1) {
				String street = startEnd.getstInfo();
				String[] stInfo = street.split(",");
				stInfo[5] = String.valueOf(startEnd.getLat());
				stInfo[6] = String.valueOf(startEnd.getLon());
				stInfo[7] = nodes[1].split(",")[0];
				stInfo[8] = nodes[1].split(",")[1];

				StringBuffer stbuf = new StringBuffer(stInfo[0]);
				for (int k = 1; k < stInfo.length; k++) {
					stbuf.append("," + stInfo[k]);
				}
				pathInfo.add(stbuf.toString());
				istart = 1;
			} else if (starthitOut == 1) {
				String street = startEnd.getstInfo();
				String[] stInfo = street.split(",");
				stInfo[5] = String.valueOf(startEnd.getLat());
				stInfo[6] = String.valueOf(startEnd.getLon());
				stInfo[7] = nodes[0].split(",")[0];
				stInfo[8] = nodes[0].split(",")[1];

				StringBuffer stbuf = new StringBuffer(stInfo[0]);
				for (int k = 1; k < stInfo.length; k++) {
					stbuf.append("," + stInfo[k]);
				}
				pathInfo.add(stbuf.toString());
				istart = 0;
			}
			// handle the very beginning ends
			int count = 0;

			for (int i = istart; i < nodes.length - 2; i++) {
				String query = nodes[i] + "," + nodes[i + 1];
				// test
				// findbuf.append("queryInfo: "+query+"\n");
				// test ends

				// count++;
				if (i == nodes.length - 3) {
					// findbuf.append("Im inside!!\n");

					if (endhitIn == 1) {

						// findbuf.append("IM inside!!\n");

						String ori = streets2.get(query);

						// findbuf.append("corresponding: "+ori);

						String oriInfo[] = ori.split(",");
						oriInfo[7] = String.valueOf(endEnd.getLat());
						oriInfo[8] = String.valueOf(endEnd.getLon());

						StringBuffer stbuf = new StringBuffer(oriInfo[0]);
						for (int k = 1; k < oriInfo.length; k++) {
							stbuf.append("," + oriInfo[k]);
						}

						// test
						// lastlineIn = ori;
						// lastputIn = stbuf.toString();
						// test ends

						pathInfo.add(stbuf.toString());
					} else if (endhitOut == 1) {
						String st = endEnd.getstInfo();
						String[] stInfo = st.split(",");
						stInfo[5] = nodes[nodes.length - 2].split(",")[0];
						stInfo[6] = nodes[nodes.length - 2].split(",")[1];
						stInfo[7] = String.valueOf(endEnd.getLat());
						stInfo[8] = String.valueOf(endEnd.getLon());

						StringBuffer stbuf = new StringBuffer(stInfo[0]);
						for (int k = 1; k < stInfo.length; k++) {
							stbuf.append("," + stInfo[k]);
						}

						// test
						// lastlineOut = st;
						// lastputOut = stbuf.toString();
						// test ends

						pathInfo.add(streets2.get(query));
						pathInfo.add(stbuf.toString());
					}
				} else {
					String streetsstring = streets2.get(query);
					pathInfo.add(streets2.get(query));
					// findbuf.append("corresponding : "+streets2.get(query)+"\n");
				}
				// break;
				// }

				// }

				// br2.close();
				// in2.close();
				// is.close();

			}
			// findbuf.append("count = "+count+"\n");

		} catch (Exception e) {
			e.printStackTrace();
		}
//		System.out.println("Exit findpathInfo");
	}

	public int getDirection2(double st_lat, double st_lon, double end_lat,
			double end_lon) {
		double latitude1 = st_lat * Math.PI / 180.;// coordinate1.Latitude.ToRadian();
		double latitude2 = end_lat * Math.PI / 180.;// coordinate2.Latitude.ToRadian();
		double longitudeDifference = (end_lon - st_lon) * Math.PI / 180.;// (coordinate2.Longitude
																			// -
																			// coordinate1.Longitude).ToRadian();
		double y = Math.sin(longitudeDifference) * Math.cos(latitude2);
		double x = Math.cos(latitude1) * Math.sin(latitude2)
				- Math.sin(latitude1) * Math.cos(latitude2)
				* Math.cos(longitudeDifference);

		// return Math.atan2(y, x);
		double direction = (Math.atan2(y, x) * 180. / Math.PI + 360) % 360;

		if ((direction >= 0. && direction <= 45.)
				|| (direction > 315. && direction <= 360.))
			return 0; // dir = "N";
		else if (direction > 45. && direction <= 135.)
			return 2;// dir = "E";
		else if (direction > 135. && direction <= 225.)
			return 1;// dir = "S";
		else if (direction > 225. && direction <= 315.)
			return 3;// dir = "W";

		return -1; // wrong case

	}

	public String NameFilter(String ori) {
		try {
			String[] nodes = ori.split(";");
			for (int i = 0; i < nodes.length; i++) { // skannan-comment: i++ is
														// dead code
				if (nodes[i].startsWith("I-"))
					;// skannan-comment: investigate the dead code :P
				return nodes[i];
			}
			return nodes[0];
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// one special case "7B" -> "7 B"
	public String CheckB(String ori) {
		if (ori.length() < 2)
			return ori;
		char[] array = ori.toCharArray();
		for (int i = 0; i < array.length - 1; i++) {
			if (!(array[i] >= '0' && array[i] <= '9'))
				return ori;
		}

		if (array[array.length - 1] == 'B') {
			String str = "";
			for (int i = 0; i < array.length - 1; i++)
				str += array[i];
			return str = str + " " + "B";
		}

		return ori;

	}

	public String CheckSpecial(String ori) {
		if (ori.length() < 3)
			return ori;
		char[] array = ori.toCharArray();
		for (int i = 0; i < array.length - 2; i++) {
			if (!(array[i] >= '0' && array[i] <= '9'))
				return ori;
		}

		String check = ori.substring(ori.length() - 2);

		if (check.equals("TH")) {
			String str = "";
			for (int i = 0; i < array.length - 2; i++)
				str += array[i];
			return str = str + "th";
		}

		else if (check.equals("ST")) {
			String str = "";
			for (int i = 0; i < array.length - 2; i++)
				str += array[i];
			return str = str + "st";
		}

		else if (check.equals("ND")) {
			String str = "";
			for (int i = 0; i < array.length - 2; i++)
				str += array[i];
			return str = str + "nd";
		}

		else if (check.equals("RD")) {
			String str = "";
			for (int i = 0; i < array.length - 2; i++)
				str += array[i];
			return str = str + "rd";
		}

		return ori;
	}

	public String nameModify(String ori) {
		try {
			HashMap<String, String> modifytable = new HashMap<String, String>();
			modifytable.put("LN", "LANE");
			modifytable.put("FWY", "FREEWAY");
			modifytable.put("BLVD", " BOULEVARD");
			modifytable.put("ST", "STREET");
			modifytable.put("PL", "PLACE");

			// modifytable.put("TH", "th");
			// modifytable.put("ND", "nd");
			// modifytable.put("ST", "st");
			// modifytable.put("RD", "rd");

			String[] nodes = ori.split(" ");
			StringBuffer result = new StringBuffer("");
			if (modifytable.containsKey(nodes[0]))
				nodes[0] = modifytable.get(nodes[0]);
			result.append(CheckB(nodes[0]));

			for (int i = 1; i < nodes.length; i++) {
				if (modifytable.containsKey(nodes[i]))
					nodes[i] = modifytable.get(nodes[i]);
				// if((!nodes[i].equals("th"))&&(!nodes[i].equals("nd"))&&(!nodes[i].equals("rd")))
				result.append(" ");
				nodes[i] = CheckSpecial(nodes[i]);
				result.append(CheckB(nodes[i]));

			}

			return result.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean name_match(String st_name_pre, String st_name_o) {

		String[] nodes_pre = st_name_pre.split(";");
		String[] nodes_o = st_name_o.split(";");

		for (int i = 0; i < nodes_o.length; i++)
			for (int j = 0; j < nodes_pre.length; j++) {
				if (nodes_o[i].equals(nodes_pre[j]))
					return true;
			}

		return false;

	}
	
	public boolean is_Integer(String line){
		try{
			Integer.parseInt(line);
//			System.out.println("street with number " +line);
		}catch(NumberFormatException nfe){
			System.out.println("NumberFormatException with street name");
			return false;
		}
		return true;
	}

	public String turn_by_turn_new() {
		
//		System.out.println("Inside turn by turn new");
		String turnbyturnString = "";

		try {

			// out.print("@");
			turnbyturnString = "@";// skannan-change: to debug the output data
			DecimalFormat myformat = new java.text.DecimalFormat("#0.00");
			DecimalFormat myformat2 = new java.text.DecimalFormat("#0");

			String strLine;

			String link_id = "";
			double tail_st_x = 0.;
			double tail_st_y = 0.;
			double tail_end_x = 0.;
			double tail_end_y = 0.;
			String st_name_pre = "";
			int st_func_pre = -1;
			double A = 0.;
			double B = 0.;
			double C = 0.;

			int count = 0;
			int road_count = 0;

			int func_out = -1;
			int func_in = -1;

			double distance = 0.;

			// double initaldegree = initaldegree();

			for (int num = 0; num < pathInfo.size(); num++) {
				strLine = pathInfo.get(num);
				count++;

				String[] nodes = strLine.split(",");
				// String link_id_o =
				// nodes[0]+""+nodes[3].substring(1)+""+nodes[4].substring(1);
				String link_id_o = nodes[0];
				String st_name_o = nodes[2];
				int st_func_o = Integer.parseInt(nodes[1]);
				
				String link_id_change = "";
				
				if (count == 1) {
					tail_st_x = Double.parseDouble(nodes[6]) * 100.;
					tail_st_y = Double.parseDouble(nodes[5]) * 100.;
					tail_end_x = Double.parseDouble(nodes[8]) * 100.;
					tail_end_y = Double.parseDouble(nodes[7]) * 100.;

					link_id = link_id_o;
					st_name_pre = st_name_o;
					st_func_pre = st_func_o;

					distance = DistanceCalculator.CalculationByDistance(
							new PairInfo(Double.parseDouble(nodes[5]), Double
									.parseDouble(nodes[6])),
							new PairInfo(Double.parseDouble(nodes[7]), Double
									.parseDouble(nodes[8])));

					int direction = getDirection2(Double.parseDouble(nodes[5]),
							Double.parseDouble(nodes[6]),
							Double.parseDouble(nodes[7]),
							Double.parseDouble(nodes[8]));
					road_count++;
					String dir = "";
					int tempdegree = 0;
					if (direction == 0) {
						dir = "NORTH";
						tempdegree = 0;
					} else if (direction == 1) {
						dir = "SOUTH";
						tempdegree = 180;
					} else if (direction == 2) {
						dir = "EAST";
						tempdegree = 90;
					} else if (direction == 3) {
						dir = "WEST";
						tempdegree = 270;
					}
					// out.print(dir
					// +","+nameModify(NameFilter(st_name_o))+","+nodes[6]+","+nodes[5]);
					turnbyturnString += dir + ","
							+ nameModify(NameFilter(st_name_o)) + ","
							+ nodes[6] + "," + nodes[5];

					continue;
				}

				if (link_id_o.equals(link_id)) {
					tail_st_x = Double.parseDouble(nodes[6]) * 100.;
					tail_st_y = Double.parseDouble(nodes[5]) * 100.;
					tail_end_x = Double.parseDouble(nodes[8]) * 100.;
					tail_end_y = Double.parseDouble(nodes[7]) * 100.;

					link_id = link_id_o;
					st_name_pre = st_name_o;
					st_func_pre = st_func_o;

					distance += DistanceCalculator.CalculationByDistance(
							new PairInfo(Double.parseDouble(nodes[5]), Double
									.parseDouble(nodes[6])),
							new PairInfo(Double.parseDouble(nodes[7]), Double
									.parseDouble(nodes[8])));
				}

				else if (name_match(st_name_pre, st_name_o)) {// if the path
																// continues in
																// same street
					
					link_id_change = link_id_o;
					// test u-turn
					double x_temp = Double.parseDouble(nodes[8]) * 100.;
					double y_temp = Double.parseDouble(nodes[7]) * 100.;

					double x1_temp = tail_end_x - tail_st_x;
					double y1_temp = tail_end_y - tail_st_y;
					double x2_temp = x_temp - tail_end_x;
					double y2_temp = y_temp - tail_end_y;

					double cosTheta_temp = (x1_temp * x2_temp + y1_temp
							* y2_temp)
							/ (Math.sqrt(x1_temp * x1_temp + y1_temp * y1_temp) * Math
									.sqrt(x2_temp * x2_temp + y2_temp * y2_temp));

					if (cosTheta_temp <= -0.5 && cosTheta_temp >= -1.) {
						// out.print(","+myformat.format(distance)+",");
						turnbyturnString += "," + myformat.format(distance)
								+ ",";

						// out.print("U,"+nameModify(NameFilter(st_name_o))+","+nodes[6]+","+nodes[5]);
						turnbyturnString += "U,"
								+ nameModify(NameFilter(st_name_o)) + ","
								+ nodes[6] + "," + nodes[5];

						distance = DistanceCalculator.CalculationByDistance(
								new PairInfo(Double.parseDouble(nodes[5]),
										Double.parseDouble(nodes[6])),
								new PairInfo(Double.parseDouble(nodes[7]),
										Double.parseDouble(nodes[8])));

						tail_st_x = Double.parseDouble(nodes[6]) * 100.;
						tail_st_y = Double.parseDouble(nodes[5]) * 100.;
						tail_end_x = Double.parseDouble(nodes[8]) * 100.;
						tail_end_y = Double.parseDouble(nodes[7]) * 100.;

						link_id = link_id_o;
						st_name_pre = st_name_o;
						st_func_pre = st_func_o;

					}

					// test u-turn ends
					else {// skannan-comment: continuing straight in a road
						tail_st_x = Double.parseDouble(nodes[6]) * 100.;
						tail_st_y = Double.parseDouble(nodes[5]) * 100.;
						tail_end_x = Double.parseDouble(nodes[8]) * 100.;
						tail_end_y = Double.parseDouble(nodes[7]) * 100.;

						link_id = link_id_o;
						st_name_pre = st_name_o;
						st_func_pre = st_func_o;

						distance += DistanceCalculator.CalculationByDistance(
								new PairInfo(Double.parseDouble(nodes[5]),
										Double.parseDouble(nodes[6])),
								new PairInfo(Double.parseDouble(nodes[7]),
										Double.parseDouble(nodes[8])));
					}
				}

				else {

					if (st_name_o.equals("null")) {// skannan-comment: if
													// exiting or merging into a
													// highway
						func_out = st_func_pre;
						String strLine_null = "";
						String link_id_null = "";
						String st_name_null = "";
						int st_func_null = -1;
						num++;

						double distance_pre = distance;
						distance = DistanceCalculator.CalculationByDistance(
								new PairInfo(Double.parseDouble(nodes[5]),
										Double.parseDouble(nodes[6])),
								new PairInfo(Double.parseDouble(nodes[7]),
										Double.parseDouble(nodes[8])));
						// direction before

						A = tail_end_y - tail_st_y;
						B = -(tail_end_x - tail_st_x);
						C = (tail_end_x - tail_st_x) * tail_st_y
								- (tail_end_y - tail_st_y) * tail_st_x;

						double x_b = Double.parseDouble(nodes[8]) * 100.;
						double y_b = Double.parseDouble(nodes[7]) * 100.;

						double result_b = A * x_b + B * y_b + C;
						String direction_b = "";

						if (result_b == 0)
							direction_b = "F";
						else if (result_b > 0)
							direction_b = "R";
						else
							direction_b = "L";

						double x1 = tail_end_x - tail_st_x;
						double y1 = tail_end_y - tail_st_y;
						double x2 = x_b - tail_end_x;
						double y2 = y_b - tail_end_y;

						double cosTheta_b = (x1 * x2 + y1 * y2)
								/ (Math.sqrt(x1 * x1 + y1 * y1) * Math.sqrt(x2
										* x2 + y2 * y2));
						String direction_before = "";

						if (direction_b.equals("F"))
							direction_before = "F";
						else if (direction_b.equals("L") && cosTheta_b >= 0.86)
							direction_before = "SL";
						else if (direction_b.equals("L") && cosTheta_b < 0.86)
							direction_before = "L";
						else if (direction_b.equals("R") && cosTheta_b >= 0.86)
							direction_before = "SR";
						else
							direction_before = "R";

						// direction before ends

						tail_st_x = Double.parseDouble(nodes[6]) * 100.;
						tail_st_y = Double.parseDouble(nodes[5]) * 100.;
						tail_end_x = Double.parseDouble(nodes[8]) * 100.;
						tail_end_y = Double.parseDouble(nodes[7]) * 100.;

						for (; num < pathInfo.size(); num++) {
							count++;
							strLine_null = pathInfo.get(num);
							String[] nodes_null = strLine_null.split(",");
							link_id_null = nodes_null[0];
							st_name_null = nodes_null[2];
							st_func_null = Integer.parseInt(nodes_null[1]);

							if (!(st_name_null.equals("null"))) 
								break;

							tail_st_x = Double.parseDouble(nodes_null[6]) * 100.;
							tail_st_y = Double.parseDouble(nodes_null[5]) * 100.;
							tail_end_x = Double.parseDouble(nodes_null[8]) * 100.;
							tail_end_y = Double.parseDouble(nodes_null[7]) * 100.;

							link_id = link_id_null;
							st_name_pre = st_name_null;
							st_func_pre = st_func_null;

							distance += DistanceCalculator
									.CalculationByDistance(
											new PairInfo(
													Double.parseDouble(nodes_null[5]),
													Double.parseDouble(nodes_null[6])),
											new PairInfo(
													Double.parseDouble(nodes_null[7]),
													Double.parseDouble(nodes_null[8])));
						}

						String[] nodes_after = strLine_null.split(",");
						// direction after
						A = tail_end_y - tail_st_y;
						B = -(tail_end_x - tail_st_x);
						C = (tail_end_x - tail_st_x) * tail_st_y
								- (tail_end_y - tail_st_y) * tail_st_x;

						double x_a = Double.parseDouble(nodes_after[8]) * 100.;
						double y_a = Double.parseDouble(nodes_after[7]) * 100.;

						double result_a = A * x_a + B * y_a + C;
						String direction_a = "";

						if (result_a == 0)
							direction_a = "F";
						else if (result_a > 0)
							direction_a = "R";
						else
							direction_a = "L";

						x1 = tail_end_x - tail_st_x;
						y1 = tail_end_y - tail_st_y;
						x2 = x_a - tail_end_x;
						y2 = y_a - tail_end_y;

						double cosTheta_a = (x1 * x2 + y1 * y2)
								/ (Math.sqrt(x1 * x1 + y1 * y1) * Math.sqrt(x2
										* x2 + y2 * y2));
						String direction_after = "";

						if (direction_a.equals("F"))
							direction_after = "F";
						else if (direction_a.equals("L") && cosTheta_a >= 0.86)
							direction_after = "SL";
						else if (direction_a.equals("L") && cosTheta_a < 0.86)
							direction_after = "L";
						else if (direction_a.equals("R") && cosTheta_a >= 0.86)
							direction_after = "SR";
						else
							direction_after = "R";

						// direction after ends
						func_in = st_func_null;
						// get out of highway and enter a arterial road
						if ((func_out == 1 || func_out == 2)
								&& (func_in == 3 || func_in == 4 || func_in == 5)) {
							// out.print(","+myformat.format(distance_pre)+",");
							turnbyturnString += ","
									+ myformat.format(distance_pre) + ",";
							road_count++;
							// out.print(road_count+". ");
							// skannan-change: to integrate the exit sign
							String location = link_id; // link id
							String exitName = exitDataWithLocation.get(location
									+ "," + func_out);
							if (null == exitName) {
								exitName = exitDataWithLocation.get(location
										+ "," + func_in);
							}
//							System.out.println("EXIT," +location+","+ link_id_null + "," + exitName);

							// out.print("EXIT,"+nameModify(NameFilter(st_name_null))+";"+exitName+","+nodes[6]+","+nodes[5]+", "+myformat.format(distance)+",");
							turnbyturnString += "EXIT,"
									+ nameModify(NameFilter(st_name_null))
									+ ";" + exitName + "," + nodes[6] + ","
									+ nodes[5] + ", "
									+ myformat.format(distance) + ",";
							road_count++;
							// out.print(road_count+". ");
							// out.print("M,"+nameModify(NameFilter(st_name_null))+","+nodes_after[6]+","+nodes_after[5]);
							turnbyturnString += "M,"
									+ nameModify(NameFilter(st_name_null))
									+ "," + nodes_after[6] + ","
									+ nodes_after[5];

						}
						/*
						 * if((func_out == 1||func_out ==2)&&(func_in ==
						 * 1||func_in == 2)){ road_count++; out.print() }
						 */
						// skannan-comment: merge into highway
						else if ((func_out == 3 || func_out == 4 || func_out == 5)
								&& (func_in == 1 || func_in == 2)) {
							// out.print(","+myformat.format(distance_pre)+",");
							turnbyturnString += ","
									+ myformat.format(distance_pre) + ",";
							road_count++;
							// out.print(road_count+". ");
							// out.print("Merge onto "+st_name_null+"via the ramp on the left, "+distance+"miles\n");
							// out.print("M-VLR("+nodes[6]+"-"+nodes[5]+"-"+myformat.format(distance)+"),"+nameModify(NameFilter(st_name_null))+","+nodes_after[6]+","+nodes_after[5]);

							// skannan-change: to integrate the exit sign
							String location = nodes[0]; // link id
							String exitName = exitDataWithLocation.get(location
									+ "," + func_out);

							if (null == exitName) {
								exitName = exitDataWithLocation.get(location
										+ "," + func_in);
							}

//							System.out.println("M-VLR,Take ramp "
//									+location+","+ link_id_null + ",out " + exitName);

							// out.print("M-VLR,Merge Via Left Ramp,"+nodes[6]+","+nodes[5]+","+myformat.format(distance)+",F,"+nameModify(NameFilter(st_name_null))+";"+exitName+","+nodes_after[6]+","+nodes_after[5]);
							turnbyturnString += "M-VLR,Take ramp "
									+ nodes[6] + "," + nodes[5] + ","
									+ myformat.format(distance) + ",F,"
									+ nameModify(NameFilter(st_name_null))
									+ ";" + exitName + "," + nodes_after[6]
									+ "," + nodes_after[5];
						} else if ((func_out == 1 || func_out == 2)
								&& (func_in == 1 || func_in == 2)) { // continue
																		// in
																		// highway
						// out.print(","+myformat.format(distance_pre)+",");
							turnbyturnString += ","
									+ myformat.format(distance_pre) + ",";
							road_count++;

							// skannan-change: to integrate the exit sign
							String location = nodes[0]; // link id
							String exitName = exitDataWithLocation.get(location
									+ "," + func_out);

							if (null == exitName) {
								exitName = exitDataWithLocation.get(location
										+ "," + func_in);
							}

//							System.out.println("HE(" +location+","+ link_id_null + ",out " + exitName);

							// out.print("HE("+nodes[6]+"-"+nodes[5]+"-"+myformat.format(distance)+"),"+nameModify(NameFilter(st_name_null))+";"+exitName+","+nodes_after[6]+","+nodes_after[5]);
							turnbyturnString += "HE(" + nodes[6] + "-"
									+ nodes[5] + "-"
									+ myformat.format(distance) + "),"
									+ nameModify(NameFilter(st_name_null))
									+ ";" + exitName + "," + nodes_after[6]
									+ "," + nodes_after[5];
						}

						else {
							road_count++;
							// out.print(road_count+". ");
							// out.print("specialcase,");
							// out.print(","+myformat.format(distance_pre)+",");
							turnbyturnString += ","
									+ myformat.format(distance_pre) + ",";
							road_count++;

							// skannan-change: to integrate the exit sign
							String location = nodes[0]; // link id
							String exitName = exitDataWithLocation.get(location
									+ "," + func_out);

							if (null == exitName) {
								exitName = exitDataWithLocation.get(location
										+ "," + func_in);
							}
//							System.out.println(location+","+link_id_null + ", in " + exitName);
							// out.print("(S)"+"-"+nodes[6]+"-"+nodes[5]+"-"+direction_before+"-"+myformat.format(distance)+"-"+direction_after+","+nameModify(NameFilter(st_name_null))+","+nodes_after[6]+","+nodes_after[5]+",E,"+exitName);
							turnbyturnString += "(S)" + "-" + nodes[6] + "-"
									+ nodes[5] + "-" + direction_before + "-"
									+ myformat.format(distance) + "-"
									+ direction_after + ","
									+ nameModify(NameFilter(st_name_null))
									+ "," + nodes_after[6] + ","
									+ nodes_after[5] + ",E," + exitName;
						}

						tail_st_x = Double.parseDouble(nodes_after[6]) * 100.;
						tail_st_y = Double.parseDouble(nodes_after[5]) * 100.;
						tail_end_x = Double.parseDouble(nodes_after[8]) * 100.;
						tail_end_y = Double.parseDouble(nodes_after[7]) * 100.;

						link_id = link_id_null;
						st_name_pre = st_name_null;
						st_func_pre = st_func_null;

						distance = DistanceCalculator.CalculationByDistance(
								new PairInfo(
										Double.parseDouble(nodes_after[5]),
										Double.parseDouble(nodes_after[6])),
								new PairInfo(
										Double.parseDouble(nodes_after[7]),
										Double.parseDouble(nodes_after[8])));
						continue;
					}

					A = tail_end_y - tail_st_y;
					B = -(tail_end_x - tail_st_x);
					C = (tail_end_x - tail_st_x) * tail_st_y
							- (tail_end_y - tail_st_y) * tail_st_x;

					double x = Double.parseDouble(nodes[8]) * 100.;
					double y = Double.parseDouble(nodes[7]) * 100.;

					double result = A * x + B * y + C;
					String direction = "";

					if (result == 0)
						direction = "F";
					else if (result > 0)
						direction = "R";
					else
						direction = "L";

					double x1 = tail_end_x - tail_st_x;
					double y1 = tail_end_y - tail_st_y;
					double x2 = x - tail_end_x;
					double y2 = y - tail_end_y;

					double cosTheta = (x1 * x2 + y1 * y2)
							/ (Math.sqrt(x1 * x1 + y1 * y1) * Math.sqrt(x2 * x2
									+ y2 * y2));

					// out.print(","+myformat.format(distance)+",");
					turnbyturnString += "," + myformat.format(distance) + ",";
					road_count++;
//					System.out.println("before final directions link id: "+link_id+" link id old: "+link_id_o+" seq num: "+nodes[1]
//							+"direction "+direction);
					// out.print(road_count+". ");
					if (direction.equals("F")) {
						// out.print("F,"+nameModify(NameFilter(st_name_o))+","+nodes[6]+","+nodes[5]);
						turnbyturnString += "F,"
								+ nameModify(NameFilter(st_name_o)) + ","
								+ nodes[6] + "," + nodes[5];
					} else if (direction.equals("R")) {
						if (cosTheta >= 0.86) {
							// out.print("SR,"+nameModify(NameFilter(st_name_o))+","+nodes[6]+","+nodes[5]);
							turnbyturnString += "SR,"
									+ nameModify(NameFilter(st_name_o)) + ","
									+ nodes[6] + "," + nodes[5];
						}

						else {
							// out.print("R,"+nameModify(NameFilter(st_name_o))+","+nodes[6]+","+nodes[5]);
							turnbyturnString += "R,"
									+ nameModify(NameFilter(st_name_o)) + ","
									+ nodes[6] + "," + nodes[5];
						}

					} else {
						if (cosTheta >= 0.86) {
							// out.print("SL,"+nameModify(NameFilter(st_name_o))+","+nodes[6]+","+nodes[5]);
							turnbyturnString += "SL,"
									+ nameModify(NameFilter(st_name_o)) + ","
									+ nodes[6] + "," + nodes[5];
						}

						else {
							// out.print("L,"+nameModify(NameFilter(st_name_o))+","+nodes[6]+","+nodes[5]);
							turnbyturnString += "L,"
									+ nameModify(NameFilter(st_name_o)) + ","
									+ nodes[6] + "," + nodes[5];
						}

					}
					// out.print("\n");

					tail_st_x = Double.parseDouble(nodes[6]) * 100.;
					tail_st_y = Double.parseDouble(nodes[5]) * 100.;
					tail_end_x = Double.parseDouble(nodes[8]) * 100.;
					tail_end_y = Double.parseDouble(nodes[7]) * 100.;

					link_id = link_id_o;
					st_name_pre = st_name_o;
					st_func_pre = st_func_o;

					distance = DistanceCalculator.CalculationByDistance(
							new PairInfo(Double.parseDouble(nodes[5]), Double
									.parseDouble(nodes[6])),
							new PairInfo(Double.parseDouble(nodes[7]), Double
									.parseDouble(nodes[8])));

				}
			}
			turnbyturnString += ", " + myformat.format(distance);
			// out.print(", "+myformat.format(distance));

			// out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		System.out.println("servlet output string "+turnbyturnString);
//		System.out.println("Exit turnbyturnnew");
		return turnbyturnString;
	}

	String showaccident(int stID, int endID) {
		
//		System.out.println("Inside show accident");
		String servletOutput = "";
		try {
			Set<String> keys = accidentdisplay.keySet();
			Iterator<String> iter = keys.iterator();

			// out.print(";$"+keys.size()+";");

			StringBuffer temp1 = new StringBuffer("");
			StringBuffer temp2 = new StringBuffer("");

			int count = 0;
			int i = 0;
			while (i < keys.size()) {
				String info = iter.next();
				// test in region
				double centerx = (nodes[stID][1] + nodes[endID][1]) / 2.;
				double centery = (nodes[stID][0] + nodes[endID][0]) / 2.;
				double r = Math.sqrt(Math.pow(nodes[stID][1] - nodes[endID][1],
						2) + Math.pow(nodes[stID][0] - nodes[endID][0], 2)) / 2.;
				String[] infonodes = info.split(",");
				double locationx = Double.parseDouble(infonodes[1]);
				double locationy = Double.parseDouble(infonodes[0]);
				double test = Math.pow(locationx - centerx, 2)
						+ Math.pow(locationy - centery, 2);
				if (test <= Math.pow(r, 2)) {
					// test in region ends
					count++;
					temp2.append(info + ";");
					// out.print(info+";");
				}
				i++;
			}

			temp1.append("$" + count + ";");
			temp1.append(temp2);
			// out.print(temp1.toString());
			servletOutput = temp1.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		System.out.println("servlet output string" + servletOutput);
//		System.out.println("Exit showaccident");
		return servletOutput;
	}

	public static Connection getConnection() {
		try {
			DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
			Connection connHome = DriverManager.getConnection(url_home,
					userName, password);
			return connHome;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return connHome;

	}

	public String accidentModify() {
		String servletOutput = "";
		try {

			FileWriter fstream_out = new FileWriter(
					"H:\\Jiayunge\\accident\\accident_"
							+ System.currentTimeMillis() + "+.csv");
			BufferedWriter out_accident = new BufferedWriter(fstream_out);

			Connection con = getConnection();
			String sql = "select  event_name, lat, lon, severity, start_time, end_time, day_index, backlog, type, direction from event_realtime ";
			PreparedStatement f = con.prepareStatement(sql,
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			ResultSet rs = f.executeQuery();
			// out.print("read finishedsd1234!!\n");

			/*
			 * //add for testing FileWriter fstream_out = new FileWriter(
			 * "C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\TDSP_Servlet\\WEB-INF\\classes\\TDSP Files12345\\Func1to4.kml"
			 * ); BufferedWriter out_kml = new BufferedWriter(fstream_out);
			 * out_kml.write("<kml><Document>"); //add for testing ends
			 */

			HashMap<String, Integer> visited = new HashMap<String, Integer>();

			int count = 0;
			while (rs.next()) {
				// parse the input parameters
				double[] location = new double[2];
				location[0] = rs.getDouble(2);
				location[1] = rs.getDouble(3);
				int severity = rs.getInt(4);

				String stTimeString = rs.getString(5);
				String[] stTimeNodes = stTimeString.split(" ")[1].split(":");
				String endTimeString = rs.getString(6);
				String[] endTimeNodes = endTimeString.split(" ")[1].split(":");

				// calculate start time index
				int hours = Integer.parseInt(stTimeNodes[0]);
				int minutes = Integer.parseInt(stTimeNodes[1]);
				int minuteIndex = -1;
				if (minutes <= 8 && minutes > 53)
					minuteIndex = 0;
				else if (minutes > 8 && minutes <= 23)
					minuteIndex = 1;
				else if (minutes > 23 && minutes <= 38)
					minuteIndex = 2;
				else if (minutes > 38 && minutes <= 53)
					minuteIndex = 3;
				int stTime = (hours - 6) * 4 + minuteIndex;
				// calculate end time index
				hours = Integer.parseInt(endTimeNodes[0]);
				minutes = Integer.parseInt(endTimeNodes[1]);
				minuteIndex = -1;
				if (minutes <= 8 && minutes > 53)
					minuteIndex = 0;
				else if (minutes > 8 && minutes <= 23)
					minuteIndex = 1;
				else if (minutes > 23 && minutes <= 38)
					minuteIndex = 2;
				else if (minutes > 38 && minutes <= 53)
					minuteIndex = 3;
				int endTime = (hours - 6) * 4 + minuteIndex;

				// int stTime = rs.getInt(5);
				// int endTime = rs.getInt(6);

				int date = rs.getInt(7);
				if (date == 1)
					date = 6;
				else
					date -= 2;

				double backlog = rs.getDouble(8);

				count++;
				String type = rs.getString(9);
				// out.print(location[0]+","+location[1]+","+type+","+date+","+stTime+","+endTime+";");
				servletOutput += location[0] + "," + location[1] + "," + type
						+ "," + date + "," + stTime + "," + endTime + ";";
				// out.print(count+"\n");
				// out.print(stTimeString+","+endTimeString+"\n");
				// out.print("stID = "+ stTime+", endId = "+endTime+"\n");

				if (endTime >= 60)
					endTime = 59;
				// if(stTime>=60)
				// continue;

				if (stTime > 59 || stTime < 0 || endTime > 59 || endTime < 0)
					continue;

				if (location[0] == 0 || location[1] == 0)
					continue;

				if (accidentTable.containsKey(location[0] + "," + location[1]
						+ "," + stTime + "," + endTime + "," + date))
					continue;

				// output accident information
				out_accident.write(location[0] + "," + location[1] + "," + type
						+ "," + date + "," + stTime + "," + endTime + ":\n");

				int locationID = findNN(location[0], location[1]);

				// graphTDSP[i][index].nodes.put(j, new NodeValues(j,values));

				double length = 0.;
				int j = locationID;
				int i = -1;
				while (length <= backlog) {

					// out.print("insidep1\n");
					if (!reverseList.containsKey(j))
						break;

					// modify1
					int num = 0;
					for (; num < reverseList.get(j).size(); num++) {
						i = reverseList.get(j).get(num);
						if (visited.containsKey(i + "," + j))
							;
						else {
							visited.put(i + "," + j, 1);
							break;
						}
					}
					if (num == reverseList.get(j).size())
						break;
					// modify1 ends

					String key = "n" + i + "," + "n" + j;
					String[] info = streets.get(key).split(",");
					double distance = Double.parseDouble(info[10]);
					// out.print("insidep1, distance = "+distance+"\n");
					// add weights;
					double weight = 1.;
					double factor = length / backlog;
					if (factor < 0.1)
						weight = 0.1;
					else if (factor >= 0.1 && factor < 0.2)
						weight = 0.2;
					else if (factor >= 0.2 && factor < 0.3)
						weight = 0.3;
					else if (factor >= 0.3 && factor < 0.4)
						weight = 0.4;
					else if (factor >= 0.4 && factor < 0.5)
						weight = 0.5;
					else if (factor >= 0.5 && factor < 0.6)
						weight = 0.6;
					else if (factor >= 0.6 && factor < 0.7)
						weight = 0.7;
					else if (factor >= 0.7 && factor < 0.8)
						weight = 0.8;
					else if (factor >= 0.8 && factor < 0.9)
						weight = 0.9;
					else
						weight = 1.;
					// add weights ends;

					double fd = (1. / weight);
					int fi = (int) fd;
					int values[] = new int[60];

					values = graphTDSP[i][date].nodes.get(j).getValues();

					// double speed = 2.;
					// int time = (int)((distance*60.0*60.0*1000.0)/speed);
					// if(time==0)
					// time=1;

					for (int k = stTime; k <= endTime; k++) {
						values[k] = values[k] * fi;
						// eventLU.put(i+","+j+","+date+","+k,
						// location[0]+","+location[1]);
						eventLU.put(i + "," + j + "," + date + "," + k,
								location[0] + "," + location[1] + "," + type
										+ "," + date + "," + stTime + ","
										+ endTime + "," + backlog);
						// add accident speed values
						out_accident.write(i + "," + j + "," + date + "," + k
								+ ":" + values[k] + "\n");
						if (values[k] <= 0) {
							values[k] = (int) ((distance * 60.0 * 60.0 * 1000.0) / 2.0);
						}

					}

					graphTDSP[i][date].nodes.remove(j);
					graphTDSP[i][date].nodes.put(j, new NodeValues(j, values));

					// out_kml.write("<Placemark><name>"+location[0]+","+location[1]+"</name><description>"+"testcase1"+"</description><LineString><tessellate>1</tessellate><coordinates>"+nodes[i][1]+","+nodes[i][0]+",0 "+nodes[j][1]+","+nodes[j][0]+",0 </coordinates></LineString></Placemark>");

					length += distance;

					j = i;

				}

			}

			// out_kml.write("</Document></kml>");
			// out_kml.close();

			visited.clear();

			out_accident.close();
			fstream_out.close();

			// out.print(count+"\n");
			servletOutput += count + "\n";

			accidentTable.clear();
			rs.beforeFirst();

			while (rs.next()) {
				// parse the input parameters
				double[] location = new double[2];
				location[0] = rs.getDouble(2);
				location[1] = rs.getDouble(3);
				int severity = rs.getInt(4);

				String stTimeString = rs.getString(5);
				String[] stTimeNodes = stTimeString.split(" ")[1].split(":");
				String endTimeString = rs.getString(6);
				String[] endTimeNodes = endTimeString.split(" ")[1].split(":");

				// calculate start time index
				int hours = Integer.parseInt(stTimeNodes[0]);
				int minutes = Integer.parseInt(stTimeNodes[1]);
				int minuteIndex = -1;
				if (minutes <= 8 && minutes > 53)
					minuteIndex = 0;
				else if (minutes > 8 && minutes <= 23)
					minuteIndex = 1;
				else if (minutes > 23 && minutes <= 38)
					minuteIndex = 2;
				else if (minutes > 38 && minutes <= 53)
					minuteIndex = 3;
				int stTime = (hours - 6) * 4 + minuteIndex;
				// calculate end time index
				hours = Integer.parseInt(endTimeNodes[0]);
				minutes = Integer.parseInt(endTimeNodes[1]);
				minuteIndex = -1;
				if (minutes <= 8 && minutes > 53)
					minuteIndex = 0;
				else if (minutes > 8 && minutes <= 23)
					minuteIndex = 1;
				else if (minutes > 23 && minutes <= 38)
					minuteIndex = 2;
				else if (minutes > 38 && minutes <= 53)
					minuteIndex = 3;
				int endTime = (hours - 6) * 4 + minuteIndex;

				// int stTime = rs.getInt(5);
				// int endTime = rs.getInt(6);
				int date = rs.getInt(7);
				if (date == 1)
					date = 6;
				else
					date -= 2;
				double backlog = rs.getDouble(8);

				if (endTime >= 60)
					endTime = 59;
				// if(stTime>=60)
				// continue;

				if (stTime > 59 || stTime < 0 || endTime > 59 || endTime < 0)
					continue;

				if (location[0] == 0 || location[1] == 0)
					continue;

				accidentTable.put(location[0] + "," + location[1] + ","
						+ stTime + "," + endTime + "," + date, 1);

			}

			rs.close();
			con.close();
			// out.print("accident table updated!\n");
			servletOutput += "accident table updated!\n";

		} catch (Exception e) {
			e.printStackTrace();
		}
		return servletOutput;
	}

	public String compare() {
		String servletOutput = "";
		try {
			HashMap<String, ArrayList<Double>> updateTable = new HashMap<String, ArrayList<Double>>();

			Connection con = getConnection();

			double affectedLength = 1.5;
			// out.print("weishenme!\n");
			servletOutput += "weishenme!\n";

			String sql = "select link_id, real_time_speed from arterial_realtime ";
			// String sql =
			// "select link_id, realtime_speed from arterial_realtime_fake";

			PreparedStatement f = con.prepareStatement(sql,
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			ResultSet rs = f.executeQuery();
			// out.print("loaded arterials\n");
			servletOutput += "loaded arterials\n";
			// int count = 0;
			while (rs.next()) {
				// count++;
				// out.print(count+"\n");
				int link_id = rs.getInt(1);
				if (lookup.containsKey(link_id)) {

					double distance = 0.;

					int stID = Integer
							.parseInt(lookup.get(link_id).split(",")[0]
									.substring(1));
					int endID = Integer
							.parseInt(lookup.get(link_id).split(",")[1]
									.substring(1));

					if (updateTable.containsKey("n" + stID + "," + "n" + endID
							+ "," + dayIndex + "," + timeIndex)) {
						ArrayList<Double> temp_array = updateTable.get("n"
								+ stID + "," + "n" + endID + "," + dayIndex
								+ "," + timeIndex);
						temp_array.add(rs.getDouble(2));
						updateTable.remove("n" + stID + "," + "n" + endID + ","
								+ dayIndex + "," + timeIndex);
						updateTable.put("n" + stID + "," + "n" + endID + ","
								+ dayIndex + "," + timeIndex, temp_array);
					} else {
						ArrayList<Double> temp_array = new ArrayList<Double>();
						temp_array.add(rs.getDouble(2));
						updateTable.put("n" + stID + "," + "n" + endID + ","
								+ dayIndex + "," + timeIndex, temp_array);
					}

					distance += Double.parseDouble(streets.get(
							"n" + stID + "," + "n" + endID).split(",")[10]);

					int backID = stID;
					int forwardID = endID;
					while (distance < affectedLength) {
						// go back
						if (reverseList.containsKey(backID)) {
							int node1 = reverseList.get(backID).get(0);
							if (updateTable
									.containsKey("n" + node1 + "," + "n"
											+ backID + "," + dayIndex + ","
											+ timeIndex)) {
								ArrayList<Double> temp_array = updateTable
										.get("n" + node1 + "," + "n" + backID
												+ "," + dayIndex + ","
												+ timeIndex);
								temp_array.add(rs.getDouble(2));
								updateTable.remove("n" + node1 + "," + "n"
										+ backID + "," + dayIndex + ","
										+ timeIndex);
								updateTable.put("n" + node1 + "," + "n"
										+ backID + "," + dayIndex + ","
										+ timeIndex, temp_array);
							} else {
								ArrayList<Double> temp_array = new ArrayList<Double>();
								temp_array.add(rs.getDouble(2));
								updateTable.put("n" + node1 + "," + "n"
										+ backID + "," + dayIndex + ","
										+ timeIndex, temp_array);
							}

							distance += Double.parseDouble(streets.get(
									"n" + node1 + "," + "n" + backID)
									.split(",")[10]);
							if (distance >= affectedLength)
								break;
							else
								backID = node1;
						}

						// go forward
						if (forwardList.containsKey(forwardID)) {
							int node2 = forwardList.get(forwardID).get(0);
							if (updateTable.containsKey("n" + forwardID + ","
									+ "n" + node2 + "," + dayIndex + ","
									+ timeIndex)) {
								ArrayList<Double> temp_array = updateTable
										.get("n" + forwardID + "," + "n"
												+ node2 + "," + dayIndex + ","
												+ timeIndex);
								temp_array.add(rs.getDouble(2));
								updateTable.remove("n" + forwardID + "," + "n"
										+ node2 + "," + dayIndex + ","
										+ timeIndex);
								updateTable.put("n" + forwardID + "," + "n"
										+ node2 + "," + dayIndex + ","
										+ timeIndex, temp_array);
							} else {
								ArrayList<Double> temp_array = new ArrayList<Double>();
								temp_array.add(rs.getDouble(2));
								updateTable.put("n" + forwardID + "," + "n"
										+ node2 + "," + dayIndex + ","
										+ timeIndex, temp_array);
							}
							distance += Double.parseDouble(streets.get(
									"n" + forwardID + "," + "n" + node2).split(
									",")[10]);
							if (distance >= affectedLength)
								break;
							else
								forwardID = node2;
						}

					}
				}
			}

			rs.close();
			f.close();
			// out.print("arterial roads finished!!!\n");
			servletOutput += "arterial roads finished!!!\n";

			sql = "select link_id, real_time_speed from highway_realtime ";
			// sql =
			// "select link_id, realtime_speed from highway_realtime_fake";
			f = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			rs = f.executeQuery();

			while (rs.next()) {
				int link_id = rs.getInt(1);
				if (lookup.containsKey(link_id)) {

					double distance = 0.;

					int stID = Integer
							.parseInt(lookup.get(link_id).split(",")[0]
									.substring(1));
					int endID = Integer
							.parseInt(lookup.get(link_id).split(",")[1]
									.substring(1));

					if (updateTable.containsKey("n" + stID + "," + "n" + endID
							+ "," + dayIndex + "," + timeIndex)) {
						ArrayList<Double> temp_array = updateTable.get("n"
								+ stID + "," + "n" + endID + "," + dayIndex
								+ "," + timeIndex);
						temp_array.add(rs.getDouble(2));
						updateTable.remove("n" + stID + "," + "n" + endID + ","
								+ dayIndex + "," + timeIndex);
						updateTable.put("n" + stID + "," + "n" + endID + ","
								+ dayIndex + "," + timeIndex, temp_array);
					} else {
						ArrayList<Double> temp_array = new ArrayList<Double>();
						temp_array.add(rs.getDouble(2));
						updateTable.put("n" + stID + "," + "n" + endID + ","
								+ dayIndex + "," + timeIndex, temp_array);
					}

					distance += Double.parseDouble(streets.get(
							"n" + stID + "," + "n" + endID).split(",")[10]);

					int backID = stID;
					int forwardID = endID;
					while (distance < affectedLength) {
						// go back
						if (reverseList.containsKey(backID)) {
							int node1 = reverseList.get(backID).get(0);
							if (updateTable
									.containsKey("n" + node1 + "," + "n"
											+ backID + "," + dayIndex + ","
											+ timeIndex)) {
								ArrayList<Double> temp_array = updateTable
										.get("n" + node1 + "," + "n" + backID
												+ "," + dayIndex + ","
												+ timeIndex);
								temp_array.add(rs.getDouble(2));
								updateTable.remove("n" + node1 + "," + "n"
										+ backID + "," + dayIndex + ","
										+ timeIndex);
								updateTable.put("n" + node1 + "," + "n"
										+ backID + "," + dayIndex + ","
										+ timeIndex, temp_array);
							} else {
								ArrayList<Double> temp_array = new ArrayList<Double>();
								temp_array.add(rs.getDouble(2));
								updateTable.put("n" + node1 + "," + "n"
										+ backID + "," + dayIndex + ","
										+ timeIndex, temp_array);
							}

							distance += Double.parseDouble(streets.get(
									"n" + node1 + "," + "n" + backID)
									.split(",")[10]);
							if (distance >= affectedLength)
								break;
							else
								backID = node1;
						}

						// go forward
						if (forwardList.containsKey(forwardID)) {
							int node2 = forwardList.get(forwardID).get(0);
							if (updateTable.containsKey("n" + forwardID + ","
									+ "n" + node2 + "," + dayIndex + ","
									+ timeIndex)) {
								ArrayList<Double> temp_array = updateTable
										.get("n" + forwardID + "," + "n"
												+ node2 + "," + dayIndex + ","
												+ timeIndex);
								temp_array.add(rs.getDouble(2));
								updateTable.remove("n" + forwardID + "," + "n"
										+ node2 + "," + dayIndex + ","
										+ timeIndex);
								updateTable.put("n" + forwardID + "," + "n"
										+ node2 + "," + dayIndex + ","
										+ timeIndex, temp_array);
							} else {
								ArrayList<Double> temp_array = new ArrayList<Double>();
								temp_array.add(rs.getDouble(2));
								updateTable.put("n" + forwardID + "," + "n"
										+ node2 + "," + dayIndex + ","
										+ timeIndex, temp_array);
							}
							distance += Double.parseDouble(streets.get(
									"n" + forwardID + "," + "n" + node2).split(
									",")[10]);
							if (distance >= affectedLength)
								break;
							else
								forwardID = node2;
						}

					}

				}
			}

			rs.close();
			f.close();
			// out.print("highway finished!!!\n");
			servletOutput += "highway finished!!!\n";

			FileWriter fstream_out = new FileWriter(
					"C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\TDSP_Servlet\\WEB-INF\\classes\\TDSP Files12345\\realtimeUpdateTable.csv");
			BufferedWriter out_file = new BufferedWriter(fstream_out);

			Set<String> keys = updateTable.keySet();
			Iterator<String> iter = keys.iterator();
			int i = 0;
			while (i < keys.size()) {
				String key = iter.next();
				String nodes[] = key.split(",");
				int stID = Integer.parseInt(nodes[0].substring(1));
				int endID = Integer.parseInt(nodes[1].substring(1));

				int values[] = new int[60];
				values = graphTDSP[stID][dayIndex].nodes.get(endID).getValues();

				double speed = 0.;
				int count_valid = 0;
				ArrayList<Double> temp_array = updateTable.get(key);
				for (int m = 0; m < temp_array.size(); m++) {
					if (temp_array.get(m) > 0) {
						count_valid++;
						speed += temp_array.get(m);
					}
				}
				if (count_valid != 0)
					speed = speed / (double) count_valid;

				if (speed != 0) {
					String[] info = streets.get("n" + stID + "," + "n" + endID)
							.split(",");
					double distance = Double.parseDouble(info[10]);

					int time = (int) ((distance * 60.0 * 60.0 * 1000.0) / speed);
					if (time == 0)
						time = 1;

					double difference = 0.;
					if (values[timeIndex] == 0)
						difference = 1;
					else
						difference = (double) Math
								.abs(time - values[timeIndex])
								/ (double) values[timeIndex];
					if (difference >= 0.3)
						out_file.write(key + "," + time + "\n");
				}

				i++;
			}
			out_file.close();
			fstream_out.close();
			// out.print("day = "+dayIndex+", time = "+timeIndex+"\n");
			// out.print("im here"+"\n");
			servletOutput += "day = " + dayIndex + ", time = " + timeIndex
					+ "\n";
			servletOutput += "im here" + "\n";

			con.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return servletOutput;
	}

	private resultset tdspAtest(int start, int end, int time) {

		// System.out.println("TDSP CALLED WITH PARAMETERS:"+start+","+end+","+time+","+maxTime);

		FibonacciHeap<Node> priorityQ = new FibonacciHeap<Node>();
		HashMap<Integer, Node> priorityQMap1 = new HashMap<Integer, Node>();
		HashMap<Node, FibonacciHeapNode<Node>> priorityQMap2 = new HashMap<Node, FibonacciHeapNode<Node>>();

		int leftcount = 0;

		int i, len = graphTDSP.length, j, id;
		// System.out.println("len="+len);
		int w, arrTime;
		int[] c = new int[len];
		int[] parent = new int[len];
		// double shortestTime = 8000000;

		int[] nextNode = new int[len];

		double hdistance = distance(nodes[start][0], nodes[start][1],
				nodes[end][0], nodes[end][1]);
		int htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);

		for (i = 0; i < len; i++) {
			nextNode[i] = -1;
			parent[i] = -1;
			if (i == start)
				c[i] = 0 + htime; // starting node
			else
				c[i] = 8000000; // indicating infinity
		}

		Node n, s = new Node(start, c[start], 0); // creating the starting
		// node with nodeId =
		// start and cost = 0
		// and arrival time =
		// time
		FibonacciHeapNode<Node> new_node = new FibonacciHeapNode<Node>(s);
		priorityQ.insert(new_node, s.getNodeCost()); // inserting s into the
														// priority queue
		priorityQMap1.put(s.getNodeId(), s);
		priorityQMap2.put(s, new_node);

		while (!(priorityQ.isEmpty())) { // while Q is not empty

			// System.out.println("Size of queue="+priorityQ.size());
			FibonacciHeapNode<Node> node = priorityQ.min();
			priorityQ.removeMin();
			n = node.getData();

			n = priorityQMap1.get(n.getNodeId());
			priorityQMap1.remove(n.getNodeId());
			priorityQMap2.remove(n);

			int updTime = time;
			id = n.getNodeId();
			if (graphTDSP[id][dayIndex] == null)
				continue;

			if (n.getNodeId() == end) {
				// shortestTime = n.getArrTime();
				break;
			}

			if (n.getNodeId() != start) {
				updTime = time + getTime(n.getArrTime());

				if (updTime > 59)
					updTime = 59;
			}

			if (graphTDSP[id][dayIndex].nodes != null) {
				HashMap<Integer, NodeValues> hmap = graphTDSP[id][dayIndex].nodes;

				Set<Integer> keys = hmap.keySet();
				Iterator<Integer> iter = keys.iterator();
				// System.out.println("size->"+keys.size());
				int i2 = 0;
				while (i2 < keys.size()) {
					int key = iter.next();
					NodeValues val = hmap.get(key);
					arrTime = n.getArrTime();
					if (eventLU.containsKey(id + "," + key + "," + dayIndex
							+ "," + updTime)) {
						// double location0 =
						// Double.parseDouble(eventLU.get(id+","+key+","+dayIndex+","+updTime).split(",")[0]);
						// double location1 =
						// Double.parseDouble(eventLU.get(id+","+key+","+dayIndex+","+updTime).split(",")[1]);
						// accidentdisplay.put(eventLU.get(id+","+key+","+dayIndex+","+updTime),
						// 1);
					}
					w = val.getValues()[updTime];

					// left turn penalty test
					// int tail_id = 0;
					if (parent[id] != -1) {
						int id0 = parent[id];
						int id1 = id;
						int id2 = key;

						// tail_id = parent[id];
						double tail_st_x = nodes[id0][1] * 100.;
						double tail_st_y = nodes[id0][0] * 100.;
						double tail_end_x = nodes[id1][1] * 100.;
						double tail_end_y = nodes[id1][0] * 100.;

						double A = tail_end_y - tail_st_y;
						double B = -(tail_end_x - tail_st_x);
						double C = (tail_end_x - tail_st_x) * tail_st_y
								- (tail_end_y - tail_st_y) * tail_st_x;

						double x = nodes[id2][1] * 100.;
						double y = nodes[id2][0] * 100.;

						double result = A * x + B * y + C;

						if (result < 0) {

							double x1 = tail_end_x - tail_st_x;
							double y1 = tail_end_y - tail_st_y;
							double x2 = x - tail_end_x;
							double y2 = y - tail_end_y;

							double cosTheta = (x1 * x2 + y1 * y2)
									/ (Math.sqrt(x1 * x1 + y1 * y1) * Math
											.sqrt(x2 * x2 + y2 * y2));

							if (cosTheta < 0.5)
								w = w + 1 * 60000;
						}

					}
					// left turn penalty ends

					// g
					hdistance = distance(nodes[key][0], nodes[key][1],
							nodes[end][0], nodes[end][1]);
					htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);
					// g ends

					// System.out.println("cost="+w);
					if (arrTime + w + htime < c[key]) {
						c[key] = arrTime + w + htime;
						parent[key] = id;
						// it = priorityQ.iterator();

						// System.out.println("Size of queue="+priorityQ.size());

						if (priorityQMap1.containsKey(key)) {
							Node node_ = priorityQMap1.get(key);
							node_.setArrTime(c[key] - htime);
							node_.setNodeCost(c[key]);
							priorityQMap1.put(key, node_);
							// priorityQ.
							priorityQ.decreaseKey(priorityQMap2.get(node_),
									node_.getNodeCost());
						} else {
							Node node2 = new Node(key, c[key], c[key] - htime);
							FibonacciHeapNode<Node> new_node2 = new FibonacciHeapNode<Node>(
									node2);
							priorityQ.insert(new_node2, c[key]); // arrival time
																	// = c[i]
							priorityQMap1.put(key, node2);
							priorityQMap2.put(node2, new_node2);
						}

					}

					i2++;

				}
			}
		}

		int temp;
		temp = end;
		while (temp != -1) {
			if (parent[temp] != -1)
				nextNode[parent[temp]] = temp;
			temp = parent[temp];
		}

		double returndistance = 0.;
		double[][] pair = new double[2][2];
		int cur = 1;

		if (start == end) {
			// out.println("Your starting node is the same as your ending node.");
			// /out.println(""+nodes[start][0]+","+nodes[start][1]+";0");
			// /nodesInfo.append(""+nodes[ nextNode[i]][0]+","+nodes[
			// nextNode[i]][1] + ";");
			return new resultset(0., 0.);
		} else {
			i = start;
			j = 1;
			// /out.print(""+nodes[i][0]+","+nodes[i][1]+";");
			// /nodesInfo.append(""+nodes[i][0]+","+nodes[i][1]+";");
			pair[0][0] = nodes[i][0];
			pair[0][1] = nodes[i][1];

			while (i != end && nextNode[i] != -1) {

				// /out.print(""+nodes[ nextNode[i]][0]+","+nodes[
				// nextNode[i]][1] + ";");

				// /nodesInfo.append(""+nodes[ nextNode[i]][0]+","+nodes[
				// nextNode[i]][1] + ";");

				pair[cur][0] = nodes[nextNode[i]][0];
				pair[cur][1] = nodes[nextNode[i]][1];
				returndistance += DistanceCalculator.CalculationByDistance(
						new PairInfo(pair[0][0], pair[0][1]), new PairInfo(
								pair[1][0], pair[1][1]));
				// cur = 1-cur;
				if (cur == 1)
					cur = 0;
				else
					cur = 1;

				i = nextNode[i];
			}
			// /out.print(""+((double)c[end]/60000.0+leftcount));
			// /nodesInfo.append(""+(double)c[end]/60000.0);
			return new resultset(returndistance, (double) c[end] / 60000.0);
		}

	}

	private resultset tdspAWithCPtest(int start, int end, int time) {

		// System.out.println("TDSP CALLED WITH PARAMETERS:"+start+","+end+","+time+","+maxTime);

		FibonacciHeap<Node> priorityQ = new FibonacciHeap<Node>();
		HashMap<Integer, Node> priorityQMap1 = new HashMap<Integer, Node>();
		HashMap<Node, FibonacciHeapNode<Node>> priorityQMap2 = new HashMap<Node, FibonacciHeapNode<Node>>();

		int leftcount = 0;

		int i, len = graphTDSP.length, j, id;
		// System.out.println("len="+len);
		int w, arrTime;
		int[] c = new int[len];
		int[] parent = new int[len];
		// double shortestTime = 8000000;

		int[] nextNode = new int[len];

		double hdistance = distance(nodes[start][0], nodes[start][1],
				nodes[end][0], nodes[end][1]);
		int htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);

		for (i = 0; i < len; i++) {
			nextNode[i] = -1;
			parent[i] = -1;
			if (i == start)
				c[i] = 0 + htime; // starting node
			else
				c[i] = 8000000; // indicating infinity
		}

		Node n, s = new Node(start, c[start], 0); // creating the starting
		// node with nodeId =
		// start and cost = 0
		// and arrival time =
		// time
		FibonacciHeapNode<Node> new_node = new FibonacciHeapNode<Node>(s);
		priorityQ.insert(new_node, s.getNodeCost()); // inserting s into the
														// priority queue
		priorityQMap1.put(s.getNodeId(), s);
		priorityQMap2.put(s, new_node);

		while (!(priorityQ.isEmpty())) { // while Q is not empty

			// System.out.println("Size of queue="+priorityQ.size());
			FibonacciHeapNode<Node> node = priorityQ.min();
			priorityQ.removeMin();
			n = node.getData();

			n = priorityQMap1.get(n.getNodeId());
			priorityQMap1.remove(n.getNodeId());
			priorityQMap2.remove(n);

			int updTime = time;
			id = n.getNodeId();
			if (graphTDSP[id][dayIndex] == null)
				continue;

			if (n.getNodeId() == end) {
				// shortestTime = n.getArrTime();
				break;
			}

			if (n.getNodeId() != start) {
				updTime = time + getTime(n.getArrTime());

				if (updTime > 59)
					updTime = 59;
			}

			if (graphTDSP[id][dayIndex].nodes != null) {
				HashMap<Integer, NodeValues> hmap = graphTDSP[id][dayIndex].nodes;

				Set<Integer> keys = hmap.keySet();
				Iterator<Integer> iter = keys.iterator();
				// System.out.println("size->"+keys.size());
				int i2 = 0;
				while (i2 < keys.size()) {
					int key = iter.next();
					NodeValues val = hmap.get(key);
					arrTime = n.getArrTime();
					if (eventLU.containsKey(id + "," + key + "," + dayIndex
							+ "," + updTime)) {
						// double location0 =
						// Double.parseDouble(eventLU.get(id+","+key+","+dayIndex+","+updTime).split(",")[0]);
						// double location1 =
						// Double.parseDouble(eventLU.get(id+","+key+","+dayIndex+","+updTime).split(",")[1]);
						// accidentdisplay.put(location0+","+location1, 1);
					}
					if (CarpoolLU.containsKey("n" + id + "," + "n" + key)) {
						String carpoolstring = streets.get("n" + id + "," + "n"
								+ key);
						String[] carpoolnodes = carpoolstring.split(",");
						double carpooldistance = Double
								.parseDouble(carpoolnodes[10]);
						if ((updTime >= 6 && updTime <= 12)
								|| (updTime >= 42 && updTime <= 48))
							w = (int) ((carpooldistance * 60.0 * 60.0 * 1000.0) / 60.);
						else
							w = (int) ((carpooldistance * 60.0 * 60.0 * 1000.0) / 75.);
					} else
						w = val.getValues()[updTime];

					// left turn penalty test
					// int tail_id = 0;
					if (parent[id] != -1) {
						int id0 = parent[id];
						int id1 = id;
						int id2 = key;

						// tail_id = parent[id];
						double tail_st_x = nodes[id0][1] * 100.;
						double tail_st_y = nodes[id0][0] * 100.;
						double tail_end_x = nodes[id1][1] * 100.;
						double tail_end_y = nodes[id1][0] * 100.;

						double A = tail_end_y - tail_st_y;
						double B = -(tail_end_x - tail_st_x);
						double C = (tail_end_x - tail_st_x) * tail_st_y
								- (tail_end_y - tail_st_y) * tail_st_x;

						double x = nodes[id2][1] * 100.;
						double y = nodes[id2][0] * 100.;

						double result = A * x + B * y + C;

						if (result < 0) {

							double x1 = tail_end_x - tail_st_x;
							double y1 = tail_end_y - tail_st_y;
							double x2 = x - tail_end_x;
							double y2 = y - tail_end_y;

							double cosTheta = (x1 * x2 + y1 * y2)
									/ (Math.sqrt(x1 * x1 + y1 * y1) * Math
											.sqrt(x2 * x2 + y2 * y2));

							if (cosTheta < 0.5)
								w = w + 1 * 60000;
						}

					}
					// left turn penalty ends

					// g
					hdistance = distance(nodes[key][0], nodes[key][1],
							nodes[end][0], nodes[end][1]);
					htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);
					// g ends

					// System.out.println("cost="+w);
					if (arrTime + w + htime < c[key]) {
						c[key] = arrTime + w + htime;
						parent[key] = id;
						// it = priorityQ.iterator();

						// System.out.println("Size of queue="+priorityQ.size());

						if (priorityQMap1.containsKey(key)) {
							Node node_ = priorityQMap1.get(key);
							node_.setArrTime(c[key] - htime);
							node_.setNodeCost(c[key]);
							priorityQMap1.put(key, node_);
							// priorityQ.
							priorityQ.decreaseKey(priorityQMap2.get(node_),
									node_.getNodeCost());
						} else {
							Node node2 = new Node(key, c[key], c[key] - htime);
							FibonacciHeapNode<Node> new_node2 = new FibonacciHeapNode<Node>(
									node2);
							priorityQ.insert(new_node2, c[key]); // arrival time
																	// = c[i]
							priorityQMap1.put(key, node2);
							priorityQMap2.put(node2, new_node2);
						}

					}

					i2++;

				}
			}
		}

		int temp;
		temp = end;
		while (temp != -1) {
			if (parent[temp] != -1)
				nextNode[parent[temp]] = temp;
			temp = parent[temp];
		}

		double returndistance = 0.;
		// double returntraveltime = 0.;

		int cur = 1;
		double[][] pair = new double[2][2];
		// distance = DistanceCalculator.CalculationByDistance(new
		// PairInfo(Double.parseDouble(nodes[5]),Double.parseDouble(nodes[6])),
		// new PairInfo(Double.parseDouble(nodes[7]),
		// Double.parseDouble(nodes[8])));

		if (start == end) {

			// /out.println(""+nodes[start][0]+","+nodes[start][1]+";0");
			// /nodesInfo.append(""+nodes[ nextNode[i]][0]+","+nodes[
			// nextNode[i]][1] + ";");

			return new resultset(0., 0.);
		} else {
			i = start;
			j = 1;
			// /out.print(""+nodes[i][0]+","+nodes[i][1]+";");
			// /nodesInfo.append(""+nodes[i][0]+","+nodes[i][1]+";");

			pair[0][0] = nodes[i][0];
			pair[0][1] = nodes[i][1];

			while (i != end && nextNode[i] != -1) {

				// /out.print(""+nodes[ nextNode[i]][0]+","+nodes[
				// nextNode[i]][1] + ";");

				// /nodesInfo.append(""+nodes[ nextNode[i]][0]+","+nodes[
				// nextNode[i]][1] + ";");

				pair[cur][0] = nodes[nextNode[i]][0];
				pair[cur][1] = nodes[nextNode[i]][1];
				returndistance += DistanceCalculator.CalculationByDistance(
						new PairInfo(pair[0][0], pair[0][1]), new PairInfo(
								pair[1][0], pair[1][1]));
				// cur = 1-cur;
				if (cur == 1)
					cur = 0;
				else
					cur = 1;

				i = nextNode[i];
			}
			// /out.print(""+((double)c[end]/60000.0+leftcount));
			// /nodesInfo.append(""+(double)c[end]/60000.0);
			return new resultset(returndistance, (double) c[end] / 60000.0);
		}

	}

	public int HOVVSNOHOV(int startNodeID, int endNodeID, int time) {
		resultset res0 = tdspAtest(startNodeID, endNodeID, time);
		resultset res1 = tdspAWithCPtest(startNodeID, endNodeID, time);

		double distance0 = res0.getDistance();
		double distance1 = res1.getDistance();
		// double traveltime0 = res0.getTrvaeltime();
		// double traveltime1 = res1.getTrvaeltime();

		// out.print("88distance0 = "+distance0+","+"distance1 = "+distance1+"\n");
		if (distance0 < distance1
				&& ((distance1 - distance0) / distance0 >= 0.35)) {
			return 0;
		}

		return 1;
	}

	private String tdspA_old(int start, int end, int time) {

		String servletOutput = "";
		// System.out.println("TDSP CALLED WITH PARAMETERS:"+start+","+end+","+time+","+maxTime);

		FibonacciHeap<Node> priorityQ = new FibonacciHeap<Node>();
		HashMap<Integer, Node> priorityQMap1 = new HashMap<Integer, Node>();
		HashMap<Node, FibonacciHeapNode<Node>> priorityQMap2 = new HashMap<Node, FibonacciHeapNode<Node>>();

		int leftcount = 0;

		int i, len = graphTDSP.length, j, id;
		// System.out.println("len="+len);
		int w, arrTime;
		int[] c = new int[len];
		int[] parent = new int[len];
		// double shortestTime = 8000000;

		int[] nextNode = new int[len];

		double hdistance = distance(nodes[start][0], nodes[start][1],
				nodes[end][0], nodes[end][1]);
		int htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);

		for (i = 0; i < len; i++) {
			nextNode[i] = -1;
			parent[i] = -1;
			if (i == start)
				c[i] = 0 + htime; // starting node
			else
				c[i] = 8000000; // indicating infinity
		}

		Node n, s = new Node(start, c[start], 0); // creating the starting
		// node with nodeId =
		// start and cost = heuristic cost
		// and arrival time =
		// 0
		FibonacciHeapNode<Node> new_node = new FibonacciHeapNode<Node>(s);
		priorityQ.insert(new_node, s.getNodeCost()); // inserting s into the
														// priority queue
		priorityQMap1.put(s.getNodeId(), s);
		priorityQMap2.put(s, new_node);

		while (!(priorityQ.isEmpty())) { // while Q is not empty

			// System.out.println("Size of queue="+priorityQ.size());
			FibonacciHeapNode<Node> node = priorityQ.min();
			priorityQ.removeMin();
			n = node.getData();

			n = priorityQMap1.get(n.getNodeId());
			priorityQMap1.remove(n.getNodeId());
			priorityQMap2.remove(n);

			int updTime = time;
			id = n.getNodeId();
			if (graphTDSP[id][dayIndex] == null)
				continue;

			if (n.getNodeId() == end) {
				// shortestTime = n.getArrTime();
				break;
			}

			if (n.getNodeId() != start) {
				updTime = time + getTime(n.getArrTime());

				if (updTime > 59)
					updTime = 59;
			}

			if (graphTDSP[id][dayIndex].nodes != null) {
				HashMap<Integer, NodeValues> hmap = graphTDSP[id][dayIndex].nodes;

				Set<Integer> keys = hmap.keySet();
				Iterator<Integer> iter = keys.iterator();
				// System.out.println("size->"+keys.size());
				int i2 = 0;
				while (i2 < keys.size()) {
					int key = iter.next();
					NodeValues val = hmap.get(key);
					arrTime = n.getArrTime();
					if (eventLU.containsKey(id + "," + key + "," + dayIndex
							+ "," + updTime)) {
						// double location0 =
						// Double.parseDouble(eventLU.get(id+","+key+","+dayIndex+","+updTime).split(",")[0]);
						// double location1 =
						// Double.parseDouble(eventLU.get(id+","+key+","+dayIndex+","+updTime).split(",")[1]);
						accidentdisplay.put(
								eventLU.get(id + "," + key + "," + dayIndex
										+ "," + updTime), 1);
					}
					w = val.getValues()[updTime];

					// left turn penalty test
					// int tail_id = 0;
					if (parent[id] != -1) {
						int id0 = parent[id];
						int id1 = id;
						int id2 = key;

						// tail_id = parent[id];
						double tail_st_x = nodes[id0][1] * 100.;
						double tail_st_y = nodes[id0][0] * 100.;
						double tail_end_x = nodes[id1][1] * 100.;
						double tail_end_y = nodes[id1][0] * 100.;

						double A = tail_end_y - tail_st_y;
						double B = -(tail_end_x - tail_st_x);
						double C = (tail_end_x - tail_st_x) * tail_st_y
								- (tail_end_y - tail_st_y) * tail_st_x;

						double x = nodes[id2][1] * 100.;
						double y = nodes[id2][0] * 100.;

						double result = A * x + B * y + C;

						if (result < 0) {

							double x1 = tail_end_x - tail_st_x;
							double y1 = tail_end_y - tail_st_y;
							double x2 = x - tail_end_x;
							double y2 = y - tail_end_y;

							double cosTheta = (x1 * x2 + y1 * y2)
									/ (Math.sqrt(x1 * x1 + y1 * y1) * Math
											.sqrt(x2 * x2 + y2 * y2));

							if (cosTheta < 0.5)
								w = w + 1 * 60000;
						}

					}
					// left turn penalty ends

					// g
					hdistance = distance(nodes[key][0], nodes[key][1],
							nodes[end][0], nodes[end][1]);
					htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);
					// g ends

					// System.out.println("cost="+w);
					if (arrTime + w + htime < c[key]) {
						c[key] = arrTime + w + htime;
						parent[key] = id;
						// it = priorityQ.iterator();

						// System.out.println("Size of queue="+priorityQ.size());

						if (priorityQMap1.containsKey(key)) {
							Node node_ = priorityQMap1.get(key);
							node_.setArrTime(c[key] - htime);
							node_.setNodeCost(c[key]);
							priorityQMap1.put(key, node_);
							// priorityQ.
							priorityQ.decreaseKey(priorityQMap2.get(node_),
									node_.getNodeCost());
						} else {
							Node node2 = new Node(key, c[key], c[key] - htime);
							FibonacciHeapNode<Node> new_node2 = new FibonacciHeapNode<Node>(
									node2);
							priorityQ.insert(new_node2, c[key]); // arrival time
																	// = c[i]
							priorityQMap1.put(key, node2);
							priorityQMap2.put(node2, new_node2);
						}

					}

					i2++;

				}
			}
		}

		int temp;
		temp = end;
		while (temp != -1) {
			if (parent[temp] != -1)
				nextNode[parent[temp]] = temp;
			temp = parent[temp];
		}

		// calculate the total distance
		singleDistance = 0.;
		double[][] pair = new double[2][2];
		int cur = 1;

		if (start == end) {
			// out.println("Your starting node is the same as your ending node.");
			// out.println(""+nodes[start][0]+","+nodes[start][1]+";0");
			servletOutput += "" + nodes[start][0] + "," + nodes[start][1]
					+ ";0";
			nodesInfo.append("" + nodes[nextNode[i]][0] + ","
					+ nodes[nextNode[i]][1] + ";");
			singleDistance = 0.;
			return servletOutput;
		} else {
			i = start;
			j = 1;
			// out.print(""+nodes[i][0]+","+nodes[i][1]+";");
			// out.print(""+gpsSt[0]+","+gpsSt[1]+";");
			servletOutput += "" + gpsSt[0] + "," + gpsSt[1] + ";";
			nodesInfo.append("" + nodes[i][0] + "," + nodes[i][1] + ";");
			pair[0][0] = nodes[i][0];
			pair[0][1] = nodes[i][1];

			while (i != end && nextNode[i] != -1) {
				if (nextNode[i] == end) {
					// out.print(""+gpsEnd[0]+","+gpsEnd[1] + ";");
					servletOutput += "" + gpsEnd[0] + "," + gpsEnd[1] + ";";
				} else {
					// out.print(""+nodes[ nextNode[i]][0]+","+nodes[
					// nextNode[i]][1] + ";");
					servletOutput += "" + nodes[nextNode[i]][0] + ","
							+ nodes[nextNode[i]][1] + ";";
				}
				nodesInfo.append("" + nodes[nextNode[i]][0] + ","
						+ nodes[nextNode[i]][1] + ";");

				pair[cur][0] = nodes[nextNode[i]][0];
				pair[cur][1] = nodes[nextNode[i]][1];
				singleDistance += DistanceCalculator.CalculationByDistance(
						new PairInfo(pair[0][0], pair[0][1]), new PairInfo(
								pair[1][0], pair[1][1]));
				// cur = 1-cur;
				if (cur == 1)
					cur = 0;
				else
					cur = 1;

				i = nextNode[i];
			}
			// out.print(""+((double)c[end]/60000.0+leftcount));
			servletOutput += "" + ((double) c[end] / 60000.0 + leftcount);
			nodesInfo.append("" + (double) c[end] / 60000.0);

		}
		return servletOutput;
	}

	private String tdspAWithCP(int start, int end, int time) {

		String servletOutput = "";
		// System.out.println("TDSP CALLED WITH PARAMETERS:"+start+","+end+","+time+","+maxTime);

		FibonacciHeap<Node> priorityQ = new FibonacciHeap<Node>();
		HashMap<Integer, Node> priorityQMap1 = new HashMap<Integer, Node>();
		HashMap<Node, FibonacciHeapNode<Node>> priorityQMap2 = new HashMap<Node, FibonacciHeapNode<Node>>();

		int leftcount = 0;

		int i, len = graphTDSP.length, j, id;
		// System.out.println("len="+len);
		int w, arrTime;
		int[] c = new int[len];
		int[] parent = new int[len];
		// double shortestTime = 8000000;

		int[] nextNode = new int[len];

		double hdistance = distance(nodes[start][0], nodes[start][1],
				nodes[end][0], nodes[end][1]);
		int htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);

		for (i = 0; i < len; i++) {
			nextNode[i] = -1;
			parent[i] = -1;
			if (i == start)
				c[i] = 0 + htime; // starting node
			else
				c[i] = 8000000; // indicating infinity
		}

		Node n, s = new Node(start, c[start], 0); // creating the starting
		// node with nodeId =
		// start and cost = 0
		// and arrival time =
		// time
		FibonacciHeapNode<Node> new_node = new FibonacciHeapNode<Node>(s);
		priorityQ.insert(new_node, s.getNodeCost()); // inserting s into the
														// priority queue
		priorityQMap1.put(s.getNodeId(), s);
		priorityQMap2.put(s, new_node);

		while (!(priorityQ.isEmpty())) { // while Q is not empty

			// System.out.println("Size of queue="+priorityQ.size());
			FibonacciHeapNode<Node> node = priorityQ.min();
			priorityQ.removeMin();
			n = node.getData();

			// skannan-comment: I dont understand why they had to return the
			// same node again
			n = priorityQMap1.get(n.getNodeId());
			priorityQMap1.remove(n.getNodeId());
			priorityQMap2.remove(n);

			int updTime = time;
			id = n.getNodeId();
			if (graphTDSP[id][dayIndex] == null)
				continue;

			if (n.getNodeId() == end) {
				// shortestTime = n.getArrTime();
				break;
			}

			if (n.getNodeId() != start) {
				updTime = time + getTime(n.getArrTime());

				if (updTime > 59)
					updTime = 59;
			}

			if (graphTDSP[id][dayIndex].nodes != null) {
				HashMap<Integer, NodeValues> hmap = graphTDSP[id][dayIndex].nodes;

				Set<Integer> keys = hmap.keySet();
				Iterator<Integer> iter = keys.iterator();
				// System.out.println("size->"+keys.size());
				int i2 = 0;
				while (i2 < keys.size()) {
					int key = iter.next();
					NodeValues val = hmap.get(key);
					arrTime = n.getArrTime();
					if (eventLU.containsKey(id + "," + key + "," + dayIndex
							+ "," + updTime)) {
						// double location0 =
						// Double.parseDouble(eventLU.get(id+","+key+","+dayIndex+","+updTime).split(",")[0]);
						// double location1 =
						// Double.parseDouble(eventLU.get(id+","+key+","+dayIndex+","+updTime).split(",")[1]);
						accidentdisplay.put(
								eventLU.get(id + "," + key + "," + dayIndex
										+ "," + updTime), 1);
					}
					if (CarpoolLU.containsKey("n" + id + "," + "n" + key)) {
						String carpoolstring = streets.get("n" + id + "," + "n"
								+ key);
						String[] carpoolnodes = carpoolstring.split(",");
						double carpooldistance = Double
								.parseDouble(carpoolnodes[10]);
						if ((updTime >= 6 && updTime <= 12)
								|| (updTime >= 42 && updTime <= 48))
							w = (int) ((carpooldistance * 60.0 * 60.0 * 1000.0) / 60.);
						else
							w = (int) ((carpooldistance * 60.0 * 60.0 * 1000.0) / 75.);
					} else
						w = val.getValues()[updTime];

					// left turn penalty test
					// int tail_id = 0;
					if (parent[id] != -1) {
						int id0 = parent[id];
						int id1 = id;
						int id2 = key;

						// tail_id = parent[id];
						double tail_st_x = nodes[id0][1] * 100.;
						double tail_st_y = nodes[id0][0] * 100.;
						double tail_end_x = nodes[id1][1] * 100.;
						double tail_end_y = nodes[id1][0] * 100.;

						double A = tail_end_y - tail_st_y;
						double B = -(tail_end_x - tail_st_x);
						double C = (tail_end_x - tail_st_x) * tail_st_y
								- (tail_end_y - tail_st_y) * tail_st_x;

						double x = nodes[id2][1] * 100.;
						double y = nodes[id2][0] * 100.;

						double result = A * x + B * y + C;

						if (result < 0) {

							double x1 = tail_end_x - tail_st_x;
							double y1 = tail_end_y - tail_st_y;
							double x2 = x - tail_end_x;
							double y2 = y - tail_end_y;

							double cosTheta = (x1 * x2 + y1 * y2)
									/ (Math.sqrt(x1 * x1 + y1 * y1) * Math
											.sqrt(x2 * x2 + y2 * y2));

							if (cosTheta < 0.5)
								w = w + 1 * 60000;
						}
					}
					// left turn penalty ends

					// g
					hdistance = distance(nodes[key][0], nodes[key][1],
							nodes[end][0], nodes[end][1]);
					htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);
					// g ends

					// System.out.println("cost="+w);
					if (arrTime + w + htime < c[key]) {
						c[key] = arrTime + w + htime;
						parent[key] = id;
						// it = priorityQ.iterator();

						// System.out.println("Size of queue="+priorityQ.size());

						if (priorityQMap1.containsKey(key)) {
							Node node_ = priorityQMap1.get(key);
							node_.setArrTime(c[key] - htime);
							node_.setNodeCost(c[key]);
							priorityQMap1.put(key, node_);
							// priorityQ.
							priorityQ.decreaseKey(priorityQMap2.get(node_),
									node_.getNodeCost());
						} else {
							Node node2 = new Node(key, c[key], c[key] - htime);
							FibonacciHeapNode<Node> new_node2 = new FibonacciHeapNode<Node>(
									node2);
							priorityQ.insert(new_node2, c[key]); // arrival time
																	// = c[i]
							priorityQMap1.put(key, node2);
							priorityQMap2.put(node2, new_node2);
						}

					}

					i2++;

				}
			}
		}

		int temp;
		temp = end;
		while (temp != -1) {
			if (parent[temp] != -1)
				nextNode[parent[temp]] = temp;
			temp = parent[temp];
		}

		singleDistance = 0.;
		double[][] pair = new double[2][2];
		int cur = 1;

		if (start == end) {
			// out.println("Your starting node is the same as your ending node.");
			// out.println(""+nodes[start][0]+","+nodes[start][1]+";0");
			servletOutput += "" + nodes[start][0] + "," + nodes[start][1]
					+ ";0";
			nodesInfo.append("" + nodes[nextNode[i]][0] + ","
					+ nodes[nextNode[i]][1] + ";");
			singleDistance = 0.;
			return servletOutput;
		} else {
			i = start;
			j = 1;
			// out.print(""+nodes[i][0]+","+nodes[i][1]+";");
			// out.print(""+gpsSt[0]+","+gpsSt[1]+";");
			servletOutput += "" + gpsSt[0] + "," + gpsSt[1] + ";";
			nodesInfo.append("" + nodes[i][0] + "," + nodes[i][1] + ";");

			pair[0][0] = nodes[i][0];
			pair[0][1] = nodes[i][1];

			while (i != end && nextNode[i] != -1) {
				/*
				 * //left turn penalty test // int tail_id = 0; int id0 =
				 * parent[i]; int id1 = i; int id2 = nextNode[i];
				 * 
				 * if(id0!=-1){ //tail_id = parent[id]; double tail_st_x =
				 * nodes[id0][1]*100.; double tail_st_y = nodes[id0][0]*100.;
				 * double tail_end_x = nodes[id1][1]*100.; double tail_end_y =
				 * nodes[id1][0]*100.;
				 * 
				 * double A = tail_end_y-tail_st_y; double B =
				 * -(tail_end_x-tail_st_x); double C =
				 * (tail_end_x-tail_st_x)*tail_st_y -
				 * (tail_end_y-tail_st_y)*tail_st_x;
				 * 
				 * double x = nodes[id2][1]*100.; double y = nodes[id2][0]*100.;
				 * 
				 * double result = A*x+B*y+C ;
				 * 
				 * 
				 * if(result<0){
				 * 
				 * double x1 = tail_end_x - tail_st_x; double y1 = tail_end_y -
				 * tail_st_y; double x2 = x - tail_end_x; double y2 = y -
				 * tail_end_y;
				 * 
				 * double cosTheta =
				 * (x1*x2+y1*y2)/(Math.sqrt(x1*x1+y1*y1)*Math.sqrt
				 * (x2*x2+y2*y2));
				 * 
				 * if(cosTheta<0.5) leftcount++; }
				 * 
				 * 
				 * }
				 * 
				 * 
				 * //left turn penalty ends
				 */
				if (nextNode[i] == end) {
					// out.print(""+gpsEnd[0]+","+gpsEnd[1] + ";");
					servletOutput += "" + gpsEnd[0] + "," + gpsEnd[1] + ";";
				} else {
					// out.print(""+nodes[ nextNode[i]][0]+","+nodes[
					// nextNode[i]][1] + ";");
					servletOutput += "" + nodes[nextNode[i]][0] + ","
							+ nodes[nextNode[i]][1] + ";";
				}

				nodesInfo.append("" + nodes[nextNode[i]][0] + ","
						+ nodes[nextNode[i]][1] + ";");

				pair[cur][0] = nodes[nextNode[i]][0];
				pair[cur][1] = nodes[nextNode[i]][1];
				singleDistance += DistanceCalculator.CalculationByDistance(
						new PairInfo(pair[0][0], pair[0][1]), new PairInfo(
								pair[1][0], pair[1][1]));
				// cur = 1-cur;
				if (cur == 1)
					cur = 0;
				else
					cur = 1;

				i = nextNode[i];
			}
			// out.print(""+((double)c[end]/60000.0+leftcount));
			servletOutput += "" + ((double) c[end] / 60000.0 + leftcount);
			nodesInfo.append("" + (double) c[end] / 60000.0);

		}
		return servletOutput;
	}

	private String tdspArrival(int start, int end, int time) {

		String servletOutput = "";
		ArrayList<String> tempPath = new ArrayList<String>();
		// System.out.println("TDSP CALLED WITH PARAMETERS:"+start+","+end+","+time+","+maxTime);

		FibonacciHeap<Node> priorityQ = new FibonacciHeap<Node>();
		HashMap<Integer, Node> priorityQMap1 = new HashMap<Integer, Node>();
		HashMap<Node, FibonacciHeapNode<Node>> priorityQMap2 = new HashMap<Node, FibonacciHeapNode<Node>>();

		int leftcount = 0;

		int i, len = graphTDSP.length, j, id;
		// System.out.println("len="+len);
		int w, arrTime;
		int[] c = new int[len];
		int[] parent = new int[len];
		// double shortestTime = 8000000;

		int[] nextNode = new int[len];

		double hdistance = distance(nodes[start][0], nodes[start][1],
				nodes[end][0], nodes[end][1]);
		int htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);

		for (i = 0; i < len; i++) {
			nextNode[i] = -1;
			parent[i] = -1;
			if (i == start)
				c[i] = 0 + htime; // starting node
			else
				c[i] = 8000000; // indicating infinity
		}

		Node n, s = new Node(start, c[start], 0); // creating the starting
		// node with nodeId =
		// start and cost = 0
		// and arrival time =
		// time
		FibonacciHeapNode<Node> new_node = new FibonacciHeapNode<Node>(s);
		priorityQ.insert(new_node, s.getNodeCost()); // inserting s into the
														// priority queue
		priorityQMap1.put(s.getNodeId(), s);
		priorityQMap2.put(s, new_node);

		while (!(priorityQ.isEmpty())) { // while Q is not empty

			// System.out.println("Size of queue="+priorityQ.size());
			FibonacciHeapNode<Node> node = priorityQ.min();
			priorityQ.removeMin();
			n = node.getData();

			n = priorityQMap1.get(n.getNodeId());
			priorityQMap1.remove(n.getNodeId());
			priorityQMap2.remove(n);

			int updTime = time;
			id = n.getNodeId();
			if (!reverseList.containsKey(id))
				continue;

			if (n.getNodeId() == end) {
				// shortestTime = n.getArrTime();
				break;
			}

			if (n.getNodeId() != start) {
				updTime = time - getTime(n.getArrTime());

				if (updTime < 0)
					updTime = 0;
			}

			if (reverseList.containsKey(id)) {
				// HashMap<Integer, NodeValues> hmap =
				// graphTDSP[id][dayIndex].nodes;
				ArrayList hmap = reverseList.get(id);

				// Set<Integer> keys = hmap.keySet();
				// Iterator<Integer> iter = keys.iterator();
				// System.out.println("size->"+keys.size());
				int i2 = 0;
				while (i2 < hmap.size()) {
					int key = (Integer) hmap.get(i2);
					// NodeValues val = hmap.get(key);
					NodeValues val = graphTDSP[key][dayIndex].nodes.get(id);
					arrTime = n.getArrTime();
					if (eventLU.containsKey(key + "," + id + "," + dayIndex
							+ "," + updTime)) {
						// double location0 =
						// Double.parseDouble(eventLU.get(key+","+id+","+dayIndex+","+updTime).split(",")[0]);
						// double location1 =
						// Double.parseDouble(eventLU.get(key+","+id+","+dayIndex+","+updTime).split(",")[1]);
						accidentdisplay.put(
								eventLU.get(key + "," + id + "," + dayIndex
										+ "," + updTime), 1);
					}
					w = val.getValues()[updTime];

					// left turn penalty test
					// int tail_id = 0;
					if (parent[id] != -1) {
						int id0 = parent[id];
						int id1 = id;
						int id2 = key;

						// tail_id = parent[id];
						double tail_st_x = nodes[id0][1] * 100.;
						double tail_st_y = nodes[id0][0] * 100.;
						double tail_end_x = nodes[id1][1] * 100.;
						double tail_end_y = nodes[id1][0] * 100.;

						double A = tail_end_y - tail_st_y;
						double B = -(tail_end_x - tail_st_x);
						double C = (tail_end_x - tail_st_x) * tail_st_y
								- (tail_end_y - tail_st_y) * tail_st_x;

						double x = nodes[id2][1] * 100.;
						double y = nodes[id2][0] * 100.;

						double result = A * x + B * y + C;

						if (result > 0) {

							double x1 = tail_end_x - tail_st_x;
							double y1 = tail_end_y - tail_st_y;
							double x2 = x - tail_end_x;
							double y2 = y - tail_end_y;

							double cosTheta = (x1 * x2 + y1 * y2)
									/ (Math.sqrt(x1 * x1 + y1 * y1) * Math
											.sqrt(x2 * x2 + y2 * y2));

							if (cosTheta < 0.5)
								w = w + 1 * 60000;
						}

					}
					// left turn penalty ends

					// g
					hdistance = distance(nodes[key][0], nodes[key][1],
							nodes[end][0], nodes[end][1]);
					htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);
					// g ends

					// System.out.println("cost="+w);
					if (arrTime + w + htime < c[key]) {
						c[key] = arrTime + w + htime;
						parent[key] = id;
						// it = priorityQ.iterator();

						// System.out.println("Size of queue="+priorityQ.size());

						if (priorityQMap1.containsKey(key)) {
							Node node_ = priorityQMap1.get(key);
							node_.setArrTime(c[key] - htime);
							node_.setNodeCost(c[key]);
							priorityQMap1.put(key, node_);
							// priorityQ.
							priorityQ.decreaseKey(priorityQMap2.get(node_),
									node_.getNodeCost());
						} else {
							Node node2 = new Node(key, c[key], c[key] - htime);
							FibonacciHeapNode<Node> new_node2 = new FibonacciHeapNode<Node>(
									node2);
							priorityQ.insert(new_node2, c[key]); // arrival time
																	// = c[i]
							priorityQMap1.put(key, node2);
							priorityQMap2.put(node2, new_node2);
						}

					}

					i2++;

				}
			}
		}

		int temp;
		temp = end;
		while (temp != -1) {
			if (parent[temp] != -1)
				nextNode[parent[temp]] = temp;
			temp = parent[temp];
		}

		singleDistance = 0.;
		double[][] pair = new double[2][2];
		int cur = 1;

		if (start == end) {
			// out.println("Your starting node is the same as your ending node.");
			// out.println(""+nodes[start][0]+","+nodes[start][1]+";0");

			servletOutput += "" + nodes[start][0] + "," + nodes[start][1]
					+ ";0";
			// nodesInfo.append(""+nodes[ nextNode[i]][0]+","+nodes[
			// nextNode[i]][1] + ";");
			tempPath.add("" + nodes[start][0] + "," + nodes[start][1] + ";0");
			singleDistance = 0.;
			return servletOutput;
		} else {
			i = start;
			j = 1;
			// out.print(""+nodes[i][0]+","+nodes[i][1]+";");
			// out.print(""+gpsEnd[0]+","+gpsEnd[1]+";");
			servletOutput += "" + gpsEnd[0] + "," + gpsEnd[1] + ";";

			// nodesInfo.append(""+nodes[i][0]+","+nodes[i][1]+";");
			tempPath.add("" + nodes[i][0] + "," + nodes[i][1] + ";");

			pair[0][0] = nodes[i][0];
			pair[0][1] = nodes[i][1];

			while (i != end && nextNode[i] != -1) {
				if (nextNode[i] == end) {
					// out.print(""+gpsSt[0]+","+gpsSt[1] + ";");
					servletOutput += "" + gpsSt[0] + "," + gpsSt[1] + ";";
				} else {
					// out.print(""+nodes[ nextNode[i]][0]+","+nodes[
					// nextNode[i]][1] + ";");
					servletOutput += "" + nodes[nextNode[i]][0] + ","
							+ nodes[nextNode[i]][1] + ";";
				}

				// nodesInfo.append(""+nodes[ nextNode[i]][0]+","+nodes[
				// nextNode[i]][1] + ";");
				tempPath.add("" + nodes[nextNode[i]][0] + ","
						+ nodes[nextNode[i]][1] + ";");

				pair[cur][0] = nodes[nextNode[i]][0];
				pair[cur][1] = nodes[nextNode[i]][1];
				singleDistance += DistanceCalculator.CalculationByDistance(
						new PairInfo(pair[0][0], pair[0][1]), new PairInfo(
								pair[1][0], pair[1][1]));
				// cur = 1-cur;
				if (cur == 1)
					cur = 0;
				else
					cur = 1;

				i = nextNode[i];
			}
			// out.print(""+((double)c[end]/60000.0+leftcount));
			servletOutput += "" + ((double) c[end] / 60000.0 + leftcount);
			// nodesInfo.append(""+(double)c[end]/60000.0);
			tempPath.add("" + ((double) c[end] / 60000.0 + leftcount));
		}

		// put all the information back to
		for (int p = tempPath.size() - 2; p >= 0; p--) {
			nodesInfo.append(tempPath.get(p));
		}
		nodesInfo.append(tempPath.get(tempPath.size() - 1));
		
		return servletOutput;
	}

	private String tdspArrivalWithCP(int start, int end, int time) {

		String servletOutput = "";

		ArrayList<String> tempPath = new ArrayList<String>();
		// System.out.println("TDSP CALLED WITH PARAMETERS:"+start+","+end+","+time+","+maxTime);

		FibonacciHeap<Node> priorityQ = new FibonacciHeap<Node>();
		HashMap<Integer, Node> priorityQMap1 = new HashMap<Integer, Node>();
		HashMap<Node, FibonacciHeapNode<Node>> priorityQMap2 = new HashMap<Node, FibonacciHeapNode<Node>>();

		int leftcount = 0;

		int i, len = graphTDSP.length, j, id;
		// System.out.println("len="+len);
		int w, arrTime;
		int[] c = new int[len];
		int[] parent = new int[len];
		// double shortestTime = 8000000;

		int[] nextNode = new int[len];

		double hdistance = distance(nodes[start][0], nodes[start][1],
				nodes[end][0], nodes[end][1]);
		int htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);

		for (i = 0; i < len; i++) {
			nextNode[i] = -1;
			parent[i] = -1;
			if (i == start)
				c[i] = 0 + htime; // starting node
			else
				c[i] = 8000000; // indicating infinity
		}

		Node n, s = new Node(start, c[start], 0); // creating the starting
		// node with nodeId =
		// start and cost = 0
		// and arrival time =
		// time
		FibonacciHeapNode<Node> new_node = new FibonacciHeapNode<Node>(s);
		priorityQ.insert(new_node, s.getNodeCost()); // inserting s into the
														// priority queue
		priorityQMap1.put(s.getNodeId(), s);
		priorityQMap2.put(s, new_node);

		while (!(priorityQ.isEmpty())) { // while Q is not empty

			// System.out.println("Size of queue="+priorityQ.size());
			FibonacciHeapNode<Node> node = priorityQ.min();
			priorityQ.removeMin();
			n = node.getData();

			n = priorityQMap1.get(n.getNodeId());
			priorityQMap1.remove(n.getNodeId());
			priorityQMap2.remove(n);

			int updTime = time;
			id = n.getNodeId();
			if (!reverseList.containsKey(id))
				continue;

			if (n.getNodeId() == end) {
				// shortestTime = n.getArrTime();
				break;
			}

			if (n.getNodeId() != start) {
				updTime = time - getTime(n.getArrTime());

				if (updTime < 0)
					updTime = 0;
			}

			if (reverseList.containsKey(id)) {
				// HashMap<Integer, NodeValues> hmap =
				// graphTDSP[id][dayIndex].nodes;
				ArrayList hmap = reverseList.get(id);

				// Set<Integer> keys = hmap.keySet();
				// Iterator<Integer> iter = keys.iterator();
				// System.out.println("size->"+keys.size());
				int i2 = 0;
				while (i2 < hmap.size()) {
					int key = (Integer) hmap.get(i2);
					// NodeValues val = hmap.get(key);
					NodeValues val = graphTDSP[key][dayIndex].nodes.get(id);
					arrTime = n.getArrTime();
					if (eventLU.containsKey(key + "," + id + "," + dayIndex
							+ "," + updTime)) {
						// double location0 =
						// Double.parseDouble(eventLU.get(key+","+id+","+dayIndex+","+updTime).split(",")[0]);
						// double location1 =
						// Double.parseDouble(eventLU.get(key+","+id+","+dayIndex+","+updTime).split(",")[1]);
						accidentdisplay.put(
								eventLU.get(key + "," + id + "," + dayIndex
										+ "," + updTime), 1);
					}

					if (CarpoolLU.containsKey("n" + key + "," + "n" + id)) {
						String carpoolstring = streets.get("n" + key + ","
								+ "n" + id);
						String[] carpoolnodes = carpoolstring.split(",");
						double carpooldistance = Double
								.parseDouble(carpoolnodes[10]);
						if ((updTime >= 6 && updTime <= 12)
								|| (updTime >= 42 && updTime <= 48))
							w = (int) ((carpooldistance * 60.0 * 60.0 * 1000.0) / 60.);
						else
							w = (int) ((carpooldistance * 60.0 * 60.0 * 1000.0) / 75.);
					} else
						w = val.getValues()[updTime];

					// left turn penalty test
					// int tail_id = 0;
					if (parent[id] != -1) {
						int id0 = parent[id];
						int id1 = id;
						int id2 = key;

						// tail_id = parent[id];
						double tail_st_x = nodes[id0][1] * 100.;
						double tail_st_y = nodes[id0][0] * 100.;
						double tail_end_x = nodes[id1][1] * 100.;
						double tail_end_y = nodes[id1][0] * 100.;

						double A = tail_end_y - tail_st_y;
						double B = -(tail_end_x - tail_st_x);
						double C = (tail_end_x - tail_st_x) * tail_st_y
								- (tail_end_y - tail_st_y) * tail_st_x;

						double x = nodes[id2][1] * 100.;
						double y = nodes[id2][0] * 100.;

						double result = A * x + B * y + C;

						if (result > 0) {

							double x1 = tail_end_x - tail_st_x;
							double y1 = tail_end_y - tail_st_y;
							double x2 = x - tail_end_x;
							double y2 = y - tail_end_y;

							double cosTheta = (x1 * x2 + y1 * y2)
									/ (Math.sqrt(x1 * x1 + y1 * y1) * Math
											.sqrt(x2 * x2 + y2 * y2));

							if (cosTheta < 0.5)
								w = w + 1 * 60000;
						}

					}
					// left turn penalty ends

					// g
					hdistance = distance(nodes[key][0], nodes[key][1],
							nodes[end][0], nodes[end][1]);
					htime = (int) ((hdistance * 60.0 * 60.0 * 1000.0) / 60);
					// g ends

					// System.out.println("cost="+w);
					if (arrTime + w + htime < c[key]) {
						c[key] = arrTime + w + htime;
						parent[key] = id;
						// it = priorityQ.iterator();

						// System.out.println("Size of queue="+priorityQ.size());

						if (priorityQMap1.containsKey(key)) {
							Node node_ = priorityQMap1.get(key);
							node_.setArrTime(c[key] - htime);
							node_.setNodeCost(c[key]);
							priorityQMap1.put(key, node_);
							// priorityQ.
							priorityQ.decreaseKey(priorityQMap2.get(node_),
									node_.getNodeCost());
						} else {
							Node node2 = new Node(key, c[key], c[key] - htime);
							FibonacciHeapNode<Node> new_node2 = new FibonacciHeapNode<Node>(
									node2);
							priorityQ.insert(new_node2, c[key]); // arrival time
																	// = c[i]
							priorityQMap1.put(key, node2);
							priorityQMap2.put(node2, new_node2);
						}

					}

					i2++;

				}
			}
		}

		int temp;
		temp = end;
		while (temp != -1) {
			if (parent[temp] != -1)
				nextNode[parent[temp]] = temp;
			temp = parent[temp];
		}

		singleDistance = 0.;
		double[][] pair = new double[2][2];
		int cur = 1;

		if (start == end) {
			// out.println("Your starting node is the same as your ending node.");
			// out.println(""+nodes[start][0]+","+nodes[start][1]+";0");
			servletOutput += "" + nodes[start][0] + "," + nodes[start][1]
					+ ";0";

			// nodesInfo.append(""+nodes[ nextNode[i]][0]+","+nodes[
			// nextNode[i]][1] + ";");
			tempPath.add("" + nodes[start][0] + "," + nodes[start][1] + ";0");
			singleDistance = 0.;
			return servletOutput;
		} else {
			i = start;
			j = 1;
			// out.print(""+nodes[i][0]+","+nodes[i][1]+";");
			// out.print(""+gpsEnd[0]+","+gpsEnd[1]+";");
			servletOutput += "" + gpsEnd[0] + "," + gpsEnd[1] + ";";
			// nodesInfo.append(""+nodes[i][0]+","+nodes[i][1]+";");
			tempPath.add("" + nodes[i][0] + "," + nodes[i][1] + ";");

			pair[0][0] = nodes[i][0];
			pair[0][1] = nodes[i][1];

			while (i != end && nextNode[i] != -1) {
				if (nextNode[i] == end) {
					// out.print(""+gpsSt[0]+","+gpsSt[1] + ";");
					servletOutput += "" + gpsSt[0] + "," + gpsSt[1] + ";";
				} else {
					// out.print(""+nodes[ nextNode[i]][0]+","+nodes[
					// nextNode[i]][1] + ";");
					servletOutput += "" + nodes[nextNode[i]][0] + ","
							+ nodes[nextNode[i]][1] + ";";
				}

				// nodesInfo.append(""+nodes[ nextNode[i]][0]+","+nodes[
				// nextNode[i]][1] + ";");
				tempPath.add("" + nodes[nextNode[i]][0] + ","
						+ nodes[nextNode[i]][1] + ";");

				pair[cur][0] = nodes[nextNode[i]][0];
				pair[cur][1] = nodes[nextNode[i]][1];
				singleDistance += DistanceCalculator.CalculationByDistance(
						new PairInfo(pair[0][0], pair[0][1]), new PairInfo(
								pair[1][0], pair[1][1]));
				// cur = 1-cur;
				if (cur == 1)
					cur = 0;
				else
					cur = 1;

				i = nextNode[i];
			}
			// out.print(""+((double)c[end]/60000.0+leftcount));
			servletOutput += "" + ((double) c[end] / 60000.0 + leftcount);
			// nodesInfo.append(""+(double)c[end]/60000.0);
			tempPath.add("" + ((double) c[end] / 60000.0 + leftcount));
		}

		// put all the information back to
		for (int p = tempPath.size() - 2; p >= 0; p--) {
			nodesInfo.append(tempPath.get(p));
		}
		nodesInfo.append(tempPath.get(tempPath.size() - 1));
		
		return servletOutput;
	}

	public String realtimeModify() {
		String servletOutput = "";

		try {
			InputStream is = realtimeTableStream;
			DataInputStream in = new DataInputStream(is);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// int stID = 0;
			// out.print("file opened\n");
			servletOutput += "file opened\n";
			while ((strLine = br.readLine()) != null) {
				String[] nodes = strLine.split(",");
				int startNode = Integer.parseInt(nodes[0].substring(1));
				int endNode = Integer.parseInt(nodes[1].substring(1));
				int dayID = Integer.parseInt(nodes[2]);
				int timeID = Integer.parseInt(nodes[3]);
				int time = Integer.parseInt(nodes[4]);

				int values[] = new int[60];
				values = graphTDSP[startNode][dayID].nodes.get(endNode)
						.getValues();
				values[timeID] = time;
				graphTDSP[startNode][dayID].nodes.remove(endNode);
				graphTDSP[startNode][dayID].nodes.put(endNode, new NodeValues(
						endNode, values));
			}
			br.close();
			in.close();
			is.close();

			// out.print("updated\n");
			servletOutput += "updated\n";
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return servletOutput;
	}

	public String processSuperQuery6() {
		link_count = 0;
		int startNodeID;
		int endNodeID;
		String servletOutput = "";

		try {

			if ("False1".equals(request.getUpdate())&& "False".equals(request.getCarPool())) {// Departure time without carpool

				// add Ending points
				EndingNode startEnd = new EndingNode();
				EndingNode endEnd = new EndingNode();
				// add Ending points ends

				day = request.getDay();
				for (int i = 0; i < days.length; i++) {
					if (days[i].equals(day))
						dayIndex = i;
				}

				String[] startOrds = request.getStart().split(",");
				String[] endOrds = request.getEnd().split(",");
				startNodeID = findNN1(Double.parseDouble(startOrds[0]),
						Double.parseDouble(startOrds[1]), startEnd, endEnd);//skannan-comment: returns the node ID, mapped in nodes.csv file in Files 12345 folder
				endNodeID = findNN2(Double.parseDouble(endOrds[0]),
						Double.parseDouble(endOrds[1]), startEnd, endEnd);

				gpsSt[0] = Double.parseDouble(startOrds[0]);
				gpsSt[1] = Double.parseDouble(startOrds[1]);

				gpsEnd[0] = Double.parseDouble(endOrds[0]);
				gpsEnd[1] = Double.parseDouble(endOrds[1]);

				int time = Integer.parseInt(request.getTime());

				// add
				if (nodesInfo.length() != 0)
					nodesInfo.delete(0, nodesInfo.length());
				if (pathInfo.size() != 0)
					pathInfo.clear();
				// add ends
				// load accidents
				// accidentModifyForQuery();
				// tdsp0A(startNodeID, endNodeID, time);

				// out.print("startEnd Info: "+startEnd.getLat()+","+startEnd.getLon()+"\n");
				// out.print("EndEnd Info: "+endEnd.getLat()+","+endEnd.getLon()+"\n");
				// out.print("enter tdspA\n");

				servletOutput += tdspA(startNodeID, endNodeID, time, startEnd, endEnd);

				// out.print("exit tdspA\n");

				// out.print("-"+startNodeID+"-"+endNodeID+"-"+time+"-"+singleDistance);
				servletOutput += "-" + startNodeID + "-" + endNodeID + "-"
						+ time + "-" + singleDistance;

				nodesInfo.append("-" + startNodeID + "-" + endNodeID + "-"
						+ time);

				findPathInfo(startEnd, endEnd);
				servletOutput += turn_by_turn_new();

				servletOutput += showaccident(startNodeID, endNodeID);
				accidentdisplay.clear();// copied from original

				/*
				 * //test only String[] nodes = nodesInfo.toString().split(";");
				 * out.write("\n"); out.write("nodesInfo:\n"); for(int i=0;
				 * i<nodes.length;i++){ out.write(nodes[i]+"\n"); }
				 * 
				 * out.write("startEndRoadInfo: "+startEnd.getstInfo()+"\n");
				 * out.write("endEndRoadInfo: "+endEnd.getstInfo()+"\n");
				 * out.write("starthitIn = "+starthitIn+"\n");
				 * out.write("starthitOut = "+starthitOut+"\n");
				 * out.write("endhitIn = "+endhitIn+"\n");
				 * out.write("ednhitOut = "+endhitOut+"\n");
				 * out.write("lastlineIn = "+lastlineIn+"\n");
				 * out.write("lastputIn = "+lastputIn+"\n");
				 * out.write("lastlineOut = "+lastlineOut+"\n");
				 * out.write("lastputOut = "+lastputOut+"\n");
				 * out.write("the info inside the findPathInfo block:\n");
				 * out.write(findbuf.toString());
				 * 
				 * out.write("\npathInfo:\n"); for(int i = 0; i <
				 * pathInfo.size(); i++){ out.write(pathInfo.get(i)+"\n");
				 * 
				 * } //test ends
				 */

				if (nodesInfo.length() != 0)
					nodesInfo.delete(0, nodesInfo.length());
				if (pathInfo.size() != 0)
					pathInfo.clear();

			} else if ("False1".equals(request.getUpdate())	&& "True".equals(request.getCarPool())) {// departure time & Carpool
				day = request.getDay();
				for (int i = 0; i < days.length; i++) {
					if (days[i].equals(day))
						dayIndex = i;
				}

				String[] startOrds = request.getStart().split(",");
				String[] endOrds = request.getEnd().split(",");
				startNodeID = findNN(Double.parseDouble(startOrds[0]),
						Double.parseDouble(startOrds[1]));
				endNodeID = findNN(Double.parseDouble(endOrds[0]),
						Double.parseDouble(endOrds[1]));

				gpsSt[0] = Double.parseDouble(startOrds[0]);
				gpsSt[1] = Double.parseDouble(startOrds[1]);

				gpsEnd[0] = Double.parseDouble(endOrds[0]);
				gpsEnd[1] = Double.parseDouble(endOrds[1]);

				int time = Integer.parseInt(request.getTime());
				// checkmode
				int carpoolMode = HOVVSNOHOV(startNodeID, endNodeID, time);
				// out.print("carpoolMode ="+carpoolMode+"\n");

				// checkmode ends

				// add
				if (nodesInfo.length() != 0)
					nodesInfo.delete(0, nodesInfo.length());
				if (pathInfo.size() != 0)
					pathInfo.clear();
				// add ends
				// load accidents
				// accidentModifyForQuery();
				// tdsp0A(startNodeID, endNodeID, time);
				if (carpoolMode == 0)
					servletOutput += tdspA_old(startNodeID, endNodeID, time);
				else
					servletOutput += tdspAWithCP(startNodeID, endNodeID, time);
				// out.print("-"+startNodeID+"-"+endNodeID+"-"+time+"-"+singleDistance);
				servletOutput += "-" + startNodeID + "-" + endNodeID + "-"
						+ time + "-" + singleDistance;

				nodesInfo.append("-" + startNodeID + "-" + endNodeID + "-"
						+ time);

				findPathInfo();
				servletOutput += turn_by_turn_new();

				servletOutput += showaccident(startNodeID, endNodeID);
				accidentdisplay.clear();

				// add
				// accidentPath.clear();

				if (nodesInfo.length() != 0)
					nodesInfo.delete(0, nodesInfo.length());
				if (pathInfo.size() != 0)
					pathInfo.clear();
				// add ends
			} else if ("False2".equals(request.getUpdate())
					&& "False".equals(request.getCarPool())) { // arrival time
																// without
																// Carpool
				day = request.getDay();
				for (int i = 0; i < days.length; i++) {
					if (days[i].equals(day))
						dayIndex = i;
				}

				String[] startOrds = request.getStart().split(",");
				String[] endOrds = request.getEnd().split(",");
				startNodeID = findNN(Double.parseDouble(startOrds[0]),
						Double.parseDouble(startOrds[1]));
				endNodeID = findNN(Double.parseDouble(endOrds[0]),
						Double.parseDouble(endOrds[1]));

				gpsSt[0] = Double.parseDouble(startOrds[0]);
				gpsSt[1] = Double.parseDouble(startOrds[1]);

				gpsEnd[0] = Double.parseDouble(endOrds[0]);
				gpsEnd[1] = Double.parseDouble(endOrds[1]);

				int time = Integer.parseInt(request.getTime());

				// add
				if (nodesInfo.length() != 0)
					nodesInfo.delete(0, nodesInfo.length());
				if (pathInfo.size() != 0)
					pathInfo.clear();
				// add ends
				// load accidents
				// accidentModifyForQuery();
				// tdsp0A(startNodeID, endNodeID, time);
				servletOutput += tdspArrival(endNodeID, startNodeID, time);
				// out.print("-"+startNodeID+"-"+endNodeID+"-"+time+"-"+singleDistance);
				servletOutput += "-" + startNodeID + "-" + endNodeID + "-"
						+ time + "-" + singleDistance;

				nodesInfo.append("-" + startNodeID + "-" + endNodeID + "-"
						+ time);

				findPathInfo();
				servletOutput += turn_by_turn_new();

				servletOutput += showaccident(startNodeID, endNodeID);
				accidentdisplay.clear();

				// add
				// accidentPath.clear();

				if (nodesInfo.length() != 0)
					nodesInfo.delete(0, nodesInfo.length());
				if (pathInfo.size() != 0)
					pathInfo.clear();
				// add ends

			} else if ("False2".equals(request.getUpdate())
					&& "True".equals(request.getCarPool())) {// arrival time &
																// Carpool
				day = request.getDay();
				for (int i = 0; i < days.length; i++) {
					if (days[i].equals(day))
						dayIndex = i;
				}

				String[] startOrds = request.getStart().split(",");
				String[] endOrds = request.getEnd().split(",");
				startNodeID = findNN(Double.parseDouble(startOrds[0]),
						Double.parseDouble(startOrds[1]));
				endNodeID = findNN(Double.parseDouble(endOrds[0]),
						Double.parseDouble(endOrds[1]));

				gpsSt[0] = Double.parseDouble(startOrds[0]);
				gpsSt[1] = Double.parseDouble(startOrds[1]);

				gpsEnd[0] = Double.parseDouble(endOrds[0]);
				gpsEnd[1] = Double.parseDouble(endOrds[1]);

				int time = Integer.parseInt(request.getTime());

				// add
				if (nodesInfo.length() != 0)
					nodesInfo.delete(0, nodesInfo.length());
				if (pathInfo.size() != 0)
					pathInfo.clear();
				// add ends
				// load accidents
				// accidentModifyForQuery();
				// tdsp0A(startNodeID, endNodeID, time);
				servletOutput += tdspArrivalWithCP(endNodeID, startNodeID, time);
				// out.print("-"+startNodeID+"-"+endNodeID+"-"+time+"-"+singleDistance);
				servletOutput += "-" + startNodeID + "-" + endNodeID + "-"
						+ time + "-" + singleDistance;

				nodesInfo.append("-" + startNodeID + "-" + endNodeID + "-"
						+ time);

				findPathInfo();
				servletOutput += turn_by_turn_new();

				servletOutput += showaccident(startNodeID, endNodeID);
				accidentdisplay.clear();

				// add
				// accidentPath.clear();

				if (nodesInfo.length() != 0)
					nodesInfo.delete(0, nodesInfo.length());
				if (pathInfo.size() != 0)
					pathInfo.clear();
				// add ends

			} else if ("True1".equals(request.getUpdate())) {
				servletOutput += accidentModify();
				// out.print("accident finished\n");
				servletOutput += "accident finished\n";

			} else if ("True2".equals(request.getUpdate())) {
				dayIndex = Integer.parseInt(request.getDay());
				timeIndex = Integer.parseInt(request.getTime());
				if (timeIndex > 59)
					timeIndex = 59;

				servletOutput += compare();
				// out.print("file generated, begin to update realtime memory\n");
				servletOutput += "file generated, begin to update realtime memory\n";

				servletOutput += realtimeModify();
				// out.print("realtime update finished\n");
				servletOutput += "realtime update finished\n";
			}

			// out.close();
		} catch (Exception e) {
			e.printStackTrace();
			// out.print("error");
		}
//		System.out.println(servletOutput);
		return servletOutput;
	}
}
