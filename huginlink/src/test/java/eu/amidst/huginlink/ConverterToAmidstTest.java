package eu.amidst.huginlink;


import COM.hugin.HAPI.DefaultClassParseListener;
import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import COM.hugin.HAPI.ParseListener;
import eu.amidst.core.models.BayesianNetwork;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by afa on 19/11/14.
 */
public class ConverterToAmidstTest {

    @Before
    public void setUp() throws ExceptionHugin {

        ParseListener parseListener = new DefaultClassParseListener();
        Domain huginBN = new Domain ("networks/huginNetworkFromAMIDST.net", parseListener);
        System.out.println("\n\nConverting the Hugin network into AMIDST format ...");
        BayesianNetwork amidstBN = ConverterToAMIDST.convertToAmidst(huginBN);
        System.out.println("\nAMIDST network object created.");
    }

    @Test
    public void test() throws ExceptionHugin {


    }


}
