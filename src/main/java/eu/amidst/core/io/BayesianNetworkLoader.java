package eu.amidst.core.io;

import eu.amidst.core.models.BayesianNetwork;

import java.io.*;

/**
 * Created by afa on 11/12/14.
 */
public final class BayesianNetworkLoader {

    public static BayesianNetwork loadFromFile(String fileName) throws IOException, ClassNotFoundException {

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
        Object obj = ois.readObject();
        ois.close();
        return (BayesianNetwork)obj;
    }
}

