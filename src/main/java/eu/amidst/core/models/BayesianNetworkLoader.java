package eu.amidst.core.models;

import java.io.*;

/**
 * Created by afa on 11/12/14.
 */
public class BayesianNetworkLoader {

    public static BayesianNetwork loadFromFile(String fileName) throws IOException, ClassNotFoundException {

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
        Object obj = ois.readObject();
        ois.close();
        return (BayesianNetwork)obj;
    }
}

