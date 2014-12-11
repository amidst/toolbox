package eu.amidst.core.models;

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.huginlink.ConverterToHugin;

import java.io.*;

/**
 * Created by afa on 11/12/14.
 */
public class BayesianNetworkWriter implements Serializable {

    public static void saveToHuginFile(BayesianNetwork amidstBN, String file) throws ExceptionHugin {
        Domain huginBN = ConverterToHugin.convertToHugin(amidstBN);
        huginBN.saveAsNet(file);
    }

    public static void saveSerializableObjectToFile(BayesianNetwork amidstBN, String file) throws IOException {
        FileOutputStream fout = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(amidstBN);
        oos.close();
    }
}
