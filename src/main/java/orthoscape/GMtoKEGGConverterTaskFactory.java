package orthoscape;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class GMtoKEGGConverterTaskFactory extends AbstractTaskFactory {

	private CyApplicationManager appMgr;
	public GMtoKEGGConverterTaskFactory(CyApplicationManager appMgr){
		this.appMgr = appMgr;
	}
	public TaskIterator createTaskIterator(){
		return new TaskIterator(new GMtoKEGGConverterTask(this.appMgr.getCurrentNetwork()));
	}
}
