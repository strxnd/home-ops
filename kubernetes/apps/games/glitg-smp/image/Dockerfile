# Build context is the repository root.
FROM docker.io/library/gradle:9.3.0-jdk25@sha256:9523988cbc939905df31e4223513ce0efa05154b6111fe66a6af8d2fe2c044c9 AS smprules-build
WORKDIR /src
COPY smprules/build.gradle.kts smprules/settings.gradle.kts ./
COPY smprules/src ./src
RUN mkdir -p libs \
  && curl --fail --location --output libs/lifestealz-2.21.1.jar https://github.com/ZetaPlugins/LifeStealZ/releases/download/2.21.1/lifestealz-2.21.1.jar \
  && echo 'c4d466f579ec91ef3dd51c01fbdcab3aa8cf98009489c229abe4c823a4dbaea1  libs/lifestealz-2.21.1.jar' | sha256sum --check \
  && gradle --no-daemon build

FROM docker.io/itzg/minecraft-server:2026.8.0-java25@sha256:9246d987f7d0f26ab1359868404f65df59fecf5a6597628231d23f909ee806bc
COPY --from=smprules-build --chown=1000:1000 /src/build/libs/SMPRules-2.0.0.jar /plugins/SMPRules-2.0.0.jar
COPY --chown=1000:1000 kubernetes/apps/games/glitg-smp/config/ /config/
