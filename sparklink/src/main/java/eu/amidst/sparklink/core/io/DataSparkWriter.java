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

package eu.amidst.sparklink.core.io;

import eu.amidst.sparklink.core.data.DataSpark;
import org.apache.spark.sql.DataFrameWriter;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SaveMode;

/**
 * Created by rcabanas on 23/9/15.
 */
public class DataSparkWriter {

    public static void writeDataToFolder(DataSpark data, String path, SQLContext sqlContext) throws Exception {
        String formatFile = "";
        //Determine the format of the file
        if (path.endsWith(".json")) {
            formatFile = "json";
  //      }else if (path.endsWith(".jdbc")) {
  //          formatFile = "jdbc";
        }else if (path.endsWith(".parquet")) {
            formatFile = "parquet";

        } else {
            throw new IllegalArgumentException("Cannot determine the format of the file: use one built-in sources (json, parquet) or explicitily indicate it");
        }

		// Save it
		writeDataToFolder(data, path, sqlContext, formatFile);
    }

	public static void writeDataToFolder(DataSpark data, String path, SQLContext sqlContext, String formatFile) throws Exception {

		data.getDataFrame(sqlContext).write().mode(SaveMode.Overwrite).format(formatFile).save(path);
    }



}
