[![Build Status](https://travis-ci.org/OpenOlitor/openolitor-server.svg?branch=master)](https://travis-ci.org/OpenOlitor/openolitor-server)
# openolitor-server
Server Backend der OpenOlitor Administrationsplattform

## Entwicklungs-Setup
https://github.com/OpenOlitor/OpenOlitor/wiki/Doku-Technisch_Server_Ent-Setup

## Dokumentation
Die gesamte Dokumentation befindet sich auf dem OpenOlitor-Projekt-Wiki
https://github.com/OpenOlitor/OpenOlitor/wiki/

Release Notes stehen projektübergreifend zur Verfügung:
https://github.com/OpenOlitor/OpenOlitor/wiki/Release-Notes

## bumpversion.sh
Mittels `./bumpversion.sh` (`./bumpversion.sh -v 1.0.x`) wird die Version in `project/Build.scala` erhöht.
Mit dem Flag -c/--commit wird ein git commit und ein git tag mit entsprechender Nachricht gemacht.
Anderseits werden die nötigen git Befehle ausgegeben.

