package orthoscape;
import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		CyApplicationManager cyApplicationManagerServiceRef = getService(bc,CyApplicationManager.class);
		CyGroupFactory globalcyGroupCreator = getService(bc, CyGroupFactory.class);
		CyGroupManager globalcyGroupManager = getService(bc, CyGroupManager.class);
		
		// GeneMania to KEGG data initialization
		GMtoKEGGConverterTaskFactory GMtoKEGGConverterTaskFactory = new GMtoKEGGConverterTaskFactory(cyApplicationManagerServiceRef);
		
		Properties GMtoKEGGConverterTaskFactoryProps = new Properties();
		GMtoKEGGConverterTaskFactoryProps.setProperty("preferredMenu","Apps.Orthoscape.1) Converting");
		GMtoKEGGConverterTaskFactoryProps.setProperty("title","Convert GeneMANIA network");
		registerService(bc,GMtoKEGGConverterTaskFactory,TaskFactory.class, GMtoKEGGConverterTaskFactoryProps);
		
		// BioPax to KEGG data initialization
		BPtoKEGGConverterTaskFactory BPtoKEGGConverterTaskFactory = new BPtoKEGGConverterTaskFactory(cyApplicationManagerServiceRef);
				
		Properties BPtoKEGGConverterTaskFactoryProps = new Properties();
		BPtoKEGGConverterTaskFactoryProps.setProperty("preferredMenu","Apps.Orthoscape.1) Converting");
		BPtoKEGGConverterTaskFactoryProps.setProperty("title","Convert CyPath2 network");
		registerService(bc,BPtoKEGGConverterTaskFactory,TaskFactory.class, BPtoKEGGConverterTaskFactoryProps);

		// Homology search data initialization
		HomologySearchTaskFactory homologySearchTaskFactory = new HomologySearchTaskFactory(cyApplicationManagerServiceRef);
		
		Properties homologySearchTaskFactoryProps = new Properties();
		homologySearchTaskFactoryProps.setProperty("preferredMenu","Apps.Orthoscape.2) Working");
		homologySearchTaskFactoryProps.setProperty("title","Homology analysis");
		registerService(bc,homologySearchTaskFactory,TaskFactory.class, homologySearchTaskFactoryProps);
		
		// PAI/DI search data initialization
		AgeSearchTaskFactory ageSearchTaskFactory = new AgeSearchTaskFactory(cyApplicationManagerServiceRef);
		
		Properties ageSearchTaskFactoryProps = new Properties();
		ageSearchTaskFactoryProps.setProperty("preferredMenu","Apps.Orthoscape.2) Working");
		ageSearchTaskFactoryProps.setProperty("title","PAI and DI analysis");
		registerService(bc,ageSearchTaskFactory,TaskFactory.class, ageSearchTaskFactoryProps);
		
		// Grouping had been broken after one of the Cytoscape updates
		// The gouped nodes are not showing in the network view or it only shown one of all nodes
		// Can't fix from the first time. It seems groups are correct - system see all groups, create and collapse/ungroup them correctly but they are
		// not in the network view.
		// Will fix later or abandon this ability since it still have no any usecases.
		
		// Homology grouping data initialization
//		HomologGroupingTaskFactory homologGroupingTaskFactory = new HomologGroupingTaskFactory(cyApplicationManagerServiceRef);
//		homologGroupingTaskFactory.setcyGroupCreator(globalcyGroupCreator);
//		homologGroupingTaskFactory.setcyGroupManager(globalcyGroupManager);
		
//		Properties homologGroupingTaskFactoryProps = new Properties();
//		homologGroupingTaskFactoryProps.setProperty("preferredMenu","Apps.Orthoscape.3) Grouping");
//		homologGroupingTaskFactoryProps.setProperty("title","Group the homologs");
//		registerService(bc,homologGroupingTaskFactory,TaskFactory.class, homologGroupingTaskFactoryProps);
		
		// Homology ungrouping data initialization
//		HomologUngroupingTaskFactory homologUngroupingTaskFactory = new HomologUngroupingTaskFactory(cyApplicationManagerServiceRef);
//		homologUngroupingTaskFactory.setcyGroupManager(globalcyGroupManager);
		
