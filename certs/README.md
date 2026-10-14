# certs

Put the CockroachDB Cloud CA certificate here as `root.crt` (download it from the cluster's "Connect" dialog).
It is a public CA certificate, not a secret. Confirm the file starts with `-----BEGIN CERTIFICATE-----`
and never contains a private key before committing it.
