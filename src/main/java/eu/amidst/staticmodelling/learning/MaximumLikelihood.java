/*
package eu.amidst.staticmodelling.learning;


import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.database.statics.readers.DataStream;
import eu.amidst.core.exponentialfamily.ExponentialFamilyDistribution;
import eu.amidst.core.exponentialfamily.MultinomialDistribution;
import eu.amidst.core.modelstructure.statics.BayesianNetwork;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.utils.Utils;
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
        for (int i = 0; i<bn.getNumberOfNodes(); i++){
            if (Utils.isMissing(dataInstance.getValue(i)) && bn.getVariable(i).isLeave())
                continue;

            ExponentialFamilyDistribution estimator = (ExponentialFamilyDistribution)bn.getDistribution(i);
            Distribution.ExpectationParameters expPara = estimator.getExpectationParameters();
            ExponentialFamilyDistribution.SufficientStatistics suffStatistics = estimator.getSufficientStatistics(dataInstance);
            Utils.accumulatedSumVectors(expPara.getExpectationParameters(), ((MultinomialDistribution.SufficientStatistics)suffStatistics).getCounts());
        }
    }
    @Override
    public void learnModelFromStream(DataStream data) {
        while(data.hasMoreDataInstances()){
            this.model.updateModel(data.nextDataInstance());
        }
    }
}
*/
