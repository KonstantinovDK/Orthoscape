package orthoscape;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
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

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyTable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import orthoscape.ApplicationConstants;

public class AgeSearchTask extends AbstractTask  implements ApplicationConstants {

	private CyNetwork network;		// current network
	CyTable nodeTable;				// network nodes table
	double equality;				// identity value (homologs with lower value will be rejected)
	int SWScore;					// Smith-Waterman score (homologs with lower value will be rejected)
	File mybasedirectory;			// local base directory
	String sep = File.separator;	// directory separator in current operating system
	
	Boolean inputmark;				// mark to create local storage base
	Boolean updatemark;				// mark to update local storage base
	Boolean outputmark;				// mark to create output data to make analysis and reports
	
	int domensNumber;				// number of domains required to be same (gene-homolog comparison)
	Boolean domenmark;				// mark to use specific domains
	List<String> selectedDomens;	// and place to put these domains
	
	String[] allAges;				// currrent PAI value for every node
	int[] allAgesPower;				// current number of analyzed orthologs for every node
	String orgtax;					// taxonomy row of organism in network
	
	int taxonomyDistance;			// Maximum difference on taxonomic tree. DI threshold
	
	Boolean DImark;					// mark to turn on DI analysis (Niedlmann-Wunsh + KaKs calculator)
	String organismAminoSequence;	// Aminoacid sequence of gene from network
	String orthologAminoSequence;	// Aminoacid sequence of current ortholog
	
	String organismNucleoSequence;	// Nucleotide sequence of gene from network
	String orthologNucleoSequence;	// Nucleotide sequence of current ortholog network
	
	Boolean speciesmark;			// mark to analyze 3 species only:
	Boolean orangutanmark;			// Orangutan
	Boolean bonobomark;				// Bonobo
	Boolean chimpanzeemark;			// Chimpanze
	List<String> selectedSpecies;	// and list to store chosen ones
	
	Boolean bigGenemark;			// mark to (DI)analyze genes with aminoacid sequence length > 7000 
	
	Double[] DISumm;				// current DI values of every node
	Double[] DISummx2;				// current DI*DI values of every node
	Integer[] KsEmpty;				// number of pairs (gene-ortholog) with "infinity" returned by KaKs 
	
	ArrayList<ArrayList<String>> geneorgTable;
	ArrayList<String> geneRow;
	ArrayList<String> orgColumn;

	// Kaks calculator's fields
	// NG - Method variables.
	private double ngKa;
	private double ngKs;
	private double ngKaKs;

	// LWL - Model variables
	private double lwlKa;
	private double lwlKs;
	private double lwlKaKs;
	private double lwlVKa;
	private double lwlVKs;

	// Both NG and LWL
	private double mlwlKa;
	private double mlwlKs;
	private double mlwlKaKs;

	private Map<String, String> nsxMap;
	private Map<String, String> aminoMap;
	private Map<String, String> codonDegreeMap;
	
	public AgeSearchTask(CyNetwork network){    
		
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		JLabel emptylabel1 = new JLabel("\n\n");
		JLabel emptylabel2 = new JLabel("\n\n");
		JLabel emptylabel3 = new JLabel("\n\n");
		JLabel emptylabel4 = new JLabel("\n\n");
		  
		// different input data formats
		MaskFormatter floatformatter = null;
		MaskFormatter longIntegerformatter = null;
		MaskFormatter shortIntegerformatter = null;
		
		try {
			floatformatter = new MaskFormatter("#.###");
			longIntegerformatter = new MaskFormatter("#####");
			shortIntegerformatter = new MaskFormatter("##");
		}catch (ParseException e) {System.out.println("Wrong data format");}
		
		JFormattedTextField equalityField = new JFormattedTextField(floatformatter);
		equalityField.setFocusLostBehavior(JFormattedTextField.COMMIT);

		JFormattedTextField SWequalityField = new JFormattedTextField(longIntegerformatter);
		SWequalityField.setFocusLostBehavior(JFormattedTextField.COMMIT);
		
		JFormattedTextField distanceField = new JFormattedTextField(shortIntegerformatter);
		distanceField.setFocusLostBehavior(JFormattedTextField.COMMIT);
				
		JFormattedTextField domensField = new JFormattedTextField(shortIntegerformatter);
		domensField.setFocusLostBehavior(JFormattedTextField.COMMIT);
		
		JFormattedTextField aminolengthField = new JFormattedTextField(floatformatter);
		aminolengthField.setFocusLostBehavior(JFormattedTextField.COMMIT);
		
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
		
		JLabel storagelabels = new JLabel("It requires to make reports in any time you need without network analysis and it will give you additional information"
										+ " but requires some MB space.");
		storagelabels.setAlignmentX(Component.LEFT_ALIGNMENT);		
				
		// DI initialization
		JLabel DIlabels = new JLabel("Next otpions affect the divergency index analyses only.");
		DIlabels.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		JCheckBox DIbaseBox = new JCheckBox();
		JPanel DIBoxPanel = new JPanel();
		DIBoxPanel.setLayout(new BoxLayout(DIBoxPanel, BoxLayout.X_AXIS));
		DIBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		DIBoxPanel.add(DIbaseBox);
		DIBoxPanel.add(new JLabel(" check it to count divergency index."));
		
		JPanel taxonomyDistanceBoxPanel = new JPanel();
		taxonomyDistanceBoxPanel.setLayout(new BoxLayout(taxonomyDistanceBoxPanel, BoxLayout.X_AXIS));
		taxonomyDistanceBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		taxonomyDistanceBoxPanel.add(new JLabel("Put the acceptable taxonomy distance: "));
		
		base = "1";
		distanceField.setValue(base.trim());
		taxonomyDistanceBoxPanel.add(distanceField);
		
		// mark to turn on DI analysis
		JCheckBox DIspeciesBox = new JCheckBox();
		JPanel DISpeciesPanel = new JPanel();
		DISpeciesPanel.setLayout(new BoxLayout(DISpeciesPanel, BoxLayout.X_AXIS));
		DISpeciesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		DISpeciesPanel.add(DIspeciesBox);
		DISpeciesPanel.add(new JLabel(" check it to use selected species only"));
		
		// mark to choose specific species
		JPanel DIConcreteSpeciesPanel = new JPanel();
		DIConcreteSpeciesPanel.setLayout(new BoxLayout(DIConcreteSpeciesPanel, BoxLayout.X_AXIS));
		DIConcreteSpeciesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JCheckBox DIorangutanBox = new JCheckBox();
		DIConcreteSpeciesPanel.add(DIorangutanBox);
		DIConcreteSpeciesPanel.add(new JLabel(" Pongo abelii (sumatran Orangutan)"));
		JCheckBox DIbonoboBox = new JCheckBox();
		DIConcreteSpeciesPanel.add(DIbonoboBox);
		DIConcreteSpeciesPanel.add(new JLabel(" Pan paniscus (bonobo)"));
		JCheckBox DIchimpanzeeBox = new JCheckBox();
		DIConcreteSpeciesPanel.add(DIchimpanzeeBox);
		DIConcreteSpeciesPanel.add(new JLabel(" Pan troglodytes (chimpanzee)"));
		
		// mark to turn on big sequences analysis
		JCheckBox bigGeneBox = new JCheckBox();
		JPanel bigGeneBoxPanel = new JPanel();
		bigGeneBoxPanel.setLayout(new BoxLayout(bigGeneBoxPanel, BoxLayout.X_AXIS));
		bigGeneBoxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		bigGeneBoxPanel.add(bigGeneBox);
		bigGeneBoxPanel.add(new JLabel(" check it to unlock the analysis of big genes (>7000 aminoacids)."));
		
		JLabel bigGenelabels = new JLabel("It requires a lot of time to work with such data. Be sure your PC is good enough.");
		bigGenelabels.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		// Form filling
		p.add(homologyBoxPanel);
		p.add(SWhomologyBoxPanel);
			p.add(emptylabel1);
		p.add(checkBoxPanel);
		p.add(manylabels);
		p.add(updateBoxPanel);
		p.add(storageBoxPanel);
		p.add(storagelabels);
			p.add(emptylabel2);
		p.add(domensBoxPanel);
		p.add(domensPanel);
		p.add(domenArea);
			p.add(emptylabel3);
			p.add(DIlabels);
		p.add(DIBoxPanel);
		p.add(taxonomyDistanceBoxPanel);
		p.add(DISpeciesPanel);
		p.add(DIConcreteSpeciesPanel);
		p.add(emptylabel4);
		p.add(bigGeneBoxPanel);
		p.add(bigGenelabels);
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
		this.inputmark = localbaseBox.isSelected();
		this.updatemark = updatebaseBox.isSelected();
		
		this.domensNumber = Integer.parseInt(domensField.getText().trim());
		this.taxonomyDistance = Integer.parseInt(distanceField.getText().trim());
		
		this.outputmark = storagebaseBox.isSelected();
		this.DImark = DIbaseBox.isSelected();
		
		this.speciesmark = DIspeciesBox.isSelected();
		this.domenmark = domensBox.isSelected();
		
		this.bigGenemark = bigGeneBox.isSelected();
		
		if (this.inputmark || this.outputmark || this.DImark){
			// Form to choose the base
			JFrame myframe = new JFrame();
			JFileChooser dialog = new JFileChooser();
			dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			dialog.setAcceptAllFileFilterUsed(false); 
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
				System.out.println("Cancelled");
				return;
			}
	        
			// Directories creating
	        if (this.inputmark){
		        File dir = new File(mybasedirectory + sep + "Input" + sep);
			    if (!dir.isDirectory()){
			    	dir.mkdir();
			    }	
			    			        
				dir = new File(mybasedirectory + sep + "Input" + sep + "OrganismBase" + sep);
				if (!dir.isDirectory()){
					dir.mkdir();
				}
			    
			    dir = new File(mybasedirectory + sep +"Input" + sep + "OrthologBase" + sep);
			    if (!dir.isDirectory()){
			    	dir.mkdir();
			    }
			    
			    if (this.domensNumber != 0){
				    dir = new File(mybasedirectory + sep + "Input" + sep + "Domains" + sep);
				    if (!dir.isDirectory()){
				    	dir.mkdir();
				    }
			    }
			    
			    // Directories creating
			    if (this.DImark){
					dir = new File(mybasedirectory + sep + "Input" + sep + "GeneSequenceBase" + sep);
					if (!dir.isDirectory()){
						dir.mkdir();
					}
					
					dir = new File(mybasedirectory + sep +"Input" + sep + "OrthologSequenceBase" + sep);
					if (!dir.isDirectory()){
						dir.mkdir();
					}
					
				    dir = new File(mybasedirectory + sep + "Input" + sep + "DIBase" + sep);
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
			    
			    dir = new File(mybasedirectory + sep + "Output" + sep + "Full ages data" + sep);
				if (!dir.isDirectory()){
					dir.mkdir();
				}				
				
				dir = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep);
		    	if (!dir.isDirectory()){
				    dir.mkdir();
			   	}
		    			    
				if (this.domensNumber != 0){
				    dir = new File(mybasedirectory + sep + "Output" + sep + "OrthologDomains" + sep);
				    if (!dir.isDirectory()){
				    	dir.mkdir();
				    }
				}
				
				// Directories creating
			    if (this.DImark){
			    	 dir = new File(mybasedirectory + sep + "Output" + sep +"KaKs" + sep);
				     if (!dir.isDirectory()){
				    	 dir.mkdir();
				     }	
				     
			    	 dir = new File(mybasedirectory + sep + "Output" + sep + "AlignedSequences" + sep);
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
	        }catch (IOException e2){ System.out.println("Can't create a baseVersion file"); }
		}
		
		// Specific DI species list initialization
		if (this.speciesmark){
			selectedSpecies = new ArrayList<String>();
			this.orangutanmark = DIorangutanBox.isSelected();
			this.bonobomark = DIbonoboBox.isSelected();
			this.chimpanzeemark = DIchimpanzeeBox.isSelected();
			
			if (this.orangutanmark){
				selectedSpecies.add("pon");
			}
			if (this.bonobomark){
				selectedSpecies.add("pps");
			}
			if (this.chimpanzeemark){
				selectedSpecies.add("ptr");
			}
		}
		
		// Domains initialization
		if (this.domenmark){
			
			selectedDomens = new ArrayList<String>();
					
			String unseparatedDomens = domenArea.getText();
			String[] separatedDomens = unseparatedDomens.split(","); 
			
			for (int dom=0; dom<separatedDomens.length; dom++){
				selectedDomens.add(separatedDomens[dom].trim().toUpperCase());
			}			
		}
        			
		this.network = network;				
		this.nodeTable = network.getDefaultNodeTable();
		this.orgtax = "";
		this.organismAminoSequence = "";
		this.orthologAminoSequence = "";
		
		geneRow = new ArrayList<String>();
		orgColumn = new ArrayList<String>();
		geneorgTable = new ArrayList<ArrayList<String>>();
				
		// KaKs fields
		// NSX Map
		nsxMap = new HashMap<String, String>();
		nsxMap.put(ApplicationConstants.PHENYLALANINE, "NNX");
		nsxMap.put(LEUCINE, "XNX");
		nsxMap.put(ISOLEUCINE, "NNX");
		nsxMap.put(METHIONINE, "NNN");
		nsxMap.put(VALINE, "NNS");
		nsxMap.put(SERINE1, "NNS");
		nsxMap.put(SERINE2, "XNX");
		nsxMap.put(PROLINE, "NNS");
		nsxMap.put(THREONINE, "NNS");
		nsxMap.put(ALANINE, "NNS");
		nsxMap.put(TYROSINE, "NNX");
		nsxMap.put(STOP_CODON, "NXX");
		nsxMap.put(HISTIDINE, "NNX");
		nsxMap.put(GLUTAMINE, "NNX");
		nsxMap.put(ASPARAGINE, "NNX");
		nsxMap.put(LYSINE, "NNX");
		nsxMap.put(ASPARTIC_ACID, "NNX");
		nsxMap.put(GLUTAMIC_ACID, "NNX");
		nsxMap.put(CYSTEINE, "NNX");
		nsxMap.put(TRYPTOPHAN, "NNN");
		nsxMap.put(ARGININE, "XNX");
		nsxMap.put(GLYCINE, "NNS");

		// Amino acid Map
		aminoMap = new HashMap<String, String>();
		aminoMap.put("UUU", PHENYLALANINE);
		aminoMap.put("UUC", PHENYLALANINE);
		aminoMap.put("UUA", LEUCINE);
		aminoMap.put("UUG", LEUCINE);
		aminoMap.put("CUU", LEUCINE);
		aminoMap.put("CUC", LEUCINE);
		aminoMap.put("CUA", LEUCINE);
		aminoMap.put("CUG", LEUCINE);
		aminoMap.put("AUU", ISOLEUCINE);
		aminoMap.put("AUC", ISOLEUCINE);
		aminoMap.put("AUA", ISOLEUCINE);
		aminoMap.put("AUG", METHIONINE);
		aminoMap.put("GUU", VALINE);
		aminoMap.put("GUC", VALINE);
		aminoMap.put("GUA", VALINE);
		aminoMap.put("GUG", VALINE);
		aminoMap.put("UCU", SERINE1);
		aminoMap.put("UCC", SERINE1);
		aminoMap.put("UCA", SERINE1);
		aminoMap.put("UCG", SERINE1);
		aminoMap.put("CCU", PROLINE);
		aminoMap.put("CCC", PROLINE);
		aminoMap.put("CCA", PROLINE);
		aminoMap.put("CCG", PROLINE);
		aminoMap.put("ACU", THREONINE);
		aminoMap.put("ACC", THREONINE);
		aminoMap.put("ACA", THREONINE);
		aminoMap.put("ACG", THREONINE);
		aminoMap.put("GCU", ALANINE);
		aminoMap.put("GCC", ALANINE);
		aminoMap.put("GCA", ALANINE);
		aminoMap.put("GCG", ALANINE);
		aminoMap.put("UAU", TYROSINE);
		aminoMap.put("UAC", TYROSINE);
		aminoMap.put("UAA", STOP_CODON);
		aminoMap.put("UAG", STOP_CODON);
		aminoMap.put("UGA", STOP_CODON);
		aminoMap.put("CAU", HISTIDINE);
		aminoMap.put("CAC", HISTIDINE);
		aminoMap.put("CAA", GLUTAMINE);
		aminoMap.put("CAG", GLUTAMINE);
		aminoMap.put("AAU", ASPARAGINE);
		aminoMap.put("AAC", ASPARAGINE);
		aminoMap.put("AAA", LYSINE);
		aminoMap.put("AAG", LYSINE);
		aminoMap.put("GAU", ASPARTIC_ACID);
		aminoMap.put("GAC", ASPARTIC_ACID);
		aminoMap.put("GAA", GLUTAMIC_ACID);
		aminoMap.put("GAG", GLUTAMIC_ACID);
		aminoMap.put("UGU", CYSTEINE);
		aminoMap.put("UGC", CYSTEINE);
		aminoMap.put("UGG", TRYPTOPHAN);
		aminoMap.put("CGU", ARGININE);
		aminoMap.put("CGC", ARGININE);
		aminoMap.put("CGA", ARGININE);
		aminoMap.put("CGG", ARGININE);
		aminoMap.put("AGU", SERINE2);
		aminoMap.put("AGC", SERINE2);
		aminoMap.put("AGA", ARGININE);
		aminoMap.put("AGG", ARGININE);
		aminoMap.put("GGU", GLYCINE);
		aminoMap.put("GGC", GLYCINE);
		aminoMap.put("GGA", GLYCINE);
		aminoMap.put("GGG", GLYCINE);

		// Codon Degree Map
		codonDegreeMap = new HashMap<String, String>();
		codonDegreeMap.put(PHENYLALANINE, "002");
		codonDegreeMap.put(LEUCINE, "204");
		codonDegreeMap.put(ISOLEUCINE, "002");
		codonDegreeMap.put(METHIONINE, "000");
		codonDegreeMap.put(VALINE, "004");
		codonDegreeMap.put(SERINE1, "004");
		codonDegreeMap.put(SERINE2, "004");
		codonDegreeMap.put(PROLINE, "004");
		codonDegreeMap.put(THREONINE, "004");
		codonDegreeMap.put(ALANINE, "004");
		codonDegreeMap.put(TYROSINE, "002");
		codonDegreeMap.put(STOP_CODON, "022");
		codonDegreeMap.put(HISTIDINE, "002");
		codonDegreeMap.put(GLUTAMINE, "002");
		codonDegreeMap.put(ASPARAGINE, "002");
		codonDegreeMap.put(LYSINE, "002");
		codonDegreeMap.put(ASPARTIC_ACID, "002");
		codonDegreeMap.put(GLUTAMIC_ACID, "002");
		codonDegreeMap.put(CYSTEINE, "002");
		codonDegreeMap.put(TRYPTOPHAN, "000");
		codonDegreeMap.put(ARGININE, "204");
		codonDegreeMap.put(GLYCINE, "004");			
	}
	
