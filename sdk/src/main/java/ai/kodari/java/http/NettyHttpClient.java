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

    private static final int MAX_CONTENT_LENGTH = 52428800;

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

    public CompletableFuture<BinaryHttpResponse> postForBytes(
            String url,
            Map<String, String> headers,
            String body
    ) {
        ByteBuf content = Unpooled.copiedBuffer(body, StandardCharsets.UTF_8);
        return binaryRequest(HttpMethod.POST, url, headers, content)
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
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        doRequest(method, url, headers, content, new ResponseHandler(future), future);
        return future;
    }

    private CompletableFuture<BinaryHttpResponse> binaryRequest(
            HttpMethod method,
            String url,
            Map<String, String> headers,
            ByteBuf content
    ) {
        CompletableFuture<BinaryHttpResponse> future = new CompletableFuture<>();
        doRequest(method, url, headers, content, new BinaryResponseHandler(future), future);
        return future;
    }

    private <T> void doRequest(
            HttpMethod method,
            String url,
            Map<String, String> headers,
            ByteBuf content,
            SimpleChannelInboundHandler<FullHttpResponse> handler,
            CompletableFuture<T> future
    ) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        boolean ssl = "https".equalsIgnoreCase(uri.getScheme());
        int port = uri.getPort() == -1 ? (ssl ? 443 : 80) : uri.getPort();
        String path = uri.getRawPath();

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
                        pipeline.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
                        pipeline.addLast(handler);
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

    private static class BinaryResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final CompletableFuture<BinaryHttpResponse> future;

        BinaryResponseHandler(
                CompletableFuture<BinaryHttpResponse> future
        ) {
            this.future = future;
        }

        @Override
        protected void channelRead0(
                ChannelHandlerContext ctx,
                FullHttpResponse response
        ) {
            byte[] bytes = new byte[response.content().readableBytes()];
            response.content().readBytes(bytes);

            String filename = response.headers().get("X-Filename");
            int statusCode = response.status().code();

            future.complete(new BinaryHttpResponse(statusCode, bytes, filename));
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
