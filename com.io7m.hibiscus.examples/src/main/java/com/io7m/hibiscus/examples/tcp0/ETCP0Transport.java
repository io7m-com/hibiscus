/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.hibiscus.examples.tcp0;

import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class ETCP0Transport
  implements HBTransportType<ETCP0MessageType, ETCP0Exception>
{
  private final CloseableCollectionType<ETCP0Exception> resources;
  private final Socket socket;
  private final DataInputStream input;
  private final DataOutputStream output;
  private final LinkedBlockingQueue<ETCP0MessageType> inbox;
  private final Thread readerThread;

  ETCP0Transport(
    final Socket inSocket,
    final InputStream inInputStream,
    final OutputStream inOutputStream)
  {
    this.resources =
      CloseableCollection.create(() -> new ETCP0Exception(
        "Failed to close resources."));

    this.socket =
      this.resources.add(
        Objects.requireNonNull(inSocket, "inCloseable"));
    this.input =
      this.resources.add(
        new DataInputStream(
          Objects.requireNonNull(inInputStream, "inInput")));
    this.output =
      this.resources.add(
        new DataOutputStream(
          Objects.requireNonNull(inOutputStream, "inOutput")));

    this.inbox =
      new LinkedBlockingQueue<>();
    this.readerThread =
      Thread.startVirtualThread(this::readLoop);
  }

  private void readLoop()
  {
    while (true) {
      try {
        final var msgLength =
          this.input.readInt();
        final var msgData =
          this.input.readNBytes(msgLength);

        this.inbox.add(ETCP0Messages.fromBytes(msgData));
      } catch (final Throwable e) {
        try {
          this.close();
          return;
        } catch (final Throwable ex) {
          throw new RuntimeException(ex);
        }
      }
    }
  }

  @Override
  public Optional<ETCP0MessageType> read(
    final Duration timeout)
    throws ETCP0Exception
  {
    Objects.requireNonNull(timeout, "timeout");

    try {
      if (this.isClosed()) {
        throw new ETCP0Exception(new ClosedChannelException());
      }
      return Optional.ofNullable(
        this.inbox.poll(timeout.toNanos(), TimeUnit.NANOSECONDS)
      );
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  @Override
  public void write(
    final ETCP0MessageType message)
    throws ETCP0Exception
  {
    try {
      final var msgBytes = ETCP0Messages.toBytes(message);
      this.output.writeInt(msgBytes.length);
      this.output.write(msgBytes);
      this.output.flush();
    } catch (final Exception e) {
      this.close();
      throw new ETCP0Exception(e);
    }
  }

  @Override
  public void close()
    throws ETCP0Exception
  {
    this.resources.close();
  }

  @Override
  public boolean isClosed()
  {
    if (this.socket.isClosed()) {
      return true;
    }
    return !this.socket.isConnected();
  }
}
