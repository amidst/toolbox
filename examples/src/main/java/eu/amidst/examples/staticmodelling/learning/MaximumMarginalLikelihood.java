/*
package eu.amidst.staticmodelling.learning;

import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.database.DataStream;
import eu.amidst.core.exponentialfamily.ExponentialFamilyDistribution;
import eu.amidst.core.exponentialfamily.MultinomialDistribution;
import eu.amidst.core.modelstructure.statics.BayesianNetwork;
import Distribution;
import Potential;
import Utils;
import eu.amidst.staticmodelling.models.LearnableModel;

*/
/**
 * OnlineEM
 * Created by andresmasegosa on 28/08/14.
 *//*

public class MaximumMarginalLikelihood implements LearningAlgorithm{

    LearnableModel model;
    double learningRate;
    double lambda = 0.01;
    double iteration = 0;

    @Override
    public void setLearnableModel(LearnableModel model) {
        this.model=model;
    }

    @Override
    public void initLearning() {
            iteration=0;
            learningRate = 1.0/(1+lambda*iteration);
    }

    @Override
    public void updateModel(DataInstance dataInstance) {
        BayesianNetwork bn = model.getBayesianNetwork();
        for (int i = 0; i<bn.getNumberOfDynamicVars(); i++){
            if (Utils.isMissingValue(dataInstance.getValue(i)) && bn.getVariableByName(i).isLeave())
                continue;

            ExponentialFamilyDistribution estimator = (ExponentialFamilyDistribution)bn.getNormalDistributions(i);
            Distribution.ExpectationParameters expPara = estimator.getExpectationParameters();
            Potential pot = model.inferenceForLearning(dataInstance, i);
            ExponentialFamilyDistribution.SufficientStatistics expSuffStatistics = estimator.getExpectedSufficientStatistics(dataInstance, pot);
            updateEquation(expPara.getExpectationParameters(), ((MultinomialDistribution.SufficientStatistics)expSuffStatistics).getCounts());
        }
        iteration++;
    }

    private void updateEquation(double[] expPara, double[] expSuffStatistics){
        for (int i=0; i<expPara.length; i++){
            expPara[i] =  (1-learningRate)*expPara[i] + learningRate*expSuffStatistics[i];
        }
    }

    @Override
    public void learnModelFromStream(DataStream data) {
        while(data.hasNext()){
            this.model.updateModel(data.next());
        }
    }
}
*/
