size = 1000
 
A= sample(c("1","2"),size, replace=TRUE, prob=c(0.2,0.8))
B= sample(c("1","2","3"),size, replace=TRUE, prob=c(0.1,0.3,0.6))
C = rnorm(size, 0.5, 1)
D = rnorm(size, -3, 1)
E = sample(c("1","2"),size, replace=TRUE, prob=c(0.3,0.7))
G = rnorm(size, -3, 0.25)
H = rnorm(size, 1.5, 1.5)
I = rnorm(size, 0.5, 0.4)

data=data.frame(A=A,B=B,C=C,D=D,E=E,G=G, H=H, I=I)

colnames(data) <- c("A","B","C","D","E","G","H","I")

write.table(data, file="/Users/afa/Dropbox/AMIDST-AFA/core/datasets/syntheticData.csv", sep=",", quote=FALSE, row.names=FALSE)





size = 1000
 
A= sample(c("1","2"),size, replace=TRUE, prob=c(0.2,0.8))
B= sample(c("1","2","3"),size, replace=TRUE, prob=c(0.1,0.3,0.6))
C = sample(c("1","2"),size, replace=TRUE, prob=c(0.3,0.7))
D = sample(c("1","2"),size, replace=TRUE, prob=c(0.3,0.7))
E = sample(c("1","2"),size, replace=TRUE, prob=c(0.3,0.7))
G = sample(c("1","2"),size, replace=TRUE, prob=c(0.3,0.7))

data=data.frame(A=A,B=B,C=C,D=D,E=E,G=G)

colnames(data) <- c("A","B","C","D","E","G")

write.table(data, file="/Users/afa/Dropbox/AMIDST-AFA/core/datasets/syntheticDataDiscrete.csv", sep=",", quote=FALSE, row.names=FALSE)


