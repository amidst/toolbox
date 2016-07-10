package eu.amidst.flinklink.examples;

import junit.framework.TestCase;
import org.junit.Test;

public class DynamicDataSetsTest extends TestCase  {
	@Test
	public void test() throws Exception {
		String[] args = {"0", "0", "100", "10", "3", "0"};
		DynamicDataSets.main(args);
	}
}