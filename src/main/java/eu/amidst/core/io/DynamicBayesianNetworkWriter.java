package eu.amidst.core.io;

import eu.amidst.core.models.DynamicBayesianNetwork;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Created by Hanen on 16/01/15.
 */
public class DynamicBayesianNetworkWriter {

    public static void saveToFile (DynamicBayesianNetwork bn, String fileName) throws IOException {

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
        out.writeObject(bn);
        out.close();
    }
}
