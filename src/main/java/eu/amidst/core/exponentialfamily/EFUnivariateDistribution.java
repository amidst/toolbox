package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.header.Variable;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public interface EFUnivariateDistribution {

    public Variable getMainVariable();

    public double getLogProbability(double val);

    public SufficientStatistics getSufficientStatistics(DataInstance data);

    public NaturalParameters getNaturalParameters();

    public MomentParameters getMomentParameters();
}
