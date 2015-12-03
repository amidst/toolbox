data <- read.table("data1.csv", header=TRUE, sep=",")
X1<-data$X1
Y1beta0<-data$beta0_batch1
X10<-data$X10
Y10beta0<-data$beta0_batch10
X100<-data$X100
Y100beta0<-data$beta0_bath100
X1000<-data$X1000
Y1000beta0<-data$beta0_bath1000
pdf("Data1_Beta0_varA_No1batch.pdf")
#plot(X1,Y1beta0,type="l",ylim=c(-1.6, 10.6), col="blue", main="Variable A (CLG)")
#lines(X10,Y10beta0, col="yellow")
plot(X10,Y10beta0,type="l",ylim=c(-1.6, -0.5), xlab = "sample", ylab = "beta0", col="blue", main="Variable A (CLG)")
lines(X100,Y100beta0, col="green")
lines(X1000,Y1000beta0, col="brown", type="o",pch=20)
lines(X1,data$beta0ML, col="red")
legend('topright', c("beta0OriginalBN", "beta0VB_batch10", "beta0VB_batch100", "beta0VB_batch1000"), 
       lty=1, col=c('red', 'blue', 'green', "brown"), bty='n', cex=.75)
dev.off()

data <- read.table("data1.csv", header=TRUE, sep=",")
X1<-data$X1
Y1beta1<-data$beta1_batch1
X10<-data$X10
Y10beta1<-data$beta1_batch10
X100<-data$X100
Y100beta1<-data$beta1_batch100
X1000<-data$X1000
Y1000beta1<-data$beta1_batch1000
pdf("Data1_Beta1_varA_No1batch.pdf")
#plot(X1,Y1beta1,type="l",ylim=c(-1.6, 10.6), col="blue", main="Variable A (CLG)")
plot(X1,Y1beta1,type="l",ylim=c(-0.43, 1.5), xlab = "sample", ylab = "beta1", col="blue", main="Variable A (CLG)")
lines(X10,Y10beta1, col="yellow")
lines(X100,Y100beta1, col="green")
lines(X1000,Y1000beta1, col="brown", type="o",pch=20)
lines(X1,data$beta1ML, col="red")
legend('topright', c("beta1OriginalBN", "beta1VB_batch1", "beta1VB_batch10", "beta1VB_batch100", "beta1VB_batch1000"), 
       lty=1, col=c('red', 'blue', 'yellow','green', "brown"), bty='n', cex=.75)
dev.off()

pdf("Data1_varB.pdf")
YsampleMeanB<-data$SamplemeanB[!is.na(data$SamplemeanB)]
YmeanB_batch1<-data$meanB_batch1[!is.na(data$meanB_batch1)]
YmeanB_batch100<-data$meanB_batch100
YmeanB_batch1000<-data$meanB_batch1000
YmeanB_batch10000<-data$meanB_batch10000
plot(X1[1:length(X1)],YsampleMeanB[1:length(YsampleMeanB)],type="l", col="blue", main="Variable B (normal)")
lines(X2,YmeanB_batch100, col="green")
lines(X3,YmeanB_batch1000, col="brown", type="p",pch=20)
lines(X1[10:length(X1)],YmeanB_batch1[10:length(X1)], col="red")
legend('topright', c("SampleMean", "meanVB_batch1", "meanVB_batch100", "meanVB_batch1000"),
       col=c('red', 'blue', 'green', "brown"), lty=c(1,1,1,NA), pch=c(NA,NA,NA,20), bty='n', cex=.75)
dev.off()