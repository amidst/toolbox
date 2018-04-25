package eu.amidst.core.examples.huginlink;

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.io.BNLoaderFromHugin;
import eu.amidst.huginlink.io.BayesianNetworkWriterToHugin;

/**
 * Created by rcabanas on 24/06/16.
 */
public class HuginIOExample {
    public static void main(String[] args) throws ExceptionHugin {
        //We load from Hugin format
        Domain huginBN = BNLoaderFromHugin.loadFromFile("networks/asia.net");

        //We save a AMIDST BN to Hugin format
        BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);
        BayesianNetworkWriterToHugin.save(amidstBN,"networks/tmp.net");

    }
}
