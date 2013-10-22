package process;

import java.util.*;

import function.*;
import object.*;
import main.*;

public class OSMRouting {

	/**
	 * @param param
	 */
	static long START_NODE 		= 554763337;
	static long END_NODE 		= 1916742592;
	static int START_TIME 		= 10;
	static int TIME_INTERVAL 	= 15;
	static int TIME_RANGE 		= 60;
	/**
	 * @param osm
	 */
	static String MOTORWAY		= "motorway";
	static String MOTORWAY_LINK	= "motorway_link";
	static String PRIMARY		= "primary";
	static String PRIMARY_LINK	= "primary_link";
	static String SECONDARY		= "secondary";
	static String SECONDARY_LINK= "secondary_link";
	static String TERTIARY		= "tertiary";
	static String TERTIARY_LINK	= "tertiary_link";
	static String RESIDENTIAL	= "residential";
	static String CYCLEWAY		= "cycleway";
	static String PATH			= "path";
	static String TRACK			= "track";
	static String TRUNK			= "trunk";
	static String TRUNK_LINK	= "trunk_link";
	static String CONSTRUCTION	= "construction";
	static String PROPOSED		= "proposed";
	static String ROAD			= "road";
	static String ABANDONED		= "abandoned";
	static String SCALE			= "scale";
	static String UNCLASSIFIED	= "unclassified";
	/**
	 * @param node
	 */
	static HashMap<Long, NodeInfo> nodeHashMap = new HashMap<Long, NodeInfo>();
	/**
	 * @param edge
	 */
	static HashMap<Long, EdgeInfo> edgeHashMap = new HashMap<Long, EdgeInfo>();
	/**
	 * @param graph
	 */
	static HashMap<Long, ArrayList<ToNodeInfo>> adjListHashMap = new HashMap<Long, ArrayList<ToNodeInfo>>();
	static HashMap<Long, ArrayList<ToNodeInfo>> adjReverseListHashMap = new HashMap<Long, ArrayList<ToNodeInfo>>();
	static HashMap<String, EdgeInfo> nodesToEdge = new HashMap<String, EdgeInfo>();
	/**
	 * @param path
	 */
	static HashMap<String, Integer> hierarchyHashMap = new HashMap<String, Integer>();
	static ArrayList<Long> pathNodeList = new ArrayList<Long>();

	public static void main(String[] args) {
		OSMInput.paramConfig(OSMMain.osm);
		OSMOutput.paramConfig(OSMMain.osm);
		OSMInput.buildAdjList(adjListHashMap, adjReverseListHashMap);
		OSMInput.readNodeFile(nodeHashMap);
		OSMInput.readEdgeFile(edgeHashMap, nodesToEdge);
		
		initialHierarchy();
		prepareRoute(nodeHashMap);
		//tdsp(START_NODE, END_NODE, START_TIME);
		tdspHierarchy(START_NODE, END_NODE, START_TIME);
		OSMOutput.generatePathKML(nodeHashMap, pathNodeList);
		OSMOutput.generatePathNodeKML(nodeHashMap, pathNodeList);
	}
	
	public static void initialHierarchy() {
		hierarchyHashMap.put(MOTORWAY, 1);
		hierarchyHashMap.put(MOTORWAY_LINK, 1);
		hierarchyHashMap.put(PRIMARY, 2);
		hierarchyHashMap.put(PRIMARY_LINK, 2);
		hierarchyHashMap.put(SECONDARY, 3);
		hierarchyHashMap.put(SECONDARY_LINK, 3);
		hierarchyHashMap.put(TERTIARY, 4);
		hierarchyHashMap.put(TERTIARY_LINK, 4);
		hierarchyHashMap.put(RESIDENTIAL, 5);
		hierarchyHashMap.put(CYCLEWAY, 5);
		hierarchyHashMap.put(PATH, 6);
		hierarchyHashMap.put(TRACK, 6);
		hierarchyHashMap.put(TRUNK, 6);
		hierarchyHashMap.put(TRUNK_LINK, 6);
		hierarchyHashMap.put(CONSTRUCTION, 6);
		hierarchyHashMap.put(PROPOSED, 6);
		hierarchyHashMap.put(ROAD, 6);
		hierarchyHashMap.put(ABANDONED, 6);
		hierarchyHashMap.put(SCALE, 6);
		hierarchyHashMap.put(UNCLASSIFIED, 6);
	}
	
