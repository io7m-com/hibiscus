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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public final class ETCP0Server implements Closeable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ETCP0Server.class);

  private final InetSocketAddress address;
  private final ConcurrentHashMap.KeySetView<Socket, Boolean> sockets;
  private ServerSocket socket;

  public ETCP0Server(
    final InetSocketAddress inAddress)
    throws IOException
  {
    this.address =
      Objects.requireNonNull(inAddress, "address");
    this.sockets =
      ConcurrentHashMap.newKeySet();
    this.socket =
      new ServerSocket();
  }

  public void start(
    final CountDownLatch bindLatch)
    throws IOException
  {
    this.socket.setReuseAddress(true);
    this.socket.bind(this.address);
    bindLatch.countDown();

    while (true) {
      try {
        final var client = this.socket.accept();
        LOG.debug("Connected: {}", client.getRemoteSocketAddress());
        this.sockets.add(client);
        Thread.startVirtualThread(() -> {
          new ServerClient(this, client)
            .run();
        });
      } catch (final IOException e) {
        // Not a problem.
      }
    }
  }

  @Override
  public void close()
    throws IOException
  {
    LOG.debug("Close");

    for (final var s : this.sockets) {
      try {
        s.close();
      } catch (final IOException e) {
        // Don't care.
      }
    }

    try {
      this.socket.close();
    } catch (final IOException e) {
      // Don't care.
    }
  }

  private static final class ServerClient
    implements Runnable
  {
    private final ETCP0Server server;
    private final Socket socket;
    private final ReentrantLock socketLock;
    private DataInputStream input;
    private DataOutputStream output;
    private volatile boolean chatting;

    private ServerClient(
      final ETCP0Server inServer,
      final Socket inSocket)
    {
      this.server =
        Objects.requireNonNull(inServer, "server");
      this.socket =
        Objects.requireNonNull(inSocket, "socket");
      this.socketLock =
        new ReentrantLock();
    }

    @Override
    public void run()
    {
      try {
        this.runMain();
      } catch (final Exception e) {
        LOG.debug("Client: ", e);
        try {
          this.close();
        } catch (final IOException ex) {
          LOG.error("Close: ", ex);
        }
      }
    }

    private void writeMessage(
      final byte[] data)
      throws IOException
    {
      this.socketLock.lock();
      try {
        this.output.writeInt(data.length);
        this.output.write(data);
        this.output.flush();
      } finally {
        this.socketLock.unlock();
      }
    }

    private void runMain()
      throws Exception
    {
      try {
        this.input =
          new DataInputStream(this.socket.getInputStream());
        this.output =
          new DataOutputStream(this.socket.getOutputStream());

        this.doLogin();
        Thread.startVirtualThread(this::doChatTask);

        while (true) {
          this.doMessage();
        }
      } finally {
        this.close();
      }
    }

    private void doChatTask()
    {
      var timeNext =
        Instant.now()
          .plusMillis(500L);

      while (!this.socket.isClosed()) {
        final var timeNow = Instant.now();
        if (timeNow.isAfter(timeNext)) {
          timeNext = timeNow.plusMillis(500L);
          if (this.chatting) {
            try {
              this.sendHello();
            } catch (final Exception e) {
              // Ignored
            }
          }
        }
      }
    }

    private void sendHello()
      throws IOException, ETCP0Exception
    {
      LOG.debug("sendHello");

      final var res =
        new ETCP0CommandHello(UUID.randomUUID(), "Hello!");

      this.writeMessage(ETCP0Messages.toBytes(res));
    }

    private void doMessage()
      throws Exception
    {
      final var msgLen =
        this.input.readInt();
      final var msgBytes =
        this.input.readNBytes(msgLen);
      final var msg =
        ETCP0Messages.fromBytes(msgBytes);

      LOG.debug("Received: {}", msg);

      switch (msg) {
        case final ETCP0CommandType c -> {
          switch (c) {
            case final ETCP0CommandHello cc -> {
              if (Objects.equals(cc.message(), "Chatting")) {
                 this.chatting = !this.chatting;
                 LOG.debug("Chatting: {}", Boolean.valueOf(this.chatting));
              }
              this.sendResponseOK(msg);
            }
            case final ETCP0CommandLogin cc -> {
              throw this.sendFail(msg, "Can't use a Login message here!");
            }
          }
        }
        case final ETCP0ResponseType r -> {
          throw this.sendFail(msg, "Can't use a Response message here!");
        }
      }
    }

    private void sendResponseOK(
      final ETCP0MessageType msg)
      throws IOException, ETCP0Exception
    {
      final var res =
        new ETCP0ResponseOK(
          UUID.randomUUID(),
          msg.messageId()
        );

      this.writeMessage(ETCP0Messages.toBytes(res));
    }

    private Exception sendFail(
      final ETCP0MessageType msg,
      final String errorMessage)
      throws Exception
    {
      final var res =
        new ETCP0ResponseFailure(
          UUID.randomUUID(),
          msg.messageId(),
          errorMessage
        );

      this.writeMessage(ETCP0Messages.toBytes(res));
      this.close();
      return new Exception(errorMessage);
    }

    private void close()
      throws IOException
    {
      this.server.sockets.remove(this.socket);
      this.socket.close();
    }

    private void doLogin()
      throws Exception
    {
      LOG.debug("Awaiting login...");

      final var msgLen =
        this.input.readInt();
      final var msgBytes =
        this.input.readNBytes(msgLen);
      final var msg =
        ETCP0Messages.fromBytes(msgBytes);

      if (msg instanceof final ETCP0CommandLogin login) {
        if (Objects.equals(login.user(), "someone")
            && Objects.equals(login.password(), "password")) {
          LOG.debug("Logged in!");

          final var res =
            new ETCP0ResponseOK(UUID.randomUUID(), msg.messageId());

          this.writeMessage(ETCP0Messages.toBytes(res));
          return;
        }
      }

      throw this.sendFail(msg, "Login failed!");
    }
  }
}
