#!/bin/bash

 #                                                                           #
#    ____                   ____  ___ __                                      #
#   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          #
#  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             #
# / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal #
# \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   #
#     /_/                                                                     #
#                                                                             #
# This program is free software: you can redistribute it and/or modify it     #
# under the terms of the GNU General Public License as published by           #
# the Free Software Foundation, either version 3 of the License,              #
# or (at your option) any later version.                                      #
#                                                                             #
# This program is distributed in the hope that it will be useful, but         #
# WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  #
# or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for #
# more details.                                                               #
#                                                                             #
# You should have received a copy of the GNU General Public License along     #
# with this program. If not, see http://www.gnu.org/licenses/                 #
#                                                                             #
 #                                                                           #

while [[ $# -gt 0 ]]
do
  key="$1"

  case $key in
    -v|--version)
      VERSION="$2"
      shift
      ;;
    -c|--commit)
      COMMIT=true
      shift
      ;;
    *)

  ;;

esac
shift
done

VERSION_REGEX="version.*?\K(\d+)\.(\d+)\.(\d+)(-SNAPSHOT)?"
MANIFEST_REGEX="openolitor-server-\K(\d+).(\d+).(\d+)(-SNAPSHOT)?"

BUILD_FILE='project/Build.scala'

CURRENT_VERSION="$(grep -Po $VERSION_REGEX $BUILD_FILE)"
echo "Current version is: "$CURRENT_VERSION

IFS='.-' read V1 V2 V3 SNAPSHOT <<< $CURRENT_VERSION

if [ -n "$SNAPSHOT" ]
then
  SNAPSHOT="-$SNAPSHOT"
fi

NEXT_VERSION="$V1.$V2."$(($V3 + 1))$SNAPSHOT

VERSION=${VERSION:-$NEXT_VERSION}
COMMIT=${COMMIT:-false}

shopt -s nullglob
MANIFEST_FILES=(manifest*.yml);
shopt -u nullglob

for MANIFEST_FILE in "${MANIFEST_FILES[@]}"
do
  OUT=$(cat $MANIFEST_FILE | perl -pe 's/'$MANIFEST_REGEX'/'$VERSION'/g')

  echo "$OUT" > $MANIFEST_FILE
done

BUILD_OUT=$(cat $BUILD_FILE | perl -pe 's/'$VERSION_REGEX'/'$VERSION'/g')

echo "$BUILD_OUT" > $BUILD_FILE

echo "Updated the version to: $VERSION"

MESSAGE="Bumped version to $VERSION"

echo $MESSAGE

if [[ $COMMIT == true ]]
then
  ( git commit -am "$MESSAGE" && git tag -a $VERSION -m "$MESSAGE" )
  echo "You may now do 'git push && git push origin $VERSION'"
else
  echo "The changes have been made to $BUILD_FILE and ${MANIFEST_FILES[@]}"
  echo "Commit your changes 'git commit -a -m \"$MESSAGE\" && git tag -a $VERSION -m \"$MESSAGE\"'"
  echo "After that use 'git push && git push origin $VERSION' to push everything to origin"
fi