	public static void prepareRoute(HashMap<Long, NodeInfo> map) {
		for(NodeInfo node : map.values()) {
			node.prepareRoute();
		}
	}
	
	public static void prepareRoute(Stack<NodeInfo> stack) {
		while(!stack.isEmpty()) {
			NodeInfo node = stack.pop();
			node.prepareRoute();
		}
	}
	
	public static HashMap<Long, HighwayEntrance> searchHighwayExit(long endNode) {
		Stack<NodeInfo> nodeStack = new Stack<NodeInfo>();
		HashMap<Long, HighwayEntrance> highwayExitMap = new HashMap<Long, HighwayEntrance>();
		
		PriorityQueue<NodeInfo> priorityQ = new PriorityQueue<NodeInfo>( 20, new Comparator<NodeInfo>() {
			public int compare(NodeInfo n1, NodeInfo n2) {
				return n1.getCost() - n2.getCost();
			}
		});
		
		NodeInfo current = nodeHashMap.get(endNode);	// get start node
		nodeStack.push(current);
		if(current == null) {
			System.err.println("cannot find start node, program exit!");
			System.exit(-1);
		}
		
		current.setCost(0);	// set start cost to 0
		
		priorityQ.offer(current);
		
		// find four exit
		while ((current = priorityQ.poll()) != null && highwayExitMap.size() < 4) {
			long nodeId = current.getNodeId();
			
			ArrayList<ToNodeInfo> adjNodeList = adjReverseListHashMap.get(nodeId);
			if(adjNodeList == null)
				continue;
			
			int arrTime = current.getCost();
			
			for(ToNodeInfo fromNode : adjNodeList) {
				long fromNodeId = fromNode.getNodeId();
				
				NodeInfo fromNodeInfo = nodeHashMap.get(fromNodeId);
				
				if(fromNodeInfo.isVisited())	// if the node is visited, we bypass it
					continue;
				
				nodeStack.push(fromNodeInfo);

				fromNodeInfo.setVisited();
				
				String nodeIdKey = fromNodeId + "," + nodeId;
				EdgeInfo edge = nodesToEdge.get(nodeIdKey);
				
				String highway = edge.getHighway();
				int hierarchy = hierarchyHashMap.get(highway);
				if(hierarchy == 1) {	// find one highway exit
					ArrayList<Long> path = new ArrayList<Long>();
					NodeInfo entrance = current;
					path.add(entrance.getNodeId());
					while(entrance.getParentId() != -1) {
						entrance = nodeHashMap.get(entrance.getParentId());
						if(entrance == null) {
							System.err.println("cannot find intermediate node, program exit!");
							System.exit(-1);
						}
						path.add(entrance.getNodeId());	// add intermediate node
					}

					HighwayEntrance highwayExit = new HighwayEntrance(nodeId);
					highwayExit.setLocalToHighPath(path);
					highwayExitMap.put(nodeId, highwayExit);
					continue;
				}
				
				int travelTime;
				if(fromNode.isFix())	// fix time
					travelTime = fromNode.getTravelTime();
				else	// fetch from time array
					travelTime = fromNode.getTravelTimeArray()[30];	// use mid value
				
				// if we find a node with updated distance, just insert it to the priority queue
				// even we pop out another node with same id later, we know that it was visited and will ignore it
				int totalTime = arrTime + travelTime;
				if (totalTime < fromNodeInfo.getCost()) {
					fromNodeInfo.setCost(totalTime);
					fromNodeInfo.setParentId(nodeId);
					priorityQ.offer(fromNodeInfo);
				}
			}
		}
		
		prepareRoute(nodeStack);
		
		return highwayExitMap;
	}
	
