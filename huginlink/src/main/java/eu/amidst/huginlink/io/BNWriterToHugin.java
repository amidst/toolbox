package eu.amidst.huginlink.io;

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.converters.BNConverterToHugin;

/**
 * This class is a writer to create Hugin Bayesian network files from AMIDST Bayesian networks.
 */
public class BNWriterToHugin {

    /**
     * Creates a Hugin Bayesian network file from a AMIDST Bayesian network model.
     * @param amidstBN the {@link eu.amidst.core.models.BayesianNetwork} in AMIDST format.
     * @param file the name of the Hugin file in which the model will be saved.
     * @throws ExceptionHugin
     */
    public static void saveToHuginFile(BayesianNetwork amidstBN, String file) throws ExceptionHugin {
        Domain huginBN = BNConverterToHugin.convertToHugin(amidstBN);
        huginBN.saveAsNet(file);
    }
}
