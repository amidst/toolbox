package eu.amidst.huginlink.converters;

import COM.hugin.HAPI.Class;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.corestatic.io.DynamicBayesianNetworkWriter;
import eu.amidst.corestatic.models.DynamicBayesianNetwork;
import eu.amidst.huginlink.io.DBNLoaderFromHugin;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by afa on 12/1/15.
 */
public class DBNConverterToAmidstTest {

    @Before
    public void setUp() throws ExceptionHugin, IOException {

        /*DynamicBayesianNetwork amidstDBN = DBNExample.getAmidst_DBN_Example();
        System.out.println("\nConverting the AMIDST Dynamic BN into Hugin format ...");
        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        //The name of the DBN must be the same as the name of the .net file !!!
        String nameModel = "huginDBNFromAMIDST";
        huginDBN.setName(nameModel);
        String outFile = new String("networks/"+nameModel+".oobn");
        huginDBN.saveAsNet(outFile);
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");

        System.out.println("\nConverting the Hugin Dynamic BN into AMIDST format ...");
        amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);

        System.out.println("\nConverting the AMIDST Dynamic BN into Hugin format ...");
        huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);

        nameModel = "huginDBNFromAMIDST2";
        //The name of the DBN must be the same as the name of the .net file !!!
        huginDBN.setName(nameModel);
        outFile = new String("networks/"+nameModel+".oobn");
        huginDBN.saveAsNet(outFile);
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");*/

        Class huginDBN = DBNLoaderFromHugin.loadFromFile("networks/CajamarDBN.oobn");
        DynamicBayesianNetwork amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);
        DynamicBayesianNetworkWriter.saveToFile(amidstDBN, "networks/CajamarDBN.dbn");

    }

    @Test
    public void testModels() throws ExceptionHugin {

    }
}