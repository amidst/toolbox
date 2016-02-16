package eu.amidst.modelExperiments;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Created by ana@cs.aau.dk on 09/02/16.
 */
public class ParseOutput {

    public static void main(String[] args) throws Exception {

        /*
         * Parse output to prepare data in colums
         */

        String pathInput = args[0];
        String pathOutput = args[1];

        PrintWriter out = new PrintWriter(pathOutput);
        try (BufferedReader br = new BufferedReader(new FileReader(pathInput))) {
            String line;
            String output = "";
            while ((line = br.readLine()) != null) {
                if(line.contains("Global bound at")) {
                    line = line.substring(line.indexOf("iteration:")+11);
                    String[] parts = line.split(" |,|>");
                    output += parts[0]+"\t"; // Iteration
                    output += "0\t"; //Percentage
                    output += parts[1]+"\t"; //Global bound
                    output += parts[2]; //Time
                    out.println(output);
                    output = "";
                    continue;
                }
                if(line.contains("Global bound is")) {
                    line = line.substring(line.indexOf("increasing:")+12);
                    String[] parts = line.split(" |,|>");
                    output += parts[0]+"\t"; // Iteration
                    output += parts[1]+"\t"; //Percentage
                    output += parts[2]+"\t"; //Global bound
                    output += parts[4]; //Time
                    out.println(output);
                    output = "";
                    continue;
                }
                if(line.contains("Global bound Conv")) {
                    line = line.substring(line.indexOf("Convergence:")+13);
                    String[] parts = line.split(" |,|>");
                    output += parts[0]+"\t"; // Iteration
                    output += parts[1]+"\t"; //Percentage
                    output += parts[2]+"\t"; //Global bound
                    output += parts[3]; //Time
                    out.println(output);
                    output = "";
                    continue;
                }
                if(line.contains("SVI ELBO")) {
                    line = line.substring(line.indexOf("ELBO:")+6,line.length()-8);
                    line = line.replaceAll("\\s+","");
                    String[] parts = line.split(",");
                    output += parts[0]+"\t"; // Iteration
                    output += parts[1]+"\t"; //Stepsize
                    output += parts[2]+"\t"; //Global bound
                    output += parts[3]; //Time
                    out.println(output);
                    output = "";
                    continue;
                }
            }
            out.close();
        }
    }
}
