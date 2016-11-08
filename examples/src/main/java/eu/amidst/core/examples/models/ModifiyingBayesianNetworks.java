package eu.amidst.core.examples.models;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.variables.Variable;

/**
 *
 * In this example we show how to access and modify the conditional probabilities of a Bayesian network model.
 * Created by andresmasegosa on 24/6/15.
 */
public class ModifiyingBayesianNetworks {

    public static void main (String[] args){

        //We first generate a Bayesian network with one multinomial, one Gaussian variable and one link
        BayesianNetworkGenerator.setNumberOfGaussianVars(1);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(1,2);
        BayesianNetworkGenerator.setNumberOfLinks(1);

        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();

        //We print the randomly generated Bayesian networks
        System.out.println(bn.toString());

        //We first access the variable we are interested in
        Variable multiVar = bn.getVariables().getVariableByName("DiscreteVar0");

        //Using the above variable we can get the associated distribution and modify it
        Multinomial multinomial = bn.getConditionalDistribution(multiVar);
        multinomial.setProbabilities(new double[]{0.2, 0.8});

        //Same than before but accessing the another variable
        Variable normalVar = bn.getVariables().getVariableByName("GaussianVar0");

        //In this case, the conditional distribtuion is of the type "Normal given Multinomial Parents"
        Normal_MultinomialParents normalMultiDist = bn.getConditionalDistribution(normalVar);
        normalMultiDist.getNormal(0).setMean(1.0);
        normalMultiDist.getNormal(0).setVariance(1.0);

        normalMultiDist.getNormal(1).setMean(0.0);
        normalMultiDist.getNormal(1).setVariance(1.0);

        //We print modified Bayesian network
        System.out.println(bn.toString());
    }
}
