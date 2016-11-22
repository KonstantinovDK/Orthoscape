package orthoscape;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class AgeSearchTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager appMgr;
	public AgeSearchTaskFactory(CyApplicationManager appMgr){
		this.appMgr = appMgr;
	}
	public TaskIterator createTaskIterator(){
		return new TaskIterator(new AgeSearchTask(this.appMgr.getCurrentNetwork()));
	}
}
