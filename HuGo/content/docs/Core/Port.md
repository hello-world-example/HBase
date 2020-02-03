# 常用端口



| 端口  | 描述                           |                                |                    |
| ----- | ------------------------------ | ------------------------------ | ------------------ |
| 2181  | Zookeeper 端口                 |                                |                    |
|       |                                |                                |                    |
| 8080  | HBase REST Port                | `hbase.rest.port`              |                    |
| 16000 | HBase Master                   | `hbase.master.port`            | 1.0 之前 **60000** |
| 16010 | **HBase Master Web UI port**   | `hbase.master.info.port`       | 1.0 之前 **60010** |
| 16020 | HBase RegionServer Port        | `hbase.regionserver.port`      | 1.0 之前 **60020** |
| 16030 | HBase RegionServer Web UI port | `hbase.regionserver.info.port` | 1.0 之前 **60030** |



## Read More

- 官方文档 [7.2. HBase Default Configuration](http://hbase.apache.org/book.html#hbase_default_configurations)
- [CDH端口汇总](https://blog.csdn.net/Brady_heitong/article/details/79404331)