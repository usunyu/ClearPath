package highwayPattern;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;
import Objects.*;

public class highwayPatternGeneration {

	/**
	 * @param args
	 */
	// file
	static String root = "file";
	static String highwayLinkFile = "Highway_Link_List.txt";
	static String highwayLinkKML = "Highway_Link_List.kml";
	static String highwaySensorKML = "Highway_Sensor_List.kml";
	// param
	static double searchDistance = 0.05;
	static int devide = 10;
	// database
	static String urlHome = "jdbc:oracle:thin:@geodb.usc.edu:1521/geodbs";
	static String userName = "clearp";
	static String password = "clearp";
	static Connection connHome = null;
	// data struct
	// link
	static ArrayList<Integer> linkIdList = new ArrayList<Integer>();
	static ArrayList<LinkInfo> linkList = new ArrayList<LinkInfo>();
	static HashMap<Integer, LinkInfo> linkMap = new HashMap<Integer, LinkInfo>();
	// node
	static HashMap<Integer, PairInfo> nodePositionMap = new HashMap<Integer, PairInfo>();
	// sensor
	static HashSet<Integer> checkSensor = new HashSet<Integer>();
	static ArrayList<SensorInfo> sensorList = new ArrayList<SensorInfo>();
	static ArrayList<SensorInfo> matchSensorList = new ArrayList<SensorInfo>();
	static HashSet<Integer> checkMatchSensor = new HashSet<Integer>();

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// fetchLinkId();
		// fetchLink();
		// writeLinkFile();
		readLinkFile();
		fetchSensor();
		matchLinkSensor();
		generateLinkKML();
		generateSensorKML();
	}
	
	private static void matchLinkSensor() {
		System.out.println("match sensors to links...");
		for (int i = 0; i < linkList.size(); i++) {
			LinkInfo link = linkList.get(i);
			ArrayList<PairInfo> nodeList = link.getNodeList();
			for (double step = searchDistance / devide; step < searchDistance; step += step) {
				for (int j = 0; j < nodeList.size(); j++) {
					PairInfo node1 = nodeList.get(j);
					for (int k = 0; k < sensorList.size(); k++) {
						SensorInfo sensor = sensorList.get(k);
						PairInfo node2 = sensor.getNode();
						// same direction
						String allDir = link.getAllDir();
						String[] dirList = allDir.split(",");
						for(int d = 0; d < dirList.length; d++) {
							if(sensor.getDirection() == Integer.parseInt(dirList[d])) {
								double distance = DistanceCalculator.CalculationByDistance(node1, node2);
								// in the search area
								if (distance < step) {
									// match sensor
									if(!link.containSensor(sensor))
										link.addSensor(sensor);
									if(!checkMatchSensor.contains(sensor.getSensorId())) {
										matchSensorList.add(sensor);
										checkMatchSensor.add(sensor.getSensorId());
									}
								}
							}
						}
					}
				}
			}
			if(i % 100 == 0)
				System.out.println((float)i / linkList.size() * 100 + "%");
		}
		System.out.println("match Sensors finish!");
	}
	
	private static void generateSensorKML() {
		System.out.println("generate sensor kml...");
		try {
			FileWriter fstream = new FileWriter(root + "/" + highwaySensorKML);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("<kml><Document>");
			for (int i = 0; i < matchSensorList.size(); i++) {
				SensorInfo sensor = matchSensorList.get(i);
				int id = sensor.getSensorId();
				double lati = sensor.getNode().getLati();
				double longi = sensor.getNode().getLongi();
				out.write("<Placemark><name>" + id + "</name><description>On:"
						+ sensor.getOnStreet() + ", From: "
						+ sensor.getFromStreet() + ", Dir:"
						+ sensor.getDirection()
						+ "</description><Point><coordinates>" + longi + ","
						+ lati + ",0</coordinates></Point></Placemark>");
			}
			out.write("</Document></kml>");
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("generate sensor kml finish!");
	}

	private static void readLinkFile() {
		System.out.println("fetch link file...");
		try {
			FileInputStream fstream = new FileInputStream(root + "/"
					+ highwayLinkFile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				String[] nodes = strLine.split(";");
				int linkId = Integer.parseInt(nodes[0]);
				String dir = nodes[1];
				String name = nodes[2];
				int funClass = Integer.parseInt(nodes[3]);
				String nodeListString = nodes[4];
				ArrayList<PairInfo> nodeList = new ArrayList<PairInfo>();
				String[] nodeString = nodeListString.split(":");
				for (int i = 0; i < nodeString.length; i++) {
					String[] loc = nodeString[i].split(",");
					PairInfo node = new PairInfo(Double.parseDouble(loc[1]),
							Double.parseDouble(loc[0]));
					nodeList.add(node);
				}
				int speedCat = Integer.parseInt(nodes[5]);
				String dirTravel = nodes[6];
				int refNode = Integer.parseInt(nodes[7]);
				int nrefNode = Integer.parseInt(nodes[8]);

				if (!nodePositionMap.containsKey(refNode))
					nodePositionMap.put(refNode, nodeList.get(0));
				if (!nodePositionMap.containsKey(nrefNode))
					nodePositionMap.put(nrefNode, nodeList.get(1));

				LinkInfo link = new LinkInfo(linkId, funClass, name, refNode,
						nrefNode, nodeList, dirTravel, speedCat, dir);
				linkList.add(link);
				linkMap.put(linkId, link);

			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		System.out.println("fetch link file finish!");
	}

	private static void fetchSensor() {
		System.out.println("fetch sensor...");
		try {
			Connection con = null;
			String sql = null;
			PreparedStatement pstatement = null;
			ResultSet res = null;
			con = getConnection();
			sql = "select link_id, onstreet, fromstreet, start_lat_long, direction from highway_congestion_config ";
			pstatement = con.prepareStatement(sql,
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			res = pstatement.executeQuery();
			while (res.next()) {
				int sensorId = res.getInt(1);
				// System.out.println("fetching sensor " + sensorId);
				String onStreet = res.getString(2);
				String fromStreet = res.getString(3);
				STRUCT st = (STRUCT) res.getObject(4);
				JGeometry geom = JGeometry.load(st);
				PairInfo node = new PairInfo(geom.getPoint()[1],
						geom.getPoint()[0]);
				int direction = res.getInt(5);
				if (!checkSensor.contains(sensorId)) {
					SensorInfo sensorInfo = new SensorInfo(sensorId, onStreet,
							fromStreet, node, direction);
					checkSensor.add(sensorId);
					sensorList.add(sensorInfo);
				}
			}
			res.close();
			pstatement.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("fetch sensor finish!");
	}

	private static void generateLinkKML() {
		System.out.println("generate link kml...");
		try {
			FileWriter fstream = new FileWriter(root + "/" + highwayLinkKML);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("<kml><Document>");
			for (int i = 0; i < linkList.size(); i++) {
				LinkInfo link = linkList.get(i);
				int intId = link.getIntLinkId();
				SensorInfo sensor = link.getSensor();
				String sensorStr = "NULL";
				ArrayList<PairInfo> nodeList = link.getNodeList();
				if (sensor != null) {
					sensorStr = String.valueOf(sensor.getSensorId());
				}

				String kmlStr = "<Placemark><name>Link:" + intId + "</name>";
				kmlStr += "<description>";
				kmlStr += "Sensor:" + sensorStr;
				kmlStr += ", Name:" + link.getSt_name();
				kmlStr += ", Start:" + link.getStartNode();
				kmlStr += ", End:" + link.getEndNode();
				kmlStr += ", Dir:" + link.getAllDir();
				kmlStr += ", Func:" + link.getFunc_class();
				kmlStr += ", Speed:" + link.getSpeedCat() + "</description>";
				kmlStr += "<LineString><tessellate>1</tessellate><coordinates>";
				for (int j = 0; j < nodeList.size(); j++) {
					PairInfo node = nodeList.get(j);
					kmlStr += node.getLongi() + "," + node.getLati() + ",0 ";
				}
				kmlStr += "</coordinates></LineString></Placemark>\n";

				out.write(kmlStr);
			}
			out.write("</Document></kml>");
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("generate link kml finish!");
	}

	private static void writeLinkFile() {
		System.out.println("write link file...");
		try {
			FileWriter fstream = new FileWriter(root + "/" + highwayLinkFile);
			BufferedWriter out = new BufferedWriter(fstream);
			for (int i = 0; i < linkList.size(); i++) {
				LinkInfo link = linkList.get(i);
				ArrayList<PairInfo> nodeList = link.getNodeList();
				int num = nodeList.size();
				String str = Integer.toString(link.getIntLinkId()) + ";";
				str = str + link.getAllDir() + ";" + link.getSt_name() + ";";
				str = str + link.getFunc_class() + ";";
				String nodeListString = "";
				for (int j = 0; j < num; j++) {
					String nodeString = nodeList.get(j).getLongi() + ","
							+ nodeList.get(j).getLati();
					if (nodeListString.equals(""))
						nodeListString = nodeString;
					else
						nodeListString = nodeListString + ":" + nodeString;
				}
				str = str + nodeListString + ";";
				str = str + link.getSpeedCat() + ";";
				str = str + link.getDirTravel() + ";";
				str = str + link.getStartNode() + ";";
				str = str + link.getEndNode() + "\r\n";
				out.write(str);
			}
			out.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		System.out.println("write link file finish!");
	}

	private static void fetchLink() {
		System.out.println("fetch link...");
		try {
			Connection con = null;
			String sql = null;
			PreparedStatement pstatement = null;
			ResultSet res = null;
			con = getConnection();

			for (int i = 0; i < linkIdList.size(); i++) {

				int linkId = linkIdList.get(i);
				sql = "select * from streets_dca1_new where link_id = '"
						+ linkId + "'";

				pstatement = con.prepareStatement(sql,
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				res = pstatement.executeQuery();

				res.next();

				String dirTravel = res.getString(2);
				String stName = res.getString(3);
				int func_class = res.getInt(4);
				// handle for geom point
				STRUCT st = (STRUCT) res.getObject(5);
				JGeometry geom = JGeometry.load(st);
				double[] geomPoints = geom.getOrdinatesArray();
				ArrayList<PairInfo> nodeList = new ArrayList<PairInfo>();
				for (int k = 0; k < geom.getNumPoints(); k++) {
					double latitude = geomPoints[k * 2 + 1];
					double longitude = geomPoints[k * 2 + 0];
					PairInfo node = new PairInfo(latitude, longitude);
					nodeList.add(node);
				}

				int speedCat = res.getInt(6);
				int refNode = res.getInt(7);
				int nrefNode = res.getInt(8);

				while (res.next()) {
					stName = stName + "," + res.getString(3);
				}

				int num = nodeList.size();

				if (!nodePositionMap.containsKey(refNode))
					nodePositionMap.put(refNode, nodeList.get(0));
				if (!nodePositionMap.containsKey(nrefNode))
					nodePositionMap.put(nrefNode, nodeList.get(num - 1));

				if (dirTravel.equals("B")) {
					int dir1 = DistanceCalculator.getDirection(nodeList.get(0),
							nodeList.get(num - 1));
					int dir2 = DistanceCalculator.getDirection(
							nodeList.get(num - 1), nodeList.get(0));
					String dir = dir1 + "," + dir2;
					LinkInfo link = new LinkInfo(linkId, func_class, stName,
							refNode, nrefNode, nodeList, dirTravel, speedCat,
							dir);
					linkList.add(link);
					linkMap.put(linkId, link);
				} else if (dirTravel.equals("T")) {
					int dir = DistanceCalculator.getDirection(
							nodeList.get(num - 1), nodeList.get(0));
					String sDir = String.valueOf(dir);
					LinkInfo link = new LinkInfo(linkId, func_class, stName,
							refNode, nrefNode, nodeList, dirTravel, speedCat,
							sDir);
					linkList.add(link);
					linkMap.put(linkId, link);
				} else {
					int dir = DistanceCalculator.getDirection(nodeList.get(0),
							nodeList.get(num - 1));
					String sDir = String.valueOf(dir);
					LinkInfo link = new LinkInfo(linkId, func_class, stName,
							refNode, nrefNode, nodeList, dirTravel, speedCat,
							sDir);
					linkList.add(link);
					linkMap.put(linkId, link);
				}

				if (i % 250 == 0) {
					res.close();
					pstatement.close();
					con.close();

					con = getConnection();

					System.out.println((double) i / linkIdList.size() * 100
							+ "%");
				}
			}
			res.close();
			pstatement.close();
			con.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		System.out.println("fetch link finish!");
	}

	public static void fetchLinkId() {
		System.out.println("fetch link id...");
		try {
			Connection con = null;
			String sql = null;
			PreparedStatement pstatement = null;
			ResultSet res = null;
			con = getConnection();

			sql = "SELECT distinct dc.link_id FROM streets_dca1_new dc, zlevels_new z1, zlevels_new z2"
					+ " where z1.node_id !=0 and z2.node_id !=0 "
					+ " and Ref_In_Id = z1.node_id and nRef_In_Id = z2.node_id "
					+ " and func_class IN (1,2)" + " order by dc.link_id";

			pstatement = con.prepareStatement(sql,
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			res = pstatement.executeQuery();
			while (res.next()) {

				int linkId = res.getInt(1);
				linkIdList.add(linkId);

			}
			res.close();
			pstatement.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("fetch link id finish!");
	}

	private static Connection getConnection() {
		try {
			DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
			connHome = DriverManager.getConnection(urlHome, userName, password);
			return connHome;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return connHome;

	}
}
