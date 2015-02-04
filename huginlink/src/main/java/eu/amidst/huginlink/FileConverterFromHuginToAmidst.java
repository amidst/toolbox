package eu.amidst.huginlink;

import COM.hugin.HAPI.*;
import eu.amidst.core.models.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by afa on 3/2/15.
 */
public class FileConverterFromHuginToAmidst {

    public static String getModelType(String fullName) {

        String line = null;
        try {
            BufferedReader bufferReader = new BufferedReader(new FileReader(fullName));
            do {
                line = bufferReader.readLine();
            } while (line.isEmpty() &&
                     line.compareTo("net")!=0 &&
                     line.substring(0,5).compareTo("class")!=0 &&
                     line!=null);
            bufferReader.close();

        } catch (Exception e) {
            System.out.println("Error while reading file line by line:" + e.getMessage());
        }

        if (line == null) return null;
        if (line.compareTo("net")==0) return "BN";
        if (line.substring(0,5).compareTo("class")==0) return "DBN";
        return null;
    }

    public static void convertFilesFromFolder(final File folder) throws ExceptionHugin, IOException, ClassNotFoundException {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                convertFilesFromFolder(fileEntry);
            } else {
                String fileName = fileEntry.getName();
                if(fileName.endsWith(".net")) {
                    String amidstName = fileName.substring(0,fileName.length()-4)+".ser";
                    String fullAmidstName = folder.getName() + "/" + amidstName;
                    String fullName = folder.getName() + "/" + fileName;
                    if (FileConverterFromHuginToAmidst.getModelType(fullName)=="BN"){
                        System.out.println("Converting " + fileName +" to " + amidstName);
                        ParseListener parseListener = new DefaultClassParseListener();
                        Domain huginBN = new Domain (fullName, parseListener);
                        BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);
                        BayesianNetworkWriter.saveToFile(amidstBN,fullAmidstName);

                        //Move to test
                        BayesianNetwork amidstBN2 = BayesianNetworkLoader.loadFromFile(fullAmidstName);
                        System.out.println(amidstBN.equalBNs(amidstBN2, 0.00000001));


                    }
//                    else
//                    {
//                        if (FileConverterFromHuginToAmidst.getModelType(fullName) == "DBN") {
//                            Class huginDBN = new Class(new ClassCollection(), fullName);
//
//                            DynamicBayesianNetwork amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);
//                            DynamicBayesianNetworkWriter.saveToFile(amidstDBN, amidstName);
//                            System.out.println("Converting " + fileName +" to " + amidstName);
//
//                        }
//                    }
                }
            }
        }
    }

}
