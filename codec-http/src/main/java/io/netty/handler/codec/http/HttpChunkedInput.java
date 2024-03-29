/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

/**
 * A {@link ChunkedInput} that fetches data chunk by chunk for use with HTTP chunked transfers.
 * <p>
 * Each chunk from the input data will be wrapped within a {@link HttpContent}. At the end of the input data,
 * {@link LastHttpContent} will be written.
 * <p>
 * Ensure that your HTTP response header contains {@code Transfer-Encoding: chunked}.
 * <p>
 * <pre>
 * public void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
 *     HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
 *     response.headers().set(TRANSFER_ENCODING, CHUNKED);
 *     ctx.write(response);
 *
 *     HttpContentChunkedInput httpChunkWriter = new HttpChunkedInput(
 *         new ChunkedFile(&quot;/tmp/myfile.txt&quot;));
 *     ChannelFuture sendFileFuture = ctx.write(httpChunkWriter);
 * }
 * </pre>
 * 一种数据块块接一块地获取数据块，以便与HTTP块块传输一起使用的数据块块。
 输入数据中的每个块都将包装在HttpContent中。在输入数据的末尾，将写入LastHttpContent。
 确保您的HTTP响应头包含传输编码:分块。
 */
public class HttpChunkedInput implements ChunkedInput<HttpContent> {

    private final ChunkedInput<ByteBuf> input;
    private final LastHttpContent lastHttpContent;
    private boolean sentLastChunk;

    /**
     * Creates a new instance using the specified input.使用指定的输入创建一个新实例。
     * @param input {@link ChunkedInput} containing data to write
     */
    public HttpChunkedInput(ChunkedInput<ByteBuf> input) {
        this.input = input;
        lastHttpContent = LastHttpContent.EMPTY_LAST_CONTENT;
    }

    /**
     * Creates a new instance using the specified input. {@code lastHttpContent} will be written as the terminating
     * chunk.使用指定的输入创建一个新实例。lastHttpContent将作为终止块编写。
     * @param input {@link ChunkedInput} containing data to write
     * @param lastHttpContent {@link LastHttpContent} that will be written as the terminating chunk. Use this for
     *            training headers.
     */
    public HttpChunkedInput(ChunkedInput<ByteBuf> input, LastHttpContent lastHttpContent) {
        this.input = input;
        this.lastHttpContent = lastHttpContent;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        if (input.isEndOfInput()) {
            // Only end of input after last HTTP chunk has been sent
            return sentLastChunk;
        } else {
            return false;
        }
    }

    @Override
    public void close() throws Exception {
        input.close();
    }

    @Deprecated
    @Override
    public HttpContent readChunk(ChannelHandlerContext ctx) throws Exception {
        return readChunk(ctx.alloc());
    }

    @Override
    public HttpContent readChunk(ByteBufAllocator allocator) throws Exception {
        if (input.isEndOfInput()) {
            if (sentLastChunk) {
                return null;
            } else {
                // Send last chunk for this input
                sentLastChunk = true;
                return lastHttpContent;
            }
        } else {
            ByteBuf buf = input.readChunk(allocator);
            if (buf == null) {
                return null;
            }
            return new DefaultHttpContent(buf);
        }
    }

    @Override
    public long length() {
        return input.length();
    }

    @Override
    public long progress() {
        return input.progress();
    }
}