	public void run(TaskMonitor monitor) {
		
		if (network == null){
			System.out.println("There is no network.");
			return;
		}
		
		// Field to put into Cytoscape node table after analysis performed.	
		if(nodeTable.getColumn("PAI")!= null){
			nodeTable.deleteColumn("PAI");	
			nodeTable.createColumn("PAI", String.class, false);
		}
		else {
			nodeTable.createColumn("PAI", String.class, false);
		}
		
		if(nodeTable.getColumn("PAI Power")!= null){
			nodeTable.deleteColumn("PAI Power");	
			nodeTable.createColumn("PAI Power", Integer.class, false);
		}
		else {
			nodeTable.createColumn("PAI Power", Integer.class, false);
		}	
		
		if(nodeTable.getColumn("Node Degree")!= null){
			nodeTable.deleteColumn("Node Degree");	
			nodeTable.createColumn("Node Degree", Integer.class, false);
		}
		else {
			nodeTable.createColumn("Node Degree", Integer.class, false);
		}	
		
		// Field to put DI data into Cytoscape node table after analysis performed.	
		if (this.DImark){
			if(nodeTable.getColumn("DI Average")!= null){
				nodeTable.deleteColumn("DI Average");	
				nodeTable.createColumn("DI Average", Double.class, false);
			}
			else {
				nodeTable.createColumn("DI Average", Double.class, false);
			}
			
			if(nodeTable.getColumn("DI Variance")!= null){
				nodeTable.deleteColumn("DI Variance");	
				nodeTable.createColumn("DI Variance", Double.class, false);
			}
			else {
				nodeTable.createColumn("DI Variance", Double.class, false);
			}	
		}
			
 		CyColumn mysuidcolumn = nodeTable.getColumn("SUID");
 		List<Long> suidstorage;		
 		suidstorage = mysuidcolumn.getValues(Long.class);
 			
 		this.allAges = new String[nodeTable.getRowCount()];		
 		this.allAgesPower = new int[nodeTable.getRowCount()];
 		this.DISumm = new Double[nodeTable.getRowCount()];	
 		this.DISummx2 = new Double[nodeTable.getRowCount()];	
 		this.KsEmpty = new Integer[nodeTable.getRowCount()];	 		
 		
		for (int i=0; i < nodeTable.getRowCount(); i++){						
		    CyRow nodeRow = nodeTable.getRow(suidstorage.get(i)); 
		    nodeRow.set("Node Degree", 0);
		    
			// Check if the network is BioPax
			if(nodeTable.getColumn("NCBI GENE") != null){
				// Network is BioPax. We will ignore everything except genes
				if (nodeRow.get("NCBI GENE", String.class) == null){
		    		allAges[i] = "It's a path";
		    		allAgesPower[i] = 0;
		    		DISumm[i] = 0d;
		    		DISummx2[i] = 0d;
		    		KsEmpty[i] = 0;
					continue;
				}
			}
		    
		    String namedata = nodeRow.get("name", String.class);

	    	if (namedata.contains("path:")){
	    		allAges[i] = "It's a path";
	    		allAgesPower[i] = 0;
	    		DISumm[i] = 0d;
	    		DISummx2[i] = 0d;
	    		KsEmpty[i] = 0;
	    		continue;
	    	}		    	    	    	
	    	if( (namedata.contains("cpd:") || (namedata.contains("gl:")) || (namedata.contains("dr:")) )){		        
	    		allAges[i] = "It's a compound";
	    		allAgesPower[i] = 0;
	    		DISumm[i] = 0d;
	    		DISummx2[i] = 0d;
	    		KsEmpty[i] = 0;
	    		continue;
	    	}
	    	if(namedata.contains("ko:")){		        
	    		allAges[i] = "It's Kegg own orthologous group";
	    		allAgesPower[i] = 0;
	    		DISumm[i] = 0d;
	    		DISummx2[i] = 0d;
	    		KsEmpty[i] = 0;
	    		continue;
	    	}
	    	if(namedata.contains(":ko")){		        
	    		allAges[i] = "No data";
	    		allAgesPower[i] = 0;
	    		DISumm[i] = 0d;
	    		DISummx2[i] = 0d;
	    		KsEmpty[i] = 0;
	    		continue;
	    	}
	    	
	    	allAgesPower[i] = 0;
	    	DISumm[i] = 0.d;
	 		DISummx2[i] = 0.d;
	 		KsEmpty[i] = 0;
		    List<String> v_namedata = new ArrayList<String>(); 
		    
		    // Here we separate all genes from one node
		    while (namedata.length() != 0){
		        String[] curname = namedata.split(" ", 2);     
		        v_namedata.add(curname[0]);
		        
		        if (curname.length == 1){
		        	break;
		        }
		        namedata = curname[1];
		    }
		    
		    // Loading the page with data about orthologs
		    for (int j=0; j < v_namedata.size(); j++){
   	     		   	     
	   	    	 String sURL = "http://www.kegg.jp/ssdb-bin/ssdb_best?org_gene=" + v_namedata.get(j);	   	    	 				              
			     String tempgomoname = v_namedata.get(j).replace(':', '_');
			     File file = new File(mybasedirectory + sep + "Input" + sep + "OrthologBase" + sep + tempgomoname + ".txt");
			  	 String curURL = "";
		    	 int aminoNumber = 0;			     
			     
		    	 // If the local base exists 
		    	 if ((inputmark) && (file.exists()) && (!updatemark)){
		    		 curURL = OrthoscapeHelpFunctions.completeFileReader(file);	
		    		 String[] curlines = curURL.split("\n", 2);	// These 2 rows made to evade animoacid length
		    	  	 curURL = curlines[1];						// The old parameter should be deleted in the next version
		    	 }
		    	 // If the local base doesn't exist
		    	 else{
		    		curURL = OrthoscapeHelpFunctions.loadUrl(sURL);		    		
		    		if (!curURL.contains("KEGG ID")){
			    		if (allAges[i] == null){
			    			allAges[i] = "No data";
			    		}
		    	        continue;
		    		}
		    		
			        String[] curlines = new String[2];     
			        curlines = OrthoscapeHelpFunctions.stringFounder(curURL, "KEGG ID");
			   	   					       	    		
		       	    Pattern aminoLength  = Pattern.compile("[(]{1}[0-9]+");
		       	    Matcher m_aminoLength =  aminoLength.matcher(curlines[0]);

		       	    if (m_aminoLength.find()){
		       	      	String allFounded = m_aminoLength.group();
		       	       	String digitsOnly = allFounded.substring(1, allFounded.length());
		       	       	aminoNumber = Integer.parseInt(digitsOnly);
		       	    }	
		       	    curlines = OrthoscapeHelpFunctions.stringFounder(curlines[1], "Entry");
		       	    curlines = curlines[1].split("\n", 2);
		       	    
		       	    curURL = curlines[1];
			       	
		       	    // If we want to create local base
					if (inputmark || updatemark){
						try {
							file.createNewFile();
						}catch (IOException e2){ System.out.println("Can't create the file " + file.toString()); }
						OrthoscapeHelpFunctions.doubleFilePrinting(file, Integer.toString(aminoNumber), curURL);
					}					 		       	
		    	}
		    	// Algorithmic part (the work with orthologs list)
		    	itishappened(curURL, i, v_namedata.get(j));			
		    }   
		}
					
       	// Node's degrees analysis 				
 		List<CyEdge> allEdges = network.getEdgeList();
 		int genesEdges = 0;
 		int genesItself = 0;
 		for (int i=0; i < allEdges.size(); i++){
 			Long first  = allEdges.get(i).getSource().getSUID();
 			Long second = allEdges.get(i).getTarget().getSUID();
 			
 			// Deleting everything except genes
 			CyRow nodeRowGene1 = nodeTable.getRow(first); 
 		    String genedata = nodeRowGene1.get("type", String.class);
 		    if (genedata.equals("gene")){
 		    	genesEdges++;
 		    }
 		    CyRow nodeRowGene2 = nodeTable.getRow(second); 
		    genedata = nodeRowGene2.get("type", String.class);
		    if (genedata.equals("gene")){
		    	genesEdges++;
		    }

 			CyRow nodeRow1 = nodeTable.getRow(first);
 			int temp1 = nodeRow1.get("Node Degree", Integer.class);
 			temp1++;
 			nodeRow1.set("Node Degree", temp1);
 			
 			CyRow nodeRow2 = nodeTable.getRow(second);
 			int temp2 = nodeRow2.get("Node Degree", Integer.class);
 			temp2++;
 			nodeRow2.set("Node Degree", temp2);			
 		}		
 		// Data output (to the network table)
		int totalOrthologs = 0;
		for (int i=0; i < nodeTable.getRowCount(); i++){
					
		 	CyRow nodeRow = nodeTable.getRow(suidstorage.get(i));
		 	
 		    String genedata = nodeRow.get("type", String.class);
 		    if (genedata.equals("gene")){
 		    	genesItself++;
 		    }
		 	String[] curlines;
	       	curlines = allAges[i].split(";");
	       	if (curlines.length == 1){
	       		if (curlines[curlines.length-1].equals("Cellular Organisms")){
	       			nodeRow.set("PAI", "00_Cellular Organisms");
	       		}
	       		else{
	       			if (curlines[curlines.length-1].equals("Archaea") || curlines[curlines.length-1].equals("Bacteria") || curlines[curlines.length-1].equals("Eukaryota") ||  curlines[curlines.length-1].equals("Viruses")){
	       				nodeRow.set("PAI", "01_" + curlines[curlines.length-1].trim());
	       			}
	       			else{
		       			nodeRow.set("PAI", curlines[curlines.length-1].trim());
		       		}
	       		}
	       	}	
	       	else{
		       	if (curlines.length < 10){
		       		nodeRow.set("PAI", "0" + curlines.length + "_" + curlines[curlines.length-1].trim());
		       	}
		       	else{
		       		nodeRow.set("PAI", curlines.length + "_" + curlines[curlines.length-1].trim());
		       	}
	       	}
	       	nodeRow.set("PAI Power", allAgesPower[i]);
	       	totalOrthologs += allAgesPower[i];       	
	       	if (this.DImark){
		       	if (allAgesPower[i] == 0){
		       		nodeRow.set("DI Average", 0.d);
		       		nodeRow.set("DI Variance", 0.d);
		       	}
		       	else{
		       		nodeRow.set("DI Average", DISumm[i]/(allAgesPower[i]-KsEmpty[i]));
		       		nodeRow.set("DI Variance", (DISummx2[i] - DISumm[i]*DISumm[i]/(allAgesPower[i]-KsEmpty[i]))/(allAgesPower[i]-KsEmpty[i]));
		       	}
	       	}
	    }
			
		// Data output (to the report directory)
		if (this.outputmark){
			printTaxonomy(totalOrthologs, genesItself, genesEdges);
		}
	}
	
