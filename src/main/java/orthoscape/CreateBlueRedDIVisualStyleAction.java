package orthoscape;

import java.awt.Color;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.List;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

public class CreateBlueRedDIVisualStyleAction extends AbstractCyAction {

	private static final long serialVersionUID = 1L;
	private CyApplicationManager cyApplicationManagerServiceRef;
	private VisualStyleFactory visualStyleFactoryServiceRef;
	private VisualMappingManager vmmServiceRef;
	
	private VisualMappingFunctionFactory vmfFactoryC;
	private VisualMappingFunctionFactory vmfFactoryP;
	
	public CreateBlueRedDIVisualStyleAction(CyApplicationManager cyApplicationManagerServiceRef, VisualMappingManager vmmServiceRef, VisualStyleFactory visualStyleFactoryServiceRef, 
			VisualMappingFunctionFactory vmfFactoryC,VisualMappingFunctionFactory vmfFactoryD,VisualMappingFunctionFactory vmfFactoryP){
		super("DI Based");
		setPreferredMenu("Apps.Orthoscape.Coloring.Blue-Red Style");

		this.cyApplicationManagerServiceRef = cyApplicationManagerServiceRef;
		this.visualStyleFactoryServiceRef = visualStyleFactoryServiceRef;
		this.vmmServiceRef =  vmmServiceRef;
		this.vmfFactoryC	= vmfFactoryC;
		this.vmfFactoryP	= vmfFactoryP;
	}
	
	/**
	 *  DOCUMENT ME!
	 *
	 * @param e DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {

		// If the style already existed, remove it first
		Iterator<?> it = vmmServiceRef.getAllVisualStyles().iterator();
		while (it.hasNext()){
			VisualStyle curVS = (VisualStyle)it.next();
			if (curVS.getTitle().equalsIgnoreCase("KEGG Blue-Red DI Style"))
			{
				vmmServiceRef.removeVisualStyle(curVS);
				break;
			}
		}
		
		// Create a new Visual style
		VisualStyle vs= this.visualStyleFactoryServiceRef.createVisualStyle("KEGG Blue-Red DI Style");
		this.vmmServiceRef.addVisualStyle(vs);

		// Standard visual styles parameter's values
		PassthroughMapping<String, Paint> nodeLabelColorMapping =
				(PassthroughMapping<String, Paint>) this.vmfFactoryP.createVisualMappingFunction     ("Label",  String.class, BasicVisualLexicon.NODE_LABEL_COLOR);		
		PassthroughMapping<String, String> nodeLabelMapping     =
				(PassthroughMapping<String, String>) this.vmfFactoryP.createVisualMappingFunction    ("Label",  String.class, BasicVisualLexicon.NODE_LABEL);
		PassthroughMapping<String, NodeShape> nodeShapeMapping  =
				(PassthroughMapping<String, NodeShape>) this.vmfFactoryP.createVisualMappingFunction ("Shape",  String.class, BasicVisualLexicon.NODE_SHAPE);
		PassthroughMapping<String, Double> nodeHeightMapping    =
				(PassthroughMapping<String, Double>) this.vmfFactoryP.createVisualMappingFunction    ("Height", String.class, BasicVisualLexicon.NODE_HEIGHT);
		PassthroughMapping<String, Double> nodeWidthMapping     =
				(PassthroughMapping<String, Double>) this.vmfFactoryP.createVisualMappingFunction    ("Width",  String.class, BasicVisualLexicon.NODE_WIDTH);
        
		
		// Finding the max DI to distribute colors
		CyTable nodeTable = cyApplicationManagerServiceRef.getCurrentNetwork().getDefaultNodeTable();		
		CyColumn DIcolumn = nodeTable.getColumn("DI Average");
 		List<Double> DIstorage = DIcolumn.getValues(Double.class);
 		
 		double maxgElem = -1;
 		for (int i=0; i<DIstorage.size(); i++){
 			if (DIstorage.get(i) > maxgElem){
 				maxgElem = DIstorage.get(i);
 			}
 		}
 		
 		for (int i=0; i< DIstorage.size(); i++){
 			DIstorage.set(i, DIstorage.get(i)/maxgElem);
 		}

 		// Blue-red coloring
 	 	ContinuousMapping<Double, Paint> cMapping = (ContinuousMapping<Double, Paint>) this.vmfFactoryC.createVisualMappingFunction("DI Average", Double.class, BasicVisualLexicon.NODE_FILL_COLOR);				
 		
 	 	BoundaryRangeValues<Paint> brv = new BoundaryRangeValues<Paint>(Color.BLUE, Color.BLUE, Color.BLUE);
 	 	cMapping.addPoint(0d, brv);
 	 	BoundaryRangeValues<Paint> brv2 = new BoundaryRangeValues<Paint>(new Color(0.25f,0.35f,0.8f), new Color(0.25f,0.35f,0.8f), new Color(0.25f,0.35f,0.8f));
 	 	cMapping.addPoint((maxgElem/4), brv2);
		BoundaryRangeValues<Paint> brv3 = new BoundaryRangeValues<Paint>(new Color(0.8f,0.8f,0.8f), new Color(0.8f,0.8f,0.8f), new Color(0.8f,0.8f,0.8f));
		cMapping.addPoint((maxgElem/2), brv3);
		BoundaryRangeValues<Paint> brv4 = new BoundaryRangeValues<Paint>(new Color(0.8f,0.35f,0.25f), new Color(0.8f,0.35f,0.25f), new Color(0.8f,0.35f,0.25f));
		cMapping.addPoint((3*maxgElem/4), brv4);
		BoundaryRangeValues<Paint> brv5 = new BoundaryRangeValues<Paint>(Color.RED, Color.RED, Color.RED);
		cMapping.addPoint((maxgElem), brv5);
 				
		VisualPropertyDependency<?> dependency = null;
		for (VisualPropertyDependency<?> dep : vs.getAllVisualPropertyDependencies()){
            if (dep.getDisplayName().equalsIgnoreCase("Lock node width and height"))
                dependency = dep;
        }
        if (dependency != null && dependency.isDependencyEnabled()){
            dependency.setDependency(false);		
        }
        
        vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, 7);
        vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);
        vs.setDefaultValue(BasicVisualLexicon.NODE_HEIGHT, 20d);
        vs.setDefaultValue(BasicVisualLexicon.NODE_WIDTH, 60d);
        
        vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL, "Default node");
        
		vs.addVisualMappingFunction(nodeLabelColorMapping);
		vs.addVisualMappingFunction(nodeLabelMapping);	
		vs.addVisualMappingFunction(nodeShapeMapping);	
		vs.addVisualMappingFunction(nodeHeightMapping);	
		vs.addVisualMappingFunction(nodeWidthMapping);	
		
		vs.addVisualMappingFunction(cMapping);		
		vmmServiceRef.setCurrentVisualStyle(vs);
	}
}