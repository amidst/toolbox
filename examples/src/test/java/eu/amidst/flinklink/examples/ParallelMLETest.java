package eu.amidst.flinklink.examples;

import junit.framework.TestCase;
import org.junit.Test;

public class ParallelMLETest extends TestCase  {
	@Test
	public void test() throws Exception {

		String args[] = {"./networks/simulated/WasteIncinerator.bn", "1000"};
		ParallelMLE.main(args);
	}
}