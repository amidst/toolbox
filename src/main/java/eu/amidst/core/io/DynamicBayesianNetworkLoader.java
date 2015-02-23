package eu.amidst.core.io;

import eu.amidst.core.models.DynamicBayesianNetwork;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Created by Hanen on 15/01/15.
 */
public class DynamicBayesianNetworkLoader {

    public static DynamicBayesianNetwork loadFromFile (String fileName) throws ClassNotFoundException, IOException {

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
        Object obj = ois.readObject();
        ois.close();
        return (DynamicBayesianNetwork)obj;
    }

}
