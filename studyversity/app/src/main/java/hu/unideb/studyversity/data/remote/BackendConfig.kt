package hu.unideb.studyversity.data.remote

/**
 * Where the future backend lives. Defaults are the paths of the original deployment; override at build time
 * (gradle: -PapiBaseUrl=...) or edit here once the new server exists.
 */
object BackendConfig {
    /** When true ServiceLocator wires [RemoteBackend] instead of the on-device LocalBackend. */
    const val USE_REMOTE = false

    /** REST root; every ApiService path is relative to it. Must end with '/'. */
    const val API_BASE_URL = "https://example.invalid/studyversity/"

    /** Public document store (privacy policy, imprint, ... /displayDocument/<path>). */
    const val DOCUMENT_BASE_URL = "https://example.invalid/portal/displayDocument/"

    /** Plain JSON array of quotes [{id, quote, author, video}] for the mood game; empty = no quotes. */
    const val QUOTES_URL = ""

    /** OIDC discovery endpoint of the identity provider (the original used an OAuth2 code flow with PKCE). */
    const val OIDC_DISCOVERY_URL = "https://example.invalid/.well-known/openid-configuration"
}
