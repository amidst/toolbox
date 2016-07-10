package eu.amidst.flinklink.examples.cim2015;

import junit.framework.TestCase;
import org.junit.Test;

public class ExperimentsParallelMLTest extends TestCase  {
	@Test
	public void test() throws Exception {

		String args[] =
				{"-sampleSize", "100",
						"-numStates", "3",
						"-GV", "5",
						"-DV", "5",
						"-SPGV", "2",
						"-SPDV", "10",
						"-sampleData", "true",
						"-parallelMode", "true",
						"-windowsSize", "10"};

		ExperimentsParallelML.main(args);
	}
}