	public static HashMap<Long, HighwayEntrance> searchHighwayEntrance(long startNode) {
		Stack<NodeInfo> nodeStack = new Stack<NodeInfo>();
		HashMap<Long, HighwayEntrance> highwayEntranceMap = new HashMap<Long, HighwayEntrance>();
		
		PriorityQueue<NodeInfo> priorityQ = new PriorityQueue<NodeInfo>( 20, new Comparator<NodeInfo>() {
			public int compare(NodeInfo n1, NodeInfo n2) {
				return n1.getCost() - n2.getCost();
			}
		});
		
		NodeInfo current = nodeHashMap.get(startNode);	// get start node
		nodeStack.push(current);
		if(current == null) {
			System.err.println("cannot find start node, program exit!");
			System.exit(-1);
		}
		
		current.setCost(0);	// set start cost to 0
		
		priorityQ.offer(current);
		
		// find four entrance
		while ((current = priorityQ.poll()) != null && highwayEntranceMap.size() < 4) {
			long nodeId = current.getNodeId();
			
			ArrayList<ToNodeInfo> adjNodeList = adjListHashMap.get(nodeId);
			if(adjNodeList == null)
				continue;
			
			int arrTime = current.getCost();
			
			for(ToNodeInfo toNode : adjNodeList) {
				long toNodeId = toNode.getNodeId();
				
				NodeInfo toNodeInfo = nodeHashMap.get(toNodeId);
				
				if(toNodeInfo.isVisited())	// if the node is visited, we bypass it
					continue;
				
				nodeStack.push(toNodeInfo);

				toNodeInfo.setVisited();
				
				String nodeIdKey = nodeId + "," + toNodeId;
				EdgeInfo edge = nodesToEdge.get(nodeIdKey);
				
				String highway = edge.getHighway();
				int hierarchy = hierarchyHashMap.get(highway);
				if(hierarchy == 1) {	// find one highway entrance
					ArrayList<Long> path = new ArrayList<Long>();
					NodeInfo entrance = current;
					path.add(entrance.getNodeId());
					while(entrance.getParentId() != -1) {
						entrance = nodeHashMap.get(entrance.getParentId());
						if(entrance == null) {
							System.err.println("cannot find intermediate node, program exit!");
							System.exit(-1);
						}
						path.add(entrance.getNodeId());	// add intermediate node
					}

					Collections.reverse(path);
					HighwayEntrance highwayEntrance = new HighwayEntrance(nodeId);
					highwayEntrance.setLocalToHighPath(path);
					highwayEntranceMap.put(nodeId, highwayEntrance);
					continue;
				}
				
				int travelTime;
				if(toNode.isFix())	// fix time
					travelTime = toNode.getTravelTime();
				else	// fetch from time array
					travelTime = toNode.getTravelTimeArray()[30];	// use mid value
				
				// if we find a node with updated distance, just insert it to the priority queue
				// even we pop out another node with same id later, we know that it was visited and will ignore it
				int totalTime = arrTime + travelTime;
				if (totalTime < toNodeInfo.getCost()) {
					toNodeInfo.setCost(totalTime);
					toNodeInfo.setParentId(nodeId);
					priorityQ.offer(toNodeInfo);
				}
			}
		}
		
		prepareRoute(nodeStack);
		
		return highwayEntranceMap;
	}
	
