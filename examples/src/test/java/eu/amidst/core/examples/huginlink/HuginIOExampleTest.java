package eu.amidst.core.examples.huginlink;

import junit.framework.TestCase;
import org.junit.Test;

public class HuginIOExampleTest extends TestCase  {
	@Test
	public void test() throws Exception {

		try {
			HuginIOExample.main(null);
		}catch (UnsatisfiedLinkError err) {

		}catch (NoClassDefFoundError ex) {

		}
	}
}