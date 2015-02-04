package eu.amidst.huginlink;

import COM.hugin.HAPI.DefaultClassParseListener;
import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import COM.hugin.HAPI.ParseListener;
import eu.amidst.core.models.BayesianNetwork;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by afa on 4/2/15.
 */
public class FileConverterFromHuginToAmidstTest {

    @Before
    public void setUp() throws ExceptionHugin, IOException, ClassNotFoundException {
        final File folder = new File("./networks");
        FileConverterFromHuginToAmidst.convertFilesFromFolder(folder);
 }

    @Test
    public void test() throws ExceptionHugin {

    }

}
