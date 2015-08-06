package eu.amidst.core.io;

import eu.amidst.core.models.BayesianNetwork;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by afa on 11/12/14.
 */
public class BayesianNetworkLoaderTest {

    @Before
    public void setUp() throws IOException, ClassNotFoundException {

    }

    @Test
    public void test() throws Exception {
        BayesianNetworkLoaderTest.loadAndTestFilesFromFolder("networks");
    }

    public static void loadAndTestFilesFromFolder(final String folderName) throws Exception {

        File folder = new File(folderName);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                loadAndTestFilesFromFolder(fileEntry.getName());
            } else {
                String fileName = fileEntry.getName();
                String fullFileName = folderName + "/" + fileName;

                if (fileName.endsWith(".bn")) { //Static BN

                    System.out.println("Reading file: "+fileName);

                    BayesianNetwork amidstBN = BayesianNetworkLoader.loadFromFile(fullFileName);
                    BayesianNetworkWriter.saveToFile(amidstBN, fullFileName);

                    BayesianNetwork amidstBN2 = BayesianNetworkLoader.loadFromFile(fullFileName);

                    if (!amidstBN.equalBNs(amidstBN2, 0.0))
                        throw new Exception("Bayesian network loader for " + fileName + " failed. ");
                }
            }
        }
    }
}
