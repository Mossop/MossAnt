package com.blueprintit.mossant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

public class PreProcessorTest extends TestCase
{
	public void testFileRead() throws IOException
	{
		StringBuilder source = new StringBuilder();
		source.append("test1\n");
		source.append("test2\n");
		source.append("test3\n");
		BufferedReader reader = new BufferedReader(new PreProcessor(new File("C:\\test.txt")));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (line!=null)
		{
			builder.append(line);
			builder.append("\n");
			line=reader.readLine();
		}
		assertEquals("Check text", source.toString(), builder.toString());
	}

	public void testReader() throws IOException
	{
		StringBuilder source = new StringBuilder();
		source.append("test1\n");
		source.append("test2\n");
		source.append("test3\n");
		StringReader sourcereader = new StringReader(source.toString());
		BufferedReader reader = new BufferedReader(new PreProcessor(sourcereader));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (line!=null)
		{
			builder.append(line);
			builder.append("\n");
			line=reader.readLine();
		}
		assertEquals("Check text", source.toString(), builder.toString());
	}

	public void testFoobar() throws IOException
	{
		StringBuilder source = new StringBuilder();
		source.append("test1\n");
		source.append("#foobar\n");
		source.append("test3\n");

		StringBuilder target = new StringBuilder();
		target.append("test1\n");
		target.append("FOOBAR!!!\n");
		target.append("test3\n");

		StringReader sourcereader = new StringReader(source.toString());
		BufferedReader reader = new BufferedReader(new PreProcessor(sourcereader));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (line!=null)
		{
			builder.append(line);
			builder.append("\n");
			line=reader.readLine();
		}
		assertEquals("Check text", target.toString(), builder.toString());
	}

	public void testBadDirective() throws IOException
	{
		StringBuilder source = new StringBuilder();
		source.append("test1\n");
		source.append("#test2\n");
		source.append("test3\n");

		StringBuilder target = new StringBuilder();
		target.append("test1\n");
		target.append("test3\n");

		StringReader sourcereader = new StringReader(source.toString());
		BufferedReader reader = new BufferedReader(new PreProcessor(sourcereader));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (line!=null)
		{
			builder.append(line);
			builder.append("\n");
			line=reader.readLine();
		}
		assertEquals("Check text", target.toString(), builder.toString());
	}

	public void testIfDefDirective() throws IOException
	{
		StringBuilder source = new StringBuilder();
		source.append("test1\n");
		source.append("#ifdef\n");
		source.append("test2\n");
		source.append("#else\n");
		source.append("test3\n");
		source.append("#endif\n");

		StringBuilder target = new StringBuilder();
		target.append("test1\n");
		target.append("test2\n");

		StringReader sourcereader = new StringReader(source.toString());
		BufferedReader reader = new BufferedReader(new PreProcessor(sourcereader));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (line!=null)
		{
			builder.append(line);
			builder.append("\n");
			line=reader.readLine();
		}
		assertEquals("Check text", target.toString(), builder.toString());
	}

	public void testIfNDefDirective() throws IOException
	{
		StringBuilder source = new StringBuilder();
		source.append("test1\n");
		source.append("#ifndef\n");
		source.append("test2\n");
		source.append("#else\n");
		source.append("test3\n");
		source.append("#endif\n");

		StringBuilder target = new StringBuilder();
		target.append("test1\n");
		target.append("test3\n");

		StringReader sourcereader = new StringReader(source.toString());
		BufferedReader reader = new BufferedReader(new PreProcessor(sourcereader));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (line!=null)
		{
			builder.append(line);
			builder.append("\n");
			line=reader.readLine();
		}
		assertEquals("Check text", target.toString(), builder.toString());
	}

	public void testNestedIfDirective() throws IOException
	{
		StringBuilder source = new StringBuilder();
		source.append("test1\n");
		source.append("#ifdef\n");
		source.append("#ifndef\n");
		source.append("test2\n");
		source.append("#else\n");
		source.append("test3\n");
		source.append("#endif\n");
		source.append("#else\n");
		source.append("test4\n");
		source.append("#endif\n");

		StringBuilder target = new StringBuilder();
		target.append("test1\n");
		target.append("test3\n");

		StringReader sourcereader = new StringReader(source.toString());
		BufferedReader reader = new BufferedReader(new PreProcessor(sourcereader));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (line!=null)
		{
			builder.append(line);
			builder.append("\n");
			line=reader.readLine();
		}
		assertEquals("Check text", target.toString(), builder.toString());
	}
}
