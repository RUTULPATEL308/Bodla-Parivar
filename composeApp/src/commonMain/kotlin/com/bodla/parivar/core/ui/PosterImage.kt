package com.bodla.parivar.core.ui

/**
 * Scales a captured poster down so it stays small enough to be used as a push
 * notification picture. FCM downloads the notification image on its own and
 * drops pictures that are too large, which would leave the notification
 * without its poster.
 *
 * The longest edge of the result is at most [maxEdgePx] pixels. The original
 * bytes are returned unchanged when they are already small enough or when the
 * image cannot be decoded.
 */
expect fun PlatformShrinkPoster(png: ByteArray, maxEdgePx: Int = 1000): ByteArray
