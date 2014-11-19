package eu.amidst.core.modelstructure;

import eu.amidst.core.header.StaticModelHeader;
import eu.amidst.core.header.Variable;

import java.util.List;

/**
 * Created by Hanen on 13/11/14.
 */
public class DAG {

    private StaticModelHeader modelHeader;
    private ParentSet[] parents;
    private boolean[] m_bits;
    /* bit representation of parent sets m_bits[parentNode + childNode * totalNbrOfNodes]
    represents the directed arc from the parentNode to the childNode */

    public DAG(StaticModelHeader modelHeader) {
        this.modelHeader = modelHeader;
        this.parents = new ParentSet[modelHeader.getNumberOfVars()];

        for (int i=0;i<modelHeader.getNumberOfVars();i++) {
            parents[i] = ParentSet.newParentSet();
        }
        this.m_bits = new boolean [modelHeader.getNumberOfVars() * modelHeader.getNumberOfVars()];
    }

    public ParentSet getParentSet(Variable var) {
        return parents[var.getVarID()];
    }


    public boolean containCycles2(){

        boolean[] bDone = new boolean[this.modelHeader.getNumberOfVars()];

        for (Variable var: this.modelHeader.getVariables()){
            bDone[var.getVarID()] = false;
        }

        for (Variable var: this.modelHeader.getVariables()){

            // find a node for which all parents are 'done'
            boolean bFound = false;

            for (Variable variable2: this.modelHeader.getVariables()){
                if (!bDone[variable2.getVarID()]) {
                    boolean bHasNoParents = true;

                    for (Variable parent: this.getParentSet(variable2).getParents()){
                        if (!bDone[parent.getVarID()]) {
                            bHasNoParents = false;
                        }
                    }

                    if (bHasNoParents) {
                        bDone[variable2.getVarID()] = true;
                        bFound = true;
                        break;
                    }
                }
            }

            if (!bFound) {
                return false;
            }
        }

        return true;
    }


    public boolean containCycles(){
       /* check whether there are cycles in the BN */

        int nbrNodes = modelHeader.getNumberOfVars();

        boolean[] checked = new boolean[nbrNodes];

        for(int i= 0; i<nbrNodes; i++){

            boolean isCycle = false;

            for( int j =0; (!isCycle && j < nbrNodes); j++){

                if (!checked[j]){
                    boolean hasNoParents = true;
                    for (int par = 0; par < nbrNodes; par++){
                        if (m_bits[par + j * nbrNodes] && !checked[par]){
                            hasNoParents = false;
                        }
                    }
                    if(hasNoParents){
                        checked[j] = true;
                        isCycle = true;
                    }
                }
            }

            if(!isCycle){
                return true;
            }
        }
        return false;
    }

    public StaticModelHeader getModelHeader(){
        return this.modelHeader;
    }

    public void addParent(Variable child, Variable parent) {
        this.getParentSet(child).addParent(parent);
        this.m_bits[parent.getVarID() + child.getVarID() * modelHeader.getNumberOfVars()] = true;
    }

    public void removeParent(Variable child, Variable parent) {
        this.getParentSet(child).removeParent(parent);
        this.m_bits[parent.getVarID() + child.getVarID() * modelHeader.getNumberOfVars()] = false;
    }

    public int getNumberOfArcs(){
        int counter = 0;
        for (int i = 0; i < m_bits.length; i++) {
                if (m_bits[i]) {
                    counter++;
                }
        }
        return counter;
    }
}
