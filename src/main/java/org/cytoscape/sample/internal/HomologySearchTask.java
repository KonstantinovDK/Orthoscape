package org.cytoscape.sample.internal;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyTable;

public class HomologySearchTask extends AbstractTask {

	private CyNetwork network;		// current network
	double equality;				// identity value (homologs with lower value will be rejected)
	int SWScore;					// Smith-Waterman score (homologs with lower value will be rejected)
	String homologyType;			// orthology/paralogy analysis (to choose using java form)
	File mybasedirectory;			// local base directory
	String sep = File.separator;	// directory separator in current operating system
	
	Boolean inputmark;				// mark to create local storage base
	Boolean updatemark;				// mark to update local storage base
	Boolean outputmark;				// mark to create output data to make analysis and reports
	
	int domensNumber;				// number of domens required to be same (gene-homolog comparison)
	Boolean domenmark;				// mark to use specifiс domains
	List<String> selectedDomens;	// and place to put these domains
	
    List<String> v_namedata;		// vector to put all genes from one node
    List<String> groupdata;			// and vector to put all groups of genes from one node

    int curiter;        			// iterator to move on network table
    int maxiters;       			// number of rows in table

    int nameiter;       			// iterator to move on nodes in gene (using v_namedata)
    int maxnames;       			// current v_namedata.size()

    int maxgroupnumber;				// number of homology clusters right now (maxgroupnumber <= curiter <= maxiters)
    
    List<String> totalnames;		// list to store all current analysed genes
    List<Integer> totalgroups;		// list to store groups of all current analysed genes
    List<Long> totalsuids;			// list of suids (Cytoscape use suid like unique field)
    
	CyTable nodeTable;				// network nodes table
	
	public HomologySearchTask(CyNetwork network){
		 
		// different input data formats
		MaskFormatter floatformatter = null;
		MaskFormatter longIntegerformatter = null;
		MaskFormatter shortIntegerformatter = null;
		
		try {
			floatformatter = new MaskFormatter("#.###");
			longIntegerformatter = new MaskFormatter("#####");
			shortIntegerformatter = new MaskFormatter("##");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		JFormattedTextField equalityField = new JFormattedTextField(floatformatter);
		equalityField.setFocusLostBehavior(JFormattedTextField.COMMIT);

		JFormattedTextField SWequalityField = new JFormattedTextField(longIntegerformatter);
		SWequalityField.setFocusLostBehavior(JFormattedTextField.COMMIT);
				
		JFormattedTextField domensField = new JFormattedTextField(shortIntegerformatter);
		domensField.setFocusLostBehavior(JFormattedTextField.COMMIT);
		
		// Homology type choosing
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		String[] homoType = {"Paralogs search", "Orthologs search"};
		JComboBox<String> homologyBox = new JComboBox<String>(homoType); 			
		
		JPanel textFieldPanel = new JPanel();
		textFieldPanel.setLayout(new BoxLayout(textFieldPanel, BoxLayout.X_AXIS));
		textFieldPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		textFieldPanel.add(new JLabel("Choose the homology type: "));
		textFieldPanel.add(homologyBox);
	
		// Place to put identity
		JPanel homologyBoxPanel = new JPanel();
		homologyBoxPanel.setLayout(new BoxLayout(homologyBoxPanel, BoxLayout.X_AXIS));
		homologyBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		homologyBoxPanel.add(new JLabel("Put the homology identity value: "));
		homologyBoxPanel.add(equalityField);
		
		// Place to put SW-Score
		JPanel SWhomologyBoxPanel = new JPanel();
		SWhomologyBoxPanel.setLayout(new BoxLayout(SWhomologyBoxPanel, BoxLayout.X_AXIS));
		SWhomologyBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		SWhomologyBoxPanel.add(new JLabel("Put the Smith Waterman score value: "));
		SWhomologyBoxPanel.add(SWequalityField);
		
		// Box to use local base
		JCheckBox localbaseBox = new JCheckBox();
		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.X_AXIS));
		checkBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		checkBoxPanel.add(localbaseBox);
		checkBoxPanel.add(new JLabel(" check it to create or to use local storage of data."));
		
		JLabel manylabels = new JLabel("It will increase the rate of the future work but it requires some MB space.");
		manylabels.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		// Box to update local base
		JCheckBox updatebaseBox = new JCheckBox();
		JPanel updateBoxPanel = new JPanel();
		updateBoxPanel.setLayout(new BoxLayout(updateBoxPanel, BoxLayout.X_AXIS));
		updateBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		updateBoxPanel.add(updatebaseBox);
		updateBoxPanel.add(new JLabel(" check it to update local storage of data (current network only)."));
		
