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

# Example properties file for Doppio

FROM adoptopenjdk:15-jre-hotspot

ARG GIT_HASH
ARG VERSION

LABEL maintainer="desu@deszaras.xyz"
LABEL org.label-schema.schema-version="1.0"
LABEL org.label-schema.name="Doppio"
LABEL org.label-schema.description="A Gemini server"
LABEL org.label-schema.vcs-url="https://github.com/bhavanki/doppio"
LABEL org.label-schema.vcs-ref=$GIT_HASH
LABEL org.label-schema.version=$VERSION
LABEL org.label-schema.docker.cmd="docker run --rm -d -p 1965:1965 -e DOPPIO_HOSTNAME=example.com --mount type=bind,src=/mycapsule,dst=/var/gemini doppio"
LABEL org.label-schema.docker.params="DOPPIO_HOSTNAME=The host for the server (required)"

EXPOSE 1965/tcp
EXPOSE 31965/tcp

RUN addgroup --system doppio && adduser --system --shell /bin/false --ingroup doppio doppio

RUN mkdir /opt/doppio
COPY doppio.jar /opt/doppio/doppio.jar
RUN chown -R doppio:doppio /opt/doppio

RUN mkdir /etc/doppio
COPY doppio-docker.yaml /etc/doppio/doppio.yaml
RUN chown -R doppio:doppio /etc/doppio

RUN mkdir /var/gemini
COPY index-docker.gmi /var/gemini/index.gmi
RUN chown -R doppio:doppio /var/gemini
VOLUME /var/gemini

USER doppio
CMD ["java", "-jar", "/opt/doppio/doppio.jar", "/etc/doppio/doppio.yaml"]
