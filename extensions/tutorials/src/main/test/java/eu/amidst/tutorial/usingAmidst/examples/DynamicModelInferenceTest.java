package eu.amidst.tutorial.usingAmidst.examples;

import eu.amidst.tutorials.usingAmidst.examples.*;
import junit.framework.TestCase;
import org.junit.Test;


/**
 * Created by rcabanas on 20/05/16.
 */


public class DynamicModelInferenceTest extends TestCase {

    @Test
    public void test() throws Exception {

        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));

        DynamicModelInference.main(null);
    }

}
