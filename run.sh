#!/bin/bash

java -Xmx5000M -Djava.library.path="./huginlink/huginlib/" -cp "./huginlink/huginlib/*:./core/target/*:./examples/target/*:./moalink/target/*:./huginlink/target/*" $@
