package eu.amidst.core.variables;

/**
 * Created by andresmasegosa on 25/11/14.
 */
public class RealStateSpace extends StateSpace{

    private double minInterval;
    private double maxInterval;


    public RealStateSpace() {
        super(StateSpaceType.REAL);
        minInterval = Double.NEGATIVE_INFINITY;
        maxInterval = Double.POSITIVE_INFINITY;
    }

    public RealStateSpace(double minInterval1, double maxInterval1) {
        super(StateSpaceType.REAL);
        this.maxInterval=maxInterval1;
        this.minInterval=minInterval1;
    }

    public double getMinInterval() {
        return minInterval;
    }

    public double getMaxInterval() {
        return maxInterval;
    }

}
