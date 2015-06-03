package eu.amidst.examples.huginlink.io;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;

/**
 * This class is a loader of dynamic Bayesian networks in AMIDST format from Hugin files.
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 5/2/15
 */
public class DBNLoaderFromHugin {

    /**
     * Loads a AMIDST dynamic Bayesian network from a Hugin file by converting the format internally. In order to make
     * it simpler, we suppose that the Hugin DBN model name is the file name without extension.
     *
     * @param file the file name in which the Hugin model is stored.
     * @return a <code>DynamicBayesianNetwork</code> in AMIDST format.
     * @throws ExceptionHugin
     */
    public static Class loadFromFile(String file) throws ExceptionHugin {

        DefaultClassParseListener parseListener = new DefaultClassParseListener();
        ClassCollection cc = new ClassCollection();
        cc.parseClasses (file, parseListener);

        //Gets the model name from the file name
        String[] aux = file.split("/");
        String fileName = aux[aux.length-1];
        String modelName = fileName.substring(0,fileName.length()-5);

        Class huginDBN = cc.getClassByName(modelName);
        //DynamicBayesianNetwork amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);
        return huginDBN;
    }
}
