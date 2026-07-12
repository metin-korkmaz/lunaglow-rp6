# Release signing

Android accepts an update only when it is signed by the same key as the
installed version. Losing this key forces every installation to be removed
before a differently signed build can be installed.

The release workflow expects these GitHub Actions secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Generate the key once with `keytool`, back up the keystore in two encrypted
offline locations, and store the alias and both passwords in a password
manager. Never commit the keystore or print a secret in CI output.
