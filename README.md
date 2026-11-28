# Vanguard

## Purpose

Vanguard is a self-hosted Pi-hole DNS resolver deployed on Fly.io. Pi-hole filters DNS queries and forwards allowed queries to a local Unbound recursive resolver. The intended public encrypted-DNS service is DNS-over-TLS (DoT) at `vanguard.cyrilng.com`.

The goal is to keep the DNS resolver and its state on one small Fly machine, with no external recursive DNS provider in the Pi-hole-to-upstream path.

## Request path

```text
DNS client -- DoT/TCP 853 --> Fly TLS handler --> Pi-hole TCP 53
                                               --> Unbound 127.0.0.1:5335
                                               --> DNS root and authoritative servers
```

- Pi-hole's upstream is set to `127.0.0.1#5335`.
- Unbound listens only on loopback at port 5335 and performs recursive resolution.
- Fly's TLS handler terminates TLS on TCP port 853 and forwards the decrypted DNS-over-TCP stream to Pi-hole on port 53.
- No ALPN override is configured for this trial. This tests clients that do not require ALPN `dot`; stricter clients may reject the handshake.
- Pi-hole's web UI is not exposed by the Fly services in `fly.toml`.
- Pi-hole port 53 is for in-container traffic; it is not configured as a public Fly service.

## Custom hostname certificate

The default `vanguard.fly.dev` certificate is managed by Fly. To serve `vanguard.cyrilng.com` on the same app:

1. Check the app's current IPs with `fly ips list --app vanguard` and make sure the hostname's DNS records point to this app. At last check, the custom hostname had an AAAA record but no A record; re-check current DNS before changing it.
2. Attach a separate Fly-managed certificate with `fly certs add vanguard.cyrilng.com --app vanguard`.
3. Follow the DNS instructions from Fly, then check issuance with `fly certs check vanguard.cyrilng.com --app vanguard` until the certificate is ready.
4. Verify TLS using `vanguard.cyrilng.com` as SNI and confirm the served certificate covers that name.

This only addresses the custom hostname's certificate. The current Fly TLS handler test completed without ALPN; clients that require ALPN `dot` still need separate testing/configuration.

## Runtime and persistent state

- `Dockerfile` installs Pi-hole's supporting services: Unbound and Tailscale.
- The entrypoint starts Tailscale in userspace mode only when an auth key or saved node state exists, bootstraps and validates Unbound's root trust anchor/config, waits for Unbound on TCP 5335, then starts Pi-hole. It stops all started services during shutdown.
- The `pihole_data` Fly volume is mounted at `/etc/pihole`. Pi-hole configuration/database and Tailscale state are kept there.
- `TS_AUTHKEY` is optional and should be configured as a Fly secret, not committed. Without it, the container does not run `tailscale up`. Tailscale is not the public DoT path.
- Pi-hole query logging is disabled, privacy level 3 is enabled, and `maxDBdays = 0` disables FTL's long-term query database. `TAIL_FTL_LOG = 0` prevents the container image from tailing FTL's log to container output. Fly may still retain container startup/error output under its own log-retention policy.
- These settings stop future query-history storage; they do not erase query data already present on the persistent volume.
- The image uses small Unbound caches. `fly.toml` sets `swap_size_mb = 256`.

## Deployment

Pushing to `main` runs `.github/workflows/deploy.yml`. The workflow creates `pihole_data` in region `sin` only when no matching volume exists; it refuses to deploy if multiple matching volumes make the persistent state ambiguous. It then runs `flyctl deploy --remote-only --config fly.toml`. GitHub Actions needs the `FLY_API_TOKEN` secret.

The configured Fly app is `vanguard` in `sin`. The public DNS name should resolve to the Fly app; check the current A/AAAA or CNAME records rather than assuming Fly addresses are fixed.

## Network constraints

- DoT requires TCP port 853 to reach Fly's TLS handler; the handler forwards plaintext DNS over TCP to Pi-hole on port 53.
- IPv6 is the intended public route without a dedicated IPv4. Clients and test machines need working IPv6 connectivity.
- Fly's TLS handler can support a shared IPv4 on TLS service ports such as 853; verify current Fly IP allocation and DNS before relying on IPv4.
- The Pi-hole admin UI is not exposed.
- Tailscale currently runs in userspace mode. Full DNS on port 53 requires both UDP and TCP and is not configured; kernel-mode networking has not been validated.
- Port 53 is not currently exposed to the tailnet.

## Verification and current status

Check the custom hostname certificate from a host with network access using:

```sh
openssl s_client -connect vanguard.cyrilng.com:853 \
  -servername vanguard.cyrilng.com -verify_return_error </dev/null
```

This verifies the certificate and TLS handshake without offering ALPN. Then test a DNS query using a client known not to require ALPN `dot`; a client that requires `dot` will fail with the current Fly TLS handler setup. A TCP port check alone does not verify DoT.

Deployment success is not proof that the service is listening. Check Fly deployment/runtime logs for a listener on internal port 53 and for certificate status.

This configuration is a trial of Fly TLS termination without a `dot` ALPN override. A live IPv4 test through `vanguard.fly.dev:853` completed TLS without ALPN and returned a DNS answer. Offering ALPN `dot` failed the TLS handshake, and the custom hostname did not present a usable certificate at last test. The privacy settings in the image still need deployment.

Full DNS over Tailscale is also not configured. Tailscale still runs in userspace mode; its Serve TCP forwarder alone cannot provide standard DNS, which needs both UDP and TCP.
