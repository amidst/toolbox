package eu.amidst.core.examples.huginlink;

import junit.framework.TestCase;
import org.junit.Test;

public class HuginConversionExampleTest extends TestCase  {
	@Test
	public void test() throws Exception {

		try {
			HuginConversionExample.main(null);
		}catch (UnsatisfiedLinkError err) {

		}catch (NoClassDefFoundError ex) {

		}
	}
}