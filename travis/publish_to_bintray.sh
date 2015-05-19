#!/bin/bash
# Copyright 2015 Yahoo! Inc.
#
# Licensed under the Apache License, Version 2.0 (the License);
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an AS IS BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# TODO: Once https://github.com/travis-ci/dpl/pull/259 is merged,
#       remove this script and use the provider.

# Simple script to use from Travis-CI that will push out a jar to
# bintray maven repository.  This script should be ran from the travis
# machine, and specified to be used in the yml build configuration.
set -e

echo ${TRAVIS_TAG} | grep -q "sshd_proxy-";
IS_NOT_RELEASE=$?

if ((${IS_NOT_RELEASE} == 1)); then
    echo "Not a tagged release version."
    echo "To release this tagged artifact into bintray, the git tag must be of"
    echo "the format: sshd_proxy-*.  Example:  sshd_proxy-0.0.2, sshd_proxy-1.2.3"
    echo "etc."
    exit 0
fi

echo "Publishing to bintray at https://bintray.com/yahoo"

CURRENT_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -Ev '(^\[|Download\w+:)')
echo "Releasing version:  ${CURRENT_VERSION}"

ARTIFACTS=( sshd_proxy-${CURRENT_VERSION}.jar sshd_proxy-${CURRENT_VERSION}-site.jar )

# Upload and Publish each artifact into bintray
# https://bintray.com/yahoo/maven/artifactory_ssh_proxy/view
for artifact in "${ARTIFACTS[@]}"
do
    echo "Uploading $artifact..."
    UPLOAD_RESPONSE=curl -s -o /dev/null -w "%{http_code}" -T target/$artifact -u ${BINTRAY_USER}:${BINTRAY_API_KEY} https://api.bintray.com/content/yahoo/maven/artifactory_ssh_proxy/${CURRENT_VERSION}/${CURRENT_VERSION}/${ARTIFACT}
    if (( $UPLOAD_RESPONSE >= 200 && $UPLOAD_RESPONSE < 227 )); then
        echo "Success Uploading $artifact."
        echo
    else
        echo "Error during upload: HTTP $UPLOAD_RESPONSE"
        echo
        exit 1
    fi
    echo
done

echo "Publishing Version ${CURRENT_VERSION} of artifactory_ssh_proxy"
PUBLISH_RESPONSE=curl -s -o /dev/null -w "%{http_code}" -X POST -u ${BINTRAY_USER}:${BINTRAY_API_KEY} https://api.bintray.com/content/yahoo/maven/artifactory_ssh_proxy/${CURRENT_VERSION}/publish
if (( $PUBLISH_RESPONSE >= 200 && $PUBLISH_RESPONSE < 227 )); then
    echo "Success publishing."
    echo "https://bintray.com/yahoo/maven/artifactory_ssh_proxy/${CURRENT_VERSION}/view"
    echo
else
    echo "Error during publishing ${CURRENT_VERSION} of artifactory_ssh_proxy."
    echo "HTTP ${PUBLISH_RESPONSE}"
    exit 1
fi
echo
