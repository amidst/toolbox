package eu.amidst.flinklink.examples;

import junit.framework.TestCase;
import org.junit.Test;

public class GenerateDataTest extends TestCase  {
	@Test
	public void test() throws Exception {

		String args[] = {"networks/simulated/WasteIncinerator.bn","100"};

		GenerateData.main(args);
	}
}