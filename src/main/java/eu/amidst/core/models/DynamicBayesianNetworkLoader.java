package eu.amidst.core.models;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.huginlink.DBNConverterToAmidst;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Created by Hanen on 15/01/15.
 */
public class DynamicBayesianNetworkLoader {

    //TODO Move this method to the module huginLink

    public static DynamicBayesianNetwork loadDBNFromHugin(String file) throws ExceptionHugin {

            Class huginDBN = new Class(new ClassCollection());
            DynamicBayesianNetwork amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);
            return amidstDBN;
        }

    public static DynamicBayesianNetwork loadFromFile (String fileName) throws ClassNotFoundException, IOException {

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
        Object obj = ois.readObject();
        ois.close();
        return (DynamicBayesianNetwork)obj;
    }

}
