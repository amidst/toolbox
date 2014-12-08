package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.*;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public final class EF_DistributionBuilder {

    private EF_DistributionBuilder(){
        //Not called
    }

    public static EF_BaseDistribution_MultinomialParents newEFFromConditionalDistribution(Multinomial_MultinomialParents dist){



        return null;
    }

    public static EF_Normal toEFDistribution(Normal dist){

        EF_Normal ef_normal = new EF_Normal(dist.getVariable());
        MomentParameters momentParameters = new MomentParameters(2);
        momentParameters.set(EF_Normal.EXPECTED_MEAN,dist.getMean());
        momentParameters.set(EF_Normal.EXPECTED_SQUARE,dist.getMean()*dist.getMean() + dist.getSd()*dist.getSd());
        ef_normal.setMomentParameters(momentParameters);
        return ef_normal;

    }


    public static Normal toDistribution(EF_Normal efNormal){

        Normal normal = new Normal(efNormal.getVariable());
        double mean = efNormal.getMomentParameters().get(EF_Normal.EXPECTED_MEAN);
        double sigma = efNormal.getMomentParameters().get(EF_Normal.EXPECTED_SQUARE) - mean*mean;

        normal.setMean(mean);
        normal.setSd(Math.sqrt(sigma));

        return normal;
    }


    public static EF_Multinomial toEFDistribution(Multinomial dist) {
        EF_Multinomial ef_multinomial = new EF_Multinomial(dist.getVariable());

        MomentParameters momentParameters = new MomentParameters(dist.getVariable().getNumberOfStates());

        for (int i=0; i<dist.getVariable().getNumberOfStates(); i++){
            momentParameters.set(i,dist.getProbabilityOfState(i));
        }

        ef_multinomial.setMomentParameters(momentParameters);

        return ef_multinomial;
    }

    public static Multinomial toDistribution(EF_Multinomial efmultinomial) {

        Multinomial multinomial = new Multinomial(efmultinomial.getVariable());

        for (int i=0; i<multinomial.getVariable().getNumberOfStates(); i++){
            multinomial.setProbabilityOfState(i, efmultinomial.getMomentParameters().get(i));
        }

        return multinomial;
    }

    }
