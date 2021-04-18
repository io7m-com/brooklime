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
import com.io7m.brooklime.api.BLStagingRepositoryCreate;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

@Parameters(commandDescription = "Create a staging repository")
public final class BLCommandCreateStagingRepository extends BLCommandRoot
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLCommandCreateStagingRepository.class);

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
    names = "--baseURI",
    description = "The Nexus URI",
    required = false
  )
  private URI baseURI = URI.create("https://oss.sonatype.org:443/");

  @Parameter(
    names = "--description",
    description = "The staging repository description",
    required = true
  )
  private String stagingRepositoryDescription;

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
    names = "--outputFile",
    description = "The output file that will contain the staging repository ID",
    required = false
  )
  private Path outputFile;

  public BLCommandCreateStagingRepository()
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

    try (PrintStream output = this.outputStream()) {
      try (BLNexusClientType client = clients.createClient(clientConfiguration)) {
        BLChatter.getInstance().start();

        final String repository =
          client.stagingRepositoryCreate(
            BLStagingRepositoryCreate.builder()
              .setDescription(this.stagingRepositoryDescription)
              .build()
          );

        output.println(repository);
        output.flush();
      }
    }

    return Status.SUCCESS;
  }

  private PrintStream outputStream()
    throws IOException
  {
    if (this.outputFile != null) {
      return new PrintStream(
        Files.newOutputStream(this.outputFile, CREATE, TRUNCATE_EXISTING, WRITE),
        false,
        StandardCharsets.UTF_8.name()
      );
    }

    return new PrintStream(
      new CloseShieldOutputStream(System.out),
      false,
      StandardCharsets.UTF_8.name()
    );
  }
}
