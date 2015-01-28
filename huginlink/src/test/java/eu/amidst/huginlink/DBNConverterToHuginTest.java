package eu.amidst.huginlink;

import COM.hugin.HAPI.Class;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.examples.DBNExample;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by afa on 8/1/15.
 */
public class DBNConverterToHuginTest {


    @Before
    public void setUp() throws ExceptionHugin {

        DynamicBayesianNetwork amidstDBN = DBNExample.getAmidst_DBN_Example();
        System.out.println("\n\nConverting the AMIDST Dynamic BN into Hugin format ...");
        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        String outFile = new String("networks/huginDBNFromAMIDST.net");

        //The name of the DBN must be the same as the name of the out file !!!
        huginDBN.setName("huginDBNFromAMIDST");

        huginDBN.saveAsNet(outFile);
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");
        //--------------------------------------------------------------------------------------------------------------
    }

    @Test
    public void testModels() throws ExceptionHugin {

    }
}