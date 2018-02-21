package se.aceone.maui.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class ModuleInclude {
	Map<String, List<IncludePath>> includes = new HashMap<String, List<IncludePath>>();

	public ModuleInclude() {
	}

	public List<IncludePath> getIncludes(String module) {
		return includes.get(module);
	}

	public List<IncludePath> getIncludes() {
		List<IncludePath> allInc = new ArrayList<IncludePath>();
		for (List<IncludePath> moduleInc : includes.values()) {
			for (IncludePath string : moduleInc) {
				if (!allInc.contains(string)) {
					allInc.add(string);
				}
			}

		}
		return allInc;
	}

	public void addInclude(String moduleName, String include) {
		if(include == null) {
			return;
		}
		List<IncludePath> module = includes.get(moduleName);
		if (module == null) {
			module = new ArrayList<IncludePath>();	
			includes.put(moduleName, module);
		}
		IPath path = new Path(include.trim());
		if(path.getFileExtension() != null && path.getFileExtension().equals(".h")) {
			path = path.removeLastSegments(1);
		} 
		//.toPortableString().toLowerCase();
		include = path.toPortableString().toLowerCase();
		IncludePath incPath = new IncludePath(include);
		if (include.length() > 0 && !module.contains(incPath)) {
			module.add(incPath);
		}
	}
	
	class IncludePath{
		String path;
		boolean matchedToSrc = false;
		IncludePath(String path){
			this.path = path;
		}

		@Override
		public boolean equals(Object obj) {
					boolean res = (obj != null) && obj.equals(path);
					return res;
		}

		public String getPath() {
			return path;
		}
	}
}
