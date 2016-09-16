package org.cytoscape.sample.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class BPtoKEGGConverterTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager appMgr;
	public BPtoKEGGConverterTaskFactory(CyApplicationManager appMgr){
		this.appMgr = appMgr;
	}
	public TaskIterator createTaskIterator(){
		return new TaskIterator(new BPtoKEGGConverterTask(this.appMgr.getCurrentNetwork()));
	}
}
