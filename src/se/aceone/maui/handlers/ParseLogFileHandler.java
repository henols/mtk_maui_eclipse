package se.aceone.maui.handlers;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import se.aceone.maui.tools.Common;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class ParseLogFileHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
//		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
//		MessageDialog.openInformation(window.getShell(), "MAUI", "Update me");

		System.out.println(event);
		System.out.println(Common.getSelectedProjects());
		IProject selectedProjects[] = Common.getSelectedProjects();
		switch (selectedProjects.length) {
		case 0:
			Common.log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, "No project found."));
			break;
		case 1:
			final IProject buildProject = selectedProjects[0];
			Job mBuildJob = new Job("") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {

							String cConf = CoreModel.getDefault().getProjectDescription(buildProject)
									.getActiveConfiguration().getName();
							ParseLogFileWrapper.update(buildProject, cConf);
						}
					});
					

					return Status.OK_STATUS;
				}

			};
			mBuildJob.setPriority(Job.INTERACTIVE);
			mBuildJob.schedule();
			break;
		default:
			Common.log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, "Only 1 project should be seleted: found "
					+ Integer.toString(selectedProjects.length) + " the names are :" + selectedProjects.toString()));

		}

		return null;
	}

}
