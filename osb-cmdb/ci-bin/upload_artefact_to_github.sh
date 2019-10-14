#!/usr/bin/env bash
#
# adapted from https://gist.githubusercontent.com/stefanbuck/ce788fee19ab6eb0b4447a85fc99f447/raw/18fd80fabd60be52d513fe2a439773c5fff7df79/upload-github-release-asset.sh
#
# This script accepts the following parameters:
#
# * owner
# * repo
# * tag
# * filename
# * github_api_token
#
# Script to create a release with asset using the GitHub API v3.
#
# Example:
#
# upload_artefact_to_github.sh github_api_token=TOKEN owner=orange-cloudfoundry repo=osb-cmdb-spike tag=v0.1.0 filename=./cf-ops-automation-terraform-broker-0.5.0-SNAPSHOT.jar
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

# Read release asset tags.
response=$(curl -sH "$AUTH_HEADER" $GITHUB_TAGS)

# Get ID of the asset based on given filename.
eval $(echo "$response" | grep -m 1 "id.:" | grep -w id | tr : = | tr -cd '[[:alnum:]]=')
[ "$id" ] || { echo "Error: Failed to get release id for tag: $tag"; echo "$response" | awk 'length($0)<100' >&2; exit 1; }

# Upload asset
printf "%bUploading artefact to GitHub ...%b\n" "${YELLOW}" "${STD}"
GITHUB_ASSET="https://uploads.github.com/repos/$owner/$repo/releases/$id/assets?name=$(basename $filename)"
curl --silent --write-out '%{http_code}\n' --data-binary @"$filename" -H "$AUTH_HEADER" -H "Content-Type: application/zip" -H "Accept: application/vnd.github.manifold-preview" $GITHUB_ASSET
#http_status=$(curl --data-binary @"$filename" -H "$AUTH_HEADER" -H "Content-Type: application/zip" -H "Accept: application/vnd.github.manifold-preview" $GITHUB_ASSET)
#if [ "201" != "$http_status" ] ; then
#	printf "\n%bERROR: Cannot create asset for release $tag%b\n\n" "${RED}" "${STD}"
#	exit 1
#fi
