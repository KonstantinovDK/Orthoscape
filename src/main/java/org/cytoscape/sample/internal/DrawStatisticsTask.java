package org.cytoscape.sample.internal;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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
    
	File mybasedirectory;
	String sep =  File.separator;
	public DrawStatisticsTask(CyNetwork network){
		// Form to choose the base
		JFrame myframe = new JFrame();
		JFileChooser dialog = new JFileChooser();
		dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		dialog.setAcceptAllFileFilterUsed(false); 
	    dialog.showOpenDialog(myframe);
	    
	    // Local base initialization
	    this.mybasedirectory = dialog.getSelectedFile();
	    dialog.setVisible(true);
	    // "Cancel" button option
	    File dir = new File(mybasedirectory + sep);
    	if (!dir.exists()){
    		return;
    	}
	      
    	// Old code with bar chat data 
//	    dir = new File(mybasedirectory + sep + "Output" + sep + "Full taxonomy data" + sep);
//    	if (!dir.exists()){
//    		JPanel errorpanel = new JPanel();
//    		errorpanel.setLayout(new BoxLayout(errorpanel, BoxLayout.Y_AXIS));
//    		errorpanel.add(new JLabel("You have to count something in \"Search the age\" mode and save statistics to use this mode."));
//    		errorpanel.add(new JLabel("Don't forget to choose same workspace you used in the \"Search the age\" mode"));
//    		
//    		JOptionPane.showMessageDialog(null, errorpanel);
//    		return;
//    	}	
    	
    	
    	alotofdata = new ArrayList<XYSeries>();
        alotofnames = new ArrayList<String>();
        alotoforgs = new ArrayList<String>(); 
	}
	
	public void run(TaskMonitor monitor) {	
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
	}
		
	public void statisticCounter(String statname, String threshold){
		
	try {   	    
		// Find all files in work directory
		File workdir = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep);
		ArrayList<File> filesInDirectory = new ArrayList<File>();
        getListFiles(filesInDirectory, workdir.toString());
        		    	
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
        		Double tempidentity = Double.parseDouble(datas[1]);
        		curidentity = (int) (100*tempidentity);
        		curid=namedatas[namedatas.length-1];
        		datas = curid.split("=");
        		String newdata = datas[1].replace(".txt", "");
        		curSWScore = Integer.parseInt(newdata);    
        	}
        	if (namedatas.length == 4){
            	// Good file
        		String curid=namedatas[2];
        		String[] datas = curid.split("=");
        		Double tempidentity = Double.parseDouble(datas[1]);
        		curidentity = (int) (100*tempidentity);
        		curid=namedatas[3];
        		datas = curid.split("=");
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
            			for (int i=1; i<taxdata.size(); i++){
            				statistic+=taxdata.get(i)*i;
            			}
            			statistic = statistic/totalsum;
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
        			for (int i=1; i<taxdata.size(); i++){
        				statistic+=taxdata.get(i)*i;
        			}
        			statistic = statistic/totalsum;
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
           	ImageIO.write(imagik, "PNG", new File(dirik + sep  + statname + " comparison(" + threshold +").PNG"));
		}
        
        for (int ser=0; ser<alotofdata.size(); ser++){
        	alotofdata.get(ser).clear();
        }
		}catch (IOException e2){ e2.printStackTrace(); } 
	}	
	
	public void reportsCreating(){
		try{
		// Make the list of organisms only
        List<String> alotofuniqueorgs = new ArrayList<String>();
        for (int j=0; j<alotoforgs.size(); j++){
        	if (!alotofuniqueorgs.contains(alotoforgs.get(j))){
        		alotofuniqueorgs.add(alotoforgs.get(j));
        	}
        }
        // Making the global report
        for (int z=0; z<alotofuniqueorgs.size(); z++){
	    	PrintStream htmlik = new PrintStream(mybasedirectory + sep + "Output" + sep +"Pictures and reports" + sep + alotofuniqueorgs.get(z) + sep + "Global report.html");
	    	htmlik.println(""+
	    	"<html>" +
			" <head>" +
			"  <title>Global report</title>" +
			" </head>" +
			" <body>" +
			
			" <center><h1>Global analysis of Phylostratigraphic Age Index </h1></center>");
	    	htmlik.println("List of  networks:");										 
	    	for (int alot=0; alot < alotofnames.size() ; alot++){
				 htmlik.println("<p><a href=\"" + alotofnames.get(alot) + sep + alotofnames.get(alot) + " report.html" + "\">" + alotofnames.get(alot) + "</a></p>");										
			}
			 htmlik.println(				
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
			 
			 htmlik.println("This html file generated by Orthoscape plugin");	 // Здесь будет гиперссылка на github/app store									
//			 htmlik.println("<p><a href=\"file:///" + mybasedirectory + sep + "Output" + sep + "Pictures and reports"+ sep + alotofuniqueorgs.get(z) + sep + alotofnames.get(alot) + sep + alotofnames.get(alot) + " report.html" + "\">" + alotofnames.get(alot) + "</a></p>");										
				
			htmlik.println("</body>" +
			"</html>");
	    	htmlik.close();
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
			    String line = "";
		    	
		    	if (file.exists()){
		    		BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
		    		StringBuffer result = new StringBuffer();
		  	    	while ((line = reader.readLine()) != null) {
						result.append(line).append("\n");
					}
		  	    	reader.close();
		  	    	curURLagain = result.toString();
			    }
		    	else{      		 	    	
		 			curURLagain = this.loadUrl(sURL);
		 	    	String[] curlines = this.stringFounder(curURLagain, "Lineage");
		 	    	curURLagain = curlines[1];
		 	    				
		 	       	String[] curlinesmore;
		 	       	curlinesmore = curURLagain.split(">", 4);
		 	       	curURLagain = curlinesmore[2];
		        	
		 	       	String[] curlinesless;
		 	       	curlinesless = curURLagain.split("<", 2);
		 	       	curURLagain = curlinesless[0];	
		        } 	
    			myTaxonomy.put(alotoforgs.get(i), curURLagain);
    	    }
	    	      	    	
			String[] alltaxes = myTaxonomy.get(alotoforgs.get(i)).split(";");           		
			List<String> taxstorage = new ArrayList<String>();
			
			taxstorage.add("Cellular Organisms");
			for (int t=0; t<=alltaxes.length-1; t++){
				taxstorage.add(alltaxes[t].trim());
			}
	    	PrintStream htmlik = new PrintStream(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + alotoforgs.get(i) + sep + alotofnames.get(i) + sep + alotofnames.get(i) + " report.html");
	    	
	    	// It will be unique html for every pair (network, organism)
	    	htmlik.println(""+
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
				htmlik.println("go to <a href=\"http://www.kegg.jp/kegg-bin/show_pathway?map=map" + KEGGIDStr[1] + "&show_description=show" + "\">" + "KEGG" + "</a>");
			}
	    	
	    	int geneInfo = 0;
	    	for (int subcounter=0; subcounter<filesInsubDirectory.length; subcounter++){
	    		String curfilename = filesInsubDirectory[subcounter].getName();
	    		if (curfilename.contains(".PNG")){
	            	
	    			String txtname = curfilename.replace(".PNG", ".txt");
	    			txtname = txtname.replace("graphic", "source");
	            	File txtFileChecker = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + alotoforgs.get(i) + sep + alotofnames.get(i) + sep + txtname);
	            	if (!txtFileChecker.exists()){
	            		continue;
	            	}
	            		            	            	
	            	List<Double> taxdata = new ArrayList<Double>();
	            	String line = "";
	            	double totalsum=0;
	            	          	
	            	int counter = 0;
	            	double curSum[] = new double[4];
	            	double orthologsNumber = 0;
	            	double TDIweighted = 0;
	            	
	            	double totalGenes;
//	            	double totalEdges;
	            		            	         	
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
		  	       	
		  	       	reader.close();   	  
		  	       	
		  	       	if (geneInfo == 0){
		  	       	    htmlik.print("<br/>");
			  	       	htmlik.println("Number of genes: " + (int)totalGenes);
			  	  //  	htmlik.println("Number of interactions between genes and edges: " + totalEdges);
		  	       		htmlik.println("<hr>");
			  	        geneInfo++;
		  	       	}
		  	       	
		  	        htmlik.print("<center><img alt=\"");
		    		htmlik.print(curfilename);
		    		htmlik.print("\" src=\"");
		    		htmlik.print(curfilename);
		    		htmlik.print("\"></center>");
		    		
		    		htmlik.print("<br/><br/>");
		    		htmlik.print("<br/>");
		  	        htmlik.println("<p><a href=\"" + txtname + "\">source</a></p>");										
	            	  		    		
		    		double wstatistic=0;
        			for (int ic=1; ic<taxdata.size(); ic++){
        				wstatistic+=taxdata.get(ic)*ic;
        			}
        			wstatistic = wstatistic/totalsum;
        			
        			// First 3 digits
        			BigDecimal wstatDecimal = new BigDecimal(wstatistic);
        			wstatDecimal = wstatDecimal.setScale(3, BigDecimal.ROUND_HALF_UP);
        			
	            	htmlik.println("Gene set PAI = " + wstatDecimal.doubleValue());
	            	htmlik.print("<br/>");
	            	
        			// First 3 digits
        			BigDecimal weightDecimal = new BigDecimal(TDIweighted);
        			weightDecimal = weightDecimal.setScale(3, BigDecimal.ROUND_HALF_UP);
	            	
	            	htmlik.println("Network PAI = " + weightDecimal.doubleValue());
	            	htmlik.print("<br/>");
	            	
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
            			
	            	htmlik.println("Median taxon = " + taxstorage.get(statistic));
	            	htmlik.print("<br/>");
        			
	            	statistic=0;
	            	for (int ic=0; ic<taxdata.size()-1; ic++){
        				if (taxdata.get(ic) == 0){
        					continue;
        				}
        				statistic = ic;
        				break;
        			}
	            	htmlik.println("Oldest taxon = " + taxstorage.get(statistic));
	            	htmlik.print("<br/>");
		    		
		    		statistic=0;
        			for (int ic=taxdata.size()-1; ic>=0; ic--){
        				if (taxdata.get(ic) == 0){
        					continue;
        				}
        				statistic = ic;
        				break;
        			}
	            	htmlik.println("Youngest taxon = " + taxstorage.get(statistic));    				            			    		    		
		    		htmlik.print("<br/><br/>");
		    		
	            	htmlik.println("Number of analyzed taxonomy sequences is equal to " + (int)orthologsNumber);    				            			    		    		  				            			    		    		
		    		htmlik.print("<br/><br/>");
	    		}
	    	}
	    	htmlik.println(
	    	  "<hr>" +
	    	  "<br/><br/>" +
			 "</body>" +
			"</html>");
	    	htmlik.close();    	    
    	}
		}catch (IOException e2){ e2.printStackTrace(); }
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
	
	public static void getListFiles(ArrayList<File> filesIntestDirectory, String str) {
        File f = new File(str);
        for (File s : f.listFiles()) {
            if (s.isFile()) {
            	filesIntestDirectory.add(s);
            } else if (s.isDirectory()) {
                getListFiles(filesIntestDirectory, s.getAbsolutePath());      
            }
        }	        
	}
}