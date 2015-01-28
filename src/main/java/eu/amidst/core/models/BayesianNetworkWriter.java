package eu.amidst.core.models;

import java.io.*;

/**
 * Created by afa on 11/12/14.
 */
public class BayesianNetworkWriter {

    public static void saveToFile (BayesianNetwork bn, String fileName) throws IOException {

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
        out.writeObject(bn);
        out.close();
    }
}
