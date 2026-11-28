# Vanguard

## Purpose

Vanguard is a self-hosted Pi-hole DNS resolver deployed on Fly.io. Pi-hole filters DNS queries and forwards allowed queries to a local Unbound recursive resolver. The intended public encrypted-DNS service is DNS-over-TLS (DoT) at `vanguard.cyrilng.com`.

The goal is to keep the DNS resolver and its state on one small Fly machine, with no external recursive DNS provider in the Pi-hole-to-upstream path.

## Request path

```text
DNS client -- DoT/TCP 853 --> Fly.io --> Traefik
                                      --> Pi-hole TCP 53
                                      --> Unbound 127.0.0.1:5335
                                      --> DNS root and authoritative servers
```

- Pi-hole's upstream is set to `127.0.0.1#5335`.
- Unbound listens only on loopback at port 5335 and performs recursive resolution.
- Traefik terminates TLS on TCP port 853. Its TCP router matches `vanguard.cyrilng.com` and requires ALPN `dot`.
- Let's Encrypt certificates use the HTTP-01 challenge: Fly port 80 forwards HTTP to Traefik's internal port 8080.
- Pi-hole's web UI is not routed through Traefik or exposed by the Fly services in `fly.toml`.
- Pi-hole port 53 is for in-container traffic; it is not configured as a public Fly service.

Fly's managed TLS handler is not used for DoT: it supports HTTP ALPN protocols, while this service needs the `dot` ALPN. Keep port 853 as raw TCP to Traefik.

## Runtime and persistent state

- `Dockerfile` installs Pi-hole's supporting services: Unbound, Traefik, and Tailscale.
- The entrypoint starts Tailscale in userspace mode, bootstraps Unbound's root trust anchor, starts Pi-hole, then starts Traefik after Pi-hole accepts TCP connections on port 53.
- The `pihole_data` Fly volume is mounted at `/etc/pihole`. Pi-hole configuration/database, Traefik's ACME store, and Tailscale state are kept there.
- Traefik's ACME file must be mode `600`; the entrypoint reapplies this after Pi-hole starts.
- `TS_AUTHKEY` is optional and should be configured as a Fly secret, not committed. Without it, the container does not run `tailscale up`. Tailscale is not the public DoT path.
- The image uses small Unbound caches and Pi-hole retains seven days of database history. `fly.toml` sets `swap_size_mb = 256`.

## Deployment

Pushing to `main` runs `.github/workflows/deploy.yml`. The workflow ensures an unattached `pihole_data` volume exists in region `sin`, then runs `flyctl deploy --remote-only --config fly.toml`. GitHub Actions needs the `FLY_API_TOKEN` secret.

The configured Fly app is `vanguard` in `sin`. The public DNS name should resolve to the Fly app; check the current A/AAAA or CNAME records rather than assuming Fly addresses are fixed.

## Network constraints

- DoT requires TCP port 853 to reach Traefik.
- IPv6 is the intended public route without a dedicated IPv4. Clients and test machines need working IPv6 connectivity.
- Fly shared IPv4 is not suitable for arbitrary raw TCP service ports such as 853. IPv4 DoT requires a dedicated IPv4 allocation, which may incur a charge.
- Port 80 exists for certificate validation; it does not expose the Pi-hole admin UI.
- Tailscale currently runs in userspace mode. Tailscale Serve can forward TCP, but full DNS on port 53 requires both UDP and TCP. Do not treat TCP-only forwarding as a complete DNS service; first verify TUN support on Fly or choose a UDP-capable relay. The entrypoint logs whether `/dev/net/tun` exists.
- Port 53 is not currently exposed to the tailnet.

## Verification and current status

Test DoT from a host with IPv6 using:

```sh
kdig -6 @vanguard.cyrilng.com -p 853 +tls +tls-ca \
  +tls-host=vanguard.cyrilng.com example.com A
```

A DNS answer with certificate verification passing confirms the TLS connection and DNS query. If the client reports it cannot connect to the IPv6 address, it has not reached TLS; test from an IPv6-capable network. A TCP port check alone does not verify DoT.

Deployment success is not proof that the service is listening. Check Fly deployment/runtime logs for listeners on internal ports 853 and 8080 and for Traefik/ACME errors.

As of 2026-11-28, deployment run 9 completed successfully, but Fly still reported no listeners on 853 or 8080 (only Pi-hole listeners were found). Runtime logs show Tailscale connected, Pi-hole ready on TCP 53, and the entrypoint reaching “Starting Traefik with debug logging”; they contain no Traefik startup/error output. The failure is narrowed to Traefik not binding after launch, but its exact cause is still unknown. The entrypoint now checks both local ports after launch and logs whether Traefik exited or remained alive without binding; this diagnostic change still needs deployment. Do not assume a successful deploy means DoT or tailnet DNS works.

Full DNS over Tailscale is also not configured. Tailscale is in userspace mode, and its Serve TCP forwarder alone cannot provide standard DNS, which needs both UDP and TCP. Verify whether Fly exposes `/dev/net/tun`; if not, a UDP-capable relay or different host is required.
