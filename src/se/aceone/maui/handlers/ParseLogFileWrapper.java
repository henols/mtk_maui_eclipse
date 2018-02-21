package se.aceone.maui.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.ProblemMarkerInfo;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import se.aceone.maui.tools.Common;

public class ParseLogFileWrapper implements IMarkerGenerator {
	private static final String MAUI_COMPILER = "se.aceone.mediatek.maui.rvct.compiler";
	static ParseLogFileWrapper PACK_WRAPPER = null;
	// MessageConsole console = null;
	private IProject project;

	private ParseLogFileWrapper() {
		// no constructor needed
	}

	static private ParseLogFileWrapper getParseLogFileWrapper() {
		if (PACK_WRAPPER == null) {
			PACK_WRAPPER = new ParseLogFileWrapper();
		}
		return PACK_WRAPPER;
	}

	static public void update(IProject project, String cConf) {
		getParseLogFileWrapper().internalUpdate(project, cConf);
	}

	public void internalUpdate(final IProject project, String cConf) {

		this.project = project;
		// Check that we have a AVR Project
		try {
			if (project == null || !(project.hasNature(Common.Cnatureid))) {
				Common.log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, "The current selected project is not an C Project", null));
				return;
			}
		} catch (CoreException e) {
			// Log the Exception
			Common.log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, "Can't access project nature", e));
		}

		// String UpLoadTool = Common.getBuildEnvironmentVariable(project,
		// cConf, LinkItConst.ENV_KEY_upload_tool, "");
		// String MComPort = Common.getBuildEnvironmentVariable(project, cConf,
		// LinkItConst.cos, "");
		// console = findConsole("MAUI Compile logs");
		// console.clearConsole();
		// console.activate();
		// console.setInputStream(inputStream);

		executCommand(project, "Parse Log Files");

	}

	protected void executCommand(final IProject project, final String jobName) {
		// final MessageConsole console = this.console;
		Job job = new Job(jobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// MessageConsoleStream msg = console.newMessageStream();
				try {
					project.refreshLocal(IResource.DEPTH_ZERO, monitor);

					ICProjectDescription projectDescription = CCorePlugin.getDefault().getProjectDescriptionManager().getProjectDescription(project);
					ICConfigurationDescription configurationDescription = projectDescription.getDefaultSettingConfiguration();

					parseLogFiles(project, monitor);

				} catch (CoreException e) {
					// TODO Auto-generated catch block
					Common.log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, jobName + ", Failed to build structure in " + project.getName(), e));
					return Status.OK_STATUS;
				}

				// try {
				//// msg.close();
				// } catch (IOException e) {
				// e.printStackTrace();
				// }
				return Status.OK_STATUS;

			}

		};
		job.setRule(null);
		job.setPriority(Job.LONG);
		job.setUser(true);
		job.schedule();
	}

	void parseLogFiles(IProject project, IProgressMonitor monitor) throws CoreException {

		Common.log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Parse log files"));

		File logsFolder = new File(Common.getProjectBuildRoot(), "log");

		// Set up console
		IConsole console = CCorePlugin.getDefault().getConsole();
		console.start(project);

		String[] modules = Common.getSelectedModules((IResource) project);
		project.deleteMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
		
		ErrorParserManager epm = new ErrorParserManager(project, this, new String[] { MAUI_COMPILER });
		PrintStream out = new PrintStream(console.getOutputStream());
		for (String module : modules) {
			File logFile = new File(logsFolder, module + ".log");
			if (logFile.exists() && logFile.isFile()) {
				try {
					out.println("Open Log: "+logFile.getAbsolutePath());
					BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));

					for (String line = in.readLine(); line != null; line = in.readLine()) {
						epm.processLine(line);
						out.println(line);
					}
					in.close();
				} catch (IOException e) {
					CCorePlugin.log(e);
				}

			} else {
				Common.log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "No log file found for: " + module));
			}
		}
		try {
			epm.close();
		} catch (IOException e) {
		}
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

	}

	@Override
	public void addMarker(IResource file, int lineNumber, String errorDesc, int severity, String errorVar) {
		addMarker(new ProblemMarkerInfo(file, lineNumber, errorDesc, severity, errorVar, null));
	}

	@Override
	public void addMarker(ProblemMarkerInfo problemMarkerInfo) {
		try {
			IResource markerResource = problemMarkerInfo.file;
			if (markerResource == null) {
				markerResource = project;
			}
			String externalLocation = null;
			if (problemMarkerInfo.externalPath != null && !problemMarkerInfo.externalPath.isEmpty()) {
				
				externalLocation = problemMarkerInfo.externalPath.toOSString();
//				
//				markerResource =	ResourcesPlugin.getWorkspace().getRoot().findMember(externalLocation, true);
//				project.getFile(path)
//				markerResource = project.getFile(externalLocation);
			}
			
			// Try to find matching markers and don't put in duplicates
			IMarker[] markers = markerResource.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_ONE);
			for (IMarker m : markers) {
				int line = m.getAttribute(IMarker.LINE_NUMBER, -1);
				int sev = m.getAttribute(IMarker.SEVERITY, -1);
				String msg = (String) m.getAttribute(IMarker.MESSAGE);
				if (line == problemMarkerInfo.lineNumber && sev == mapMarkerSeverity(problemMarkerInfo.severity) && msg.equals(problemMarkerInfo.description)) {
					String extloc = (String) m.getAttribute(ICModelMarker.C_MODEL_MARKER_EXTERNAL_LOCATION);
					if (extloc == externalLocation || (extloc != null && extloc.equals(externalLocation))) {
						if (project == null || project.equals(markerResource.getProject())) {
							return;
						}
						String source = (String) m.getAttribute(IMarker.SOURCE_ID);
						if (project.getName().equals(source)) {
							return;
						}
					}
				}
			}

			String type = problemMarkerInfo.getType();
			if (type == null) {
				type = ICModelMarker.C_MODEL_PROBLEM_MARKER;
			}

			IMarker marker = markerResource.createMarker(type);
			marker.setAttribute(IMarker.MESSAGE, problemMarkerInfo.description);
			marker.setAttribute(IMarker.SEVERITY, mapMarkerSeverity(problemMarkerInfo.severity));
			marker.setAttribute(IMarker.LINE_NUMBER, problemMarkerInfo.lineNumber);
			marker.setAttribute(IMarker.CHAR_START, problemMarkerInfo.startChar);
			marker.setAttribute(IMarker.CHAR_END, problemMarkerInfo.endChar);
			if (problemMarkerInfo.variableName != null) {
				marker.setAttribute(ICModelMarker.C_MODEL_MARKER_VARIABLE, problemMarkerInfo.variableName);
			}
			if (externalLocation != null) {
				File file = new File(Common.getSourceCodeRoot(),externalLocation);
				try {
				URI uri = URIUtil.toURI(file.getCanonicalPath());
				if (uri.getScheme() != null) {
					marker.setAttribute(ICModelMarker.C_MODEL_MARKER_EXTERNAL_LOCATION, externalLocation);
					String locationText = NLS.bind(CCorePlugin.getResourceString("ACBuilder.ProblemsView.Location"), //$NON-NLS-1$
							problemMarkerInfo.lineNumber, externalLocation);
					marker.setAttribute(IMarker.LOCATION, locationText);
				}
				}catch (Exception e) {
				}
			} else if (problemMarkerInfo.lineNumber == 0) {
				marker.setAttribute(IMarker.LOCATION, " "); //$NON-NLS-1$
			}else {
			}
			// Set source attribute only if the marker is being set to a file
			// from different project
			if (project != null && !project.equals(markerResource.getProject())) {
				marker.setAttribute(IMarker.SOURCE_ID, project.getName());
			}

			// Add all other client defined attributes.
			Map<String, String> attributes = problemMarkerInfo.getAttributes();
			if (attributes != null) {
				for (Entry<String, String> entry : attributes.entrySet()) {
					marker.setAttribute(entry.getKey(), entry.getValue());
				}
			}
		} catch (CoreException e) {
			CCorePlugin.log(e.getStatus());
		}
	}

	private int mapMarkerSeverity(int severity) {
		switch (severity) {
		case SEVERITY_ERROR_BUILD:
		case SEVERITY_ERROR_RESOURCE:
			return IMarker.SEVERITY_ERROR;
		case SEVERITY_INFO:
			return IMarker.SEVERITY_INFO;
		case SEVERITY_WARNING:
			return IMarker.SEVERITY_WARNING;
		}
		return IMarker.SEVERITY_ERROR;
	}

}
