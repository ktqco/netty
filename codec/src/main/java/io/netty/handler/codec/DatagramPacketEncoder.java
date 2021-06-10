/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBufConvertible;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvokerCallback;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.util.internal.StringUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * An encoder that encodes the content in {@link AddressedEnvelope} to {@link DatagramPacket} using
 * the specified message encoder. E.g.,
 *
 * <pre><code>
 * {@link ChannelPipeline} pipeline = ...;
 * pipeline.addLast("udpEncoder", new {@link DatagramPacketEncoder}(new {@link ProtobufEncoder}(...));
 * </code></pre>
 *
 * Note: As UDP packets are out-of-order, you should make sure the encoded message size are not greater than
 * the max safe packet size in your particular network path which guarantees no packet fragmentation.
 *
 * @param <M> the type of message to be encoded
 */
public class DatagramPacketEncoder<M> extends MessageToMessageEncoder<AddressedEnvelope<M, InetSocketAddress>> {

    private final MessageToMessageEncoder<? super M> encoder;

    /**
     * Create an encoder that encodes the content in {@link AddressedEnvelope} to {@link DatagramPacket} using
     * the specified message encoder.
     *
     * @param encoder the specified message encoder
     */
    public DatagramPacketEncoder(MessageToMessageEncoder<? super M> encoder) {
        this.encoder = requireNonNull(encoder, "encoder");
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        if (super.acceptOutboundMessage(msg)) {
            @SuppressWarnings("rawtypes")
            AddressedEnvelope envelope = (AddressedEnvelope) msg;
            return encoder.acceptOutboundMessage(envelope.content())
                    && (envelope.sender() instanceof InetSocketAddress || envelope.sender() == null)
                    && envelope.recipient() instanceof InetSocketAddress;
        }
        return false;
    }

    @Override
    protected void encode(
            ChannelHandlerContext ctx, AddressedEnvelope<M, InetSocketAddress> msg, List<Object> out) throws Exception {
        assert out.isEmpty();

        encoder.encode(ctx, msg.content(), out);
        if (out.size() != 1) {
            throw new EncoderException(
                    StringUtil.simpleClassName(encoder) + " must produce only one message.");
        }
        Object content = out.get(0);
        if (content instanceof ByteBufConvertible) {
            // Replace the ByteBuf with a DatagramPacket.
            out.set(0, new DatagramPacket(((ByteBufConvertible) content).asByteBuf(), msg.recipient(), msg.sender()));
        } else {
            throw new EncoderException(
                    StringUtil.simpleClassName(encoder) + " must produce only ByteBuf.");
        }
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelOutboundInvokerCallback callback)
            throws Exception {
        encoder.bind(ctx, localAddress, callback);
    }

    @Override
    public void connect(
            ChannelHandlerContext ctx, SocketAddress remoteAddress,
            SocketAddress localAddress, ChannelOutboundInvokerCallback callback) throws Exception {
        encoder.connect(ctx, remoteAddress, localAddress, callback);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelOutboundInvokerCallback callback) throws Exception {
        encoder.disconnect(ctx, callback);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelOutboundInvokerCallback callback) throws Exception {
        encoder.close(ctx, callback);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelOutboundInvokerCallback callback) throws Exception {
        encoder.deregister(ctx, callback);
    }

    @Override
    public void read(ChannelHandlerContext ctx, ChannelOutboundInvokerCallback callback) throws Exception {
        encoder.read(ctx, callback);
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelOutboundInvokerCallback callback) throws Exception {
        encoder.flush(ctx, callback);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        encoder.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        encoder.handlerRemoved(ctx);
    }

    @Override
    public boolean isSharable() {
        return encoder.isSharable();
    }
}
