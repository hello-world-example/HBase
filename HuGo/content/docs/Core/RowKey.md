
# Rowkey 设计

- Rowkey 是一段二进制码流，**最大长度为64KB**
- Rowkey 以**字典顺序排序**，其方法是，按照字母顺序，或者数字小大顺序，由小到大的形成序列；有两个rowkey：123,3,排列顺序为:3,123
- 查找数据的时候，先查找 Rowkey 所在的Region，然后将求路由到该Region获取数据
- 按单个Rowkey检索的效率是很高的，耗时在1毫秒以下

## 设计原则

### 长度原则



### 散列原则



### 唯一性



## Read More

- [HBase Rowkey设计](http://www.chepoo.com/hbase-rowkey-design.html)
- [HBase的RowKey设计原则](http://www.chepoo.com/rowkey-design-of-hbase.html)
- [HBase Rowkey的散列与预分区设计](http://www.cnblogs.com/bdifn/p/3801737.html)