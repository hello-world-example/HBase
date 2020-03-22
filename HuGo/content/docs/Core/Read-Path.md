#### 

第二个新功能是全链路 Off-heap，意思就是读写链路数据端到端 Off-heap，减少 java GC 带来的停顿，进一步降低 P999 延迟，提高吞吐。这个功能我们从两方面来实现的：写链路 Off-heap，我们使用在 RPC 层使用 Netty 的 Off-heap ByteBuffer，使用支持 Off-heap 的 Protobuf。同时使用 Off-heap 的 Chunk 来存储 Memstore 中的 KeyValue。

在读链路 Off-heap 方面，使用 Off-heap 的 Bucket Cache，HBase 自己管理内存的，我们从 Bucket Cache 读取数据的时候，先要从 Protobuf 做一次拷贝，因为可能读取的时候，发生内存不够了，再次分配的情况。在读取对 Bucket Cache 进行引用计数，保证读取的时候，内存不会被回收掉，读取时不再需要先拷贝到 heap，对 Bucket Cache 进行了一系列性能优化。