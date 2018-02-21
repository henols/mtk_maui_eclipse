/*
 * LinkIt Tool Chain, an eclipse plugin for LinkIt SDK 1.0 and 2.0
 * 
 * Copyright © 2015 Henrik Olsson (henols@gmail.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.aceone.maui.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import se.aceone.maui.handlers.UpdateHandlerWrapper;
import se.aceone.maui.tools.Common;
import se.aceone.maui.tools.ShouldHaveBeenInCDT;

public class NewMauiProjectWizard extends AbstrctNewMauiProjectWizard {
	
	private static final String TOOL_CHAIN_ID = "se.aceone.mediatek.maui.toolChain.default.rvct";
	private static final String PROJECT_TYPE_ID = "se.aceone.mediatek.maui.projectType";
	private static final String BUILD_SYSTEM_ID = "se.aceone.mediatek.maui.builder.rvct";
//	private static final String BUILD_SYSTEM_ID = "se.aceone.mediatek.maui.buildDefinitions";
	static final String COMPILER_TOOL_PATH = "COMPILERTOOLPATH";
	static final String COMPILER_TOOL_PATH_RVTC = "Programs/3.1/569/win_32-pentium";
	static final String COMPILER_PATH = "COMPILERPATH";

	public static final String COMPILER_IT_SDK10_RTVC = "RVCT31BIN";

	public NewMauiProjectWizard() {
		super();
	}

	@Override
	/**
	 * adds pages to the wizard. We are using the standard project wizard of Eclipse
	 */
	public void addPages() {
		//
		// We assume everything is OK as it is tested in the handler
		// create each page and fill in the title and description
		// first page to fill in the project name
		//
		mWizardPage = new WizardNewProjectCreationPage("New MAUI Tool Chain Project");
		mWizardPage.setDescription("Create a new MAUI Tool Chain Project.");
		mWizardPage.setTitle("New MAUI Tool Chain Project");
//		AbstractUIPlugin plugin = Activator.getDefault();

		// TODO add a nice image
		// ImageRegistry imageRegistry = plugin.getImageRegistry();
		// Image myImage = imageRegistry.get(Activator.CPU_64PX);
		// ImageDescriptor image = ImageDescriptor.createFromImage(myImage);
		// mWizardPage.setImageDescriptor(image);
		//
		// /
		addPage(mWizardPage);
	}

	/**
	 * This creates the project in the workspace.
	 * 
	 * @param desc
	 * @param projectHandle
	 * @param monitor
	 * @throws OperationCanceledException
	 */
	protected void createProject(IProjectDescription desc, IProject project, IProgressMonitor monitor) throws OperationCanceledException {

		monitor.beginTask("", 2000);
		try {

			project.create(desc, monitor);

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			
			project.open(IResource.BACKGROUND_REFRESH, monitor);
			ICProjectDescription prjCDesc = ShouldHaveBeenInCDT.setCProjectDescription(project, TOOL_CHAIN_ID, true, monitor);

			
//			
//			IProject cdtProj = CCorePlugin.getDefault().createCDTProject(description, project, monitor);
//
//
//			ICProjectDescription pd = CCorePlugin.getDefault().createProjectDescription(cdtProj, true);
//			IManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
//
//			IProjectType type = ManagedBuildManager.getProjectType(PROJECT_TYPE_ID);
//			IManagedProject 
//			mProj = ManagedBuildManager.createManagedProject(project, type);
//			info.setManagedProject(mProj);
//
//			IToolChain toolChain = ManagedBuildManager.getExtensionToolChain(TOOL_CHAIN_ID);
//
//			IConfiguration[] conf = ManagedBuildManager.getExtensionConfigurations(toolChain, type);
//
//			if (mProj != null) {
//				for (int i = 0; i < conf.length; i++) {
//					IConfiguration config = conf[i];
//					int id = ManagedBuildManager.getRandomNumber();
//					IConfiguration newConfig = mProj.createConfiguration(config, config.getId() + "." + id);
//					newConfig.setArtifactName(project.getName());
//					ICConfigurationDescription des= pd.createConfiguration(BUILD_SYSTEM_ID, newConfig.getConfigurationData());
//					
//					setDefaultLanguageSettingsProviders(project, TOOL_CHAIN_ID, newConfig, des);
//					
//				}
//				
//				IConfiguration[] newConfigs = mProj.getConfigurations();
//				IConfiguration defaultCfg = null;
//				for (int i = 0; i < newConfigs.length; i++) {
//					if (newConfigs[i].isSupported()) {
//						defaultCfg = newConfigs[i];
//						break;
//					}
//				}
//
//				
//				
//				
//				CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, pd, true, monitor);
//
//				if (defaultCfg != null && newConfigs.length > 0) {
//					defaultCfg = newConfigs[0];
//				}
//
//				if (defaultCfg != null) {
//					ManagedBuildManager.setDefaultConfiguration(project, defaultCfg);
//					ManagedBuildManager.setSelectedConfiguration(project, defaultCfg);
//				}
				
//				
//				ManagedBuildManager.setNewProjectVersion(project);
//				
//
//			}
//			
//			pd.setCdtProjectCreated();
////			CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, pd, true, monitor);
//
			addTheNatures(desc);
////			ParseLogFileWrapper.update(project, project.getName());
//
//			monitor.done();

			
			ICConfigurationDescription defaultConfigDescription = prjCDesc.getConfigurations()[0];

//			IConfiguration[] newConfigs = mProj.getConfigurations();
//			IConfiguration defaultCfg = null;
//			for (int i = 0; i < newConfigs.length; i++) {
//				if (newConfigs[i].isSupported()) {
//					defaultCfg = newConfigs[i];
//					break;
//				}
//			}

			
//			armcc --thumb --bss_threshold=0 --split_sections --dwarf2 --cpu=ARM7EJ-S --fpmode=ieee_fixed -O3 --diag_suppress=1,1295,1296,2548 -Iheader_temp
//			-c 
//					-o./build/GT03_W325/gprs/MT2502o/mmi_app/ViewSettingCommon.obj 
			
//			CFLAGS = --cpu ARM7EJ-S --littleend -O3 --remove_unneeded_entities -D__RVCT__ -JC:\Progra~1\ARM\RVCT\Data\3.1\569\include\windows --fpmode=ieee_fixed --split_sections --diag_suppress 1,1295,1296,2548 --dwarf2 -D__SERIAL_FLASH_EN__ -D__SERIAL_FLASH_SUPPORT__ --bss_threshold=0
//					CPLUSFLAGS = --cpp --cpu ARM7EJ-S --littleend -O3 --remove_unneeded_entities -D__RVCT__ -JC:\Progra~1\ARM\RVCT\Data\3.1\569\include\windows --fpmode=ieee_fixed --split_sections --diag_suppress 1,1295,1296,2548 --dwarf2 --bss_threshold=0
			
			IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
			IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
			contribEnv.addVariable(new EnvironmentVariable(COMPILER_TOOL_PATH, new Path(COMPILER_TOOL_PATH_RVTC).toPortableString()), defaultConfigDescription);
			contribEnv.addVariable(new EnvironmentVariable(COMPILER_PATH, new Path(getCompilerPath()).toPortableString()), defaultConfigDescription);
			contribEnv.addVariable(new EnvironmentVariable("SOURCE_CODE_ROOT", new Path(Common.getSourceCodeRoot().getCanonicalPath()).toPortableString()), defaultConfigDescription);
			contribEnv.addVariable(new EnvironmentVariable("PROJECT_BUILD_ROOT", new Path(Common.getProjectBuildRoot().getCanonicalPath()).toPortableString()), defaultConfigDescription);
			
			prjCDesc.setActiveConfiguration(defaultConfigDescription);
			prjCDesc.setCdtProjectCreated();
			CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, prjCDesc, true,
					null);
			project.setDescription(desc, new NullProgressMonitor());
			
			Common.setSelectedModules(project, Common.getDefaultModules());
			UpdateHandlerWrapper.update(project, project.getName());

			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			


			monitor.done();

		} catch (Exception e) {
			String message = "Failed to create project " + project.getName();
			Common.log(new Status(IStatus.ERROR, Common.CORE_PLUGIN_ID, message, e));
			throw new OperationCanceledException(message);
		}

	}
	public String getCompilerPath() {
		String compiler = System.getenv().get(COMPILER_IT_SDK10_RTVC);
		if (compiler == null) {
			compiler = "C:/Program Files/ARM/RVCT";
		} else {
			compiler = new Path(compiler).removeLastSegments(4).toPortableString();
		}
		return compiler;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}


	private void setDefaultLanguageSettingsProviders(IProject project, String toolChainId, IConfiguration cfg, ICConfigurationDescription cfgDescription) {
		// propagate the preference to project properties
		boolean isPreferenceEnabled = ScannerDiscoveryLegacySupport.isLanguageSettingsProvidersFunctionalityEnabled(null);
		ScannerDiscoveryLegacySupport.setLanguageSettingsProvidersFunctionalityEnabled(project, isPreferenceEnabled);

		if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
			ILanguageSettingsProvidersKeeper lspk = (ILanguageSettingsProvidersKeeper) cfgDescription;

			lspk.setDefaultLanguageSettingsProvidersIds(new String[] { toolChainId });

			List<ILanguageSettingsProvider> providers = getDefaultLanguageSettingsProviders(cfg, cfgDescription);
			lspk.setLanguageSettingProviders(providers);
		}
	}

	private List<ILanguageSettingsProvider> getDefaultLanguageSettingsProviders(IConfiguration cfg, ICConfigurationDescription cfgDescription) {
		List<ILanguageSettingsProvider> providers = new ArrayList<ILanguageSettingsProvider>();
		String[] ids = cfg != null ? cfg.getDefaultLanguageSettingsProviderIds() : null;

		if (ids == null) {
			// Try with legacy providers
			ids = ScannerDiscoveryLegacySupport.getDefaultProviderIdsLegacy(cfgDescription);
		}

		if (ids != null) {
			for (String id : ids) {
				ILanguageSettingsProvider provider = null;
				if (!LanguageSettingsManager.isPreferShared(id)) {
					provider = LanguageSettingsManager.getExtensionProviderCopy(id, false);
				}
				if (provider == null) {
					provider = LanguageSettingsManager.getWorkspaceProvider(id);
				}
				providers.add(provider);
			}
		}

		return providers;
	}

}
