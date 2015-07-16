package eu.amidst.huginlink.io;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;

/**
 * This class is a loader of dynamic Bayesian networks in AMIDST format from Hugin files.
 */
public class DBNLoaderFromHugin {

    /**
     * Loads an AMIDST dynamic Bayesian network from a Hugin file by converting the format internally.
     * In order to simplify, we assume that the Hugin DBN model name is the file name without extension.
     * @param file the file name in which the Hugin model is stored.
     * @return a {@link eu.amidst.core.models.BayesianNetwork} in AMIDST format.
     * @throws ExceptionHugin
     */
    public static Class loadFromFile(String file) throws ExceptionHugin {

        DefaultClassParseListener parseListener = new DefaultClassParseListener();
        ClassCollection cc = new ClassCollection();
        cc.parseClasses (file, parseListener);

        //Get the model name from the file name
        String[] aux = file.split("/");
        String fileName = aux[aux.length-1];
        String modelName = fileName.substring(0,fileName.length()-5);

        Class huginDBN = cc.getClassByName(modelName);
        return huginDBN;
    }
}
