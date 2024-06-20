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

package com.io7m.brooklime.cmdline.internal;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.brooklime.api.BLErrorLogging;
import com.io7m.brooklime.api.BLException;
import com.io7m.brooklime.api.BLHTTPErrorException;
import com.io7m.brooklime.api.BLNexusClientConfiguration;
import com.io7m.brooklime.api.BLStagingRepositoryDrop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * A command to drop a staging repository.
 */

@Parameters(commandDescription = "Drop an existing staging repository")
public final class BLCommandDropStagingRepository extends BLCommandRoot
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLCommandDropStagingRepository.class);

  @Parameter(
    names = "--baseURI",
    description = "The Nexus URI",
    required = false
  )
  private URI baseURI = URI.create("https://oss.sonatype.org:443/");

  @Parameter(
    names = "--retrySeconds",
    description = "The seconds to wait between retries of failed requests",
    required = false
  )
  private long retrySeconds = 5L;

  @Parameter(
    names = "--retryCount",
    description = "The maximum number of times to retry failed requests",
    required = false
  )
  private int retryCount = 25;

  @Parameter(
    names = "--user",
    description = "The Nexus user name",
    required = true
  )
  private String userName;

  @Parameter(
    names = "--password",
    description = "The Nexus password",
    required = true
  )
  private String password;

  @Parameter(
    names = "--stagingProfileId",
    description = "The Nexus staging profile id",
    required = true
  )
  private String stagingProfileId;

  @Parameter(
    names = "--repository",
    description = "The staging repository ID",
    required = true
  )
  private String stagingRepositoryId;

  /**
   * A command to drop a staging repository.
   */

  public BLCommandDropStagingRepository()
  {

  }

  @Override
  public Status execute()
    throws BLException, IOException
  {
    if (super.execute() == Status.FAILURE) {
      return Status.FAILURE;
    }

    final var clients =
      BLServices.findClients();

    final var clientConfiguration =
      BLNexusClientConfiguration.builder()
        .setApplicationVersion(BLServices.findApplicationVersion())
        .setUserName(this.userName)
        .setPassword(this.password)
        .setBaseURI(this.baseURI)
        .setStagingProfileId(this.stagingProfileId)
        .setRetryCount(this.retryCount)
        .setRetryDelay(Duration.ofSeconds(this.retrySeconds))
        .build();

    try (var client = clients.createClient(clientConfiguration)) {
      BLChatter.getInstance().start();

      client.stagingRepositoryDrop(
        BLStagingRepositoryDrop.builder()
          .addStagingRepositories(this.stagingRepositoryId)
          .build()
      );

      while (true) {
        LOG.debug("waiting for repository to drop...");

        try {
          Thread.sleep(2_000L);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        final var repositoryOpt =
          client.stagingRepositoryGet(this.stagingRepositoryId);

        if (!repositoryOpt.isPresent()) {
          break;
        }
      }
    } catch (final BLHTTPErrorException e) {
      BLErrorLogging.logErrors(LOG, e.errors());
      LOG.error("HTTP error: ", e);
      return Status.FAILURE;
    } catch (final IOException | BLException e) {
      throw e;
    }

    return Status.SUCCESS;
  }
}
