package eu.amidst.flinklink.examples.reviewMeeting2015;

import junit.framework.TestCase;
import org.junit.Test;

public class ConceptDriftDetectorTest extends TestCase  {
	@Test
	public void test() throws Exception {

		String args[] = {"3","500", "5", "false"};
		GenerateData.main(args);

		String args2[] = {"2"};
		ConceptDriftDetector.main(args2);
	}
}