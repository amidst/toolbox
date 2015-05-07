#!/bin/bash
java -cp "../classes/artifacts/moalink_jar2/*" eu.amidst.ida2015.GlobalHiddenConceptDrift $1 100 0 >& SEA_HYPER_EXP/output_$2_W100.txt
java -cp "../classes/artifacts/moalink_jar2/*" eu.amidst.ida2015.GlobalHiddenConceptDrift $1 100 1 >& SEA_HYPER_EXP/output_$2_W100_NB.txt
java -cp "../classes/artifacts/moalink_jar2/*" eu.amidst.ida2015.GlobalHiddenConceptDrift $1 1000 0 >& SEA_HYPER_EXP/output_$2_W1000.txt
java -cp "../classes/artifacts/moalink_jar2/*" eu.amidst.ida2015.GlobalHiddenConceptDrift $1 1000 1 >& SEA_HYPER_EXP/output_$2_W1000_NB.txt
