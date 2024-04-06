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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public final class EUDP0Server implements Closeable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EUDP0Server.class);

  private final InetSocketAddress address;
  private final ConcurrentHashMap<SocketAddress, ServerClient> clients;
  private DatagramSocket socket;

  public EUDP0Server(
    final InetSocketAddress inAddress)
  {
    this.address =
      Objects.requireNonNull(inAddress, "address");
    this.clients =
      new ConcurrentHashMap<>();
  }

  public void start(
    final CountDownLatch bindLatch)
    throws IOException
  {
    this.socket = new DatagramSocket(this.address);
    bindLatch.countDown();

    final var receivePacketBuffer =
      new byte[512];

    while (!this.socket.isClosed()) {
      try {
        final var packet =
          new DatagramPacket(receivePacketBuffer, receivePacketBuffer.length);

        LOG.debug("receive");
        this.socket.receive(packet);

        LOG.debug(
          "received {} {}",
          Integer.valueOf(packet.getLength()),
          packet.getSocketAddress()
        );

        final var source =
          packet.getSocketAddress();

        final var message =
          EUDP0Messages.fromBytes(
            Arrays.copyOf(packet.getData(), packet.getLength())
          );

        var existing = this.clients.get(source);
        if (existing == null) {
          existing = new ServerClient(this, source);
          Thread.startVirtualThread(existing::doChatTask);
        }

        this.clients.put(source, existing);
        existing.onReceive(message);
      } catch (final EUDP0Exception e) {
        LOG.error("I/O: ", e);
      }
    }
  }

  @Override
  public void close()
    throws IOException
  {
    LOG.debug("Close");
    this.socket.close();
  }

  private static final class ServerClient
  {
    private final EUDP0Server server;
    private final SocketAddress source;
    private volatile boolean chatting;
    private volatile boolean loggedIn;

    private ServerClient(
      final EUDP0Server inServer,
      final SocketAddress inSource)
    {
      this.server =
        Objects.requireNonNull(inServer, "server");
      this.source =
        Objects.requireNonNull(inSource, "source");
      this.loggedIn =
        false;
      this.chatting =
        false;
    }

    public void onReceive(
      final EUDP0MessageType message)
      throws EUDP0Exception
    {
      switch (message) {
        case final EUDP0CommandType c -> {
          switch (c) {
            case final EUDP0CommandHello cc -> {
              this.onReceiveCommandHello(cc);
            }
            case final EUDP0CommandLogin cc -> {
              this.onReceiveCommandLogin(cc);
            }
          }
        }
        case final EUDP0ResponseType r -> {
          switch (r) {
            case final EUDP0ResponseFailure rr -> {
              // Ignore
            }
            case final EUDP0ResponseOK rr -> {
              // Ignore
            }
          }
        }
      }
    }

    private void onReceiveCommandHello(
      final EUDP0CommandHello cc)
      throws EUDP0Exception
    {
      if (!this.loggedIn) {
        this.server.send(
          new EUDP0ResponseFailure(
            UUID.randomUUID(),
            cc.messageId(),
            "Not logged in!"
          ),
          this.source
        );
        return;
      }

      if (Objects.equals(cc.message(), "Chatting")) {
        this.chatting = !this.chatting;
        LOG.debug("Chatting: {}", Boolean.valueOf(this.chatting));
      }

      this.server.send(
        new EUDP0ResponseOK(
          UUID.randomUUID(),
          cc.messageId()
        ),
        this.source
      );
    }

    private void onReceiveCommandLogin(
      final EUDP0CommandLogin cc)
      throws EUDP0Exception
    {
      if (Objects.equals(cc.user(), "someone")
          && Objects.equals(cc.password(), "password")) {
        this.loggedIn = true;
        this.server.send(
          new EUDP0ResponseOK(
            UUID.randomUUID(),
            cc.messageId()
          ),
          this.source
        );
        return;
      }

      this.server.send(
        new EUDP0ResponseFailure(
          UUID.randomUUID(),
          cc.messageId(),
          "Authentication failed!"
        ),
        this.source
      );
    }

    public void doChatTask()
    {
      var timeNext =
        Instant.now()
          .plusMillis(500L);

      while (!this.server.socket.isClosed()) {
        final var timeNow = Instant.now();
        if (timeNow.isAfter(timeNext)) {
          timeNext = timeNow.plusMillis(500L);
          if (this.chatting) {
            try {
              this.server.send(
                new EUDP0CommandHello(
                  UUID.randomUUID(),
                  "Hello!"
                ),
                this.source
              );
            } catch (final Exception e) {
              // Ignored
            }
          }
        }
      }
    }
  }

  private void send(
    final EUDP0MessageType message,
    final SocketAddress target)
    throws EUDP0Exception
  {
    final var msgBytes =
      EUDP0Messages.toBytes(message);
    final var packet =
      new DatagramPacket(msgBytes, msgBytes.length, target);

    try {
      this.socket.send(packet);
    } catch (final IOException e) {
      throw new EUDP0Exception(e);
    }
  }
}
