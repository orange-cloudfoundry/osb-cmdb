#!/usr/bin/env bash
#
# adapted from https://gist.githubusercontent.com/stefanbuck/ce788fee19ab6eb0b4447a85fc99f447/raw/18fd80fabd60be52d513fe2a439773c5fff7df79/upload-github-release-asset.sh
#
# This script accepts the following parameters:
#
# * owner
# * repo
# * tag
# * github_api_token
#
# Script to create a release with asset using the GitHub API v3.
#
# Example:
#
# create_github_release.sh github_api_token=TOKEN owner=orange-cloudfoundry repo=osb-cmdb-spike tag=v0.1.0
#

#--- Colors and styles
export RED='\033[1;31m'
export YELLOW='\033[1;33m'
export STD='\033[0m'

# Check dependencies.
set -e
xargs=$(which gxargs || which xargs)

# Validate settings.
[ "$TRACE" ] && set -x

CONFIG=$@

for line in $CONFIG; do
  eval "$line"
done

# Define variables.
GITHUB_API="https://api.github.com"
GITHUB_REPO="$GITHUB_API/repos/$owner/$repo"
GITHUB_TAGS="$GITHUB_REPO/releases/tags/$tag"
AUTH_HEADER="Authorization: token $github_api_token"
TAG_NAME=$tag

if [[ "$tag" == 'LATEST' ]]; then
  GITHUB_TAGS="$GITHUB_REPO/releases/latest"
fi

# Validate token.
curl -o /dev/null -sH "$AUTH_HEADER" $GITHUB_REPO || { echo "Error: Invalid repo, token or network issue!";  exit 1; }

# create release
printf "%bCreating GitHub release for tag $tag ...%b\n" "${YELLOW}" "${STD}"
GITHUB_DATA='{"tag_name":"'$tag'","draft": false,"prerelease": true}'
GITHUB_RELEASE="$GITHUB_API/repos/orange-cloudfoundry/osb-cmdb-spike/releases"
echo "Github data: $GITHUB_DATA"
sleep 10
curl -X POST --silent --write-out '%{http_code}\n' --data "$GITHUB_DATA" -H "$AUTH_HEADER" $GITHUB_RELEASE
#http_status=$(curl -X POST --silent --write-out '%{http_code}\n' --data "$GITHUB_DATA" -H "$AUTH_HEADER" $GITHUB_RELEASE)
#if [ "201" != "$http_status" ] ; then
#	printf "\n%bERROR: Cannot create release for tag $tag%b\n\n" "${RED}" "${STD}"
#	exit 1
#fi
