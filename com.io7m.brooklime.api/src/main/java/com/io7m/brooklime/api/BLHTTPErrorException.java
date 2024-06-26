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

package com.io7m.brooklime.api;

import java.util.List;
import java.util.Objects;

/**
 * The type of exceptions raised by HTTP servers returning unexpected errors.
 */

public final class BLHTTPErrorException extends BLHTTPException
{
  private final int statusCode;
  private final String statusMessage;
  private final List<BLNexusError> errors;

  /**
   * Construct an exception.
   *
   * @param inStatusCode    The HTTP status code
   * @param inStatusMessage The HTTP status message
   * @param inErrors        The Nexus errors
   */

  public BLHTTPErrorException(
    final int inStatusCode,
    final String inStatusMessage,
    final List<BLNexusError> inErrors)
  {
    super(inStatusMessage);

    this.statusCode =
      inStatusCode;
    this.statusMessage =
      Objects.requireNonNull(inStatusMessage, "inStatusMessage");
    this.errors =
      List.copyOf(Objects.requireNonNull(inErrors, "inErrors"));
  }

  /**
   * @return The Nexus errors, if any
   */

  public List<BLNexusError> errors()
  {
    return this.errors;
  }

  /**
   * @return The HTTP status code
   */

  public int statusCode()
  {
    return this.statusCode;
  }

  /**
   * @return The HTTP status message
   */

  public String statusMessage()
  {
    return this.statusMessage;
  }
}
