package org.cytoscape.sample.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class HomologGroupingTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager appMgr;
	private CyGroupFactory globalcyGroupCreator;
	
	public HomologGroupingTaskFactory(CyApplicationManager appMgr){
		this.appMgr = appMgr;
	}
	public void setcyGroupCreator(CyGroupFactory myfactory){
		globalcyGroupCreator = myfactory;
	}
	
	public TaskIterator createTaskIterator(){
		return new TaskIterator(new HomologGroupingTask(this.appMgr.getCurrentNetwork(), globalcyGroupCreator));
	}
}