	public static void tdspHierarchy(long startNode, long endNode, int startTime) {
		System.out.println("start finding the path...");
		
		HashMap<Long, HighwayEntrance> entranceMap = searchHighwayEntrance(startNode);
		HashMap<Long, HighwayEntrance> exitMap	= searchHighwayExit(endNode);
		
		// test
		OSMOutput.generateEntranceExitKML(startNode, endNode, entranceMap, exitMap, nodeHashMap);
		
		int cost = Integer.MAX_VALUE;
		long finalEntrance = -1;
		long finalExit = -1;
		
		for(long entranceId : entranceMap.keySet()) {
			PriorityQueue<NodeInfo> priorityQ = new PriorityQueue<NodeInfo>( 20, new Comparator<NodeInfo>() {
				public int compare(NodeInfo n1, NodeInfo n2) {
					return n1.getCost() - n2.getCost();
				}
			});
			
			Stack<NodeInfo> nodeStack = new Stack<NodeInfo>();
			NodeInfo current = nodeHashMap.get(entranceId);	// get start node
			nodeStack.push(current);
			if(current == null) {
				System.err.println("cannot find entrance node, program exit!");
				System.exit(-1);
			}
			
			current.setCost(0);	// set start cost to 0
			
			priorityQ.offer(current);
			
			while ((current = priorityQ.poll()) != null) {
				long nodeId = current.getNodeId();
				
				if(exitMap.get(nodeId) != null) {	// find exit
					break;
				}
				
				int timeIndex = startTime + current.getCost() / 60 / TIME_INTERVAL;
				
				if (timeIndex > TIME_RANGE - 1)	// time [6am - 9 pm], we regard times after 9pm as constant edge weights
					timeIndex = TIME_RANGE - 1;
				
				ArrayList<ToNodeInfo> adjNodeList = adjListHashMap.get(nodeId);
				if(adjNodeList == null)
					continue;
				
				int arrTime = current.getCost();
				
				for(ToNodeInfo toNode : adjNodeList) {
					long toNodeId = toNode.getNodeId();
					
					NodeInfo toNodeInfo = nodeHashMap.get(toNodeId);
					
					if(toNodeInfo.isVisited())	// if the node is visited, we bypass it
						continue;
					nodeStack.push(toNodeInfo);
					
					toNodeInfo.setVisited();
					
					String nodeIdKey = nodeId + "," + toNodeId;
					EdgeInfo edge = nodesToEdge.get(nodeIdKey);
					
					String highway = edge.getHighway();
					int hierarchy = hierarchyHashMap.get(highway);
					if(hierarchy != 1)	// we keep on in highway
						continue;
					
					int travelTime;
					if(toNode.isFix())	// fix time
						travelTime = toNode.getTravelTime();
					else	// fetch from time array
						travelTime = toNode.getTravelTimeArray()[timeIndex];
					
					// if we find a node with updated distance, just insert it to the priority queue
					// even we pop out another node with same id later, we know that it was visited and will ignore it
					int totalTime = arrTime + travelTime;
					if (totalTime < toNodeInfo.getCost()) {
						toNodeInfo.setCost(totalTime);
						toNodeInfo.setParentId(nodeId);
						priorityQ.offer(toNodeInfo);
					}
				}
			}
			
			if(current != null && current.getCost() < cost) {	// find less cost path
				cost = current.getCost();
				long exitId = current.getNodeId();
				pathNodeList = new ArrayList<Long>();
				if(entranceId == exitId) {
					System.out.println("entrance node is the same as exit node.");
				}
				else {
					pathNodeList.add(exitId);
					while(current.getParentId() != -1) {
						current = nodeHashMap.get(current.getParentId());
						if(current == null) {
							System.err.println("cannot find intermediate node, program exit!");
							System.exit(-1);
						}
						pathNodeList.add(current.getNodeId());	// add intermediate node
					}
					Collections.reverse(pathNodeList);
				}
				finalEntrance = entranceId;
				finalExit = exitId;
			}
			
			// prepare for next time
			prepareRoute(nodeStack);
		}
		
		HighwayEntrance entrance = entranceMap.get(finalEntrance);
		HighwayEntrance exit = exitMap.get(finalExit);
		if(entrance != null && exit != null) {
			ArrayList<Long> entrancePath = entrance.getLocalToHighPath();
			ArrayList<Long> exitPath = exit.getLocalToHighPath();
			// add entrance and exit path
			pathNodeList.addAll(0, entrancePath);
			pathNodeList.addAll(pathNodeList.size(), exitPath);
			System.out.println("find the path successful!");
		}
		else {
			System.err.println("cannot find the entrance, program exit!");
			System.exit(-1);
		}
	}

