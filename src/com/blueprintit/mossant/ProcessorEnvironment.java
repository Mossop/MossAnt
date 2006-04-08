package com.blueprintit.mossant;

import java.io.File;

public interface ProcessorEnvironment
{
	public File getIncludedFile(String path);
	
	public boolean isDefined(String line);
	
	public String processDefines(String line);
}
