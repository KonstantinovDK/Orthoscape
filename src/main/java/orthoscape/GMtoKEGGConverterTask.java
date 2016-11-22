package orthoscape;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyTable;

public class GMtoKEGGConverterTask extends AbstractTask{

	private CyNetwork network;		// Активная сеть
	CyTable nodeTable;
//	File mybasedirectory;			// Директория с локальной базой

	public GMtoKEGGConverterTask(CyNetwork network){    	   
		this.network = network;		
	}
	
	public void run(TaskMonitor monitor) {
		if (network == null){
			System.out.println("There is no network.");
			return;
		}
		
		CyTable netTable = network.getDefaultNetworkTable();
	    CyColumn netcolumn = netTable.getColumn("organism");
	    List<String> netstorage = netcolumn.getValues(String.class);
	    
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
	   
	 	String KEGGorgname = "";
	 	CyRow netRow = netTable.getRow(netsuidstorage.get(0));
	    if (netstorage.get(0).equals("H. sapiens")){    	
			netRow.set("org", "hsa");
	    	KEGGorgname = "hsa";	    	
	    }
	    if (netstorage.get(0).equals("C. elegans")){    	
			netRow.set("org", "cel");
	    	KEGGorgname = "cel";	    	
	    }
	    if (netstorage.get(0).equals("A. thaliana")){    	
			netRow.set("org", "ath");
	    	KEGGorgname = "ath";	    	
	    }
	    if (netstorage.get(0).equals("D. melanogaster")){    	
			netRow.set("org", "dme");
	    	KEGGorgname = "dme";	    	
	    }
	    if (netstorage.get(0).equals("M. musculus")){    	
			netRow.set("org", "mmu");
	    	KEGGorgname = "mmu";	    	
	    }
	    if (netstorage.get(0).equals("S. cerevisiae")){    	
			netRow.set("org", "sce");
	    	KEGGorgname = "sce";	    	
	    }
	    if (netstorage.get(0).equals("D. rerio")){    	
			netRow.set("org", "dre");
	    	KEGGorgname = "dre";	    	
	    }
	    if (netstorage.get(0).equals("R. norvegicus")){    	
			netRow.set("org", "rno");
	    	KEGGorgname = "rno";	    	
	    }
 		CyColumn mynodesuidcolumn = nodeTable.getColumn("SUID");
 		List<Long> nodesuidstorage = mynodesuidcolumn.getValues(Long.class);

 		
 		String netName = "";			
		for (int i=0; i < nodeTable.getRowCount(); i++){			
		    CyRow nodeRow = nodeTable.getRow(nodesuidstorage.get(i)); 
		    String keggID = nodeRow.get("Entrez Gene ID", String.class);
		    keggID = KEGGorgname+":"+keggID;
		    nodeRow.set("name", keggID);
		    nodeRow.set("type", "gene");
		    
		    String checkName = nodeRow.get("node type", String.class);
		    if (checkName.equals("query")){
		    	String geneName = nodeRow.get("gene name", String.class);
		    	netName = netName + geneName + ", ";
		    }
		}

		netName = netName.substring(0, netName.length()-2);
		netRow.set("title", "GM: " + netName); 
	}
}