//		Properties homologUngroupingTaskFactoryProps = new Properties();
//		homologUngroupingTaskFactoryProps.setProperty("preferredMenu","Apps.Orthoscape.3) Grouping");
//		homologUngroupingTaskFactoryProps.setProperty("title","Ungroup the homologs");
//		registerService(bc,homologUngroupingTaskFactory,TaskFactory.class, homologUngroupingTaskFactoryProps);
							
		// Visual stiles data initialization (common to all styles)
		VisualMappingManager vmmServiceRef = getService(bc,VisualMappingManager.class);		
		VisualStyleFactory visualStyleFactoryServiceRef = getService(bc,VisualStyleFactory.class);
		
		VisualMappingFunctionFactory vmfFactoryC = getService(bc,VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
		VisualMappingFunctionFactory vmfFactoryD = getService(bc,VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
		VisualMappingFunctionFactory vmfFactoryP = getService(bc,VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
			
		
		// Visual stiles data initialization (DI Blue-Red)
		CreateBlueRedDIVisualStyleAction CreateBlueRedDIVisualStyleAction = new CreateBlueRedDIVisualStyleAction(cyApplicationManagerServiceRef, vmmServiceRef, visualStyleFactoryServiceRef, 
				vmfFactoryC, vmfFactoryD, vmfFactoryP);
		
		registerService(bc,CreateBlueRedDIVisualStyleAction,CyAction.class, new Properties());
		
		// Visual stiles data initialization (Homology clusters Blue-Red)
		CreateBlueRedGroupVisualStyleAction CreateBlueRedGroupVisualStyleAction = new CreateBlueRedGroupVisualStyleAction(cyApplicationManagerServiceRef, vmmServiceRef, visualStyleFactoryServiceRef, 
				vmfFactoryC, vmfFactoryD, vmfFactoryP);
		
		registerService(bc,CreateBlueRedGroupVisualStyleAction,CyAction.class, new Properties());
		
		// Visual stiles data initialization (PAI Blue-Red)
		CreateBlueRedPAIVisualStyleAction CreateBlueRedPAIVisualStyleAction = new CreateBlueRedPAIVisualStyleAction(cyApplicationManagerServiceRef, vmmServiceRef, visualStyleFactoryServiceRef, 
				vmfFactoryC, vmfFactoryD, vmfFactoryP);
		
		registerService(bc,CreateBlueRedPAIVisualStyleAction,CyAction.class, new Properties());
				
		// Visual stiles data initialization (DI heatmap)
		CreateHeatmapDIVisualStyleAction CreateHeatmapDIVisualStyleAction = new CreateHeatmapDIVisualStyleAction(cyApplicationManagerServiceRef, vmmServiceRef, visualStyleFactoryServiceRef, 
				vmfFactoryC, vmfFactoryD, vmfFactoryP);
		
		registerService(bc,CreateHeatmapDIVisualStyleAction,CyAction.class, new Properties());
		
		// Visual stiles data initialization (Homology clusters heatmap)
		CreateHeatmapGroupVisualStyleAction CreateHeatmapGroupVisualStyleAction = new CreateHeatmapGroupVisualStyleAction(cyApplicationManagerServiceRef, vmmServiceRef, visualStyleFactoryServiceRef, 
				vmfFactoryC, vmfFactoryD, vmfFactoryP);
		
		registerService(bc,CreateHeatmapGroupVisualStyleAction,CyAction.class, new Properties());
			
		// Visual stiles data initialization (PAI heatmap)
		CreateHeatMapPAIVisualStyleAction CreateHeatMapPAIVisualStyleAction = new CreateHeatMapPAIVisualStyleAction(cyApplicationManagerServiceRef, vmmServiceRef, visualStyleFactoryServiceRef, 
				vmfFactoryC, vmfFactoryD, vmfFactoryP);
		
		registerService(bc,CreateHeatMapPAIVisualStyleAction,CyAction.class, new Properties());
		
		// Reporting data initialization
		DrawStatisticsTaskFactory drawStatisticsTaskFactory = new DrawStatisticsTaskFactory(cyApplicationManagerServiceRef);
		
		Properties drawStatisticsTaskFactoryProps = new Properties();
		drawStatisticsTaskFactoryProps.setProperty("preferredMenu","Apps.Orthoscape");
		drawStatisticsTaskFactoryProps.setProperty("title","4) Reporting");
		registerService(bc,drawStatisticsTaskFactory,TaskFactory.class, drawStatisticsTaskFactoryProps);
	}
}

//try {    
//	PrintStream out = new PrintStream("path");
//	out.println(I am here);
//}catch (IOException e2){ e2.printStackTrace(); }