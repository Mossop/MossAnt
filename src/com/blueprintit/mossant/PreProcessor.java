package com.blueprintit.mossant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreProcessor extends Reader
{
	private class State
	{
		private int line = 0;
		private File file;
		private BufferedReader reader;
		private State parent;
		
		public State(State parent, File file, BufferedReader reader)
		{
			this.parent=parent;
			this.file=file;
			this.reader=reader;
			this.line=0;
		}
		
		public State getParent()
		{
			return parent;
		}
		
		public String readLine() throws IOException
		{
			try
			{
				String result = reader.readLine();
				line++;
				return result;
			}
			catch (IOException e)
			{
				throw new IOException("Error reading from file "+file.getPath()+":"+line+" "+e.getMessage());
			}
		}
		
		public void close() throws IOException
		{
			try
			{
				reader.close();
			}
			catch (IOException e)
			{
			}
		}
	}
	
	private abstract class ControlState
	{
		protected int startline;
		protected State fromstate;
		protected ControlState parent;
		
		protected ControlState(ControlState parent)
		{
			this.startline=state.line;
			this.fromstate=state;
			this.parent=parent;
		}
		
		public ControlState getParent()
		{
			return parent;
		}
		
		public State getState()
		{
			return fromstate;
		}
		
		public boolean handleDirective(String verb, String line) throws IOException
		{
			if (parent!=null)
				return parent.handleDirective(verb, line);
			else
				return true;
		}
		
		public void handleLine(String line) throws IOException
		{
			if (parent!=null)
				parent.handleLine(line);
			else
				fifo.write(processDefines(line)+"\n");
		}
		
		public void close() throws IOException
		{
		}
	}
	
	private class BaseControl extends ControlState
	{
		public BaseControl(ControlState parent)
		{
			super(parent);
		}

		public boolean handleDirective(String verb, String line) throws IOException
		{
			if (verb.equals("define"))
			{
				Pattern pattern = Pattern.compile("^(\\S*)(?:\\s+(.*))?$");
				Matcher matcher = pattern.matcher(line);
				if (matcher.matches())
				{
					if (!defines.containsKey(matcher.group(1)))
					{
						String value=matcher.group(2);
						if (value==null)
							value="";
						defines.put(matcher.group(1),value);
						return true;
					}
				}
				return false;
			}
			else if (verb.equals("include"))
			{
				File include = null;
				if ((line.charAt(0)=='"')&&(line.charAt(line.length()-1)=='"'))
				{
					include = new File(fromstate.file,line.substring(1,line.length()-2));
				}
				else if ((line.charAt(0)=='<')&&(line.charAt(line.length()-1)=='>'))
				{
					include = searchIncludes(line.substring(1,line.length()-2));
				}
				if (include!=null)
				{
					pushState(include, new BufferedReader(new FileReader(include)));
					return true;
				}
				else
				{
					return false;
				}
			}
			else if (verb.equals("foobar"))
			{
				fifo.write("FOOBAR!!!\n");
				return true;
			}
			else if ((verb.equals("ifdef"))||(verb.equals("ifndef")))
			{
				IfControl newcontrol = new IfControl(control);
				newcontrol.initialise(verb,line);
				pushControl(newcontrol);
				return true;
			}
			else
			{
				return super.handleDirective(verb,line);
			}
		}
	}
	
	private class IfControl extends ControlState
	{
		private boolean displaying = true;
		private boolean displayed = false;
		
		public IfControl(ControlState parent)
		{
			super(parent);
		}

		public void initialise(String verb, String line)
		{
			if (verb.equals("ifdef"))
			{
				displaying=defines.containsKey(line);
			}
			else if (verb.equals("ifndef"))
			{
				displaying=!defines.containsKey(line);
			}
			displayed=displaying;
		}
		
		public boolean handleDirective(String verb, String line) throws IOException
		{
			if (verb.equals("else"))
			{
				displaying=!displayed;
				displayed=true;
				return true;
			}
			else if (verb.equals("elifdef"))
			{
				if (!displayed)
				{
					displaying=defines.containsKey(line);
					displayed=displaying;
				}
				else
				{
					displaying=false;
				}
				return true;
			}
			else if (verb.equals("elifndef"))
			{
				if (!displayed)
				{
					displaying=!defines.containsKey(line);
					displayed=displaying;
				}
				else
				{
					displaying=false;
				}
				return true;
			}
			else if (verb.equals("endif"))
			{
				popControl(this);
				return true;
			}
			else
			{
				return super.handleDirective(verb, line);
			}
		}
		
		public void handleLine(String line) throws IOException
		{
			if (displaying)
				super.handleLine(line);
		}
		
		public void close() throws IOException
		{
			throw new IOException("Unexpected end of if statement. Start was at "+state.file.getPath()+":"+startline);
		}
	}
		
	private State state = null;
	private Fifo fifo = new Fifo();
	private ControlState control = null;
	private Map defines = new HashMap();
	private ProcessorEnvironment environment;
	
	private boolean blankdirectives = false;
	
	protected PreProcessor(Reader reader)
	{
		pushState(null,new BufferedReader(reader));
		control = new BaseControl(null);
	}
	
	protected PreProcessor(File file) throws FileNotFoundException
	{
		BufferedReader in = new BufferedReader(new FileReader(file));
		pushState(file,in);
		control = new BaseControl(null);
	}
	
	public void setEnvironment(ProcessorEnvironment env)
	{
		environment=env;
	}

	private String processDefines(String text)
	{
		StringBuilder builder = new StringBuilder(text);
		boolean changed = false;
		do
		{
			changed=false;
			Iterator loop = defines.keySet().iterator();
			while (loop.hasNext())
			{
				String define = (String)loop.next();
				int pos = builder.indexOf(define);
				while (pos>=0)
				{
					changed=true;
					builder.replace(pos,pos+define.length(),(String)defines.get(define));
					pos=builder.indexOf(define);
				}
			}
		} while (changed);
		if (environment!=null)
			return environment.processLine(builder.toString());

		return builder.toString();
	}
	
	private File searchIncludes(String path)
	{
		if (environment!=null)
			return environment.getIncludedFile(path);

		return null;
	}
	
	private void pushState(File base, BufferedReader reader)
	{
		state = new State(state, base, reader);
	}
	
	private void popState() throws IOException
	{
		while ((control!=null)&&(control.getState()==state))
		{
			popControl(control);
		}
		state.close();
		state=state.getParent();
	}
	
	private boolean hasState()
	{
		return state!=null;
	}
	
	private void pushControl(ControlState control)
	{
		this.control = control;
	}
	
	private void popControl(ControlState control) throws IOException
	{
		while (this.control!=control)
		{
			this.control.close();
			this.control=this.control.getParent();
		}
		this.control = control.getParent();
	}
	
	private void parseMore() throws IOException
	{
		String line=null;
		while ((line==null)&&(hasState()))
		{
			line=state.readLine();
			if (line==null)
			{
				popState();
			}
		}
		if (line!=null)
		{
			Pattern pattern = Pattern.compile("^#(\\w+)\\s*(.*)?\\s*$");
			Matcher matcher = pattern.matcher(line);
			if (matcher.matches())
			{
				if (control.handleDirective(matcher.group(1),matcher.group(2)))
				{
					if (blankdirectives)
						fifo.write("\n");
				}
				else
					control.handleLine(line);
			}
			else
			{
				control.handleLine(line);
			}
		}
	}

	// Reader implementation
	
	public int read() throws IOException
	{
		while ((hasState())&&(fifo.getCount()<1))
			parseMore();

		return fifo.read();
	}

	public int read(char[] buffer) throws IOException
	{
		return read(buffer, 0, buffer.length);
	}
	
	public int read(char[] buffer, int offs, int length) throws IOException
	{
		while ((hasState())&&(fifo.getCount()<length))
			parseMore();

		return fifo.read(buffer,offs,length);
	}

	public void close() throws IOException
	{
		while (hasState())
		{
			popState();
		}
		fifo.clear();
	}
}
