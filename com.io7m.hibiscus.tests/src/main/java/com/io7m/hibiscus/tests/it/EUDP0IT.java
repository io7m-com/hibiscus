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


package com.io7m.hibiscus.tests.it;

import com.io7m.hibiscus.api.HBStateType.HBStateConnected;
import com.io7m.hibiscus.api.HBStateType.HBStateConnectionFailed;
import com.io7m.hibiscus.api.HBStateType.HBStateDisconnected;
import com.io7m.hibiscus.examples.udp0.EUDP0ClientType;
import com.io7m.hibiscus.examples.udp0.EUDP0Clients;
import com.io7m.hibiscus.examples.udp0.EUDP0CommandHello;
import com.io7m.hibiscus.examples.udp0.EUDP0Configuration;
import com.io7m.hibiscus.examples.udp0.EUDP0ConnectionParameters;
import com.io7m.hibiscus.examples.udp0.EUDP0Exception;
import com.io7m.hibiscus.examples.udp0.EUDP0ResponseOK;
import com.io7m.hibiscus.examples.udp0.EUDP0Server;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public final class EUDP0IT
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ETCP0IT.class);

  private static final Duration TIMEOUT = Duration.ofSeconds(10L);

  private static final int PORT = 45000;
  private static EUDP0Server SERVER;
  private static InetSocketAddress ADDRESS;

  private EUDP0Server server;
  private EUDP0Clients clients;
  private EUDP0ClientType client;
  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private LinkedBlockingDeque<String> clientStates;

  @BeforeAll
  public static void setupOnce()
    throws InterruptedException
  {
    ADDRESS =
      new InetSocketAddress("localhost", PORT);
    SERVER =
      new EUDP0Server(ADDRESS);

    final var latch = new CountDownLatch(1);
    Thread.startVirtualThread(() -> {
      try {
        SERVER.start(latch);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    });
    latch.await(60L, TimeUnit.SECONDS);
    LOG.debug("Server up!");
  }

  @AfterAll
  public static void teardownOnce()
    throws IOException
  {
    SERVER.close();
  }
  
  @BeforeEach
  public void setup(
    final TestInfo info)
  {
    LOG.debug("setup: Test: {}", info.getDisplayName());

    this.resources =
      CloseableCollection.create();

    this.clients =
      new EUDP0Clients();
    this.client =
      this.resources.add(this.clients.create(new EUDP0Configuration()));

    this.clientStates =
      new LinkedBlockingDeque<>();

    this.client.state()
      .subscribe(new HBPerpetualSubscriber<>(
        state -> this.clientStates.add(state.toString())
      ));
  }

  @AfterEach
  public void tearDown(
    final TestInfo info)
    throws Exception
  {
    LOG.debug("tearDown: Test: {}", info.getDisplayName());

    this.resources.close();
  }

  @Test
  public void testConnectAsk()
    throws Exception
  {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      final var parameters =
        new EUDP0ConnectionParameters(
          ADDRESS,
          "someone",
          "password",
          Duration.ofSeconds(1L)
        );

      assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
      this.client.connect(parameters);
      assertInstanceOf(HBStateConnected.class, this.client.stateNow());

      {
        final var r =
          this.client.sendAndWait(
            new EUDP0CommandHello(UUID.randomUUID(), "Hello!"),
            Duration.ofSeconds(1L)
          );

        assertInstanceOf(EUDP0ResponseOK.class, r);
      }

      {
        final var r =
          this.client.sendAndWait(
            new EUDP0CommandHello(UUID.randomUUID(), "Hello!"),
            Duration.ofSeconds(1L)
          );

        assertInstanceOf(EUDP0ResponseOK.class, r);
      }

      {
        final var r =
          this.client.sendAndWait(
            new EUDP0CommandHello(UUID.randomUUID(), "Hello!"),
            Duration.ofSeconds(1L)
          );

        assertInstanceOf(EUDP0ResponseOK.class, r);
      }

      assertEquals(
        List.of(
          "CONNECTING",
          "CONNECTION_SUCCEEDED",
          "CONNECTED"
        ),
        List.copyOf(this.clientStates)
      );
    });
  }

  @Test
  public void testConnectSend()
    throws Exception
  {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      final var parameters =
        new EUDP0ConnectionParameters(
          ADDRESS,
          "someone",
          "password",
          Duration.ofSeconds(1L)
        );

      assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
      this.client.connect(parameters);
      assertInstanceOf(HBStateConnected.class, this.client.stateNow());

      this.client.send(new EUDP0CommandHello(UUID.randomUUID(), "Hello!"));
      this.client.send(new EUDP0CommandHello(UUID.randomUUID(), "Hello!"));
      this.client.send(new EUDP0CommandHello(UUID.randomUUID(), "Hello!"));

      assertEquals(
        List.of(
          "CONNECTING",
          "CONNECTION_SUCCEEDED",
          "CONNECTED"
        ),
        List.copyOf(this.clientStates)
      );
    });
  }

  @Test
  public void testConnectFailure0()
    throws Exception
  {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      final var parameters =
        new EUDP0ConnectionParameters(
          ADDRESS,
          "someone",
          "wrong!",
          Duration.ofSeconds(1L)
        );

      assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
      this.client.connect(parameters);
      assertInstanceOf(HBStateConnectionFailed.class, this.client.stateNow());

      assertEquals(
        List.of(
          "CONNECTING",
          "CONNECTION_FAILED"
        ),
        List.copyOf(this.clientStates)
      );
    });
  }

  @Test
  public void testConnectFailure1()
    throws Exception
  {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      final var parameters =
        new EUDP0ConnectionParameters(
          new InetSocketAddress(
            ADDRESS.getAddress(),
            9999
          ),
          "someone",
          "wrong!",
          Duration.ofSeconds(1L)
        );

      assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
      this.client.connect(parameters);
      assertInstanceOf(HBStateConnectionFailed.class, this.client.stateNow());

      assertEquals(
        List.of(
          "CONNECTING",
          "CONNECTION_FAILED"
        ),
        List.copyOf(this.clientStates)
      );
    });
  }

  @Test
  public void testNotConnected()
    throws Exception
  {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      final var parameters =
        new EUDP0ConnectionParameters(
          ADDRESS,
          "someone",
          "password",
          Duration.ofSeconds(1L)
        );

      assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());

      assertThrows(EUDP0Exception.class, () -> {
        this.client.receive(Duration.ZERO);
      });
      assertThrows(EUDP0Exception.class, () -> {
        this.client.send(new EUDP0CommandHello(UUID.randomUUID(), "Hello!"));
      });
      assertThrows(EUDP0Exception.class, () -> {
        this.client.sendAndWait(
          new EUDP0CommandHello(UUID.randomUUID(), "Hello!"),
          Duration.ofSeconds(1L)
        );
      });
    });
  }

  @Test
  public void testConnectConnect()
    throws Exception
  {
    assertTimeoutPreemptively(TIMEOUT, () -> {
      final var parameters =
        new EUDP0ConnectionParameters(
          ADDRESS,
          "someone",
          "password",
          Duration.ofSeconds(1L)
        );

      assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
      this.client.connect(parameters);
      assertInstanceOf(HBStateConnected.class, this.client.stateNow());
      this.client.connect(parameters);
      assertInstanceOf(HBStateConnected.class, this.client.stateNow());

      assertEquals(
        List.of(
          "CONNECTING",
          "CONNECTION_SUCCEEDED",
          "CONNECTED",
          "DISCONNECTED",
          "CONNECTING",
          "CONNECTION_SUCCEEDED",
          "CONNECTED"
        ),
        List.copyOf(this.clientStates)
      );
    });
  }
}
