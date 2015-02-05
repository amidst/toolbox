package eu.amidst.huginlink;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.models.DynamicBayesianNetwork;

/**
 * Created by afa on 5/2/15.
 */
public class DBNLoaderFromHugin {

    // In order to make it simpler, we suppose that the DBN model name is the file name without extension
    public static DynamicBayesianNetwork loadFromFile(String file) throws ExceptionHugin {

        DefaultClassParseListener parseListener = new DefaultClassParseListener();
        ClassCollection cc = new ClassCollection();
        cc.parseClasses (file, parseListener);

        //Gets the model name from the file name
        String[] aux = file.split("/");
        String fileName = aux[aux.length-1];
        String modelName = fileName.substring(0,fileName.length()-5);

        Class huginDBN = cc.getClassByName(modelName);
        DynamicBayesianNetwork amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);
        return amidstDBN;
    }
}
