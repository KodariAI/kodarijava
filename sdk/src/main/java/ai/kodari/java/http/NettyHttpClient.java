package ai.kodari.java.http;

import ai.kodari.java.exception.KodariException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class NettyHttpClient implements Closeable {

    private final EventLoopGroup group;
    private final SslContext sslContext;

    public NettyHttpClient() {
        this.group = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory());
        try {
            this.sslContext = SslContextBuilder.forClient().build();
        } catch (SSLException e) {
            group.shutdownGracefully();
            throw new KodariException("Failed to initialize SSL context", e);
        }
    }

    public CompletableFuture<HttpResponse> get(
            String url,
            Map<String, String> headers
    ) {
        return request(HttpMethod.GET, url, headers, Unpooled.EMPTY_BUFFER);
    }

    public CompletableFuture<HttpResponse> post(
            String url,
            Map<String, String> headers,
            String body
    ) {
        ByteBuf content = Unpooled.copiedBuffer(body, StandardCharsets.UTF_8);
        return request(HttpMethod.POST, url, headers, content)
                .whenComplete((response, throwable) -> {
                    if (throwable != null && content.refCnt() > 0)
                        content.release();
                });
    }

    private CompletableFuture<HttpResponse> request(
            HttpMethod method,
            String url,
            Map<String, String> headers,
            ByteBuf content
    ) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        boolean ssl = "https".equalsIgnoreCase(uri.getScheme());
        int port = uri.getPort() == -1 ? (ssl ? 443 : 80) : uri.getPort();
        String path = uri.getRawPath();

        CompletableFuture<HttpResponse> future = new CompletableFuture<>();

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(
                            SocketChannel ch
                    ) {
                        ChannelPipeline pipeline = ch.pipeline();

                        if (ssl)
                            pipeline.addLast(sslContext.newHandler(ch.alloc(), host, port));

                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpObjectAggregator(1048576));
                        pipeline.addLast(new ResponseHandler(future));
                    }
                });

        bootstrap.connect(host, port).addListener((ChannelFutureListener) connectFuture -> {
            if (!connectFuture.isSuccess()) {
                future.completeExceptionally(
                        new KodariException("Failed to connect to " + host + ":" + port, connectFuture.cause())
                );
                return;
            }

            Channel channel = connectFuture.channel();

            FullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, method, path, content
            );
            request.headers().set(HttpHeaderNames.HOST, host);
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.headers().set(header.getKey(), header.getValue());
            }

            channel.writeAndFlush(request);
        });

        return future;
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }

    private static class ResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final CompletableFuture<HttpResponse> future;

        ResponseHandler(
                CompletableFuture<HttpResponse> future
        ) {
            this.future = future;
        }

        @Override
        protected void channelRead0(
                ChannelHandlerContext ctx,
                FullHttpResponse response
        ) {
            String body = response.content().toString(StandardCharsets.UTF_8);
            int statusCode = response.status().code();
            future.complete(new HttpResponse(statusCode, body));
            ctx.close();
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx,
                Throwable cause
        ) {
            future.completeExceptionally(new KodariException("Request failed", cause));
            ctx.close();
        }
    }
}
