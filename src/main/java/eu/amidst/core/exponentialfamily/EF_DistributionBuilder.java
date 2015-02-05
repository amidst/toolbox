package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.*;
import eu.amidst.core.exponentialfamily.EF_Normal_NormalParents.CompoundVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 *
 * TODO: Redesign this class!!!!!
 * Created by andresmasegosa on 12/11/14.
 */
public final class EF_DistributionBuilder {

    private EF_DistributionBuilder() {
        //Not called
    }

    /*
    * ----------------- Methods Converting Distributions to Exponential Family (EF) Distributions -----------------
    */

   /*
    * Convert an UnivariateDistribution to an EF_UnivariateDistribution
   */
    public static EF_UnivariateDistribution toUnivariateDistribution(UnivariateDistribution dist) {
        if (dist.getClass().getName().equals("eu.amidst.core.distribution.Normal")) {
            return toEFDistribution((eu.amidst.core.distribution.Normal) dist);
        }else if (dist.getClass().getName().equals("eu.amidst.core.distribution.Multinomial")) {
            return toEFDistribution((eu.amidst.core.distribution.Multinomial)dist);
        }else{
            throw new IllegalArgumentException("This univariate distribution can not be converted to an an exponential form: "+ dist.getClass().getName());
        }
    }

    /*
    * Convert a ConditionalDistribution to an EF_ConditionalDistribution
    */
    public static EF_ConditionalDistribution toEFDistributionGeneral(ConditionalDistribution dist) {

        if (dist.getClass().getName().equals("eu.amidst.core.distribution.Multinomial_MultinomialParents")) {
            return toEFDistribution((Multinomial_MultinomialParents) dist);
        } else if (dist.getClass().getName().equals("eu.amidst.core.distribution.Normal_MultinomialParents")) {
            return toEFDistribution((Normal_MultinomialParents) dist);
        } else if (dist.getClass().getName().equals("eu.amidst.core.distribution.Normal_NormalParents")) {
            return toEFDistribution((Normal_NormalParents) dist);
        } else if (dist.getClass().getName().equals("eu.amidst.core.distribution.Normal_MultinomialNormalParents")) {
            return toEFDistribution((Normal_MultinomialNormalParents) dist);
        }else{
            throw new IllegalArgumentException("This conditional distribution can not be converted to an exponential form: "+ dist.getClass().getName());
        }
    }

    /*
    * Convert a Multinomial Distribution to an EF_Multinomial
    */
    public static EF_Multinomial toEFDistribution(Multinomial dist) {
        EF_Multinomial newDist = new EF_Multinomial(dist.getVariable());

        MomentParameters momentParameters = newDist.createZeroedMomentParameters();

        for (int i = 0; i < dist.getVariable().getNumberOfStates(); i++) {
            momentParameters.set(i, dist.getProbabilityOfState(i));
        }

        newDist.setMomentParameters(momentParameters);

        return newDist;
    }

    /*
    * Convert a Normal Distribution to an EF_Normal
    */
    public static EF_Normal toEFDistribution(Normal dist) {

        EF_Normal newDist = new EF_Normal(dist.getVariable());
        MomentParameters momentParameters = newDist.createZeroedMomentParameters();
        momentParameters.set(EF_Normal.EXPECTED_MEAN, dist.getMean());
        momentParameters.set(EF_Normal.EXPECTED_SQUARE, dist.getMean() * dist.getMean() + dist.getSd() * dist.getSd());
        newDist.setMomentParameters(momentParameters);
        return newDist;
    }

    /*
    * Convert a Multinomial_MultinomialParents Distribution to an EF_BaseDistribution_MultinomialParents<EF_Multinomial>
    */

    public static EF_BaseDistribution_MultinomialParents<EF_Multinomial> toEFDistribution(Multinomial_MultinomialParents dist) {

        List<EF_Multinomial> newDist = new ArrayList<EF_Multinomial>();

        for (int i = 0; i < dist.getNumberOfParentAssignments(); i++) {
            newDist.add(EF_DistributionBuilder.toEFDistribution(dist.getMultinomial(i)));
        }

        return new EF_BaseDistribution_MultinomialParents<EF_Multinomial>(dist.getConditioningVariables(), newDist);
    }

    /*
    * Convert a Normal_MultinomialParents Distribution to an EF_BaseDistribution_MultinomialParents<EF_Normal>
    */

