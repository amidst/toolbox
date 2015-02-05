package eu.amidst.huginlink;

import COM.hugin.HAPI.DefaultClassParseListener;
import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import COM.hugin.HAPI.ParseListener;
import eu.amidst.core.models.BayesianNetwork;


/**
 * Created by afa on 11/12/14.
 */
public class BNLoaderFromHugin {

    public static BayesianNetwork loadFromFile(String file) throws ExceptionHugin {

        ParseListener parseListener = new DefaultClassParseListener();
        Domain huginBN = new Domain (file, parseListener);
        BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);
        return amidstBN;
    }
}

