package orthoscape;

import java.util.List;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyTable;

public class BPtoKEGGConverterTask extends AbstractTask{

	private CyNetwork network;		// Current network
	public BPtoKEGGConverterTask(CyNetwork network){    
		this.network = network;		
	}
	
	public void run(TaskMonitor monitor) {
	
		if (network == null){
			System.out.println("There is no network.");
			return;
		}
		CyTable netTable = network.getDefaultNetworkTable();
	    
	    if(netTable.getColumn("org")!= null){
	    	netTable.deleteColumn("org");	
	    	netTable.createColumn("org", String.class, false);
		}
		else {
			netTable.createColumn("org", String.class, false);
		}
	    
	    if(netTable.getColumn("title")!= null){
	    	netTable.deleteColumn("title");	
	    	netTable.createColumn("title", String.class, false);
		}
		else {
			netTable.createColumn("title", String.class, false);
		}	
	    
	    CyTable nodeTable = network.getDefaultNodeTable();
	    
	    if(nodeTable.getColumn("type") == null){
	    	nodeTable.createColumn("type", String.class, false);
		}
	        	
 		CyColumn mynetsuidcolumn = netTable.getColumn("SUID");
 		List<Long> netsuidstorage = mynetsuidcolumn.getValues(Long.class);
	   
	 	CyRow netRow = netTable.getRow(netsuidstorage.get(0));
		netRow.set("org", "hsa");
		netRow.set("title", "BP: " + netRow.get("name", String.class));
	   
 		CyColumn mynodesuidcolumn = nodeTable.getColumn("SUID");
 		List<Long> nodesuidstorage = mynodesuidcolumn.getValues(Long.class);
 					
		for (int i=0; i < nodeTable.getRowCount(); i++){	
			CyRow nodeRow = nodeTable.getRow(nodesuidstorage.get(i));
			if (nodeRow.get("NCBI GENE", String.class) == null){
				nodeRow.set("type", "not gene");
				continue;
			}
		    String keggID = nodeRow.get("NCBI GENE", String.class);
		    keggID = "hsa:"+keggID;
		    nodeRow.set("name", keggID);
		    nodeRow.set("type", "gene");
		} 
	}
}