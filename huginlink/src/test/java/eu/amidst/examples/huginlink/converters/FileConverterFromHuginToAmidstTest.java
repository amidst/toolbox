package eu.amidst.examples.huginlink.converters;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by afa on 5/2/15.
 */
public class FileConverterFromHuginToAmidstTest {

    @Before
    public void setUp() throws Exception {
        //To make the test easier the check is done in the original class.
        FileConverterFromHuginToAmidst.convertFilesFromFolder("networks");
    }

    @Test
    public void testModels()  {

    }
}
