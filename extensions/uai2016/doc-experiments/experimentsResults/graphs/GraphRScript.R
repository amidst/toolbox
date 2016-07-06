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


###### Comparison: Distributed VMP vs Stochastic VI

######### MONTH 1 (local mode)
data <- read.csv(file="/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/data10K.csv",head=TRUE,sep=",")

data <- data[-c(1:10), ] 
pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/10KCajaMarData_MONTH1.pdf"),width=7, height=5)
valmax<-max(c(data$VMP1000L,data$VMP100L,data$SVI10L,data$SVI100L,data$SVI1000L),na.rm = TRUE)
valmin<-min(c(data$VMP1000L,data$VMP100L,data$SVI10L,data$SVI100L,data$SVI1000L),na.rm = TRUE)
plot(data$VMP100T,data$VMP100L,type="o", ylim = c(valmin,valmax),pch=18,
     xlab="Time (seconds)", ylab="Global lower bound",
     col=terrain.colors(3)[1], main="10K points - Month 1 - local")
lines(data$SVI10T,data$SVI10L,type="o",pch=18, col="brown")
lines(data$SVI100T,data$SVI100L,type="o",pch=18, col="orange")
lines(data$SVI1000T,data$SVI1000L,type="o",pch=18, col="blue")
legend("bottomright", inset=.05, title="Batch Size",
       c("VMP100","SVI10","SVI100","SVI1000"), fill=c(terrain.colors(3)[1],"brown","orange","blue"), horiz=FALSE)
dev.off()

pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/10KCajaMarData_MONTH1_NoSVI10.pdf"),width=7, height=5)
valmax<-max(c(data$VMP1000L,data$VMP100L,data$SVI100L,data$SVI1000L),na.rm = TRUE)
valmin<-min(c(data$VMP1000L,data$VMP100L,data$SVI100L,data$SVI1000L),na.rm = TRUE)
plot(data$VMP100T,data$VMP100L,type="o", ylim = c(valmin,valmax),pch=18,
     xlab="Time (seconds)", ylab="Global lower bound",
     col=terrain.colors(3)[1], main="10K points - Month 1 - local")
lines(data$VMP1000T,data$VMP1000L,type="o",pch=18, col="brown")
lines(data$SVI100T,data$SVI100L,type="o",pch=18, col="orange")
lines(data$SVI1000T,data$SVI1000L,type="o",pch=18, col="blue")
legend("bottomright", inset=.05, title="Batch Size",
       c("VMP100","VMP1000","SVI100","SVI1000"), fill=c(terrain.colors(3)[1],"brown","orange","blue"), horiz=FALSE)
dev.off()

######### MONTH 1 (global mode)


data <- read.csv(file="/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/data10KonCluster.csv",head=TRUE,sep=",")

data <- data[-c(1:8), ] 
pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/10KCajaMarData_MONTH1onCluster.pdf"),width=7, height=5)
valmax<-max(c(data$VMP1000L,data$SVI100L,data$SVI1000L),na.rm = TRUE)
valmin<-min(c(data$VMP1000L,data$SVI100L,data$SVI1000L),na.rm = TRUE)
plot(data$VMP1000T,data$VMP1000L,type="o", ylim = c(valmin,valmax),pch=18,
     xlab="Time (seconds)", ylab="Global lower bound",
     col=terrain.colors(3)[1], main="10K points - Month 1 - cluster")
lines(data$SVI100T,data$SVI100L,type="o",pch=18, col="orange")
lines(data$SVI1000T,data$SVI1000L,type="o",pch=18, col="blue")
legend("bottomright", inset=.05, title="Batch Size",
       c("VMP1000","SVI100","SVI1000"), fill=c(terrain.colors(3)[1],"orange","blue"), horiz=FALSE)
dev.off()


################ REAL DATA

data <- read.csv(file="/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/realData55K.csv",head=TRUE,sep=",")

############# VMP ############
pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/realData55K_VMP.pdf"),width=7, height=5)
valmax<-max(c(data$VMP550L,data$VMP2750L,data$VMP5500L),na.rm = TRUE)
valmin<-min(c(data$VMP550L,data$VMP2750L,data$VMP5500L),na.rm = TRUE)
plot(data$VMP550T,data$VMP550L,type="o", ylim = c(valmin,valmax),pch=18,
     xlab="Time (seconds)", ylab="Global lower bound",
     col=terrain.colors(3)[1], main="real CajaMar data - VMP")
lines(data$VMP2750T,data$VMP2750L,type="o",pch=18, col="orange")
lines(data$VMP5500T,data$VMP5500L,type="o",pch=18, col="blue")
legend("bottomright", inset=.05, title="Batch Size",
       c("550","2750","5500"), fill=c(terrain.colors(3)[1],"orange","blue"), horiz=FALSE)
dev.off()

############# SVI ############

myColours <-c("darkseagreen1","darkseagreen","darkseagreen4","goldenrod1","gold",
               "goldenrod3","slategray1","steelblue2","steelblue",
              "lightpink","lightpink3","hotpink2")
