package se.aceone.maui.toolchain;

import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IOptionCommandGenerator;
import org.eclipse.cdt.utils.cdtvariables.IVariableSubstitutor;

public class IncludePathGenerator implements IOptionCommandGenerator {

	public IncludePathGenerator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String generateCommand(IOption option, IVariableSubstitutor macroSubstitutor) {
//		-IC:/dev/tt2_project/W325GT/header_temp -IC:/dev/tt2_project/W325GT/build/GT03_W325 
		return "-I${SOURCE_CODE_ROOT}/header_temp -I${PROJECT_BUILD_ROOT} -I${SOURCE_CODE_ROOT}/plutommi/mmi/Inc";
	}
}
