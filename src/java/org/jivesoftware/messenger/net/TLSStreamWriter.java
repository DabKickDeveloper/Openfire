/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2005 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * A <code>TLSStreamWriter</code> that returns a special OutputStream that hides the ByteBuffers
 * used by the underlying Channels.
 *
 * @author Hao Chen
 *
 */
public class TLSStreamWriter {

	/**
	 * <code>TLSWrapper</code> is a TLS wrapper for connections requiring TLS protocol.
	 */
	private TLSWrapper wrapper;

	private WritableByteChannel wbc;

	private ByteBuffer outAppData;

	public TLSStreamWriter(TLSWrapper tlsWrapper, Socket socket) throws IOException {
		wrapper = tlsWrapper;
		wbc = Channels.newChannel(socket.getOutputStream());
		outAppData = ByteBuffer.allocate(tlsWrapper.getAppBuffSize());
	}

	private void doWrite(ByteBuffer buff) throws IOException {

		if (buff == null) {
			// Possibly handshaking process
			buff = ByteBuffer.allocate(0);
		}

		if (wrapper == null) {
			writeToSocket(buff);
		} else {
			tlsWrite(buff);
		}
	}

	private void tlsWrite(final ByteBuffer buf) throws IOException {
		ByteBuffer tlsBuffer = null;
		ByteBuffer tlsOutput = null;
		do {
			tlsBuffer = ByteBuffer.allocate(Math.min(buf.remaining(), wrapper.getAppBuffSize()));
			tlsOutput = ByteBuffer.allocate(wrapper.getNetBuffSize());

			while (tlsBuffer.hasRemaining() && buf.hasRemaining()) {
				tlsBuffer.put(buf.get());
			}

			tlsBuffer.flip();
			wrapper.wrap(tlsBuffer, tlsOutput);

			tlsOutput.flip();
			writeToSocket(tlsOutput);

			tlsOutput.clear();
		} while (buf.hasRemaining());
	}

	/*
	 * Writes outNetData to the SocketChannel. <P> Returns true when the ByteBuffer has no remaining
	 * data.
	 */
	private boolean writeToSocket(final ByteBuffer outNetData) throws IOException {
		wbc.write(outNetData);
		return !outNetData.hasRemaining();
	}

	public OutputStream getOutputStream() {
		return createOutputStream();
	}

	/*
	 * Returns an output stream for a ByteBuffer. The write() methods use the relative ByteBuffer
	 * put() methods.
	 */
	private OutputStream createOutputStream() {
		return new OutputStream() {
			public synchronized void write(int b) throws IOException {
				outAppData.put((byte) b);
				outAppData.flip();
				doWrite(outAppData);
				outAppData.clear();
			}

			public synchronized void write(byte[] bytes, int off, int len) throws IOException {
				outAppData.put(bytes, off, len);
				outAppData.flip();
				doWrite(outAppData);
				outAppData.clear();
			}
		};
	}

}
