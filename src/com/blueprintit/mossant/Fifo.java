package com.blueprintit.mossant;

import java.io.IOException;
import java.util.Arrays;

public class Fifo
{
	private int BUFFER_LENGTH = 1024;
	private int BUFFER_SPACE = 10;
	
	private Object[] buffers = new Object[0];
	private int buffercount = 0;
	private int count = 0;
	int startoffset = 0;
	int endoffset = 0;
	private int mincapacity = 0;
	private int minbuffers = 0;
	
	public Fifo()
	{
	}
	
	public Fifo(int group, int length)
	{
		BUFFER_LENGTH=length;
		BUFFER_SPACE=group;
	}
	
	void dump()
	{
		for (int i=0; i<buffers.length; i++)
		{
			if (buffers[i]!=null)
			{
				System.out.println("\""+String.copyValueOf((char[])buffers[i])+"\"");
			}
			else
			{
				System.out.println("<null>");
			}
		}
	}
	
	private char[] getBuffer(int offset)
	{
		return (char[])buffers[offset/BUFFER_LENGTH];
	}
	
	private int getBufferOffset(int offset)
	{
		return offset % BUFFER_LENGTH;
	}
	
	public int getCount()
	{
		return count;
	}
	
	public int getCapacity()
	{
		return BUFFER_LENGTH*buffercount;
	}
	
	public void setMinCapacity(int length)
	{
		minbuffers=((length-1)/BUFFER_LENGTH)+1;

		if (length>mincapacity)
		{
			mincapacity=length;
			setCapacity(length);
		}
		else
		{
			mincapacity=length;
			collectBuffers();
		}
	}

	private void setCapacity(int length)
	{
		length=Math.max(count,Math.max(length,mincapacity));
		int target = ((length-1)/BUFFER_LENGTH)+1;

		if (target>buffercount)
		{
			int spaces = ((target-1)/BUFFER_SPACE)+1;
			spaces*=BUFFER_SPACE;

			Object[] newbuffers = buffers;
			if (buffers.length<spaces)
			{
				newbuffers = new Object[spaces];
			}
			
			int start = buffercount;
			int number = target-buffercount;

			if (buffercount>0)
			{
				// Use the actual last character held for this section
				int endoff=(endoffset+getCapacity()-1)%getCapacity();
	
				if (endoff<startoffset)
				{
					int bottom = endoff/BUFFER_LENGTH;
					int top = startoffset/BUFFER_LENGTH;
					
					start=bottom+1;
					
					System.arraycopy(buffers,top,newbuffers,top+number,buffercount-top);
					startoffset+=number*BUFFER_LENGTH;
					
					if (bottom==top)
					{
						number--;
						if (buffers!=newbuffers)
							System.arraycopy(buffers,0,newbuffers,0,bottom-1);
						char[] newbuffer = new char[BUFFER_LENGTH];
						System.arraycopy(buffers[bottom],0,newbuffer,0,1+endoff%BUFFER_LENGTH);
						newbuffers[bottom]=newbuffer;
					}
					else if (buffers!=newbuffers)
					{
						System.arraycopy(buffers,0,newbuffers,0,bottom+1);
					}
				}
				else if (buffers!=newbuffers)
				{
					System.arraycopy(buffers,0,newbuffers,0,buffercount);
				}
			}
			
			buffers=newbuffers;
			buffercount=target;
			
			for (int i=0; i<number; i++)
				buffers[start+i] = new char[BUFFER_LENGTH];
			
			endoffset=(startoffset+count)%getCapacity();
		}
	}
	
