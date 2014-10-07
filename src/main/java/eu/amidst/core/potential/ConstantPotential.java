package eu.amidst.core.potential;

import java.util.List;

/**
 * Created by andresmasegosa on 28/08/14.
 */
public class ConstantPotential implements Potential {
    double val;
    public ConstantPotential(double val){
        this.val=val;
    }
    @Override
    public void setVariables(List variables) {

    }

    @Override
    public List getVariables() {
        return null;
    }

    @Override
    public void combine(Potential pot) {

    }

    @Override
    public void marginalize(List variables) {

    }
}
