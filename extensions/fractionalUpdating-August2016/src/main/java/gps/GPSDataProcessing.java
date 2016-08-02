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

package gps;import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by andresmasegosa on 19/7/16.
 */
public class GPSDataProcessing {

    public static void createDataByWeek(String[] args) throws Exception{



        int LIMIT = Integer.parseInt(args[0]);

        int SKIP = Integer.parseInt(args[1]);

        String path = args[2];

        String output = args[3];

        for (int year = 2007; year <= 2012; year++) {
            for (int month = 0; month < 13; month++) {

                FileWriter fileWriter = new FileWriter(output+"_year_"+year+"_mont_"+month+".arff");

                fileWriter.write("@relation mixture-"+LIMIT+"\n");

                for (int i = 0; i < LIMIT; i++) {
                    fileWriter.write("@attribute GPSX_"+i+" real\n");
                    fileWriter.write("@attribute GPSY_"+i+" real\n");
                }

                fileWriter.write("@attribute DAY {1,2,3,4,5,6,7}\n");

                fileWriter.write("@data\n");

//                for (int i = 0; i < 182; i++) {
                for (int i = 0; i < 20; i++) {

                        String client=null;
                        if (i<10)
                            client="00"+i;
                        else if (i<100)
                            client="0"+i;
                        else
                            client=i+"";

                        File folder = new File(path + client + "/Trajectory/");
                        System.out.println(path + client + "/Trajectory/");

                        for (final String fileEntry : folder.list()) {
                            Path path1 = Paths.get(path + client + "/Trajectory/"+fileEntry);
                            Reader source = Files.newBufferedReader(path1);
                            BufferedReader reader = new BufferedReader(source);

                            Iterator<String> headerLines =  reader.lines().skip(6).iterator();
                            StringBuilder builder = new StringBuilder();
                            int count = 0;
                            int countLine = 0;

                            while (headerLines.hasNext()){
                                String line = headerLines.next();
                                countLine++;
                                if (countLine%SKIP!=0){
                                    continue;
                                }

                                String[] parts= line.split(",");

                                DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

                                format.parse(parts[5]);
                                int dayOfWeek =  format.getCalendar().get(Calendar.DAY_OF_WEEK);

                                if (year!=format.getCalendar().get(Calendar.YEAR))
                                    continue;

                                if (month!=format.getCalendar().get(Calendar.MONTH))
                                    continue;

                                if (count<LIMIT){
                                    builder.append(parts[0]+","+parts[1]+",");
                                    count++;
                                }else {
                                    fileWriter.write(builder.toString()+dayOfWeek+"\n");
                                    count=0;
                                    builder = new StringBuilder();
                                }

                            }


                            reader.close();
                            source.close();


                        }
                    }
                fileWriter.close();
            }
        }

    }

    public static void main(String[] args) throws Exception {

        args = new String[4];
        args[0]="1";
        args[1]="10";
        args[2]="/Users/andresmasegosa/Dropbox/Amidst/datasets/Geo/Data/";
        args[3]="/Users/andresmasegosa/Dropbox/Amidst/datasets/Geo/out/Mix";

        createDataByWeek(args);
    }

}
