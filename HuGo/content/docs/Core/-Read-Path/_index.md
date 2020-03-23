#### 

第二个新功能是全链路 Off-heap，意思就是读写链路数据端到端 Off-heap，减少 java GC 带来的停顿，进一步降低 P999 延迟，提高吞吐。这个功能我们从两方面来实现的：写链路 Off-heap，我们使用在 RPC 层使用 Netty 的 Off-heap ByteBuffer，使用支持 Off-heap 的 Protobuf。同时使用 Off-heap 的 Chunk 来存储 Memstore 中的 KeyValue。

在读链路 Off-heap 方面，使用 Off-heap 的 Bucket Cache，HBase 自己管理内存的，我们从 Bucket Cache 读取数据的时候，先要从 Protobuf 做一次拷贝，因为可能读取的时候，发生内存不够了，再次分配的情况。在读取对 Bucket Cache 进行引用计数，保证读取的时候，内存不会被回收掉，读取时不再需要先拷贝到 heap，对 Bucket Cache 进行了一系列性能优化。



我们可以看到，RowKey需要重复保存很多次，而且Family:Column这个往往都是非常相似的，它也需要保存很多次．这对磁盘非常不友好．当Family:Column越多时，就需要占用越多不必要的磁盘空间．

如果仅仅是磁盘空间，也没什么关系，毕竟我们可以通过Snappy/GZ等压缩方式，对HFile进行Compression．而且磁盘又便宜，对吧？

可是，对HBase熟悉的读者，都知道，当读取数据时，是需要先读MemStore，然后再读BlockCache的．那我们的Block越小，能放到BlockCache中的数据就越多，命中率就越高，对Scan就越友好．

Block Encoding就是做这件事情的，它就是通过某种算法，对Data Block中的数据进行压缩，这样Block的Size小了，放到Block Cache中的就多了．

这儿提出两个问题:

- 压缩以后，占的Disk/Memory是少了，但是解压的时候，需要更多的CPU时间．如何均衡呢?
- 如果我们的业务，偏重的是随机Get，那放到Block Cache中不一定好吧？不仅放到Block Cache中的Block很容易读不到，对性能并没有什么提升，还会产生额外的开销，比如将其它偏重Scan的业务的Block排挤出Block Cache，导致其它业务变慢．



作者：AlstonWilliams
链接：https://www.jianshu.com/p/a62e49f749f3
来源：简书
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。

## Read More
- https://blog.csdn.net/u014297175/article/details/47976909
- https://www.cnblogs.com/tgzhu/archive/2016/09/11/5862299.html