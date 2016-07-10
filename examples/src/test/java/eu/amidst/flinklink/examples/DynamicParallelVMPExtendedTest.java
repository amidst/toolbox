package eu.amidst.flinklink.examples;

import junit.framework.TestCase;
import org.junit.Test;

public class DynamicParallelVMPExtendedTest extends TestCase  {
	@Test
	public void test() throws Exception {
		String[] args = {"0", "10", "100", "100", "100", "100", "3", "0", "true"};
		DynamicParallelVMPExtended.main(args);
	}
}