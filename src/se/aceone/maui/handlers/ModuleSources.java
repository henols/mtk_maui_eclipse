package se.aceone.maui.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import se.aceone.maui.tools.Common;

public class ModuleSources {
	Map<String, Map<String, List<String>>> sources = new HashMap<String, Map<String, List<String>>>();

	public ModuleSources() {
	}

	public Map<String, List<String>> getSources(String module) {
		return sources.get(module);
	}

	public Map<String, List<String>> getSources() {
		Map<String, List<String>> allSrc = new HashMap<String, List<String>>();
		for (Map<String, List<String>> module : sources.values()) {
			for (String path : module.keySet()) {
				List<String> wsPath = allSrc.get(path);
				if (wsPath == null) {
					wsPath = new ArrayList<String>();
					allSrc.put(path, wsPath);
				}
				List<String> modulePath = module.get(path);
				for (String srcFile : modulePath) {
					if (!wsPath.contains(srcFile)) {
						wsPath.add(srcFile);
					}
				}
			}

		}
		return allSrc;
	}

	public void addSource(String moduleName, String source) {
		if (source == null) {
			return;
		}
		Map<String, List<String>> module = sources.get(moduleName);
		if (module == null) {
			module = new HashMap<String, List<String>>();
			sources.put(moduleName, module);
		}
		source = source.trim();
		if (source.length() > 0) {
			String wsPath = new Path(source).removeLastSegments(1).toPortableString().toLowerCase();
			List<String> files = module.get(wsPath);
			if (files == null) {
				files = new ArrayList<String>();
				module.put(wsPath, files);
			}
			String file = new Path(source).toPortableString().toLowerCase();
			if (!files.contains(file)) {
				files.add(file);
			}
		}
	}

}
