package com.blueprintit.mossant;

import java.io.IOException;

import junit.framework.TestCase;

public class FifoTest extends TestCase
{
	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.getCount()'
	 */
	public void testGetCount() throws IOException
	{
		Fifo fifo = new Fifo();
		assertEquals("Initial fifo shouldl be empty", 0, fifo.getCount());
		fifo.write("Hello");
		assertEquals("Written fifo should contain 5 characters", 5, fifo.getCount());
	}

	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.setMinCapacity(int)'
	 */
	public void testSetMinCapacity()
	{
		Fifo fifo = new Fifo();
		assertEquals("Initial fifo should have zero capacity", 0, fifo.getCapacity());
		fifo.setMinCapacity(5);
		assertEquals("Min capacity of 5 should give us that", 1024, fifo.getCapacity());
		fifo.setMinCapacity(2056);
		assertEquals("Min capacity of 2056 should give us that", 3072, fifo.getCapacity());
		fifo.setMinCapacity(5);
		assertEquals("Min capacity of 0 on an empty fifo should reduce it to nothing", 0, fifo.getCapacity());
	}

	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.clear()'
	 */
	public void testClear() throws IOException
	{
		Fifo fifo = new Fifo();
		fifo.write("Hello");
		fifo.clear();
		assertEquals("Cleared fifo should be empty", 0, fifo.getCount());
	}

	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.clear()'
	 */
	public void testWriteRead() throws IOException
	{
		Fifo fifo = new Fifo();
		fifo.write("Hello");
		assertEquals("Should read out what went in", "Hello", fifo.read(5));
		assertEquals("Fully read fifo should be empty", 0, fifo.getCount());
		assertEquals("Empty fifo should have no capacity", 0, fifo.getCapacity());
	}

	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.clear()'
	 */
	public void testWraparound() throws IOException
	{
		Fifo fifo = new Fifo(10,10);
		fifo.write("1234567890");
		assertEquals("Should be full", 10, fifo.getCapacity());
		assertEquals("Should be full", 10, fifo.getCount());
		assertEquals("Should return start of string ", "12345", fifo.read(5));
		assertEquals("Should contain 5 characters", 5, fifo.getCount());
		fifo.write("ABCDE");
		assertEquals("Should still be at capacity", 10, fifo.getCapacity());
		assertEquals("Should contain 10 characters", 10, fifo.getCount());
		assertEquals("Should contain the write text", "67890ABCDE", fifo.read(10));
	}

	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.clear()'
	 */
	public void testDynamicExtension() throws IOException
	{
		Fifo fifo = new Fifo(10,10);
		fifo.write("1234567890");
		fifo.read(5);
		assertEquals("Start offset", 5, fifo.startoffset);
		assertEquals("End offset", 0, fifo.endoffset);
		fifo.write("ABCDEFGHIJ");
		assertEquals("Should now be at capacity", 20, fifo.getCapacity());
		assertEquals("Should contain 10 characters", 15, fifo.getCount());
		assertEquals("Should contain the right text", "67890ABCDEFGHIJ", fifo.read(15));
	}

	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.clear()'
	 */
	public void testDynamicWrappedExtension() throws IOException
	{
		Fifo fifo = new Fifo(10,10);
		fifo.write("1234567890");
		fifo.read(5);
		fifo.write("ABC");
		fifo.write("DEFGHIJ");
		assertEquals("Capacity", 20, fifo.getCapacity());
		assertEquals("Count", 15, fifo.getCount());
		assertEquals("Result", "67890ABCDEFGHIJ", fifo.read(15));
	}

	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.clear()'
	 */
	public void testDynamicLargeWrappedExtension() throws IOException
	{
		Fifo fifo = new Fifo(10,10);
		fifo.write("1234567890ABCDEFGHIJ");
		fifo.read(5);
		fifo.write("ABC");
		fifo.read(10);
		assertEquals("Capacity", 20, fifo.getCapacity());
		assertEquals("Count", 8, fifo.getCount());
		fifo.write("1234567890");
		assertEquals("Result", "FGHIJABC1234567890", fifo.read(18));
	}

	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.clear()'
	 */
	public void testStripStart() throws IOException
	{
		Fifo fifo = new Fifo(10,10);
		fifo.write("1234567890ABCDEFGHIJ");
		fifo.read(11);
		fifo.dump();
		assertEquals("Count", 9, fifo.getCount());
		assertEquals("Capacity", 10, fifo.getCapacity());
		assertEquals("Result", "BCDEFGHIJ", fifo.read(9));
	}


	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.clear()'
	 */
	public void testStripMiddle() throws IOException
	{
		Fifo fifo = new Fifo(10,10);
		fifo.write("1234567890ABCDEFGHIJKLMNOPQRST");
		fifo.read(5);
		fifo.write("UVW");
		fifo.read(20);
		assertEquals("Count", 8, fifo.getCount());
		assertEquals("Capacity", 20, fifo.getCapacity());
		assertEquals("Result", "PQRSTUVW", fifo.read(8));
	}

	/*
	 * Test method for 'com.blueprintit.mossant.Fifo.clear()'
	 */
	public void testStripEnd() throws IOException
	{
		Fifo fifo = new Fifo(10,10);
		fifo.write("1234567890ABCDEFGHIJ");
		fifo.read(8);
		fifo.write("KLMNO");
		fifo.read(13);
		assertEquals("Capacity", 10, fifo.getCapacity());
		assertEquals("Count", 4, fifo.getCount());
		assertEquals("Result", "LMNO", fifo.read(4));
	}
}
