package tdsp.servlets;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import library.FibonacciHeap;
import library.FibonacciHeapNode;


import Objects.LinkInfo;
import Objects.Node;
import Objects.NodeConnectionValues;
import Objects.NodeValues;

import java.util.ArrayList;

/**
 * This class is TDSP processing Java Servlet Code counterpart of Java Code
 * for network 1234 using Fibonacci Heap Implementatation
 * @see FibonacciHeap
 * @see FibonacciHeapNode
 * @see TDSPQueryListJavaCodeFibo1234
 */
public class TDSPQueryListNewFibo1234accident extends HttpServlet {
	
	//static ArrayList<String> nodesInfo = new ArrayList<String>(); 
	static StringBuffer nodesInfo = new StringBuffer("");
	static ArrayList<String> pathInfo = new ArrayList<String>();
	
	//accident table
       static HashMap<String, String> accident = new HashMap<String, String>();
	
	//accident table ends
	
	private static final long serialVersionUID = 1L;
    private NodeConnectionValues[][] graphTDSP;

    double[][] nodes;
     Map<Integer, Integer> midPoints;
     double[][] midPointOrds;
	 LinkInfo links [] = new LinkInfo[100000];
	 int link_count = 0;
	 private int length1; 
	 private int length2; 
	 private String day;
	 private int dayIndex;
	 private int length3;
	 private static String [] days = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday","All"};
      PrintWriter out;
  
    @Override
    public void init(ServletConfig config){
    	try {
			super.init(config);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	readFile();
    	for(int i=0;i<days.length;i++)
        	readListTDSP(i,days[i]);
    }
      
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        ResourceBundle rb =
            ResourceBundle.getBundle("LocalStrings",request.getLocale());
        response.setContentType("text/plain");
        out = response.getWriter();
        link_count=0;
        int startNodeID;
        int endNodeID;
        day = request.getParameter("day");
        for(int i=0;i<days.length;i++)
        {
        	if(days[i].equals(day))
        		dayIndex = i;
        }

        if( request.getParameter("update").equals("False") ){
            String[] startOrds = request.getParameter("start").split(",");
            String[] endOrds = request.getParameter("end").split(",");
            startNodeID = findNN(Double.parseDouble(startOrds[0]), Double.parseDouble(startOrds[1]));
            endNodeID = findNN(Double.parseDouble(endOrds[0]), Double.parseDouble(endOrds[1]));
        }else{
            startNodeID = Integer.parseInt(request.getParameter("start"));
            endNodeID = Integer.parseInt(request.getParameter("end"));
        }

        int time = Integer.parseInt(request.getParameter("time"));

        tdsp(startNodeID, endNodeID, time);
        out.print("-"+startNodeID+"-"+endNodeID+"-"+time);
        nodesInfo.append("-"+startNodeID+"-"+endNodeID+"-"+time);
        
       // findPathInfo();
       // turn_by_turn();
    }


    private int findNN(double latitude, double longitude){
        int NNID = 1;
        double minDistance = (latitude-nodes[0][0])*(latitude-nodes[0][0]) + (longitude-nodes[0][1])*(longitude-nodes[0][1]);
        for(int i=1; i<nodes.length; i++){
            double dist = (latitude-nodes[i][0])*(latitude-nodes[i][0]) + (longitude-nodes[i][1])*(longitude-nodes[i][1]);
            if(dist < minDistance){
                NNID = i;
                minDistance = dist;
            }
        }
        return NNID;
    }

		
    private void readFile(){
        try{
        	  InputStream is = getServletContext().getResourceAsStream("\\WEB-INF\\classes\\TDSP Files1234\\TDSPData.obj");
  			 //FileInputStream fstream = new FileInputStream("C:\\Users\\Shireesh\\Desktop\\IMSC DR\\My Work\\March Work\\TDSPData.obj");
		     ObjectInputStream ois = new ObjectInputStream(is);
            
            length1 = ois.readInt();
            length2 = ois.readInt();
            length3 = ois.readInt();
            graphTDSP = new NodeConnectionValues[length1][8];
            
            int nodeNum = ois.readInt();
            nodes = new double[nodeNum][2];
            for(int i=0; i<nodeNum; i++){
                nodes[i][0] = ois.readDouble();
                nodes[i][1] = ois.readDouble();
            }

            ois.close();
            
            //accident part
            InputStream input = getServletContext().getResourceAsStream("\\WEB-INF\\classes\\TDSP Files1234\\accidents.txt");
  			DataInputStream in = new DataInputStream(input);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
	        String strLine;
	        
	        while((strLine = br.readLine())!=null){
	        	String[] nodes = strLine.split(",");
	        	accident.put(nodes[0]+","+nodes[1], nodes[2]);	
	        }
            
	        //accident part ends
	        
        }catch(IOException ioe){
            ioe.printStackTrace();
            System.exit(-1);
        }
    }

