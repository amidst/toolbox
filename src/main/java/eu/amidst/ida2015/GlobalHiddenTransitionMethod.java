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
    double meanStart;
    double noise;

    public GlobalHiddenTransitionMethod(Variable globalHiddenVar_, double meanStart_, double noise_){
        this.globalHiddenVar=globalHiddenVar_;
        this.meanStart = meanStart_;
        this.noise = noise_;
    }

    @Override
    public EF_LearningBayesianNetwork initModel(EF_LearningBayesianNetwork bayesianNetwork) {
        EF_NormalGamma normal = (EF_NormalGamma)bayesianNetwork.getDistribution(this.globalHiddenVar);


        Variable gammaVar = normal.getGammaParameterVariable();

        EF_Gamma gamma = ((EF_BaseDistribution_MultinomialParents<EF_Gamma>)bayesianNetwork.getDistribution(gammaVar)).getBaseEFDistribution(0);

        double alpha = 100000;
        double beta = alpha*1000;

        gamma.getNaturalParameters().set(0, alpha - 1);
        gamma.getNaturalParameters().set(1, -beta);
        gamma.updateMomentFromNaturalParameters();

        Variable meanVar = normal.getMeanParameterVariable();
        EF_Normal meanDist = ((EF_BaseDistribution_MultinomialParents<EF_Normal>)bayesianNetwork.getDistribution(meanVar)).getBaseEFDistribution(0);

        double mean = meanStart ;
        double var = 0.01;

        meanDist.getNaturalParameters().set(0,mean/(var));
        meanDist.getNaturalParameters().set(1,-1/(2*var));
        meanDist.updateMomentFromNaturalParameters();


        for (Variable paramVariable : bayesianNetwork.getParametersVariables().getListOfVariables()){

            if (!paramVariable.isNormalParameter())
                continue;


            EF_Normal prior = ((EF_BaseDistribution_MultinomialParents<EF_Normal>)bayesianNetwork.getDistribution(paramVariable)).getBaseEFDistribution(0);

            double precisionPrior = 1/0.1;
            double meanPrior = 1;

            prior.getNaturalParameters().set(0, precisionPrior*meanPrior);
            prior.getNaturalParameters().set(1, -0.5*precisionPrior);
            prior.updateMomentFromNaturalParameters();

        }


        return bayesianNetwork;

    }

    @Override
    public EF_LearningBayesianNetwork transitionModel(EF_LearningBayesianNetwork bayesianNetwork, PlateuStructure plateuStructure) {

        Normal normalGlobalHiddenPreviousTimeStep = plateuStructure.getEFVariablePosterior(globalHiddenVar, 0).toUnivariateDistribution();

        EF_NormalGamma normal = (EF_NormalGamma)bayesianNetwork.getDistribution(this.globalHiddenVar);

        Variable gammaVar = normal.getGammaParameterVariable();

        EF_Gamma gamma = ((EF_BaseDistribution_MultinomialParents<EF_Gamma>)bayesianNetwork.getDistribution(gammaVar)).getBaseEFDistribution(0);

        double variance = normalGlobalHiddenPreviousTimeStep.getVariance() + this.noise;

        double alpha = 1000;
        double beta = alpha*variance;

        gamma.getNaturalParameters().set(0, alpha - 1);
        gamma.getNaturalParameters().set(1, -beta);
        gamma.updateMomentFromNaturalParameters();

        Variable meanVar = normal.getMeanParameterVariable();
        EF_Normal meanDist = ((EF_BaseDistribution_MultinomialParents<EF_Normal>)bayesianNetwork.getDistribution(meanVar)).getBaseEFDistribution(0);

        double mean =  normalGlobalHiddenPreviousTimeStep.getMean();

        meanDist.getNaturalParameters().set(0,mean/(variance));
        meanDist.getNaturalParameters().set(1,-1/(2*variance));
        meanDist.updateMomentFromNaturalParameters();


        /***** FADING ****/
        /*
        double fading = 0.9;

        bayesianNetwork.getParametersVariables().getListOfVariables().stream().forEach(var -> {
            EF_BaseDistribution_MultinomialParents dist = (EF_BaseDistribution_MultinomialParents) bayesianNetwork.getDistribution(var);
            EF_UnivariateDistribution prior = dist.getBaseEFUnivariateDistribution(0);
            NaturalParameters naturalParameters = prior.getNaturalParameters();
            naturalParameters.multiplyBy(fading);
            prior.setNaturalParameters(naturalParameters);
            dist.setBaseEFDistribution(0, prior);
        });

        */





        return bayesianNetwork;
    }
}
