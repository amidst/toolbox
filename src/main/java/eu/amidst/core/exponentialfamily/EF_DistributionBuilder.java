package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.*;

import java.util.ArrayList;
import java.util.List;

/**
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
        return null;
    }

    public static Normal_NormalParents toDistribution(EF_Normal_NormalParents ef) {
        return null;
    }


}

