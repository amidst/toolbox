package eu.amidst.core.hugin_interface;
import COM.hugin.HAPI.*;
import java.util.ListIterator;




public class Converter
{









    /**
     * This function parses the given NET file, compiles the network,
     * and prints the prior beliefs and expected utilities of all
     * nodes.  If a case file is given, the function loads the file,
     * propagates the evidence, and prints the updated results.
     *
     * If the network is a LIMID, we assume that we should compute
     * policies for all decisions (rather than use the ones specified
     * in the NET file).  Likewise, we update the policies when new
     * evidence arrives.
     */
    public static void LAP (String netName, String caseName)
    {
        try {

            //Load a network from a .net file
            ParseListener parseListener = new DefaultClassParseListener();
            Domain domain = new Domain (netName + ".net", parseListener);

            //Extract the information from the network
            NodeList nodeList = domain.getNodes();
            NodeList parents;
            Node node, parent;
            boolean continuousParents, discreteParents;
            ListIterator it1 = nodeList.listIterator();
            ListIterator it2;

            while (it1.hasNext()) {
                node = (Node) it1.next();
                parents = node.getParents();

                if(node.getKind().compareTo(NetworkModel.H_KIND_DISCRETE)==0){
                     System.out.println("Multinomial_MultinomialParents distribution");
                }
                else {
                    continuousParents=false;
                    discreteParents=false;
                    it2 = parents.listIterator();

                    while (it2.hasNext()) {
                        parent = (Node) it2.next();
                        if (parent.getKind().compareTo(NetworkModel.H_KIND_DISCRETE)==0){
                            discreteParents=true;
                        }
                        else {
                            continuousParents=true;
                        }
                    }

                    if(discreteParents & !continuousParents){
                        System.out.println("Normal_MultinomialParents distribution");
                    }
                    if(!discreteParents & continuousParents){
                        System.out.println("Normal_NormalParents distribution");
                    }
                    if(discreteParents & continuousParents){
                        System.out.println("Normal_MultinomialNormalParents distribution");
                    }
                }
                System.out.print(" (" + node.getName() + ")");
                System.out.println(" -  Parents: "+ node.getParents().toString());
                System.out.println();
            }

            System.out.println("CONDITIONAL DISTRIBUTIONS");

            //for (int j=0;j<node.getTable().getData().length;j++){
            //    System.out.print(node.getTable().getData()[j] + " ");
            //}




            //Simulate from a network ?? Where the sample is stored?
            //domain.setNumberOfCases(10);
            //domain.simulate(); //???
            //System.out.println(domain.getNumberOfCases());




            domain.openLogFile (netName + ".log");
            domain.triangulate (Domain.H_TM_BEST_GREEDY);
            domain.compile();
            domain.closeLogFile();



            boolean hasUtilities = containsUtilities (domain.getNodes());

            if (!hasUtilities)
                System.out.println ("Prior beliefs:");
            else {
                domain.updatePolicies();
                System.out.println ("Overall expected utility: "
                        + domain.getExpectedUtility());
                System.out.println ();
                System.out.println ("Prior beliefs (and expected utilities):");
            }
            printBeliefsAndUtilities (domain);

            if (caseName != null) {
                domain.parseCase (caseName, parseListener);

                System.out.println ();
                System.out.println ();
                System.out.println ("Propagating the evidence specified in \""
                        + caseName + "\"");

                domain.propagate (Domain.H_EQUILIBRIUM_SUM,
                        Domain.H_EVIDENCE_MODE_NORMAL);

                System.out.println ();
                System.out.println ("P(evidence) = "
                        + domain.getNormalizationConstant());
                System.out.println ();

                if (!hasUtilities)
                    System.out.println ("Updated beliefs:");
                else {
                    domain.updatePolicies();
                    System.out.println ("Overall expected utility: "
                            + domain.getExpectedUtility());
                    System.out.println ();
                    System.out.println ("Updated beliefs (and expected utilities):");
                }
                printBeliefsAndUtilities (domain);
            }

            domain.delete();
        } catch (ExceptionHugin e) {
            System.out.println ("Exception caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println ("General exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void processMultinomial() {


    }





    /**
     * Print the beliefs and expected utilities of all nodes in the domain.
     */
    public static void printBeliefsAndUtilities (Domain domain)
            throws ExceptionHugin
    {
        NodeList nodes = domain.getNodes();
        boolean hasUtilities = containsUtilities (nodes);
        java.util.ListIterator it = nodes.listIterator();

        while (it.hasNext()) {
            Node node = (Node) it.next();

            System.out.println ();
            System.out.println (node.getLabel() + " (" + node.getName() + ")");

            if (node instanceof UtilityNode)
                System.out.println ("  - Expected utility: "
                        + ((UtilityNode) node).getExpectedUtility());
            else if (node instanceof FunctionNode)
                try {
                    System.out.println ("  - Value: "
                            + ((FunctionNode) node).getValue());
                } catch (ExceptionHugin e) {
                    System.out.println ("  - Value: N/A");
                }
            else if (node instanceof DiscreteNode) {
                DiscreteNode dNode = (DiscreteNode) node;

                for (int i = 0, n = (int) dNode.getNumberOfStates(); i < n; i++) {
                    System.out.print ("  - " + dNode.getStateLabel (i)
                            + " " + dNode.getBelief (i));
                    if (hasUtilities)
                        System.out.println (" (" + dNode.getExpectedUtility (i) + ")");
                    else
                        System.out.println();
                }
            } else {
                ContinuousChanceNode ccNode = (ContinuousChanceNode) node;

                System.out.println ("  - Mean : " + ccNode.getMean());
                System.out.println ("  - SD   : " + Math.sqrt (ccNode.getVariance()));
            }
        }
    }

    /**
     * Are there utility nodes in the list?
     */
    public static boolean containsUtilities (NodeList list)
    {
        java.util.ListIterator it = list.listIterator();

        while (it.hasNext())
            if (it.next() instanceof UtilityNode)
                return true;

        return false;
    }

    /**
     * Load a Hugin NET file, compile the network, and print the
     * results.  If a case file is specified, load it, propagate the
     * evidence, and print the results.
     */
    public static void main (String args[])
    {
        if (args.length == 1)
            LAP (args[0], null);
        else if (args.length == 2)
            LAP (args[0], args[1]);
        else
            System.err.println ("Usage: <netName> [<caseName>]");
    }
}
