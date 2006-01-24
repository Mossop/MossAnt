package com.blueprintit.mossant;

import java.io.File;

public interface ProcessorEnvironment
{
	public File getIncludedFile(String path);
	
	public String processLine(String line);
}
