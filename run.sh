#!/bin/bash

java -Xmx5000M -Djava.library.path="./huginlink/huginlib/" -cp "./huginlink/huginlib/*:./core/target/*:./examples/target/*:./moalink/target/*:./huginlink/target/*:./flinklink/target/*:./extensions/uai2016/target/*:./extensions/nips2016/target/*" $@
