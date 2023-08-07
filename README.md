简易实现RPC框架

基本调用过程：<br />
1、服务调用方<br />
发送报文 writeAndFlush(object)<br />
pipeline生效，报文开始出栈<br />
---> 第一个处理器（out）（转化 object -> msg（请求报文）)<br />
---> 第二个处理器（out）（序列化）<br />
---> 第三个处理器（out）（压缩）<br />

2、服务提供方<br />
通过netty接收报文<br />
pipeline生效，报文开始出栈<br />
发送报文 writeAndFlush(object)<br />
pipeline生效报文开始出栈<br />
---> 第一个处理器（in）（解压缩）<br />
---> 第二个处理器（in）（反序列化）<br />
---> 第三个处理器（in）（解析报文)<br />

3、执行方法调用，得到结果

4、服务调用方<br />
发送报文 writeAndFlush(object)<br />
pipeline生效，报文开始出栈<br />
---> 第一个处理器（out）（转化 object -> msg（响应报文）)<br />
---> 第二个处理器（out）（序列化）<br />
---> 第三个处理器（out）（压缩）<br />

5、服务提供方<br />
通过netty接收响应报文<br />
pipeline生效，报文开始出栈<br />
发送报文 writeAndFlush(object)<br />
pipeline生效报文开始出栈<br />
---> 第一个处理器（in）（解压缩）<br />
---> 第二个处理器（in）（反序列化）<br />
---> 第三个处理器（in）（解析报文)<br />

6、得到结果返回