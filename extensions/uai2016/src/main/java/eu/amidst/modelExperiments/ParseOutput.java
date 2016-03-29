/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

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
        PrintWriter printer = null;
        if(args.length==2) {
            printer = new PrintWriter(args[1]);
        }


        try (BufferedReader br = new BufferedReader(new FileReader(pathInput))) {
            String line;
            String output = "";
            while ((line = br.readLine()) != null) {
                if(line.contains("Global bound at")) {
                    line = line.substring(line.indexOf("iteration:")+11);
                    String[] parts = line.split(" |,|>");
                    //output += parts[0]+"\t"; // Iteration
                    //output += "0\t"; //Percentage
                    output += parts[2]+"\t"; //Time
                    output += parts[1]; //Global bound
                    if(printer!=null)printer.println(output);
                    else System.out.println(output);
                    output = "";
                    continue;
                }
                if(line.contains("Global bound is")) {
                    line = line.substring(line.indexOf("increasing:")+12);
                    String[] parts = line.split(" |,|>|<");
                    //output += parts[0]+"\t"; // Iteration
                    //output += parts[1]+"\t"; //Percentage
                    if(parts.length>4)output += parts[4]+"\t"; //Time
                    output += parts[2]; //Global bound
                    if(printer!=null)printer.println(output);
                    else System.out.println(output);
                    output = "";
                    continue;
                }
                if(line.contains("Global bound Conv")) {
                    line = line.substring(line.indexOf("Convergence:")+13);
                    String[] parts = line.split(" |,|>");
                    //output += parts[0]+"\t"; // Iteration
                    //output += parts[1]+"\t"; //Percentage
                    output += parts[3]+"\t"; //Time
                    output += parts[2]; //Global bound Time
                    if(printer!=null)printer.println(output);
                    else System.out.println(output);
                    output = "";
                    continue;
                }
                if(line.contains("SVI ELBO")) {
                    line = line.substring(line.indexOf("ELBO:")+6,line.length()-8);
                    line = line.replaceAll("\\s+","");
                    line = line.replaceAll("seconds",",");
                    String[] parts = line.split(",");
                    //output += parts[0]+"\t"; // Iteration
                    //output += parts[1]+"\t"; //Stepsize
                    output += parts[5]+"\t"; //Time without global bound calculation
                    output += parts[2]+"\t"; //Global bound
                    //output += parts[4]; //Time without global bound calculation
                    if(printer!=null)printer.println(output);
                    else System.out.println(output);
                    output = "";
                    continue;
                }
            }
            if(printer!=null) printer.close();
        }
    }
}
