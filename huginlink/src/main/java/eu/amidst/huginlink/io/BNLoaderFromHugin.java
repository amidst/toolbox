package eu.amidst.huginlink.io;

import COM.hugin.HAPI.DefaultClassParseListener;
import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import COM.hugin.HAPI.ParseListener;

/**
 * This class is a loader to create AMIDST Bayesian networks from Hugin Bayesian network files.
 */
public class BNLoaderFromHugin {

    /**
     * Creates an AMIDST Bayesian network model from a Hugin Bayesian network stored in a file.
     * @param file the name of the file including the Hugin model.
     * @return a {@link eu.amidst.core.models.BayesianNetwork} in AMIDST format.
     * @throws ExceptionHugin
     */
    public static Domain loadFromFile(String file) throws ExceptionHugin {
        ParseListener parseListener = new DefaultClassParseListener();
        Domain huginBN = new Domain (file, parseListener);
        return huginBN;
    }
}

