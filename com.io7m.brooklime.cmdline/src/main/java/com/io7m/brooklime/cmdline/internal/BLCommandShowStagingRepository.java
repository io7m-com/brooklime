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
import com.io7m.brooklime.api.BLNexusClientConfiguration;
import com.io7m.brooklime.api.BLNexusClientProviderType;
import com.io7m.brooklime.api.BLNexusClientType;
import com.io7m.brooklime.api.BLStagingProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * A command to show a staging repository.
 */

@Parameters(commandDescription = "Show an existing staging repository")
public final class BLCommandShowStagingRepository extends BLCommandRoot
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLCommandShowStagingRepository.class);

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
   * A command to show a staging repository.
   */

  public BLCommandShowStagingRepository()
  {

  }

  @Override
  public Status execute()
    throws Exception
  {
    if (super.execute() == Status.FAILURE) {
      return Status.FAILURE;
    }

    final BLNexusClientProviderType clients = BLServices.findClients();

    final BLNexusClientConfiguration clientConfiguration =
      BLNexusClientConfiguration.builder()
        .setApplicationVersion(BLServices.findApplicationVersion())
        .setUserName(this.userName)
        .setPassword(this.password)
        .setBaseURI(this.baseURI)
        .setStagingProfileId(this.stagingProfileId)
        .setRetryCount(this.retryCount)
        .setRetryDelay(Duration.ofSeconds(this.retrySeconds))
        .build();

    try (BLNexusClientType client = clients.createClient(clientConfiguration)) {
      BLChatter.getInstance().start();

      final Optional<BLStagingProfileRepository> repositoryOpt =
        client.stagingRepositoryGet(this.stagingRepositoryId);

      if (repositoryOpt.isPresent()) {
        final BLStagingProfileRepository repository = repositoryOpt.get();
        System.out.println("Created: " + repository.created());
        System.out.println("Description: " + repository.description());
        System.out.println("IP: " + repository.ipAddress());
        System.out.println("Notifications: " + repository.notifications());
        System.out.println("Policy: " + repository.policy());
        System.out.println("Profile ID: " + repository.profileId());
        System.out.println("Profile name: " + repository.profileName());
        System.out.println("Profile type: " + repository.profileType());
        System.out.println("Provider: " + repository.provider());
        System.out.println("Release repository ID: " + repository.releaseRepositoryId());
        System.out.println("Release repository name: " + repository.releaseRepositoryName());
        System.out.println("Repository URI: " + repository.repositoryURI());
        System.out.println("Repository: " + repository.repositoryId());
        System.out.println("Transitioning: " + repository.transitioning());
        System.out.println("Type: " + repository.type());
        System.out.println("Updated: " + repository.updated());
        System.out.println("User ID: " + repository.userId());
        System.out.println("User agent: " + repository.userAgent());
      } else {
        LOG.error("No such repository");
        return Status.FAILURE;
      }
    }

    return Status.SUCCESS;
  }
}
