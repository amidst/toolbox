package eu.amidst.huginlink.converters;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.huginlink.io.BNLoaderFromHugin;
import eu.amidst.huginlink.io.DBNLoaderFromHugin;

import java.io.File;

/**
 * This class converts a set of Hugin networks (static and dynamic) into AMIDST networks.
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 3/2/15
 */
public class FileConverterFromHuginToAmidst {

    /**
     * Converts a set the Hugin network files (dynamic and static) into AMIDST format.
     *
     * @param folderName the path from which the conversion will be carried out (applied recursively in sub-folders too).
     * @throws Exception
     */
    public static void convertFilesFromFolder(final String folderName) throws Exception {

        File folder = new File(folderName);

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                convertFilesFromFolder(fileEntry.getName());
            } else {

                String amidstBNExtension = ".bn";
                String amidstDBNExtension = ".dbn";
                String huginBNExtension = ".net";
                String huginDBNExtension =".oobn";

                String fileName = fileEntry.getName();
                String fullFileName = folder.getName() + "/" + fileName;

                if (fileName.endsWith(huginBNExtension)) { //Static BN

                    String modelName = fileName.substring(0, fileName.length() - 4);
                    String amidstFileName = modelName + amidstBNExtension;
                    String fullAmidstFileName = folder.getName() + "/" + amidstFileName;

                    System.out.println("Converting " + fileName + " to " + amidstFileName);

                    Domain huginBN = BNLoaderFromHugin.loadFromFile(fullFileName);
                    BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);
                    BayesianNetworkWriter.saveToFile(amidstBN, fullAmidstFileName);

                    //***************************************** TEST PART **********************************************
                    BayesianNetwork amidstBN2 = BayesianNetworkLoader.loadFromFile(fullAmidstFileName);
                    if(!amidstBN.equalBNs(amidstBN2, 0.0))
                       throw new Exception("Conversion from " + fileName + " to " + amidstFileName + " failed. ");
                    //**************************************************************************************************
                }

                if (fileName.endsWith(huginDBNExtension)) { //Dynamic BN
                    String modelName = fileName.substring(0, fileName.length() - 5);
                    String amidstFileName = modelName + amidstDBNExtension;
                    String fullAmidstFileName = folder.getName() + "/" + amidstFileName;

                    System.out.println("Converting " + fileName + " to " + amidstFileName);


                    Class huginDBN = DBNLoaderFromHugin.loadFromFile(fullFileName);
                    DynamicBayesianNetwork amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);
                    DynamicBayesianNetworkWriter.saveToFile(amidstDBN, fullAmidstFileName);

                    //***************************************** TEST PART **********************************************
                    DynamicBayesianNetwork amidstDBN2 = DynamicBayesianNetworkLoader.loadFromFile(fullAmidstFileName);
                    if(!amidstDBN.equalDBNs(amidstDBN2, 0.0))
                        throw new Exception("Conversion from " + fileName + " to " + amidstFileName + " failed. ");
                    //**************************************************************************************************
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        FileConverterFromHuginToAmidst.convertFilesFromFolder("networks");
    }
}