    public static EF_BaseDistribution_MultinomialParents<EF_Normal> toEFDistribution(Normal_MultinomialParents dist) {

        List<EF_Normal> newDist = new ArrayList<EF_Normal>();

        for (int i = 0; i < dist.getNumberOfParentAssignments(); i++) {
            newDist.add(EF_DistributionBuilder.toEFDistribution(dist.getNormal(i)));
        }

        return new EF_BaseDistribution_MultinomialParents<EF_Normal>(dist.getConditioningVariables(), newDist);
    }

    /*
    * Convert a Normal_MultinomialNormalParents Distribution to an EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents>
    */
    public static EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents> toEFDistribution(Normal_MultinomialNormalParents dist) {

        List<EF_Normal_NormalParents> newDist = new ArrayList<EF_Normal_NormalParents>();

        for (int i = 0; i < dist.getNumberOfParentAssignments(); i++) {
            newDist.add(EF_DistributionBuilder.toEFDistribution(dist.getNormal_NormalParentsDistribution(i)));
        }

        return new EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents>(dist.getMultinomialParents(), newDist);
    }

    /*
    * Convert a Normal_NormalParents Distribution to an EF_Normal_NormalParents
    */
    public static EF_Normal_NormalParents toEFDistribution(Normal_NormalParents dist) {

        EF_Normal_NormalParents newDist = new EF_Normal_NormalParents(dist.getVariable(), dist.getConditioningVariables());

        CompoundVector naturalParameters = newDist.createEmtpyCompoundVector();

        double beta_0 = dist.getIntercept();
        double[] coeffParents = dist.getCoeffParents();
        double sd = dist.getSd();

        double variance = sd*sd;
        /*
         * 1) theta_0
         */
        double theta_0 = beta_0 / variance;
        naturalParameters.setThetaBeta0_NatParam(theta_0);

        /*
         * 2) theta_0Theta
         */
        double variance2Inv =  1.0/(2*variance);
        //IntStream.range(0,coeffParents.length).forEach(i-> coeffParents[i]*=(beta_0*variance2Inv));
        double[] theta0_beta = Arrays.stream(coeffParents).map(w->-w*beta_0/variance).toArray();
        naturalParameters.setThetaBeta0Beta_NatParam(theta0_beta);

        /*
         * 3) theta_Minus1
         */
        double theta_Minus1 = -variance2Inv;

        /*
         * 4) theta_beta & 5) theta_betaBeta
         */
        naturalParameters.setThetaCov_NatParam(theta_Minus1,coeffParents, variance2Inv);

        newDist.setNaturalParameters(naturalParameters);
        return newDist;
    }


    /*
    * ----------------- Methods Converting Exponential Family (EF) Distributions to Distributions -----------------
    */

    /*
    * Convert an EF_UnivariateDistribution to an UnivariateDistribution
    */
    public static UnivariateDistribution toUnivariateDistribution(EF_UnivariateDistribution dist) {
        if (dist.getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_Normal")) {
            return toDistribution((eu.amidst.core.exponentialfamily.EF_Normal) dist);
        }else if (dist.getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_Multinomial")) {
            return toDistribution((eu.amidst.core.exponentialfamily.EF_Multinomial)dist);
        }else{
            throw new IllegalArgumentException("This exponential family univariate distribution can not be converted to an univariate distribution: "+ dist.getClass().getName());
        }
    }

    /*
    * Convert an EF_ConditionalDistribution to an ConditionalDistribution
    */
    public static ConditionalDistribution toDistributionGeneral(EF_ConditionalDistribution dist) {

        if (dist.getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_BaseDistribution_MultinomialParents")) {

            EF_BaseDistribution_MultinomialParents newDist = (EF_BaseDistribution_MultinomialParents)dist;

            if (newDist.getEFBaseDistribution(0).getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_Multinomial")){
                EF_BaseDistribution_MultinomialParents<EF_Multinomial> newDistMulti =  (EF_BaseDistribution_MultinomialParents<EF_Multinomial>)dist;
                return toDistribution(newDistMulti,newDistMulti.getEFBaseDistribution(0));
            }else if (newDist.getEFBaseDistribution(0).getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_Normal")){
                EF_BaseDistribution_MultinomialParents<EF_Normal> newDistMulti =  (EF_BaseDistribution_MultinomialParents<EF_Normal>)dist;
                return toDistribution(newDistMulti,newDistMulti.getEFBaseDistribution(0));
            }else if (newDist.getEFBaseDistribution(0).getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_Normal_NormalParents")) {
                EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents> newDistMulti =  (EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents>)dist;
                return toDistribution(newDistMulti,newDistMulti.getEFBaseDistribution(0));
            }else {
                throw new IllegalArgumentException("This Exponential Family Conditional Distribution cannot be converted to a Conditional distribution: "+ dist.getClass().getName());
            }
        } else if (dist.getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_Normal_NormalParents")) {
            return toDistribution((EF_Normal_NormalParents) dist);
        } else{
            throw new IllegalArgumentException("This exponential family conditional distribution cannot be converted to a conditional distribution: "+ dist.getClass().getName());
        }
    }

