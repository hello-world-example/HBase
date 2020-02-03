# Docker 搭建单机 CDH 环境

> - [QuickStarts for CDH 5.13](https://www.cloudera.com/downloads/quickstart_vms/5-13.html) 官方下载地址
>
> - [Cloudera Docker Container 官方文档](https://docs.cloudera.com/documentation/enterprise/5-13-x/topics/quickstart_docker_container.html)
> - [cloudera/quickstart 仓库](https://hub.docker.com/r/cloudera/quickstart)



## 使用 Docker 安装

> - [MAC 设置 Docker 内存](https://docs.docker.com/docker-for-mac/)
> - 端口描述详见 [Port]({{< relref "/docs/Core/Port.md" >}})

```bash
# 镜像有 5G， 下载比较慢
# wget https://downloads.cloudera.com/demo_vm/docker/cloudera-quickstart-vm-5.13.0-0-beta-docker.tar.gz
ᐅ docker pull cloudera/quickstart

# 启动容器
ᐅ docker run -d \
		--hostname=quickstart.cloudera \
		--privileged=true \
		--name=cdh-5.13 \
		-p 81:80 \
		-p 2181:2181 \
		-p 9092:9092 \
		-p 60010:60010 \
		-p 60000:60000 \
		-p 8888:8888 \
		-p 7180:7180 \
		cloudera/quickstart bash -c "while true; do echo noting; sleep 1; done"
		# /usr/bin/docker-quickstart

# 进入容器
ᐅ docker exec -it cdh-5.13 bash

# 查看服务状态
[root@quickstart /] ᐅ service --status-all
# 查看服务名
[root@quickstart /] ᐅ cat /usr/bin/docker-quickstart

# 启动关注的服务
[root@quickstart /] ᐅ service zookeeper-server start
[root@quickstart /] ᐅ service hadoop-hdfs-datanode start
[root@quickstart /] ᐅ service hadoop-hdfs-journalnode start
[root@quickstart /] ᐅ service hadoop-hdfs-namenode start
[root@quickstart /] ᐅ service hadoop-hdfs-secondarynamenode start
[root@quickstart /] ᐅ service hbase-master start
[root@quickstart /] ᐅ service hbase-regionserver start

		
# 默认是没有开启 CM 服务的，需要手动开启（默认账户： cloudera/cloudera ）
[root@quickstart /] ᐅ sudo /home/cloudera/cloudera-manager --force --express
```
