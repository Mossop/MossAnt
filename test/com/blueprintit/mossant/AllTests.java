package com.blueprintit.mossant;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite("Test for com.blueprintit.mossant");
		//$JUnit-BEGIN$
		suite.addTestSuite(FifoTest.class);
		//$JUnit-END$
		return suite;
	}

}
