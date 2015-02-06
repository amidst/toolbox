package eu.amidst.huginlink.io;

import COM.hugin.HAPI.ExceptionHugin;
import COM.hugin.HAPI.Class;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.huginlink.converters.DBNConverterToHugin;


/**
 * Created by Hanen on 16/01/15.
 */
public class DBNWriterToHugin {
    public static void saveToHuginFile(DynamicBayesianNetwork amidstDBN, String file) throws ExceptionHugin {
        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        huginDBN.saveAsNet(file);

    }
}
