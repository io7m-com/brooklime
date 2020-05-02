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

package com.io7m.brooklime.tests;

import com.io7m.brooklime.api.BLApplicationVersion;
import com.io7m.brooklime.api.BLNexusClientConfiguration;
import com.io7m.brooklime.api.BLStagingProfileRepository;
import com.io7m.brooklime.api.BLStagingRepositoryClose;
import com.io7m.brooklime.api.BLStagingRepositoryCreateType;
import com.io7m.brooklime.api.BLStagingRepositoryDropType;
import com.io7m.brooklime.api.BLStagingRepositoryReleaseType;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

public final class BLEqualsTest
{
  @Test
  public void testStagingProfileRepositoryEquals()
  {
    EqualsVerifier.forClass(BLStagingProfileRepository.class)
      .withNonnullFields(
        "profileId",
        "profileName",
        "profileType",
        "repositoryId",
        "type",
        "policy",
        "userId",
        "userAgent",
        "ipAddress",
        "repositoryURI",
        "created",
        "updated",
        "description",
        "provider",
        "releaseRepositoryId",
        "releaseRepositoryName",
        "notifications"
      ).verify();
  }

  @Test
  public void testStagingRepositoryClose()
  {
    EqualsVerifier.forClass(BLStagingRepositoryClose.class)
      .withNonnullFields("stagingRepositories")
      .verify();
  }

  @Test
  public void testApplicationVersion()
  {
    EqualsVerifier.forClass(BLApplicationVersion.class)
      .withNonnullFields("applicationName", "applicationVersion")
      .verify();
  }

  @Test
  public void testNexusClientConfiguration()
  {
    EqualsVerifier.forClass(BLNexusClientConfiguration.class)
      .withNonnullFields(
        "applicationVersion",
        "userName",
        "password",
        "stagingProfileId",
        "baseURI"
      ).verify();
  }

  @TestFactory
  public Stream<DynamicTest> testBulkRequests()
  {
    return Stream.of(
      BLStagingRepositoryCreateType.class,
      BLStagingRepositoryDropType.class,
      BLStagingRepositoryReleaseType.class)
      .map(c -> {
        return DynamicTest.dynamicTest("test" + c.getCanonicalName(), () -> {
          EqualsVerifier.forClass(c)
            .verify();
        });
      });
  }
}