    private void readListTDSP(int index, String day) {

    	try {
    		
    		    // now we set accident date and time: Thursday 10:00-11:00
    		
    			InputStream is = getServletContext().getResourceAsStream(getFileName(day));
  			//FileInputStream fstream = new FileInputStream("C:\\Users\\Shireesh\\Desktop\\IMSC DR\\My Work\\March Work\\AdjList.txt");
				 DataInputStream in = new DataInputStream(is);
	        	 BufferedReader file = new BufferedReader(new InputStreamReader(in));
	       	     String tmp , temp, temp2;
	            tmp = file.readLine();
	            
	              
	            int i = 0;
	            while (tmp != null) {
	            	
	            	if(!(tmp.equals("NA")))
	            		{
	            		graphTDSP[i][index] = new NodeConnectionValues();
	            		StringTokenizer sT = new StringTokenizer(tmp, ";");
	            		
		                int j = 0, k=0;
		                while (sT.hasMoreTokens()) {
		                	temp = sT.nextToken(); 
		                	
		                	j = Integer.parseInt(temp.substring(1, temp.indexOf("(")));
		                    String type = temp.substring(temp.indexOf("(")+1, temp.indexOf(")"));
		                    int values [] = new int[60];
		                    if(type.equals("V"))
		                    {
		                    	k = 0;
			                    
			                    StringTokenizer sT2 = new StringTokenizer(temp, ",");
			                    
			                    while(sT2.hasMoreTokens()) {
			                        temp2 = sT2.nextToken();
			                        if(temp2.indexOf(":")!= -1)
			                        {
			                        
			                           //add accident code
			                           String test = "n"+i+","+"n"+j;	
			                           if(index == 3 && (k>=12&&k<=15)&& accident.containsKey(test)){
			                        	   double distance = Double.parseDouble(accident.get(test));
			                        	   double speed = 2.;
			                        	   int time = (int)((distance*60.0*60.0*1000.0)/speed);
                                           if(time==0)
                                              time=1;
                                            values[k++] = time;
			                            }
			                        	
			                        //add accident code ends
			                           else	
			                        	  values[k++] = Integer.parseInt(temp2.substring(temp2.indexOf(":")+1));
			                        }
			                        else
			                        {
			                        //add accident code
			                        	 String test = "n"+i+","+"n"+j;	
					                        if(index == 3 && (k>=12&&k<=15)&& accident.containsKey(test)){
					                        	double distance = Double.parseDouble(accident.get(test));
					                        	double speed = 2.;
					                        	int time = (int)((distance*60.0*60.0*1000.0)/speed);
		                                        if(time==0)
		                                            time=1;
		                                        values[k++] = time;
					                        }
					                        	
			                        //add accident code ends
					                   else     
			                        	  values[k++] = Integer.parseInt(temp2);
			                        }
			                    }
		                    }
		                    else
		                    {
		                    	for(k=0;k<length3;k++)
		                    		{
		                    		  //accident code
		                    		   String test = "n"+i+","+"n"+j;	
				                        if(index == 3 && (k>=12&&k<=15)&& accident.containsKey(test)){
				                        	double distance = Double.parseDouble(accident.get(test));
				                        	double speed = 2.;
				                        	int time = (int)((distance*60.0*60.0*1000.0)/speed);
	                                        if(time==0)
	                                            time=1;
	                                        values[k++] = time;
				                        }
		                    		   
		                    		  //accident code ends
				                        else
				                        	values[k] = Integer.parseInt(temp.substring(temp.indexOf(":")+1));
		                    		
		                    		}
		                    }
		                    graphTDSP[i][index].nodes.remove(j);
		                	graphTDSP[i][index].nodes.put(j, new NodeValues(j,values));
		                }		
	            	
	            }
	            	
	                i++;
	                tmp = file.readLine();
	            
	            }
	            file.close();
	            
	        }
	        catch (IOException io) {
	            io.printStackTrace();
	            System.exit(1);
	        } 
	        catch (RuntimeException re) {
	        	re.printStackTrace();
	            System.exit(1);
	        }
    }
	
    
    