	private void printTaxonomy(int totalOrthologs, int totalGenes, int totalEdges){
	          
		Map<String, Integer> myTaxonomy = new HashMap<String, Integer>();
		myTaxonomy.put("It's a path", 0);
		myTaxonomy.put("It's a compound", 0);
		myTaxonomy.put("It's Kegg own orthologous group", 0);
		myTaxonomy.put("00_Cellular Organisms", 0);
		myTaxonomy.put("No data", 0);
		
		String[] alltaxes = orgtax.split(";");
		for (int i=0; i<alltaxes.length; i++){
			if (i<9){
				alltaxes[i] = "0" + (i+1) + "_" + alltaxes[i].trim();
				myTaxonomy.put(alltaxes[i], 0);
			}
			else{
				alltaxes[i] = (i+1) + "_" + alltaxes[i].trim();
				myTaxonomy.put(alltaxes[i], 0);
			}		
		}
		Map<String, Integer> taxesWeight = new HashMap<String, Integer>();
		taxesWeight.put("00_Cellular Organisms", 0);
		for (int i=0; i<alltaxes.length; i++){
			taxesWeight.put(alltaxes[i].trim(), i+1);			
		}
		taxesWeight.put("It's a path", 0);
		taxesWeight.put("It's a compound", 0);
		taxesWeight.put("It's Kegg own orthologous group", 0);	
		taxesWeight.put("No data", 0);
 		CyColumn mysuidcolumn = nodeTable.getColumn("SUID");
 		List<Long> suidstorage;		
 		suidstorage = mysuidcolumn.getValues(Long.class);
 		
 		double weightedDI = 0;
 		// Here we count number of every taxon 
 		for (int agi=0; agi<suidstorage.size(); agi++){
 		    CyRow ageRow = nodeTable.getRow(suidstorage.get(agi)); 
   		    String agedata = ageRow.get("PAI", String.class);
 			if (myTaxonomy.containsKey(agedata)){
 				int curnum = myTaxonomy.get(agedata)+1;
 				myTaxonomy.replace(agedata, curnum);
 			}
 			weightedDI += taxesWeight.get(agedata) * ageRow.get("Node Degree", Integer.class);
 		}
 		// And here we delete the same ones and their influence
 		for (int i=0; i<suidstorage.size(); i++){
 		    CyRow nameRow = nodeTable.getRow(suidstorage.get(i)); 
   		    String namedata = nameRow.get("name", String.class);
 			int nodeexist=0;
 			for (int j=0; j<i; j++){
 			    CyRow allnameRow = nodeTable.getRow(suidstorage.get(j)); 
 		   		String allnamedata = allnameRow.get("name", String.class);
 				if (namedata.equals(allnamedata)){
 					nodeexist=1;
 					break;
 				}
 			}
 			if (nodeexist==1){
 	 		    CyRow ageRow = nodeTable.getRow(suidstorage.get(i)); 
 	   		    String agedata = ageRow.get("PAI", String.class);
 				int curnum = myTaxonomy.get(agedata)-1;
 				myTaxonomy.replace(agedata, curnum);
 				
 	 		    CyRow powerRow = nodeTable.getRow(suidstorage.get(i)); 
 	   		    Integer powerdata = powerRow.get("PAI Power", Integer.class);
 	   		    totalOrthologs -= powerdata;
 			}
 		}		
	    CyTable networksTable = network.getDefaultNetworkTable();
	    CyColumn netcolumn = networksTable.getColumn("title");
	 	List<String> netstorage = netcolumn.getValues(String.class);
	 	
	    CyColumn orgcolumn = networksTable.getColumn("org");
	 	List<String> orgstorage = orgcolumn.getValues(String.class);
	 	 	 		 		
	 	// Bad symbols
	 	netstorage.set(0, netstorage.get(0).replace("\\", "__"));
	 	netstorage.set(0, netstorage.get(0).replace("/", "__"));
	 	netstorage.set(0, netstorage.get(0).replace(":", "__"));
	 	netstorage.set(0, netstorage.get(0).replace("*", "__"));
	 	netstorage.set(0, netstorage.get(0).replace("?", "__"));
	 	netstorage.set(0, netstorage.get(0).replace("\"", "__"));
	 	netstorage.set(0, netstorage.get(0).replace(">", "__"));
	 	netstorage.set(0, netstorage.get(0).replace("<", "__"));
	 	netstorage.set(0, netstorage.get(0).replace("|", "__"));
	 	
	 	// reducing the number of symbols in network name to 35 (+ KEGGID if necessary) 
	 	if (netstorage.get(0).length() > 40){
	 		netstorage.set(0, netstorage.get(0).substring(0, 35) + "..etc");
	 	}
	 	
	 	int notApplicationedNetwork = 0;	
 	 	String netName = "";
 	 	if (networksTable.getColumn("BIOPAX_NETWORK") != null){
 	 		netName = netstorage.get(0);
 	 		notApplicationedNetwork = 1;
 	 	}
 	 	if (networksTable.getColumn("type") != null){
 	 		CyColumn typecolumn = networksTable.getColumn("type");
 	 	 	List<String> typestorage = typecolumn.getValues(String.class);
	 	 	if (typestorage.get(0).equals("genemania")){	
	 	 		netName = netstorage.get(0);
	 	 		notApplicationedNetwork = 1;
	 	 	}
 	 	}	
 	 	if (networksTable.getColumn("number") != null){
 		    CyColumn numbercolumn = networksTable.getColumn("number");
 	 	 	List<String> numberstorage = numbercolumn.getValues(String.class);
 	 	 	netName = netstorage.get(0) + "_KEGGID=" + numberstorage.get(0);
 	 	 	notApplicationedNetwork = 1;
 	 	}
 	 	if (notApplicationedNetwork == 0){
 	 		netName = netstorage.get(0);
 	 	}
 	 	
	    File orgDir = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + orgstorage.get(0) + sep);
    	if (!orgDir.isDirectory()){
    		orgDir.mkdir();
	   	}
	    File deseaseDir = new File(mybasedirectory + sep + "Output" + sep + "Pictures and reports" + sep + orgstorage.get(0) + sep + netName + sep);
	   
    	if (!deseaseDir.isDirectory()){
    		deseaseDir.mkdir();
	   	}
	 	
    	String stridentity = Double.toString(equality);
    	if (stridentity.length() == 3){
    		stridentity +="0";
    	}
    	
    	String strSW = Integer.toString(SWScore);
    	if (strSW.length() == 3){
    		strSW = "00" + strSW;
    	}
    	if (strSW.length() == 4){
    		strSW = "0" + strSW;
    	}
    	
	 	PrintStream sourceFile = null;
		try{
			sourceFile = new PrintStream(mybasedirectory + sep + "Output" + sep
			+ "Pictures and reports" + sep + orgstorage.get(0) + sep + netName + sep
			+ netName + "___source___identity=" + stridentity + "___SW-Score=" + strSW + ".txt");
		}catch (FileNotFoundException e4){System.out.println("Can't create a source file");}
		
		sourceFile.println(myTaxonomy.get("00_Cellular Organisms"));
		for (int t=0; t<=alltaxes.length-1; t++){
			sourceFile.println(myTaxonomy.get(alltaxes[t].trim()));
		}
		sourceFile.println(totalOrthologs);
		if (totalEdges == 0){
			sourceFile.println(0);
		}
		else{
			sourceFile.println(weightedDI/totalEdges);
		}		
		sourceFile.println(totalGenes);
		sourceFile.println(totalEdges);
		sourceFile.close();
		
		PrintStream tableFile = null;
		try{
			tableFile = new PrintStream(mybasedirectory + sep + "Output" + sep
			+ "Pictures and reports" + sep + orgstorage.get(0) + sep + netName + sep
			+ netName + "___table___identity=" + stridentity + "___SW-Score=" + strSW + ".txt");
		}catch (FileNotFoundException e4){System.out.println("Can't create a table file");}
		
		tableFile.print("       ");
		for (int it=0; it<geneRow.size(); it++){
			tableFile.print(geneRow.get(it));
			tableFile.print("   ");
		}
		tableFile.println();
		for (int oit=0; oit<orgColumn.size(); oit++){
			tableFile.print(orgColumn.get(oit));
			for (int spcounter=0; spcounter < 10 - orgColumn.get(oit).length(); spcounter++){
				tableFile.print(" ");
			}
			for (int it=0; it<geneorgTable.get(oit).size(); it++){
				tableFile.print(geneorgTable.get(oit).get(it));
				for (int spcounter=0; spcounter < geneRow.get(it).length(); spcounter++){
					tableFile.print(" ");
				}
				tableFile.print("  ");
			}
			for (int it=geneorgTable.get(oit).size(); it<geneRow.size(); it++){
				tableFile.print("-");
				for (int spcounter=0; spcounter < geneRow.get(it).length(); spcounter++){
					tableFile.print(" ");
				}
				tableFile.print("  ");
			}
			tableFile.println();
		}
		tableFile.close();
			
		// PNG bar chart about PAI
        DefaultCategoryDataset xyDataset = new DefaultCategoryDataset();
        String serie = netstorage.get(0);
        
        xyDataset.addValue(myTaxonomy.get("00_Cellular Organisms"), serie, "00_Cellular Organisms");
		for (int t=0; t<=alltaxes.length-1; t++){
			xyDataset.addValue(myTaxonomy.get(alltaxes[t].trim()), serie, alltaxes[t].trim());
		}
                      
        JFreeChart chart = ChartFactory
            .createBarChart(netstorage.get(0) + " (identity=" + stridentity +  ", SW-score=" + strSW + ")",
            "", "Nodes on level", xyDataset, PlotOrientation.VERTICAL, true, true, true);
            
