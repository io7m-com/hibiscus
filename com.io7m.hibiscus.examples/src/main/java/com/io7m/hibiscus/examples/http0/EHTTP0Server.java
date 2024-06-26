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


package com.io7m.hibiscus.examples.http0;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.http.HttpRouting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;

import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static java.net.StandardSocketOptions.SO_REUSEPORT;

public final class EHTTP0Server implements Closeable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EHTTP0Server.class);

  private final InetSocketAddress address;
  private WebServer webServer;

  public EHTTP0Server(
    final InetSocketAddress inAddress)
  {
    this.address =
      Objects.requireNonNull(inAddress, "address");

    final var handler = new EHTTP0ServerHandler();
    final var routing = HttpRouting.builder();
    routing.post("/", handler);
    routing.post("/*", handler);

    this.webServer =
      WebServerConfig.builder()
        .port(this.address.getPort())
        .address(this.address.getAddress())
        .routing(routing)
        .listenerSocketOptions(Map.ofEntries(
          Map.entry(SO_REUSEADDR, Boolean.TRUE),
          Map.entry(SO_REUSEPORT, Boolean.TRUE)
        ))
        .build();
  }

  public void start()
    throws InterruptedException
  {
    this.webServer.start();

    while (!this.webServer.isRunning()) {
      Thread.sleep(100L);
    }
  }

  @Override
  public void close()
    throws IOException
  {
    LOG.debug("Close");
    this.webServer.stop();
  }
}
