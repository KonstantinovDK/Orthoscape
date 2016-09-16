package org.cytoscape.sample.internal;

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

public class BPtoKEGGConverterTask extends AbstractTask{

	private CyNetwork network;		// Current network
	CyTable nodeTable;

	public BPtoKEGGConverterTask(CyNetwork network){    
		this.network = network;		
		this.nodeTable = network.getDefaultNodeTable();
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
	
	public String loadUrl(String StrUrl){
		StringBuffer result = new StringBuffer();
		try{
			URL url = new URL(StrUrl);			
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();           		   
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.connect();
				       
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF8"));
			String urline;
	
			while ((urline = rd.readLine()) != null) {
				result.append(urline).append("\n");
			}
			connection.disconnect();
		    rd.close();
		} catch (MalformedURLException e) {
				e.printStackTrace();
		} catch (IOException e) {
				e.printStackTrace();
		}
		return result.toString();
	}

	public String[] stringFounder(String strWhereFind, String strToFind){	
		String[] curlines = new String[2];
	
		while (strWhereFind.length() != 0){
	   	   	curlines = strWhereFind.split("\n", 2);
		    if (curlines.length == 1){
		       	break;
		    }
		    strWhereFind = curlines[1];
		    if (curlines[0].contains(strToFind)){
		        break;
		    }        	        
		}
		return curlines;
	}

	public void singleFilePrinting(File file, String data){
		try {
	    	PrintStream outStream = new PrintStream(file.toString());
	    	outStream.println(data);
	    	outStream.close();	
		}catch (IOException e2){ e2.printStackTrace(); }
	}
}