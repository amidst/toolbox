package eu.amidst.core.variables;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by andresmasegosa on 25/11/14.
 */
public class MultinomialStateSpace extends StateSpace implements Iterable<String>{

    private int numberOfStates;
    private List<String> statesNames;


    public MultinomialStateSpace(int numberOfStates1) {
        super(StateSpaceType.FINITE_SET);
        this.numberOfStates=numberOfStates1;
        this.statesNames = new ArrayList<>();
        for (int i=0; i<numberOfStates1; i++){
            this.statesNames.add("State_"+i);
        }
    }

    public MultinomialStateSpace(List<String> statesNames1) {
        super(StateSpaceType.FINITE_SET);
        this.numberOfStates=statesNames1.size();
        this.statesNames = new ArrayList<>();
        for(String state: statesNames1) {
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
