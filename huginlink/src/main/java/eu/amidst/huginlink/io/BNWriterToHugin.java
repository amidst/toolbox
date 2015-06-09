package eu.amidst.huginlink.io;

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.huginlink.converters.BNConverterToHugin;

/**
 * This class is a writer to create Hugin Bayesian network files from AMIDST Bayesian networks.
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 11/12/14
 */
public class BNWriterToHugin {

    /**
     * Creates a Hugin Bayesian network file from a AMIDST Bayesian network model.
     * @param amidstBN the <code>BayesianNetwork</code> in AMIDST format.
     * @param file the name of the Hugin file in which the model is saved.
     * @throws ExceptionHugin
     */
    public static void saveToHuginFile(BayesianNetwork amidstBN, String file) throws ExceptionHugin {
        Domain huginBN = BNConverterToHugin.convertToHugin(amidstBN);
        huginBN.saveAsNet(file);
    }
}
