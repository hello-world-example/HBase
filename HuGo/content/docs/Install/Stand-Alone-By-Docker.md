# Docker 安装 HBase

> 端口描述详见 [Port]({{< relref "/docs/Core/Port.md" >}})
>
> 可选
>
> -p 8084:8080 \
> -p 8085:8085 \
> -p 9090:9090 \
> -p 9095:9095 \

```bash
$ docker run -d \
  -p 2181:2181 \
  -p 8084:8080 \
  -p 8085:8085 \
  -p 16000:16000 \
  -p 16010:16010 \
  -p 16020:16020 \
  -p 16030:16030 \
  --name hbase \
  harisekhon/hbase
  
# 启动 HBase REST API
$ docker exec -it hbase bash
# 不加端口的情况下，端口默认为8080
$$ hbase-daemon.sh start rest -p <port>
$$ hbase-daemon.sh stop rest

# HBase Shell
$ docker exec -it hbase hbase shell
```

## Read More

- Docker Hub [harisekhon/hbase](https://hub.docker.com/r/harisekhon/hbase)
- Github [HariSekhon / Dockerfiles](https://github.com/HariSekhon/Dockerfiles)