	private void collectBuffers()
	{
		if (count==0)
		{
			startoffset=0;
			endoffset=0;
			if ((minbuffers+1)<buffercount)
				Arrays.fill(buffers,minbuffers+1,buffercount,null);
			buffercount=0;
			return;
		}
		int endoff=(endoffset+getCapacity()-1)%getCapacity();
		if (endoff<startoffset)
		{
			int top = startoffset/BUFFER_LENGTH;
			int bottom = endoff/BUFFER_LENGTH+1;
			int drop = top-bottom;
			drop=buffercount-Math.max(minbuffers,buffercount-drop);
			
			if (drop>0)
			{
				if ((drop)>=BUFFER_SPACE)
				{
					int buffcount = buffercount-drop;
					buffcount=(buffcount-1)/BUFFER_SPACE+1;
					Object[] newbuffers = new Object[buffcount*BUFFER_SPACE];
					System.arraycopy(buffers,0,newbuffers,0,bottom);
					System.arraycopy(buffers,bottom+drop,newbuffers,bottom,buffercount-(bottom+drop));
					buffers=newbuffers;
				}
				else
					System.arraycopy(buffers,bottom+drop,buffers,bottom,buffercount-(bottom+drop));
				buffercount-=drop;
				startoffset-=(drop*BUFFER_LENGTH);
			}
		}
		else
		{
			int bottom = Math.min(startoffset,endoff)/BUFFER_LENGTH;
			int top = (Math.max(startoffset,endoff)/BUFFER_LENGTH)+1;
			int diff = buffercount-Math.max(minbuffers,top-bottom);
			if (diff>0)
			{
				if (buffercount-top>=diff)
				{
					top=buffercount-diff;
					for (int i=top; i<buffercount; i++)
					{
						buffers[i]=null;
					}
					buffercount=top;
				}
				else
				{
					bottom=diff-(buffercount-top);
					int buffcount = top-bottom;
					int spaces = BUFFER_SPACE*((buffcount-1)/BUFFER_SPACE+1);
					Object[] newbuffers = buffers;
					if (buffers.length!=spaces)
						newbuffers = new Object[spaces];
					System.arraycopy(buffers,bottom,newbuffers,0,buffcount);
					buffers=newbuffers;
					for (int i=buffercount-diff; i<buffercount; i++)
						buffers[i]=null;
					buffercount-=diff;
					startoffset-=(BUFFER_LENGTH*bottom);
				}
			}
		}
	}
	
	public void clear()
	{
		this.count=0;
		collectBuffers();
	}
	
	// Write methods
	
	public void write(int c) throws IOException
	{
		setCapacity(getCount()+1);

		char[] buffer = getBuffer(endoffset);
		int offset = getBufferOffset(endoffset);
		buffer[offset]=(char)c;
		endoffset=(endoffset+1) % getCapacity();
		count++;
	}
	
	public void write(char[] buffer) throws IOException
	{
		write(buffer,0,buffer.length);
	}
	
	public void write(char[] buffer, int offset, int length) throws IOException
	{
		setCapacity(getCount()+length);
		
		int buffpos = endoffset / BUFFER_LENGTH;
		int buffoff = endoffset % BUFFER_LENGTH;
		int count=0;
		while (count<length)
		{
			char[] buff = (char[])buffers[buffpos];
			int cut = Math.min(length-count, BUFFER_LENGTH-buffoff);
			System.arraycopy(buffer,offset+count,buff,buffoff,cut);
			count+=cut;
			buffpos=(buffpos+1) % buffercount;
			buffoff=0;
		}
		endoffset=(endoffset+length)%getCapacity();
		this.count+=length;
	}

	public void write(String buffer) throws IOException
	{
		write(buffer.toCharArray());
	}
	
	public void write(String buffer, int offset, int length) throws IOException
	{
		write(buffer.toCharArray(), offset, length);
	}

	// Read methods
	
	public int read() throws IOException
	{
		if (count==0)
			return -1;
		
		char[] buffer = getBuffer(startoffset);
		int offset = getBufferOffset(startoffset);
		char result = buffer[offset];
		startoffset=(startoffset+1) % getCapacity();
		count--;
		collectBuffers();

		return (int)result;
	}
	
	public int read(char[] buffer) throws IOException
	{
		return read(buffer,0,buffer.length);
	}
	
	public int read(char[] buffer, int offset, int length) throws IOException
	{
		if (count==0)
			return -1;
		
		length=Math.min(length,getCount());
		
		int buffpos = startoffset / BUFFER_LENGTH;
		int buffoff = startoffset % BUFFER_LENGTH;
		int count=0;
		while (count<length)
		{
			char[] buff = (char[])buffers[buffpos];
			int cut = Math.min(length-count, BUFFER_LENGTH-buffoff);
			System.arraycopy(buff,buffoff,buffer,offset+count,cut);
			count+=cut;
			buffpos=(buffpos+1) % buffercount;
			buffoff=0;
		}
		startoffset=(startoffset+length)%getCapacity();
		this.count-=length;
		
		collectBuffers();
		return length;
	}
	
	public String read(int length) throws IOException
	{
		char[] buffer = new char[length];
		length = read(buffer);
		if (length<0)
			return null;
		return String.copyValueOf(buffer,0,length);
	}
}
