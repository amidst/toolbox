package eu.amidst.flinklink.examples;

import junit.framework.TestCase;
import org.junit.Test;

public class GenerateRandomTest extends TestCase  {
	@Test
	public void test() throws Exception {
		String args[] = {"-sampleSize", "100", "-DiscV", "11", "-numStates", "5", "-GausV", "10"};
		GenerateRandom.main(args);
	}
}



