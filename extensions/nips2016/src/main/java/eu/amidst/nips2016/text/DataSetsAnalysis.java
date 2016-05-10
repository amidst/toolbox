/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.nips2016.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 9/5/16.
 */
public class DataSetsAnalysis {

    public static void main(String[] args) throws IOException {
        String pathVocab = "/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/vocab.kos.txt";
        String pathText = "/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/docword.kos.txt";

        List<String> words = Files.lines(Paths.get(pathVocab)).collect(Collectors.toList());


        System.out.print("site:dailykos.com ");
        String docIndex = 3+"";
        Files.lines(Paths.get(pathText)).skip(3)
                .map(line -> line.split(" "))
                .filter(array -> array[0].compareTo(docIndex)==0)
                .filter(array -> Integer.parseInt(array[2])>1)
                .forEach(array -> System.out.print(words.get(Integer.parseInt(array[1])-1)+" "));


    }
}
