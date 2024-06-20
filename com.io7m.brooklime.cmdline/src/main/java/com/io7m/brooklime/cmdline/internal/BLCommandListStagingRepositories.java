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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * A command to list staging repositories.
 */

@Parameters(commandDescription = "List the current staging repositories")
public final class BLCommandListStagingRepositories extends BLCommandRoot
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLCommandListStagingRepositories.class);

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

  /**
   * A command to list staging repositories.
   */

  public BLCommandListStagingRepositories()
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

      final var repositories =
        client.stagingRepositories();

      if (repositories.isEmpty()) {
        LOG.debug("no staging repositories available");
        return Status.SUCCESS;
      }

      System.out.printf(
        "%-32s %-8s %-64s\n",
        "ID",
        "Status",
        "Description"
      );

      for (final var repository : repositories) {
        System.out.printf(
          "%-32s %-8s %-64s\n",
          repository.repositoryId(),
          repository.type(),
          repository.description()
        );
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
