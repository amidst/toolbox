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

        //The network name must be the same as the file name (without .dbn)
        String[] parts = file.split("/");
        String fileName = parts[parts.length-1];
        String networkName = fileName.substring(0,fileName.length()-5);


        System.out.println(networkName);
        huginDBN.setName(networkName);

        huginDBN.saveAsNet(file);

    }
}
