package eu.amidst.flinklink.examples;

import junit.framework.TestCase;
import org.junit.Test;

public class ParallelVMPTest extends TestCase  {
	@Test
	public void test() throws Exception {

		String args[] = {"./networks/dataWeka/asia.bn", "1000" ,"./datasets/simulated/sampledAsia.arff", "100"};
		ParallelVMP.main(args);
	}
}