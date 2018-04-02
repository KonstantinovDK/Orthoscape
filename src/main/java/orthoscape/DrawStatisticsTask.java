package orthoscape;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class DrawStatisticsTask extends AbstractTask {

	List<XYSeries> alotofdata;		// Array with data required to draw Png's (using JFreeChart)
    List<String> alotofnames;		// Array with networks names
    List<String> alotoforgs;		// Array with organisms names (To analyze same networks of different organisms).
    
    ArrayList<ArrayList<String>> Rmasses;				// Array with data required to create global R scripts
    ArrayList<ArrayList<String>> Rlabels;				// Array with names required to create global R scripts
    
    List<String> uniqueRNames;							// Array with names of identity/SW-Score pare
    List<String> uniqueOrgs;							// Array with organisms to control same named networks
    													// In current algorithm they are not unique by themselves but any their pare is unique
    
    List<Integer> uniqueTaxes;							// Array with number of taxons. Not unique at all :) Just to the same name pattern
//    Integer[] colors = {3,8,12,19,31,33,43,48,53,57,62,63,66,76,84,86,91,101,102,109,116};	// Colors good enough to use in R violin plots 
    
    double PAIvaltoExcel;
    double PAIvartoExcel;
    
	File mybasedirectory;
	String sep =  File.separator;
	Boolean cancelled = false;
	public DrawStatisticsTask(CyNetwork network){
		// Form to choose the base
		JFrame myframe = new JFrame();
		JFileChooser dialog = new JFileChooser();
		dialog.setDialogTitle("Choose local base directory");
		dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		dialog.setAcceptAllFileFilterUsed(false); 
	    
		// Local base initialization
		int returnValue = dialog.showOpenDialog(myframe);		
		if (returnValue == javax.swing.JFileChooser.APPROVE_OPTION){
			this.mybasedirectory = dialog.getSelectedFile();
			dialog.setVisible(true);
			
			File dir = new File(mybasedirectory + sep);
		    if (!dir.isDirectory()){
		    	return;
		    }
		}
		else{
		    // "Cancel" button option
			System.out.println("Cancelled");
			cancelled = true;
			return;
		}
			          	   	
    	alotofdata = new ArrayList<XYSeries>();
        alotofnames = new ArrayList<String>();
        alotoforgs = new ArrayList<String>();
        
        Rmasses = new ArrayList<ArrayList<String>>();
        Rlabels = new ArrayList<ArrayList<String>>();
        uniqueRNames = new ArrayList<String>();
        uniqueOrgs = new ArrayList<String>();
        uniqueTaxes = new ArrayList<Integer>();
        
        PAIvaltoExcel = 0;
        PAIvartoExcel = 0;
	}
	
	public void run(TaskMonitor monitor) {			
		if (cancelled){
			return;
		};
		
		// Here we counting 5 current statistics. Every function launch will create their own Png graph
		statisticCounter("Gene set PAI statistic",  "identity");
		statisticCounter("Network PAI statistic",  "identity");
		statisticCounter("Median statistic",  "identity");
		statisticCounter("Oldest statistic",  "identity");
		statisticCounter("Youngest statistic","identity");
		statisticCounter("Gene set PAI statistic",  "SW-Score");
		statisticCounter("Network PAI statistic",  "SW-Score");
		statisticCounter("Median statistic",  "SW-Score");
		statisticCounter("Oldest statistic",  "SW-Score");
		statisticCounter("Youngest statistic","SW-Score");

		// Here we will create html's based on graphs we already create
		reportsCreating();
		RreportsCreating();
		PAITablesCreating();
	}
		
	public void statisticCounter(String statname, String threshold){
	    
		// Find all files in work directory
		File workdir = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep);
		ArrayList<File> filesInDirectory = new ArrayList<File>();
        OrthoscapeHelpFunctions.getListFiles(filesInDirectory, workdir.toString());
        		    	
        // Filter them to find only bar charts (source) data files
        for (int counter=0; counter<filesInDirectory.size(); counter++){	        	
        	String curfilename = filesInDirectory.get(counter).toString();
        	if (!curfilename.contains("source")){
        		continue;
        	}
        	
        	String[] fileSplitter;
        	fileSplitter = curfilename.split(sep+sep);
        			        	
        	String curnetname = fileSplitter[fileSplitter.length-2];
        	String curorg = fileSplitter[fileSplitter.length-3];
        	int curidentity=0;
        	int curSWScore=0;
            	 
        	// source files must have 4 blocks in their names separated by "___".
        	String[] namedatas = fileSplitter[fileSplitter.length-1].split("___");
        	
        	String tempID = "";
        	String tempSW = "";
        	if (namedatas.length < 4){
        		// Wrong source file. If we are here then program bugged on source files creating step.
        	}
        	if (namedatas.length > 4){
        		// If we are here then network name contains "___". Theoretically possible...
        		for (int i=0; i<namedatas.length-3; i++){
        			curnetname += namedatas[i] + "___";
        		}
        		curnetname += namedatas[namedatas.length-3];
        		
        		String curid=namedatas[namedatas.length-2];
        		String[] datas = curid.split("=");
        		tempID = datas[1];
        		Double tempidentity = Double.parseDouble(datas[1]);
        		curidentity = (int) (100*tempidentity);
        		curid=namedatas[namedatas.length-1];
        		datas = curid.split("=");
        		tempSW = datas[1].replace(".txt", "");
        		String newdata = datas[1].replace(".txt", "");
        		curSWScore = Integer.parseInt(newdata);    
        	}
        	if (namedatas.length == 4){
            	// Good file
        		String curid=namedatas[2];
        		String[] datas = curid.split("=");
        		tempID = datas[1];
        		Double tempidentity = Double.parseDouble(datas[1]);
        		curidentity = (int) (100*tempidentity);
        		curid=namedatas[3];
        		datas = curid.split("=");
        		tempSW = datas[1].replace(".txt", "");
        		String newdata = datas[1].replace(".txt", "");
        		curSWScore = Integer.parseInt(newdata);       		
        	}             
        	
        	// Taxons data to create bar charts
        	List<Double> taxdata = new ArrayList<Double>();
        	String line = "";
        	double totalsum=0;
        	    	           	
        	int counter01 = 0;
        	double TDIweighted;
        	
        	double curSum[] = new double[4];
        	// Loading of the data: PAI taxons number, number of orthologs, Network PAI, number of genes, number of edges.  
        	try{
	       		BufferedReader reader = new BufferedReader(new FileReader(filesInDirectory.get(counter).toString()));
	  	       	while ((line = reader.readLine()) != null) {
			   		taxdata.add(Double.parseDouble(line));
			   		totalsum+=Double.parseDouble(line);
			   		
			   		curSum[counter01] = Double.parseDouble(line);
			   		counter01++;
			   		if (counter01 == 4){
			   			counter01 = 0;
			   		}
			   	}	
	  	       	reader.close();
        	}catch (IOException e2){ System.out.println("Can't read the file " + filesInDirectory.get(counter).toString());}
  	       	
  	       	totalsum -= curSum[0];
  	       	totalsum -= curSum[1];
  	        totalsum -= curSum[2];
  	        totalsum -= curSum[3];
  	        TDIweighted = taxdata.get(taxdata.size()-3);
  	    	taxdata.remove(taxdata.size()-1);
  	       	taxdata.remove(taxdata.size()-1);
  	       	taxdata.remove(taxdata.size()-1);
  	      	taxdata.remove(taxdata.size()-1);
        	         	
        	int ic=0;
        	int nameexist=0;
        	   	    	
        	// Statistics value counting. It will be used on global graphs
        	for (ic=0; ic<alotofnames.size(); ic++){
        		// If we are here then this pair (network, organism) obtained has already been analyzed before (i.e. some identity-SWScore data already exist)
        		if (alotofnames.get(ic).equals(curnetname) && alotoforgs.get(ic).equals(curorg)){
        			double statistic=0;
        			
        			if (statname.equals("Gene set PAI statistic")){  
        				double sumx2 = 0;
	    				double sum = 0;
            			for (int i=1; i<taxdata.size(); i++){
            				statistic+=taxdata.get(i)*i;
            				
            				// To count variance
	        				sumx2 += taxdata.get(i)*i*i;
	        				sum+=taxdata.get(i)*i;
            			}
            			statistic = statistic/totalsum;
            			PAIvaltoExcel = statistic;
	        			PAIvartoExcel = Math.sqrt((sumx2 - sum*sum/totalsum)/totalsum);
        			}
        			if (statname.equals("Network PAI statistic")){           			
            			statistic = TDIweighted;
        			}
        			if (statname.equals("Median statistic")){  
        				double particalsum = taxdata.get(taxdata.size()-1);
            			for (int i=taxdata.size()-1; i>=0; i--){
            				if (i == 0){
            					statistic=i;
            					break;
            				}
            				if (particalsum < totalsum/2){
            					particalsum += taxdata.get(i-1);
            					continue;
            				}
            				statistic=i;
            				break;
            			}
        			}
        			
        			if (statname.equals("Oldest statistic")){           			
            			for (int i=0; i<taxdata.size()-1; i++){
            				if (taxdata.get(i) == 0){
            					continue;
            				}
            				statistic = i;
            				break;
            			}
            		}
        			if (statname.equals("Youngest statistic")){           			
            			for (int i=taxdata.size()-1; i>=0; i--){
            				if (taxdata.get(i) == 0){
            					continue;
            				}
            				statistic = i;
            				break;
            			}
        			}
        			
        			if (threshold.equals("identity")){
        				alotofdata.get(ic).add(curidentity, statistic);
        			}
        			if (threshold.equals("SW-Score")){
        				alotofdata.get(ic).add(curSWScore, statistic);
        			}
        			           			
        			nameexist=1;
        			break;
        		}
        	}    
        	if (nameexist == 0){
        		// If we are here then this pair (network, organism) obtained by the first time (i.e. identity-SWScore doesn't exist)
        		alotofnames.add(curnetname);
    			alotoforgs.add(curorg);
    			XYSeries serie = new XYSeries(curnetname);
    			
    			double statistic=0;
    			
    			if (statname.equals("Gene set PAI statistic")){    
    				double sumx2 = 0;
    				double sum = 0;
        			for (int i=1; i<taxdata.size(); i++){
        				statistic+=taxdata.get(i)*i;
        				
        				// To count variance
        				sumx2 += taxdata.get(i)*i*i;
        				sum += taxdata.get(i)*i;
        			}
        			statistic = statistic/totalsum;
        			PAIvaltoExcel = statistic;
        			PAIvartoExcel = Math.sqrt((sumx2 - sum*sum/totalsum)/totalsum);
    			}
    			if (statname.equals("Network PAI statistic")){           			
        			statistic = TDIweighted;
    			}
    			if (statname.equals("Median statistic")){  
    				double particalsum = taxdata.get(taxdata.size()-1);
        			for (int i=taxdata.size()-1; i>=0; i--){
        				if (i == 0){
        					statistic=i;
        					break;
        				}
        				if (particalsum < totalsum/2){
        					particalsum += taxdata.get(i-1);
        					continue;
        				}
        				statistic=i;
        				break;
        			}
    			}
    			
    			if (statname.equals("Oldest statistic")){           			
        			for (int i=0; i<taxdata.size()-1; i++){
        				if (taxdata.get(i) == 0){
        					continue;
        				}
        				statistic = i;
        				break;
        			}
        		}
    			if (statname.equals("Youngest statistic")){           			
        			for (int i=taxdata.size()-1; i>=0; i--){
        				if (taxdata.get(i) == 0){
        					continue;
        				}
        				statistic = i;
        				break;
        			}
    			}
    			
    			if (threshold.equals("identity")){
    				serie.add(curidentity, statistic);
    			}
    			if (threshold.equals("SW-Score")){
    				serie.add(curSWScore, statistic);
    			}
    			alotofdata.add(serie);    
    		}
        	// New section to print PAI and PAI Variance to .txt tab separated file
	        if ((statname.equals("Gene set PAI statistic")) && (threshold.equals("identity"))){
	        	File dir = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + curorg + sep + "Excel stats" + sep);
		    	if (!dir.isDirectory()){
				    dir.mkdir();
			   	}
		    	
		    	try{
	//		    	File curExcelFile = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + curorg + sep + "Excel stats" + sep + "identity=" + tempID + "_SW-Score=" + tempSW + ".txt");
			    	String fileName = mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + curorg + sep + "Excel stats" + sep + "PAI___identity=" + tempID + "_SW-Score=" + tempSW + ".txt";
		        	PrintWriter PAItoExcel = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName, true)));
		        	PAItoExcel.println(curnetname + "\t" + PAIvaltoExcel + "\t" + (int)totalsum + "\t" + PAIvartoExcel);
		        	PAItoExcel.close();
		        	
		        	String DIfilename = curfilename.replace("source", "PamlDI");
		        	File DIfile = new File(DIfilename);
		        	if (DIfile.exists()){
		        		double nonzeros = 0;
		        		int total = 0;
		        		double DIsum = 0;
		        		BufferedReader DIreader = new BufferedReader(new FileReader(DIfile.toString()));
		        		String DIline = DIreader.readLine();
		        		if (DIline != null){
			  	       		String[] DIsplit = DIline.split("\t"); 
					   		while (!DIsplit[2].equals("0.0")){
					   			nonzeros++;
					   			total++;
					   			DIsum += Double.parseDouble(DIsplit[2]);
					   			DIline = DIreader.readLine();
					   			if (DIline == null){
					   				break;
					   			}
					   			DIsplit = DIline.split("\t"); 				   			
					   		}
					   		while (DIline != null){
					   			total++;
					   			DIline = DIreader.readLine();
					   		}				   		
					   	}	
			  	       	DIreader.close();
			  	       				  	       	
			        	fileName = mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + curorg + sep + "Excel stats" + sep + "DIbool___identity=" + tempID + "_SW-Score=" + tempSW + ".txt";
			        	PrintWriter DIbooltoExcel = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName, true)));
			        	DIbooltoExcel.println(curnetname + "\t" + nonzeros + "\t" + total + "\t" + nonzeros/total);
			        	DIbooltoExcel.close();
			        	
			        	fileName = mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + curorg + sep + "Excel stats" + sep + "DIdouble___identity=" + tempID + "_SW-Score=" + tempSW + ".txt";
			        	PrintWriter DIdoubletoExcel = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName, true)));
			        	DIdoubletoExcel.println(curnetname + "\t" + DIsum + "\t" + total + "\t" + DIsum/total);
			        	DIdoubletoExcel.close();
		        	}
		        	
		    	}catch (IOException e2){ System.out.println("Can't create am Excel statistics file");; }
	        }
        }    
        // Make the list of organisms only
        List<String> alotofuniqueorgs = new ArrayList<String>();
        for (int j=0; j<alotoforgs.size(); j++){
        	if (!alotofuniqueorgs.contains(alotoforgs.get(j))){
        		alotofuniqueorgs.add(alotoforgs.get(j));
        	}
        }
        
        // Using unique organisms list we can make directories and global graphics
        for (int z=0; z<alotofuniqueorgs.size(); z++){
       		XYSeriesCollection xyDataset = new XYSeriesCollection();
       		for (int j=0; j<alotofdata.size(); j++){
       			if (alotoforgs.get(j).equals(alotofuniqueorgs.get(z))){
       				xyDataset.addSeries(alotofdata.get(j));       		
       			}
       		}
       		String curthreshold = "";
   			if (threshold.equals("identity")){
   				curthreshold = "Identity, %";
   			}
   			if (threshold.equals("SW-Score")){
   				curthreshold = "Smith-Waterman score";
   			}
    			
       		JFreeChart chartik = ChartFactory
       			.createXYLineChart(statname + " values comparison based on " + threshold , curthreshold, statname + "'s value",
       	        xyDataset, PlotOrientation.VERTICAL, true, true, true);
       	     
       	    NumberAxis domAxis = (NumberAxis) chartik.getXYPlot().getDomainAxis();
       	    domAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());  
       	    chartik.getXYPlot().setDomainAxis(domAxis);
       	    
       	    NumberAxis rangeAxis = (NumberAxis) chartik.getXYPlot().getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            chartik.getXYPlot().setRangeAxis(rangeAxis);
        	        
       	    BufferedImage imagik = chartik.createBufferedImage(800, 450);
        	    
       	    File filik = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + alotofuniqueorgs.get(z) + sep + "1.txt");
       	    File dirik = filik.getParentFile();
           	if (!dirik.isDirectory()){
           		dirik.mkdir();
           	}
           	try {
				ImageIO.write(imagik, "PNG", new File(dirik + sep  + statname + " comparison(" + threshold +").PNG"));
			} catch (IOException e) {System.out.println("Can't create network comparison image");}
		}
        
        for (int ser=0; ser<alotofdata.size(); ser++){
        	alotofdata.get(ser).clear();
        }
	}	
	
	public void reportsCreating(){
		PrintStream emptyStream = null;	// Empty stream to avoid potential problems with file creating
		try{
			emptyStream = new PrintStream(System.getProperty("java.io.tmpdir") + sep + "emptyStream.txt");
		}catch (FileNotFoundException e1){
			System.out.println("Can't create am empty stream in system's temp directory");
			try{ // another try in local base directory
				emptyStream = new PrintStream(mybasedirectory + sep + "errorsLog.txt");
			}catch (FileNotFoundException e2){System.out.println("Can't create am empty stream in local base directory");}
		}

		
		// Make the list of organisms only
        List<String> alotofuniqueorgs = new ArrayList<String>();
        for (int j=0; j<alotoforgs.size(); j++){
        	if (!alotofuniqueorgs.contains(alotoforgs.get(j))){
        		alotofuniqueorgs.add(alotoforgs.get(j));
        	}
        }
        // Making the global report
        for (int z=0; z<alotofuniqueorgs.size(); z++){
	    	PrintStream globalHTML = null;
			try {
				globalHTML = new PrintStream(mybasedirectory + sep + "Output" + sep +"Pictures and reports" + sep + alotofuniqueorgs.get(z) + sep + "Global report.html");
			}catch (FileNotFoundException e) {
				System.out.println("Can't create global html report");
				globalHTML = emptyStream;
			}
			globalHTML.println(""+
	    	"<html>" +
			" <head>" +
			"  <title>Global report</title>" +
			" </head>" +
			" <body>" +
			
			" <center><h1>Global analysis of Phylostratigraphic Age Index </h1></center>");
			globalHTML.println("List of  networks:");										 
	    	for (int alot=0; alot < alotofnames.size() ; alot++){
	    		if (alotoforgs.get(alot).equals(alotofuniqueorgs.get(z))){
	    			globalHTML.println("<p><a href=\"" + alotofnames.get(alot) + sep + alotofnames.get(alot) + " report.html" + "\">" + alotofnames.get(alot) + "</a></p>");										
	    		}
	    	}
	    	globalHTML.println(				
			" <center><img alt=\"Gene set PAI statistic values comparison(identity)\" src=\"Gene set PAI statistic comparison(identity).PNG\"></center>" +
			" <br/><br/>" +
			" <center><img alt=\"Gene set PAI statistic values comparison(SW-Score)\" src=\"Gene set PAI statistic comparison(SW-Score).PNG\"></center>" +
			" <br/><br/>" +
			" <center><img alt=\"Network PAI statistic values comparison(identity)\" src=\"Network PAI statistic comparison(identity).PNG\"></center>" +
			" <br/><br/>" +
			" <center><img alt=\"Network PAI statistic values comparison(SW-Score)\" src=\"Network PAI statistic comparison(SW-Score).PNG\"></center>" +
			" <br/><br/>" +
			" <center><img alt=\"Median statistic values comparison(identity)\" src=\"Median statistic comparison(identity).PNG\"></center>" +
			" <br/><br/>" +
			" <center><img alt=\"Median statistic values comparison(SW-Score)\" src=\"Median statistic comparison(SW-Score).PNG\"></center>" +
			" <br/><br/>" +
			" <center><img alt=\"Oldest statistic values comparison(identity)\" src=\"Oldest statistic comparison(identity).PNG\"></center>" +
			" <br/><br/>" +
			" <center><img alt=\"Oldest statistic values comparison(SW-Score)\" src=\"Oldest statistic comparison(SW-Score).PNG\"></center>" +
			" <br/><br/>" +
			" <center><img alt=\"Youngest statistic values comparison(identity)\" src=\"Youngest statistic comparison(identity).PNG\"></center>" +
			" <br/><br/>" +
			" <center><img alt=\"Youngest statistic values comparison(SW-Score)\" src=\"Youngest statistic comparison(SW-Score).PNG\"></center>");
			 
	    	globalHTML.print("This html file generated by ");
	    	globalHTML.print("<a href=\"https://github.com/ZakharM/Orthoscape"+ "\">" + "Orthoscape" + "</a>");										
	    	globalHTML.println(" plugin");
	    	globalHTML.println("</body>" +
			"</html>");
	    	globalHTML.close();
        }
		
		// Reporting each network separately
   	    for (int i=0; i<alotofnames.size(); i++){    	        	    	
		    File subfile = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + alotoforgs.get(i) + sep + alotofnames.get(i) + sep + "1.txt");
		    File subdir = subfile.getParentFile();
	    	File[] filesInsubDirectory = new File(subdir.toString()).listFiles();
	    	
	    	Map<String, String> myTaxonomy = new HashMap<String, String>();       	    	        	    	
	    	if (!myTaxonomy.containsKey(alotoforgs.get(i))){
   	 					
    	 		//Organism loading to get taxonomy
    	 		String curURLagain = "";        	 		
		    	String sURL = "http://www.kegg.jp/dbget-bin/www_bget?" + alotoforgs.get(i);	        
			    File file = new File(mybasedirectory + sep + "Input" + sep + "OrganismBase" + sep + alotoforgs.get(i) + ".txt");
		    	
			    if (alotoforgs.get(i).equals("con")){
			    	curURLagain = "Bacteria; Proteobacteria; Alphaproteobacteria; Rhodobacterales; Rhodobacteraceae; Confluentimicrobium";
			    }
			    else{
			    	if (file.exists()){
			  	    	curURLagain = OrthoscapeHelpFunctions.completeFileReader(file);
				    }
			    	else{      		 	    	
			 			curURLagain = OrthoscapeHelpFunctions.loadUrl(sURL);
			 	    	String[] curlines = OrthoscapeHelpFunctions.stringFounder(curURLagain, "Lineage");
			 	    	curURLagain = curlines[1];
			 	    				
			 	       	String[] curlinesmore;
			 	       	curlinesmore = curURLagain.split(">", 4);
			 	       	curURLagain = curlinesmore[2];
			        	
			 	       	String[] curlinesless;
			 	       	curlinesless = curURLagain.split("<", 2);
			 	       	curURLagain = curlinesless[0];	
			        } 
			    }
    			myTaxonomy.put(alotoforgs.get(i), curURLagain);
    	    }
	    	      	    	
			String[] alltaxes = myTaxonomy.get(alotoforgs.get(i)).split(";");           		
			List<String> taxstorage = new ArrayList<String>();
			
			taxstorage.add("Cellular Organisms");
			for (int t=0; t<=alltaxes.length-1; t++){
				taxstorage.add(alltaxes[t].trim());
			}
	    	PrintStream localHTML = null;
			try {
				localHTML = new PrintStream(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + alotoforgs.get(i) + sep + alotofnames.get(i) + sep + alotofnames.get(i) + " report.html");
			} catch (FileNotFoundException e) {
				System.out.println("Can't create local html report");
				localHTML = emptyStream;
			}
	    	
	    	// It will be unique html for every pair (network, organism)
			localHTML.println(""+
	    	"<html>" +
			 "<head>" +
			  "<title>" + alotoforgs.get(i) + " " + alotofnames.get(i) + " report</title>" +
			 "</head>" +
			 "<body>" +		 
			  "<center><h1>" + "Analysis of Phylostratigraphic Age Index for \"" + alotoforgs.get(i) + " " + alotofnames.get(i) + "\" network." + "</center></h1>" +
			  "go to <a href=\"../Global report.html" + "\">" + "Global report" + "</a>" +
			 "<br/>"); 
			if (alotofnames.get(i).contains("KEGGID")){
				String[] KEGGIDStr = alotofnames.get(i).split("=");
				localHTML.println("go to <a href=\"http://www.kegg.jp/kegg-bin/show_pathway?map=map" + KEGGIDStr[1] + "&show_description=show" + "\">" + "KEGG" + "</a>");
			}
	    	
	    	int geneInfo = 0;
        	
	    	// R script
	    	PrintStream Rscript = null;
			try {
				Rscript = new PrintStream(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + alotoforgs.get(i) + sep + alotofnames.get(i) + sep + alotofnames.get(i) + ".R");
			} catch (FileNotFoundException e) {
				System.out.println("Can't create R script file");
				Rscript = emptyStream;
			}
	    	Rscript.println("require(vioplot)");
	    	int numOfSourceFiles = 0;
	    	List<String> swList = new ArrayList<String>();
	    	List<String> idList = new ArrayList<String>();        	
	    	
        	for (int subcounter=0; subcounter<filesInsubDirectory.length; subcounter++){
	    		String curfilename = filesInsubDirectory[subcounter].getName();
	    		if (curfilename.contains(".PNG")){
	            	
	    			String txtname = curfilename.replace(".PNG", ".txt");
	    			txtname = txtname.replace("graphic", "source");
	            	File txtFileChecker = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + alotoforgs.get(i) + sep + alotofnames.get(i) + sep + txtname);
	            	if (!txtFileChecker.exists()){
	            		continue;
	            	}
	            	numOfSourceFiles++;
	            		            	            	
	            	List<Double> taxdata = new ArrayList<Double>();
	            	String line = "";
	            	double totalsum=0;
	            	          	
	            	int counter = 0;
	            	double curSum[] = new double[4];
	            	double orthologsNumber = 0;
	            	double TDIweighted = 0;
	            	
	            	double totalGenes;
//	            	double totalEdges;
	            	try{	            	         	
		           		BufferedReader reader = new BufferedReader(new FileReader(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + alotoforgs.get(i) + sep + alotofnames.get(i) + sep + txtname));
			  	       	while ((line = reader.readLine()) != null) {
					   		taxdata.add(Double.parseDouble(line));
					   		totalsum+=Double.parseDouble(line);
					   		curSum[counter] = Double.parseDouble(line);
					   		counter++;
					   		if (counter == 4){
					   			counter = 0;
					   		}
					   	}
			  	       	reader.close(); 
	    			}catch (IOException e2){ System.out.println("Can't read " + txtname + " network file");}
		  	       	
		  	        totalsum -= curSum[0];
	      	       	totalsum -= curSum[1];
	      	        totalsum -= curSum[2];
	      	        totalsum -= curSum[3];		  	       	
		  	       	orthologsNumber = taxdata.get(taxdata.size()-4);
		  	       	TDIweighted = taxdata.get(taxdata.size()-3);
		  	        totalGenes = taxdata.get(taxdata.size()-2);
//	      	      	totalEdges = taxdata.get(taxdata.size()-1);
		  	       	taxdata.remove(taxdata.size()-1);
		  	       	taxdata.remove(taxdata.size()-1);
		  	        taxdata.remove(taxdata.size()-1);
	      	      	taxdata.remove(taxdata.size()-1);
		  	       	
		  	       	  	  
		  	       	
		  	       	if (geneInfo == 0){
		  	       		localHTML.print("<br/>");
		  	       		localHTML.println("Number of genes: " + (int)totalGenes);
			  	  //  	htmlik.println("Number of interactions between genes and edges: " + totalEdges);
		  	       		localHTML.println("<hr>");
			  	        geneInfo++;
		  	       	}
		  	       	
		  	       	localHTML.print("<center><img alt=\"");
		  	       	localHTML.print(curfilename);
		  	       	localHTML.print("\" src=\"");
		  	       	localHTML.print(curfilename);
		  	       	localHTML.print("\"></center>");
		    		
		  	       	localHTML.print("<br/><br/>");
		  	       	localHTML.print("<br/>");
		  	       	localHTML.println("<p><a href=\"" + txtname + "\">source</a></p>");										
	            	  		    		
		    		double wstatistic=0;
        			for (int ic=1; ic<taxdata.size(); ic++){
        				wstatistic+=taxdata.get(ic)*ic;
        			}
        			wstatistic = wstatistic/totalsum;
        			
        			// First 3 digits
        			BigDecimal wstatDecimal = new BigDecimal(wstatistic);
        			wstatDecimal = wstatDecimal.setScale(3, BigDecimal.ROUND_HALF_UP);
        			
        			localHTML.println("Gene set PAI = " + wstatDecimal.doubleValue());
        			localHTML.print("<br/>");
	            	
        			// First 3 digits
        			BigDecimal weightDecimal = new BigDecimal(TDIweighted);
        			weightDecimal = weightDecimal.setScale(3, BigDecimal.ROUND_HALF_UP);
	            	
        			localHTML.println("Network PAI = " + weightDecimal.doubleValue());
        			localHTML.print("<br/>");
	            	
	            	String masCollector = "";
	            	masCollector += "= c(";
	            	for (int rr=0; rr<taxdata.size()-1; rr++){
	            		for (int drr=0; drr < taxdata.get(rr); drr++){
	            			masCollector += rr + ",";
	            		}
	            	}
	            	if (taxdata.get(taxdata.size()-1) == 0){
	            		masCollector = masCollector.substring(0, masCollector.length()-1) + ")";
	            	}
	            	else{
	            		for (int drr=0; drr < taxdata.get(taxdata.size()-1)-1; drr++){
	            			masCollector += taxdata.size()-1 + ",";
	            		}
	            		masCollector += taxdata.size()-1 + ")";
	            	}

	            	Rscript.println("mas" + numOfSourceFiles + masCollector);
	            	
	            	
	            	// source files must have 4 blocks in their names separated by "___".
	            	String[] namedatas = txtname.split("___");
            		String curid=namedatas[2];
            		String[] datas = curid.split("=");
            		Double tempidentity = Double.parseDouble(datas[1]);
            		idList.add(String.valueOf((int) (100*tempidentity)));
            		curid=namedatas[3];
            		datas = curid.split("=");
            		String newdata = datas[1].replace(".txt", "");
            		swList.add(Integer.toString(Integer.parseInt(newdata)));         
            		
            		
            		String RdirName = namedatas[2]+"_"+namedatas[3].replace("txt", "R");
            		
            		if ( (uniqueRNames.contains(RdirName)) && (uniqueOrgs.get(uniqueRNames.indexOf(RdirName)).equals(alotoforgs.get(i)))){
            			int ourIndex = uniqueRNames.indexOf(RdirName);
            			Rmasses.get(ourIndex).add(masCollector);
            			Rlabels.get(ourIndex).add("\"" + namedatas[0] + "\"");
            		} 
            		else{
            			uniqueRNames.add(RdirName);
            			uniqueOrgs.add(alotoforgs.get(i));
            			uniqueTaxes.add(taxstorage.size());
            			ArrayList<String> newList1 = new ArrayList<String>();
            			ArrayList<String> newList2 = new ArrayList<String>();
            			Rmasses.add(newList1);
            			Rlabels.add(newList2);
            			int ourIndex = Rmasses.size()-1;
            			Rmasses.get(ourIndex).add(masCollector);
            			Rlabels.get(ourIndex).add("\"" + namedatas[0] + "\"");
            		} 		

		    		int statistic=0;
		    		double particalsum = taxdata.get(taxdata.size()-1);
        			for (int ic=taxdata.size()-1; ic>=0; ic--){
        				if (ic == 0){
        					statistic=ic;
        					break;
        				}
        				if (particalsum < totalsum/2){
        					particalsum += taxdata.get(ic-1);
        					continue;
        				}
        				statistic=ic;
        				break;
        			}      			
            			
        			localHTML.println("Median taxon = " + taxstorage.get(statistic));
        			localHTML.print("<br/>");
        			
	            	statistic=0;
	            	for (int ic=0; ic<taxdata.size()-1; ic++){
        				if (taxdata.get(ic) == 0){
        					continue;
        				}
        				statistic = ic;
        				break;
        			}
	            	localHTML.println("Oldest taxon = " + taxstorage.get(statistic));
	            	localHTML.print("<br/>");
		    		
		    		statistic=0;
        			for (int ic=taxdata.size()-1; ic>=0; ic--){
        				if (taxdata.get(ic) == 0){
        					continue;
        				}
        				statistic = ic;
        				break;
        			}
        			localHTML.println("Youngest taxon = " + taxstorage.get(statistic));    				            			    		    		
        			localHTML.print("<br/><br/>");
		    		
        			localHTML.println("Number of analyzed taxonomy sequences is equal to " + (int)orthologsNumber);    				            			    		    		  				            			    		    		
        			localHTML.print("<br/><br/>");
	    		}
	    	}
        	
        	Rscript.print("allMasses = list(");
        	for (int subcounter=1; subcounter<numOfSourceFiles; subcounter++){
        		Rscript.print("mas" + subcounter + ",");
        	}
        	Rscript.println("mas" + numOfSourceFiles + ")");
        	
        	Rscript.print("allLabels = list(");
        	for (int subcounter=1; subcounter<numOfSourceFiles; subcounter++){
        		Rscript.print("\"SW=" + swList.get(subcounter-1) + ";\n id=" + idList.get(subcounter-1) + "%\",");
        	}
        	Rscript.println("\"SW=" + swList.get(numOfSourceFiles-1) + ";\n id=" + idList.get(numOfSourceFiles-1) + "%\")");
        	
        	Rscript.println("mycolors = c(8,12,19,31,33,43,48,53,57,62,63,66,76,84,86,91,101,102,109,116,3,259,401,404)");
        	Rscript.println("plot(0,0,type=\"n\",xlim=c(0," + 3*(numOfSourceFiles+1) + "), ylim=c(0," + taxstorage.size() + "),  xaxt = 'n', xlab =\"thresholds\", ylab = \"PAI\",  main =\"PAI comparison\")");
        	if (numOfSourceFiles>23){
        		Rscript.println("for (i in 1:" + numOfSourceFiles + ") { vioplot(na.omit(allMasses[[i]]), rectCol=\"gray\", wex=3, at = 3*i, add = T, col = colours()[3*i+1]) }");	
        	}
        	else{
        		Rscript.println("for (i in 1:" + numOfSourceFiles + ") { vioplot(na.omit(allMasses[[i]]), rectCol=\"gray\", wex=3, at = 3*i, add = T, col = colours()[mycolors[i]]) }");
        	}
        	Rscript.println("axis(side=1,at=3*(1:" + numOfSourceFiles + "),labels=allLabels)");
    		Rscript.close();
        	// R script end
	    	
    		localHTML.println(
	    	  "<hr>" +
	    	  "<br/><br/>");
	    	  
	    	localHTML.print("This html file generated by ");
		    localHTML.print("<a href=\"https://github.com/ZakharM/Orthoscape"+ "\">" + "Orthoscape" + "</a>");										
		    localHTML.println(" plugin");
		    		  
	    	  
		    localHTML.println("</body>" +
			"</html>");
    		localHTML.close();    	    
    	}
   	    emptyStream.close();
	}
	
	public void RreportsCreating(){
		PrintStream emptyStream = null;	// Empty stream to avoid potential problems with file creating
		try{
			emptyStream = new PrintStream(System.getProperty("java.io.tmpdir") + sep + "emptyStream.txt");
		}catch (FileNotFoundException e1){
			System.out.println("Can't create am empty stream in system's temp directory");
			try{ // another try in local base directory
				emptyStream = new PrintStream(mybasedirectory + sep + "errorsLog.txt");
			}catch (FileNotFoundException e2){System.out.println("Can't create am empty stream in local base directory");}
		}
		
	    // Creating R global reports for every SW/identity pares									
        for (int z=0; z<uniqueRNames.size(); z++){
        	File dir = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + uniqueOrgs.get(z) + sep + "R scripts" + sep);
	    	if (!dir.isDirectory()){
			    dir.mkdir();
		   	}
	    	PrintStream curIDSWreport = null;
			try {
				curIDSWreport = new PrintStream(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + uniqueOrgs.get(z) + sep + "R scripts" + sep + uniqueRNames.get(z));
			} catch (FileNotFoundException e) {
				System.out.println("Can't create ID-SW unique R report");
				curIDSWreport = emptyStream;
			}
	    	curIDSWreport.println("require(vioplot)");
	    		    	
	    	for (int mas=0; mas<Rmasses.get(z).size(); mas++){
	    		curIDSWreport.println("mas" + (mas+1) + Rmasses.get(z).get(mas));	
	    	}
	    	
	    	curIDSWreport.print("allMasses = list(");	    	
        	for (int subcounter=1; subcounter<Rmasses.get(z).size(); subcounter++){
        		curIDSWreport.print("mas" + subcounter + ",");
        	}       	
        	curIDSWreport.println("mas" + Rmasses.get(z).size() + ")");
        	
        	curIDSWreport.print("allLabels = list(");
        	for (int mas=0; mas<Rlabels.get(z).size()-1; mas++){
        		curIDSWreport.print(Rlabels.get(z).get(mas) + ",");
        	}
        	curIDSWreport.println(Rlabels.get(z).get(Rlabels.get(z).size()-1) + ")");
        	
        	curIDSWreport.println("mycolors = c(8,12,19,31,33,43,48,53,57,62,63,66,76,84,86,91,101,102,109,116,3,259,401,404)");
        	curIDSWreport.println("plot(0,0,type=\"n\",xlim=c(0," + 3*(Rmasses.get(z).size()+1) + "), ylim=c(0," + uniqueTaxes.get(z) + "),  xaxt = 'n', xlab =\"thresholds\", ylab = \"PAI\",  main =\"PAI comparison\")");
        	if (Rmasses.get(z).size()>23){
        		curIDSWreport.println("for (i in 1:" + Rmasses.get(z).size() + ") { vioplot(na.omit(allMasses[[i]]), at = 3*i, add = T, col = colours()[3*i+1]) }");
        		}
        	else{
        		curIDSWreport.println("for (i in 1:" + Rmasses.get(z).size() + ") { vioplot(na.omit(allMasses[[i]]), at = 3*i, add = T, col = colours()[mycolors[i]]) }");
        		}
        	curIDSWreport.println("axis(side=1,at=3*(1:" + Rmasses.get(z).size() + "),labels=allLabels)");
        	curIDSWreport.close();
        }   
        emptyStream.close();
	}
	
	public void PAITablesCreating(){

		// Find all files in work directory
		File workdir = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep);
		ArrayList<File> filesInDirectory = new ArrayList<File>();
        OrthoscapeHelpFunctions.getListFiles(filesInDirectory, workdir.toString());
        
        String oldNetworkName = "oldNetworkName";
        List<String> currentFileCompleteTable = new ArrayList<String>();
        String curorg = "";
        // Filter them to find only bar charts (source) data files
        for (int counter=filesInDirectory.size()-1; counter>=0; counter--){	        	
        	String curfilename = filesInDirectory.get(counter).toString();
        	if (!curfilename.contains("___PAI___")){
        		continue;
        	}

        	String[] fileSplitter;
        	fileSplitter = curfilename.split(sep+sep);
        			        	
        	String curnetname = fileSplitter[fileSplitter.length-2];
        	curorg = fileSplitter[fileSplitter.length-3];
        	double curidentity=0;
 //       	int curSWScore=0;
        	
        	if (!curnetname.equals(oldNetworkName)){
        		if (currentFileCompleteTable.size() != 0){
        			// Print the identity based table of genes with PAI into one file
                	String fileName = mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + curorg + sep + oldNetworkName + sep + oldNetworkName + "_completePAITable.txt";
                	try{
                		PrintStream completeFile = new PrintStream(fileName);
                		for (int i=0; i<currentFileCompleteTable.size(); i++){
                			completeFile.println(currentFileCompleteTable.get(i));
                		}
                		completeFile.close();
                	}catch (IOException e2){ System.out.println("Can't create completeTable file");}

        			currentFileCompleteTable.clear();
        		}
        		oldNetworkName = curnetname;
        	} 
        	// source files must have 4 blocks in their names separated by "___".
        	String[] namedatas = fileSplitter[fileSplitter.length-1].split("___");
        	
        	if (namedatas.length > 4){
        		// If we are here then network name contains "___". Theoretically possible...
        		for (int i=0; i<namedatas.length-3; i++){
        			curnetname += namedatas[i] + "___";
        		}
        		curnetname += namedatas[namedatas.length-3];
        		
        		String curid=namedatas[namedatas.length-2];
        		String[] datas = curid.split("=");
        		curidentity = Double.parseDouble(datas[1]);
        		curid=namedatas[namedatas.length-1];
        		datas = curid.split("=");
 //       		String newdata = datas[1].replace(".txt", "");
 //       		curSWScore = Integer.parseInt(newdata);    
        	}
        	if (namedatas.length == 4){
            	// Good file
        		String curid=namedatas[2];
        		String[] datas = curid.split("=");
        		curidentity = Double.parseDouble(datas[1]);
        		curid=namedatas[3];
        		datas = curid.split("=");
 //       		String newdata = datas[1].replace(".txt", "");
 //       		curSWScore = Integer.parseInt(newdata);       		
        	}    
        	
        	String line = "";
       // 	List<String> currentGene = new ArrayList<String>();
      //  	List<String> currentPAI = new ArrayList<String>();
        	
        	List<String> currentFile = new ArrayList<String>();
        	try{
	       		BufferedReader reader = new BufferedReader(new FileReader(filesInDirectory.get(counter).toString()));
	  	       	while ((line = reader.readLine()) != null) {
			//   		String[] curLine = line.split("\t");
			//   		currentGene.add(curLine[0]+"\t"+curLine[1]);
			//   		currentGene.sort(null);
	  	       		currentFile.add(line);
			   	}	
	  	       	reader.close();
        	}catch (IOException e2){ System.out.println("Can't read the file " + filesInDirectory.get(counter).toString());}
  	       	
        	currentFile.sort(null);
        	if (currentFileCompleteTable.size() == 0){
        		currentFileCompleteTable.add("\t\tidentity\t");
        		currentFileCompleteTable.add("gene ID\tLabel\t\t");
        		for (int liner=0; liner<currentFile.size(); liner++){
        			String curLine[] = currentFile.get(liner).split("\t");
        			currentFileCompleteTable.add(curLine[0] +"\t" + curLine[1] + "\t\t");      		        	
        		}
        	}
        	currentFileCompleteTable.set(0, currentFileCompleteTable.get(0) + curidentity + "\t");
        	for (int liner=0; liner<currentFile.size(); liner++){
    			String curLine[] = currentFile.get(liner).split("\t");
    			currentFileCompleteTable.set(2+liner, currentFileCompleteTable.get(2+liner) + curLine[2].substring(0, 2) + "\t");    		        	
    		}   	      	
        }
        
        if (currentFileCompleteTable.size() != 0){
			// Print the identity based table of genes with PAI into one file
        	String fileName = mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + curorg + sep + oldNetworkName + sep + oldNetworkName + "_completePAITable.txt";
        	try{
        		PrintStream completeFile = new PrintStream(fileName);
        		for (int i=0; i<currentFileCompleteTable.size(); i++){
        			completeFile.println(currentFileCompleteTable.get(i));
        		}
        		completeFile.close();
        	}catch (IOException e2){ System.out.println("Can't create completeTable file");}

			currentFileCompleteTable.clear();
		}    
	}
}