package se.aceone.maui.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.CMacroEntry;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IProjectActionFilter;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import se.aceone.maui.handlers.ModuleInclude.IncludePath;
import se.aceone.maui.tools.Common;

public class UpdateHandlerWrapper {
	static UpdateHandlerWrapper PACK_WRAPPER = null;
	MessageConsoleStream msg = null;
	MessageConsole console = null;

	private UpdateHandlerWrapper() {
		// no constructor needed
	}

	static private UpdateHandlerWrapper getUpdateHandlerWrapper() {
		if (PACK_WRAPPER == null) {
			PACK_WRAPPER = new UpdateHandlerWrapper();
		}
		return PACK_WRAPPER;
	}

	static public void update(IProject project, String cConf) {
		getUpdateHandlerWrapper().internalUpdate(project, cConf);
	}

	public void internalUpdate(final IProject project, String cConf) {
		console = findConsole("MAUI Update");
		console.clearConsole();
		console.activate();
		msg = console.newMessageStream();

		// Check that we have a AVR Project
		try {
			if (project == null || !(project.hasNature(Common.Cnatureid) || project.hasNature(Common.CCnatureid))) {
				log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, "The current selected project is not an C/C++ Project", null));
				return;
			}
		} catch (CoreException e) {
			// Log the Exception
			log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, "Can't access project nature", e));
		}

		// String UpLoadTool = Common.getBuildEnvironmentVariable(project,
		// cConf, LinkItConst.ENV_KEY_upload_tool, "");
		// String MComPort = Common.getBuildEnvironmentVariable(project, cConf,
		// LinkItConst.cos, "");

		executCommand(project, "Updating resources");

	}

	private void log(IStatus status) {
		StringBuffer buf = new StringBuffer();
		int severity = status.getSeverity();
		if (severity == IStatus.OK) {
			buf.append("OK"); //$NON-NLS-1$
		} else if (severity == IStatus.ERROR) {
			buf.append("ERROR"); //$NON-NLS-1$
		} else if (severity == IStatus.WARNING) {
			buf.append("WARNING"); //$NON-NLS-1$
		} else if (severity == IStatus.INFO) {
			buf.append("INFO"); //$NON-NLS-1$
		} else if (severity == IStatus.CANCEL) {
			buf.append("CANCEL"); //$NON-NLS-1$
		} else {
			buf.append("severity="); //$NON-NLS-1$
			buf.append(severity);
		}
		buf.append(": "); //$NON-NLS-1$
		buf.append(status.getMessage());
		msg.println(buf.toString());
		if (status.getException() != null) {
			msg.println(status.getException().toString());
		}

		if (status.getSeverity() > IStatus.INFO) {
			Common.log(status);
		}
	}

	protected void executCommand(final IProject project, final String jobName) {
		// final MessageConsole console = this.console;
		Job job = new Job(jobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				msg.println("Updating resources");
				try {
					project.refreshLocal(IResource.DEPTH_ZERO, monitor);

					ICProjectDescription projectDescription = CCorePlugin.getDefault().getProjectDescriptionManager().getProjectDescription(project);
					ICConfigurationDescription configurationDescription = projectDescription.getDefaultSettingConfiguration();

					MauiIndexerSetupParticipant indexerParticipant = new MauiIndexerSetupParticipant();
					CCorePlugin.getIndexManager().addIndexerSetupParticipant(indexerParticipant);

					List<ICLanguageSettingEntry> macros = new ArrayList<ICLanguageSettingEntry>();
					ModuleInclude includes = new ModuleInclude();

					collectMacros(project, includes, macros);
					addIncludeFromVia(project, monitor, includes);

					// Map<String, List<File>> sourceFiles = new HashMap<String, List<File>>();
					ModuleSources sourceFiles = new ModuleSources();
					addIncludesList(project, monitor, includes);

					addSourceFileList(project, monitor, sourceFiles);

					addSourceComponentDependencies(project, monitor, sourceFiles, includes);

					matchSourceAndIncludes(sourceFiles, includes);

					linkSourceFolders(project, monitor, configurationDescription, sourceFiles);

					setMacros(configurationDescription, macros);

					linkMakeFiles(project, monitor);

					// linkLogFiles(project, monitor);

					setIncludeFolders(configurationDescription, includes);

					String name = project.getName();

					project.refreshLocal(IResource.DEPTH_ONE, monitor);
					ICProject cProject = CCorePlugin.getDefault().getCoreModel().getCModel().getCProject(name);
					CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, projectDescription, true, monitor);
					indexerParticipant.setPostponeIndexer(false);
					CCorePlugin.getIndexManager().reindex(cProject); // reindex

				} catch (CoreException e) {
					// TODO Auto-generated catch block
					log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, jobName + ", Failed to build structure in " + project.getName(), e));
					return Status.OK_STATUS;
				}

				try {
					msg.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;

			}

		};
		job.setRule(null);
		job.setPriority(Job.LONG);
		job.setUser(true);
		job.schedule();
	}

	private void matchSourceAndIncludes(ModuleSources sourceFiles, ModuleInclude includes) {
		int removedSrc = 0;
		int removedInc = 0;

		List<IncludePath> allIncludes = includes.getIncludes();
		Map<String, List<String>> allSrc = sourceFiles.getSources();

		System.out.println(" Nr source folders: " + allSrc.size());
		System.out.println(" Nr inc folders: " + allIncludes.size());

		for (IncludePath incPath : allIncludes) {
			List<String> list = allSrc.get(incPath.getPath());
			if (list != null) {
				if (list.isEmpty()) {
					removedSrc++;
					// sourceFiles.remove(string);
				} else {
					// removedInc++;
					// includes.remove(string);
				}
			}
		}

		for (IncludePath incPath : allIncludes) {
			String[] l = new File(Common.getSourceCodeRoot(), incPath.getPath()).list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".c");
				}
			});
			if (l == null || l.length == 0) {
				IPath inc = new Path(incPath.getPath());
				for (String string : allSrc.keySet()) {
					String[] s = new File(Common.getSourceCodeRoot(), string).list(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.endsWith(".h");
						}
					});
					if(s==null || s.length == 0) {
					IPath srcPath = new Path(string);
					if (!srcPath.equals(inc) && srcPath.segmentCount() == inc.segmentCount()
							&& srcPath.removeLastSegments(1).equals(inc.removeLastSegments(1))) {
						incPath.matchedToSrc = true;
						String[] i = new File(Common.getSourceCodeRoot(), incPath.getPath()).list(new FilenameFilter() {
							@Override
							public boolean accept(File dir, String name) {
								return name.endsWith(".h");
							}
						});
						for (String srcInc : i) {
							sourceFiles.addSource("__INCS__", new Path(incPath.getPath()).append(srcInc).toPortableString());
						}

						break;
					}}
				}
			}

		}

		System.out.println(" Nr source folders: " + (allSrc.size() - removedSrc));
		System.out.println(" Nr inc folders: " + (allIncludes.size() - removedInc));

		log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Removed " + removedSrc + " source folders, removed " + removedInc + " include folders."));
	}

	public void collectMacros(IProject project, ModuleInclude includes, List<ICLanguageSettingEntry> macros) {

		log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Collecting macros"));
		// addMacro(macros, "MOD_MMI", "NULL"); // not found anywhere just add it.

		try {
			File infoLog = new File(Common.getProjectBuildRoot(), "log/info.log");
			BufferedReader br = new BufferedReader(new FileReader(infoLog));
			String line;
			boolean doMacros = false;
			boolean doIncludes = false;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.equals("[ COMMON OPTION ]")) {
					doMacros = true;
					doIncludes = false;
				} else if (line.equals("[ COMMON INCLUDE PATH ]")) {
					doIncludes = true;
					doMacros = false;
				} else if (doMacros) {
					String[] ss = line.split("=");
					if (ss.length == 2) {
						addMacro(macros, ss[0], ss[1]);
					} else {
						addMacro(macros, line, null);
					}

				} else if (doIncludes) {
					IPath inc = new Path(line);
					String portableString = inc.toPortableString();
					includes.addInclude("__COMMON__", portableString);
				}

			}
			br.close();

			
		} catch (IOException e) {
		}
		String[] modules = Common.getSelectedModules((IResource) project);
		for (String module : modules) {
			File resFile = new File(Common.getProjectBuildRoot(), "module/" + module + "/" + module + ".def");
			if (!resFile.exists()) {
				resFile = new File(Common.getProjectBuildRoot(), "module/PLUTOMMI/" + module + "/" + module + ".def");
				if (!resFile.exists()) {
					log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Module def list for: " + module + " not found."));
					continue;
				}
			}
			try {
				BufferedReader br = new BufferedReader(new FileReader(resFile));
				String line;
				while ((line = br.readLine()) != null) {
					String[] ss = line.split("=");
					if (ss.length == 2) {
						addMacro(macros, ss[0], ss[1]);
					} else {
						addMacro(macros, line, null);
					}
				}
				br.close();
			} catch (IOException e) {
				log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to read inc for: " + module));
			}
		}

	}

	public void addMacro(List<ICLanguageSettingEntry> macros, String macro, String value) {
		if (value != null) {
			log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Adding macro: " + macro + " = " + value));
		} else {
			log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Adding macro: " + macro));
		}
		macros.add(new CMacroEntry(macro, value, ICSettingEntry.BUILTIN | ICSettingEntry.READONLY));
	}

	public void setMacros(ICConfigurationDescription configurationDescription, List<ICLanguageSettingEntry> macros) {
		// find all languages
		for (ICFolderDescription folderDescription : configurationDescription.getFolderDescriptions()) {
			ICLanguageSetting[] settings = folderDescription.getLanguageSettings();

			// Add include path to all languages
			for (ICLanguageSetting setting : settings) {
				String langId = setting.getLanguageId();
				if (langId != null && langId.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
					setting.setSettingEntries(ICSettingEntry.MACRO, macros);
				}
			}
		}
	}

	void linkMakeFiles(IProject project, IProgressMonitor monitor) {

		log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Linking make files"));

		IFolder make = project.getFolder("make");
		if (!make.exists()) {
			try {
				make.create(true, true, monitor);
			} catch (CoreException e) {
				log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to create local make folder"));
				return;
			}
		}

		final List<IFile> filesToDelete = new ArrayList<IFile>();
		final List<IFolder> foldersToDelete = new ArrayList<IFolder>();

		try {
			make.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					if (resource.isLinked()) {
						if (resource.getType() == IResource.FILE) {
							filesToDelete.add((IFile) resource);
						} else if (resource.getType() == IResource.FOLDER) {
							foldersToDelete.add((IFolder) resource);
						}
						return false;
					}
					return true;
				}
			});
			for (IFile file : filesToDelete) {
				file.delete(true, monitor);
				System.out.println("Delete file: " + file);
			}
			for (IFolder folder : foldersToDelete) {
				System.out.println("Delete folder: " + folder);
				folder.delete(true, monitor);
			}

		} catch (CoreException e1) {
			e1.printStackTrace();
		}

		try {
			File makeFoleder = new File(Common.getSourceCodeRoot(), "make");
			for (File makeFile : makeFoleder.listFiles()) {
				if (makeFile.isFile() && (makeFile.getName().contains(Common.getProjectName()) || makeFile.getName().contains("GT03_O"))) {
					IFile fileRes = make.getFile(makeFile.getName());
					try {
						fileRes.createLink(new Path(makeFile.getCanonicalPath()), IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, monitor);
					} catch (CoreException e) {
						log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to link: " + makeFile.getName()));
					}
				}
			}

			String[] modules = Common.getSelectedModules((IResource) project);
			IPath makePath = new Path(Common.getSourceCodeRoot().getCanonicalPath()).append("make");
			for (String module : modules) {
				IFolder moduleFolder = make.getFolder(module);

				IPath modulePath = makePath.append(module);
				try {
					if (modulePath.toFile().isDirectory()) {
						moduleFolder.createLink(modulePath, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, monitor);
					} else {
						modulePath = makePath.append("plutommi").append(module);
						moduleFolder.createLink(modulePath, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, monitor);
					}
				} catch (CoreException e) {
					log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to link: " + modulePath));
				}
			}
		} catch (IOException e) {
			log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to link make files"));
		}

	}

	void linkLogFiles(IProject project, IProgressMonitor monitor) {

		log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Linking log files"));

		IFolder logs = project.getFolder("log");
		if (!logs.exists()) {
			try {
				logs.create(true, true, monitor);
			} catch (CoreException e) {
				log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to create local log folder"));
				return;
			}
		}

		final List<IFile> filesToDelete = new ArrayList<IFile>();
		final List<IFolder> foldersToDelete = new ArrayList<IFolder>();

		try {
			logs.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					if (resource.isLinked()) {
						if (resource.getType() == IResource.FILE) {
							filesToDelete.add((IFile) resource);
						} else if (resource.getType() == IResource.FOLDER) {
							foldersToDelete.add((IFolder) resource);
						}
						return false;
					}
					return true;
				}
			});
			for (IFile file : filesToDelete) {
				file.delete(true, monitor);
				System.out.println("Delete file: " + file);
			}
			for (IFolder folder : foldersToDelete) {
				System.out.println("Delete folder: " + folder);
				folder.delete(true, monitor);
			}

		} catch (CoreException e1) {
			e1.printStackTrace();
		}

		File logsFolder = new File(Common.getProjectBuildRoot(), "log");

		String[] modules = Common.getSelectedModules((IResource) project);
		for (String module : modules) {
			File logFile = new File(logsFolder, module + ".log");
			if (logFile.exists() && logFile.isFile()) {
				IFile fileRes = logs.getFile(logFile.getName());

				try {
					fileRes.createLink(new Path(logFile.getCanonicalPath()), IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, monitor);
				} catch (Exception e) {
					log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to link: " + logFile.getName()));
				}

			} else {
				log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "No log file found for: " + module));
			}
		}
	}

	void linkSourceFolders(IProject project, IProgressMonitor monitor, ICConfigurationDescription configuration, ModuleSources sourceFiles) {
		Map<String, List<String>> allSrc = sourceFiles.getSources();
		log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Number of Source File folders: " + allSrc.size()));

		IFolder src = project.getFolder("src");
		if (!src.exists()) {
			try {
				src.create(true, true, monitor);
			} catch (CoreException e) {
				log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, "Failed to creare source folder.", e));
				return;
			}
		}

		// project.accept(new IResourceVistor(type name = new type();){});

		// accept(IResourceProxyVisitor)

		final List<IFile> filesToDelete = new ArrayList<IFile>();
		final List<IFolder> foldersToDelete = new ArrayList<IFolder>();

		try {
			src.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					if (resource.isLinked()) {
						if (resource.getType() == IResource.FILE) {
							filesToDelete.add((IFile) resource);
						} else if (resource.getType() == IResource.FOLDER) {
							foldersToDelete.add((IFolder) resource);
						}
						return false;
					}
					return true;
				}
			});
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		List<CSourceEntry> srcFolders = new ArrayList<CSourceEntry>();

		try {
			for (String name : allSrc.keySet()) {
				// System.out.println("Src ---- \"" + name + "\"");
				IFolder folderHandle = src.getFolder(name);
				List<String> sourceFileList = allSrc.get(name);
				List<IPath> exclPatten = new ArrayList<IPath>();
				int match = matchFilesInFolder(name, sourceFileList, exclPatten);
				log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Matching: " + match + " name: " + name));
				if (match == MATCH || sourceFileList == null) {
					// System.out.println("We have all files");
					IContainer pCont = folderHandle.getParent();
					if (pCont.getType() == IResource.FOLDER) {
						IFolder parent = (IFolder) pCont;
						boolean res = makeFolders(parent, monitor);
						if (res) {
							IPath source = new Path(Common.getSourceCodeRoot().getCanonicalPath()).append(name);
							try {
								if (!folderHandle.isLinked()) {
									folderHandle.createLink(source, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, monitor);
								}
								foldersToDelete.remove(folderHandle);
							} catch (CoreException e) {
								log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, "Failed to create link for: " + source));
							}
						} else {
							log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to create folder, probably exists: " + parent));
						}
						srcFolders.add(new CSourceEntry(folderHandle, exclPatten.toArray(new IPath[0]), ICSettingEntry.NONE));
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			for (IFile file : filesToDelete) {
				file.delete(true, monitor);
				System.out.println("Delete file: " + file);
			}
			for (IFolder folder : foldersToDelete) {
				System.out.println("Delete folder: " + folder);
				folder.delete(true, monitor);
			}

			configuration.setSourceEntries(srcFolders.toArray(new CSourceEntry[0]));
		} catch (CoreException e) {
			log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, "Failed to add source entries", e));
		}

	}

	final static int DONT_MATCH = 0;
	final static int MATCH = 1;
	final static int ALL_EXCLUDED = 2;

	private int matchFilesInFolder(String name, List<String> sourceFileList, List<IPath> expPatten) {
		int match = MATCH;
		if(sourceFileList == null) {
			return DONT_MATCH;
		}
		File sourceFolder = new File(Common.getSourceCodeRoot(), name);
		if (!sourceFolder.isDirectory()) {
			return DONT_MATCH;
		}
		File[] fileList = sourceFolder.listFiles();
		if (fileList == null || fileList.length < sourceFileList.size()) {
			log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "FileList.length < sourceFileList.size() " + fileList.length + " < " + sourceFileList.size()));
			for (String file : sourceFileList) {
				log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Missing file: " + file.toString()));
			}
			return DONT_MATCH;
		}

		IPath srcRoot = new Path(Common.getSourceCodeRoot().getAbsolutePath());

		if (fileList.length > sourceFileList.size()) {
			boolean hasFiles = false;
			for (File file : fileList) {
				if (file.isFile()) {
					String fileName = file.getName().toLowerCase();
					String relPath = new Path(file.getAbsolutePath()).makeRelativeTo(srcRoot).toPortableString().toLowerCase();
					// if (!(fileName.endsWith(".txt") || fileName.endsWith(".res") || fileName.endsWith(".h")) &&
					// !sourceFileList.contains(relPath)) {
					if (!(fileName.endsWith(".txt") || fileName.endsWith(".res") || fileName.endsWith(".h")) && !sourceFileList.contains(relPath)) {
						log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Exclude: " + relPath));
						expPatten.add(new Path(file.getName()));
						// match = DONT_MATCH;
						hasFiles = true;
					}
				} else if (file.isDirectory()) {
					// for (File f : file.listFiles()) {
					// String fileName = f.getName().toLowerCase();
					// if (!(fileName.endsWith(".txt") || fileName.endsWith(".res"))) {
					// log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Folder with non desired files: " + f));
					// expPatten.add(new Path(file.getName()));
					// break;
					// }
					// }
					expPatten.add(new Path(file.getName()));
				}
			}
			if (hasFiles && sourceFileList.isEmpty()) {
				return DONT_MATCH;
			}
			if (/* !hasFiles || */fileList.length == expPatten.size()) {
				log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "All excluded: " + name));
				return ALL_EXCLUDED;
			}
			if (match == MATCH) {
				log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Odd match for: " + name));
			}
		}
		return match;
	}

	private boolean makeFolders(IFolder srcHandle, IProgressMonitor monitor) {
		boolean res = true;
		try {
			if (!srcHandle.exists()) {
				IContainer parent = srcHandle.getParent();
				if (parent.getType() == IResource.FOLDER) {
					res = makeFolders((IFolder) parent, monitor);
				}
				if (res) {
					srcHandle.create(true, true, monitor);
				}
			}
		} catch (CoreException e) {
			log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to create folder: " + srcHandle.getFullPath()));
			return false;
		}
		return res;
	}

	public void setIncludeFolders(ICConfigurationDescription configurationDescription, ModuleInclude moduleIncludes) {
		List<ICLanguageSettingEntry> inc = new ArrayList<ICLanguageSettingEntry>();

		List<IncludePath> includes = moduleIncludes.getIncludes();
		for (IncludePath path : includes) {
			File includeFolder = new File(Common.getSourceCodeRoot(), path.getPath());

			FilenameFilter fileFilter = new FilenameFilter() {
				@Override
				public boolean accept(File current, String name) {
					return new File(current, name).isFile();
					// return true;
				}
			};

			if (includeFolder.exists() && includeFolder.isDirectory() && includeFolder.list(fileFilter) != null && includeFolder.list(fileFilter).length > 0) {
				if (!(includeFolder.list(fileFilter) != null && includeFolder.list(fileFilter).length > 0)) {
					log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "No files here: " + path));
				}
				try {
					inc.add(new CIncludePathEntry(new Path(includeFolder.getCanonicalPath()), ICSettingEntry.READONLY));
				} catch (IOException e) {
					log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Could not get path for: " + path));
				}
			}
		}

		IPath compilerLocation = new Path(Common.getCompilerPath());
		IPath rvtcIncl = compilerLocation.append("Data/3.1/569/include/windows");
		inc.add(0, new CIncludePathEntry(rvtcIncl, ICSettingEntry.READONLY));

		log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "Number of Include file folder: " + inc.size()));

		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] settings = folderDescription.getLanguageSettings();

		// Add include path to all languages
		for (ICLanguageSetting setting : settings) {
			String langId = setting.getLanguageId();
			if (langId != null && langId.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
				setting.setSettingEntries(ICSettingEntry.INCLUDE_PATH, inc);
			}
		}
	}

	private boolean addIncludeFromVia(IProject project, IProgressMonitor monitor, ModuleInclude includes) {
		String[] modules = Common.getSelectedModules((IResource) project);
		for (String module : modules) {
			File resFile = new File(Common.getProjectBuildRoot(), "via/" + module + "_inc.via");
			if (!resFile.exists()) {
				log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Module via for: " + module + " not found."));
				continue;
			}
			try {
				BufferedReader br = new BufferedReader(new FileReader(resFile));
				String line;
				while ((line = br.readLine()) != null) {
					String[] incs = line.split("-I");
					for (String inc : incs) {
						String include = inc.trim();
						if (!include.equals("") && !include.equals("header_temp")) {
							includes.addInclude(module, include);
						}

					}
				}

				br.close();
			} catch (IOException e) {
				log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to read via for: " + module));
			}
		}
		return true;
	}

	private boolean addIncludesList(IProject project, IProgressMonitor monitor, ModuleInclude includes) {
		String[] modules = Common.getSelectedModules((IResource) project);
		for (String module : modules) {
			File resFile = new File(Common.getProjectBuildRoot(), "module/" + module + "/" + module + ".inc");
			if (!resFile.exists()) {
				resFile = new File(Common.getProjectBuildRoot(), "module/PLUTOMMI/" + module + "/" + module + ".inc");
				if (!resFile.exists()) {
					log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Module include list for: " + module + " not found."));
					continue;
				}
			}
			try {
				BufferedReader br = new BufferedReader(new FileReader(resFile));
				String line;
				while ((line = br.readLine()) != null) {
					includes.addInclude(module, line);
					// addSourceFile(line, sourceFiles);
				}
				br.close();
			} catch (IOException e) {
				log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to read inc for: " + module));
			}
		}
		return true;
	}

	private boolean addSourceFileList(IProject project, IProgressMonitor monitor, ModuleSources sourceFiles) {
		String[] modules = Common.getSelectedModules((IResource) project);
		for (String module : modules) {
			File resFile = new File(Common.getProjectBuildRoot(), "module/" + module + "/" + module + ".lis");
			if (!resFile.exists()) {
				resFile = new File(Common.getProjectBuildRoot(), "module/PLUTOMMI/" + module + "/" + module + ".lis");
				if (!resFile.exists()) {
					log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Module source file list for: " + module + " not found."));
					continue;
				}
			}
			try {
				BufferedReader br = new BufferedReader(new FileReader(resFile));
				String line;
				while ((line = br.readLine()) != null) {
					sourceFiles.addSource(module, line);
					// addSourceFile(line, sourceFiles);
				}
				br.close();
			} catch (IOException e) {
				log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to read source files for: " + module));
			}
		}
		return true;
	}

	private boolean addSourceComponentDependencies(IProject project, IProgressMonitor monitor, ModuleSources sourceFiles, ModuleInclude includes) {
		String[] modules = Common.getSelectedModules((IResource) project);
		for (String module : modules) {
			File resFile = new File(Common.getProjectBuildRoot(), "gprs/MT2502r/comp_dep/" + module + ".det");
			if (!resFile.exists()) {
				log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Module dependency for: " + module + " not found."));
				continue;
			}
			try {
				BufferedReader br = new BufferedReader(new FileReader(resFile));
				String line;
				while ((line = br.readLine()) != null) {

					int lastSlash = line.lastIndexOf('\\');
					int colon = line.lastIndexOf(':') + 1;

					// int lastSlash = line.lastIndexOf('\\', dotIndex);
					if (lastSlash > 0) {
						// System.out.println("line.substring("+dotIndex+", "+lastSlash+")");
						// System.out.println(line.substring(0,lastSlash));
						String path = line.substring(colon, lastSlash).trim();
						if (path.endsWith(".c")) {
							sourceFiles.addSource(module, path);
						} else if (path.endsWith(".h")) {
							includes.addInclude(module, path);
						}
					} else {
						log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Cant parse dependency line" + lastSlash + " " + line));

					}

				}
				br.close();
			} catch (IOException e) {
				log(new Status(IStatus.WARNING, Common.CORE_PLUGIN_ID, "Failed to read dependency for: " + module));
			}
		}

		return true;

	}

	public static MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

}
