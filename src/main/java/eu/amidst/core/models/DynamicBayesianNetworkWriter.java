package eu.amidst.core.models;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.huginlink.DBNConverterToHugin;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Created by Hanen on 16/01/15.
 */
public class DynamicBayesianNetworkWriter {

    //TODO Move this method to the module huginLink
    public static void saveToHuginFile(DynamicBayesianNetwork amidstDBN, String file) throws ExceptionHugin {
        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        huginDBN.saveAsNet(file);

    }

    public static void saveToFile (DynamicBayesianNetwork bn, String fileName) throws IOException {

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
        out.writeObject(bn);
        out.close();
    }
}
