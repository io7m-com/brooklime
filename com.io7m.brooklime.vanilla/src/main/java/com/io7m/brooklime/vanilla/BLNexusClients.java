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
import com.io7m.brooklime.api.BLException;
import com.io7m.brooklime.api.BLNexusClientConfiguration;
import com.io7m.brooklime.api.BLNexusClientProviderType;
import com.io7m.brooklime.api.BLNexusClientType;
import com.io7m.brooklime.vanilla.internal.BLAggressiveRetryStrategy;
import com.io7m.brooklime.vanilla.internal.BLNexusClient;
import com.io7m.brooklime.vanilla.internal.BLNexusParsers;
import com.io7m.brooklime.vanilla.internal.BLNexusRequests;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Clock;

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

  public static BLApplicationVersion findClientVersion()
    throws IOException
  {
    final URL resource =
      BLNexusClients.class.getResource(
        "/com/io7m/brooklime/vanilla/version.properties"
      );

    try (InputStream stream = resource.openStream()) {
      return BLApplicationVersions.ofStream(stream);
    }
  }

  private static String userAgent(
    final BLNexusClientConfiguration configuration)
    throws IOException
  {
    final BLApplicationVersion clientVersion = findClientVersion();

    return String.format(
      "%s/%s (%s/%s)",
      configuration.applicationVersion().applicationName(),
      configuration.applicationVersion().applicationVersion(),
      clientVersion.applicationName(),
      clientVersion.applicationVersion()
    );
  }

  @Override
  public BLNexusClientType createClient(
    final BLNexusClientConfiguration configuration)
    throws BLException
  {
    try {
      final BasicCredentialsProvider credsProvider =
        new BasicCredentialsProvider();

      credsProvider.setCredentials(
        new AuthScope(
          configuration.baseURI().getHost(),
          configuration.baseURI().getPort()
        ),
        new UsernamePasswordCredentials(
          configuration.userName(),
          configuration.password().toCharArray()
        )
      );

      final BLAggressiveRetryStrategy retryStrategy =
        new BLAggressiveRetryStrategy(
          configuration.retryCount(),
          TimeValue.ofSeconds(configuration.retryDelay().getSeconds())
        );

      final CloseableHttpClient client =
        HttpClientBuilder.create()
          .setUserAgent(userAgent(configuration))
          .setDefaultCredentialsProvider(credsProvider)
          .setRetryStrategy(retryStrategy)
          .build();

      final BLNexusParsers parsers =
        new BLNexusParsers();
      final BLNexusRequests requests =
        new BLNexusRequests(client, parsers, configuration);

      return new BLNexusClient(
        client,
        requests,
        Clock.systemUTC()
      );
    } catch (final IOException e) {
      throw new BLException(e);
    }
  }
}
