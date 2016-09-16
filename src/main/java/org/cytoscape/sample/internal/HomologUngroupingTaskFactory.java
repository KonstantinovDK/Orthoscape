package org.cytoscape.sample.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class HomologUngroupingTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager appMgr;
	private CyGroupManager globalcyGroupManager;
	
	public HomologUngroupingTaskFactory(CyApplicationManager appMgr){
		this.appMgr = appMgr;
	}
	public void setcyGroupManager(CyGroupManager mymanager){
		globalcyGroupManager = mymanager;
	}
	
	public TaskIterator createTaskIterator(){
		return new TaskIterator(new HomologUngroupingTask(this.appMgr.getCurrentNetwork(), globalcyGroupManager));
	}
}
