package se.aceone.maui.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.PropertyPage;

import se.aceone.maui.Activator;
import se.aceone.maui.tools.Common;

public class MauiModules extends PropertyPage {

	protected Table table;
	protected CheckboxTableViewer tv;
	protected Composite parserGroup;

	protected Map<String, MauiModuleConfiguration> configMap;

	/**
	 * Constructor for SamplePropertyPage.
	 */
	public MauiModules() {
		super();
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		// Composite composite = new Composite(parent, SWT.NONE);
		// GridLayout layout = new GridLayout();
		// composite.setLayout(layout);
		// GridData data = new GridData();
		// data.grabExcessHorizontalSpace = true;
		// data.grabExcessVerticalSpace = true;
		// composite.setLayoutData(data);

		/* put everything inside a scrolled composite, in case we have a very long list of slots */
		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.BORDER | SWT.V_SCROLL);
		scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolledComposite.setLayout(new GridLayout());
		scrolledComposite.setAlwaysShowScrollBars(true);

		/* create a sub-composite that contains all the slot details */
		Composite panel = new Composite(scrolledComposite, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel.setLayout(layout);

		table = new Table(panel, SWT.BORDER | SWT.CHECK | SWT.SINGLE);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// handleBinaryParserChanged();
				// updateButtons();
			}
		});
		tv = new CheckboxTableViewer(table);
		
		tv.setComparator(new ViewerComparator());
		
		tv.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		tv.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String txt = (element != null) ? element.toString() : "";
				if (element instanceof MauiModuleConfiguration)
					txt = ((MauiModuleConfiguration) element).getName();
				return txt;
			}
		});

		tv.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent e) {
				// saveChecked();
			}
		});

		// sashForm.setWeights(new int[] {100, 100});
		initializeModuleList();
		// initializeParserPageMap();
		// handleBinaryParserChanged();

		// addFirstSection(composite);
		// addSeparator(composite);
		// addSecondSection(composite);

		IAdaptable element = getElement();
		if (element instanceof IResource) {
			updateData(Common.getSelectedModules((IResource)element));
		}

		/* tell the ScrolledComposite what it's managing, and how big it should be. */
		scrolledComposite.setContent(panel);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setMinWidth(0);
		panel.setSize(panel.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		return panel;
	}

	private void initializeModuleList() {
		// IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(CCorePlugin.PLUGIN_ID,
		// CCorePlugin.BINARY_PARSER_SIMPLE_ID);
		// if (point != null) {
		// IExtension[] exts = point.getExtensions();
		configMap = new HashMap<String, MauiModuleConfiguration>();
		// for (IExtension ext : exts) {
		File modules = new File(Common.getProjectBuildRoot(), "gprs/MT2502o");
		if (modules.isDirectory()) {
			for (File module : modules.listFiles()) {
				if (module.isDirectory()) {
					String name = module.getName();
					configMap.put(name, new MauiModuleConfiguration(name, name));
				}
			}

		}
		// }
	}

	public void updateData(String[] ids) {
		Object[] data = new Object[configMap.size()];

		HashMap<String, MauiModuleConfiguration> clone = new HashMap<String, MauiModuleConfiguration>(configMap);
		// add checked elements
		int i;
		for (i = 0; i < ids.length; i++) {
			data[i] = clone.get(ids[i]);
			clone.remove(ids[i]);
		}
		// add remaining parsers (unchecked)
		Iterator<String> it = clone.keySet().iterator();
		// i = 0;
		while (it.hasNext()) {
			String s = it.next();
			data[i++] = clone.get(s);
		}
		tv.setInput(data);
		tv.setAllChecked(false);
		// set check marks
		for (i = 0; i < ids.length; i++) {
			if (configMap.containsKey(ids[i])) {
				tv.setChecked(configMap.get(ids[i]), true);
			}
		}
//		tv.getTable().setSortColumn(tv.getTable().getColumn(0));

	}

	protected void performDefaults() {
		super.performDefaults();
		updateData(Common.getDefaultModules());
	}


	public boolean performOk() {
		// store the value in the owner text field

		List<String> props = new ArrayList<String>();

		Object[] checkedElements = tv.getCheckedElements();
		for (Object o : checkedElements) {
			props.add(o.toString());
		}

		String[] modules = props.toArray(new String[0]);
		
		Common.setSelectedModules(((IResource) getElement()), modules);
		return true;
	}


	protected class MauiModuleConfiguration {
		private String id;
		private String name;

		public MauiModuleConfiguration(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getID() {
			return id;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return getID();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof MauiModuleConfiguration) {
				return this.getID().equals(((MauiModuleConfiguration) obj).getID());
			}
			return super.equals(obj);
		}
	}

}