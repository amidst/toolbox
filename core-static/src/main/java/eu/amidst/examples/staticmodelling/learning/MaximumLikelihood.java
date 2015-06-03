/*
package eu.amidst.staticmodelling.learning;


import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.database.DataStream;
import eu.amidst.core.exponentialfamily.ExponentialFamilyDistribution;
import eu.amidst.core.exponentialfamily.MultinomialDistribution;
import eu.amidst.core.modelstructure.statics.BayesianNetwork;
import Distribution;
import Utils;
import eu.amidst.staticmodelling.models.LearnableModel;

*/
/**
 * Created by andresmasegosa on 28/08/14.
 *//*

public class MaximumLikelihood implements LearningAlgorithm{

    LearnableModel model;

    @Override
    public void setLearnableModel(LearnableModel model) {
        this.model=model;
    }

    @Override
    public void initLearning() {

    }

    @Override
    public void updateModel(DataInstance dataInstance) {
        BayesianNetwork bn = model.getBayesianNetwork();
        for (int i = 0; i<bn.getNumberOfDynamicVars(); i++){
            if (Utils.isMissingValue(dataInstance.getValue(i)) && bn.getVariableByName(i).isLeave())
                continue;

            ExponentialFamilyDistribution estimator = (ExponentialFamilyDistribution)bn.getNormalDistributions(i);
            Distribution.ExpectationParameters expPara = estimator.getExpectationParameters();
            ExponentialFamilyDistribution.SufficientStatistics suffStatistics = estimator.getSufficientStatistics(dataInstance);
            Utils.accumulatedSumVectors(expPara.getExpectationParameters(), ((MultinomialDistribution.SufficientStatistics)suffStatistics).getCounts());
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
