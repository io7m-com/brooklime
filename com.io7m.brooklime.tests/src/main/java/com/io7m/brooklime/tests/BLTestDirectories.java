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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public final class BLTestDirectories
{
  private static final Logger LOGGER =
    LoggerFactory.getLogger(BLTestDirectories.class);

  private BLTestDirectories()
  {

  }

  public static Path createBaseDirectory()
    throws IOException
  {
    final Path path =
      Paths.get(System.getProperty("java.io.tmpdir")).resolve("brooklime");
    Files.createDirectories(path);
    return path;
  }

  public static Path createTempDirectory()
    throws IOException
  {
    final Path path = createBaseDirectory();
    final Path temp = path.resolve(UUID.randomUUID().toString());
    Files.createDirectories(temp);
    return temp;
  }

  public static Path resourceOf(
    final Class<?> clazz,
    final Path output,
    final String name)
    throws IOException
  {
    final String internal = String.format("/com/io7m/brooklime/tests/%s", name);
    final URL url = clazz.getResource(internal);
    if (url == null) {
      throw new NoSuchFileException(internal);
    }

    final Path target = output.resolve(name);
    LOGGER.debug("copy {} {}", name, target);

    try (InputStream stream = url.openStream()) {
      Files.copy(stream, target);
    }
    return target;
  }

  public static Path resourceOf(
    final Path output,
    final String name)
    throws IOException
  {
    return resourceOf(BLTestDirectories.class, output, name);
  }

  public static InputStream resourceStreamOf(
    final Class<?> clazz,
    final Path output,
    final String name)
    throws IOException
  {
    return Files.newInputStream(resourceOf(clazz, output, name));
  }

  public static InputStream resourceStreamOf(
    final Path output,
    final String name)
    throws IOException
  {
    return resourceStreamOf(BLTestDirectories.class, output, name);
  }

  public static byte[] resourceBytesOf(
    final Class<?> clazz,
    final Path output,
    final String name)
    throws IOException
  {
    return IOUtils.toByteArray(
      Files.newInputStream(resourceOf(clazz, output, name))
    );
  }

  public static byte[] resourceBytesOf(
    final Path output,
    final String name)
    throws IOException
  {
    return resourceBytesOf(BLTestDirectories.class, output, name);
  }
}
