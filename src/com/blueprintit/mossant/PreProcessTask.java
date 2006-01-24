package com.blueprintit.mossant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileNameMapper;

public class PreProcessTask extends Task implements ProcessorEnvironment
{
	private List filesets = new ArrayList();
	private Mapper mapper;
	private Path includes;
	private File destdir;
	private boolean failonerror;
	private boolean overwrite;
	
	public void addFileset(FileSet fileset)
	{
		filesets.add(fileset);
	}
	
	public Path createIncludes()
	{
		includes = new Path(getProject());
		return includes;
	}
	
	public Mapper createMapper()
	{
		mapper = new Mapper(getProject());
		return mapper;
	}
	
	public void setDestdir(File dest)
	{
		destdir=dest;
	}
	
	public void setFaironerror(boolean value)
	{
		failonerror=value;
	}
	
	public void setOverwrite(boolean value)
	{
		overwrite=value;
	}
	
	public void processFile(File source, File target)
	{
		if ((target.exists())&&(target.lastModified()>source.lastModified())&&(!overwrite))
			return;
		log("Preprocessing "+source.getPath()+" to "+target.getPath());
		try
		{
			PreProcessor processor = new PreProcessor(source);
			processor.setEnvironment(this);
			BufferedReader reader = new BufferedReader(processor);
			FileWriter writer = new FileWriter(target);
			String line = reader.readLine();
			while (line!=null)
			{
				writer.write(line);
				line=reader.readLine();
			}
		}
		catch (IOException e)
		{
			if (failonerror)
			{
				throw new BuildException("Error processing "+source.getPath(),e);
			}
			else
			{
				log("Error processing "+source.getPath(),Project.MSG_ERR);
			}
		}
		catch (Throwable t)
		{
			throw new BuildException("Task failure.",t);
		}
	}

	public void execute()
	{
		FileNameMapper mapper = null;
		if (this.mapper!=null)
			mapper=this.mapper.getImplementation();
		
		Iterator i = filesets.iterator();
		while (i.hasNext())
		{
			FileSet set = (FileSet)i.next();
			DirectoryScanner scanner = set.getDirectoryScanner(getProject());
			scanner.scan();
			String[] files = scanner.getIncludedFiles();
			for (int j = 0; j<files.length; j++)
			{
				File source = new File(scanner.getBasedir(),files[j]);
				if (mapper!=null)
				{
					String[] targets = mapper.mapFileName(files[j]);
					if (targets==null)
						continue;
					for (int k = 0; k<targets.length; k++)
					{
						File target = new File(destdir, targets[k]);
						processFile(source, target);
					}
				}
				else
				{
					File target = new File(destdir, files[j]);
					processFile(source, target);
				}
			}
		}
	}

	// Ant Processor Environment
	
	public File getIncludedFile(String path)
	{
		if (includes==null)
			return null;
		
		String[] dirs = includes.list();
		for (int i=0; i<dirs.length; i++)
		{
			File dir = new File(getProject().getBaseDir(), dirs[i]);
			File file = new File(dir,path);
			if ((file.exists())&&(file.isFile()))
				return file;
		}
		return null;
	}

	public String processLine(String text)
	{
		return getProject().replaceProperties(text);
	}
}
