package eu.amidst.huginlink.io;

import COM.hugin.HAPI.DefaultClassParseListener;
import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import COM.hugin.HAPI.ParseListener;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;

/**
 * This class is a loader to create AMIDST Bayesian networks from Hugin Bayesian network files.
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 11/12/14
 */
public class BNLoaderFromHugin {

    /**
     * Creates an AMIDST Bayesian network model from a Hugin Bayesian network stored in a file.
     * @param file the name of the file with the Hugin model.
     * @return a <code>BayesianNetwork</code> in AMIDST format.
     * @throws ExceptionHugin
     */
    public static Domain loadFromFile(String file) throws ExceptionHugin {
        ParseListener parseListener = new DefaultClassParseListener();
        Domain huginBN = new Domain (file, parseListener);
        //BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);
        return huginBN;
    }
}

