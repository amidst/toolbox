package eu.amidst.dynamic.examples.inference;

import junit.framework.TestCase;
import org.junit.Test;

public class DynamicHugin_FactoredFrontierTest extends TestCase  {
	@Test
	public void test() throws Exception {
		try {
			DynamicHugin_FactoredFrontier.main(null);
		}catch (NoClassDefFoundError ex) {

		}catch (UnsatisfiedLinkError err) {

		}
	}
}