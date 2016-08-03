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

package gps;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 2/8/16.
 */
public class BCCDataProcessing {
    final static int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};

    public static void main(String[] args) throws IOException {
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/";
        String dataPathOut = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWekaNoPeakMonths/";

        String[] strings = new File(dataPath).list();
        Arrays.sort(strings);
        for (String string : strings) {
            if (!string.endsWith(".arff"))
                continue;
            int currentMonth = Integer.parseInt(string.substring(8,8+2));

            if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                continue;

            FileUtils.copyFile(new File(dataPath+string),new File(dataPathOut+string));

        }

    }
}
