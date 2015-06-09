package eu.amidst.dynamic.io;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by afa on 5/2/15.
 */
public class DynamicBayesianNetworkWriterTest {

    @Before
    public void setUp() throws IOException, ClassNotFoundException {

    }

    @Test
    public void test() throws Exception {
        //This class is already tested when using the DynamicBayesianNetworkLoaderTest
        DynamicBayesianNetworkLoaderTest.loadAndTestFilesFromFolder("networks");
    }


}
