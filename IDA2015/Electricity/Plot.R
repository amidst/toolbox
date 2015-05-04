df = read.table('/Users/ana/Documents/Admist-MyFiles/Papers/IDA2015/ExperimentsElectricity/ElecOriginal.csv', header=T, sep=",")
#Pre-processing
dfp = cbind(read.fwf(file = textConnection(as.character(df[, 1])), 
                       widths = c(2, 2, 2), colClasses = "character", 
                       col.names = c("Year","Month", "Day")), 
                       df[-1])
aux1 <- dfp$Month[25153:25488]
aux2 <- dfp$Month[34897:35232]
aux3 <- dfp$Month[35905:36240]
aux4 <- dfp$Month[40609:40944]
dfp$Month[25153:25488] <- dfp$Day[25153:25488]
dfp$Day[25153:25488] <- aux1
dfp$Month[34897:35232] <- dfp$Day[34897:35232]
dfp$Day[34897:35232] <- aux2
dfp$Month[35905:36240] <- dfp$Day[35905:36240]
dfp$Day[35905:36240] <- aux3
dfp$Month[40609:40944] <- dfp$Day[40609:40944]
dfp$Day[40609:40944] <- aux4

#NSWDEMAND
meanNswDemand <- aggregate(dfp$nswdemand, list(dfp$class,dfp$Month,dfp$Year), mean) 
meanNswDemandUP <- split(meanNswDemand,meanNswDemand$Group.1)[[2]][2:4] 
slots <- length(meanNswDemandUP$x)
oddlabs <- seq(1,slots,2)
lablist <- paste(meanNswDemandUP$Group.2[1:slots],"/",meanNswDemandUP$Group.3[1:slots],sep="")
lablist <- lablist[oddlabs]
pdf(paste("/Users/ana/Documents/Admist-MyFiles/Papers/IDA2015/ExperimentsElectricity/nswDemandAttPerMonthUP.pdf"))
plot(1:slots,meanNswDemandUP$x,xaxt = "n", xlab='Time', ylab='NSW demand',type="b", main="UP")
axis(1, at=1:slots, labels=FALSE)
text(seq(1,slots,2), par("usr")[3] - 40.2, labels = lablist, srt = 90, pos = 2, offset = -0.1, xpd = TRUE)
dev.off()
meanNswDemandDOWN <- split(meanNswDemand,meanNswDemand$Group.1)[[1]][2:4]
pdf(paste("/Users/ana/Documents/Admist-MyFiles/Papers/IDA2015/ExperimentsElectricity/nswDemandAttPerMonthDOWN.pdf"))
plot(1:slots,meanNswDemandDOWN$x,xaxt = "n", xlab='Time', ylab='NSW demand',type="b", main="DOWN")
axis(1, at=1:slots, labels=FALSE)
text(seq(1,slots,2), par("usr")[3] - 40.2, labels = lablist, srt = 90, pos = 2, offset = -0.1, xpd = TRUE)
dev.off()

#NSWPRICE
meanNswPrice <- aggregate(dfp$nswprice, list(dfp$class,dfp$Month,dfp$Year), mean) 
meanNswPriceUP <- split(meanNswPrice,meanNswPrice$Group.1)[[2]][2:4] 
slots <- length(meanNswPriceUP$x)
oddlabs <- seq(1,slots,2)
lablist <- paste(meanNswPriceUP$Group.2[1:slots],"/",meanNswPriceUP$Group.3[1:slots],sep="")
lablist <- lablist[oddlabs]
pdf(paste("/Users/ana/Documents/Admist-MyFiles/Papers/IDA2015/ExperimentsElectricity/nswPriceAttPerMonthUP.pdf"))
plot(1:slots,meanNswPriceUP$x,xaxt = "n", xlab='Time', ylab='NSW Price',type="b", main="UP")
axis(1, at=1:slots, labels=FALSE)
text(seq(1,slots,2), par("usr")[3] - 40.2, labels = lablist, srt = 90, pos = 2, offset = -0.1, xpd = TRUE)
dev.off()
meanNswPriceDOWN <- split(meanNswPrice,meanNswPrice$Group.1)[[1]][2:4]
pdf(paste("/Users/ana/Documents/Admist-MyFiles/Papers/IDA2015/ExperimentsElectricity/nswPriceAttPerMonthDOWN.pdf"))
plot(1:slots,meanNswPriceDOWN$x,xaxt = "n", xlab='Time', ylab='NSW Price',type="b", main="DOWN")
axis(1, at=1:slots, labels=FALSE)
text(seq(1,slots,2), par("usr")[3] - 40.2, labels = lablist, srt = 90, pos = 2, offset = -0.1, xpd = TRUE)
dev.off()