nskip <- 1

# BATCH SIZE 413 - 1%
pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/realData55K_SVI413.pdf"),width=7, height=5)
valmax<-max(c(data$SVI550_055L[-c(1:nskip)],data$SVI550_075L[-c(1:nskip)],data$SVI550_099L[-c(1:5)]),na.rm = TRUE)
valmin<-min(c(data$SVI550_055L[-c(1:nskip)],data$SVI550_075L[-c(1:nskip)],data$SVI550_099L[-c(1:5)]),na.rm = TRUE)
plot(data$SVI550_055T[-c(1:nskip)],data$SVI550_055L[-c(1:nskip)],type="o", ylim = c(valmin,valmax),pch=18,
     xlab="Time (seconds)", ylab="Global lower bound",
     col=myColours[1], main="real CajaMar data - SVI Batchsize 1%")
lines(data$SVI550_075T[-c(1:nskip)],data$SVI550_075L[-c(1:nskip)],type="o",pch=18, col=myColours[2])
lines(data$SVI550_099T[-c(1:nskip)],data$SVI550_099L[-c(1:nskip)],type="o",pch=18, col=myColours[3])
legend("bottomright", inset=.05, title="Learning rate",
       c("0.55","0.75","0.99"), fill=c(terrain.colors(3)[1],"orange","blue"), horiz=FALSE)
dev.off()

# BATCH SIZE 2065 - 5%
pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/realData55K_SVI2065.pdf"),width=7, height=5)
valmax<-max(c(data$SVI2750_055L,data$SVI2750_075L,data$SVI2750_099L),na.rm = TRUE)
valmin<-min(c(data$SVI2750_055L,data$SVI2750_075L,data$SVI2750_099L),na.rm = TRUE)
plot(data$SVI2750_055T,data$SVI2750_055L,type="o", ylim = c(valmin,valmax),pch=18,
     xlab="Time (seconds)", ylab="Global lower bound",
     col=myColours[4], main="real CajaMar data - SVI Batchsize 5%")
lines(data$SVI2750_075T,data$SVI2750_075L,type="o",pch=18, col=myColours[5])
lines(data$SVI2750_099T,data$SVI2750_099L,type="o",pch=18, col=myColours[6])
legend("bottomright", inset=.05, title="Learning rate",
       c("0.55","0.75","0.99"), fill=c(terrain.colors(3)[1],"orange","blue"), horiz=FALSE)
dev.off()

# BATCH SIZE 4129 - 10%
pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/realData55K_SVI4129.pdf"),width=7, height=5)
valmax<-max(c(data$SVI5500_055L,data$SVI5500_075L,data$SVI5500_099L),na.rm = TRUE)
valmin<-min(c(data$SVI5500_055L,data$SVI5500_075L,data$SVI5500_099L),na.rm = TRUE)
valxmax<-max(c(data$SVI5500_055T,data$SVI5500_075T,data$SVI5500_099T),na.rm = TRUE)
valxmin<-min(c(data$SVI5500_055T,data$SVI5500_075T,data$SVI5500_099T),na.rm = TRUE)
plot(data$SVI5500_055T,data$SVI5500_055L,type="o", ylim = c(valmin,valmax),pch=18,
     xlim = c(valxmin,valxmax), xlab="Time (seconds)", ylab="Global lower bound",
     col=myColours[7], main="real CajaMar data - SVI Batchsize 10%")
lines(data$SVI5500_075T,data$SVI5500_075L,type="o",pch=18, col=myColours[8])
lines(data$SVI5500_099T,data$SVI5500_099L,type="o",pch=18, col=myColours[9])
legend("bottomright", inset=.05, title="Learning rate",
       c("0.55","0.75","0.99"), fill=c(terrain.colors(3)[1],"orange","blue"), horiz=FALSE)
dev.off()

# SVI vs VMP
nskip <- 2
data <- read.csv(file="/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/realData55K.csv",head=TRUE,sep=",")
myColours <-c("darkseagreen1","darkseagreen","darkseagreen4","goldenrod1","gold",
              "goldenrod3","slategray1","steelblue2","steelblue",
              "lightpink","lightpink3","hotpink2")
allL <- c(data$SVI550_055L,data$SVI550_075L,data$SVI550_099L,
          data$SVI2750_055L,data$SVI2750_075L,data$SVI2750_099L,
          data$SVI5500_055L,data$SVI5500_075L,data$SVI5500_099L,
          data$VMP550L,data$VMP2750L,data$VMP5500L)
allT <- c(data$SVI550_055T,data$SVI550_075T,data$SVI550_099T,
          data$SVI2750_055T,data$SVI2750_075T,data$SVI2750_099T,
          data$SVI5500_055T,data$SVI5500_075T,data$SVI5500_099T,
          data$VMP550T,data$VMP2750T,data$VMP5500T)

pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/realData55K_SVIvsVMP15M.pdf"),width=7, height=5)
par(mar=c(5.1, 4.1, 4.1, 8.5), xpd=FALSE)
valymax<-max(allL,na.rm = TRUE)
valymin<-min(allL,na.rm = TRUE)
valxmax<-max(allT,na.rm = TRUE)
valxmin<-min(allT,na.rm = TRUE)
plot(data$SVI550_055T,data$SVI550_055L,type="o",
     ylim = c(-15000000,valymax),pch=18,
     xlab="Time (seconds)", ylab="Global lower bound",
     col="darkseagreen1",
     xlim = c(valxmin,valxmax))
lines(data$SVI550_075T,data$SVI550_075L,type="o",pch=18, 
      col="darkseagreen")
lines(data$SVI550_099T,data$SVI550_099L,type="o",pch=18, col="darkseagreen4")
lines(data$SVI2750_055T,data$SVI2750_055L,type="o",pch=18, col="goldenrod1")
lines(data$SVI2750_075T,data$SVI2750_075L,type="o",pch=18, col="gold")
lines(data$SVI2750_099T,data$SVI2750_099L,type="o",pch=18, col="goldenrod3")
lines(data$SVI5500_055T,data$SVI5500_055L,type="o",pch=18, col="slategray1")
lines(data$SVI5500_075T,data$SVI5500_075L,type="o",pch=18, col="steelblue2")
lines(data$SVI5500_099T,data$SVI5500_099L,type="o",pch=18, col="steelblue")
lines(data$VMP550T,data$VMP550L,type="o",pch=18, col="lightpink")
lines(data$VMP2750T,data$VMP2750L,type="o",pch=18, col="lightpink3")
lines(data$VMP5500T,data$VMP5500L,type="o",pch=18, col="hotpink2")
par(mar=c(5.1, 4.1, 4.1, 8.5), xpd=TRUE)
legend("bottomright",inset=c(-0.36,0), title="Alg. BS(data%)/LR",
       c("SVI 1%/0.55","SVI 1%/0.75","SVI 1%/0.99",
         "SVI 5%/0.55","SVI 5%/0.75","SVI 5%/0.99",
         "SVI 10%/0.55","SVI 10%/0.75","SVI 10%/0.99",
         "d-VMP 0.5%", "d-VMP 1%","d-VMP 2%"), 
       fill=myColours, horiz=FALSE)
dev.off()


#SCALABILITY
data <- read.csv(file="/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/simulatedData42Mp.csv",head=TRUE,sep=",")
allL <- c(data$nodes2L,data$nodes4L,data$nodes8L,data$nodes16L)
allT <- c(data$nodes2T,data$nodes4T,data$nodes8T,data$nodes16T)
valymax<-max(allL,na.rm = TRUE)
valymin<-min(allL,na.rm = TRUE)
valxmax<-max(allT,na.rm = TRUE)
valxmin<-min(allT,na.rm = TRUE)
par(mar=c(5.1, 4.1, 4.1, 4.1), xpd=FALSE)
pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/scalability.pdf"),width=7, height=5)
plot(data$nodes16T,data$nodes16L,type="o", ylim = c(-2.19E+09,valymax),xlim=c(valxmin,valxmax),pch=18,
     xlab="Time (seconds)", ylab="Global lower bound",
     col="blue")
lines(data$nodes8T,data$nodes8L,type="o",pch=18, col="brown")
lines(data$nodes4T,data$nodes4L,type="o",pch=18, col="orange")
lines(data$nodes2T,data$nodes2L,type="o",pch=18, col=terrain.colors(3)[1])
abline(h = -2017877248, col = "gray60")
legend("topleft", inset=.05, title="#Nodes",
       c("16","8","4","2"), fill=c("blue","brown","orange",terrain.colors(3)[1]), horiz=FALSE)
dev.off()

# Alg v1 vs alg v2
data <- read.csv(file="/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/algV1vsAlgV2.csv",head=TRUE,sep=",")
allL <- c(data$v1L,data$v2L)
allT <- c(data$v1T,data$v2T)
valymax<-max(allL,na.rm = TRUE)
valymin<-min(allL,na.rm = TRUE)
valxmax<-max(allT,na.rm = TRUE)
valxmin<-min(allT,na.rm = TRUE)
par(mar=c(5.1, 4.1, 4.1, 4.1), xpd=FALSE)
pdf(paste("/Users/ana/core/extensions/uai2016/doc-experiments/experimentsResults/graphs/algV1vsAlgV2.pdf"),width=7, height=5)
plot(data$v1T,data$v1L,type="o", ylim = c(valymin,valymax),xlim=c(valxmin,valxmax),pch=18,
     xlab="Time (seconds)", ylab="Global lower bound",
     col=terrain.colors(3)[1])
lines(data$v2T,data$v2L,type="o",pch=18, col="brown")
legend("bottomright", inset=.05, 
      c("d-VMP v1","d-VMP v2"), fill=c(terrain.colors(3)[1], "brown"), horiz=FALSE)
dev.off()