	public static void tdsp(long startNode, long endNode, int startTime) {
		System.out.println("start finding the path...");
		PriorityQueue<NodeInfo> priorityQ = new PriorityQueue<NodeInfo>( 20, new Comparator<NodeInfo>() {
			public int compare(NodeInfo n1, NodeInfo n2) {
				return n1.getCost() - n2.getCost();
			}
		});
		Stack<NodeInfo> nodeStack = new Stack<NodeInfo>();
		NodeInfo current = nodeHashMap.get(startNode);	// get start node
		nodeStack.push(current);
		if(current == null) {
			System.err.println("cannot find start node, program exit!");
			System.exit(-1);
		}
		
		current.setCost(0);	// set start cost to 0
		
		priorityQ.offer(current);
		
		while ((current = priorityQ.poll()) != null) {
			long nodeId = current.getNodeId();
			
			if(nodeId == endNode)	// find the end
				break;
			
			int timeIndex = startTime + current.getCost() / 60 / TIME_INTERVAL;
			
			if (timeIndex > TIME_RANGE - 1)	// time [6am - 9 pm], we regard times after 9pm as constant edge weights
				timeIndex = TIME_RANGE - 1;
			
			ArrayList<ToNodeInfo> adjNodeList = adjListHashMap.get(nodeId);
			if(adjNodeList == null)
				continue;
			
			int arrTime = current.getCost();
			
			for(ToNodeInfo toNode : adjNodeList) {
				long toNodeId = toNode.getNodeId();
				
				NodeInfo toNodeInfo = nodeHashMap.get(toNodeId);
				
				if(toNodeInfo.isVisited())	// if the node is visited, we bypass it
					continue;
				
				nodeStack.push(toNodeInfo);

				toNodeInfo.setVisited();
				
				int travelTime;
				if(toNode.isFix())	// fix time
					travelTime = toNode.getTravelTime();
				else	// fetch from time array
					travelTime = toNode.getTravelTimeArray()[timeIndex];
				
				// if we find a node with updated distance, just insert it to the priority queue
				// even we pop out another node with same id later, we know that it was visited and will ignore it
				int totalTime = arrTime + travelTime;
				if (totalTime < toNodeInfo.getCost()) {
					toNodeInfo.setCost(totalTime);
					toNodeInfo.setParentId(nodeId);
					priorityQ.offer(toNodeInfo);
				}
			}
		}
		
		if (startNode == endNode)
			System.out.println("start node is the same as end node.");
		else {
			current = nodeHashMap.get(endNode);
			if(current == null) {
				System.err.println("cannot find end node, program exit!");
				System.exit(-1);
			}
			pathNodeList.add(endNode);	// add end node
			
			while(current.getParentId() != -1 && current.getParentId() != startNode) {
				current = nodeHashMap.get(current.getParentId());
				if(current == null) {
					System.err.println("cannot find intermediate node, program exit!");
					System.exit(-1);
				}
				pathNodeList.add(current.getNodeId());	// add intermediate node
			}
			
			if(current.getParentId() == -1) {
				System.err.println("cannot find the path, program exit!");
				System.exit(-1);
			}
			
			if(current.getParentId() == startNode)
				pathNodeList.add(startNode);	// add start node
			
			Collections.reverse(pathNodeList);	// reverse the path list
		}
		
		// prepare for next time
		prepareRoute(nodeStack);
		
		System.out.println("find the path successful!");
	}
}
