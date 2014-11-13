package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.*;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public class EF_DistributionBuilder {

    public static EF_Multinomial_MultinomialParents newEFFromConditionalDistribution(Multinomial_MultinomialParents dist){
        return null;
    }

    public static EF_Multinomial newEFFromUnivariateDistribution(Multinomial dist){
        return null;
    }

    public static EF_Normal newEFFromUnivariateDistribution(Normal dist){

        EF_Normal ef_normal = new EF_Normal(dist.getVariable());
        MomentParameters momentParameters = new MomentParameters(2);
        momentParameters.set(EF_Normal.EXPECTED_MEAN,dist.getMean());
        momentParameters.set(EF_Normal.EXPECTED_SQUARE,dist.getMean()*dist.getMean() + dist.getSd()*dist.getSd());
        ef_normal.setMomentParameters(momentParameters);
        return ef_normal;

    }


    public static Normal newNormalFromEF_NormalDistribution(EF_Normal ef_normal){

        Normal normal = new Normal(ef_normal.getVariable());
        double mean = ef_normal.getMomentParameters().get(EF_Normal.EXPECTED_MEAN);
        double sigma = ef_normal.getMomentParameters().get(EF_Normal.EXPECTED_SQUARE) - mean*mean;

        normal.setMean(mean);
        normal.setSd(Math.sqrt(sigma));

        return normal;
    }

}
