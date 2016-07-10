package eu.amidst.flinklink.examples;

import junit.framework.TestCase;
import org.junit.Test;

public class ParallelVMPExtendedTest extends TestCase  {
	@Test
	public void test() throws Exception {

		String args[] = {"5", "5", "1000", "100", "10", "100", "1234"};
		ParallelVMPExtended.main(args);
	}
}