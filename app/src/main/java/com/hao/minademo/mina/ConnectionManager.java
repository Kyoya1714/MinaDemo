package com.hao.minademo.mina;

import android.content.Context;
import android.util.Log;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
public class ConnectionManager {
    public static final String TAG="ConnectionManager";
    private ConnectionConfig mConfig;
    private WeakReference<Context> mContext;
    public NioSocketConnector mConnection;
    private IoSession mSession;
    private InetSocketAddress mAddress;

    public ConnectionManager(ConnectionConfig config){

        this.mConfig = config;
        this.mContext = new WeakReference<Context>(config.getContext());
        init();
    }

    private void init() {
        mAddress = new InetSocketAddress(mConfig.getIp(), mConfig.getPort());
        mConnection = new NioSocketConnector();
        mConnection.getSessionConfig().setReadBufferSize(mConfig.getReadBufferSize());
        //设置多长时间没有进行读写操作进入空闲状态，会调用sessionIdle方法，单位（秒）
        mConnection.getSessionConfig().setReaderIdleTime(60*5);
        mConnection.getSessionConfig().setWriterIdleTime(60*5);
        mConnection.getSessionConfig().setBothIdleTime(60*5);
        mConnection.getFilterChain().addFirst("reconnection", new MyIoFilterAdapter());
        //自定义编解码器
        mConnection.getFilterChain().addLast("mycoder", new ProtocolCodecFilter(new MyCodecFactory()));
        //添加消息处理器
        mConnection.setHandler(new DefaultHandler(mContext.get()));
        mConnection.setDefaultRemoteAddress(mAddress);
    }

    /**
     * 与服务器连接
     * @return true连接成功，false连接失败
     */
    public boolean connect(){
        try{
            ConnectFuture future = mConnection.connect();
            future.awaitUninterruptibly();
            mSession = future.getSession();
            if(mSession!=null && mSession.isConnected()) {
                SessionManager.getInstance().setSession(mSession);
            }else {
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 断开连接
     */
    public void disConnect(){
        mConnection.dispose();
        mConnection=null;
        mSession=null;
        mAddress=null;
        mContext = null;
    }

    private class DefaultHandler extends IoHandlerAdapter {

        private Context mContext;
        private DefaultHandler(Context context){
            this.mContext = context;
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            super.sessionOpened(session);
            Log.d(TAG, "连接打开");
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            Log.d(TAG, "收到数据，接下来你要怎么解析数据就是你的事了");
            IoBuffer buf = (IoBuffer) message;
            HandlerEvent.getInstance().handle(buf);
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
            super.sessionIdle(session, status);
            Log.d(TAG, "-客户端与服务端连接空闲");
            //进入空闲状态我们把会话关闭，接着会调用MyIoFilterAdapter的sessionClosed方法，进行重新连接
            if(session != null){
                session.closeOnFlush();
            }
        }
    }

    private  class MyIoFilterAdapter extends IoFilterAdapter {
        @Override
        public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
            Log.d(TAG, "连接关闭，每隔5秒进行重新连接");
            for(;;){
                if(mConnection==null){
                    break;
                }
                if(ConnectionManager.this.connect()){
                    Log.d(TAG, "断线重连[" + mConnection.getDefaultRemoteAddress().getHostName() + ":" +
                            mConnection.getDefaultRemoteAddress().getPort() + "]成功");
                    break;
                }
                Thread.sleep(5000);
            }
        }

    }
}
