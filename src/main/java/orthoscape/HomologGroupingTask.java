package orthoscape;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyTable;

public class HomologGroupingTask extends AbstractTask {

	private CyNetwork network;
	private CyGroupFactory cyGroupCreator;
	private CyTable nodeTable;
	
	public HomologGroupingTask(CyNetwork network, CyGroupFactory myfactory){
		this.network = network;
		this.cyGroupCreator = myfactory;		
		this.nodeTable = network.getDefaultNodeTable();
	}
		
	public void run(TaskMonitor monitor) {
		if (network == null){
			System.out.println("There is no network.");
			return;
		}
						
 		CyColumn mysuidcolumn = nodeTable.getColumn("SUID");
 		List<Long> suidstorage;		
 		suidstorage = mysuidcolumn.getValues(Long.class);
 		
 		CyColumn mygroupcolumn = nodeTable.getColumn("Homology Cluster");
 		List<Integer> groupstorage;		
 		groupstorage = mygroupcolumn.getValues(Integer.class);
 		
 		// Search the number of groups
 		int groupmax = -1;
 		for (int i=0; i<groupstorage.size(); i++){
 			if (groupstorage.get(i) > groupmax){
 				groupmax = groupstorage.get(i);
 			}
 		}
 			
 		// Create empty groups
 		CyGroup[] allGroups = new CyGroup[groupmax+2];
 		@SuppressWarnings("unchecked")
		List<CyNode>[] allLists = new ArrayList[groupmax+2];
 		String[] allNames = new String[groupmax+2];
 		for (int i=0; i<groupmax+2; i++){
 			allGroups[i] = cyGroupCreator.createGroup(network, true);
 			List<CyNode> curList = new ArrayList<CyNode>();
 			allLists[i] = curList;
 			allNames[i] = "";
 		}

 		// Groups description in the table
 		for (int k=0; k<groupstorage.size(); k++){
			allLists[groupstorage.get(k)+1].add(network.getNode(suidstorage.get(k)));
		    CyRow nodeRow = nodeTable.getRow(suidstorage.get(k)); 
		    String namedata = nodeRow.get("name", String.class);
		    if (!allNames[groupstorage.get(k)+1].contains(namedata)){
		    	allNames[groupstorage.get(k)+1] += namedata + ", ";
		    }
 		} 	
 		
 		for (int i=0; i<groupmax+2; i++){
 			allNames[i] = allNames[i].substring(0, allNames[i].length()-2);
 		}
 		
 		// Fullfilling the groups		  		
 		for (int i=0; i<groupmax+2; i++){
 			allGroups[i].addNodes(allLists[i]);
 			allGroups[i].collapse(network);
 			
 	 		List<CyNode> curList = new ArrayList<CyNode>();
 	 		curList = network.getNodeList();
 	 		for (int j=0; j< curList.size(); j++){
 	 			CyRow nodeRow = nodeTable.getRow(curList.get(j).getSUID());
 		 		if (!nodeRow.isSet("Homology Cluster")){
 		 	 		nodeRow.set("Homology Cluster", i-1); 
 		 	 		
 		 	 		if(nodeTable.getColumn("Type")!= null){
 		 	 			nodeRow.set("Type", "group");
 		 	 		}
 		 	 		if(nodeTable.getColumn("Label")!= null){
 		 	 			nodeRow.set("Label", "Group " + String.valueOf(i-1));
 		 	 		}
 		 	 		
 		 	 		nodeRow.set("shared name", allNames[i]);
 		 	 		nodeRow.set("name", allNames[i]);
 		    	}    
 	 		}
 		}
	}
    
	public void cancel() {
		cancelled = true;
	}
}
