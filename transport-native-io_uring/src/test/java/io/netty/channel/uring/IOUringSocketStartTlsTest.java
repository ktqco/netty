/*
 * Copyright 2020 The Netty Project
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
package io.netty.channel.uring;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.handler.ssl.SslContext;
import io.netty.testsuite.transport.TestsuitePermutation;
import io.netty.testsuite.transport.socket.SocketStartTlsTest;
import org.junit.BeforeClass;

import java.util.List;

import static org.junit.Assume.assumeTrue;

public class IOUringSocketStartTlsTest extends SocketStartTlsTest {

    public IOUringSocketStartTlsTest(SslContext serverCtx, SslContext clientCtx) {
        super(serverCtx, clientCtx);
    }

    @BeforeClass
    public static void loadJNI() {
        assumeTrue(IOUring.isAvailable());
    }

    @Override
    protected List<TestsuitePermutation.BootstrapComboFactory<ServerBootstrap, Bootstrap>> newFactories() {
        return IOUringSocketTestPermutation.INSTANCE.socket();
    }
}