		// Box to create output data to make reports
		JCheckBox storagebaseBox = new JCheckBox();
		JPanel storageBoxPanel = new JPanel();
		storageBoxPanel.setLayout(new BoxLayout(storageBoxPanel, BoxLayout.X_AXIS));
		storageBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		storageBoxPanel.add(storagebaseBox);
		storageBoxPanel.add(new JLabel(" check it to create output data."));
		
		JLabel storagelabels = new JLabel("It requires to make reports in any time you need without network analysis and"
										+ " it will give you additional information but requires some MB space.");
		storagelabels.setAlignmentX(Component.LEFT_ALIGNMENT);	
		
		
		// Place to put domains number
		JPanel domensBoxPanel = new JPanel();
		domensBoxPanel.setLayout(new BoxLayout(domensBoxPanel, BoxLayout.X_AXIS));
		domensBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		domensBoxPanel.add(new JLabel("Put the number of  domains we should compare: "));
		
		String base = "0";
		domensField.setValue(base.trim());
		domensBoxPanel.add(domensField);
		
		// Place to put specific domains
		JCheckBox domensBox = new JCheckBox();
		JPanel domensPanel = new JPanel();
		domensPanel.setLayout(new BoxLayout(domensPanel, BoxLayout.X_AXIS));
		domensPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		domensPanel.add(domensBox);
		domensPanel.add(new JLabel(" check it to use selected domens only (write it in the next field using \",\" separator)"));
		
		JTextField domenArea = new JTextField();
		domenArea.setAlignmentX(Component.LEFT_ALIGNMENT);	
		
		// Form filling
		p.add(textFieldPanel);
		p.add(homologyBoxPanel);
		p.add(SWhomologyBoxPanel);
		p.add(checkBoxPanel);
		p.add(manylabels);
		p.add(updateBoxPanel);
		p.add(storageBoxPanel);
		p.add(storagelabels);
		
		p.add(domensBoxPanel);
		p.add(domensPanel);
		p.add(domenArea);
		p.setAlignmentY(Component.LEFT_ALIGNMENT);
		
		int myOption = JOptionPane.showConfirmDialog(null, p, "Setup parameters: ", JOptionPane.OK_CANCEL_OPTION);	
		
		// "Cancel" button
		if (myOption == 2){
			System.out.println("Cancelled");
			return;
		}
		
		// Form reading
		this.equality = Double.parseDouble(equalityField.getText());
		this.SWScore = Integer.parseInt(SWequalityField.getText().trim());
		this.homologyType = homologyBox.getSelectedItem().toString();

		this.domensNumber = Integer.parseInt(domensField.getText().trim());
		
		this.inputmark = localbaseBox.isSelected();
		this.updatemark = updatebaseBox.isSelected();
		this.outputmark = storagebaseBox.isSelected();
		
		this.domenmark = domensBox.isSelected();
		
		if (this.inputmark || this.outputmark){
			// Form to choose the base
			JFrame myframe = new JFrame();
			JFileChooser dialog = new JFileChooser();
			dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			dialog.setAcceptAllFileFilterUsed(false); 			
			int returnValue = dialog.showOpenDialog(myframe);
			
			if (returnValue == javax.swing.JFileChooser.APPROVE_OPTION){
				this.mybasedirectory = dialog.getSelectedFile();
				dialog.setVisible(true);
			}
			else{
				System.out.println("Cancelled");
				return;
			}
	        
	        // Directories creating
	        if (this.inputmark){
		        File dir = new File(mybasedirectory + sep + "Input" + sep);
			    if (!dir.isDirectory()){
			    	dir.mkdir();
			    }	
			    		 
			    if (homologyType == "Paralogs search"){
				    dir = new File(mybasedirectory + sep + "Input" + sep + "ParalogBase" + sep);
				    if (!dir.isDirectory()){
				    	dir.mkdir();
				    }
			    }
			    else{
				    dir = new File(mybasedirectory + sep + "Input" + sep + "OrthologBase" + sep);
				    if (!dir.isDirectory()){
				    	dir.mkdir();
				    }
			    }
			    
				dir = new File(mybasedirectory + sep + "Input" + sep + "OrganismBase" + sep);
				if (!dir.isDirectory()){
					dir.mkdir();
				}
			    			    
			    if (this.domensNumber != 0){
				    dir = new File(mybasedirectory + sep + "Input" + sep + "Domains" + sep);
				    if (!dir.isDirectory()){
				    	dir.mkdir();
				    }
			    }
	        }
	        // Directories creating
	        if (this.outputmark){
			    File dir = new File(mybasedirectory + sep + "Output" + sep);
			    if (!dir.isDirectory()){
			    	dir.mkdir();
			    }	
   
			    if (homologyType == "Paralogs search"){
				    dir = new File(mybasedirectory + sep + "Output" + sep + "ParalogDomains" + sep);
				    if (!dir.isDirectory()){
				    	dir.mkdir();
				    }
			    }
			    else{
				    dir = new File(mybasedirectory + sep + "Output" + sep + "OrthologDomains" + sep);
				    if (!dir.isDirectory()){
				    	dir.mkdir();
				    }
			    }
	        }
	        
	        // File with base version
	        try { 
		        File file = new File(mybasedirectory + sep + "baseVersion.txt");
		        if(!file.exists()) {
		        	file.createNewFile();
			        PrintWriter out = new PrintWriter(file.getAbsoluteFile());
			        out.print("The base forming started " + new java.util.Date());
			        out.close();
		      	}
	        }catch (IOException e2){ e2.printStackTrace(); }
		}
		
