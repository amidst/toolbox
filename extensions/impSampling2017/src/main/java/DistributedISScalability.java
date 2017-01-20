import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.inference.DistributedImportanceSamplingCLG;

import java.util.List;

/**
 * Created by dario on 19/1/17.
 */
public class DistributedISScalability {
    public static void main(String[] args) throws Exception {

        DistributedImportanceSamplingCLG distributedIS = new DistributedImportanceSamplingCLG();


        int seedBN = 326762;
        int nDiscreteVars = 1000;
        int nContVars = 1000;
        BayesianNetworkGenerator.setSeed(seedBN);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(nDiscreteVars, 2);
        BayesianNetworkGenerator.setNumberOfGaussianVars(nContVars);
        BayesianNetworkGenerator.setNumberOfLinks( (int)2.5*(nDiscreteVars+nContVars));
        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();

        System.out.println(bn);

        List<Variable> variableList = bn.getVariables().getListOfVariables();

        int seedIS = 111235236;
        int sampleSize = 50000;

        distributedIS.setSeed(seedIS);
        distributedIS.setModel(bn);
        distributedIS.setSampleSize(sampleSize);
        distributedIS.setVariablesOfInterest(variableList);
        distributedIS.setGaussianMixturePosteriors(true);

        distributedIS.runInference();

        for (int i = 0; i < variableList.size(); i++) {
            Variable var = variableList.get(i);
            System.out.println("Var: " + var.getName() + ", conditional=" + bn.getConditionalDistribution(var) + ", posterior=" + distributedIS.getPosterior(var));
        }

        
        

    }
}
