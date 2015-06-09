package eu.amidst.dynamic.io;

import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by afa on 11/12/14.
 */
public class DynamicBayesianNetworkLoaderTest {

    @Before
    public void setUp() throws IOException, ClassNotFoundException {

    }

    @Test
    public void test() throws Exception {
        DynamicBayesianNetworkLoaderTest.loadAndTestFilesFromFolder("networks");
    }

    public static void loadAndTestFilesFromFolder(final String folderName) throws Exception {

        File folder = new File(folderName);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                loadAndTestFilesFromFolder(fileEntry.getName());
            } else {
                String fileName = fileEntry.getName();
                String fullFileName = folderName + "/" + fileName;

                if (fileName.endsWith(".dbn")) { //Dynamic BN

                    DynamicBayesianNetwork amidstDBN = DynamicBayesianNetworkLoader.loadFromFile(fullFileName);
                    DynamicBayesianNetworkWriter.saveToFile(amidstDBN, fullFileName);

                    DynamicBayesianNetwork amidstDBN2 = DynamicBayesianNetworkLoader.loadFromFile(fullFileName);

                    if (!amidstDBN.equalDBNs(amidstDBN2, 0.0))
                        throw new Exception("Dynamic Bayesian network loader for " + fileName + " failed. ");
                }
            }
        }
    }
}
