package eu.amidst.cim2015.examples;

import junit.framework.TestCase;
import org.junit.Test;

public class ExperimentsParallelMLTest extends TestCase  {
	@Test
	public void test() throws Exception {

		String args[] = {"-pathToFile", "./datasets/simulated/syntheticData.arff", "-coreComparison", "false"};
		//ExperimentsParallelML.main(args);
        ExperimentsParallelML.compareBatchSizes();
	}
}