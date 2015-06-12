#Antonio F.
library(zoo)
setwd("~/Documents/core/IDA2015/CAJARSCRIPT2")
path = 'DataGlobalHidden.txt'

df = read.table(path, header=F, sep=",")

meses <- as.yearmon(seq(ISOdate(2007,04,01), by = "month", length.out = 84))

postscript(paste(path,".eps",sep=""), width=12.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
plot(df$V1, type='l', ylab=expression(H^{t} ~ "variable"),xlab='',
main='',col='black',xaxt="n")
axis(1, at=seq(3,84,by=3), lab=meses[seq(3,84,by=3)], las=2)  
abline(v = seq(3,84,by=3), col = "gray", lty = 2)
dev.off()


#Unemployment Rate GRAPH
path = 'UnemploymentRateAlmeria.txt'
path2 = 'UnemploymentRateSpain.txt'

df = read.table(path, header=F, sep=",")
df2 = read.table(path2, header=F, sep=",")


meses <- as.yearmon(seq(ISOdate(2007,04,01), by = "month", length.out = 84))
trimester <- meses[seq(3, length(meses), 3)]

postscript(paste("UnemployementRates.eps",sep=""), width=12.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
plot(na.approx(df$V1), type='l', ylab='',xlab='',
     main='',col='black',xaxt="n",ylim=c(7,40))
lines(df2$V1, col='blue', lty=2)
legend("bottomright", c("Unempl. rate in Almeria","Unempl. rate in Spain (non-seasonal)"), col=c("black","blue"),lty = c(1,2))
axis(1, at=seq(3,84,by=3), lab=meses[seq(3,84,by=3)], las=2)  
abline(v = seq(3,84,by=3), col = "gray", lty = 2)
#axis(1, at=seq(1,length(df$V1),by=1), lab=trimester, las=2)  
#abline(v = seq(1,length(df$V1),by=1), col = "gray", lty = 2)
dev.off()

#Unemployment Rate in Almeria GRAPH
path = 'UnemploymentRateAlmeriaCorrelationTri.txt'

df = read.table(path, header=F, sep=",")

meses <- as.yearmon(seq(ISOdate(2007,04,01), by = "month", length.out = 84))
trimester <- meses[seq(3, length(meses), 3)]

postscript(paste("UnemployementRatesAlmeria.eps",sep=""), width=12.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
plot(df$V1, type='l', ylab=expression(H^{t} ~ "variable"),xlab='',
     main='',col='black',xaxt="n")
#lines(df2$V1, col='blue', lty=2)
axis(1, at=seq(1,length(df$V1),by=1), lab=trimester, las=2)  
abline(v = seq(1,length(df$V1),by=1), col = "gray", lty = 2)
dev.off()


#GDP GRAPH
path = 'GDPEvolution.txt'

df = read.table(path, header=F, sep=",")

meses <- as.yearmon(seq(ISOdate(2007,04,01), by = "month", length.out = 84))
trimester <- meses[seq(3, length(meses), 3)]

postscript(paste(path,".eps",sep=""), width=12.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
plot(df$V1, type='l', ylab='',xlab='',
     main='',col='black',xaxt="n")
axis(1, at=seq(1,length(df$V1),by=1), lab=trimester, las=2)  
abline(v = seq(1,length(df$V1),by=1), col = "gray", lty = 2)
dev.off()


path = 'UnemploymentRateAlmeriaCorrelation.txt'
path2 = 'DataGlobalHiddenCorrelation.txt'
path3 = 'UnemploymentRateSpain.txt'

df = read.table(path, header=F, sep=",")
df2 = read.table(path2, header=F, sep=",")
df3 = read.table(path3, header=F, sep=",")

meses <- as.yearmon(seq(ISOdate(2007,04,01), by = "month", length.out = 84))

postscript("correlationLine.eps", width=12.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
#Unemployment Spain
linReg <- lm(df2$V1~df3$V1)
plot(df3$V1,df2$V1, type='p', ylab=expression(H^{t} ~ "variable"),xlab='Unemployement rate',
     main='',col='black')
abline(linReg)
unemploymentSpain <- df3$V1
globalHSpain <- df2$V1
pearsonCC <- cor(unemploymentSpain,globalHSpain)
spearmanCC <- cor(unemploymentSpain,globalHSpain,method="spearman")
dev.off()


#Unemployment in Almeria
postscript("correlationLine.eps", width=12.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
linReg <- lm(df2$V1~df$V1)
plot(df$V1,df2$V1, type='p', ylab=expression(H^{t} ~ "variable"),xlab='Unemployement rate',
     main='',col='black')
abline(linReg)
unemploymentAlmeria <- df$V1
globalHSpain <- df2$V1
pearsonCC <- cor(unemploymentAlmeria,globalHSpain)
spearmanCC <- cor(unemploymentAlmeria,globalHSpain,method="spearman")
dev.off()


RM <-c(2,4,6,8,10,12,14)
globalHLow <- globalH[-RM]
unemploymentLow <- unemployment[-RM]
linReg <- lm(globalHLow~unemploymentLow)
plot(unemploymentLow,globalHLow, type='p', ylab='',xlab='',
     main='',col='black')
abline(linReg)
pearsonCC <- cor(unemploymentLow,globalHLow)
spearmanCC <- cor(unemploymentLow,globalHLow,method="spearman")