		// Specific domains initialization
		if (this.domenmark){
					
			String unseparatedDomens = domenArea.getText();
			String[] separatedDomens = unseparatedDomens.split(","); 
			
			selectedDomens = new ArrayList<String>();
			for (int dom=0; dom<separatedDomens.length; dom++){
				selectedDomens.add(separatedDomens[dom].trim().toUpperCase());
			}			
		}
	    		
		this.network = network;		
		this.totalnames = new ArrayList<String>();
		this.totalgroups = new ArrayList<Integer>();
		this.totalsuids = new ArrayList<Long>();
		
		this.nodeTable = network.getDefaultNodeTable();		
	    this.v_namedata = new ArrayList<String>();
	    this.groupdata = new ArrayList<String>();       
	}
	
	public void run(TaskMonitor monitor) {				
		if (network == null){
			System.out.println("There is no network.");
			return;
		}
		// Field to put into Cytoscape node table after analysis performed.			
		if(nodeTable.getColumn("Homology Cluster")!= null){
			nodeTable.deleteColumn("Homology Cluster");	
			nodeTable.createColumn("Homology Cluster", Integer.class, false);
		}
		else {
			nodeTable.createColumn("Homology Cluster", Integer.class, false);
		}		
							
	    maxiters = network.getNodeCount();
	    curiter = 0;
	    	    
 		CyColumn mysuidcolumn = nodeTable.getColumn("SUID");
 		List<Long> suidstorage = mysuidcolumn.getValues(Long.class);
	    CyRow nodeRow = nodeTable.getRow(suidstorage.get(curiter)); 
	      			    
	    // Reading all the numbers of genes from first row (to put start group number is equal to 0)
	    String namedata = nodeRow.get("name", String.class);
	    totalnames.add(namedata);
	    if ((// BioPax
	    		(nodeTable.getColumn("NCBI GENE") != null) && (nodeRow.get("NCBI GENE", String.class) == null)
	    	)	
	    		||
	    	(// KEGG																									
	    	namedata.contains("path:") ||
		    namedata.contains("cpd:")  || 
		    namedata.contains("gl:")   ||
		    namedata.contains("dr:")   ||
		    namedata.contains("ko:")   ||
		    namedata.contains(":null")
		    )){
	    	totalgroups.add(-1);
		    maxgroupnumber = -1;
	    }
	    else{
	    	totalgroups.add(0);
		    maxgroupnumber = 0;
	    }
	    
	    curiter++;
	    
	    // One row parsing
        if (curiter < maxiters){
	       tableline_parser();
	    }
      	resultsconfirming(); 
	}
		
	private void tableline_parser(){
		CyColumn mysuidcolumn = nodeTable.getColumn("SUID");
 		List<Long> suidstorage = mysuidcolumn.getValues(Long.class);
	    CyRow nodeRow = nodeTable.getRow(suidstorage.get(curiter)); 
	       			    
	    String namedata = nodeRow.get("name", String.class);				
	    v_namedata.clear();		// Here we go to new row so we can clear old vector
	    
	    if ((// BioPax
	    		(nodeTable.getColumn("NCBI GENE") != null) && (nodeRow.get("NCBI GENE", String.class) == null)
	    	)	
	    		||
	    	(// KEGG																									
	    	namedata.contains("path:") ||
		    namedata.contains("cpd:")  || 
		    namedata.contains("gl:")   ||
		    namedata.contains("dr:")   ||
		    namedata.contains("ko:")   ||
		    namedata.contains(":null")
		    )){
	    	
	    	totalnames.add(namedata);
	    	totalgroups.add(-1);
		    curiter++;
	        if (curiter < maxiters){
   	            tableline_parser();
   	        }
	    }
	    else{    		    
		    totalnames.add(namedata);		// Add node to node list
		    totalgroups.add(666);			// and group for this node to change it later
		    
		    // Here we separate all genes from one node
		    while (namedata.length() != 0){
		        String[] curname = namedata.split(" ", 2);     
		        v_namedata.add(curname[0]);
		        
		        if (curname.length == 1){
		        	break;
		        }
		        namedata = curname[1];
		    }
	
		    nameiter = 0;
		    // Start the work with specific gene 
		    maxnames = v_namedata.size();		    
		    oneurlname_parser();	
	    }
	}

	void oneurlname_parser(){
		try {
		
			// Loading the page with data about orthologs and paralogs			String gomoname = v_namedata.get(nameiter);		// Current gene name
		    String sURL = "";								// url in KEGG base
		    String BaseType;								// Homology type
		    
		    if (homologyType == "Paralogs search"){
			    sURL = "http://www.kegg.jp/ssdb-bin/ssdb_paralog?org_gene=" + gomoname;
			    BaseType = sep + "ParalogBase";
		    } 
		    else{
		    	sURL = "http://www.kegg.jp/ssdb-bin/ssdb_best?org_gene=" + gomoname;
		    	BaseType = sep + "OrthologBase";
		    }
		                
		    String tempgomoname = gomoname.replace(':', '_');
		    File file = new File(mybasedirectory + sep + "Input" + BaseType + sep + tempgomoname + ".txt");
		     		    String curURL = "";								// gene's url in string		   	
	    	int uselessaminoNumber = 0;						// DI important param (needs here only because of local base same for both algorithms)
	    		    	
	    	// If the local base exists 
	    	if ((inputmark) && (file.exists()) && (!updatemark)){
	    		 BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
	    		 String line = reader.readLine();
	    		 uselessaminoNumber = Integer.parseInt(line);
	    			
	    		 StringBuffer result = new StringBuffer();
	  	       	 while ((line = reader.readLine()) != null) {
	  	       		 result.append(line).append("\n");
	  	       	 }
	  	       	 curURL = result.toString();
	  	       	 reader.close();
	    	 }
	    	 // If the local base doesn't exist
	    	 else{
	    		curURL = this.loadUrl(sURL);
	    		
	    		if (!curURL.contains("KEGG ID")){
	    			
	    	    	totalgroups.set(totalgroups.size()-1, -1);
	    	    	nameiter++;
	       	        if (nameiter < maxnames){
	       	            oneurlname_parser();
	       	        }
	    		    curiter++;
	    	        if (curiter < maxiters){
	       	            tableline_parser();
	       	        }
	    	        return;
	    		}
	    		
		        String[] curlines = new String[2];     
		        curlines = this.stringFounder(curURL, "KEGG ID");
		   	   					       	    		
	       	    Pattern aminoLength  = Pattern.compile("[(]{1}[0-9]+");
	       	    Matcher m_aminoLength =  aminoLength.matcher(curlines[0]);

	       	    if (m_aminoLength.find()){
	       	      	String allFounded = m_aminoLength.group();
	       	       	String digitsOnly = allFounded.substring(1, allFounded.length());
	       	     uselessaminoNumber = Integer.parseInt(digitsOnly);
	       	    }	
	       	    curlines = this.stringFounder(curlines[1], "Entry");
	       	    curlines = curlines[1].split("\n", 2);
	       	    
	       	    curURL = curlines[1];
		       	
	       	    // If we want to create local base
				if (inputmark || updatemark){
					file.createNewFile();
					this.doubleFilePrinting(file, Integer.toString(uselessaminoNumber), curURL);
				}					 		       	
	    	 }
	    	// Algorithmic part
	    	itishappened(curURL);     
		}catch (IOException e2){ e2.printStackTrace(); }
	}
	
	void itishappened(String curURL){		
		try{
	 		
 		// If same node was analyzed then we just copy group value
    	int groupdone = 0;
    	for (int globali=0; globali<totalnames.size()-1; globali++){
    	    
    		String namedata_first = totalnames.get(globali);
	        List<String> v_namedata_again_first = new ArrayList<String>();         
	        // Find all genes in the same node analysed before current gene
	        while (namedata_first.length() != 0){
	        	String[] curname = namedata_first.split(" ", 2);    
	    	    v_namedata_again_first.add(curname[0]);
	    	        
	    	    if (curname.length == 1){
	    	    	break;
	    	    }
	    	    namedata_first = curname[1];
	    	}
	    		        	        
	        String namedata_second = totalnames.get(curiter);
	        List<String> v_namedata_again_second = new ArrayList<String>();         
	        // Find all genes in the same node analyzed before current gene
	        while (namedata_second.length() != 0){
	        	String[] curname = namedata_second.split(" ", 2);    
	    	    v_namedata_again_second.add(curname[0]);
	    	        
	    	    if (curname.length == 1){
	    	    	break;
	    	    }
	    	    namedata_second = curname[1];
	    	}
	    	    	       	        
	    	for (int iname=0; iname < v_namedata_again_first.size(); iname++ ){
	    		for (int iname2=0; iname2 < v_namedata_again_second.size(); iname2++ ){
		    		if ( v_namedata_again_first.get(iname).equals(v_namedata_again_second.get(iname2)) ){	
		    			totalgroups.set(curiter, totalgroups.get(globali));
		    			groupdone = 1;
		    	        break;
		    	    }
	    		}
	    	}
    	}
    	int homologdetected = 0;
    	
    	// If we didn't find node copy before then we should analyze it
   	    if(groupdone == 0){
   	        String curlines[] = curURL.split("\n", 2);
   	    	String currentline = curlines[0];	    	
            curURL = curlines[1];	        

       	    String strequality = "0.5";
   	        String strscore = "0";
   
   	        Pattern regequality  = Pattern.compile("[\\s]{1}[0-9]{1}[\\.]{1}[0-9]{3}[\\s]{1}");
   	        Pattern regname		 = Pattern.compile("[a-z]{2,9}[:]{1}(\\w+)");
   	        Pattern regSWScore   = Pattern.compile("[\\s]{2}[\\d]+[\\s]{1}");
   	        
   	        Matcher m_regequality;
   	        Matcher m_regname;
   	        Matcher m_SWScore;

   	        String regSep = " ";
   	        String nameline = "";
   	        // The list with genes which identity > user identity threshold
   	        List<String> currentgomologs = new ArrayList<String>(); 	        
   	        m_regequality  = regequality.matcher(currentline);
   	        m_SWScore	   = regSWScore.matcher(currentline);
   	        
   	        int badTablemetka = 0;
   	        
   	        if (m_regequality.find()){
    	        strequality = m_regequality.group();
   	        }
   	        else{
   	        	// No homologs or bad url
   	        	badTablemetka = 1;
   	        }
   	        
   	        if (m_SWScore.find()){
   	        	if (m_SWScore.find()){
   	        		strscore = m_SWScore.group();
   	        	}
   	        }
   	        
   	   	    double curequality = 0;
   		    int curSWScore = 0;
   		    
   		    if (badTablemetka == 0){
   		    	curequality = Double.parseDouble(strequality);
   		    	curSWScore = Integer.parseInt(strscore.trim());
   		    }  
   	        
   	        while (1>0){
   	        	
   	        	if (badTablemetka == 1){
   	        		break;
   	        	}
   	        	
   	        	String[] namelines = currentline.split(regSep, 5);
   	        	m_regname = regname.matcher(namelines[3]);
   	        	if (m_regname.find()){
   	        		nameline = m_regname.group();
   	        	}
   	            if( (equality <= curequality) && (SWScore <= curSWScore) ){
   	            	currentgomologs.add(nameline);
   	            }
    	            
    	   		curlines = curURL.split("\n", 2);
    	   		currentline = curlines[0];
    	   		curURL = curlines[1];

        	    m_regequality  = regequality.matcher(currentline);      	        
        	    if (m_regequality.find()){
        	       	strequality = m_regequality.group();
        	       	curequality = Double.parseDouble(strequality);
        	    }
        	    else{
        	       	break;
        	    }
        	    
        	    m_SWScore = regSWScore.matcher(currentline);
       	        if (m_SWScore.find()){
       	        	if (m_SWScore.find()){
       	        		strscore = m_SWScore.group();
       	        		curSWScore = Integer.parseInt(strscore.trim());
       	        	}
       	        }
        	    else{
        	       	break;
        	    }
    	    }
   	        
   	        List<String> curGeneDomens = null;
   	        ArrayList<String> domensToAccept = null;  
   	        List<ArrayList<String>> allOrtoDomens = null;
	   	    
   	        if (this.domensNumber != 0){
	   	        // Organism's domains loading    		
   	        	curGeneDomens = new ArrayList<String>();
	   		    	
	   		    String tempcurOrgName = v_namedata.get(nameiter).replace(':', '_');
	   	    	String sURL = "http://rest.kegg.jp/get/" + v_namedata.get(nameiter);	        
	   		    File file = new File(mybasedirectory + sep + "Input" + sep + "Domains" + sep + tempcurOrgName + ".txt");	     
	   		    String curURLagain;
	   	    	String line = "";
	   	    	
	   	    	if ((inputmark) && (file.exists()) && (!updatemark)){
	   	    		BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
	   	    		while ((line = reader.readLine()) != null) {
	   	    			curGeneDomens.add(line);
	   				}
	   	  	    	reader.close();
	   	  	    }
	   		    else{
	   		       	curURLagain = this.loadUrl(sURL);	
	   		        curlines = this.stringFounder(curURLagain, "Pfam");
	   	   	    	
	   		        if (curlines[0].contains("Pfam")){		    
				    	curlines = curlines[0].split(": ");
				    	curlines = curlines[1].split(" ");
				    	
				    	for (int domNum=0; domNum<curlines.length; domNum++){
				    		curlines[domNum] = curlines[domNum].trim();
				    		curGeneDomens.add(curlines[domNum]);
				    	}
				    }
	   		   	    
	   		        // If we want to create local base
	   		       	if (inputmark || updatemark){
	   				   	file.createNewFile();				    	  
	   				  	this.listFilePrinting(file, curGeneDomens);
	   				}
	   	        }
	   	        
	   	    	// Homolog domains loading  
	   	        allOrtoDomens = new ArrayList<ArrayList<String>>();
	 	   	    domensToAccept = new ArrayList<String>();  
	    	 	// Domains analyze starting here
	    	    for (int counter=0; counter<currentgomologs.size(); counter++){
	    	    	// Organism loading	    									    	
		 	    	ArrayList<String> curOrtoDomens = new ArrayList<String>();
		 	    	
		 	    	String tempcurOrtoName = currentgomologs.get(counter).replace(':', '_');
		 	    	sURL = "http://rest.kegg.jp/get/" + currentgomologs.get(counter);	        
		 		    file = new File(mybasedirectory + sep + "Input" + sep + "Domains" + sep + tempcurOrtoName + ".txt");
		 	    	line = "";
		
		 	    	if ((inputmark) && (file.exists()) && (!updatemark)){
		     			BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
		     			while ((line = reader.readLine()) != null) {
		     				curOrtoDomens.add(line);
		 		   		}
		   	       		reader.close();
		 	       	}
		 	       	else{
		 	       		curURLagain = this.loadUrl(sURL);
		 	       		curlines = this.stringFounder(curURLagain, "Pfam");
		 			    	   	    	
			 	       	if (curlines[0].contains("Pfam")){		    
					    	curlines = curlines[0].split(": ");
					    	curlines = curlines[1].split(" ");
					    	
					    	for (int domNum=0; domNum<curlines.length; domNum++){
					    		curlines[domNum] = curlines[domNum].trim();
					    		curGeneDomens.add(curlines[domNum]);
					    	}
					    }
		 		   	    
			 	       	// If we want to create local base
		 			   	if (inputmark || updatemark){
		 			   		file.createNewFile();
		 			       	this.listFilePrinting(file, curOrtoDomens);	
		 			    }
		 	       	}	
		 	    	allOrtoDomens.add(curOrtoDomens);
		    	}
	    	    // Analyze of domains array
	 	   	    Map<String, Integer> orthologDomens = new HashMap<String, Integer>(); 
	 	   	    ArrayList<String> uniqueDomens = new ArrayList<String>();
	 	   	    
	 	   	    for (int outto=0; outto<allOrtoDomens.size(); outto++){
	 	   	    	for (int into=0; into<allOrtoDomens.get(outto).size(); into++){
	 	
	 		   	    	if (orthologDomens.containsKey(allOrtoDomens.get(outto).get(into))){
	 						int curnum = orthologDomens.get(allOrtoDomens.get(outto).get(into))+1;
	 						orthologDomens.replace(allOrtoDomens.get(outto).get(into), curnum);
	 					}
	 		   	    	else{
	 		   	    		orthologDomens.put(allOrtoDomens.get(outto).get(into), 1);
	 		   	    		uniqueDomens.add(allOrtoDomens.get(outto).get(into));
	 		   	    	}
	 	   	    	}	
	 			}	 	   	    
	 	   		tempcurOrgName = v_namedata.get(nameiter).replace(':', '_');
	 	   		PrintStream outdomain;
			    if (homologyType == "Paralogs search"){
			    	outdomain = new PrintStream(mybasedirectory + sep + "Output" + sep + "ParalogDomains" + sep + tempcurOrgName + ".txt");
			    }
			    else{
			    	outdomain = new PrintStream(mybasedirectory + sep + "Output" + sep +"OrthologDomains" + sep + tempcurOrgName + ".txt");
			    }
	    		 
	    		outdomain.println("Gene domains:");	    	
	    		
	    		for (int cou = 0; cou < curGeneDomens.size(); cou++){   			   		    	    	
	    			outdomain.println(curGeneDomens.get(cou));   	
	    		}
	    		outdomain.println();
			    if (homologyType == "Paralogs search"){
			    	outdomain.println("Paralog domains:");
			    }
			    else{
			    	outdomain.println("Ortholog domains:");
			    }
	    		    		   
		 	    List<Entry<String,Integer>> orthologDomensSorted = new ArrayList<Entry<String,Integer>>(orthologDomens.entrySet());
		 		Collections.sort(orthologDomensSorted, new Comparator<Entry<String,Integer>>() {		
		 			public int compare(Entry<String,Integer> e1, Entry<String,Integer> e2) {
		 				return e2.getValue().compareTo(e1.getValue());
		 			}   		 
		 		});
		 	   
		 		int domSize = Math.min(this.domensNumber, curGeneDomens.size());
		 		int missedDomens = 0;
		 		for (int cou = 0; cou < orthologDomensSorted.size(); cou++){
		 			 		 
		 			int digits = 30 - orthologDomensSorted.get(cou).getKey().length();
		 			outdomain.print(orthologDomensSorted.get(cou).getKey());	    	
		 			for (int dig=0; dig<digits; dig++){
		 				outdomain.print(" ");	  
		 			}
		 			outdomain.println((double)orthologDomensSorted.get(cou).getValue()/allOrtoDomens.size());	
		 			
		 			// Only specified domains
		 			if (this.domenmark){
		 				if (selectedDomens.contains(orthologDomensSorted.get(cou).getKey().toUpperCase())){
		 					domensToAccept.add(orthologDomensSorted.get(cou).getKey());
		 				}
		 				continue;
		 			}
		 			
		 			// Only presented domains based on users domain threshold 
					if ( (cou < (domSize + missedDomens)) && (curGeneDomens.contains(orthologDomensSorted.get(cou).getKey())) ){
						domensToAccept.add(orthologDomensSorted.get(cou).getKey());
					}
					else{
						missedDomens++;
					}
		 		}
	   	        
		 	   
			    if (homologyType == "Paralogs search"){
			    	outdomain.println("Number of analized paralogs: " + allOrtoDomens.size());	
			    }
			    else{
			    	outdomain.println("Number of analized orthologs: " + allOrtoDomens.size());	
			    }
		 		outdomain.close();
   	        }
 		
	 		// Based on final homology list we can find a group
    	    for (int globali=0; globali<totalnames.size()-1; globali++){	
    	    	
   	        	if (badTablemetka == 1){
   	        		break;
   	        	}
   	        	
    	        // All genes names
    	        String namedata = totalnames.get(globali);
    	        List<String> v_namedata_again = new ArrayList<String>();

    	        // Unite all genes in node before current gene
    	        while (namedata.length() != 0){
    	    	    String[] curname = namedata.split(" ", 2);    
    	    	    v_namedata_again.add(curname[0]);
    	    	       
    	    	    if (curname.length == 1){
    	    	     	break;
    	    	    }
    	    	    namedata = curname[1];
    	    	}	    
	   	        //  If any genes of current (globali) node contained in homology list of
    	        //  current (curiter/nameiter) gene then current gene will obtain the group number of founded (globali)
    	        //  gene, else it obtains new group number (maxgroup+1)
    	        int javametka = 0;	    	
    	        for (int i=0; i< v_namedata_again.size(); i++){    	        	
    	            for (int j=0; j< currentgomologs.size(); j++){  
    	            	
    	            	// Domains criteria selection 
    	            	if (this.domensNumber != 0){
    	    	   	    	int domainNotFoundMetka = 0;
    	    	   	    	for (int id = 0; id < domensToAccept.size(); id++){   	    	   	    			   	    		
    	    	   	    		if (!allOrtoDomens.get(j).contains(domensToAccept.get(id))){
    	    	   	    			domainNotFoundMetka = 1;
    	    	   	    			break;
    	    	   	    		}
    	    	   	    	}
    	    	   	    	if (domainNotFoundMetka == 1){
    	    	   	    		continue;  	    	   	    		
    	    	   	    	}
    	    			}
    	            	    	            	
    	                if (currentgomologs.get(j).equals(v_namedata_again.get(i))){
    	                	// If group == 666 ( first time analyzed) then give the node a group.
    	                	// If group != 666 then gives the node group same with founded.    	                	    		        	    	
    	                	if (totalgroups.get(curiter) == 666){
        	                	totalgroups.set(curiter, totalgroups.get(globali));        	                    	                	
    	                	}
    	                	else{
    	                		int orthogroup_to_be_changed =  totalgroups.get(globali);
        	                	totalgroups.set(globali, totalgroups.get(curiter));
        	                	// Regrouping old nodes based on new info. 
        	                	for (int deepglobali=0; deepglobali<totalnames.size()-1; deepglobali++){      
        	                		if (totalgroups.get(deepglobali) == orthogroup_to_be_changed){       	                		      			
                	                	totalgroups.set(deepglobali, totalgroups.get(curiter));
            	                	}	
        	            	    }  	
    	                	}       	                	    	                 	                    	        	          	        	        
    	                	homologdetected = 1;
    	                    javametka = 1;
    	                    break;
    	                }
    	            }
    	            if (javametka == 1){
    	              	break;
    	    	    }
    	        }
    	    }
   	    }
   	    // If we found another copy of node. iterations counter ++ , groups counter same as above.
    	if (groupdone == 1){   	    	
    	    curiter++;
   	        if (curiter < maxiters){
   	            tableline_parser();
   	        }               	   	        	
   	    }
    	// If it's unique node we continue it's analysis
   	    else{
   	        nameiter++;
   	        if (nameiter < maxnames){
   	            oneurlname_parser();
   	        }
   	        else{
   	        	if( (homologdetected == 0) && (totalgroups.get(curiter) == 666) ){
   	        		maxgroupnumber++;
   	   	        	totalgroups.set(curiter, maxgroupnumber);
   	        	}
   	            curiter++;
   	            if (curiter < maxiters){
   	                tableline_parser();
   	            }
   	        }
   	    }
	 }catch (IOException e2){ e2.printStackTrace(); }
     }	
	
	// Writing the results into Cytoscape table
	private void resultsconfirming(){
 		CyColumn mysuidcolumn = nodeTable.getColumn("SUID");
 		List<Long> suidstorage;		
 		suidstorage = mysuidcolumn.getValues(Long.class);
	            	
       	List<Integer> allgroups = new ArrayList<Integer>();
       	for (int i=0; i<totalgroups.size(); i++){
       		if ( !allgroups.contains(totalgroups.get(i)) && (totalgroups.get(i) != -1) ){
   			allgroups.add(totalgroups.get(i));
       		}
      	}
	           	
	    for (int i=0; i<totalgroups.size(); i++){
	    	for (int j=0; j<allgroups.size(); j++){
	    		if (totalgroups.get(i) == allgroups.get(j)){
	    			totalgroups.set(i, j);
	    		}
	    	}
	    }  	            	  	            	
	    for (int i=0; i<nodeTable.getRowCount(); i++){
	        CyRow nodeRow = nodeTable.getRow(suidstorage.get(i));
	        nodeRow.set("Homology Cluster", totalgroups.get(i));             
	  	}   	   	        			   	          	   	        	
	}
	
	// Url loading function
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
	
	// Specific substring search in big string 
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
	// Function to print 2 strings (using here to print orthologues and paralogues bases)
	public void doubleFilePrinting(File file, String data1, String data2){
	    try {
	    	PrintStream outStream = new PrintStream(file.toString());
	    	outStream.println(data1);
	    	outStream.println(data2);
	    	outStream.close();	
		}catch (IOException e2){ e2.printStackTrace(); }
	}
	// Function to print string's list (using to print domains)  
	public void listFilePrinting(File file, List<String> data){
		try {
	    	PrintStream outStream = new PrintStream(file.toString());
	    	for (int z=0; z<data.size(); z++){
	    		outStream.println(data.get(z));							    		
	    	}
	    	outStream.close();	
		}catch (IOException e2){ e2.printStackTrace(); }
	}
}
