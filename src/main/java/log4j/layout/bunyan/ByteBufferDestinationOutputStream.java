/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package log4j.layout.bunyan;

import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class ByteBufferDestinationOutputStream extends OutputStream {
    public static class ByteBufferDestinationOutputStreamSupplier implements Supplier<ByteBufferDestinationOutputStream> {
        @Override
        public ByteBufferDestinationOutputStream get() {
            return new ByteBufferDestinationOutputStream();
        }
    }
    public static ByteBufferDestinationOutputStreamSupplier SUPPLIER = new ByteBufferDestinationOutputStreamSupplier();
    private ByteBufferDestination destination = null;

    public ByteBufferDestinationOutputStream() {
    }

    @Override
    public void write(final int i) {
        ByteBuffer toBuf = destination.getByteBuffer();
        toBuf.put((byte)i);
    }

    @Override
    public void write(final byte[] bytes, final int offset, final int length) {
        destination.writeBytes(bytes, offset, length);
    }

    public void setDestination(final ByteBufferDestination destination) {
        this.destination = destination;
    }
}
