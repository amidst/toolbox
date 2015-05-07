#Antonio F.
library(zoo)
setwd("~/Documents/core/IDA2015/CAJARSCRIPT2")
path = 'DataGlobalHidden.txt'

df = read.table(path, header=F, sep=",")

meses <- as.yearmon(seq(ISOdate(2007,04,01), by = "month", length.out = 84))

postscript(paste(path,".eps",sep=""), width=12.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
plot(df$V1, type='l', ylab='',xlab='',
main='',col='black',xaxt="n")
axis(1, at=seq(3,84,by=3), lab=meses[seq(3,84,by=3)], las=2)  
abline(v = seq(3,84,by=3), col = "gray", lty = 2)
dev.off()

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