    private void tdsp( int start, int end, int time) {

    	 //System.out.println("TDSP CALLED WITH PARAMETERS:"+start+","+end+","+time+","+maxTime);

		FibonacciHeap<Node> priorityQ = new FibonacciHeap<Node>();
		HashMap<Integer,Node> priorityQMap1 = new HashMap<Integer,Node>();
		HashMap<Node,FibonacciHeapNode<Node>> priorityQMap2 = new HashMap<Node,FibonacciHeapNode<Node>>();


		int i, len = graphTDSP.length, j, id;
		// System.out.println("len="+len);
		int w, arrTime;
		int[] c = new int[len];
		int[] parent = new int[len];
		double shortestTime = 8000000;

		int[] nextNode = new int[len];
	
		for (i = 0; i < len; i++) {
			nextNode[i] = -1;
			parent[i] = -1;
			if (i == start)
				c[i] = 0; // starting node
			else
				c[i] = 8000000; // indicating infinity
		}

		Node n, s = new Node(start, 0, time); 			// creating the starting
														// node with nodeId =
														// start and cost = 0
														// and arrival time =
														// time
		  FibonacciHeapNode<Node> new_node = new FibonacciHeapNode<Node>(s);
		  priorityQ.insert(new_node, s.getNodeCost());  // inserting s into the priority queue
		  priorityQMap1.put(s.getNodeId(), s);
		  priorityQMap2.put(s, new_node);
		  
		  while (!(priorityQ.isEmpty())) { //while Q is not empty

	        	//System.out.println("Size of queue="+priorityQ.size());
	        	FibonacciHeapNode<Node> node = priorityQ.min();
	        	priorityQ.removeMin();
	        	n = node.getData();
	        	
	        	
			priorityQMap1.remove(n.getNodeId());	
			priorityQMap2.remove(n);
			
			int updTime = time;
			id = n.getNodeId();
			if (graphTDSP[id][dayIndex] == null)
				continue;
			

			if (n.getNodeId() == end) {
				shortestTime = n.getArrTime();
			}

			if (n.getNodeId() != start) {
				updTime = time + getTime(n.getArrTime());

				if (updTime > 59)
					updTime = 59;
			}

			if (graphTDSP[id][dayIndex].nodes != null && n.getArrTime() < shortestTime) {
				HashMap<Integer, NodeValues> hmap = graphTDSP[id][dayIndex].nodes;

				Set<Integer> keys = hmap.keySet();
				Iterator<Integer> iter = keys.iterator();
				 //System.out.println("size->"+keys.size());
				int i2 = 0;
				while (i2 < keys.size()) {
					int key = iter.next();
					NodeValues val = hmap.get(key);
					arrTime = n.getArrTime();
					w = val.getValues()[updTime];
					
					//System.out.println("cost="+w);
					 if (arrTime + w < c[key]) {
                  	 c[key] = arrTime + w;
						parent[key] = id;
						//it = priorityQ.iterator();
						 						
						//System.out.println("Size of queue="+priorityQ.size());
						
						if(priorityQMap1.containsKey(key))
						{
						Node node_ = priorityQMap1.get(key);
						node_.setArrTime(c[key]);
						priorityQ.decreaseKey(priorityQMap2.get(node_),node_.getNodeCost());
						}
						else
						{
							Node node2 = new Node(key, c[key], c[key]);
							FibonacciHeapNode<Node> new_node2 = new FibonacciHeapNode<Node>(node2);
							priorityQ.insert(new_node2,c[key]);  //arrival time = c[i]
							priorityQMap1.put(key,node2);
							priorityQMap2.put(node2, new_node2);
						}
						
                   }
					 
					i2++;

				}
			}
		}


        int temp;
        temp = end;
        while(temp != -1) {
            if(parent[temp] != -1)
                nextNode[parent[temp]] = temp;
            temp = parent[temp];
        }


        if (start == end){
            //out.println("Your starting node is the same as your ending node.");
            out.println(""+nodes[start][0]+","+nodes[start][1]+";0");
            nodesInfo.append(""+nodes[ nextNode[i]][0]+","+nodes[ nextNode[i]][1] + ";");
            return;
        }
        else {
            i = start;
            j = 1;
            out.print(""+nodes[i][0]+","+nodes[i][1]+";");
            while (i != end && nextNode[i]!=-1) {
                 
            	out.print(""+nodes[ nextNode[i]][0]+","+nodes[ nextNode[i]][1] + ";");
            	
            	nodesInfo.append(""+nodes[ nextNode[i]][0]+","+nodes[ nextNode[i]][1] + ";");
            	
            	i = nextNode[i];
            }
            out.print(""+(double)c[end]/60000.0);
            nodesInfo.append(""+(double)c[end]/60000.0);
            
        }

    }
    
