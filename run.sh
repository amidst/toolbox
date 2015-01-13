#!/bin/bash

java -Xmx5000M -cp "./huginlib/*:./target/*" -Djava.library.path="./huginlib/" $@
