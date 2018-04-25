#!/bin/bash
#This script sample the uai100M.arff dataset to preserve concept drift
#Each csv file contains 10K samples
# $1 is the number of files per month
# $2 is the number of resulting samples = 10.000*100*$1 = 1M*files_per_month
# $3 is the number of samples per file (if all of them, then 10000)
mkdir uai${2}.arff
mkdir uai${2}.arff/data
cp uai100M.arff/name.txt uai${2}.arff/
cp uai100M.arff/attributes.txt uai${2}.arff/
for i in $(eval echo {1..$1});
do
   for j in {1..100}
   do
        head -n${3} uai100M.arff/data/bank_data_active_IDA${j}_${i}.csv >> uai${2}.arff/data/bank_data_active_IDA_${1}_files.csv
   done
done