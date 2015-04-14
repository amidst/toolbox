package eu.amidst.ida2015;

import eu.amidst.core.distribution.Normal;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.learning.PlateuStructure;
import eu.amidst.core.learning.TransitionMethod;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 13/4/15.
 */
public class GlobalHiddenTransitionMethod implements TransitionMethod{

    Variable globalHiddenVar;
    double noise;

    public GlobalHiddenTransitionMethod(Variable globalHiddenVar_, double noise_){
        this.globalHiddenVar=globalHiddenVar_;
        this.noise = noise_;
    }

    @Override
    public EF_LearningBayesianNetwork initModel(EF_LearningBayesianNetwork bayesianNetwork) {
        EF_NormalGamma normal = (EF_NormalGamma)bayesianNetwork.getDistribution(this.globalHiddenVar);


        Variable gammaVar = normal.getGammaParameterVariable();

        EF_Gamma gamma = ((EF_BaseDistribution_MultinomialParents<EF_Gamma>)bayesianNetwork.getDistribution(gammaVar)).getBaseEFDistribution(0);

        double alpha = 1000;
        double beta = alpha*0.01;

        gamma.getNaturalParameters().set(0, alpha - 1);
        gamma.getNaturalParameters().set(1, -beta);
        gamma.updateMomentFromNaturalParameters();

        Variable meanVar = normal.getMeanParameterVariable();
        EF_Normal meanDist = ((EF_BaseDistribution_MultinomialParents<EF_Normal>)bayesianNetwork.getDistribution(meanVar)).getBaseEFDistribution(0);

        double mean = 0.1 ;
        double var = 0.01;

        meanDist.getNaturalParameters().set(0,mean/(var));
        meanDist.getNaturalParameters().set(1,-1/(2*var));
        meanDist.updateMomentFromNaturalParameters();

        return bayesianNetwork;

    }

    @Override
    public EF_LearningBayesianNetwork transitionModel(EF_LearningBayesianNetwork bayesianNetwork) {


        EF_NormalGamma normal = (EF_NormalGamma)bayesianNetwork.getDistribution(this.globalHiddenVar);

        Variable gammaVar = normal.getGammaParameterVariable();

        EF_Gamma gamma = ((EF_BaseDistribution_MultinomialParents<EF_Gamma>)bayesianNetwork.getDistribution(gammaVar)).getBaseEFDistribution(0);

        double precision = gamma.getExpectedParameters().get(0);

        double var= 1.0/precision + this.noise;

        double alpha = 1000;
        double beta = alpha*var;

        gamma.getNaturalParameters().set(0, alpha - 1);
        gamma.getNaturalParameters().set(1, -beta);
        gamma.updateMomentFromNaturalParameters();

        Variable meanVar = normal.getMeanParameterVariable();
        EF_Normal meanDist = ((EF_BaseDistribution_MultinomialParents<EF_Normal>)bayesianNetwork.getDistribution(meanVar)).getBaseEFDistribution(0);

        double mean = meanDist.getExpectedParameters().get(0);

        meanDist.getNaturalParameters().set(0,mean/(var));
        meanDist.getNaturalParameters().set(1,-1/(2*var));
        meanDist.updateMomentFromNaturalParameters();

        return bayesianNetwork;
    }
}
