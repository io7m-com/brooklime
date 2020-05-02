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
import com.io7m.brooklime.vanilla.internal.BLNexusClient;
import com.io7m.brooklime.vanilla.internal.BLNexusParsers;
import com.io7m.brooklime.vanilla.internal.BLNexusRequests;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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

  @Override
  public BLNexusClientType createClient(
    final BLNexusClientConfiguration configuration)
    throws BLException
  {
    try {
      final BLApplicationVersion clientVersion =
        findClientVersion();

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

      final CloseableHttpClient client =
        HttpClients.custom()
          .setDefaultCredentialsProvider(credsProvider)
          .build();

      final BLNexusParsers parsers =
        new BLNexusParsers();
      final BLNexusRequests requests =
        new BLNexusRequests(client, clientVersion, parsers, configuration);

      return new BLNexusClient(client, configuration, requests);
    } catch (final IOException e) {
      throw new BLException(e);
    }
  }
}
