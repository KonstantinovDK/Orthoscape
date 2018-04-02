package orthoscape;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class HomologGroupingTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager appMgr;
	private CyGroupFactory globalcyGroupCreator;
	private CyGroupManager globalcyGroupManager;
	
	public HomologGroupingTaskFactory(CyApplicationManager appMgr){
		this.appMgr = appMgr;
	}
	public void setcyGroupCreator(CyGroupFactory myfactory){
		globalcyGroupCreator = myfactory;
	}
	public void setcyGroupManager(CyGroupManager mymanager){
		globalcyGroupManager = mymanager;
	}
	
	public TaskIterator createTaskIterator(){
		return new TaskIterator(new HomologGroupingTask(this.appMgr.getCurrentNetwork(), globalcyGroupCreator, globalcyGroupManager));
	}
}
