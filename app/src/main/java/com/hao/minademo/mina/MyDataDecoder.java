package com.hao.minademo.mina;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class MyDataDecoder extends CumulativeProtocolDecoder {
    /**
     * 返回值含义:
     * 1、当内容刚好时，返回false，告知父类接收下一批内容
     * 2、内容不够时需要下一批发过来的内容，此时返回false，这样父类 CumulativeProtocolDecoder
     * 会将内容放进IoSession中，等下次来数据后就自动拼装再交给本类的doDecode
     * 3、当内容多时，返回true，因为需要再将本批数据进行读取，父类会将剩余的数据再次推送本类的doDecode方法
     */
    @Override
    public boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        /**
         * 假定消息格式为：消息头（int类型：表示消息体的长度、short类型：表示事件号）+消息体
         */
        if (in.remaining() < 4)//是用来当拆包时候剩余长度小于4的时候的保护，不加容易出错
        {
            return false;
        }
        if (in.remaining() > 1) {
            //以便后继的reset操作能恢复position位置
            in.mark();
            ////前6字节是包头，一个int和一个short，我们先取一个int
            int len = in.getInt();//先获取包体数据长度值

            //比较消息长度和实际收到的长度是否相等，这里-2是因为我们的消息头有个short值还没取
            if (len > in.remaining() - 2) {
                //出现断包，则重置恢复position位置到操作前,进入下一轮, 接收新数据，以拼凑成完整数据
                in.reset();
                return false;
            } else {
                //消息内容足够
                in.reset();//重置恢复position位置到操作前
                int sumLen = 6 + len;//总长 = 包头+包体
                byte[] packArr = new byte[sumLen];
                in.get(packArr, 0, sumLen);
                IoBuffer buffer = IoBuffer.allocate(sumLen);
                buffer.put(packArr);
                buffer.flip();
                out.write(buffer);
                //走到这里会调用DefaultHandler的messageReceived方法

                if (in.remaining() > 0) {//出现粘包，就让父类再调用一次，进行下一次解析
                    return true;
                }
            }
        }
        return false;//处理成功，让父类进行接收下个包
    }
}