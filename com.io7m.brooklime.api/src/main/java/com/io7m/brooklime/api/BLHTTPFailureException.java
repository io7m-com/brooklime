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

/**
 * The type of exceptions caused by failures to make HTTP requests.
 */

public class BLHTTPFailureException extends BLHTTPException
{
  /**
   * Construct an exception.
   *
   * @param message The message
   */

  public BLHTTPFailureException(
    final String message)
  {
    super(message);
  }

  /**
   * Construct an exception.
   *
   * @param message The message
   * @param cause   The cause
   */

  public BLHTTPFailureException(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Construct an exception.
   *
   * @param cause The cause
   */

  public BLHTTPFailureException(
    final Throwable cause)
  {
    super(cause);
  }
}
