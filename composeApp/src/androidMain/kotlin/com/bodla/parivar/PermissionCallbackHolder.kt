package com.bodla.parivar

object PermissionCallbackHolder {
    /**
     * Holds a temporary callback for permission requests.
     * The callback is cleared after invocation to avoid leaks.
     */
    var callback: (() -> Unit)? = null
}
