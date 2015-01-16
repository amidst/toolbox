package eu.amidst.core.models;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.huginlink.ConverterToHugin;
import eu.amidst.core.huginlink.DBNConverterToAmidst;
import eu.amidst.core.huginlink.DBNConverterToHugin;

/**
 * Created by Hanen on 16/01/15.
 */
public class DynamicBayesianNetworkWriter {
    //TODO Antonio please check this!
    public static void saveToHuginFile(DynamicBayesianNetwork amidstDBN, String file) throws ExceptionHugin {
        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        huginDBN.saveAsNet(file);

    }
}
