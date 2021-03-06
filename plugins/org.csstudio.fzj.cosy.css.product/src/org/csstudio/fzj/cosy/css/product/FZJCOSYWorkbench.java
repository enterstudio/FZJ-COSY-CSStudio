package org.csstudio.fzj.cosy.css.product;

import java.util.Map;

import org.csstudio.startup.application.OpenDocumentEventProcessor;
import org.csstudio.utility.product.Workbench;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.WorkbenchAdvisor;

/**
 * 
 * <code>FZJCOSYWorkbench</code> is a workbench that takes care of disabling some unneeded stuff.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class FZJCOSYWorkbench extends Workbench {

    /*
     * (non-Javadoc)
     * @see org.csstudio.utility.product.Workbench#beforeWorkbenchCreation(org.eclipse.swt.widgets.Display, org.eclipse.equinox.app.IApplicationContext, java.util.Map)
     */
	@Override
	public Object beforeWorkbenchCreation(Display display,
			IApplicationContext context, Map<String, Object> parameters) {
		WorkbenchUtil.removeUnWantedLog();
		return super.beforeWorkbenchCreation(display, context, parameters);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.csstudio.utility.product.Workbench#createWorkbenchAdvisor(java.util.Map)
	 */

	protected WorkbenchAdvisor createWorkbenchAdvisor(
			Map<String, Object> parameters) {
	    OpenDocumentEventProcessor openDocProcessor = (OpenDocumentEventProcessor) parameters.get(
		        OpenDocumentEventProcessor.OPEN_DOC_PROCESSOR);
		return new FZJCOSYWorkbenchAdvisor(openDocProcessor);
	}
	
	@Override
	public Object runWorkbench(Display display, IApplicationContext context,
			Map<String, Object> parameters) {
		return super.runWorkbench(display, context, parameters);
	}

}
