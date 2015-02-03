package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.*;
import eu.amidst.core.exponentialfamily.EF_Normal_NormalParents.CompoundVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

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

    public static ConditionalDistribution toDistributionGeneral(EF_ConditionalDistribution dist) {

        if (dist.getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_BaseDistribution_MultinomialParents")) {
            EF_BaseDistribution_MultinomialParents newDist = (EF_BaseDistribution_MultinomialParents)dist;
            if (newDist.getEF_BaseDistribution(0).getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_Multinomial")){
                EF_BaseDistribution_MultinomialParents<EF_Multinomial> newDistMulti =  (EF_BaseDistribution_MultinomialParents<EF_Multinomial>)dist;
                return toDistribution(newDistMulti,newDistMulti.getEF_BaseDistribution(0));
            }else if (newDist.getEF_BaseDistribution(0).getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_Normal")){
                EF_BaseDistribution_MultinomialParents<EF_Normal> newDistMulti =  (EF_BaseDistribution_MultinomialParents<EF_Normal>)dist;
                return toDistribution(newDistMulti,newDistMulti.getEF_BaseDistribution(0));
            }else if (newDist.getEF_BaseDistribution(0).getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_Normal_NormalParents")) {
                EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents> newDistMulti =  (EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents>)dist;
                return toDistribution(newDistMulti,newDistMulti.getEF_BaseDistribution(0));
            }else {
                throw new IllegalArgumentException("This conditional distribution can not be converted to an exponential form: "+ dist.getClass().getName());
            }
        } else if (dist.getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_Normal_NormalParents")) {
            return toDistribution((EF_Normal_NormalParents) dist);
        } else{
            throw new IllegalArgumentException("This conditional distribution can not be converted to an exponential form: "+ dist.getClass().getName());
        }

    }


    public static Normal_MultinomialNormalParents toDistribution(EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents> dist, EF_Normal_NormalParents base) {

        Normal_MultinomialNormalParents newDist = new Normal_MultinomialNormalParents(dist.getVariable(), dist.getConditioningVariables());

        for (int i = 0; i < dist.numberOfConfigurations(); i++) {
            newDist.setNormal_NormalParentsDistribution(i, EF_DistributionBuilder.toDistribution(dist.getEF_BaseDistribution(i)));
        }

        return newDist;

    }

    public static EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents> toEFDistribution(Normal_MultinomialNormalParents dist) {

        List<EF_Normal_NormalParents> newDist = new ArrayList<EF_Normal_NormalParents>();

        for (int i = 0; i < dist.getNumberOfParentAssignments(); i++) {
            newDist.add(EF_DistributionBuilder.toEFDistribution(dist.getNormal_NormalParentsDistribution(i)));
        }

        return new EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents>(dist.getMultinomialParents(), newDist);

    }

    public static Normal_MultinomialParents toDistribution(EF_BaseDistribution_MultinomialParents<EF_Normal> dist, EF_Normal base) {

        Normal_MultinomialParents newDist = new Normal_MultinomialParents(dist.getVariable(), dist.getConditioningVariables());

        for (int i = 0; i < dist.numberOfConfigurations(); i++) {
            newDist.setNormal(i, EF_DistributionBuilder.toDistribution(dist.getEF_BaseDistribution(i)));
        }

        return newDist;

    }

    public static EF_BaseDistribution_MultinomialParents<EF_Normal> toEFDistribution(Normal_MultinomialParents dist) {

        List<EF_Normal> newDist = new ArrayList<EF_Normal>();

        for (int i = 0; i < dist.getNumberOfParentAssignments(); i++) {
            newDist.add(EF_DistributionBuilder.toEFDistribution(dist.getNormal(i)));
        }

        return new EF_BaseDistribution_MultinomialParents<EF_Normal>(dist.getConditioningVariables(), newDist);

    }

    public static Multinomial_MultinomialParents toDistribution(EF_BaseDistribution_MultinomialParents<EF_Multinomial> dist, EF_Multinomial base) {

        Multinomial_MultinomialParents multi = new Multinomial_MultinomialParents(dist.getVariable(), dist.getConditioningVariables());

        for (int i = 0; i < dist.numberOfConfigurations(); i++) {
            multi.setMultinomial(i, EF_DistributionBuilder.toDistribution(dist.getEF_BaseDistribution(i)));
        }

        return multi;

    }

    public static EF_BaseDistribution_MultinomialParents<EF_Multinomial> toEFDistribution(Multinomial_MultinomialParents dist) {

        List<EF_Multinomial> distMultinomial = new ArrayList<EF_Multinomial>();

        for (int i = 0; i < dist.getNumberOfParentAssignments(); i++) {
            distMultinomial.add(EF_DistributionBuilder.toEFDistribution(dist.getMultinomial(i)));
        }

        return new EF_BaseDistribution_MultinomialParents<EF_Multinomial>(dist.getConditioningVariables(), distMultinomial);

    }

    public static EF_Normal toEFDistribution(Normal dist) {

        EF_Normal efNormal = new EF_Normal(dist.getVariable());
        MomentParameters momentParameters = efNormal.createZeroedMomentParameters();
        momentParameters.set(EF_Normal.EXPECTED_MEAN, dist.getMean());
        momentParameters.set(EF_Normal.EXPECTED_SQUARE, dist.getMean() * dist.getMean() + dist.getSd() * dist.getSd());
        efNormal.setMomentParameters(momentParameters);
        return efNormal;

    }


    public static Normal toDistribution(EF_Normal efNormal) {

        Normal normal = new Normal(efNormal.getVariable());
        double mean = efNormal.getMomentParameters().get(EF_Normal.EXPECTED_MEAN);
        double sigma = efNormal.getMomentParameters().get(EF_Normal.EXPECTED_SQUARE) - mean * mean;

        normal.setMean(mean);
        normal.setSd(Math.sqrt(sigma));

        return normal;
    }


    public static EF_Multinomial toEFDistribution(Multinomial dist) {
        EF_Multinomial efMultinomial = new EF_Multinomial(dist.getVariable());

        MomentParameters momentParameters = efMultinomial.createZeroedMomentParameters();

        for (int i = 0; i < dist.getVariable().getNumberOfStates(); i++) {
            momentParameters.set(i, dist.getProbabilityOfState(i));
        }

        efMultinomial.setMomentParameters(momentParameters);

        return efMultinomial;
    }

    public static Multinomial toDistribution(EF_Multinomial efmultinomial) {

        Multinomial multinomial = new Multinomial(efmultinomial.getVariable());

        for (int i = 0; i < multinomial.getVariable().getNumberOfStates(); i++) {
            multinomial.setProbabilityOfState(i, efmultinomial.getMomentParameters().get(i));
        }

        return multinomial;
    }

    public static EF_Normal_NormalParents toEFDistribution(Normal_NormalParents dist) {

        EF_Normal_NormalParents ef_normal_normalParents = new EF_Normal_NormalParents(dist.getVariable(), dist.getConditioningVariables());

        CompoundVector naturalParameters = ef_normal_normalParents.createEmtpyCompoundVector();

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
        double[] theta0_beta = Arrays.stream(coeffParents).map(w->-w*beta_0*variance2Inv).toArray();
        naturalParameters.setThetaBeta0Beta_NatParam(theta0_beta);

        /*
         * 3) theta_Minus1
         */
        double theta_Minus1 = -variance2Inv;

        /*
         * 4) theta_beta & 5) theta_betaBeta
         */
        double[] theta_beta = Arrays.stream(coeffParents).map(w -> w / variance).toArray();
        naturalParameters.setThetaCov_NatParam(theta_Minus1,theta_beta, variance2Inv);

        ef_normal_normalParents.setNaturalParameters(naturalParameters);
        return ef_normal_normalParents;
    }

    public static Normal_NormalParents toDistribution(EF_Normal_NormalParents ef) {

        Normal_NormalParents normal_normal = new Normal_NormalParents(ef.getVariable(), ef.getConditioningVariables());

        double[] allBeta = ef.getAllBetaValues();

        normal_normal.setIntercept(allBeta[0]);
        normal_normal.setCoeffParents(Arrays.copyOfRange(allBeta, 1, allBeta.length));
        normal_normal.setSd(Math.sqrt(ef.getVariance()));

        return normal_normal;
    }


}

