package orthoscape;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyTable;

public class HomologUngroupingTask extends AbstractTask {

	private CyNetwork network;
	private CyGroupManager cyGroupManager;
	private CyTable nodeTable;
	
	public HomologUngroupingTask(CyNetwork network, CyGroupManager mymanager){
		this.network = network;	
		this.cyGroupManager = mymanager;
		this.nodeTable = network.getDefaultNodeTable();
	}
		
	public void run(TaskMonitor monitor) {
		if (network == null){
			System.out.println("There is no network.");
			return;
		}

		// Ungrouping all current nodes (expanding and destroying all groups)
		// P.S. If the user expaned the group by himself it will not be destroyed until collapsed again.
		List<CyNode> curList4 = new ArrayList<CyNode>();		
		curList4 = network.getNodeList();
		for (int j=0; j< curList4.size(); j++){	
			if (cyGroupManager.isGroup(curList4.get(j), network)){

				CyGroup curGroup = cyGroupManager.getGroup(curList4.get(j), network);
				if (curGroup.isCollapsed(network)){
					curGroup.expand(network);
				}
		//		curGroup.removeGroupFromNetwork(network);
				
				cyGroupManager.destroyGroup(cyGroupManager.getGroup(curList4.get(j), network));
	

			}
		}

		// Clearing the ghost empty rows obtained after ungrouping. Cytoscape bug.
		// It seems it's fixed since v3.4.0. This section probably useless now.
		nodeTable = network.getDefaultNodeTable();		
 		CyColumn mysuidcolumn = nodeTable.getColumn("SUID");
 		List<Long> suidstorage;		
 		suidstorage = mysuidcolumn.getValues(Long.class);
 		
		List<Long> coltodelete = new ArrayList<Long>();
		for (int j=0; j<suidstorage.size(); j++){
			CyRow nodeRow = nodeTable.getRow(suidstorage.get(j));
			if(nodeTable.getColumn("Type")!= null){
				if (nodeRow.get("Type", String.class) == "group"){
					coltodelete.add(suidstorage.get(j));
			  	} 
			}
		}		
		nodeTable.deleteRows(coltodelete);	
		
		// Clearing the ghost empty rows obtained after ungrouping. Cytoscape bug.
		// It seems it's fixed since v3.4.0. This section probably useless now.				
		nodeTable = network.getDefaultNodeTable();		
 		CyColumn mysuidcolumn2 = nodeTable.getColumn("SUID");
 		List<Long> suidstorage2;		
 		suidstorage2 = mysuidcolumn2.getValues(Long.class);
 				
		List<Long> coltodelete2 = new ArrayList<Long>();
		for (int j=0; j<suidstorage2.size(); j++){
			CyRow nodeRow = nodeTable.getRow(suidstorage2.get(j));
			if(nodeTable.getColumn("Entry_id")!= null){
				if (nodeRow.get("Entry_id", String.class) == null){//.isSet("Homology Cluster")){
					coltodelete2.add(suidstorage2.get(j));
			  	} 
			}
		}		
		nodeTable.deleteRows(coltodelete2);	
		
		// Clearing the ghost empty rows obtained after ungrouping. Cytoscape bug.
		// It seems it's fixed since v3.4.0. This section probably useless now.			
		nodeTable = network.getDefaultNodeTable();		
 		CyColumn mysuidcolumn3 = nodeTable.getColumn("SUID");
 		List<Long> suidstorage3;		
 		suidstorage3 = mysuidcolumn3.getValues(Long.class);
 				
		List<Long> coltodelete3 = new ArrayList<Long>();
		for (int j=0; j<suidstorage3.size(); j++){
			CyRow nodeRow = nodeTable.getRow(suidstorage3.get(j));
			if(nodeTable.getColumn("Type")!= null){
				if (nodeRow.get("Type", String.class) == null){
					coltodelete3.add(suidstorage3.get(j));
			  	} 
			}
		}
		nodeTable.deleteRows(coltodelete3);	        
      
		// Clearing the ghost empty rows obtained after ungrouping. Cytoscape bug.
		// It seems it's fixed since v3.4.0. This section probably useless now.			
		CyTable edgeTable = network.getDefaultEdgeTable();		
 		CyColumn mysuidcolumn4 = edgeTable.getColumn("SUID");
 		List<Long> suidstorage4;		
 		suidstorage4 = mysuidcolumn4.getValues(Long.class);
 				
		List<Long> coltodelete4 = new ArrayList<Long>();
		for (int j=0; j<suidstorage4.size(); j++){
			CyRow nodeRow = edgeTable.getRow(suidstorage4.get(j));
			if(nodeTable.getColumn("Type")!= null){
				if (nodeRow.get("Type", String.class) == null){
					coltodelete4.add(suidstorage4.get(j));
			  	} 
			}
		}
		edgeTable.deleteRows(coltodelete4);	        		
	}
    
	public void cancel() {
		cancelled = true;
	}
}
