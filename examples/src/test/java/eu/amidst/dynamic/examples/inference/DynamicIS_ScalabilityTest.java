package eu.amidst.dynamic.examples.inference;

import junit.framework.TestCase;
import org.junit.Test;

public class DynamicIS_ScalabilityTest extends TestCase  {
	@Test
	public void test() throws Exception {
		//TODO: add arguments to main
		String args[] =     {"-d", "30", "-c", "0", "-h", "10", "-s", "2", "-l", "50", "-q", "2", "-linkNodes", "false", "-activateMiddleLayer", "true", "-seed", "1", "-samples", "100" };

		DynamicIS_Scalability.main(args);
	}
}