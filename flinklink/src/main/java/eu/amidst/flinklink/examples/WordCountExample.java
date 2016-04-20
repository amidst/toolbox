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
package eu.amidst.flinklink.examples;


import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//import org.apache.log4j.Logger;

/**
 * Created by andresmasegosa on 1/9/15.
 */
public class WordCountExample {
    static Logger logger = LoggerFactory.getLogger(WordCountExample.class);

    public static void main(String[] args) throws Exception {

        //BasicConfigurator.configure();
        PropertyConfigurator.configure(args[0]);

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        logger.info("Entering application.");

        DataSet<String> text = env.fromElements(
                "Who's there?",
                "I think I hear them. Stand, ho! Who's there?");

        List<Integer> elements = new ArrayList<Integer>();
        elements.add(0);


        DataSet<TestClass> set = env.fromElements(new TestClass(elements));


        DataSet<Tuple2<String, Integer>> wordCounts = text
                .flatMap(new LineSplitter())
                .withBroadcastSet(set, "set")
                .groupBy(0)
                .sum(1);


        //wordCounts.writeAsText("output.txt", FileSystem.WriteMode.OVERWRITE);
        wordCounts.map(new PrintTuple()).count();

    }

    public static class PrintTuple extends RichMapFunction<Tuple2<String, Integer>, Integer> {

        @Override
        public Integer map(Tuple2<String, Integer> t) throws Exception {

            System.out.println(t.f0 + "\t" + t.f1);
            return 0;

        }


    }

    public static class LineSplitter implements FlatMapFunction<String, Tuple2<String, Integer>> {

        static Logger loggerLineSplitter = LoggerFactory.getLogger(LineSplitter.class);

        @Override
        public void flatMap(String line, Collector<Tuple2<String, Integer>> out){

            loggerLineSplitter.info("Logger in LineSplitter.flatMap");
            for (String word : line.split(" ")) {
                out.collect(new Tuple2<String, Integer>(word, 1));
                //throw new RuntimeException("LineSplitter class called");
            }

        }
    }

    public static class TestClass implements Serializable {
        private static final long serialVersionUID = -2932037991574118651L;

        static Logger loggerTestClass = LoggerFactory.getLogger("TestClass.class");

        List<Integer> integerList;
        public TestClass(List<Integer> integerList){
            this.integerList=integerList;
            loggerTestClass.info("Logger in TestClass");
        }


    }
}