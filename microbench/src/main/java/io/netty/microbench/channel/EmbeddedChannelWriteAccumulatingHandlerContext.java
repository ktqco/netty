/*
 * Copyright 2017 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.netty.microbench.channel;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvokerCallback;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.ByteToMessageDecoder;

public abstract class EmbeddedChannelWriteAccumulatingHandlerContext extends EmbeddedChannelHandlerContext {
    private ByteBuf cumulation;
    private ByteToMessageDecoder.Cumulator cumulator;

    protected EmbeddedChannelWriteAccumulatingHandlerContext(ByteBufAllocator alloc, ChannelHandler handler,
                                                          ByteToMessageDecoder.Cumulator writeCumulator) {
        this(alloc, handler, writeCumulator, new EmbeddedChannel());
    }

    protected EmbeddedChannelWriteAccumulatingHandlerContext(ByteBufAllocator alloc, ChannelHandler handler,
                                                          ByteToMessageDecoder.Cumulator writeCumulator,
                                                          EmbeddedChannel channel) {
        super(alloc, handler, channel);
        this.cumulator = requireNonNull(writeCumulator, "writeCumulator");
    }

    public final ByteBuf cumulation() {
        return cumulation;
    }

    public final void releaseCumulation() {
        if (cumulation != null) {
            cumulation.release();
            cumulation = null;
        }
    }

    @Override
    public final ChannelFuture write(Object msg) {
        return write(msg, newPromise());
    }

    @Override
    public final ChannelHandlerContext write(Object msg, ChannelOutboundInvokerCallback callback) {
        try {
            if (msg instanceof ByteBuf) {
                if (cumulation == null) {
                    cumulation = (ByteBuf) msg;
                } else {
                    cumulation = cumulator.cumulate(alloc(), cumulation, (ByteBuf) msg);
                }
                callback.setSuccess();
            } else {
                channel().write(msg, callback);
            }
        } catch (Exception e) {
            callback.setFailure(e);
            handleException(e);
        }
        return callback;
    }

    @Override
    public final ChannelHandlerContext writeAndFlush(Object msg, ChannelOutboundInvokerCallback callback) {
        try {
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                if (cumulation == null) {
                    cumulation = buf;
                } else {
                    cumulation = cumulator.cumulate(alloc(), cumulation, buf);
                }
                callback.setSuccess();
            } else {
                channel().writeAndFlush(msg, callback);
            }
        } catch (Exception e) {
            callback.setFailure(e);
            handleException(e);
        }
        return callback;
    }

    @Override
    public final ChannelFuture writeAndFlush(Object msg) {
        return writeAndFlush(msg, newPromise());
    }
}
