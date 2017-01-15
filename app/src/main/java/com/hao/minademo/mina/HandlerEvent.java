package com.hao.minademo.mina;

import com.hao.minademo.mina.domain.MinaMsgHead;

import org.apache.mina.core.buffer.IoBuffer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;


/**
 * 消息事件处理
 */
public class HandlerEvent {
    private static HandlerEvent handlerEvent;
    public static HandlerEvent getInstance() {
        if (handlerEvent == null) {
            handlerEvent = new HandlerEvent();
        }
        return handlerEvent;
    }
    public void handle(IoBuffer buf) throws IOException, InterruptedException, UnsupportedEncodingException, SQLException {
        //解析包头
        MinaMsgHead msgHead = new MinaMsgHead();
        msgHead.bodyLen = buf.getInt();//包体长度
        msgHead.event = buf.getShort();//事件号

        switch (msgHead.event){//根据事件号解析消息体内容
            case Event.EV_S_C_TEST:
                byte[] by = new byte[msgHead.bodyLen];
                buf.get(by, 0, by.length);
                String json = new String(by, "UTF-8").trim();
                //接下来根据业务处理....
                break;
        }
    }
}
