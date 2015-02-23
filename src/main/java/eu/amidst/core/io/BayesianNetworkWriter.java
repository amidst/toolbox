package eu.amidst.core.io;

import eu.amidst.core.models.BayesianNetwork;

import java.io.*;

/**
 * Created by afa on 11/12/14.
 */
public final class BayesianNetworkWriter {

    public static void saveToFile (BayesianNetwork bn, String fileName) throws IOException {

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
        out.writeObject(bn);
        out.close();
    }
}
