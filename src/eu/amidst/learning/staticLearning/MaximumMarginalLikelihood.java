package eu.amidst.learning.staticLearning;

import eu.amidst.core.Estimators.Estimator;
import eu.amidst.core.Potential.Potential;
import eu.amidst.core.StaticBayesianNetwork.BayesianNetwork;
import eu.amidst.core.StaticDataBase.DataInstance;
import eu.amidst.core.StaticDataBase.DataStream;
import eu.amidst.models.staticmodels.LearnableModel;

/**
 * OnlineEM
 * Created by andresmasegosa on 28/08/14.
 */
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
    public void updateModel(DataInstance data) {
        BayesianNetwork bn = model.getBayesianNetwork();
        for (int i = 0; i<bn.getNumberOfNodes(); i++){
            Estimator estimator = bn.getEstimator(i);
            double[]  expPara = estimator.getExpectationParameters();
            Potential pot = model.inferenceForLearning(data, i);
            double[]  expSuffStatistics = estimator.getExpectedSufficientStatistics(data, pot);
            updateEquation(expPara, expSuffStatistics);
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
        while(data.hasMoreDataInstances()){
            this.model.updateModel(data.nextDataInstance());
        }
    }
}