        CategoryAxis domAxis = (CategoryAxis) chart.getCategoryPlot().getDomainAxis();
        domAxis.setCategoryLabelPositions(
                CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));//setStandardTickUnits(NumberAxis.createIntegerTickUnits());  
        chart.getCategoryPlot().setDomainAxis(domAxis);
        
        NumberAxis rangeAxis = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        chart.getCategoryPlot().setRangeAxis(rangeAxis);
        
        chart.removeLegend();
        
        BufferedImage image2 = chart.createBufferedImage(800, 450);
            	
    	       
        try{
			ImageIO.write(image2, "PNG", new File(deseaseDir + sep 
			+ netName + "___graphic___identity=" + stridentity + "___SW-Score=" + strSW + ".PNG"));
		}catch (IOException e3){System.out.println("Can't create a distribution image");}
        
        // Gene - DI table.
        if (this.DImark){
        	PrintStream outDI = null;
        	try{
        		outDI = new PrintStream(deseaseDir + sep 
        		+ netName + "___DI___identity=" + stridentity + "___SW-Score=" + strSW + ".txt");
        	}catch (FileNotFoundException e) {System.out.println("Can't create DI file");}
			
	    	Map<String, Double> unsortedDIdata = new HashMap<String, Double>(); 
	    	
	        for (int i=0; i<suidstorage.size(); i++){
	 		    CyRow nameRow = nodeTable.getRow(suidstorage.get(i)); 
	   		    String namedata = nameRow.get("name", String.class);
	   		    
	   		    if ((// BioPax
	   		    		(nodeTable.getColumn("NCBI GENE") != null) && (nameRow.get("NCBI GENE", String.class) == null)
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
	   		    	continue;
	   		    }
	   		    
	   		    Double DIdata = nameRow.get("DI Average", Double.class);
	   		    
	   		    unsortedDIdata.put(namedata, DIdata);
	 		}		
	           
		    List<Entry<String,Double>> sortedDIdata = new ArrayList<Entry<String,Double>>(unsortedDIdata.entrySet());
			Collections.sort(sortedDIdata, new Comparator<Entry<String,Double>>() {		
				public int compare(Entry<String,Double> e1, Entry<String,Double> e2) {
					return e2.getValue().compareTo(e1.getValue());
				}   		 
			});
	        	   
			for (int cou = 0; cou < sortedDIdata.size(); cou++){			 		 
				outDI.print(sortedDIdata.get(cou).getKey());
				outDI.print("\t");
				outDI.println(sortedDIdata.get(cou).getValue());	
			}	   
			outDI.close();
        }
		
        // Gene - PAI table.
        PrintStream outPAI = null;
		try{
			outPAI = new PrintStream(deseaseDir + sep
			+ netName + "___PAI___identity=" + stridentity + "___SW-Score=" + strSW + ".txt");
		}catch (FileNotFoundException e) {System.out.println("Can't create PAI file");}
		
    	Map<String, String> unsortedDIdata = new HashMap<String, String>(); 
    	
        for (int i=0; i<suidstorage.size(); i++){
 		    CyRow nameRow = nodeTable.getRow(suidstorage.get(i)); 
   		    String namedata = nameRow.get("name", String.class);
   		    
   		    if ((// BioPax
   		    		(nodeTable.getColumn("NCBI GENE") != null) && (nameRow.get("NCBI GENE", String.class) == null)
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
   		    	continue;
   		    }
   		    
   		    String PAIdata = nameRow.get("PAI", String.class);
   		    
   		    unsortedDIdata.put(namedata, PAIdata);
 		}		

	    List<Entry<String,String>> sortedDIdata = new ArrayList<Entry<String,String>>(unsortedDIdata.entrySet());
		Collections.sort(sortedDIdata, new Comparator<Entry<String,String>>() {		
			public int compare(Entry<String,String> e1, Entry<String,String> e2) {
				return e2.getValue().compareTo(e1.getValue());
			}   		 
		});
        	   
		for (int cou = 0; cou < sortedDIdata.size(); cou++){			 		 
			outPAI.print(sortedDIdata.get(cou).getKey());
			outPAI.print("\t");
			outPAI.println(sortedDIdata.get(cou).getValue());	
		}	   
		outPAI.close();
	}
		
	// Work with orthologs list and taxonomy analysis.
	void itishappened(String curURL, int rowCounter, String curName){

		// Some data about taxonomic rows
		PrintStream curTaxOut = null;
		if (outputmark){
			String parsedname = curName.replace(':', '_');
			try {  	
				curTaxOut = new PrintStream(mybasedirectory + sep + "Output" + sep + "Full ages data" + sep + parsedname + ".txt");
			}catch (IOException e2){ e2.printStackTrace(); } 
		}
			
		String[] curlines;
		String tempcurOrgName = curName.replace(':', '_');
		List<String> curGeneDomens = null;
				
		if (this.domensNumber != 0){
			// Domains loading   									    		    	
			curGeneDomens = new ArrayList<String>();
			
	    	String sURL = "http://rest.kegg.jp/get/" + curName;	 
		    File file = new File(mybasedirectory + sep + "Input" + sep + "Domains" + sep + tempcurOrgName + ".txt");	     
		    String curURLagain;
			
	    	String line = "";
	
	    	if ((inputmark) && (file.exists()) && (!updatemark)){
				try {
					BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
		    		while ((line = reader.readLine()) != null) {
		    			curGeneDomens.add(line);
					}
		  	    	reader.close();
				}catch (IOException e2){ System.out.println("Can't read the file " + file.toString());}
	  	    }
		    else{
		       	curURLagain = OrthoscapeHelpFunctions.loadUrl(sURL);	
		        curlines = OrthoscapeHelpFunctions.stringFounder(curURLagain, "Pfam");
	   	    	
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
				   	try{
		       			file.createNewFile();
		       		}catch (IOException e2){ System.out.println("Can't create the file " + file.toString()); }
				   	OrthoscapeHelpFunctions.listFilePrinting(file, curGeneDomens);
				}
	        }
		}	       	
    	// Organism loading (to get taxonomic row)	    									    	
    	Pattern orgname  = Pattern.compile("^[a-z]+");        
	    Matcher m_orgname = orgname.matcher(curName);

	    String gomoname = "";
	    if (m_orgname.find()){	 
	      	 gomoname = m_orgname.group();
	    }
	    String sURL = "http://www.kegg.jp/dbget-bin/www_bget?" + gomoname;	        
	    File file = new File(mybasedirectory + sep + "Input" + sep + "OrganismBase" + sep + gomoname + ".txt");
	     
	    String curURLagain = "";
	    String line = "";

    	if ((inputmark) && (file.exists()) && (!updatemark)){
    		curURLagain = OrthoscapeHelpFunctions.completeFileReader(file);
	    }
	    else{	    	
			curURLagain = OrthoscapeHelpFunctions.loadUrl(sURL);
	    	curlines = OrthoscapeHelpFunctions.stringFounder(curURLagain, "Lineage");
	    	curURLagain = curlines[1];
	    				
	       	String[] curlinesmore;
	       	curlinesmore = curURLagain.split(">", 4);
	       	curURLagain = curlinesmore[2];
       	
	       	String[] curlinesless;
	       	curlinesless = curURLagain.split("<", 2);
	       	curURLagain = curlinesless[0];
	       	
	       	curURLagain = curURLagain.trim();
	       	
	       	// If we want to create local base
	    	if (inputmark || updatemark){
	    		try{
	    			file.createNewFile();
	    		}catch (IOException e2){ System.out.println("Can't create the file " + file.toString()); }
	    		OrthoscapeHelpFunctions.singleFilePrinting(file, curURLagain);
	    		
	    		// Invisible symbol fixing.
	  	    	curURLagain = OrthoscapeHelpFunctions.completeFileReader(file);
	    	}
        }
    	// Taxons row data output 
       	if (outputmark){
       		curTaxOut.println(curURLagain);
       	}
       		
    	if (allAges[rowCounter] == null){
    		allAges[rowCounter] = curURLagain;
    		allAgesPower[rowCounter]++;
    		orgtax = curURLagain;
    	}
    	else{
    		// PAI analysis. Taxonomic rows comparison
    		String tempAge = allAges[rowCounter];
    		
    		String[] tempAgelines = tempAge.split(";");
    		String[] curURLlines = curURLagain.split(";");

    		int minsize = 0;
    		if (tempAgelines.length<curURLlines.length){
    			minsize = tempAgelines.length;
    		}
    		else{
    			minsize = curURLlines.length;
    		}
    		   		    		
    		int matchcounter;
    		for (matchcounter=0; matchcounter<minsize; matchcounter++){
    			if (!tempAgelines[matchcounter].equals(curURLlines[matchcounter])){
    				break;
    			}
    		}
    		    		
    		if (matchcounter == 0){
    			allAges[rowCounter] = "Cellular Organisms";
    		}
    		else{
    			String someAge = tempAge;
    			String resAge = "";
    			for (int ma=0; ma<matchcounter-1; ma++){
	    			String[] splitAges = someAge.split(";", 2);
	    			someAge = splitAges[1];
	    			resAge += splitAges[0] + ";";
    			}
    			String[] splitAges = someAge.split(";", 2);
    			resAge += splitAges[0];  			
    			allAges[rowCounter] = resAge;
    		}
    		allAgesPower[rowCounter]++;
    	}
	    // Orthologs loading
	    curlines = curURL.split("\n", 2);
	    String currentline = curlines[0];
	    curURL = curlines[1];
   	          	
   	    String strequality = "0.5";
	    String strscore = "0";
   
   	    Pattern regequality  = Pattern.compile("[\\s]{1}[0-9]{1}[\\.]{1}[0-9]{3}[\\s]{1}");
   	    Pattern regname		 = Pattern.compile("[a-z]{2,9}[:]{1}[^\"]+");
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
    	    strequality = m_regequality.group();// curlineparsed[47];								//m_regequality.group();
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
	    // Orthologs analysis. The cycle will be finished using break
   	    while (1>0){
   	    	
	        if (badTablemetka == 1){
   	        	break;
   	        }
	        	
   	      	String[] namelines = currentline.split(regSep, 5);
   	      	m_regname = regname.matcher(namelines[3]);
   	       	if (m_regname.find()){
   	       		nameline = m_regname.group();
   	       	}
   	      	else{
   	       		break;
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
   	    ArrayList<String> domensToAccept = null;
   	    List<ArrayList<String>> allOrtoDomens = null;
   	    if (this.domensNumber != 0){
	   	 	allOrtoDomens = new ArrayList<ArrayList<String>>();
	   	 	// Domains analysis
	   	    for (int counter=0; counter<currentgomologs.size(); counter++){
	   	    	// Organism's domains loading	    									    	
		    	ArrayList<String> curOrtoDomens = new ArrayList<String>();
		    	
		    	String tempcurOrtoName = currentgomologs.get(counter).replace(':', '_');
		    	sURL = "http://rest.kegg.jp/get/" + currentgomologs.get(counter);	        
			    file = new File(mybasedirectory + sep + "Input" + sep + "Domains" + sep + tempcurOrtoName + ".txt");
		    	line = "";
	
		    	if ((inputmark) && (file.exists()) && (!updatemark)){
					try{
						BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
		    			while ((line = reader.readLine()) != null) {
		    				curOrtoDomens.add(line);
				   		}
		  	       		reader.close();
					}catch (IOException e2){ System.out.println("Can't read the file " + file.toString());}
		       	}
		       	else{
		       		curURLagain = OrthoscapeHelpFunctions.loadUrl(sURL);
		       		curlines = OrthoscapeHelpFunctions.stringFounder(curURLagain, "Pfam");
		       		
		       		if (curlines[0].contains("Pfam")){		    
				    	curlines = curlines[0].split(": ");
				    	curlines = curlines[1].split(" ");
				    	
				    	for (int domNum=0; domNum<curlines.length; domNum++){
				    		curlines[domNum] = curlines[domNum].trim();
				    		curOrtoDomens.add(curlines[domNum]);
				    	}
				    }
			   	    
		       		// If we want to create local base
				   	if (inputmark || updatemark){
				   		try{
				   			file.createNewFile();
				   		}catch (IOException e2){ System.out.println("Can't create the file " + file.toString()); }
				   		OrthoscapeHelpFunctions.listFilePrinting(file, curOrtoDomens);	
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
		   	tempcurOrgName = curName.replace(':', '_');
	   		PrintStream outdomain = null;
			try {
				outdomain = new PrintStream(mybasedirectory + sep + "Output" + sep + "OrthologDomains" + sep + tempcurOrgName + ".txt");
			
				outdomain.println("Gene domains:");	    	
		   		for (int cou = 0; cou < curGeneDomens.size(); cou++){   			   		    	    	
		   			outdomain.println(curGeneDomens.get(cou));   	
		   		}
		   		outdomain.println();
		   		outdomain.println("Ortholog domains:");
			}catch (FileNotFoundException e) {System.out.println("Can't create domains output file");}
	   			   		   
		    List<Entry<String,Integer>> orthologDomensSorted = new ArrayList<Entry<String,Integer>>(orthologDomens.entrySet());
			Collections.sort(orthologDomensSorted, new Comparator<Entry<String,Integer>>() {		
				public int compare(Entry<String,Integer> e1, Entry<String,Integer> e2) {
					return e2.getValue().compareTo(e1.getValue());
				}   		 
			});
		   	
			int domSize = Math.min(this.domensNumber, curGeneDomens.size());
			int missedDomens = 0;
			domensToAccept = new ArrayList<String>();
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
	   
			outdomain.println("Number of analized orthologs: " + allOrtoDomens.size());	
			outdomain.close();
   	    }
   	       		   	    
		int missedgomologs = 0;
		int geneRowAddedmark = 0;
		for (int counter=0; counter<currentgomologs.size(); counter++){
    	
			if (this.domensNumber != 0){
	   	    	int domainNotFoundMetka = 0;
	   	    	for (int id = 0; id < domensToAccept.size(); id++){
	   	    			   	    		
	   	    		if (!allOrtoDomens.get(counter).contains(domensToAccept.get(id))){
	   	    			domainNotFoundMetka = 1;
	   	    			break;
	   	    		}
	   	    	}
	   	    	if (domainNotFoundMetka == 1){
	   	    		   	    	    
	   	    		missedgomologs++;
	   	    		continue;
	   	    		
	   	    	}
			}
					
	   	    orgname  = Pattern.compile("^[a-z]+");        
		    m_orgname = orgname.matcher(currentgomologs.get(counter));
		    
		    gomoname = "";
		    if (m_orgname.find()){	 
		      	 gomoname = m_orgname.group();
		    }
			sURL = "http://www.kegg.jp/dbget-bin/www_bget?" + gomoname;	
			
			// If we didn't analyze this gene before or we analyzing it on current iteration
			if ((!geneRow.contains(curName)) || (geneRowAddedmark == 1)){
				// genes-species table fields
				if (geneRowAddedmark == 0){
					geneRow.add(curName);
					geneRowAddedmark = 1;
				}
				if (orgColumn.contains(gomoname)){
					int rowNumber = orgColumn.indexOf(gomoname);
					int alreadyExistGenes = geneorgTable.get(rowNumber).size();
					for (int gcounter=0; gcounter<geneRow.size()-alreadyExistGenes-1; gcounter++){
						geneorgTable.get(rowNumber).add("-");
					}
					geneorgTable.get(rowNumber).add("+");
				}
				else{
					orgColumn.add(gomoname);
					ArrayList<String> newList = new ArrayList<String>();
					geneorgTable.add(newList);
					for (int gcounter=0; gcounter<geneRow.size()-1; gcounter++){
						geneorgTable.get(orgColumn.size()-1).add("-");
					}
					geneorgTable.get(orgColumn.size()-1).add("+");
				}
			}		        	        
		    file = new File(mybasedirectory + sep + "Input" + sep + "OrganismBase" + sep + gomoname + ".txt");		     
		    curURLagain = "";
	    	line = "";

	    	if ((inputmark) && (file.exists()) && (!updatemark)){
	    		curURLagain = OrthoscapeHelpFunctions.completeFileReader(file);
	       	}
	       	else{
	       		curURLagain = OrthoscapeHelpFunctions.loadUrl(sURL);
	       		try{		       		
		       		curlines = OrthoscapeHelpFunctions.stringFounder(curURLagain, "Lineage");
		       		curURLagain = curlines[1];  
			       	String[] curlinesmore;
			       	curlinesmore = curURLagain.split(">", 4);
			       	curURLagain = curlinesmore[2];
		       	
			       	String[] curlinesless;
			       	curlinesless = curURLagain.split("<", 2);
			       	curURLagain = curlinesless[0];	
	       		}catch (ArrayIndexOutOfBoundsException e){
			    	// If we are here we probably found an ortholog-virus. We should load another url in this case
	       			sURL = "http://www.kegg.jp/dbget-bin/www_bget?" + currentgomologs.get(counter);
	       			curURLagain = OrthoscapeHelpFunctions.loadUrl(sURL);
		       		curlines = OrthoscapeHelpFunctions.stringFounder(curURLagain, "Lineage");
	       			curURLagain = curlines[1];
			       	String[] curlinesmore;
			       	curlinesmore = curURLagain.split(">", 4);
			       	curURLagain = curlinesmore[2];
		       	
			       	String[] curlinesless;
			       	curlinesless = curURLagain.split("<", 2);
			       	curURLagain = curlinesless[0];	
			    }
		       	
		       	curURLagain = curURLagain.trim();
		       	
		       	// If we want to create local base
		    	if (inputmark || updatemark){
		    		try{
		    			file.createNewFile();
		    		}catch (IOException e2){ System.out.println("Can't create the file " + file.toString()); }
		    		OrthoscapeHelpFunctions.singleFilePrinting(file, curURLagain);
		    		
		    		// Invisible symbol fixing.
		  	    	curURLagain = OrthoscapeHelpFunctions.completeFileReader(file);
		    	}
	       	}    			    			       	
	       	if (outputmark){
	       		curTaxOut.println(curURLagain);
	       	}
	       	if ((allAges[rowCounter].equals("No data"))){
	    		allAges[rowCounter] = curURLagain;
	    		orgtax = curURLagain;
	    		allAgesPower[rowCounter]++;
	    	}
	    	else{    
	    		String tempAge = allAges[rowCounter];
	    		
	    		String[] tempAgelines = tempAge.split(";");
	    		String[] curURLlines = curURLagain.split(";"); 		
	    		
	    		int minsize = 0;
	    		if (tempAgelines.length<curURLlines.length){
	    			minsize = tempAgelines.length;
	    		}
	    		else{
	    			minsize = curURLlines.length;
	    		}
	    		int matchcounter;
	    		for (matchcounter=0; matchcounter<minsize; matchcounter++){
	    			if (!tempAgelines[matchcounter].equals(curURLlines[matchcounter])){
	    				break;
	    			}
	    		}
	    		if (matchcounter == 0){
	    			allAges[rowCounter] = "Cellular Organisms";
	    		}
	    		else{
	    			String someAge = tempAge;
	    			String resAge = "";
	    			for (int ma=0; ma<matchcounter-1; ma++){
		    			String[] splitAges = someAge.split(";", 2);
		    			someAge = splitAges[1];
		    			resAge += splitAges[0] + ";";
	    			}
	    			String[] splitAges = someAge.split(";", 2);
	    			resAge += splitAges[0];
	    			allAges[rowCounter] = resAge;
	    		}
    			    		//  DI analysis start	    		if (DImark && ( (tempAgelines.length - matchcounter) <=  taxonomyDistance)){	    				    			// Specific species mark	    				    			if (speciesmark){	    				if (selectedSpecies.contains(gomoname)){	    					dihappened(curName, currentgomologs.get(counter), rowCounter);	    				}
	    				// Right now specific species bad for DI works with PAI. So number of orthologs different for two procedures.
	    				// Right now it's wrong for DI Power. Without comment it will be wrong for PAI Power.    				
		    			//	else{
		    			//		allAgesPower[rowCounter]--;
		    			//	}	    			}	    			else{	    				dihappened(curName, currentgomologs.get(counter), rowCounter);	    			}				    }	    		
	    	}
		}
		allAgesPower[rowCounter] = allAgesPower[rowCounter] + currentgomologs.size() - missedgomologs;
		curTaxOut.close();         	
	}   

// DI Analysis
void dihappened(String curOrgName, String curHomoName, int rowCounter){

	// Trying to find data in local base
	String tempcurOrgName = curOrgName.replace(':', '_');
	String tempcurHomoName = curHomoName.replace(':', '_');
	
	File DIfile = new File(mybasedirectory + sep + "Input" + sep + "DIBase" + sep + tempcurOrgName + "___" + tempcurHomoName + ".txt");
	if ((inputmark) && (DIfile.exists()) && (!updatemark)){
		try{
			BufferedReader reader = new BufferedReader(new FileReader(DIfile.toString()));
			String temp = reader.readLine();
			DISumm[rowCounter] += Double.parseDouble(temp);
			temp = reader.readLine();
		    DISummx2[rowCounter] += Double.parseDouble(temp);
	   		reader.close();
	   		
	   		if ((DISumm[rowCounter] == 0) && (DISummx2[rowCounter] == 0)){
	   			KsEmpty[rowCounter] += 1; 
	   		}
		}catch (IOException e2){ System.out.println("Can't read the file " + DIfile.toString()); }
   		return;	
	}
	// Organism loading 		    									    				   
	String sURL = "http://rest.kegg.jp/get/" + curOrgName;	        
	File file = new File(mybasedirectory + sep + "Input" + sep + "GeneSequenceBase" + sep + tempcurOrgName + ".txt");
 
	String curURLagain;
	organismAminoSequence = "";
	organismNucleoSequence = "";
	String[] curlines;

	if ((inputmark) && (file.exists()) && (!updatemark)){
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
			organismAminoSequence = reader.readLine();
			organismNucleoSequence = reader.readLine();
	   		reader.close();
		}catch (IOException e2){ System.out.println("Can't read the file " + file.toString()); }
	}	else{
		curURLagain = OrthoscapeHelpFunctions.loadUrl(sURL);		curlines = OrthoscapeHelpFunctions.stringFounder(curURLagain, "AASEQ");
		curlines = curlines[1].split("NTSEQ ");
		
		curlines[0] = curlines[0].trim();
	    while (curlines[0].length() != 0){
	    	String[] seqLines = curlines[0].split("\n", 2);
	        if (seqLines.length == 1){
	        	seqLines[0] = seqLines[0].trim();
	        	organismAminoSequence += seqLines[0]; 
	           	break;
	        }
	        curlines[0] = seqLines[1];
	        seqLines[0] = seqLines[0].trim();
	       	organismAminoSequence += seqLines[0];      	        
	    }
	    curlines[1] = curlines[1].trim();
	    curlines = curlines[1].split("\n", 2);
	    while (curlines[1].length() != 0){
	    	String[] seqLines = curlines[1].split("\n", 2);
	   	    if (seqLines[0].contains("///")){	// And of file in KEGG Rest API
	            break;
	        }   	    
	        curlines[1] = seqLines[1];
	        seqLines[0] = seqLines[0].trim();
	        organismNucleoSequence += seqLines[0];	      	        
	    }
	   	organismNucleoSequence = organismNucleoSequence.toUpperCase();
	   	// If we want to create local base
		if (inputmark || updatemark){
			try{
				file.createNewFile();
			}catch (IOException e2){ System.out.println("Can't create the file " + file.toString()); }	
			OrthoscapeHelpFunctions.doubleFilePrinting(file, organismAminoSequence, organismNucleoSequence);
	    }	}

	// Ortholog loading		    									    	   		   
	sURL = "http://rest.kegg.jp/get/" + curHomoName;	        
	file = new File(mybasedirectory + sep + "Input" + sep + "OrthologSequenceBase" + sep+tempcurHomoName+".txt");
	 
	curURLagain = "";
	orthologAminoSequence = "";
	orthologNucleoSequence = "";
	if ((inputmark) && (file.exists()) && (!updatemark)){
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file.toString()));
			orthologAminoSequence = reader.readLine();
			orthologNucleoSequence = reader.readLine();
			reader.close();
		}catch (IOException e2){ System.out.println("Can't read the file " + file.toString()); }
	}
	else{		
		curURLagain = OrthoscapeHelpFunctions.loadUrl(sURL);
		curlines = OrthoscapeHelpFunctions.stringFounder(curURLagain, "AASEQ");
		curlines = curlines[1].split("NTSEQ ");
		
		curlines[0] = curlines[0].trim();
	    while (curlines[0].length() != 0){
	    	String[] seqLines = curlines[0].split("\n", 2);
	        if (seqLines.length == 1){
	        	seqLines[0] = seqLines[0].trim();
	        	orthologAminoSequence += seqLines[0]; 
	           	break;
	        }
	        curlines[0] = seqLines[1];
	        seqLines[0] = seqLines[0].trim();
	        orthologAminoSequence += seqLines[0];      	        
	    }
	    
	    curlines[1] = curlines[1].trim();
	    curlines = curlines[1].split("\n", 2);
	    while (curlines[1].length() != 0){
	    	String[] seqLines = curlines[1].split("\n", 2);
	   	    if (seqLines[0].contains("///")){	// And of file in KEGG Rest API
	            break;
	        }  
	        curlines[1] = seqLines[1];
	        seqLines[0] = seqLines[0].trim();
	        orthologNucleoSequence += seqLines[0];	      	        
	    }
	    			       	
		orthologNucleoSequence = orthologNucleoSequence.toUpperCase();
		// If we want to create local base
		if (inputmark || updatemark){
			try{
				file.createNewFile();
			}catch (IOException e2){ System.out.println("Can't create the file " + file.toString()); }			
			OrthoscapeHelpFunctions.doubleFilePrinting(file, orthologAminoSequence, orthologNucleoSequence);
		}
	}	
	double difpercent = (double)Math.abs(orthologAminoSequence.length()-organismAminoSequence.length())/Math.min(orthologAminoSequence.length(), organismAminoSequence.length());				
	// Sometimes KEGG have wrong data (like one nucleotide in the end missing). There is the place to fix it.	
	while ( (float)(organismNucleoSequence.length()/organismAminoSequence.length()) < 3){	
		organismNucleoSequence += "C";	
	}	
	while ( (float)(organismNucleoSequence.length()/organismAminoSequence.length()) > 3){	
		organismNucleoSequence = organismNucleoSequence.substring(0, organismNucleoSequence.length()-1);	
	}		
	// Sometimes KEGG have wrong data (like one nucleotide in the end missing). There is the place to fix it.	
	while ( (float)(orthologNucleoSequence.length()/orthologAminoSequence.length()) < 3){	
		orthologNucleoSequence += "C";	
	}	
		while ( (float)(orthologNucleoSequence.length()/orthologAminoSequence.length()) > 3){	
		orthologNucleoSequence = orthologNucleoSequence.substring(0, orthologNucleoSequence.length()-1);	
	}	
	// Needleman–Wunsch alignment  	
	if (!organismAminoSequence.equals(orthologAminoSequence)){   	  	
		int gap_open=-11,gap_extn=-1;			
		// There is log with everyone missed because of aminoacid sequence length
		if ((organismAminoSequence.length()>6999) || (orthologAminoSequence.length() > 6999)){
			
			if (bigGenemark){
				NeedlemanWunsch(organismAminoSequence, orthologAminoSequence, organismNucleoSequence, orthologNucleoSequence, gap_open, gap_extn);			
			}
			else{
				File dir = new File(mybasedirectory + sep + "BigGenesLog" + sep);
				if (!dir.isDirectory()){
					dir.mkdir();
				}

				PrintStream logStream = null;
				try {
					logStream = new PrintStream(mybasedirectory + sep + "BigGenesLog" + sep + new java.util.Date().toString().replace(':', '_') + ".txt");
				} catch (FileNotFoundException e) {System.out.println("Can't create BigGenesLog file");}
				
				CyTable networksTable = network.getDefaultNetworkTable();
			    CyColumn netcolumn = networksTable.getColumn("title");
			 	List<String> netstorage = netcolumn.getValues(String.class);			 		
			 	// Bad symbols
			 	netstorage.set(0, netstorage.get(0).replace("\\", "__"));
			 	netstorage.set(0, netstorage.get(0).replace("/", "__"));
			 	netstorage.set(0, netstorage.get(0).replace(":", "__"));
			 	netstorage.set(0, netstorage.get(0).replace("*", "__"));
			 	netstorage.set(0, netstorage.get(0).replace("?", "__"));
			 	netstorage.set(0, netstorage.get(0).replace("\"", "__"));
			 	netstorage.set(0, netstorage.get(0).replace(">", "__"));
			 	netstorage.set(0, netstorage.get(0).replace("<", "__"));
			 	netstorage.set(0, netstorage.get(0).replace("|", "__"));
			 	
			 	logStream.println("Big gene founded in :" + netstorage.get(0) + " network");
				logStream.println();
								
				logStream.println("Gene " + curOrgName + " contains " + organismAminoSequence.length() + " aminoacids");				
				logStream.println("Ortholog " + curHomoName + " contains " + orthologAminoSequence.length() + " aminoacids");					
				logStream.println("Analysis missed");
				logStream.close();
				
				KsEmpty[rowCounter] += 1;
				
				// Sleep to make separated logs
				try {
				    Thread.sleep(1000);                 // 1 sec
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
				return;
			}
		}
		else{
			NeedlemanWunsch(organismAminoSequence, orthologAminoSequence, organismNucleoSequence, orthologNucleoSequence, gap_open, gap_extn);	
		}
	}  
	organismNucleoSequence = organismNucleoSequence.replace("T", "U");	
	orthologNucleoSequence = orthologNucleoSequence.replace("T", "U");	  	
	// Kaks calculator to get DI
	if (organismNucleoSequence.equals(orthologNucleoSequence)){	
		this.ngKa = 0;	
	    this.ngKs = 0;
	    this.ngKaKs = 0;	
	    this.mlwlKa = 0;	
	    this.mlwlKs = 0;	
	    this.mlwlKaKs = 0;
	    this.lwlKa = 0;	
	    this.lwlKs = 0;	
	    this.lwlKaKs = 0;	  	
	}else{		kaKsCalcNG(organismNucleoSequence, orthologNucleoSequence);	
		kaKsCalcMLWL(organismNucleoSequence, orthologNucleoSequence);	
		kaKsCalcLWL(organismNucleoSequence, orthologNucleoSequence);	
	}
 	
	if (lwlKs > 0){	
		DISumm[rowCounter] += lwlKaKs;	
	    DISummx2[rowCounter] += lwlKaKs*lwlKaKs;
	}	
	else{	
		KsEmpty[rowCounter] += 1;          	
	}	
	
	if (inputmark || updatemark){
		myDIPrinting(difpercent, tempcurOrgName, tempcurHomoName);
		try{
			DIfile.createNewFile();
		}catch (IOException e2){ System.out.println("Can't create the file " + DIfile.toString()); }		
		if (lwlKs > 0){	
			OrthoscapeHelpFunctions.doubleFilePrinting(DIfile, Double.toString(lwlKaKs), Double.toString(lwlKaKs*lwlKaKs));
		}	
		else{	
			OrthoscapeHelpFunctions.doubleFilePrinting(DIfile, "0", "0");         	
		}	  		
    }
}

private void myDIPrinting(double difpercent, String orgName, String ortoName){
	try {    
		PrintStream outStream2 = new PrintStream(mybasedirectory + sep + "Output" + sep + "KaKs" + sep + orgName + "___" + ortoName + ".txt");	    	
		outStream2.println("Jukes-Cantor (JC) Method:");
		outStream2.println("Ka: " + this.ngKa);
		outStream2.println("Ks: " + this.ngKs);
		outStream2.println("Ka/Ks: " + this.ngKaKs);
		outStream2.println();
    	
		outStream2.println("Kimuras- two parameter (K2P) model:");
		outStream2.println("Ka: " + this.mlwlKa);
		outStream2.println("Ks: " + this.mlwlKs);
		outStream2.println("Ka/Ks: " + this.mlwlKaKs);
		outStream2.println();
    	
		outStream2.println("JC and K2P Models:");
		outStream2.println("Ka: " + this.lwlKa);
		outStream2.println("Ks: " + this.lwlKs);
		outStream2.println("Ka/Ks: " + this.lwlKaKs);
		outStream2.close();
    	
		PrintStream outStream3 = new PrintStream(mybasedirectory + sep + "Output" + sep + "AlignedSequences" + sep + orgName + "___" + ortoName + ".txt");
    	outStream3.println(organismNucleoSequence);
    	outStream3.println(orthologNucleoSequence);
    	outStream3.close();
	}catch (IOException e2){ e2.printStackTrace(); }
}


//// KaKsCalculator source code implemented
// http://kakscalculator.fumba.me/index.jsp
// https://github.com/fumba/kaks-calculator

///**
// * Calculation of Ka/Ks using both JK and K2P models.
// * 
// * Ks - JC
// * 
// * Ka = K2P
// * 
// * Correcting - K2P
// * 
// * @param cleanOriginalSequence
// * @param cleanMutatedSequence
// */

//
///**
//* Calculate KaKs using the Jukes-Cantor (JC) method.
//*/
private void kaKsCalcNG(String originalSequence, String mutatedSequence) {

	String nsxSequence = translateToNSXSequence(originalSequence);	
	String nsxCodonMatchSequence = getEvolutionCode(originalSequence, mutatedSequence);
	
	// Count the occurrence of N and S from the NSX code of the given
	// sequences
	double nSeq = 0, sSeq = 0, nEvo = 0, sEvo = 0;
	for (Character code : nsxSequence.toCharArray()) {
		if (code.equals(CHAR_S) || code.equals(CHAR_X)) {
			sSeq++;
		}
		if (code.equals(CHAR_N) || code.equals(CHAR_X)) {
			nSeq++;
		}
	}
	for (Character code : nsxCodonMatchSequence.toCharArray()) {
		if (code.equals(CHAR_S) || code.equals(CHAR_X)) {
			sEvo++;
		}
		if (code.equals(CHAR_N) || code.equals(CHAR_X)) {
			nEvo++;
		}
	}
	
	this.setNgKa(sEvo / sSeq);
	this.setNgKs(nEvo / nSeq);
	this.setNgKaKs(this.ngKa / this.ngKs);
}

private void kaKsCalcMLWL(String cleanOriginalSequence, String cleanMutatedSequence){

	final double piQvRation = this.calcPiQvRatio(cleanOriginalSequence, cleanMutatedSequence);	 
	Map<String, Object> pQValues = this.calculatePQValues(cleanOriginalSequence, cleanMutatedSequence);
	
	final double L0 = (Double) pQValues.get(L0_AVERAGE_COUNT);
	final double L2 = (Double) pQValues.get(L2_AVERAGE_COUNT);
	final double L4 = (Double) pQValues.get(L4_AVERAGE_COUNT);
	
	Map<String, Double> calculatedValues = this.calculateMeanVariance(CHAR_0, cleanOriginalSequence, cleanMutatedSequence, pQValues);
	final double K0 = calculatedValues.get(TOTAL_SUBSTITUTIONS);

	calculatedValues = this.calculateMeanVariance(CHAR_2, cleanOriginalSequence, cleanMutatedSequence, pQValues);
	final double A2 = calculatedValues.get(TRANSITIONAL_SUBSTITUTIONS);
	final double B2 = calculatedValues.get(TRANSVERSION_SUBSTITUTIONS);  	

	calculatedValues = this.calculateMeanVariance(CHAR_4, cleanOriginalSequence, cleanMutatedSequence, pQValues);
	final double K4 = calculatedValues.get(TOTAL_SUBSTITUTIONS);   	
	
	if (piQvRation >= 2) {
		this.setMlwlKa((L2 * B2 + L0 * K0)
				/ (((2 * L2) / ((piQvRation - 1) + 2)) + L0));
		this.setMlwlKs((L2 * A2 + L4 * K4)
				/ (((piQvRation - 1) * L2 / (piQvRation - 1) + 2) + L4));
	} else {
		this.setMlwlKa((L2 * B2 + L0 * K0) / (((2 * L2) / 3) + L0));
		this.setMlwlKs((L2 * A2 + L4 * K4) / ((L2 / 3) + L4));
	}
	this.setMlwlKaKs(this.mlwlKa / this.mlwlKs);	
}
//
///**
// * Calculates the transition / transversion mutation rate ratio.
// * 
// * @param cleanOriginalSequence
// * @param cleanMutatedSequence
// * @return
// */
private double calcPiQvRatio(String cleanOriginalSequence, String cleanMutatedSequence) {
	double countPi = 0; // transitions
	double countQv = 0; // transversions

	String changeType;
	for (int index = 0; index < cleanOriginalSequence.length(); index++) {
		if (index <= cleanMutatedSequence.length()) {
			changeType = this.computeTransversionVsTransitionChange(
					cleanOriginalSequence.charAt(index),
					cleanMutatedSequence.charAt(index));
			
			if (changeType == TRANSITION) {
				countPi++;
			}
			if (changeType == TRANSVERSION){
				countQv++;
			}
			
		}
	}
	return countPi / countQv;
}
//
///**
// * Calculates Ka/Ks ration using the Kimuras- two parameter (K2P) model.
// * 
// * @param cleanOriginalSequence
// * @param cleanMutatedSequence
// */
private void kaKsCalcLWL(String cleanOriginalSequence, String cleanMutatedSequence) {

	Map<String, Object> pQValues = this.calculatePQValues(cleanOriginalSequence, cleanMutatedSequence);
	final double L0 = (Double) pQValues.get(L0_AVERAGE_COUNT);
	final double L2 = (Double) pQValues.get(L2_AVERAGE_COUNT);
	final double L4 = (Double) pQValues.get(L4_AVERAGE_COUNT);

	Map<String, Double> calculatedValues = this.calculateMeanVariance(CHAR_0, cleanOriginalSequence, cleanMutatedSequence, pQValues);
	final double K0 = calculatedValues.get(TOTAL_SUBSTITUTIONS);
	final double VK0 = calculatedValues.get(TOTAL_SUBSTITUTIONS_ERROR_VARIANCE);

	calculatedValues = this.calculateMeanVariance(CHAR_2, cleanOriginalSequence, cleanMutatedSequence, pQValues);
	final double A2 = calculatedValues.get(TRANSITIONAL_SUBSTITUTIONS);
	final double VA2 = calculatedValues.get(TRANSITIONAL_SUBSTITUTIONS_ERROR_VARIANCE);
	final double B2 = calculatedValues.get(TRANSVERSION_SUBSTITUTIONS);
	final double VB2 = calculatedValues.get(TRANSVERSION_SUBSTITUTIONS_ERROR_VARIANCE);

	calculatedValues = this.calculateMeanVariance(CHAR_4, cleanOriginalSequence, cleanMutatedSequence, pQValues);
	final double K4 = calculatedValues.get(TOTAL_SUBSTITUTIONS);
	final double VK4 = calculatedValues.get(TOTAL_SUBSTITUTIONS_ERROR_VARIANCE);

	this.setLwlKs(3 * ((L2 * A2) + (L4 * K4)) 				/	 (L2 + (3 * L4)));
	this.setLwlVKs(9 * (L2 * L2 * VA2 + L4 * L4 * VK4)		/	 Math.pow((L2 + 3 * L4), 2));
	this.setLwlKa(3 * (L2 * B2 + L0 * K0) 					/	 (2 * L2 + 3 * L0));
	this.setLwlVKa(9 * (L2 * L2 * VB2 + L0 * L0 * VK0)		/	 Math.pow((2 * L2 + 3 * L0), 2));
	
	this.setLwlKaKs(this.lwlKa / this.lwlKs);
}
//
///**
// * Calculates mean and approximate error variance. Reference Li, Wu, and Luo
// * P. 152-153.
// * 
// * @param i
// * @param cleanOriginalSequence
// * @param cleanMutatedSequence
// * @param pQValues
// * @return
// */
private Map<String, Double> calculateMeanVariance(
		final Character degenerationType, String cleanOriginalSequence,
		String cleanMutatedSequence, Map<String, Object> pQValues) {
	
	final double L0 = (Double) pQValues.get(L0_AVERAGE_COUNT);
	final double L2 = (Double) pQValues.get(L2_AVERAGE_COUNT);
	final double L4 = (Double) pQValues.get(L4_AVERAGE_COUNT);
	final double[] pValues = (double[]) pQValues.get(P_VALUES);
	final double[] qValues = (double[]) pQValues.get(Q_VALUES);

	double L = 0;
	double P = 0;
	double Q = 0;

	double ai = 0;
	double bi = 0;
	double ci = 0;
	double di = 0;

	if (CHAR_0.equals(degenerationType)) {
		L = L0;
		P = pValues[0];
		Q = qValues[0];
	} else if (CHAR_2.equals(degenerationType)) {
		L = L2;
		P = pValues[1];
		Q = qValues[1];
	} else if (CHAR_4.equals(degenerationType)) {
		L = L4;
		P = pValues[2];
		Q = qValues[2];
	}

	ai = 1 / (1 - (2 * P) - Q);
	if (ai < 0) {
		ai = 0.1; // FIXME (deal with negative values)
	}
	bi = 1 / (1 - (2 * Q));
	if (bi < 0) {
		bi = 0.1; // FIXME (deal with negative values)
	}
	ci = (ai - bi) / 2;
	if (ci < 0) {
		ci = 0.1; // FIXME (deal with negative values)
	}
	di = bi + ci;

	// Mean of transitional substitutions per i-th site.
	final double Ai = 0.5 * Math.log(ai) - 0.25 * Math.log(bi); // Natural
																// log (e)

	// Mean if transversional substitutions per i-th site
	final double Bi = 0.5 * Math.log(bi);

	// Approx error variance (transitional substitutions)
	final double VAi = (((ai * ai) * P + (ci * ci)) - Math.pow((ai * P)
			+ (ci * Q), 2))
			/ L;

	// Approx error variance (transversion substitutions)
	final double VBi = (((bi * bi) * Q) * (1 - Q)) / L;

	// total number of substitutions per i-th site
	final double Ki = Ai + Bi;

	// Variance
	final double VKi = (((ai * ai) * P + (di * di) * Q) - Math.pow((ai * P)
			+ (ci * Q), 2))
			/ L;

	Map<String, Double> resultMap = new HashMap<String, Double>();
	resultMap.put(TRANSITIONAL_SUBSTITUTIONS, Ai);
	resultMap.put(TRANSITIONAL_SUBSTITUTIONS_ERROR_VARIANCE, VAi);
	resultMap.put(TRANSVERSION_SUBSTITUTIONS, Bi);
	resultMap.put(TRANSVERSION_SUBSTITUTIONS_ERROR_VARIANCE, VBi);
	resultMap.put(TOTAL_SUBSTITUTIONS, Ki);
	resultMap.put(TOTAL_SUBSTITUTIONS_ERROR_VARIANCE, VKi);
	return resultMap;
}
//
///**
// * Calculates the observed transition( Pi) and transversion (Qv)
// * differences.
// * 
// * @param cleanMutatedSequence
// * @param cleanOriginalSequence
// * @return
// */
private Map<String, Object> calculatePQValues(String cleanOriginalSequence,	String cleanMutatedSequence) {

	Map<String, Object> resultMap = new HashMap<String, Object>();
	double[] pValues = new double[3]; // [L0, L2, L4]
	double[] qValues = new double[3]; // Same as above

	String degeneracySequence = this.calculateDegeneracySequence(cleanOriginalSequence);
	double L0, L2, L4;

	Map<String, Double> calculatedAvg024 = this.calculateAverage024(cleanOriginalSequence, cleanMutatedSequence);
	L0 = calculatedAvg024.get(L0_AVERAGE_COUNT);
	L2 = calculatedAvg024.get(L2_AVERAGE_COUNT);
	L4 = calculatedAvg024.get(L4_AVERAGE_COUNT);

	char originalNucleotide;
	char mutatedNucleotide;
	String changeType;
	Character degeneracyCode;

	for (int index = 0; index < cleanOriginalSequence.length(); index++) {
		if (index <= cleanMutatedSequence.length()) {
			originalNucleotide = cleanOriginalSequence.charAt(index);
			mutatedNucleotide = cleanMutatedSequence.charAt(index);
			if (originalNucleotide != mutatedNucleotide) {

				changeType = this.computeTransversionVsTransitionChange(
						originalNucleotide, mutatedNucleotide);
				degeneracyCode = degeneracySequence.charAt(index);
				
				if (changeType == TRANSITION) {
					if (degeneracyCode.equals(CHAR_0)) {
						pValues[0] = 1 / L0;
					} else if (degeneracyCode.equals(CHAR_2)) {
						pValues[1] = 1 / L2;
					} else if (degeneracyCode.equals(CHAR_4)) {
						pValues[2] = 1 / L4;
					}
				}
				if (changeType == TRANSVERSION){
					if (degeneracyCode.equals(CHAR_0)) {
					qValues[0] = 1 / L0;
				} else if (degeneracyCode.equals(CHAR_2)) {
					qValues[1] = 1 / L2;
				} else if (degeneracyCode.equals(CHAR_4)) {
					qValues[2] = 1 / L4;
				}
				}
				
			}
		}
	}

	resultMap.put(L0_AVERAGE_COUNT, L0);
	resultMap.put(L2_AVERAGE_COUNT, L2);
	resultMap.put(L4_AVERAGE_COUNT, L4);
	resultMap.put(P_VALUES, pValues);
	resultMap.put(Q_VALUES, qValues);
	return resultMap;
}
//
///**
// * Test if the change is transversion or transition. KEY:
// * 
// * Pi = TRANSITION (A > G or viceversa | A > C or viceversa ) Qv =
// * TRANSVERSION ( other combinations )
// * 
// * @param originalNucleotide
// * @param mutatedNucleotide
// * @return
// */
private String computeTransversionVsTransitionChange(
		char originalNucleotide, char mutatedNucleotide) {

	String original = EMPTY + originalNucleotide;
	String mutated = EMPTY + mutatedNucleotide;
	
	if ((original.equals(ADENINE) && mutated.equals(GUANINE)) ||
		(original.equals(GUANINE) && mutated.equals(ADENINE))) {
		return TRANSITION;
	}
	if ((original.equals(URACIL)   && mutated.equals(CYTOSINE)) ||
		(original.equals(CYTOSINE) && mutated.equals(URACIL))) {
		return TRANSITION;
	}
	return TRANSVERSION;
}
//
///**
// * Calculate average 0,2,4 degeneracy values for 2 sequences.
// * 
// * @param cleanOriginalSequence
// * @param cleanMutatedSequence
// * @return
// */
private Map<String, Double> calculateAverage024(String cleanOriginalSequence, String cleanMutatedSequence) {

	Map<String, Double> resultMap = new HashMap<String, Double>();

	String originalSeqDegCode = this.calculateDegeneracySequence(cleanOriginalSequence);
	String mutatedSeqDegCode  = this.calculateDegeneracySequence(cleanMutatedSequence);

	double originalCount0, originalCount2, originalCount4;
	Map<String, Double> counted024 = this.count024(originalSeqDegCode);
	originalCount0 = counted024.get(COUNT_0);
	originalCount2 = counted024.get(COUNT_2);
	originalCount4 = counted024.get(COUNT_4);

	double mutatedCount0, mutatedCount2, mutatedCount4;
	counted024 = this.count024(mutatedSeqDegCode);
	mutatedCount0 = counted024.get(COUNT_0);
	mutatedCount2 = counted024.get(COUNT_2);
	mutatedCount4 = counted024.get(COUNT_4);

	resultMap.put(L0_AVERAGE_COUNT, (originalCount0 + mutatedCount0) / 2);
	resultMap.put(L2_AVERAGE_COUNT, (originalCount2 + mutatedCount2) / 2);
	resultMap.put(L4_AVERAGE_COUNT, (originalCount4 + mutatedCount4) / 2);

	return resultMap;
}
//
///**
// * Calculates the frequencies for the three different types of degeneracy
// * types (0,2,4)
// * 
// * @param originalSeqDegCode
// * @return
// */
private Map<String, Double> count024(String seqDegCode) {
	double count0 = 0;
	double count2 = 0;
	double count4 = 0;

	Map<String, Double> resultMap = new HashMap<String, Double>();
	for (Character code : seqDegCode.toCharArray()) {
		if (code.equals(CHAR_0)) {
			count0++;
		} else if (code.equals(CHAR_2)) {
			count2++;
		} else if (code.equals(CHAR_4)) {
			count4++;
		}
	}
	resultMap.put(COUNT_0, count0);
	resultMap.put(COUNT_2, count2);
	resultMap.put(COUNT_4, count4);
	return resultMap;
}
//
///**
// * Provides the complete sequence in degeneracy code.
// * 
// * @param cleanOriginalSequence
// * @return
// */
private String calculateDegeneracySequence(String cleanOriginalSequence) {
	StringBuffer buffer = new StringBuffer();

	String[] codonList = getCodonList(cleanOriginalSequence);
	for (String codon : codonList) {
		buffer.append(this.codonDegreeMap.get(this.getAmino(codon)));
	}
	return buffer.toString();
}
//
///**
// * Detects mutations in codons from 2 sequences.
// * 
// * @param originalSequence
// * @param mutatedSequence
// * @return
// */
private String getEvolutionCode(String originalSequence, String mutatedSequence) {

	StringBuffer buffer = new StringBuffer();
	String[] originalSeqCodons = this.getCodonList(originalSequence);
	String[] mutatedSeqCodons = this.getCodonList(mutatedSequence);

//	boolean match;
	int myOwnMatch = 0;
	
	for (int index = 0; index < originalSeqCodons.length; index++) {
		if (index <= mutatedSeqCodons.length) {			

			Pattern nucleotides  = Pattern.compile("[^ACTGU]"); 	    
			Matcher nucleomatcher1;
			Matcher nucleomatcher2;
			
			nucleomatcher1  = nucleotides.matcher(originalSeqCodons[index]);
			nucleomatcher2  = nucleotides.matcher(mutatedSeqCodons[index]);
	        
			if (nucleomatcher1.find() || nucleomatcher2.find()){
				continue;
			}

			myOwnMatch = (this.getAmino(originalSeqCodons[index])).compareTo(this.getAmino(mutatedSeqCodons[index]));
			if (myOwnMatch == 0) {
				buffer.append(SYNONYMOUS);
			} else {
				buffer.append(NON_SYNONYMOUS);
			}
		}
	}
	return buffer.toString();
}
//
///**
// * Provides the complete NSX code for a given complete sequence. Used to
// * Analyze both evolved and original sequences.
// * 
// * @param originalSequence
// * @return
// */
private String translateToNSXSequence(String sequence) {
	StringBuffer buffer = new StringBuffer();
	String[] codonList = getCodonList(sequence);
	for (String codon : codonList) {
		buffer.append(this.getNSXForCodon(codon));
	}	
	return buffer.toString();
}
//
///**
// * Returns the Synonymity state of a given codon.
// * 
// * @param codon
// * @return
// */
private Object getNSXForCodon(String codon) {
	return nsxMap.get(this.getAmino(codon));
}
//
///**
// * Gets the amino acid coded by the 3-char codons.
// * 
// * @param codon
// * @return
// */
private String getAmino(String codon) {
	return aminoMap.get(codon);
}
//
///**
// * Extracts codons from a sequence
// * 
// * @param sequence
// * @return
// */
private String[] getCodonList(String sequence) {
	int codonNumber = sequence.length()/3;
	String[] codonMass = new String[codonNumber];
	for (int i=0; i< codonNumber; i++){
		codonMass[i] = sequence.substring(3*i, 3*(i+1));	
	}
	return codonMass;
}

public double getNgKa() {
	return ngKa;
}

public void setNgKa(double ngKa) {
	this.ngKa = ngKa;
}

public double getNgKs() {
	return ngKs;
}

public void setNgKs(double ngKs) {
	this.ngKs = ngKs;
}

public double getNgKaKs() {
	return ngKaKs;
}

public void setNgKaKs(double ngKaKs) {
	this.ngKaKs = ngKaKs;
}

public double getLwlKa() {
	return lwlKa;
}

public void setLwlKa(double lwlKa) {
	this.lwlKa = lwlKa;
}

public double getLwlKs() {
	return lwlKs;
}

public void setLwlKs(double lwlKs) {
	this.lwlKs = lwlKs;
}

public double getLwlKaKs() {
	return lwlKaKs;
}

public void setLwlKaKs(double lwlKaKs) {
	this.lwlKaKs = lwlKaKs;
}

public double getLwlVKa() {
	return lwlVKa;
}

public void setLwlVKa(double lwlVKa) {
	this.lwlVKa = lwlVKa;
}

public double getLwlVKs() {
	return lwlVKs;
}

public void setLwlVKs(double lwlVKs) {
	this.lwlVKs = lwlVKs;
}

public double getMlwlKa() {
	return mlwlKa;
}

public void setMlwlKa(double mlwlKa) {
	this.mlwlKa = mlwlKa;
}

public double getMlwlKs() {
	return mlwlKs;
}

public void setMlwlKs(double mlwlKs) {
	this.mlwlKs = mlwlKs;
}

public double getMlwlKaKs() {
	return mlwlKaKs;
}

public void setMlwlKaKs(double mlwlKaKs) {
	this.mlwlKaKs = mlwlKaKs;
}

// NW alignment
// http://zhanglab.ccmb.med.umich.edu/NW-align/
////start new alignment
	public void  NeedlemanWunsch(String f1,String f2, String nucf1, String nucf2, int gap_open,int gap_extn){
		
		int[][] imut = new int[24][24];                      
		Blosum62Matrix(imut);                              // Read Blosum scoring matrix and store it in the imut variable.
		String seqW = "*ARNDCQEGHILKMFPSTWYVBZX";	       // Amino acide order in the BLAST's scoring matrix (e.g.,Blosum62). 
		f1 = "*" + f1;                                     // Add a '*' character in the head of a sequence and this can make java code much more consistent with orginal fortran code.   
		f2 = "*" + f2;                                     // Use 1 to represent the first position of the sequence in the original fortran code,and 1 stand for the second position in java code. Here, add a '*' character in the head of a sequence could make 1 standard for the first postion of thse sequence in java code.     
		
		nucf1 = "***" + nucf1;                             // Add a '*' character in the head of a sequence and this can make java code much more consistent with orginal fortran code.   
		nucf2 = "***" + nucf2;                             // Use 1 to represent the first position of the sequence in the original fortran code,and 1 stand for the second position in java code. Here, add a '*' character in the head of a sequence could make 1 standard for the first postion of thse sequence in java code.     
		
		int[] seq1 = new int[f1.length()];                 
		int[] seq2 = new int[f2.length()];		           // seq1 and seq2 are arrays that store the amino acid order numbers of sequence1 and sequence2.

		String[] nucseq1 = new String[3*f1.length()];                 
		String[] nucseq2 = new String[3*f2.length()];		
		
		int i,j;		                                   // For example, 1 stand for A, 2 represent R and etc.
		for(i=1;i<f1.length();i++)
		{
			for(j=1;j<seqW.length();j++)
			{
				if(f1.charAt(i)==seqW.charAt(j))
				{
					seq1[i]=j;
					nucseq1[i] = nucf1.substring(3*i, 3*i+3);
				}
			}
		}		
		
		for(i=1;i<f2.length();i++)
		{
			for(j=1;j<seqW.length();j++)
			{
				if(f2.charAt(i)==seqW.charAt(j))
				{
					seq2[i]=j;				
					nucseq2[i] = nucf2.substring(3*i, 3*i+3);
				}
			}
		}
	 	 int[][] score = new int[f1.length()][f2.length()];		// score[i][j] stard for the alignment score that align ith position of the first sequence to the jth position of the second sequence.
		 for(i=1;i<f1.length();i++)
		 {
			  for(j=1;j<f2.length();j++)
			  {
				  score[i][j] = imut[seq1[i]][seq2[j]];
			  }
		 }	
		
		int[] j2i = new int[f2.length()+1];
		for(j=1;j<f2.length();j++)
		{
			j2i[j] = -1; // !all are not aligned
		}		
		
		int[][] val = new int[f1.length()+1][f2.length()+1];  // val[][] was assigned as a global variable, and the value could be printed in the final.
		int[][] idir = new int[f1.length()+1][f2.length()+1];
		int[][] preV = new int[f1.length()+1][f2.length()+1];
		int[][] preH = new int[f1.length()+1][f2.length()+1];
		int D,V,H;
		boolean standard = true;
		if(standard)    // If you want to use alternative implementation of Needleman-Wunsch dynamic program , you can assign "false" value to the "standard" variable.  
		{			
			////////////////////////////////////////////////////////////////////////////////
			//	     This is a standard Needleman-Wunsch dynamic program (by Y. Zhang 2005).
			//	     1. Count multiple-gap.
			//	     2. The gap penality W(k)=Go+Ge*k1+Go+Ge*k2 if gap open on both sequences			
			//	     idir[i][j]=1,2,3, from diagonal, horizontal, vertical
			//	     val[i][j] is the cumulative score of (i,j)
			////////////////////////////////////////////////////////////////////////////////			
			
			int[][] jpV = new int[f1.length()+1][f2.length()+1];
			int[][] jpH = new int[f1.length()+1][f2.length()+1];								
			val[0][0]=0;
			val[1][0] =gap_open;	
			for(i=2;i<f1.length();i++){
				val[i][0] = val[i-1][0]+gap_extn;
			}
			for(i=1;i<f1.length();i++)
			{
				
				preV[i][0] = val[i][0]; // not use preV at the beginning
		        idir[i][0] = 0;         // useless
		        jpV[i][0] = 1;          // useless
		        jpH[i][0] = i;          // useless
			}
			val[0][1]=gap_open;
			for(j=2;j<f2.length();j++){
				val[0][j]=val[0][j-1]+gap_extn;
			}
			for(j=1;j<f2.length();j++)
			{
		         preH[0][j] = val[0][j];
		         idir[0][j] = 0;
		         jpV[0][j] = j;
		         jpH[0][j] = 1;
			}
			// DP ------------------------------>
			for(j=1;j<f2.length();j++)
			{			
				for(i=1;i<f1.length();i++)
				{			
					// D=VAL(i-1,j-1)+SCORE(i,j)--------------->
					D = val[i-1][j-1] + score[i][j];	// from diagonal, val(i,j) is val(i-1,j-1)			
					
					//	H=H+gap_open ------->				
					jpH[i][j] = 1;				
					int val1 = val[i-1][j] + gap_open;  // gap_open from both D and V
					int val2 = preH[i-1][j] + gap_extn; // gap_extn from horizontal
					if(val1>val2)   // last step from D or V
					{
						H = val1;
					}
					else            // last step from H
					{
						H = val2;
						if(i > 1)
						{
							jpH[i][j] = jpH[i-1][j] + 1;  // record long-gap
						}
					}
					
					//	V=V+gap_open --------->					
					jpV[i][j] = 1;
					val1 = val[i][j-1] + gap_open;
					val2 = preV[i][j-1] + gap_extn;
					if(val1>val2)
					{
						V = val1;
					}
					else
					{
						V = val2;
						if(j > 1)
						{
							jpV[i][j] = jpV[i][j-1] + 1;   // record long-gap   
						}
					}
					
					preH[i][j] = H; // unaccepted H
					preV[i][j] = V;	// unaccepted V				
					if((D>H)&&(D>V))
					{
						idir[i][j]=1;
						val[i][j]=D;
					}
					else if(H > V)
					{   
						idir[i][j] = 2;
		                val[i][j] = H;
					}
		            else
		            {
		            	 idir[i][j] = 3;
			              val[i][j] = V;
		            }
				}			
			}			
			//  tracing back the pathway			
			i = f1.length()-1;
			j = f2.length()-1;		
			while((i>0)&&(j>0))  
			{			 
				 if(idir[i][j]==1)       // from diagonal
				 {
					j2i[j] = i;
		            i=i-1;
		            j=j-1;
				 }
				 else if(idir[i][j]==2)  // from horizonal
		         { 	        	 
					 int temp1 = jpH[i][j];                  //  
		        	 for(int me=1;me<=temp1;me++)            //  In the point view of a programer, 
		        	 {                                       //  you should not use the  "for(int me=1;me<=jpH[i][j];me++)".
		        		if(i>0)	        	                 //  If you use up sentence,the value of jpH[i][j] is changed when variable i changes. 
		                {                                    //  So the value of jpH[i][j] was assigned to the value temp1 and use the setence "for(int me=1;me<=temp1;me++)" here. 
		            	   i=i-1;                            // 
		                }	                                 //
		        	  }	        	                                
		         }
				 else
		         {	 
					int temp2 = jpV[i][j]; 
		            for(int me=1;me<=temp2;me++)             //  In the point view of a programer,
		            {                                        //  you should not use the  "for(int me=1;me<=jpV[i][j];me++)".
		               if(j>0)                               //  Because when variable i change, the jpV[i][j] employed here is also change. 
		               {                                     //  So the value of jpV[i][j] was assigned to the value temp2 and use the setence "for(int me=1;me<=temp2;me++)" here.
		                  j=j-1;                             //
		               }
		            }	           
		         }			 
			}	
		}
		else   
		{			
			/////////////////////////////////////////////////////////////////////////////////
			//		     This is an alternative implementation of Needleman-Wunsch dynamic program 
			//		     (by Y. Zhang 2005)
			//		     1. Count two-layer iteration and multiple-gaps
			//		     2. The gap penality W(k)=Go+Ge*k1+Ge*k2 if gap open on both sequences
			//		
			//		     idir[i][j]=1,2,3, from diagonal, horizontal, vertical
			//		     val[i][j] is the cumulative score of (i,j)
			////////////////////////////////////////////////////////////////////////////////			
			
			int[][] preD = new int[f1.length()+1][f2.length()+1];
			int[][] idirH = new int[f1.length()+1][f2.length()+1];
			int[][] idirV = new int[f1.length()+1][f2.length()+1];						
			val[0][0] = 0;		
			for(i=1;i<f1.length();i++)
			{
				val[i][0] = 0;
				idir[i][0] = 0;
				preD[i][0] = 0;
				preH[i][0] = -1000;
				preV[i][0] = -1000;
			}
			
			for(j=1;j<f2.length();j++)
			{
				val[0][j] = 0;
				idir[0][j] = 0;
				preD[0][j] = 0;
				preH[0][j] = -1000;
				preV[0][j] = -1000;
			}
			// DP ------------------------------>
			for(j=1;j<f2.length();j++)
			{			
				for(i=1;i<f1.length();i++)
				{			
					// preD=VAL(i-1,j-1)+SCORE(i,j)--------------->
					preD[i][j] = val[i-1][j-1] + score[i][j];
					// preH: pre-accepted H----------------------->
					D = preD[i-1][j] + gap_open;
					H = preH[i-1][j] + gap_extn;
					V = preV[i-1][j] + gap_extn;
					if((D>H)&&(D>V))
					{
						preH[i][j] = D;
						idirH[i-1][j] = 1;
					}
					else if(H>V)
					{
						preH[i][j] = H;
						idirH[i-1][j] = 2;
					}
					else
					{
						preH[i][j] = V;
						idirH[i-1][j] = 3;
					}
					
					// preV: pre-accepted V----------------------->
					D = preD[i][j-1] + gap_open;
					H = preH[i][j-1] + gap_extn;
					V = preV[i][j-1] + gap_extn;				
					if((D>H)&&(D>V))
					{
						preV[i][j] = D;
						idirV[i][j-1] = 1;
					}
					else if(H>V)
					{
						preV[i][j] = H;
						idirV[i][j-1] = 2;
					}
					else
					{
						preV[i][j] = V;
						idirV[i][j-1] = 3;
					}
					
					// decide idir(i,j)----------->
					if((preD[i][j]>preH[i][j])&&(preD[i][j]>preV[i][j]))
					{
						idir[i][j] = 1;
						val[i][j] = preD[i][j];
					}			
					else if(preH[i][j]>preV[i][j])
					{
						idir[i][j] = 2;
						val[i][j] = preH[i][j];
					}
					else
					{
						idir[i][j] = 3;
						val[i][j] = preV[i][j];
					}				
				}			
			}		
					
			//  tracing back the pathway			
			i = f1.length()-1;
			j = f2.length()-1;		
			while((i>0)&&(j>0))  
			{			 
				 if(idir[i][j]==1)
				 {
					 j2i[j] = i;
					 i = i - 1;
					 j = j - 1;
				 }
				 else if(idir[i][j]==2)
				 {
					 i = i - 1;
					 idir[i][j] = idirH[i][j];
				 }
				 else
				 {
					 j = j - 1;
					 idir[i][j] = idirV[i][j];
				 }
			}			
		}		
		
		// calculate sequence identity			
		int L_id=0;
	    int L_ali=0;
	    for(j=1;j<f2.length();j++)
	    {	    		
	    		if(j2i[j]>0)
	            {
	    			i=j2i[j];
	    			L_ali=L_ali+1;
		            if(seq1[i]==seq2[j])
		            {	            	
		            	L_id=L_id+1;
		            }
	            } 	        
	    }	   
	        
//	    double identity = L_id*1.0/(f2.length()-1);	    
//	    int fina_score = val[f1.length()-1][f2.length()-1];
//		System.out.println("Alignment score=" + fina_score);
//	    System.out.println("Length of sequence 1:" + (f1.length()-1));
//	    System.out.println("Length of sequence 2:" + (f2.length()-1));
//	    System.out.println("Aligned length      :" + L_ali);
//	    System.out.println("Identical length    :" + L_id);
	    
//	    DecimalFormat df = new DecimalFormat("0.000");      // Correct the identity to 3 decimal places. 
//	    System.out.print("Sequence identity=" + df.format(identity));
//	    System.out.println(" " + L_id  + "/" + (f2.length()-1));		    
//	    System.out.println();
	    // output aligned sequences	    
	    char[] sequenceA = new char[f1.length()+f2.length()];
	    char[] sequenceB = new char[f1.length()+f2.length()];
	    char[] sequenceM = new char[f1.length()+f2.length()];
	    
    	organismNucleoSequence = "";
    	orthologNucleoSequence = "";
	        
	    int k = 0;
	    i=1;
	    j=1;	    
	    while(true)
	    {	    	
	    	if((i>(f1.length()-1))&&(j>(f2.length()-1)))
	    		break;	    	
	    	if((i>(f1.length()-1))&&(j<(f2.length()-1)))     // unaligned C on 1
		    {
		    	k = k + 1;
		    	sequenceA[k] = '-';
		    	sequenceB[k] = seqW.charAt(seq2[j]);
		    	sequenceM[k] = ' ';
		    			    	
		    	j = j + 1;
		    }	    	
	    	else if((i<(f1.length()-1))&&(j>(f2.length()-1))) // unaligned C on 2
	        {	        	
	        	k = k + 1;
	        	sequenceA[k] = seqW.charAt(seq1[i]);
	        	sequenceB[k] = '-';
	        	sequenceM[k] = ' ';
	        	i = i + 1;
	        }	        
	    	else if(i==j2i[j]) // if align
	        {
	        	k = k + 1;
	        	sequenceA[k] = seqW.charAt(seq1[i]);
	        	sequenceB[k] = seqW.charAt(seq2[j]);
	        	organismNucleoSequence += nucseq1[i];
	        	orthologNucleoSequence += nucseq2[j];
	        	
	        	if(seq1[i]==seq2[j])  // identical
	        	{
	        		sequenceM[k] = ':';
	        	}
	        	else
	        	{
	        		sequenceM[k] = ' ';
	        	}
	        	i = i + 1;
	        	j = j + 1;
	        }	        
	        else if(j2i[j]<0)   // gap on 1
	        {
	        	k = k + 1;
	        	sequenceA[k] = '-';
	        	sequenceB[k] = seqW.charAt(seq2[j]);
	        	sequenceM[k] = ' ';
	        	j = j + 1;
	        }	        
	        else if(j2i[j] >= 0)  // gap on 2
	        {
	        	k = k + 1;
	        	sequenceA[k] = seqW.charAt(seq1[i]);
	        	sequenceB[k] = '-';
	        	sequenceM[k] = ' ';
	        	i = i + 1;
	        }	        
	    } 
	    
	    organismAminoSequence = "";
		orthologAminoSequence = "";  
		
	    for(i=1;i<=k;i++){
			if ((sequenceA[i] == '-') || (sequenceB[i] == '-')){
				continue;
			}
			organismAminoSequence += sequenceA[i];
			orthologAminoSequence += sequenceB[i];  
	    }	      
}
	
	public static void Blosum62Matrix(int[][] imut)     // Folowing from BLOSUM62 used in BLAST.
	{		                                            // This was directly copy from original fortran code.
		imut[1][1]=4;                                   // b,z,x are additional
		imut[1][2]=-1;
		imut[1][3]=-2;
		imut[1][4]=-2;
		imut[1][5]=0;
		imut[1][6]=-1;
		imut[1][7]=-1;
		imut[1][8]=0;
		imut[1][9]=-2;
		imut[1][10]=-1;
		imut[1][11]=-1;
		imut[1][12]=-1;
		imut[1][13]=-1;
		imut[1][14]=-2;
		imut[1][15]=-1;
		imut[1][16]=1;
		imut[1][17]=0;
		imut[1][18]=-3;
		imut[1][19]=-2;
		imut[1][20]=0;
		imut[1][21]=-2;
		imut[1][22]=-1;
		imut[1][23]=0;
		imut[2][1]=-1;
		imut[2][2]=5;
		imut[2][3]=0;
		imut[2][4]=-2;
		imut[2][5]=-3;
		imut[2][6]=1;
		imut[2][7]=0;
		imut[2][8]=-2;
		imut[2][9]=0;
		imut[2][10]=-3;
		imut[2][11]=-2;
		imut[2][12]=2;
		imut[2][13]=-1;
		imut[2][14]=-3;
		imut[2][15]=-2;
		imut[2][16]=-1;
		imut[2][17]=-1;
		imut[2][18]=-3;
		imut[2][19]=-2;
		imut[2][20]=-3;
		imut[2][21]=-1;
		imut[2][22]=0;
		imut[2][23]=-1;
		imut[3][1]=-2;
		imut[3][2]=0;
		imut[3][3]=6;
		imut[3][4]=1;
		imut[3][5]=-3;
		imut[3][6]=0;
		imut[3][7]=0;
		imut[3][8]=0;
		imut[3][9]=1;
		imut[3][10]=-3;
		imut[3][11]=-3;
		imut[3][12]=0;
		imut[3][13]=-2;
		imut[3][14]=-3;
		imut[3][15]=-2;
		imut[3][16]=1;
		imut[3][17]=0;
		imut[3][18]=-4;
		imut[3][19]=-2;
		imut[3][20]=-3;
		imut[3][21]=3;
		imut[3][22]=0;
		imut[3][23]=-1;
		imut[4][1]=-2;
		imut[4][2]=-2;
		imut[4][3]=1;
		imut[4][4]=6;
		imut[4][5]=-3;
		imut[4][6]=0;
		imut[4][7]=2;
		imut[4][8]=-1;
		imut[4][9]=-1;
		imut[4][10]=-3;
		imut[4][11]=-4;
		imut[4][12]=-1;
		imut[4][13]=-3;
		imut[4][14]=-3;
		imut[4][15]=-1;
		imut[4][16]=0;
		imut[4][17]=-1;
		imut[4][18]=-4;
		imut[4][19]=-3;
		imut[4][20]=-3;
		imut[4][21]=4;
		imut[4][22]=1;
		imut[4][23]=-1;
		imut[5][1]=0;
		imut[5][2]=-3;
		imut[5][3]=-3;
		imut[5][4]=-3;
		imut[5][5]=9;
		imut[5][6]=-3;
		imut[5][7]=-4;
		imut[5][8]=-3;
		imut[5][9]=-3;
		imut[5][10]=-1;
		imut[5][11]=-1;
		imut[5][12]=-3;
		imut[5][13]=-1;
		imut[5][14]=-2;
		imut[5][15]=-3;
		imut[5][16]=-1;
		imut[5][17]=-1;
		imut[5][18]=-2;
		imut[5][19]=-2;
		imut[5][20]=-1;
		imut[5][21]=-3;
		imut[5][22]=-3;
		imut[5][23]=-2;
		imut[6][1]=-1;
		imut[6][2]=1;
		imut[6][3]=0;
		imut[6][4]=0;
		imut[6][5]=-3;
		imut[6][6]=5;
		imut[6][7]=2;
		imut[6][8]=-2;
		imut[6][9]=0;
		imut[6][10]=-3;
		imut[6][11]=-2;
		imut[6][12]=1;
		imut[6][13]=0;
		imut[6][14]=-3;
		imut[6][15]=-1;
		imut[6][16]=0;
		imut[6][17]=-1;
		imut[6][18]=-2;
		imut[6][19]=-1;
		imut[6][20]=-2;
		imut[6][21]=0;
		imut[6][22]=3;
		imut[6][23]=-1;
		imut[7][1]=-1;
		imut[7][2]=0;
		imut[7][3]=0;
		imut[7][4]=2;
		imut[7][5]=-4;
		imut[7][6]=2;
		imut[7][7]=5;
		imut[7][8]=-2;
		imut[7][9]=0;
		imut[7][10]=-3;
		imut[7][11]=-3;
		imut[7][12]=1;
		imut[7][13]=-2;
		imut[7][14]=-3;
		imut[7][15]=-1;
		imut[7][16]=0;
		imut[7][17]=-1;
		imut[7][18]=-3;
		imut[7][19]=-2;
		imut[7][20]=-2;
		imut[7][21]=1;
		imut[7][22]=4;
		imut[7][23]=-1;
		imut[8][1]=0;
		imut[8][2]=-2;
		imut[8][3]=0;
		imut[8][4]=-1;
		imut[8][5]=-3;
		imut[8][6]=-2;
		imut[8][7]=-2;
		imut[8][8]=6;
		imut[8][9]=-2;
		imut[8][10]=-4;
		imut[8][11]=-4;
		imut[8][12]=-2;
		imut[8][13]=-3;
		imut[8][14]=-3;
		imut[8][15]=-2;
		imut[8][16]=0;
		imut[8][17]=-2;
		imut[8][18]=-2;
		imut[8][19]=-3;
		imut[8][20]=-3;
		imut[8][21]=-1;
		imut[8][22]=-2;
		imut[8][23]=-1;
		imut[9][1]=-2;
		imut[9][2]=0;
		imut[9][3]=1;
		imut[9][4]=-1;
		imut[9][5]=-3;
		imut[9][6]=0;
		imut[9][7]=0;
		imut[9][8]=-2;
		imut[9][9]=8;
		imut[9][10]=-3;
		imut[9][11]=-3;
		imut[9][12]=-1;
		imut[9][13]=-2;
		imut[9][14]=-1;
		imut[9][15]=-2;
		imut[9][16]=-1;
		imut[9][17]=-2;
		imut[9][18]=-2;
		imut[9][19]=2;
		imut[9][20]=-3;
		imut[9][21]=0;
		imut[9][22]=0;
		imut[9][23]=-1;
		imut[10][1]=-1;
		imut[10][2]=-3;
		imut[10][3]=-3;
		imut[10][4]=-3;
		imut[10][5]=-1;
		imut[10][6]=-3;
		imut[10][7]=-3;
		imut[10][8]=-4;
		imut[10][9]=-3;
		imut[10][10]=4;
		imut[10][11]=2;
		imut[10][12]=-3;
		imut[10][13]=1;
		imut[10][14]=0;
		imut[10][15]=-3;
		imut[10][16]=-2;
		imut[10][17]=-1;
		imut[10][18]=-3;
		imut[10][19]=-1;
		imut[10][20]=3;
		imut[10][21]=-3;
		imut[10][22]=-3;
		imut[10][23]=-1;
		imut[11][1]=-1;
		imut[11][2]=-2;
		imut[11][3]=-3;
		imut[11][4]=-4;
		imut[11][5]=-1;
		imut[11][6]=-2;
		imut[11][7]=-3;
		imut[11][8]=-4;
		imut[11][9]=-3;
		imut[11][10]=2;
		imut[11][11]=4;
		imut[11][12]=-2;
		imut[11][13]=2;
		imut[11][14]=0;
		imut[11][15]=-3;
		imut[11][16]=-2;
		imut[11][17]=-1;
		imut[11][18]=-2;
		imut[11][19]=-1;
		imut[11][20]=1;
		imut[11][21]=-4;
		imut[11][22]=-3;
		imut[11][23]=-1;
		imut[12][1]=-1;
		imut[12][2]=2;
		imut[12][3]=0;
		imut[12][4]=-1;
		imut[12][5]=-3;
		imut[12][6]=1;
		imut[12][7]=1;
		imut[12][8]=-2;
		imut[12][9]=-1;
		imut[12][10]=-3;
		imut[12][11]=-2;
		imut[12][12]=5;
		imut[12][13]=-1;
		imut[12][14]=-3;
		imut[12][15]=-1;
		imut[12][16]=0;
		imut[12][17]=-1;
		imut[12][18]=-3;
		imut[12][19]=-2;
		imut[12][20]=-2;
		imut[12][21]=0;
		imut[12][22]=1;
		imut[12][23]=-1;
		imut[13][1]=-1;
		imut[13][2]=-1;
		imut[13][3]=-2;
		imut[13][4]=-3;
		imut[13][5]=-1;
		imut[13][6]=0;
		imut[13][7]=-2;
		imut[13][8]=-3;
		imut[13][9]=-2;
		imut[13][10]=1;
		imut[13][11]=2;
		imut[13][12]=-1;
		imut[13][13]=5;
		imut[13][14]=0;
		imut[13][15]=-2;
		imut[13][16]=-1;
		imut[13][17]=-1;
		imut[13][18]=-1;
		imut[13][19]=-1;
		imut[13][20]=1;
		imut[13][21]=-3;
		imut[13][22]=-1;
		imut[13][23]=-1;
		imut[14][1]=-2;
		imut[14][2]=-3;
		imut[14][3]=-3;
		imut[14][4]=-3;
		imut[14][5]=-2;
		imut[14][6]=-3;
		imut[14][7]=-3;
		imut[14][8]=-3;
		imut[14][9]=-1;
		imut[14][10]=0;
		imut[14][11]=0;
		imut[14][12]=-3;
		imut[14][13]=0;
		imut[14][14]=6;
		imut[14][15]=-4;
		imut[14][16]=-2;
		imut[14][17]=-2;
		imut[14][18]=1;
		imut[14][19]=3;
		imut[14][20]=-1;
		imut[14][21]=-3;
		imut[14][22]=-3;
		imut[14][23]=-1;
		imut[15][1]=-1;
		imut[15][2]=-2;
		imut[15][3]=-2;
		imut[15][4]=-1;
		imut[15][5]=-3;
		imut[15][6]=-1;
		imut[15][7]=-1;
		imut[15][8]=-2;
		imut[15][9]=-2;
		imut[15][10]=-3;
		imut[15][11]=-3;
		imut[15][12]=-1;
		imut[15][13]=-2;
		imut[15][14]=-4;
		imut[15][15]=7;
		imut[15][16]=-1;
		imut[15][17]=-1;
		imut[15][18]=-4;
		imut[15][19]=-3;
		imut[15][20]=-2;
		imut[15][21]=-2;
		imut[15][22]=-1;
		imut[15][23]=-2;
		imut[16][1]=1;
		imut[16][2]=-1;
		imut[16][3]=1;
		imut[16][4]=0;
		imut[16][5]=-1;
		imut[16][6]=0;
		imut[16][7]=0;
		imut[16][8]=0;
		imut[16][9]=-1;
		imut[16][10]=-2;
		imut[16][11]=-2;
		imut[16][12]=0;
		imut[16][13]=-1;
		imut[16][14]=-2;
		imut[16][15]=-1;
		imut[16][16]=4;
		imut[16][17]=1;
		imut[16][18]=-3;
		imut[16][19]=-2;
		imut[16][20]=-2;
		imut[16][21]=0;
		imut[16][22]=0;
		imut[16][23]=0;
		imut[17][1]=0;
		imut[17][2]=-1;
		imut[17][3]=0;
		imut[17][4]=-1;
		imut[17][5]=-1;
		imut[17][6]=-1;
		imut[17][7]=-1;
		imut[17][8]=-2;
		imut[17][9]=-2;
		imut[17][10]=-1;
		imut[17][11]=-1;
		imut[17][12]=-1;
		imut[17][13]=-1;
		imut[17][14]=-2;
		imut[17][15]=-1;
		imut[17][16]=1;
		imut[17][17]=5;
		imut[17][18]=-2;
		imut[17][19]=-2;
		imut[17][20]=0;
		imut[17][21]=-1;
		imut[17][22]=-1;
		imut[17][23]=0;
		imut[18][1]=-3;
		imut[18][2]=-3;
		imut[18][3]=-4;
		imut[18][4]=-4;
		imut[18][5]=-2;
		imut[18][6]=-2;
		imut[18][7]=-3;
		imut[18][8]=-2;
		imut[18][9]=-2;
		imut[18][10]=-3;
		imut[18][11]=-2;
		imut[18][12]=-3;
		imut[18][13]=-1;
		imut[18][14]=1;
		imut[18][15]=-4;
		imut[18][16]=-3;
		imut[18][17]=-2;
		imut[18][18]=11;
		imut[18][19]=2;
		imut[18][20]=-3;
		imut[18][21]=-4;
		imut[18][22]=-3;
		imut[18][23]=-2;
		imut[19][1]=-2;
		imut[19][2]=-2;
		imut[19][3]=-2;
		imut[19][4]=-3;
		imut[19][5]=-2;
		imut[19][6]=-1;
		imut[19][7]=-2;
		imut[19][8]=-3;
		imut[19][9]=2;
		imut[19][10]=-1;
		imut[19][11]=-1;
		imut[19][12]=-2;
		imut[19][13]=-1;
		imut[19][14]=3;
		imut[19][15]=-3;
		imut[19][16]=-2;
		imut[19][17]=-2;
		imut[19][18]=2;
		imut[19][19]=7;
		imut[19][20]=-1;
		imut[19][21]=-3;
		imut[19][22]=-2;
		imut[19][23]=-1;
		imut[20][1]=0;
		imut[20][2]=-3;
		imut[20][3]=-3;
		imut[20][4]=-3;
		imut[20][5]=-1;
		imut[20][6]=-2;
		imut[20][7]=-2;
		imut[20][8]=-3;
		imut[20][9]=-3;
		imut[20][10]=3;
		imut[20][11]=1;
		imut[20][12]=-2;
		imut[20][13]=1;
		imut[20][14]=-1;
		imut[20][15]=-2;
		imut[20][16]=-2;
		imut[20][17]=0;
		imut[20][18]=-3;
		imut[20][19]=-1;
		imut[20][20]=4;
		imut[20][21]=-3;
		imut[20][22]=-2;
		imut[20][23]=-1;
		imut[21][1]=-2;
		imut[21][2]=-1;
		imut[21][3]=3;
		imut[21][4]=4;
		imut[21][5]=-3;
		imut[21][6]=0;
		imut[21][7]=1;
		imut[21][8]=-1;
		imut[21][9]=0;
		imut[21][10]=-3;
		imut[21][11]=-4;
		imut[21][12]=0;
		imut[21][13]=-3;
		imut[21][14]=-3;
		imut[21][15]=-2;
		imut[21][16]=0;
		imut[21][17]=-1;
		imut[21][18]=-4;
		imut[21][19]=-3;
		imut[21][20]=-3;
		imut[21][21]=4;
		imut[21][22]=1;
		imut[21][23]=-1;
		imut[22][1]=-1;
		imut[22][2]=0;
		imut[22][3]=0;
		imut[22][4]=1;
		imut[22][5]=-3;
		imut[22][6]=3;
		imut[22][7]=4;
		imut[22][8]=-2;
		imut[22][9]=0;
		imut[22][10]=-3;
		imut[22][11]=-3;
		imut[22][12]=1;
		imut[22][13]=-1;
		imut[22][14]=-3;
		imut[22][15]=-1;
		imut[22][16]=0;
		imut[22][17]=-1;
		imut[22][18]=-3;
		imut[22][19]=-2;
		imut[22][20]=-2;
		imut[22][21]=1;
		imut[22][22]=4;
		imut[22][23]=-1;
		imut[23][1]=0;
		imut[23][2]=-1;
		imut[23][3]=-1;
		imut[23][4]=-1;
		imut[23][5]=-2;
		imut[23][6]=-1;
		imut[23][7]=-1;
		imut[23][8]=-1;
		imut[23][9]=-1;
		imut[23][10]=-1;
		imut[23][11]=-1;
		imut[23][12]=-1;
		imut[23][13]=-1;
		imut[23][14]=-1;
		imut[23][15]=-2;
		imut[23][16]=0;
		imut[23][17]=0;
		imut[23][18]=-2;
		imut[23][19]=-1;
		imut[23][20]=-1;
		imut[23][21]=-1;
		imut[23][22]=-1;
		imut[23][23]=-1;
	}
}