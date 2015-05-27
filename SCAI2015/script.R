#####################################################
#GRAPH SELECTED ATTRIBUTES PER MONTH
#####################################################
setwd("~/Documents/Admist-MyFiles/Papers/SCAI2015/FirstResults/RScripts")
library(zoo)
path = 'selectedAtts.csv'
df <- read.table(path,header=FALSE,sep=",",fill=TRUE,colClasses=c(rep("factor",10)))
bm <- as.data.frame(setNames(replicate(11,numeric(0), simplify = F), c("VAR01", "VAR02","VAR03","VAR04","VAR07","VAR08","VAR10","VAR11","VAR12","VAR13","VAR14")))

vars<-c("VAR01", "VAR02","VAR03","VAR04","VAR07","VAR08","VAR10","VAR11","VAR12","VAR13","VAR14")

for (i in 1:71) {
  for (j in 1:ncol(bm)){
    bm[i,j] = 1 
  }
}
  
for (i in 1:nrow(df)) {
  for (j in 2:length(df[i,])) {
    bm[i,which(vars==df[i,j])] <- 0
  }
}

meses <- as.yearmon(seq(ISOdate(2007,05,01), by = "month", length.out = 71))
trimester <- meses[seq(1, length(meses), 3)]

postscript("selectedAttributes.eps", width=32.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
image(data.matrix(bm), axes = FALSE, col = c("black", "white"))
#abline(v = seq(0,1,by=1/70), col = "gray", lty = 2)
axis(2,at=seq(0,1,0.1),labels=c("VAR01", "VAR02","VAR03","VAR04","VAR05","VAR06","VAR07","VAR08","VAR09","VAR10","VAR11"), las=1) 
#axis(1,at=seq(0, 1,by=1/70),labels=meses, las=2)
axis(1,at=seq(0, 1,by=2/70),labels=meses[seq(1,71,by=2)], las=2)
#axis(1,at=seq(0, 1,by=3/70),labels=trimester, las=2)
dev.off()

#####################################################
#INCLUDING SOCIODEMOGRAPHIC VARIABLES
#####################################################
setwd("~/Documents/Admist-MyFiles/Papers/SCAI2015/FirstResults/RScripts")
library(zoo)
path = 'selectedAttsSocioD.csv'
df <- read.table(path,header=FALSE,sep=",",fill=TRUE,colClasses=c(rep("factor",10)))
bm <- as.data.frame(setNames(replicate(44,numeric(0), simplify = F), c("VAR01", "VAR02","VAR03","VAR04","VAR07","VAR08","VAR10","VAR11","VAR12","VAR13","VAR14","VAR17","VAR18","VAR19","VAR20","VAR24","VAR26","VAR27","VAR28","VAR29","VAR30","VAR31","VAR32","VAR33","VAR34","VAR35","VAR36","VAR37","VAR38","VAR39","VAR40","VAR41","VAR42","VAR43","VAR46","VAR47","VAR48","VAR54","VAR55","VAR58","VAR59","VAR60","VAR61","VAR62")))

vars<-c("VAR01", "VAR02","VAR03","VAR04","VAR07","VAR08","VAR10","VAR11","VAR12","VAR13","VAR14","VAR17","VAR18","VAR19","VAR20","VAR24","VAR26","VAR27","VAR28","VAR29","VAR30","VAR31","VAR32","VAR33","VAR34","VAR35","VAR36","VAR37","VAR38","VAR39","VAR40","VAR41","VAR42","VAR43","VAR46","VAR47","VAR48","VAR54","VAR55","VAR58","VAR59","VAR60","VAR61","VAR62")

for (i in 1:71) {
  for (j in 1:ncol(bm)){
    bm[i,j] = 1 
  }
}

for (i in 1:nrow(df)) {
  for (j in 2:length(df[i,])) {
    bm[i,which(vars==df[i,j])] <- 0
  }
}

#Remove rows with that only contain ones.


meses <- as.yearmon(seq(ISOdate(2007,05,01), by = "month", length.out = 71))
trimester <- meses[seq(1, length(meses), 3)]

postscript("selectedAttributes.eps", width=32.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
image(data.matrix(bm), axes = FALSE, col = c("black", "white"))
#abline(v = seq(0,1,by=1/70), col = "gray", lty = 2)
axis(2,at=seq(0,1,0.023),labels=c("VAR01", "VAR02","VAR03","VAR04","VAR05","VAR06","VAR07","VAR08","VAR09","VAR10","VAR11","SOC01","SOC02","SOC03","SOC04","SOC05","SOC06","SOC07","SOC08","SOC09","SOC10","SOC11","SOC12","SOC13","SOC14","SOC15","SOC16","SOC17","SOC18","SOC19","SOC20","SOC21","SOC22","SOC23","SOC24","SOC25","SOC26","SOC27","SOC28","SOC29","SOC30","SOC31","SOC32","SOC33"), las=1) 
#axis(1,at=seq(0, 1,by=1/70),labels=meses, las=2)
axis(1,at=seq(0, 1,by=2/70),labels=meses[seq(1,71,by=2)], las=2)
#axis(1,at=seq(0, 1,by=3/70),labels=trimester, las=2)
dev.off()

#####################################################
#GRAPH AUC - ROC
#####################################################
setwd("~/Documents/Admist-MyFiles/Papers/SCAI2015/FirstResults/RScripts")
library(zoo)
path = 'IDAdata.csv'
df = read.table(path, header=T, sep=",")
meses <- as.yearmon(seq(ISOdate(2007,05,01), by = "month", length.out = 71))

postscript("IDAdataAUC.eps", width=32.0 ,height=6.0, horizontal = FALSE, onefile = FALSE)
plot(df$NB.FS,xaxt="n",xlab='',type='l',main='',col='black', ylab="AUCROC",ylim=c(0.65,1))
lines(df$NB.DYNAMIC, col='blue', lty=2)
legend("bottomright", c("Dynamic NB with FS","Dynamic NB"), col=c("black","blue"),lty = c(1,2))
axis(1,at=seq(1, 71,by=2),labels=meses[seq(1,71,by=2)], las=2) 
#abline(v = seq(3,84,by=3), col = "gray", lty = 2)
dev.off()
