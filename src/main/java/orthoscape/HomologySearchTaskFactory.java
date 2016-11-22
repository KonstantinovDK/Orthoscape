package orthoscape;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class HomologySearchTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager appMgr;
	public HomologySearchTaskFactory(CyApplicationManager appMgr){
		this.appMgr = appMgr;
	}
	public TaskIterator createTaskIterator(){
		return new TaskIterator(new HomologySearchTask(this.appMgr.getCurrentNetwork()));
	}
}
