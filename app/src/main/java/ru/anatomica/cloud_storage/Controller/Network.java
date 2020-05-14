package ru.anatomica.cloud_storage.Controller;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import ru.anatomica.cloud_storage.Handlers.AuthHandler;
import ru.anatomica.cloud_storage.Handlers.ProtocolHandler;
import ru.anatomica.cloud_storage.MainActivity;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class Network {

    private Channel currentChannel;
    private MainActivity mainActivity;
    private static Network ourInstance = new Network();

    public static Network getInstance() {
        return ourInstance;
    }

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void start(MainActivity mainActivity, Callback authOkCallback, Callback refreshCallback, Callback reloadTableCallback, CountDownLatch countDownLatch) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group);
            clientBootstrap.channel(NioSocketChannel.class);
            clientBootstrap.remoteAddress(new InetSocketAddress(FileService.hostAddress, FileService.hostPort));
            clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(new AuthHandler(authOkCallback, mainActivity), new ProtocolHandler(mainActivity, refreshCallback, reloadTableCallback));
                    currentChannel = socketChannel;
                }
            });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            countDownLatch.countDown();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            countDownLatch.countDown();
            // MainActivity.showError(e);
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        currentChannel.close();
    }
}
