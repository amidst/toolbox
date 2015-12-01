/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package org.apache.spark.examples.mllib;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithSGD;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;

import java.util.regex.Pattern;

/**
 * Logistic regression based classification using ML Lib.
 */
public class JavaLR2 {

    static class ParsePoint implements Function<String, LabeledPoint> {
        private static final Pattern COMMA = Pattern.compile(",");
        private static final Pattern SPACE = Pattern.compile(" ");

        @Override
        public LabeledPoint call(String line) {
            String[] parts = COMMA.split(line);
            double y = Double.parseDouble(parts[0]);
            String[] tok = SPACE.split(parts[1]);
            double[] x = new double[tok.length];
            for (int i = 0; i < tok.length; ++i) {
                x[i] = Double.parseDouble(tok[i]);
            }
            return new LabeledPoint(y, Vectors.dense(x));
        }
    }

    public static void main(String[] args2) {
        String[] args = {"datasets/dataSpark/lr-data/random.data", "1", "4"};
        if (args.length != 3) {
            System.err.println("Usage: JavaLR2 <input_dir> <step_size> <niters>");
            System.exit(1);
        }
        SparkConf sparkConf = new SparkConf().setAppName("JavaLR2");//.setMaster("spark://localhost:7077");
        sparkConf.setMaster("local");
        //sparkConf.setSparkHome("/Users/andresmasegosa/Dropbox/Amidst/spark-1.1.1-bin-hadoop1/");
        sparkConf.setSparkHome("//Users/andresmasegosa/Dropbox/codigo/spark/");
        //sparkConf.validateSettings();
        JavaSparkContext sc = new JavaSparkContext(sparkConf);
        JavaRDD<String> lines = sc.textFile(args[0]);
        JavaRDD<LabeledPoint> points = lines.map(new ParsePoint()).cache();
        double stepSize = Double.parseDouble(args[1]);
        int iterations = Integer.parseInt(args[2]);

        // Another way to configure LogisticRegression
        //
        // LogisticRegressionWithSGD lr = new LogisticRegressionWithSGD();
        // lr.optimizer().setNumIterations(iterations)
        //               .setStepSize(stepSize)
        //               .setMiniBatchFraction(1.0);
        // lr.setIntercept(true);
        // LogisticRegressionModel model = lr.train(points.rdd());

        LogisticRegressionModel model = LogisticRegressionWithSGD.train(points.rdd(),
                iterations, stepSize);

        System.out.print("Final w: " + model.weights());

        sc.stop();
    }
}

