package eu.amidst.core.io;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by afa on 5/2/15.
 */
public class BayesianNetworkWriterTest {

    @Before
    public void setUp() throws IOException, ClassNotFoundException {

    }

    @Test
    public void test() throws Exception {
        //This class is already tested when using the BayesianNetworkLoaderTest
        BayesianNetworkLoaderTest.loadAndTestFilesFromFolder("networks");
    }
}
