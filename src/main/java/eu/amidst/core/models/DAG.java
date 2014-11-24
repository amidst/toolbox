package eu.amidst.core.models;

import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Hanen on 13/11/14.
 */
public class DAG {

    private StaticVariables variables;
    private ParentSet[] parents;

    public DAG(StaticVariables variables) {
        this.variables = variables;
        this.parents = new ParentSet[variables.getNumberOfVars()];

        for (Variable var: variables){
            parents[var.getVarID()] = new ParentSetImpl(var);
        }
    }

    public StaticVariables getStaticVariables(){
        return this.variables;
    }

    public ParentSet getParentSet(Variable var) {
        return parents[var.getVarID()];
    }

    public boolean containCycles(){

        boolean[] bDone = new boolean[this.variables.getNumberOfVars()];

        for (Variable var: this.variables){
            bDone[var.getVarID()] = false;
        }

        for (Variable var: this.variables){

            // find a node for which all parents are 'done'
            boolean bFound = false;

            for (Variable variable2: this.variables){
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
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
//        if (this == o) return true;
//
//        if (o == null || getClass() != o.getClass()) return false;
//
//        DAG dag = (DAG) o;
//
//    if (this.variables.getNumberOfVars() != dag.variables.getNumberOfVars()){
//        return false;
//    } else{
//        int i = 0;
//        boolean iguales = true;
//        while (i < this.variables.getNumberOfVars() && iguales){
//            if (this.getParentSet(this.variables.getListOfVariables().get(i)).equals(dag.getParentSet(dag.variables.getListOfVariables().get(i)))){
//                i++;
//            }else{
//                iguales = false;
//            }
//        }
//        return iguales;
 //   }
        return false;

}

    public String toString(){
        String str = "DAG\n";
        for (Variable var: this.getStaticVariables()){
            str+=var.getName() +" : "+this.getParentSet(var).toString() + "\n";
        }
        return str;
    }




    private class ParentSetImpl implements ParentSet {

        private Variable mainVar;
        private List<Variable> vars;

        private ParentSetImpl(Variable mainVar_){
            mainVar = mainVar_;
            this.vars = new ArrayList<Variable>();
        }

        public void addParent(Variable var){
            if (!Utils.isLinkCLG(mainVar,var))
                throw new IllegalArgumentException("Adding a Gaussian variable as parent of a Multinomial variable");

            if (this.contains(var))
                throw new IllegalArgumentException("Trying to add a duplicated parent");

            vars.add(var);
        }

        public void removeParent(Variable var){
            vars.remove(var);
        }

        public List<Variable> getParents(){
            return vars;
        }

        public int getNumberOfParents(){
            return vars.size();
        }

        @Override
        public String toString() {

            int numParents = getNumberOfParents();
            String str = new String("{ ");


            for(int i=0;i<numParents;i++){
                Variable parent = getParents().get(i);
                str = str + parent.getName();
                if (i<numParents-1)
                    str = str + ", ";
            }



            str = str + " }";
            return str;
        }

        /**
         * Is an ArrayList pointer to an ArrayList unmodifiable object still unmodifiable? I guess so right?
         */
        public void blockParents() {
            vars = Collections.unmodifiableList(vars);
        }

        public boolean contains(Variable var){
            return this.vars.contains(var);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            ParentSet parentset = (ParentSet) o;

            if (this.getNumberOfParents() != parentset.getNumberOfParents()){
                return false;
            } else{
                int i = 0;
                boolean iguales = true;
                while (i < this.getNumberOfParents() && iguales){
                    if (this.getParents().get(i).equals(parentset.getParents().get(i))){
                        i++;
                    }else{
                        iguales = false;
                    }
                }
                return iguales;
            }
        }
    }
}
