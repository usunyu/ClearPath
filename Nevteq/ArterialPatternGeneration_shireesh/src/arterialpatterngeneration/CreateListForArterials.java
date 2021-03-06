/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//1000 created

package arterialpatterngeneration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import Objects.DistanceCalculator;
import Objects.LinkInfo;
import Objects.PairInfo;
import Objects.UtilClass;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreateListForArterials {

	static int numElem = 4575;
	static HashMap<String, LinkInfo> links = new HashMap<String, LinkInfo>();
	static HashMap<Long, LinkInfo> links2 = new HashMap<Long, LinkInfo>();
        static HashMap<Integer,HashMap<Integer,Double []>> links_speed = new HashMap<Integer,HashMap<Integer,Double []>>();

	static int link_count = 0;
	//static ArrayList<AdjList> listTDSP;
	static String url_home = "jdbc:oracle:thin:@geodb.usc.edu:1521/geodbs";
	static String userName = "clearp";
	static String password = "clearp";
	static Connection connHome = null;
	static BufferedWriter out;
	static int noData = 0;
	static int LinkIds[] = new int[numElem];
	static PairInfo pairs[] = new PairInfo[numElem];
	static boolean hasEdge[] = new boolean[numElem];
	static int Direction[] = new int[numElem];
	@SuppressWarnings("unchecked")
	static ArrayList<Double>[] speeds = (ArrayList<Double>[]) new ArrayList[60];
	@SuppressWarnings("unchecked")
	static ArrayList<Integer>[][] sensorsGrid = (ArrayList<Integer>[][]) new ArrayList[100][100];
	static LinkInfo[] links_with_sensors = new LinkInfo[189833];
	static ArrayList<Integer> check = new ArrayList<Integer>();
	private static int links_with_sensor_count;
	//private static String FILE_LINK ="H:\\clearp_arterial\\links_all_2.csv";
	private static String FILE_LINK = "../GeneratedFile/Edges_G_1234_final_withextranames.csv";
		//"H:\\clearp\\links_all.csv";
        private static String [] days = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday","All"};
    static HashMap<String,String> links_funcAndspeedcat = new HashMap<String,String>();
    static HashMap<Integer,Double> speedcat = new HashMap<Integer,Double>();

	public static void main(String[] args) throws SQLException,
			NumberFormatException, IOException {

		readFileInMemory();
                readEdgeSensors();
		GetLinkWithSensors();
		
		//add
		getSpeedcatInfo();
		getlinksFuncAndSpeedcatInfo();
		
		//add ends
                for(int i=0;i<days.length;i++){

                    final int index = i;
                    Thread t1 = new Thread() {

                        @Override
                        public void run() {
                    try {
                        System.out.println("Getting Speeds for " + days[index]);
                        getArterialSensorAverages(index, days[index]);
                        System.out.println("Creating patterns for " + days[index]);
                        createPatterns_new(index, days[index]);

                    } catch (SQLException ex) {
                        Logger.getLogger(CreateListForArterials.class.getName()).log(Level.SEVERE, null, ex);
                    }
                        }
            };
                    t1.start();
            }
		/*
		 int index = 3;
		 System.out.println("Getting Speeds for " + days[index]);
         getArterialSensorAverages(index, days[index]);
         System.out.println("Creating patterns for " + days[index]);
        // createPatterns(index, days[index]);
         createPatterns_new(index, days[index]);
         */
	}

public static double approximation(LinkInfo link, int k){
	double penalty = 1.;
	if((k>=3&&k<=12)||(k>=43&&k<=52)){
		penalty *= 0.5;
	    if(links_funcAndspeedcat.get(link.getLinkId()).split(",")[0].equals("4")){
	    penalty -= 0.1;    	
	    }
	    else
	    	penalty += 0.1;
	}
	
	return penalty*speedcat.get(Integer.parseInt(links_funcAndspeedcat.get(link.getLinkId()).split(",")[1]));
		
		
  
}
	
public static void getlinksFuncAndSpeedcatInfo(){
	try{
		FileInputStream fstream = new FileInputStream("H:\\Jiayunge\\Edges_withSpeedCat.csv");
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		while ((strLine = br.readLine()) != null) {
			String[] nodes = strLine.split(",");
			String LinkId = nodes[0];
			int FuncClass = Integer.parseInt(nodes[1]);

            if(FuncClass == 3 || FuncClass==4)
            {
			String st_name = nodes[2];
			String st_node = nodes[3];
			String end_node = nodes[4];
			String index = st_node.substring(1)+""+end_node.substring(1);
			String LinkIdIndex = LinkId+""+index;
            String index2 = LinkIdIndex;

            links_funcAndspeedcat.put(index2,nodes[1]+","+nodes[9]);
		    }
         }
		in.close();
		
	}catch(Exception e){
		e.printStackTrace();
	}
}
	
public static void getSpeedcatInfo(){
	speedcat.put(1, 80.);
	speedcat.put(2,65.);
	speedcat.put(3,55.);
	speedcat.put(4,41.);
	speedcat.put(5,31.);
	speedcat.put(6,21.);
	speedcat.put(7,15.);
	speedcat.put(8,6.);
}
	
	
	private static void createPatterns_new(int index, String day) {
        int i = 0, j = 0;
FileWriter fstream = null;
BufferedWriter out = null;
try {

	while (i < links_with_sensor_count) {

		if(i%200==0)
		{
			System.out.println(((double)i/links_with_sensor_count*100.0)+"% of Links Completed");
		}

		LinkInfo link = links_with_sensors[i];
		//System.out.println(i + " " + link.toString());
                        fstream = new FileWriter("H:\\Jiayunge\\Output_"+day+"\\"
				+ link.getLinkId() + ".txt");
		out = new BufferedWriter(fstream);

		double distance = getDistance(link);
		//System.out.println("Link Distance=" + distance);
		out.write(link.toString());
		out.write("\n");
		out.write("Link Distance=" + distance);
		out.write("\n");

		int count = link.sensors.size();
		for (int k = 0; k < 60; k++) {
                        double avg=0.0;
                        int count_valid = 0;
                            for(j = 0;j<count;j++)
                        {
                              if(links_speed.get(index).containsKey(link.sensors.get(j)))
                                avg+=links_speed.get(index).get(link.sensors.get(j))[k];
                              if(links_speed.get(index).get(link.sensors.get(j))[k]>0.)
                            	  count_valid++;
                        }
                       
                        if(avg!=0.0){
                            avg/=count_valid;
                            Double TravelTime = distance / avg * 60;
			                out.write(k + "," + avg + "," + TravelTime);
			                out.write("\n");
                        }
                        else {
                        	double sudu = approximation(link,k);
                        	if(sudu>0.){
                        		out.write(k + "," + sudu + "," + distance / sudu * 60.);
                        	    out.write("\n");
                        	}
                        	else{
                              out.write("Average");
				              out.write("\n");
				              out.close();
				              fstream.close();
                              break;
                        	}
                        }

                    }
                      	i++;
		out.close();
		fstream.close();
		}




	System.out.println("File Writing Complete for "+day);
} catch (Exception e) {
	e.printStackTrace();
}
}
	
	private static void readEdgeSensors() {

		try {
			System.out.println("Reading Edge Sensors");

			FileInputStream fstream = new FileInputStream("H:\\Jiayunge\\Arterial_Sensor_close.csv");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				if(strLine.equals(""))
					continue;

				String[] nodes = strLine.split(",");

				if(nodes.length==0 || nodes.length==1 || nodes.length==2)
					continue;
				String LinkId = nodes[0]+""+nodes[1];
                                System.out.println(LinkId);
				int i = 2;
				ArrayList<Integer> sensors = new ArrayList<Integer>();
				while (i < nodes.length) {
					sensors.add(Integer.parseInt(nodes[i]));
					i++;
				}
				LinkInfo link = links.get(LinkId);
				link.sensors = sensors;
				links.put(LinkId, link);

			}
				in.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error: " + e.getMessage());
		}

		}


	private static Connection getConnection() {
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

	private static void readFileInMemory() {

		try {
			FileInputStream fstream = new FileInputStream(FILE_LINK);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				String[] nodes = strLine.split(",");
				String LinkId = nodes[0];
				int FuncClass = Integer.parseInt(nodes[1]);

                                if(FuncClass == 3 || FuncClass==4)
                                {
				String st_name = nodes[2];
				String st_node = nodes[3];
				String end_node = nodes[4];
				String index = st_node.substring(1)+""+end_node.substring(1);
				String LinkIdIndex = LinkId+""+index;
                            	String index2 = LinkIdIndex;

				int i = 5, count = 0;
				PairInfo[] pairs = new PairInfo[10];
				while (i < nodes.length) {
					double lati = Double.parseDouble(nodes[i]);
					double longi = Double.parseDouble(nodes[i + 1]);
					pairs[count] = new PairInfo(lati, longi);
					count++;
					i = i + 2;
				}
				if(links.get(index2) != null)
					System.out.println(links.get(index2)+"Duplicate LinkIds");

				links.put(index2, new LinkInfo(index2, FuncClass, st_name, st_node,
						end_node, pairs, count));

			}
                    }
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error: " + e.getMessage());
		}

	}

	private static void GetLinkWithSensors() {
		int i = 0, count = 0;
		Set<String> keys = links.keySet();
		Iterator<String> iter = keys.iterator();
		while (i < keys.size()) {
			LinkInfo link = links.get(iter.next());
			if (!(link.sensors.isEmpty())) {
				links_with_sensors[count] = link;
				count++;
				for (int j = 0; j < link.sensors.size(); j++) {
					if (!(check.contains(link.sensors.get(j)))) {
						check.add(link.sensors.get(j));
					}
				}

			}
			i++;
		}
		System.out.println("Total Sensors found =" + check.size()
				+ " and total links on which sensors found=" + count);
		links_with_sensor_count = count;
	}

	private static void createPatterns(int index, String day) {
                int i = 0, j = 0;
		FileWriter fstream = null;
		BufferedWriter out = null;
		try {

			while (i < links_with_sensor_count) {

				if(i%200==0)
				{
					System.out.println(((double)i/links_with_sensor_count*100.0)+"% of Links Completed");
				}

				LinkInfo link = links_with_sensors[i];
				//System.out.println(i + " " + link.toString());
                                fstream = new FileWriter("H:\\Jiayunge\\Output_"+day+"\\"
						+ link.getLinkId() + ".txt");
				out = new BufferedWriter(fstream);

				double distance = getDistance(link);
				//System.out.println("Link Distance=" + distance);
				out.write(link.toString());
				out.write("\n");
				out.write("Link Distance=" + distance);
				out.write("\n");

				int count = link.sensors.size();
				for (int k = 0; k < 60; k++) {
                                double avg=0.0;
                                    for(j = 0;j<count;j++)
                                {
                                      if(links_speed.get(index).containsKey(link.sensors.get(j)))
                                        avg+=links_speed.get(index).get(link.sensors.get(j))[k];
                                }
                               
                                if(avg!=0.0){
                                    avg/=count;
                                    Double TravelTime = distance / avg * 60;
					out.write(k + "," + avg + "," + TravelTime);
					out.write("\n");
                                }
                                else {
                                        out.write("Average");
						out.write("\n");
						out.close();
						fstream.close();
                                                break;
                                }

                            }
                              	i++;
				out.close();
				fstream.close();
				}




			System.out.println("File Writing Complete for "+day);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static double getDistance(LinkInfo link) {
		PairInfo[] pairs = link.getNodes();
		double distance = 0.0;
		for (int i = 0; i < link.getPairCount() - 1; i++) {
			distance += DistanceCalculator.CalculationByDistance(pairs[i],
					pairs[i + 1]);
		}
		return distance;
	}

    private static void getArterialSensorAverages(int index, String day) throws SQLException {
        
        String sql = "select link_id from arterial_congestion_config";
		Connection con = getConnection();
		PreparedStatement f = con.prepareStatement(sql,
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet rs = f.executeQuery();

		int count = 0;
		System.out.println("Getting Sensors from Config");
		while (rs.next()) {
			LinkIds[count] = rs.getInt(1);
			count++;
		}
		rs.close();
		f.close();
		con.close();int c2 = 0,c3=0;
		System.out.println(count+" Sensors Successfully Imported from Config");
		con = getConnection(); 
		HashMap<Integer,Double []> speeds = new HashMap<Integer,Double []>();
                for(int i=0;i<count;i++){
                    if(i%100==0){
                        System.out.println(i);
                        con.close();
                        con = getConnection();
                    }

                    if(day.equals("All"))
                     sql = "select avg(speed) from arterial_averages3 where link_id="+LinkIds[i]+" group by time order by time";
                    else
                     sql = "select speed from arterial_averages3 where day='"+day+"' and link_id="+LinkIds[i]+" order by time";
                    
                     f = con.prepareStatement(sql,
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                     rs = f.executeQuery();
                     int k=0;
                     Double speed[] = new Double[60];
                     while(rs.next() && k<60){
                        speed[k++] = rs.getDouble(1);
                     }
                    
                     speeds.put(LinkIds[i], speed);
                     
                     rs.close();
                     f.close();
                }
                
                Double speed[] = new Double[60];
                for(int k=0;k<60;k++)
                	speed[k] = 0.;
                speeds.put(-1, speed);
                
      links_speed.put(index,speeds);
    }



}

