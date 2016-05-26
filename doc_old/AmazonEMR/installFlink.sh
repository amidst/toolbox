#Download Flink 0.10.0 for Hadoop 2.6.0
wget http://ftp.download-by.net/apache/flink/flink-0.10.0/flink-0.10.0-bin-hadoop26-scala_2.10.tgz
#Extract it
tar xzf flink-*.tgz
#Important to set the Hadoop configuration parameters
export HADOOP\_CONF\_DIR=/etc/hadoop/conf
