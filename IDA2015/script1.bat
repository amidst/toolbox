@echo off

java -Xmx5000M  -classpath "./*"  moa.DoTask EvaluatePrequential -l \(bayes.amidstModels -d GLOBAL -w 100 -v 0.1\) -s \(ArffFileStream -f \(%1\)\) -f 1000 > script1_global_w100_v0.1.txt
java -Xmx5000M  -classpath "./*"  moa.DoTask EvaluatePrequential -l \(bayes.amidstModels -d LOCAL -w 100 -v 0.1\) -s \(ArffFileStream -f \(%1\)\) -f 1000 > script1_local_w100_v0.1.txt