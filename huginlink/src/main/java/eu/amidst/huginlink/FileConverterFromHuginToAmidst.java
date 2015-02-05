package eu.amidst.huginlink;

import eu.amidst.core.models.*;
import eu.amidst.core.models.DynamicBayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetworkWriter;
import java.io.File;

/**
 * Created by afa on 3/2/15.
 */
public class FileConverterFromHuginToAmidst {

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

                    BayesianNetwork amidstBN = BNLoaderFromHugin.loadFromFile(fullFileName);
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

                    DynamicBayesianNetwork amidstDBN = DBNLoaderFromHugin.loadFromFile(fullFileName);
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
}
