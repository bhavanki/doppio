#!/usr/bin/env bash
#
# Copyright (C) 2021 Bill Havanki
#
# This file is part of Doppio.
#
# Doppio is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

# Run this script to build a Docker image for Doppio. Build Doppio first.

set -e -x

DIR=$(dirname "$0")
cp "${DIR}"/../target/doppio-*.jar ./doppio.jar

GIT_HASH=$(git rev-parse HEAD)
VERSION=$(mvn -f "${DIR}/../pom.xml" help:evaluate -Dexpression=project.version -q -DforceStdout)

docker build . \
  --build-arg "GIT_HASH=${GIT_HASH}" \
  --build-arg "VERSION=${VERSION}" \
  -t "doppio:${VERSION}"
