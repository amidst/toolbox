package eu.amidst.core.models;

import eu.amidst.examples.BNExample;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by afa on 11/12/14.
 */
public class BayesianNetworkLoaderTest {

    //TODO Implement a test comparing all the elements of the BayesianNetwork before saving the file and after loading it
    @Before
    public void setUp() throws IOException, ClassNotFoundException {

        BayesianNetwork bn1 = BNExample.getAmidst_BN_Example();

        System.out.println("------------  BN model before saving the object  --------------");
        System.out.println(bn1.toString());
        BayesianNetworkWriter.saveToFile(bn1, "networks/bn.bn");

        System.out.println("------------  BN model loaded from the file -------------------");
        BayesianNetwork bn2 = BayesianNetworkLoader.loadFromFile("networks/bn.bn");
        System.out.println(bn2.toString());


        //TODO move this piece of code to the module huginLink
        //String file = new String("networks/huginNetworkFromAMIDST.net");
        //BayesianNetwork amidstBN = BayesianNetworkLoader.loadFromHugin(file);
        //System.out.println("\nAMIDST network loaded from Hugin file.");
        //System.out.println(amidstBN.getDAG().toString());
        //System.out.println(amidstBN.toString());
        //String file2 = new String("networks/huginNetworkFromAMIDST2.net");
        //BayesianNetworkWriter.saveToHuginFile(amidstBN,file2);
        //System.out.println("\nAMIDST network save to Hugin file.");

    }

    @Test
    public void test(){

    }

}
