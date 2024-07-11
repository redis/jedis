package redis.clients.jedis.asyncio;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.Closeable;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connection implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(Connection.class);

    private final CommandArgumentsHandler commandArgumentsHandler = new CommandArgumentsHandler();

    private final EventLoopGroup group;
    private final Channel channel;

    public Connection(String host, int port) throws InterruptedException {
        this.group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        initializeBootstrap(bootstrap);
        this.channel = bootstrap.connect(host, port).sync().channel();
    }

    private void initializeBootstrap(Bootstrap bs) {
        bs.group(this.group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline chPipe = ch.pipeline();
                        chPipe.addLast(commandArgumentsHandler);
                        chPipe.addLast(new CommandResponseHandler());
                    }
                });
    }

    @Override
    public void close() throws IOException {
        try {
            channel.close().sync();
        } catch (InterruptedException inter) {
            logger.error("Error while closing Channel.", inter);
        } finally {
            group.shutdownGracefully();
        }
    }

    public <T> CommandObject<T> executeCommand(CommandObject<T> command) throws InterruptedException {
        try {
            ChannelFuture writeFuture = channel.writeAndFlush(command);
            writeFuture.addListener((ChannelFuture future) -> {
                if (!future.isSuccess()) {
                    logger.error("Write failed", future.cause());
                }
            });
            writeFuture.sync();
        } finally {
            return command;
        }
    }
}
