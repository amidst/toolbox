package eu.amidst.core.examples.huginlink;

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.converters.BNConverterToHugin;
import eu.amidst.huginlink.io.BNLoaderFromHugin;

/**
 * Created by rcabanas on 24/06/16.
 */
public class HuginConversionExample {
    public static void main(String[] args) throws ExceptionHugin {
        //We load from Hugin format
        Domain huginBN = BNLoaderFromHugin.loadFromFile("./networks/simulated/WasteIncinerator.bn");

        //Then, it is converted to AMIDST BayesianNetwork object
        BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);

        //Then, it is converted to Hugin Bayesian Network object
        huginBN = BNConverterToHugin.convertToHugin(amidstBN);

        System.out.println(amidstBN.toString());
        System.out.println(huginBN.toString());

    }
}