    private int getTime(int arrTime) {
    	int minutesTime = (int)(arrTime/60000.0);
    	if(minutesTime>=0 && minutesTime<7)
    	return 0;
    	else if(minutesTime>=7 && minutesTime<22)
    		return 1;
    	else if(minutesTime>=22 && minutesTime<37)
    		return 2;
    	else if(minutesTime>=37 && minutesTime<52)
    		return 3;
    	else
    		return 4;
    	
    }
    
    private static String getFileName(String day){
		return("\\WEB-INF\\classes\\TDSP Files1234\\AdjList_"+day+".txt");
	}
    
    
    //add turn-by-turn
    
    public static boolean  name_match(String st_name_pre, String st_name_o){
    	
		String[] nodes_pre = st_name_pre.split(";");
		String[] nodes_o   = st_name_o.split(";");
		
		for(int i = 0; i< nodes_o.length;i++)
		   for(int j = 0 ;j < nodes_pre.length; j++){
			   if(nodes_o[i].equals(nodes_pre[j]))
				   return true;
		   }
		
		return false;
	
		
	
}
    
    
    public  void findPathInfo(){
		try{       
			String[] nodes = nodesInfo.toString().split(";");
			
			for(int i = 0;i<nodes.length-2;i++){
				String query = nodes[i]+","+nodes[i+1];
				InputStream is = getServletContext().getResourceAsStream("\\WEB-INF\\classes\\TDSP Files1234\\Edges.csv");
				DataInputStream in2 = new DataInputStream(is);
				BufferedReader br2 = new BufferedReader(new InputStreamReader(in2));
				
				String strLine2;
				while ((strLine2 = br2.readLine()) != null){
					   String[] nodes2 = strLine2.split(",");
					   String test  =  nodes2[5]+","+nodes2[6]+","+nodes2[7]+","+nodes2[8];
					   
				       if(query.equals(test)){
				    	  
				    	   pathInfo.add(strLine2);
				        
				    	   break;
				       }
				
				
				}
				
				
				br2.close();
				in2.close();
				is.close();
				
		 }
		 		   
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
      
    public  void  turn_by_turn(){
		try{
			
			out.print("\n");
			
			String strLine;
			
		
			String link_id = "";
			double tail_st_x = 0.;
			double tail_st_y = 0.;
			double tail_end_x = 0.;
			double tail_end_y = 0.;
			String st_name_pre = "";
			double A = 0.;
			double B = 0.;
			double C = 0.; 
			
			int count = 0;
			
			
			for(int num = 0;num<pathInfo.size();num++) {
				strLine = pathInfo.get(num);
				count++;
				
   			    String[] nodes =  strLine.split(",");
   			    String link_id_o = nodes[0]+""+nodes[3].substring(1)+""+nodes[4].substring(1);
   			    String st_name_o = nodes[2];
   			    
   			    if(count==1){
   			    	tail_st_x =  Double.parseDouble(nodes[6])*100.;
   			    	tail_st_y =  Double.parseDouble(nodes[5])*100.;
   			    	tail_end_x = Double.parseDouble(nodes[8])*100.;
   			    	tail_end_y = Double.parseDouble(nodes[7])*100.;
   			    	
   			    	link_id = link_id_o;
   			    	st_name_pre = st_name_o;
   			    	continue;
   			    }
   			    
   			    if(link_id_o.equals(link_id)){
   			    	tail_st_x =  Double.parseDouble(nodes[6])*100.;
   			    	tail_st_y =  Double.parseDouble(nodes[5])*100.;
   			    	tail_end_x = Double.parseDouble(nodes[8])*100.;
   			    	tail_end_y = Double.parseDouble(nodes[7])*100.;
   			    	
   			    	link_id = link_id_o;
   			    	st_name_pre = st_name_o;
   			    }
   			    	
   			    else if(name_match(st_name_pre,st_name_o)){
   			    	tail_st_x =  Double.parseDouble(nodes[6])*100.;
   			    	tail_st_y =  Double.parseDouble(nodes[5])*100.;
   			    	tail_end_x = Double.parseDouble(nodes[8])*100.;
   			    	tail_end_y = Double.parseDouble(nodes[7])*100.;
   			    	
   			    	link_id = link_id_o;
   			    	st_name_pre = st_name_o;
   			    }
   			      	
   			    else{
   			    	
   			     A = tail_end_y-tail_st_y;
 	             B = -(tail_end_x-tail_st_x);
 	             C = (tail_end_x-tail_st_x)*tail_st_y - (tail_end_y-tail_st_y)*tail_st_x;
 	             
 	             double x = Double.parseDouble(nodes[8])*100.;
 	             double y = Double.parseDouble(nodes[7])*100.;
 	             
 	             double result = A*x+B*y+C ;
 	             String direction = "";
 	             
 	             if(result == 0)
 	            	 direction = "F";
 	             else if(result > 0)
 	            	 direction = "R";
 	             else
 	            	 direction = "L";
 	             
 	            double x1 = tail_end_x - tail_st_x;
 	            double y1 = tail_end_y - tail_st_y;
 	            double x2 = x - tail_end_x;
 	            double y2 = y - tail_end_y;
 	             
 	            double cosTheta = (x1*x2+y1*y2)/(Math.sqrt(x1*x1+y1*y1)*Math.sqrt(x2*x2+y2*y2));
 	            
 	            out.print(count+". ");
 	            if(direction.equals("F"))
 	            	out.print("go onto "+st_name_o.split(";")[0]);
 	            else if(direction.equals("R")){
 	            	   if(cosTheta>=0.86)
 	            	     out.print("trun slight right onto "+st_name_o.split(";")[0]);
 	            	   else 
 	            		 out.print("turn right onto "+st_name_o.split(";")[0]);
 	            	}
 	            else{
 	            	if(cosTheta>=0.86)  
 	            	     out.print("turn slight left onto "+st_name_o.split(";")[0]);
 	            	else
 	            		 out.print("turn left onto "+st_name_o.split(";")[0]);
 	               }
 	            out.print("\n");
 	             
 	            tail_st_x =  Double.parseDouble(nodes[6])*100.;
			    tail_st_y =  Double.parseDouble(nodes[5])*100.;
			    tail_end_x = Double.parseDouble(nodes[8])*100.;
			    tail_end_y = Double.parseDouble(nodes[7])*100.;
			    	
			    link_id = link_id_o;
			    st_name_pre = st_name_o;	 
   			    	
   			    }
   			    
   			    
   	     
   	      
			}
			//out.close();
			
		}catch(Exception e){
			 e.printStackTrace(); 
		}
	}
    
    
    
}



