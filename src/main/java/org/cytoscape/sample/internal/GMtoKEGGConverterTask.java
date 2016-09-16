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

public class GMtoKEGGConverterTask extends AbstractTask{

	private CyNetwork network;		// Активная сеть
	CyTable nodeTable;
//	File mybasedirectory;			// Директория с локальной базой

	public GMtoKEGGConverterTask(CyNetwork network){    
		
		// Useless code required to obtain synonyms to convert GeneMania data into KEGG data.
		// Right now original GeneMania data enough to do converting without using synonyms bases.
		
//		// Form initialization
//		JFrame myframe = new JFrame();
//		JFileChooser dialog = new JFileChooser();
//		dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//		dialog.setAcceptAllFileFilterUsed(false); 
//	    dialog.showOpenDialog(myframe);
//	    
//	    // Local base directory 
//	    this.mybasedirectory = dialog.getSelectedFile();
//	    dialog.setVisible(true);
//	    
//	    // Cancel if the base missing
//	    File dir = new File(mybasedirectory + "\\");
//    	if (!dir.exists()){
//    		return;
//    	}
//	    
//	    dir = new File(mybasedirectory + "\\Input\\");
//	    if (!dir.isDirectory()){
//	    	dir.mkdir();
//	    }	
//	    
//	    dir = new File(mybasedirectory + "\\Input\\Synonims\\");
//	    if (!dir.isDirectory()){
//	    	dir.mkdir();
//	    }
//	    
		this.network = network;		
		this.nodeTable = network.getDefaultNodeTable();
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
	
		while (strWhereFind.length() != 0){ //цикл до упоминания искомой строки
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



//Useless code required to obtain synonyms to convert GeneMania data into KEGG data.
// Right now original GeneMania data enough to do converting without using synonyms bases.

//String GMorgname = nodeRow.get("gene name", String.class);
//
//outStream.println(GMorgname);
//
//try {		   	     
//   	String sURL = "http://www.genome.jp/dbget-bin/www_bfind_sub?mode=bfind&max_hit=1000&locale=en&serv=kegg&dbkey=genes&keywords=" + GMorgname;
//   	File file = new File(mybasedirectory + "\\Input\\Synonims" + "\\"+GMorgname+".txt");
//   	StringBuffer result = new StringBuffer();			  	 
//     
//  	String curURL = "";
//  	if (file.exists()){
//	  	BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
//		String line = reader.readLine();
//		
//		// Old file reading version
//       	 while ((line = reader.readLine()) != null) {
//       		 result.append(line).append("\n");
//       	 }
//       	 curURL = result.toString();
//       	 reader.close();
//	}
//	else{
//		curURL = this.loadUrl(sURL);
//		String[] curlines = new String[2];     
//	    curlines = this.stringFounder(curURL, "button");
//	    
//	    String[] allGenes = curlines[1].split("</div></div>");
//	    
//	    String KEGGGeneName = "";
//	    for (int j=0; j< allGenes.length; j++){				    	
//	    	// Looking for genes and organism data 
//	    	if ( (allGenes[j].contains(" " + GMorgname + ",")) || (allGenes[j].contains(" " + GMorgname + ";")) ){
//	    		// If organism from network same to organism in base
//	    		if ( (allGenes[j].contains(KEGGorgname)) ){
//	    		
//	    			Pattern regname		 = Pattern.compile("[a-z]{2,9}[:]{1}[^\"]+");
//	    			Matcher m_regname = regname.matcher(allGenes[j]);
//	    			m_regname.find();
//	       	       	if (m_regname.find()){
//	       	       		KEGGGeneName = KEGGGeneName + m_regname.group() + " ";
//	       	       	}
//	    		}
//	    	}	
//	    }
//	    KEGGGeneName = KEGGGeneName.trim();
//	    nodeRow.set("name", KEGGGeneName);
//		
//   	    // local base saving
//		file.createNewFile();
//		this.singleFilePrinting(file, KEGGGeneName);				 		       	
//	}
//}catch (IOException e2){ e2.printStackTrace(); }