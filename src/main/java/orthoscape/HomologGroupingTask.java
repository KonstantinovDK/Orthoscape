package orthoscape;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
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
	private CyGroupManager cyGroupManager;
	private CyTable nodeTable;
	
	public HomologGroupingTask(CyNetwork network, CyGroupFactory myfactory, CyGroupManager mymanager){
		this.network = network;
		this.cyGroupCreator = myfactory;	
		this.cyGroupManager = mymanager;
		this.nodeTable = network.getDefaultNodeTable();
	}
		
	public void run(TaskMonitor monitor) {
		
		try{
		PrintStream testfile = new PrintStream("D:\\2017OrthoscapeBases\\GroupingFix\\log.txt");
		testfile.println("here1");				
		
		if (network == null){
			System.out.println("There is no network.");
			return;
		}
		
		testfile.println("here2");
		
		if(nodeTable.getColumn("Homology Cluster") == null){
			
			JPanel errorpanel = new JPanel();
    		errorpanel.setLayout(new BoxLayout(errorpanel, BoxLayout.Y_AXIS));
    		errorpanel.add(new JLabel("You have to launch homology analyis first"));	
    		JOptionPane.showMessageDialog(null, errorpanel);
    		return;
		}
		
		testfile.println("here3");
		
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
 		testfile.println("gm " + groupmax);
 		testfile.println("here4");
 		
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
 		testfile.println("here5");
 		
 		for (int i=0; i<groupmax+2; i++){
 			allNames[i] = allNames[i].substring(0, allNames[i].length()-2);
 	//		testfile.println(allLists[i]);
 		}
 		
 		testfile.println("still correct");
 		// Fullfilling the groups		  		
 		for (int i=0; i<groupmax+2; i++){
 			allGroups[i].addNodes(allLists[i]);
 			testfile.println("i=" + i);
 			testfile.println("listSize=" + allLists.length);
 			testfile.println("groupSize=" + allGroups.length);
 			
 			testfile.println("list=");
 			testfile.println(allLists[i]);
 			
 			testfile.println("new test=");
 			testfile.println(allGroups[i].getNodeList());
 			
 //			for (int z=0; z<allLists.length; z++){
 //	 			testfile.println(allLists[z]);
 //	 		}
 			
 			testfile.println("group=");
 			testfile.println(allGroups[i]);
 //			for (int z=0; z<allGroups.length; z++){
 //	 			testfile.println(allGroups[z]);
 //	 		}
 			
 			testfile.println("all groyups = " + cyGroupManager.getGroupSet(network));
 			cyGroupManager.addGroup(allGroups[i]);//;.destroyGroup(cyGroupManager.getGroup(curList4.get(j), network));
 			allGroups[i].collapse(network);
 		}	
 		
 		List<CyNode> curList = new ArrayList<CyNode>();
 	 	curList = network.getNodeList();
 	 	testfile.println("curList size = " + curList.size());
 		for (int j=0; j< curList.size(); j++){
 			testfile.println( " j= " + j);
 			testfile.println(curList.get(j));
 			CyRow nodeRow = nodeTable.getRow(curList.get(j).getSUID());
	 		if (!nodeRow.isSet("Homology Cluster")){
	 	 		nodeRow.set("Homology Cluster", j-1); 
	 	 		
	 	 		if(nodeTable.getColumn("Type")!= null){
	 	 			nodeRow.set("Type", "group");
	 	 		}
	 	 		if(nodeTable.getColumn("Label")!= null){
	 	 			nodeRow.set("Label", "Group " + String.valueOf(j-1));
	 	 		}
	 	 		
	 	 		nodeRow.set("shared name", allNames[j]);
	 	 		nodeRow.set("name", allNames[j]);
	    	}    
 		}
 		testfile.println("here6");
 		testfile.close();
		}catch (IOException e2){ e2.printStackTrace(); }
	}
    
	public void cancel() {
		cancelled = true;
	}
}
