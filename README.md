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

Fly manages the certificate for `vanguard.cyrilng.com`; attach it once with `fly certs add vanguard.cyrilng.com` if it is not already attached.

## Runtime and persistent state

- `Dockerfile` installs Pi-hole's supporting services: Unbound and Tailscale.
- The entrypoint starts Tailscale in userspace mode, bootstraps Unbound's root trust anchor, waits for Unbound on TCP 5335, then starts Pi-hole. Unbound runs in the foreground so startup errors appear in the logs.
- The `pihole_data` Fly volume is mounted at `/etc/pihole`. Pi-hole configuration/database and Tailscale state are kept there.
- `TS_AUTHKEY` is optional and should be configured as a Fly secret, not committed. Without it, the container does not run `tailscale up`. Tailscale is not the public DoT path.
- The image uses small Unbound caches and Pi-hole retains seven days of database history. `fly.toml` sets `swap_size_mb = 256`.

## Deployment

Pushing to `main` runs `.github/workflows/deploy.yml`. The workflow ensures an unattached `pihole_data` volume exists in region `sin`, then runs `flyctl deploy --remote-only --config fly.toml`. GitHub Actions needs the `FLY_API_TOKEN` secret.

The configured Fly app is `vanguard` in `sin`. The public DNS name should resolve to the Fly app; check the current A/AAAA or CNAME records rather than assuming Fly addresses are fixed.

## Network constraints

- DoT requires TCP port 853 to reach Fly's TLS handler; the handler forwards plaintext DNS over TCP to Pi-hole on port 53.
- IPv6 is the intended public route without a dedicated IPv4. Clients and test machines need working IPv6 connectivity.
- Fly's TLS handler can support a shared IPv4 on TLS service ports such as 853; verify current Fly IP allocation and DNS before relying on IPv4.
- The Pi-hole admin UI is not exposed.
- Tailscale currently runs in userspace mode. Tailscale Serve can forward TCP, but full DNS on port 53 requires both UDP and TCP. Do not treat TCP-only forwarding as a complete DNS service; first verify TUN support on Fly or choose a UDP-capable relay. The entrypoint logs whether `/dev/net/tun` exists.
- Port 53 is not currently exposed to the tailnet.

## Verification and current status

Test DoT from a host with IPv6 using:

```sh
kdig -6 @vanguard.cyrilng.com -p 853 +tls +tls-ca \
  +tls-host=vanguard.cyrilng.com example.com A
```

A DNS answer with certificate verification passing confirms the TLS connection and DNS query. If the client reports it cannot connect to the IPv6 address, it has not reached TLS; test from an IPv6-capable network. A TCP port check alone does not verify DoT.

Deployment success is not proof that the service is listening. Check Fly deployment/runtime logs for a listener on internal port 53 and for certificate status.

This configuration is a trial of Fly TLS termination without a `dot` ALPN override. The latest test through `vanguard.fly.dev:853` completed TLS without ALPN, but the DNS query was refused; logs also showed FTL could not connect to Unbound on 127.0.0.1:5335. The entrypoint now checks and supervises Unbound before starting Pi-hole; this change still needs deployment.

Full DNS over Tailscale is also not configured. The latest logs show `/dev/net/tun` exists, but Tailscale still runs in userspace mode; its Serve TCP forwarder alone cannot provide standard DNS, which needs both UDP and TCP. Verify that kernel TUN networking is permitted before switching modes.
