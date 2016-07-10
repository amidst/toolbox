package eu.amidst.flinklink.examples.reviewMeeting2015;

import junit.framework.TestCase;
import org.junit.Test;

public class GenerateDataTest extends TestCase  {
	@Test
	public void test() throws Exception {


		String args[] = {"3","2000", "5", "false"};


		GenerateData.main(args);
	}
}