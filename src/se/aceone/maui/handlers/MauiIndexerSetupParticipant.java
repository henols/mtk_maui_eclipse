package se.aceone.maui.handlers;

import org.eclipse.cdt.core.index.IndexerSetupParticipant;
import org.eclipse.cdt.core.model.ICProject;

public class MauiIndexerSetupParticipant extends IndexerSetupParticipant {
	boolean postponeIndexer = true;
	public boolean postponeIndexerSetup(ICProject cProject) {
		return postponeIndexer; // implement your condition as long as project creation and setup is pending
	}

	@Override
	public void onIndexerSetup(ICProject project) {
		// TODO Auto-generated method stub
		super.onIndexerSetup(project);
	}

	
	public void setPostponeIndexer(boolean postponeIndexer) {
		this.postponeIndexer = postponeIndexer;
	}
};
