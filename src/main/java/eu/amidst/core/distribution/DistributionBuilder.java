package eu.amidst.core.distribution;

import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public final class DistributionBuilder {

    private DistributionBuilder(){
        //Not called
    }
    public static ConditionalDistribution newDistribution(Variable mainVar, List<Variable> conditioningVars){

        if (mainVar.getDistributionType().compareTo(DistType.MULTINOMIAL)==0){
            return new Multinomial_MultinomialParents(mainVar, conditioningVars);
        }else if (mainVar.getDistributionType().compareTo(DistType.GAUSSIAN)==0) {
            boolean multinomialParents = false;
            boolean normalParents = false;
            boolean indicator = false;

            /* The parents of a gaussian variable are either multinomial and/or normal */
            for (Variable v : conditioningVars) {
                if (v.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0 || (v.getDistributionType().compareTo(DistType.MULTINOMIAL_LOGISTIC)==0)) {
                    multinomialParents = true;
                } else if (v.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {
                    normalParents = true;
                } else if (v.getDistributionType().compareTo(DistType.INDICATOR) == 0) {
                    indicator = true;
                } else {
                    throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
                }
            }

            if (indicator) {
                List<Variable> newconditioningVars = new ArrayList<>();
                Variable indicatorVar = null;
                for (Variable v : conditioningVars) {
                   if (v.getDistributionType().compareTo(DistType.INDICATOR) != 0) {
                       newconditioningVars.add(v);
                   }else{
                       indicatorVar=v;
                   }
                }

                if (!indicatorVar.getAttribute().equals(mainVar.getAttribute())) {
                    throw new IllegalArgumentException("The indicator var does not correspond to the main var.");
                }

                ConditionalDistribution dist = newDistribution(mainVar,newconditioningVars);

                return new Indicator(indicatorVar,dist);

            }else if (normalParents && !multinomialParents) {
                return new Normal_NormalParents(mainVar, conditioningVars);
            } else if ((!normalParents & multinomialParents) || (conditioningVars.size() == 0)) {
                return new Normal_MultinomialParents(mainVar, conditioningVars);
            } else if (normalParents & multinomialParents) {
                return new Normal_MultinomialNormalParents(mainVar, conditioningVars);
            } else {
                throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
            }
        }else if (mainVar.getDistributionType().compareTo(DistType.MULTINOMIAL_LOGISTIC)==0){
            return new Multinomial_LogisticParents(mainVar, conditioningVars);
        }else if (mainVar.getDistributionType().compareTo(DistType.INDICATOR)==0){
            return new Normal_MultinomialParents(mainVar, new ArrayList<>()); //A Normal by default is assigned to indicator variables.
        }else {
            throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
        }
    }
}
