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

#Aggregate data by day
meanPerDayNsw <- aggregate(dfp$nswdemand, list(dfp$Day,dfp$Month,dfp$Year), mean) 
colnames(meanPerDayNsw) <- c("Day","Month","Year","nswdemand")
meanPerDayNswPrice <- aggregate(dfp$nswprice, list(dfp$Day,dfp$Month,dfp$Year), mean)
meanPerDayNsw$nswprice <- meanPerDayNswPrice$x
#Add TIME_ID to Dataset
timeID = rep(0,nrow(meanPerDayNsw))
year = meanPerDayNsw$Year[1]
month = meanPerDayNsw$Month[1]
for (i in 2 : nrow(meanPerDayNsw)){
  if ((meanPerDayNsw$Year[i]!=year) || (meanPerDayNsw$Month[i]!=month)){
    year = meanPerDayNsw$Year[i]
    month = meanPerDayNsw$Month[i]
    timeID[i] = timeID[i-1]+1
    print(timeID[i])
  }else{
    timeID[i] = timeID[i-1]
  }
}
meanPerDayNsw$TIME_ID <- timeID
meanPerDayNsw$class <- rep("UP",length(meanPerDayNsw$TIME_ID))
meanPerDayNsw <- meanPerDayNsw[,c("TIME_ID","nswdemand","nswprice","class")]
write.csv(meanPerDayNsw, file = "electricityAggPerDayTIMEID.csv",row.names=FALSE)

#Add TIME_ID to Dataset
timeID = rep(0,nrow(dfp))
year = dfp$Year[1]
month = dfp$Month[1]
for (i in 2 : nrow(dfp)){
  if ((dfp$Year[i]!=year) || (dfp$Month[i]!=month)){
    year = dfp$Year[i]
    month = dfp$Month[i]
    timeID[i] = timeID[i-1]+1
    print(timeID[i])
  }else{
    timeID[i] = timeID[i-1]
  }
}
dfp$TIME_ID <- timeID

data <- dfp[,c("nswprice","nswdemand","class","TIME_ID")]
write.csv(data, file = "electricityTIMEID.csv",row.names=FALSE)

#Plot NSWDEMAND
meanNswDemand <- aggregate(dfp$nswdemand, list(dfp$class,dfp$Month,dfp$Year), var) 
meanNswDemandUP <- split(meanNswDemand,meanNswDemand$Group.1)[[2]][2:4] 
slots <- length(meanNswDemandUP$x)
oddlabs <- seq(1,slots,2)
lablist <- paste(meanNswDemandUP$Group.2[1:slots],"/",meanNswDemandUP$Group.3[1:slots],sep="")
lablist <- lablist[oddlabs]
pdf(paste("/Users/ana/Documents/Admist-MyFiles/Papers/IDA2015/ExperimentsElectricity/nswDemandAttPerMonthUPvar.pdf"))
plot(1:slots,meanNswDemandUP$x,xaxt = "n", xlab='Time', ylab='NSW demand',type="b", main="UP")
axis(1, at=1:slots, labels=FALSE)
text(seq(1,slots,2), par("usr")[3] - 40.2, labels = lablist, srt = 90, pos = 2, offset = -0.1, xpd = TRUE)
dev.off()
meanNswDemandDOWN <- split(meanNswDemand,meanNswDemand$Group.1)[[1]][2:4]
pdf(paste("/Users/ana/Documents/Admist-MyFiles/Papers/IDA2015/ExperimentsElectricity/nswDemandAttPerMonthDOWNvar.pdf"))
plot(1:slots,meanNswDemandDOWN$x,xaxt = "n", xlab='Time', ylab='NSW demand',type="b", main="DOWN")
axis(1, at=1:slots, labels=FALSE)
text(seq(1,slots,2), par("usr")[3] - 40.2, labels = lablist, srt = 90, pos = 2, offset = -0.1, xpd = TRUE)
dev.off()

#Plot NSWPRICE
meanNswPrice <- aggregate(dfp$nswprice, list(dfp$class,dfp$Month,dfp$Year), var) 
meanNswPriceUP <- split(meanNswPrice,meanNswPrice$Group.1)[[2]][2:4] 
slots <- length(meanNswPriceUP$x)
oddlabs <- seq(1,slots,2)
lablist <- paste(meanNswPriceUP$Group.2[1:slots],"/",meanNswPriceUP$Group.3[1:slots],sep="")
lablist <- lablist[oddlabs]
pdf(paste("/Users/ana/Documents/Admist-MyFiles/Papers/IDA2015/ExperimentsElectricity/nswPriceAttPerMonthUPvar.pdf"))
plot(1:slots,meanNswPriceUP$x,xaxt = "n", xlab='Time', ylab='NSW Price',type="b", main="UP")
axis(1, at=1:slots, labels=FALSE)
text(seq(1,slots,2), par("usr")[3] - 40.2, labels = lablist, srt = 90, pos = 2, offset = -0.1, xpd = TRUE)
dev.off()
meanNswPriceDOWN <- split(meanNswPrice,meanNswPrice$Group.1)[[1]][2:4]
pdf(paste("/Users/ana/Documents/Admist-MyFiles/Papers/IDA2015/ExperimentsElectricity/nswPriceAttPerMonthDOWNvar.pdf"))
plot(1:slots,meanNswPriceDOWN$x,xaxt = "n", xlab='Time', ylab='NSW Price',type="b", main="DOWN")
axis(1, at=1:slots, labels=FALSE)
text(seq(1,slots,2), par("usr")[3] - 40.2, labels = lablist, srt = 90, pos = 2, offset = -0.1, xpd = TRUE)
dev.off()