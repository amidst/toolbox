
package eu.amidst;

import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.BayesianNetworkLoader;

import java.nio.file.Path;

public class Main {

 public static void main(String[] args) throws Exception {



     System.out.println("HOLA");

     String bnFile = "Normal_MultinomialParents";

     BayesianNetwork trueBN = BayesianNetworkLoader.loadFromHugin("./networks/"+bnFile+".net");


     eu.amidst.core.models.BayesianNetworkWriter.saveToFile(trueBN,"./networks/"+bnFile+".ser");
 }

}