/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package highwaypatterngeneration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import Objects.DistanceCalculator;
import Objects.LinkInfo;
import Objects.PairInfo;
import Objects.TimeShot;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreateAdjList {

	static HashMap<String, LinkInfo> links = new HashMap<String, LinkInfo>();
	static String FILE_LINK = "H:\\Jiayunge\\Edges_G_1234_final_withextranames.csv";
    static HashMap<Integer,ArrayList<TimeShot>> SpeedNodes = new HashMap<Integer,ArrayList<TimeShot>>();
    //static TimeShot [] SpeedArray;
        private static String [] days = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday","All"};
	private static double[] ArterialAverages;
	private static double[] HighwayAverages;
	public static void main(String args[])
	{
		readFileInMemory();
		getHighwayArterialAverageSpeeds();
                 for(int i=0;i<days.length;i++){
                      final int index = i;
                      Thread t1 = new Thread() {
                @Override
                        public void run() {
                    try {
                        System.out.println("Getting Speeds for " + days[index]);
                        fillAdjList(index,days[index]);
                        System.out.println("Creating patterns for " + days[index]);
                        putListToArray(index,days[index]);

                    } catch (Exception ex) {
                        Logger.getLogger(CreateAdjList.class.getName()).log(Level.SEVERE, null, ex);
                    }
                        }
            };
           
                    t1.start();
             }

		/*
		 int  index = 3;
		 System.out.println("Getting Speeds for " + days[index]);
         fillAdjList(index,days[index]);
         System.out.println("Creating patterns for " + days[index]);
         putListToArray(index,days[index]);
*/
		  
		
         }

	private static void getHighwayArterialAverageSpeeds() {

		System.out.println("GETTING AVERAGE SPEEDS FOR HIGHWAYS/ARTERIALS");
		FileInputStream fstream = null;
		DataInputStream in = null;
		BufferedReader br  = null;
		try
		{
			HighwayAverages = new double[60];
			fstream = new FileInputStream("H:\\Jiayunge\\AverageSpeeds_Highways.txt");
			in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int count =0;
			while ((strLine = br.readLine()) != null) {
				HighwayAverages[count++] = Double.parseDouble(strLine.split(",")[1]);
			}
			in.close();fstream.close();

			ArterialAverages = new double[60];
			fstream = new FileInputStream("H:\\Jiayunge\\AverageSpeeds_Arterials.txt");
			in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in));
			count =0;
			while ((strLine = br.readLine()) != null) {
				ArterialAverages[count++] = Double.parseDouble(strLine.split(",")[1]);
			}
			in.close();fstream.close();


		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

	private static void putListToArray(int index, String day) {

		System.out.println("WRITING FILE FOR ADJACENCY LIST...FOR   "+day);
		FileWriter fstream = null;
		BufferedWriter out = null;
		try
		{
		fstream = new FileWriter("H:\\Jiayunge\\AdjList_"+day+".txt");
                
		out = new BufferedWriter(fstream);

		for(int i=0;i<111533;i++)
		{
                    if(i%100==0)
                        System.out.println(i);
                    
			boolean flag = false;
			for(int j=0;j<SpeedNodes.get(index).size();j++)
			{
				//Get Nodes with start index i
				TimeShot ts = SpeedNodes.get(index).get(j);
				if(Integer.parseInt(ts.getStartNode().substring(1))==i)
				{
					flag = true;
				    double[] travelTime = ts.getTravelTime();
				    int count=0;
				  //Check if the end node is V on anything !=1000.0
				    for(int k=0;k<59;k++)
				     {
				    	 if(travelTime[k+1]==travelTime[k] && travelTime[k]!=1000.0)
				    	 {
				    		 count++;
				    	 }
				     }
				    if(count == 60)
				    {
				    	out.write(ts.getEndNode()+"(F):"+(int)(travelTime[0]*60.0*1000.0)+";");
				    	continue;
				    }

				    out.write(ts.getEndNode()+"(V):");
				    for(int k=0;k<60;k++)
				     {
				    	 if(travelTime[k]==1000.0)
				    	 {
				    		 double distance = ts.getDistance();
				    		 double speed = getAverageTravelTime(ts.getFuncClass(),k);
				    		 int time = (int)((distance*60.0*60.0*1000.0)/speed);
                                                 if(time==0)
                                                     time=1;

				    		 if(k!=59)
				    		  out.write(time+",");
				    		 else
				    	      out.write(time+";");
				    	 }
				    	 else
				    	 {
				    		 if(k!=59)
					    		 out.write((int)(travelTime[k]*60.0*1000.0)+",");
					    		 else
					    		 out.write((int)(travelTime[k]*60.0*1000.0)+";");
				    	 }
				     }

				// If 1000.0 then get Average speed for every index 0->60 depending on FuncClass 1/2
				// Else if V/F write appropriately to the file.
				}

			}
			if(!flag)
				out.write("NA");
			out.write("\n");
		}
		out.close();
		System.out.println("finished!!");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private static double getAverageTravelTime(int funcClass, int k) {
		if(funcClass == 3 || funcClass == 4)
		{
			//changed with my code
			if(funcClass == 4)
				return 0.85*ArterialAverages[k];
			else
				return ArterialAverages[k];	
		    //my code ends
		
			/*original code
			return ArterialAverages[k];
			original code ends*/
		}
		else if(funcClass ==1 || funcClass == 2)
		{
			//changed with my code
			   if(funcClass == 2)
				   return 0.9*HighwayAverages[k];
			   else 
				   return HighwayAverages[k];
			
			//my code ends
			
			/*original code
			return HighwayAverages[k];
			original code ends*/
		}
		return 0.0;
	}

	private static void readFileInMemory() {

		System.out.println("READING LINK FILE AND GENERATING NODES");
		try {
			FileInputStream fstream = new FileInputStream(FILE_LINK);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				String[] nodes = strLine.split(",");
				String LinkId = nodes[0];
				int FuncClass = Integer.parseInt(nodes[1]);

                                String st_name = nodes[2];
				String st_node = nodes[3];
				String end_node = nodes[4];
				String index = st_node.substring(1)+""+end_node.substring(1);
				String LinkIdIndex = LinkId+""+index;
                            	String index2 = LinkIdIndex;
                                //System.out.println(st_node+" "+end_node);
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
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error: " + e.getMessage());
		}


	}

	private static void fillAdjList(int index, String day){
		System.out.println("GETTING LINKS AND ADDING TO TIMESHOTS FOR "+day);

		int i = 0;
		Set<String> keys = links.keySet();
		Iterator<String> iter = keys.iterator();
                ArrayList<TimeShot> speeds = new ArrayList<TimeShot>();
		while (i < keys.size()) {
			LinkInfo link = links.get(iter.next());
                        if(i%1000==0)
                        System.out.println(i);
			double []  travelTime = getTravelTimeFromFile(day,link);
                        speeds.add(new TimeShot(link.getStart_node(), link.getEnd_node(), link.getFunc_class(), getDistance(link), travelTime));
			
			i++;
		}
                SpeedNodes.put(index,speeds);


	}

	private static double[] getTravelTimeFromFile(String day,LinkInfo link) {
		double [] travelTime = new double[60];
		try {
                        FileInputStream fstream = new FileInputStream("H:\\Jiayunge\\Output_"+day+"\\"+link.getLinkId()+".txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int lineNumber =0;

			while ((strLine = br.readLine()) != null) {
			//System.out.println(strLine);
			if(lineNumber == 2 && strLine.equals("Average"))
			{
			int x =0;
			for(;x<60;x++)
			{
				travelTime[x] = 1000.0;
			}
			return travelTime;
			}
			if(lineNumber < 2)
			{	lineNumber++;
				continue;
			}
			if(strLine.equals("Average")&& lineNumber>2)
			{
				int x = lineNumber-2;
				for(;x<60;x++)
				{
					travelTime[x] = 1000.0;
				}
				return travelTime;
			}
                        travelTime[lineNumber-2] = Double.parseDouble(strLine.split(",")[2]);
			lineNumber++;
			}

			return travelTime;
		}
			catch(Exception e)
			{
				int x =0;
				for(;x<60;x++)
				{
					travelTime[x] = 1000.0;
				}
				return travelTime;
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

}
