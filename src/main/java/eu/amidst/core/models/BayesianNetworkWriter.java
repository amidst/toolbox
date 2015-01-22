package eu.amidst.core.models;

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.huginlink.ConverterToHugin;

import java.io.*;

/**
 * Created by afa on 11/12/14.
 */
public class BayesianNetworkWriter {

    //TODO Move this method to the module huginLink
    public static void saveToHuginFile(BayesianNetwork amidstBN, String file) throws ExceptionHugin {
        Domain huginBN = ConverterToHugin.convertToHugin(amidstBN);
        huginBN.saveAsNet(file);
    }

    public static void saveToFile (BayesianNetwork bn, String fileName) throws IOException {

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
        out.writeObject(bn);
        out.close();
    }
}
