data <- read.csv(file="/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/data1M.csv",head=TRUE,sep=",")

pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/1MCajaMarData.pdf"),width=7, height=5)
valmax<-max(c(data$X100L,data$X1000L,data$X2000L,data$X5000L),na.rm = TRUE)
valmin<-min(data$X100L,data$X1000L,data$X2000L,data$X5000L,na.rm = TRUE)
plot(data$X100T,data$X100L,type="o", ylim = c(valmin,valmax),pch=18,
     xlab="Time (seconds)", ylab="Global lower bound",
     col=terrain.colors(3)[1], main="1M points - 12M node BN - all epochs")
lines(data$X1000T,data$X1000L,type="o",pch=18, col="brown")
lines(data$X2000T,data$X2000L,type="o",pch=18, col="orange")
lines(data$X5000T,data$X5000L,type="o",pch=18, col="blue")
legend("bottomright", inset=.05, title="Batch Size",
       c("100","1000","2000","5000"), fill=c(terrain.colors(3)[1],"brown","orange","blue"), horiz=FALSE)
dev.off()


pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/1MCajaMarDataEvery5Points.pdf"),width=7, height=5)
select <- seq_len(50) %% 5 == 0
valmax<-max(c(data$X100L[select],data$X1000L[select],data$X2000L[select],data$X5000L[select]),na.rm = TRUE)
valmin<-min(data$X100L[select],data$X1000L[select],data$X2000L[select],data$X5000L[select],na.rm = TRUE)
plot(data$X100T[select],data$X100L[select],type="o", ylim = c(valmin,valmax), pch=18,xlab="Time (seconds)", 
     ylab="Global lower bound", col=terrain.colors(3)[1], main="1M points - 12M node BN - 1 epoch")
lines(data$X1000T[select],data$X1000L[select],type="o",pch=18, col="brown")
lines(data$X2000T[select],data$X2000L[select],type="o",pch=18, col="orange")
lines(data$X5000T[select],data$X5000L[select],type="o",pch=18, col="blue")
legend("bottomright", inset=.05, title="Batch Size",
       c("100","1000","2000","5000"), fill=c(terrain.colors(3)[1],"brown","orange","blue"), horiz=FALSE)
dev.off()


####### Fixing batchSize to 1000
pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/1MCajaMarData_1000batchSize.pdf"),width=7, height=5)
valmax<-max(c(data$X1000L,data$X0.1L,data$X0.01L),na.rm = TRUE)
valmin<-min(data$X1000L,data$X0.1L,data$X0.01L,na.rm = TRUE)
plot(data$X1000T,data$X1000L,type="o", ylim = c(valmin,valmax), pch=18,xlab="Time (seconds)", ylab="Global lower bound",
     col=terrain.colors(3)[1], main="1M points - 12M node BN - all epochs - 1000 bs")
lines(data$X0.1T,data$X0.1L,type="o",pch=18, col="brown")
lines(data$X0.01T,data$X0.01L,type="o",pch=18, col="blue")
legend("bottomright", inset=.05, title="Local threshold",
       c("1","0.1","0.01"), fill=c(terrain.colors(3)[1],"brown","blue"), horiz=FALSE)
dev.off()

pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/1MCajaMarData_1000batchSizeEvery5Points.pdf"),width=7, height=5)
valmax<-max(c(data$X1000L[select],data$X0.1L[select],data$X0.01L[select]),na.rm = TRUE)
valmin<-min(data$X1000L[select],data$X0.1L[select],data$X0.01L[select],na.rm = TRUE)
plot(data$X1000T[select],data$X1000L[select],type="o", ylim = c(valmin,valmax), pch=18,xlab="Time (seconds)", ylab="Global lower bound",
     col=terrain.colors(3)[1], main="1M points - 12M node BN - 1 epoch - 1000 batchSize")
lines(data$X0.1T[select],data$X0.1L[select],type="o",pch=18, col="brown")
lines(data$X0.01T[select],data$X0.01L[select],type="o",pch=18, col="blue")
legend("bottomright", inset=.05, title="Local threshold",
       c("1","0.1","0.01"), fill=c(terrain.colors(3)[1],"brown","blue"), horiz=FALSE)
dev.off()

