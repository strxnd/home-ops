FROM docker.io/itzg/minecraft-server:2026.8.0-java21@sha256:b76b9298a2a60d5cf9d223e009cd0b8ad620c2080abd83f9a1fa5084fa87f9ab
COPY kubernetes/apps/games/glitg-smp/plugins.lock /tmp/plugins.lock
RUN mkdir -p /plugins \
  && while read -r checksum filename url; do \
       case "$checksum" in \#*|'') continue ;; esac; \
       curl --fail --location --retry 3 --output "/plugins/$filename" "$url"; \
       echo "$checksum  /plugins/$filename" | sha256sum --check; \
     done < /tmp/plugins.lock \
  && chown -R 1000:1000 /plugins \
  && rm /tmp/plugins.lock
COPY --chown=1000:1000 kubernetes/apps/games/glitg-smp/config/ /config/
COPY kubernetes/apps/games/glitg-smp/image/start.sh /image/scripts/glitg-start
RUN chmod 0755 /image/scripts/glitg-start
ENTRYPOINT ["/image/scripts/glitg-start"]
