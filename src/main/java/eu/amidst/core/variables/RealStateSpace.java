package eu.amidst.core.variables;

import java.io.Serializable;

/**
 * Created by andresmasegosa on 25/11/14.
 */
public class RealStateSpace extends StateSpace implements Serializable {

    private static final long serialVersionUID = -5178769771928040023L;

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
