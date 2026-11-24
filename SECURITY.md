# Security and privacy

M0 does not read or persist biometrics; it checks only Health Connect availability and the number of granted permissions. Sable Health does not operate a service, advertise itself as a medical device, claim direct Fitbit Air Bluetooth support, or collect telemetry.

Do not file public tickets containing actual medical/health values, account information, personal GPS routes, OAuth tokens, real Google Health JSON or identifying screenshots. Report security issues privately to the repository maintainer via GitHub security advisory if available.

M1+ must implement granular consent, least-privilege Health Connect permissions, encrypted local persistence where appropriate, retention/deletion policy and immutable source provenance before storing data. Release signing keys must never be in the repository.
