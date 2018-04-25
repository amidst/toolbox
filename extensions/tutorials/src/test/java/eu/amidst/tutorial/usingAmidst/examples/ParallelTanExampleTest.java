package eu.amidst.tutorial.usingAmidst.examples;

import junit.framework.TestCase;
import org.junit.Test;


/**
 * Created by rcabanas on 20/05/16.
 */


public class ParallelTanExampleTest extends TestCase {

    @Test
    public void test() throws Exception {

        try {
            ParallelTANExample.main(null);
        }catch (UnsatisfiedLinkError error) {
            //This error is due to the missing hugin lib and so it is ignored
            error.printStackTrace();
        }
    }

}