    /*
    * Convert an EF_Multinomial to a Multinomial
    */
    public static Multinomial toDistribution(EF_Multinomial dist) {

        Multinomial newDist = new Multinomial(dist.getVariable());

        for (int i = 0; i < newDist.getVariable().getNumberOfStates(); i++) {
            newDist.setProbabilityOfState(i, dist.getMomentParameters().get(i));
        }

        return newDist;
    }

    /*
    * Convert an EF_Normal to a Normal
    */
    public static Normal toDistribution(EF_Normal dist) {

        Normal newDist = new Normal(dist.getVariable());
        double mean = dist.getMomentParameters().get(EF_Normal.EXPECTED_MEAN);
        double sigma = dist.getMomentParameters().get(EF_Normal.EXPECTED_SQUARE) - mean * mean;

        newDist.setMean(mean);
        newDist.setSd(Math.sqrt(sigma));

        return newDist;
    }

    /*
    * Convert an EF_BaseDistribution_MultinomialParents to a Multinomial_MultinomialParents
    */
    public static Multinomial_MultinomialParents toDistribution(EF_BaseDistribution_MultinomialParents<EF_Multinomial> dist, EF_Multinomial base) {

        Multinomial_MultinomialParents newDist = new Multinomial_MultinomialParents(dist.getVariable(), dist.getConditioningVariables());

        for (int i = 0; i < dist.numberOfConfigurations(); i++) {
            newDist.setMultinomial(i, EF_DistributionBuilder.toDistribution(dist.getEFBaseDistribution(i)));
        }

        return newDist;
    }

    /*
    * Convert an EF_BaseDistribution_MultinomialParents to a Normal_MultinomialParents
    */
    public static Normal_MultinomialParents toDistribution(EF_BaseDistribution_MultinomialParents<EF_Normal> dist, EF_Normal base) {

        Normal_MultinomialParents newDist = new Normal_MultinomialParents(dist.getVariable(), dist.getConditioningVariables());

        for (int i = 0; i < dist.numberOfConfigurations(); i++) {
            newDist.setNormal(i, EF_DistributionBuilder.toDistribution(dist.getEFBaseDistribution(i)));
        }

        return newDist;
    }

    /*
    * Convert an EF_BaseDistribution_MultinomialParents to a Normal_MultinomialNormalParents
    */
    public static Normal_MultinomialNormalParents toDistribution(EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents> dist, EF_Normal_NormalParents base) {

        Normal_MultinomialNormalParents newDist = new Normal_MultinomialNormalParents(dist.getVariable(), dist.getConditioningVariables());

        for (int i = 0; i < dist.numberOfConfigurations(); i++) {
            newDist.setNormal_NormalParentsDistribution(i, EF_DistributionBuilder.toDistribution(dist.getEFBaseDistribution(i)));
        }

        return newDist;
    }

    /*
    * Convert an EF_Normal_NormalParents to a Normal_NormalParents
    */
    public static Normal_NormalParents toDistribution(EF_Normal_NormalParents dist) {

        Normal_NormalParents newDist = new Normal_NormalParents(dist.getVariable(), dist.getConditioningVariables());

        double[] allBeta = dist.getAllBetaValues();

        newDist.setIntercept(allBeta[0]);
        newDist.setCoeffParents(Arrays.copyOfRange(allBeta, 1, allBeta.length));
        newDist.setSd(Math.sqrt(dist.getVariance()));

        return newDist;
    }


}
