#!/usr/bin/env bash
#
# Installs the OIE engine jars this plugin builds against into the local
# Maven repository. The public repsy mirror does not yet carry 4.6.0, so for
# a 4.6.0 build we resolve from a local engine checkout instead.
#
# Usage:
#   ENGINE_DIR=/path/to/engine ./scripts/install-engine-jars.sh
#   mvn clean package -Dmirth.version=4.6.0
#
# If ENGINE_DIR is unset, defaults to ../engine relative to this repo.
# Override the installed version with VERSION=x.y.z (default 4.6.0).

set -euo pipefail

VERSION="${VERSION:-4.6.0}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENGINE_DIR="${ENGINE_DIR:-$(cd "$SCRIPT_DIR/../../engine" 2>/dev/null && pwd || true)}"

if [[ -z "${ENGINE_DIR}" || ! -d "${ENGINE_DIR}" ]]; then
    echo "error: engine directory not found." >&2
    echo "  set ENGINE_DIR to point at your OIE engine checkout, e.g.:" >&2
    echo "    ENGINE_DIR=/path/to/engine $0" >&2
    exit 1
fi

# artifact:candidate-path[:fallback-path...] — the four jars the modules
# depend on (see the root pom dependencyManagement).
declare -a JARS=(
    "mirth-server:server/setup/server-lib/mirth-server.jar"
    "mirth-client-core:server/setup/server-lib/mirth-client-core.jar"
    "mirth-client:server/setup/client-lib/mirth-client.jar"
    "donkey-model:donkey/setup/donkey-model.jar:server/setup/server-lib/donkey/donkey-model.jar"
)

resolve_jar() {
    local spec="$1"
    IFS=':' read -ra parts <<< "${spec}"
    for path in "${parts[@]:1}"; do
        if [[ -f "${ENGINE_DIR}/${path}" ]]; then
            echo "${ENGINE_DIR}/${path}"
            return 0
        fi
    done
    return 1
}

for entry in "${JARS[@]}"; do
    if ! resolve_jar "${entry}" >/dev/null; then
        echo "error: could not find a jar for ${entry%%:*} under ${ENGINE_DIR}" >&2
        echo "  build the engine first (ant in donkey/ and server/) so the setup jars exist." >&2
        exit 1
    fi
done

for entry in "${JARS[@]}"; do
    artifact="${entry%%:*}"
    jar_path="$(resolve_jar "${entry}")"
    echo "installing ${artifact}-${VERSION} from ${jar_path}"
    mvn -q install:install-file \
        -Dfile="${jar_path}" \
        -DgroupId=com.mirth.connect \
        -DartifactId="${artifact}" \
        -Dversion="${VERSION}" \
        -Dpackaging=jar
done

echo "done. ${#JARS[@]} jars installed at version ${VERSION}."
