package eu.amidst.core.learning;

import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;

/**
 * Created by andresmasegosa on 13/4/15.
 */
public interface TransitionMethod {

    public default EF_LearningBayesianNetwork initModel(EF_LearningBayesianNetwork bayesianNetwork){
        return bayesianNetwork;
    }

    public EF_LearningBayesianNetwork transitionModel(EF_LearningBayesianNetwork bayesianNetwork);
}
