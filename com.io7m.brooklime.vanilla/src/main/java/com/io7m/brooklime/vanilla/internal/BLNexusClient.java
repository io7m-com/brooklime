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

package com.io7m.brooklime.vanilla.internal;

import com.io7m.brooklime.api.BLException;
import com.io7m.brooklime.api.BLNexusClientConfiguration;
import com.io7m.brooklime.api.BLNexusClientType;
import com.io7m.brooklime.api.BLStagingProfileRepository;
import com.io7m.brooklime.api.BLStagingRepositoryClose;
import com.io7m.brooklime.api.BLStagingRepositoryCreate;
import com.io7m.brooklime.api.BLStagingRepositoryDrop;
import com.io7m.brooklime.api.BLStagingRepositoryRelease;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BLNexusClient implements BLNexusClientType
{
  private final CloseableHttpClient client;
  private final BLNexusClientConfiguration configuration;
  private final BLNexusRequests requests;

  public BLNexusClient(
    final CloseableHttpClient inClient,
    final BLNexusClientConfiguration inConfiguration,
    final BLNexusRequests inRequests)
  {
    this.client =
      Objects.requireNonNull(inClient, "client");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.requests =
      Objects.requireNonNull(inRequests, "inRequests");
  }

  @Override
  public void close()
    throws IOException
  {
    this.client.close();
  }

  @Override
  public List<BLStagingProfileRepository> stagingRepositories()
    throws BLException
  {
    return this.requests.stagingRepositories();
  }

  @Override
  public Optional<BLStagingProfileRepository> stagingRepositoryGet(
    final String id)
    throws BLException
  {
    return this.requests.stagingRepository(id);
  }

  @Override
  public String stagingRepositoryCreate(
    final BLStagingRepositoryCreate create)
    throws BLException
  {
    return this.requests.stagingRepositoryCreate(create);
  }

  @Override
  public void stagingRepositoryDrop(
    final BLStagingRepositoryDrop drop)
    throws BLException
  {
    try {
      this.requests.stagingRepositoryDrop(drop);
    } catch (final IOException e) {
      throw new BLException(e);
    }
  }

  @Override
  public void stagingRepositoryClose(
    final BLStagingRepositoryClose close)
    throws BLException
  {
    try {
      this.requests.stagingRepositoryClose(close);
    } catch (final IOException e) {
      throw new BLException(e);
    }
  }

  @Override
  public void stagingRepositoryRelease(
    final BLStagingRepositoryRelease release)
    throws BLException
  {
    try {
      this.requests.stagingRepositoryRelease(release);
    } catch (final IOException e) {
      throw new BLException(e);
    }
  }
}
