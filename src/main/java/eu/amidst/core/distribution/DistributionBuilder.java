package eu.amidst.core.distribution;

import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class DistributionBuilder {

    public static ConditionalDistribution newDistribution(Variable mainVar, List<Variable> conditioningVars){

        if (mainVar.getDistributionType().compareTo(DistType.MULTINOMIAL)==0){
            return new Multinomial_MultinomialParents(mainVar, conditioningVars);
        }
        else if (mainVar.getDistributionType().compareTo(DistType.GAUSSIAN)==0) {

            boolean multinomialParents = false;
            boolean normalParents = false;
            /* The parents of a gaussian variable are either multinomial and/or normal */
            for (Variable v : conditioningVars) {
                if (v.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                    multinomialParents = true;
                } else if (v.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {
                    normalParents = true;
                } else {
                    throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
                }
            }
            if (normalParents && !multinomialParents) {
                return new Normal_NormalParents(mainVar, conditioningVars);
            } else if ((!normalParents & multinomialParents) || (conditioningVars.size() == 0)) {
                return new Normal_MultinomialParents(mainVar, conditioningVars);
            } else if (normalParents & multinomialParents) {
                return new Normal_MultinomialNormalParents(mainVar, conditioningVars);
            } else {
                throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
            }
        }
        else {
            throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
        }
    }
}
