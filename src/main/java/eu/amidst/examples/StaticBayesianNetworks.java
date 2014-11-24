package eu.amidst.examples;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.huginlink.ConverterToHugin;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 22/11/14.
 */
public class StaticBayesianNetworks {

    public static void main(String[] args) throws Exception{
        //**************************************** Synthetic data ******************************************************
        DataOnDisk data = new StaticDataOnDiskFromFile(new WekaDataFileReader(new String("datasets/syntheticData.arff")));

        //***************************************** Network structure **************************************************
        //Create the structure by hand
        StaticVariables variables = new StaticVariables(data.getAttributes());

        Variable A = variables.getVariableByName("A");
        Variable B = variables.getVariableByName("B");
        Variable C = variables.getVariableByName("C");
        Variable D = variables.getVariableByName("D");
        Variable E = variables.getVariableByName("E");
        Variable G = variables.getVariableByName("G");
        Variable H = variables.getVariableByName("H");
        Variable I = variables.getVariableByName("I");

        //Example
        DAG dag = new DAG(variables);

        dag.getParentSet(E).addParent(A);
        dag.getParentSet(E).addParent(B);

        dag.getParentSet(H).addParent(A);
        dag.getParentSet(H).addParent(B);

        dag.getParentSet(I).addParent(A);
        dag.getParentSet(I).addParent(B);
        dag.getParentSet(I).addParent(C);
        dag.getParentSet(I).addParent(D);

        dag.getParentSet(G).addParent(C);
        dag.getParentSet(G).addParent(D);

        System.out.println(dag.toString());

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        System.out.println(bn.toString());

        double logProb = 0;
        for (DataInstance instance: data){
            logProb += bn.getLogProbabiltyOfFullAssignment(instance);
        }

        System.out.println(logProb);


        ConverterToHugin converterToHugin = new ConverterToHugin();
        converterToHugin.convertToHuginBN(bn);
        String outFile = new String("networks/huginStaticBNExample.net");
        converterToHugin.getHuginNetwork().saveAsNet(new String(outFile));

    }
}
