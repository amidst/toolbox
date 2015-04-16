package eu.amidst.core.learning;

import eu.amidst.core.exponentialfamily.*;

/**
 * Created by andresmasegosa on 13/4/15.
 */
public class Fading implements TransitionMethod {

    double fading = 1.0;

    public Fading(double fading_){
        this.fading=fading_;
    }

    public double getFading() {
        return fading;
    }

    public void setFading(double fading) {
        this.fading = fading;
    }

    @Override
    public EF_LearningBayesianNetwork transitionModel(EF_LearningBayesianNetwork ef_extendedBN, PlateuStructure plateuStructure) {
        ef_extendedBN.getParametersVariables().getListOfVariables().stream().forEach(var -> {
            EF_BaseDistribution_MultinomialParents dist = (EF_BaseDistribution_MultinomialParents) ef_extendedBN.getDistribution(var);
            EF_UnivariateDistribution prior = dist.getBaseEFUnivariateDistribution(0);
            NaturalParameters naturalParameters = prior.getNaturalParameters();
            naturalParameters.multiplyBy(fading);
            prior.setNaturalParameters(naturalParameters);
            dist.setBaseEFDistribution(0, prior);
        });

        return ef_extendedBN;
    }

}
