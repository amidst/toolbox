library(xlsx)
library(zoo)

setwd("E:/KEPLER/OTROS/AMIDST/IDA2015/Tratamiento/GraficasReferees")
datos <- read.xlsx(file="AccRocPlot.xlsx",1)

meses <- as.yearmon(seq(ISOdate(2007,04,01), by = "month", length.out = 84))

accuracy <- scale(datos[,3])
roc <- scale(datos[,4])

# Accuracy
postscript("accuracy.eps",width=12.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
plot(accuracy, type='l', ylab='',xlab='',  main='',col='black',xaxt="n")
axis(1, at=seq(3,84,by=3), lab=meses[seq(3,84,by=3)], las=2)  
abline(v = seq(3,84,by=3), col = "gray", lty = 2)
dev.off()

# ROC
postscript("roc.eps", width=12.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
plot(roc, type='l', ylab='',xlab='',  main='',col='black',xaxt="n")
axis(1, at=seq(3,84,by=3), lab=meses[seq(3,84,by=3)], las=2)  
abline(v = seq(3,84,by=3), col = "gray", lty = 2)
dev.off()
