package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;

/**
 * Created by ana@cs.aau.dk on 27/01/15.
 */
public interface ToEFConditionalDistribution {
    public EF_ConditionalDistribution toEFConditionalDistribution();
}
