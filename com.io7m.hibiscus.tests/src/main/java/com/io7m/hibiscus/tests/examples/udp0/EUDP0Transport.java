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


package com.io7m.hibiscus.tests.examples.udp0;

import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class EUDP0Transport
  implements HBTransportType<EUDP0MessageType, EUDP0Exception>
{
  private final CloseableCollectionType<EUDP0Exception> resources;
  private final DatagramSocket socket;
  private final LinkedBlockingQueue<EUDP0MessageType> inbox;
  private final Thread readerThread;
  private final InetSocketAddress remoteAddress;

  EUDP0Transport(
    final InetSocketAddress inRemoteAddress,
    final DatagramSocket inSocket)
  {
    this.remoteAddress =
      Objects.requireNonNull(inRemoteAddress, "inRemoteAddress");
    this.resources =
      CloseableCollection.create(() -> {
        return new EUDP0Exception("Failed to close resources.");
      });

    this.socket =
      this.resources.add(
        Objects.requireNonNull(inSocket, "inCloseable"));

    this.inbox =
      new LinkedBlockingQueue<>();
    this.readerThread =
      Thread.startVirtualThread(this::readLoop);
  }

  private void readLoop()
  {
    while (true) {
      try {
        final var data =
          new byte[512];
        final var packet =
          new DatagramPacket(data, data.length);

        this.socket.receive(packet);

        final var message =
          EUDP0Messages.fromBytes(
            Arrays.copyOf(packet.getData(), packet.getLength())
          );

        this.inbox.add(message);
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
  public Optional<EUDP0MessageType> read(
    final Duration timeout)
    throws EUDP0Exception
  {
    Objects.requireNonNull(timeout, "timeout");

    try {
      if (this.isClosed()) {
        throw new EUDP0Exception(new ClosedChannelException());
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
    final EUDP0MessageType message)
    throws EUDP0Exception
  {
    try {
      final var msgBytes =
        EUDP0Messages.toBytes(message);
      final var packet =
        new DatagramPacket(msgBytes, msgBytes.length, this.remoteAddress);

      this.socket.send(packet);
    } catch (final Exception e) {
      this.close();
      throw new EUDP0Exception(e);
    }
  }

  @Override
  public void close()
    throws EUDP0Exception
  {
    this.resources.close();
  }

  @Override
  public boolean isClosed()
  {
    return this.socket.isClosed();
  }
}
