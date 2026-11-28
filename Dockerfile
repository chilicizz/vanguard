FROM pihole/pihole:latest

# Install Unbound and Tailscale
RUN apk add --no-cache \
    bash \
    unbound \
    traefik \
    tailscale \
    ca-certificates

# Low-memory Unbound configuration (~15 MB RAM footprint)
RUN mkdir -p /etc/unbound/unbound.conf.d && \
    cat <<'EOF' > /etc/unbound/unbound.conf.d/pi-hole.conf
server:
    verbosity: 0
    interface: 127.0.0.1
    port: 5335
    do-ip4: yes
    do-udp: yes
    do-tcp: yes
    do-ip6: no
    prefer-ip6: no

    auto-trust-anchor-file: "/var/lib/unbound/root.key"

    # Tight memory limits for 256MB VM
    num-threads: 1
    msg-cache-slabs: 1
    rrset-cache-slabs: 1
    infra-cache-slabs: 1
    key-cache-slabs: 1
    rrset-cache-size: 4m
    msg-cache-size: 2m
    key-cache-size: 1m
    neg-cache-size: 1m
    infra-host-ttl: 60
    so-rcvbuf: 0

    # Privacy & hardening
    harden-glue: yes
    harden-dnssec-stripped: yes
    use-caps-for-id: no
    edns-buffer-size: 1232
    prefetch: yes

    private-address: 192.168.0.0/16
    private-address: 169.254.0.0/16
    private-address: 172.16.0.0/12
    private-address: 10.0.0.0/8
    private-address: fd00::/8
    private-address: fe80::/10
EOF

# Traefik terminates DoT and obtains a certificate through HTTP-01.
RUN mkdir -p /etc/traefik && \
    cat <<'EOF' > /etc/traefik/traefik.yml
entryPoints:
  web:
    address: ":8080"
  dot:
    address: ":853"
providers:
  file:
    filename: /etc/traefik/dynamic.yml
    watch: false
certificatesResolvers:
  letsencrypt:
    acme:
      storage: /etc/pihole/traefik/acme.json
      httpChallenge:
        entryPoint: web
EOF

RUN cat <<'EOF' > /etc/traefik/dynamic.yml
tcp:
  routers:
    dot:
      entryPoints:
        - dot
      rule: "HostSNI(`vanguard.cyrilng.com`)"
      service: pihole-dns
      tls:
        certResolver: letsencrypt
        options: dot
  services:
    pihole-dns:
      loadBalancer:
        servers:
          - address: "127.0.0.1:53"
tls:
  options:
    dot:
      alpnProtocols:
        - dot
EOF

# Entrypoint: start Tailscale (userspace), Unbound, then Pi-hole
RUN cat <<'EOF' > /usr/local/bin/entrypoint-all.sh
#!/bin/bash
set -e

# Start DoT early so Fly sees its listeners during startup checks.
install -d -m 700 /etc/pihole/traefik
if [ -f /etc/pihole/acme.json ] && [ ! -e /etc/pihole/traefik/acme.json ]; then
    mv /etc/pihole/acme.json /etc/pihole/traefik/acme.json
fi
touch /etc/pihole/traefik/acme.json
chmod 600 /etc/pihole/traefik/acme.json
echo "Starting Traefik with debug logging"
traefik --log.level=DEBUG --configFile=/etc/traefik/traefik.yml &
traefik_pid=$!
trap 'if [ -n "${pihole_pid:-}" ]; then kill -TERM "$pihole_pid" 2>/dev/null || true; fi; kill -TERM "$traefik_pid" 2>/dev/null || true' INT TERM

# 1. Start Tailscale in userspace networking mode (state stored on persistent /etc/pihole volume)
mkdir -p /etc/pihole/tailscale /var/run/tailscale
if [ -c /dev/net/tun ]; then
    echo "Tailscale TUN device is available"
else
    echo "Tailscale TUN device is unavailable; using userspace networking"
fi
tailscaled \
    --tun=userspace-networking \
    --state=/etc/pihole/tailscale/tailscaled.state \
    --socket=/var/run/tailscale/tailscaled.sock &

# Wait briefly for tailscaled socket, then bring node up if TS_AUTHKEY is provided
sleep 2
if [ -n "$TS_AUTHKEY" ]; then
    tailscale up \
        --authkey="$TS_AUTHKEY" \
        --hostname="${TS_HOSTNAME:-pihole-fly}" \
        --accept-dns=false
fi

# 2. Bootstrap DNSSEC root key and start Unbound
mkdir -p /var/lib/unbound
chown -R unbound:unbound /var/lib/unbound
su -s /bin/sh unbound -c "unbound-anchor -a /var/lib/unbound/root.key" || true
unbound -c /etc/unbound/unbound.conf

# 3. Start Pi-hole and wait for DNS before accepting DoT queries.
/usr/bin/start.sh &
pihole_pid=$!
echo "Waiting for Pi-hole TCP DNS listener on 127.0.0.1:53"
for ((attempt = 0; attempt < 45; attempt++)); do
    if ( : >/dev/tcp/127.0.0.1/53 ) 2>/dev/null; then
        break
    fi
    sleep 1
done
if ! ( : >/dev/tcp/127.0.0.1/53 ) 2>/dev/null; then
    echo "Pi-hole did not listen on TCP port 53 in time" >&2
    exit 1
fi

traefik_ready=0
for ((attempt = 0; attempt < 15; attempt++)); do
    if ( : >/dev/tcp/127.0.0.1/853 ) 2>/dev/null && ( : >/dev/tcp/127.0.0.1/8080 ) 2>/dev/null; then
        traefik_ready=1
        break
    fi
    if ! kill -0 "$traefik_pid" 2>/dev/null; then
        if wait "$traefik_pid"; then
            traefik_status=0
        else
            traefik_status=$?
        fi
        echo "Traefik exited before opening ports 853 and 8080 (status $traefik_status)" >&2
        exit 1
    fi
    sleep 1
done
if [ "$traefik_ready" -eq 1 ]; then
    echo "Traefik is listening on TCP ports 853 and 8080"
else
    echo "Traefik process $traefik_pid is still running but has not opened ports 853 and 8080" >&2
fi
wait -n "$pihole_pid" "$traefik_pid"
EOF

RUN chmod +x /usr/local/bin/entrypoint-all.sh

# Low-RAM Pi-hole v6 settings + upstream pointing to local Unbound
ENV FTLCONF_dns_upstreams="127.0.0.1#5335" \
    FTLCONF_dns_listeningMode="all" \
    FTLCONF_dns_cache_size="0" \
    FTLCONF_database_maxDBdays="7"

EXPOSE 53/tcp 53/udp 80/tcp 853/tcp 8080/tcp

ENTRYPOINT ["/usr/local/bin/entrypoint-all.sh"]
