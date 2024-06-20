/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.brooklime.vanilla;

import com.io7m.brooklime.api.BLApplicationVersion;
import com.io7m.brooklime.api.BLApplicationVersions;
import com.io7m.brooklime.api.BLNexusClientConfiguration;
import com.io7m.brooklime.api.BLNexusClientProviderType;
import com.io7m.brooklime.api.BLNexusClientType;
import com.io7m.brooklime.vanilla.internal.BLNexusClient;
import com.io7m.brooklime.vanilla.internal.BLNexusParsers;
import com.io7m.brooklime.vanilla.internal.BLNexusRequests;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieManager;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * The default provider of Nexus clients.
 */

public final class BLNexusClients implements BLNexusClientProviderType
{
  /**
   * Construct a client provider.
   */

  public BLNexusClients()
  {

  }

  /**
   * @return The application version
   *
   * @throws IOException On I/O errors
   */

  public static BLApplicationVersion findClientVersion()
    throws IOException
  {
    final var resource =
      BLNexusClients.class.getResource(
        "/com/io7m/brooklime/vanilla/version.properties"
      );

    try (var stream = resource.openStream()) {
      return BLApplicationVersions.ofStream(stream);
    }
  }

  private static String userAgent(
    final BLNexusClientConfiguration configuration)
    throws IOException
  {
    final var clientVersion = findClientVersion();

    return String.format(
      "%s/%s (%s/%s)",
      configuration.applicationVersion().applicationName(),
      configuration.applicationVersion().applicationVersion(),
      clientVersion.applicationName(),
      clientVersion.applicationVersion()
    );
  }

  private static final class BasicAuthenticator
    extends Authenticator
  {
    private final String username;
    private final char[] password;

    BasicAuthenticator(
      final String inUsername,
      final char[] inPassword)
    {
      this.username =
        Objects.requireNonNull(inUsername, "username");
      this.password =
        Objects.requireNonNull(inPassword, "password");
    }


    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
      return new PasswordAuthentication(
        this.username,
        this.password
      );
    }
  }

  @Override
  public BLNexusClientType createClient(
    final BLNexusClientConfiguration configuration)
  {
    Objects.requireNonNull(configuration, "configuration");

    final var executor =
      Executors.newScheduledThreadPool(1, r -> {
        final var thread = new Thread(r);
        thread.setName("com.io7m.brooklime.statistics-" + thread.getId());
        thread.setDaemon(true);
        return thread;
      });

    final var basicAuthenticator =
      new BasicAuthenticator(
        configuration.userName(),
        configuration.password().toCharArray()
      );

    final var httpClient =
      HttpClient.newBuilder()
        .authenticator(basicAuthenticator)
        .cookieHandler(new CookieManager())
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    final var parsers =
      new BLNexusParsers();
    final var requests =
      new BLNexusRequests(executor, httpClient, parsers, configuration);

    return new BLNexusClient(
      executor,
      httpClient,
      requests,
      Clock.systemUTC()
    );
  }
}
