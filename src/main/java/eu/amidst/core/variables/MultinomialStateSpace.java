package eu.amidst.core.variables;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by andresmasegosa on 25/11/14.
 */
public class MultinomialStateSpace extends StateSpace implements Iterable<String>{

    int numberOfStates;
    List<String> statesNames;


    public MultinomialStateSpace(int numberOfStates_) {
        super(StateSpaceType.FINITE_SET);
        this.numberOfStates=numberOfStates_;
        this.statesNames = new ArrayList<>();
        for (int i=0; i<this.statesNames.size(); i++){
            this.statesNames.add("State_"+i);
        }
    }

    public MultinomialStateSpace(List<String> statesNames_) {
        super(StateSpaceType.FINITE_SET);
        this.numberOfStates=statesNames_.size();
        this.statesNames = new ArrayList<>();
        for(String state: statesNames_) {
            this.statesNames.add(state);
        }
    }

    public int getNumberOfStates() {
        return numberOfStates;
    }

    public String getStatesName(int state) {
        return statesNames.get(state);
    }

    public int getIndexOfState(String stateName) { return statesNames.indexOf(stateName);}

    @Override
    public Iterator<String> iterator() {
        return statesNames.iterator();
    }
}
