package eu.amidst.cajamareval;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

/**
 * Created by dario on 28/11/16.
 */
public class DataValidation {

public static void main(String[] args) {

        String dataOutput = "/Users/dario/Desktop/CAJAMAR_corta/";

        String[] names = {"20131231","20140101","20140102"};

        for (int i = 0; i < names.length; i++) {
            String path = dataOutput + names[i] +"/train.arff";


                DataStream<DataInstance> batch = DataStreamLoader.open(path);


                Variables variables = new Variables(batch.getAttributes());

                DAG dag = new DAG(variables);

                Variable classVar = variables.getVariableByName("Default");
                dag.getParentSets().stream().filter(p-> p.getMainVar()!=classVar).forEach(p -> {p.addParent(classVar);});

                SVB svb = new SVB();

                svb.setDAG(dag);

                svb.initLearning();

                svb.updateModel(batch);


                BayesianNetwork net = svb.getLearntBayesianNetwork();

                for (Variable variable : dag.getVariables()) {
                    if (!variable.isNormal())
                        continue;
                    System.out.print(variable.getName()+"\t");
                }

                System.out.println();

                for (Variable variable : dag.getVariables()) {
                    if (!variable.isNormal())
                        continue;

                    Normal_MultinomialParents dist = net.getConditionalDistribution(variable);

                    Normal normal = dist.getNormal(0);

                    System.out.print(normal.getMean()+"\t");
                }

                System.out.println();


        }
    }



}
