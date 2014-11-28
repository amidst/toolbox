package eu.amidst.core.variables;

/**
 * Created by andresmasegosa on 25/11/14.
 */
public class RealStateSpace extends StateSpace{

    double minInterval;
    double maxInterval;


    public RealStateSpace() {
        super(StateSpaceType.REAL);
        minInterval = Double.NEGATIVE_INFINITY;
        maxInterval = Double.POSITIVE_INFINITY;
    }

    public RealStateSpace(double minInterval_, double maxInterval_) {
        super(StateSpaceType.REAL);
        this.maxInterval=maxInterval_;
        this.minInterval=minInterval_;
    }

    public double getMinInterval() {
        return minInterval;
    }

    public double getMaxInterval() {
        return maxInterval;
    }

}
