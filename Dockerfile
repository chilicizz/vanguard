FROM pihole/pihole:latest

# Install Unbound and Tailscale
RUN apk add --no-cache \
    bash \
    unbound \
    tailscale \
    ca-certificates

# Low-memory Unbound configuration (~15 MB RAM footprint)
RUN mkdir -p /etc/unbound/unbound.conf.d && \
    cat <<'EOF' > /etc/unbound/unbound.conf.d/pi-hole.conf
server:
    verbosity: 0
    username: "unbound"
    interface: 127.0.0.1
    port: 5335
    access-control: 127.0.0.0/8 allow
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
RUN unbound-checkconf /etc/unbound/unbound.conf.d/pi-hole.conf

# Entrypoint: start Tailscale (userspace), Unbound, then Pi-hole
RUN cat <<'EOF' > /usr/local/bin/entrypoint-all.sh
#!/bin/bash
set -e

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
unbound -d -c /etc/unbound/unbound.conf.d/pi-hole.conf &
unbound_pid=$!
trap 'if [ -n "${pihole_pid:-}" ]; then kill -TERM "$pihole_pid" 2>/dev/null || true; fi; kill -TERM "$unbound_pid" 2>/dev/null || true' INT TERM
echo "Waiting for Unbound TCP DNS listener on 127.0.0.1:5335"
for ((attempt = 0; attempt < 30; attempt++)); do
    if ( : >/dev/tcp/127.0.0.1/5335 ) 2>/dev/null; then
        break
    fi
    if ! kill -0 "$unbound_pid" 2>/dev/null; then
        if wait "$unbound_pid"; then
            unbound_status=0
        else
            unbound_status=$?
        fi
        echo "Unbound exited before listening on TCP port 5335 (status $unbound_status)" >&2
        exit 1
    fi
    sleep 1
done
if ! ( : >/dev/tcp/127.0.0.1/5335 ) 2>/dev/null; then
    echo "Unbound did not listen on TCP port 5335 in time" >&2
    exit 1
fi
echo "Unbound TCP DNS listener is ready"

# 3. Start Pi-hole and wait for DNS before accepting DoT queries.
/usr/bin/start.sh &
pihole_pid=$!
trap 'kill -TERM "$pihole_pid" 2>/dev/null || true' INT TERM
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

wait -n "$pihole_pid" "$unbound_pid"
EOF

RUN chmod +x /usr/local/bin/entrypoint-all.sh

# Low-RAM Pi-hole v6 settings + upstream pointing to local Unbound
ENV FTLCONF_dns_upstreams="127.0.0.1#5335" \
    FTLCONF_dns_listeningMode="all" \
    FTLCONF_dns_cache_size="0" \
    FTLCONF_database_maxDBdays="7"

EXPOSE 53/tcp 53/udp 80/tcp

ENTRYPOINT ["/usr/local/bin/entrypoint-all.sh"]
