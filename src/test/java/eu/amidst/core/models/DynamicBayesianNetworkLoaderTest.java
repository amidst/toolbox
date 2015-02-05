package eu.amidst.core.models;

import eu.amidst.examples.BNExample;
import eu.amidst.examples.DBNExample;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by afa on 11/12/14.
 */
public class DynamicBayesianNetworkLoaderTest {

    //TODO Implement a test comparing all the elements of the BayesianNetwork before saving the file and after loading it
    @Before
    public void setUp() throws IOException, ClassNotFoundException {

        DynamicBayesianNetwork dbn1 = DBNExample.getAmidst_DBN_Example();

        System.out.println("------------  DBN model before saving the object  --------------");
        System.out.println(dbn1.toString());
        DynamicBayesianNetworkWriter.saveToFile(dbn1, "networks/dbn.dbn");

        System.out.println("------------  DBN model loaded from the file -------------------");
        DynamicBayesianNetwork dbn2 = DynamicBayesianNetworkLoader.loadFromFile("networks/dbn.dbn");
        System.out.println(dbn2.toString());

    }

    @Test
    public void test(){

    }

}
