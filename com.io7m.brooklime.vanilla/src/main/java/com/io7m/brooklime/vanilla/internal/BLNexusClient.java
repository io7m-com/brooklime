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
import com.io7m.brooklime.api.BLNexusClientType;
import com.io7m.brooklime.api.BLProgressReceiverType;
import com.io7m.brooklime.api.BLStagingProfileRepository;
import com.io7m.brooklime.api.BLStagingRepositoryClose;
import com.io7m.brooklime.api.BLStagingRepositoryCreate;
import com.io7m.brooklime.api.BLStagingRepositoryDrop;
import com.io7m.brooklime.api.BLStagingRepositoryRelease;
import com.io7m.brooklime.api.BLStagingRepositoryUpload;
import com.io7m.brooklime.api.BLStagingRepositoryUploadRequestParameters;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A Nexus client.
 */

public final class BLNexusClient implements BLNexusClientType
{
  private final ScheduledExecutorService executor;
  private final HttpClient client;
  private final BLNexusRequests requests;
  private final Clock clock;

  /**
   * A Nexus client.
   *
   * @param inExecutor A statistics executor
   * @param inClient   An HTTP client
   * @param inRequests A request provider
   * @param inClock    A clock used to track time
   */

  public BLNexusClient(
    final ScheduledExecutorService inExecutor,
    final HttpClient inClient,
    final BLNexusRequests inRequests,
    final Clock inClock)
  {
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.client =
      Objects.requireNonNull(inClient, "client");
    this.requests =
      Objects.requireNonNull(inRequests, "inRequests");
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
  }

  @Override
  public void close()
    throws IOException
  {
    this.executor.shutdown();
  }

  @Override
  public void upload(
    final BLStagingRepositoryUpload upload,
    final BLProgressReceiverType receiver)
    throws BLException
  {
    Objects.requireNonNull(upload, "upload");
    Objects.requireNonNull(receiver, "receiver");

    final BLProgressCounter counter =
      new BLProgressCounter(this.clock, receiver);

    this.requests.upload(counter, upload);
  }

  @Override
  public BLStagingRepositoryUpload createUploadRequest(
    final BLStagingRepositoryUploadRequestParameters parameters)
    throws BLException
  {
    Objects.requireNonNull(parameters, "parameters");
    return this.requests.createUploadRequest(parameters);
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
    Objects.requireNonNull(id, "id");
    return this.requests.stagingRepository(id);
  }

  @Override
  public String stagingRepositoryCreate(
    final BLStagingRepositoryCreate create)
    throws BLException
  {
    Objects.requireNonNull(create, "create");
    return this.requests.stagingRepositoryCreate(create);
  }

  @Override
  public void stagingRepositoryDrop(
    final BLStagingRepositoryDrop drop)
    throws BLException
  {
    Objects.requireNonNull(drop, "drop");

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
    Objects.requireNonNull(close, "close");

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
    Objects.requireNonNull(release, "release");

    try {
      this.requests.stagingRepositoryRelease(release);
    } catch (final IOException e) {
      throw new BLException(e);
    }
  }
}
