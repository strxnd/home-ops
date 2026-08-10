# Build context is the repository root. This stage consumes LifeStealZ only as
# a compile-only API from its official GitHub release; it is not bundled.
FROM docker.io/library/gradle:9.3.0-jdk25@sha256:9523988cbc939905df31e4223513ce0efa05154b6111fe66a6af8d2fe2c044c9 AS smprules-build
WORKDIR /src
COPY smprules/build.gradle.kts smprules/settings.gradle.kts ./
COPY smprules/src ./src
RUN mkdir -p libs && curl --fail --location --output libs/lifestealz-2.21.1.jar https://github.com/ZetaPlugins/LifeStealZ/releases/download/2.21.1/lifestealz-2.21.1.jar && gradle --no-daemon build

FROM docker.io/itzg/minecraft-server:2026.8.0-java25@sha256:9246d987f7d0f26ab1359868404f65df59fecf5a6597628231d23f909ee806bc
LABEL org.opencontainers.image.source="https://github.com/strxnd/home-ops"
COPY --from=smprules-build --chown=1000:1000 /src/build/libs/SMPRules-1.0.0.jar /plugins/SMPRules-1.0.0.jar
COPY --chown=1000:1000 kubernetes/apps/games/glitg-smp/config/ /config/
