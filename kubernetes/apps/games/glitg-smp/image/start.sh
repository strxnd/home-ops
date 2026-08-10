#!/bin/sh
set -eu

# The persistent volume can retain prior custom-plugin jars. Keep exactly the
# image's current SMPRules jar so Paper never loads two plugins with one name.
rm -f /data/plugins/SMPRules-*.jar
exec /image/scripts/start "$@"
