package eu.amidst.core.hugin_interface;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.header.StaticModelHeader;
import eu.amidst.core.header.Variable;
import eu.amidst.core.modelstructure.BayesianNetwork;
import eu.amidst.core.modelstructure.DAG;
import org.junit.Test;

import java.util.List;

/**
 * Created by afa on 18/11/14.
 */
public class ConverterToHuginTest {


    public static void main(String args[]) throws ExceptionHugin {

        //**************************************** Synthetic data ******************************************************

        WekaDataFileReader fileReader = new WekaDataFileReader(
                new String("/Users/afa/Dropbox/AMIDST-AFA/core/datasets/syntheticData.arff"));

        StaticModelHeader modelHeader = new StaticModelHeader(fileReader.getAttributes());

        //***************************************** Network structure **************************************************
        //Create the structure by hand

        DAG dag = new DAG(modelHeader);
        List<Variable> variables = dag.getModelHeader().getVariables();

        Variable A, B, C, D, E, G, H, I;
        A = variables.get(0);
        B = variables.get(1);
        C = variables.get(2);
        D = variables.get(3);
        E = variables.get(4);
        G = variables.get(5);
        H = variables.get(6);
        I = variables.get(7);


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

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        //****************************************** Distributions *****************************************************

        bn.initializeDistributions();

        //**************************************************************************************************************

        ConverterToHugin converter = new ConverterToHugin();
        converter.setBayesianNetwork(bn);
        converter.getHuginNetwork().saveAsNet(new String("/Users/afa/Dropbox/AMIDST-AFA/core/networks/huginNetworkFromAMIDST.net"));


    }

    //*********************************************************************************
//            //Simulate a sample from a Hugin network
//            int nsamples = 100;
//            for (int j=0;j< nodeList.size();j++) {
//                System.out.print(((Node)nodeList.get(j)).getName());
//                if(j<nodeList.size()-1)
//                    System.out.print(",");
//            }
//            System.out.println();
//            for (int i=0;i<nsamples;i++){
//                domain.simulate();
//                for (int j=0;j<nodeList.size();j++){
//                    System.out.print(((ContinuousChanceNode)nodeList.get(j)).getSampledValue());
//                    if(j<nodeList.size()-1)
//                        System.out.print(",");
//                }
//                System.out.println();
//            }
//            //*********************************************************************************



}
