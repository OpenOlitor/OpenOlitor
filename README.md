[![Build Status](https://travis-ci.org/OpenOlitor/openolitor-server.svg?branch=master)](https://travis-ci.org/OpenOlitor/openolitor-server)
# openolitor-server
Server Backend der OpenOlitor Administrationsplattform

## Entwicklungs-Setup
Die Dokumentation steht im Wiki zur Verfügung:
https://github.com/OpenOlitor/openolitor-server/wiki/Entwicklungs-Setup

## REST Schnittstellen
https://github.com/OpenOlitor/openolitor-server/wiki/REST-Schnittstellen

## bumpversion.sh
Mittels `./bumpversion.sh` (`./bumpversion.sh -v 1.0.x`) wird die Version in `project/Build.scala` erhöht.
Mit dem Flag -c/--commit wird ein git commit und ein git tag mit entsprechender Nachricht gemacht.
Anderseits werden die nötigen git Befehle ausgegeben.

