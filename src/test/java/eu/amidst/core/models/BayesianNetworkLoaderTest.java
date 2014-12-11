package eu.amidst.core.models;

import COM.hugin.HAPI.ExceptionHugin;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by afa on 11/12/14.
 */
public class BayesianNetworkLoaderTest {

    @Before
    public void setUp() throws ExceptionHugin {

        String file = new String("networks/huginNetworkFromAMIDST.net");
        BayesianNetwork amidstBN = BayesianNetworkLoader.loadFromHugin(file);
        System.out.println("\nAMIDST network loaded from Hugin file.");

        String file2 = new String("networks/huginNetworkFromAMIDST2.net");
        BayesianNetworkWriter.saveToHuginFile(amidstBN,file2);
        System.out.println("\nAMIDST network save to Hugin file.");

    }

    @Test
    public void test(){

    }

}
