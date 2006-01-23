package com.blueprintit.mossant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
		private State fromstate;
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
				return false;
		}
		
		public void handleLine(String line) throws IOException
		{
			if (parent!=null)
				parent.handleLine(line);
			else
				fifo.write(line);
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
			if (line.equals("define"))
			{
				return true;
			}
			else if (line.equals("include"))
			{
				return true;
			}
			else if ((line.equals("ifdef"))||(line.equals("ifndef")))
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
		
		public IfControl(ControlState parent)
		{
			super(parent);
		}

		public void initialise(String verb, String line)
		{
		}
		
		public boolean handleDirective(String verb, String line) throws IOException
		{
			if (verb.equals("else"))
			{
				displaying=!displaying;
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

	private void pushState(File base, BufferedReader reader)
	{
		state = new State(state, base, reader);
	}
	
	private void popState() throws IOException
	{
		while (control.getState()==state)
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
				state.close();
				while (control.fromstate==state)
				{
					control.close();
					popControl(control);
				}
				popState();
			}
		}
		if (line!=null)
		{
			Pattern pattern = Pattern.compile("^#(\\W+)(?:\\w*(.*))?$");
			Matcher matcher = pattern.matcher(line);
			if (matcher.matches())
			{
				if (!control.handleDirective(matcher.group(1),matcher.group(2)))
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
		while (hasState())
		{
			if (fifo.getCount()<1)
				parseMore();
		}
		return fifo.read();
	}

	public int read(char[] buffer) throws IOException
	{
		return read(buffer, 0, buffer.length);
	}
	
	public int read(char[] buffer, int offs, int length) throws IOException
	{
		while (hasState())
		{
			if (fifo.getCount()<length)
				parseMore();
		}
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
