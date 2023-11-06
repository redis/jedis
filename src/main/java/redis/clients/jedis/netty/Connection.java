package redis.clients.jedis.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;

import java.io.Closeable;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connection implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(Connection.class);

//    private final String host;
//    private final int port;

    private final EventLoopGroup group;
    private final Channel channel;

    public Connection(String host, int port) throws InterruptedException {
//        this.host = host;
//        this.port = port;

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
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new RedisDecoder());
                        p.addLast(new RedisBulkStringAggregator());
                        p.addLast(new RedisArrayAggregator());
                        p.addLast(new RedisEncoder());
                        p.addLast(new RedisClientHandler());
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

    public void executeCommand(String line) throws InterruptedException {
        ChannelFuture writeFuture = channel.writeAndFlush(line);
        writeFuture.addListener((ChannelFuture future) -> {
            if (!future.isSuccess()) {
                //System.err.print("write failed: ");
                //future.cause().printStackTrace(System.err);
                logger.error("Write failed", future.cause());
            }
        });
        writeFuture.sync();
    }
}
