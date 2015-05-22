#!/bin/bash


java -Xmx 5000M -cp "./lib/*" eu.amidst.scai2015.wrapperBN $1 >& out_SCAI2015_FS.txt &
java -Xmx 5000M -cp "./lib/*" eu.amidst.scai2015.wrapperBN $1 onlyPrediction >& out_SCAI2015_onlyPrecition.txt &
java -Xmx 5000M -cp "./lib/*" eu.amidst.scai2015.wrapperBN $1 onlyPrediction dynamic >& out_SCAI2015_onlyPrediction_dynamic.txt &