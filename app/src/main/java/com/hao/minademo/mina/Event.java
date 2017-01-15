package com.hao.minademo.mina;

/**
 * 与服务器协议好的事件号
 */
public class Event {
    /**
     * 客户端发送给服务器事件号
     */
    public final static short EV_C_S_TEST = (short)1;
    /**
     * 服务器回应给客户端事件号
     */
    public final static short EV_S_C_TEST = (short)2;
}
