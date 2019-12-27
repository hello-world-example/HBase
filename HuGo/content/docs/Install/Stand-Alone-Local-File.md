
# 单机本地文件系统

``` bash
# 下载
wget http://mirrors.hust.edu.cn/apache/hbase/stable/hbase-1.2.6-bin.tar.gz

vim hbase-1.2.6/conf/hbase-site.xml
# 新增
#  <property>
#    <name>hbase.rootdir</name>
#    <value>file:///opt/data/hbase</value>
#  </property>
#
#  如果 hbase-env.sh 中 export HBASE_MANAGES_ZK=true 设置true，最好指定 Zookeeper 的数据路劲
#  <property>
#    <name>hbase.zookeeper.property.dataDir</name>
#    <value>/opt/data/hbase-zookeeper</value>
#  </property>
#  
#  如果 hbase-env.sh 中 export HBASE_MANAGES_ZK=false 设置false，需要再加入以下配置
#  <property>
#     <name>hbase.cluster.distributed</name>
#     <value>true</value>
#   </property>


vim hbase-1.2.6/conf/hbase-env.sh
# 修改 JAVA_HOME， 去掉前面的 #，
# export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/
# 修改 HBASE_CLASSPATH ，去掉前面的 #
# export HBASE_CLASSPATH=/opt/websuite/hbase-1.2.6
# 默认是 true ，如果使用 外部 Zookeeper ，设置为 false 即可
# export HBASE_MANAGES_ZK=true

# 启动 Hbase
./hbase-1.2.6/bin/start-hbase.sh
# 关闭hbase
./hbase-1.2.6/bin/stop-hbase.sh

# 查看日志
tail -fn 400 hbase-1.2.6/logs/hbase-kail-master-MacBook-Pro.local.out

# 进入 hbase shell
./hbase-1.2.6/bin/hbase shell
```

## WebUI

http://localhost:16010/master-status

## FAQ

### Could not start ZK at requested port of 2181.  ZK was started at port: 2182. 

新增以下配置，

```
<property>
    <name>hbase.cluster.distributed</name>
    <value>true</value>
</property>
```

## Read More

- [Quick Start - Standalone HBase](http://hbase.apache.org/book.html#quickstart)
- [HBase 快速开始](http://abloz.com/hbase/book.html#quickstart)