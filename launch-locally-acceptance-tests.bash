set -x
set -e

#Load local env
source ~/.osb-cmdb.env

build() {
  ./gradlew ${gradle_proxy_config} clean assemble osb-cmdb:bootJar -x test
#  ./gradlew ${gradle_proxy_config} assemble osb-cmdb:bootJar -x test
}

rename_jar_file_to_be_predicable() {
  #See inspiration from http://tldp.org/LDP/abs/html/globbingref.html
  #IFS="$(printf '\n\t')"   # Remove space.

  for file in ${PWD}/osb-cmdb/build/libs/osb-cmdb-*.jar ; do         # Use ./* ... NEVER bare *

    echo "copying $file into $PWD/osb-cmdb/build/libs/osb-cmdb.jar"

    # $ mv --help
    #   -u, --update                 move only when the SOURCE file is newer
    #                                 than the destination file or when the
    #                                 destination file is missing
    cp "${file}" "$PWD/osb-cmdb/build/libs/osb-cmdb.jar"
  done

  if [ ! -f  "$PWD/osb-cmdb/build/libs/osb-cmdb.jar" ]; then
    echo "did not find expected jar file at $PWD/osb-cmdb/build/libs/osb-cmdb.jar Exiting"
    exit 1
  fi
}


run_tests() {
  # The AT build.gradle explicitly propagates system properties starting with spring to the test environment


  OSB_CMDB_PROPS=""
  EXPOSE_AT_PROPS="true"
  # when set to "true", then the AT properies are exposed. Required for testing osbcmdb dynamic catalog
  if [[ $EXPOSE_AT_PROPS == "true" ]]; then
    # During debug, we once attempted to bypass env variables with JVM properties
    # This was not the root cause, and rather creates divergence with production mechanis using env var
    # with cloudfoundry cf-set-env allow vars separated with dots while bash does not allow it
    OSB_CMDB_PROPS="${OSB_CMDB_PROPS} -Dspring.cloud.appbroker.acceptance-test.cloudfoundry.api-host=${API_HOST}"
    OSB_CMDB_PROPS="${OSB_CMDB_PROPS} -Dspring.cloud.appbroker.acceptance-test.cloudfoundry.api-port=${API_PORT}"
    OSB_CMDB_PROPS="${OSB_CMDB_PROPS} -Dspring.cloud.appbroker.acceptance-test.cloudfoundry.username=${USERNAME}"
    OSB_CMDB_PROPS="${OSB_CMDB_PROPS} -Dspring.cloud.appbroker.acceptance-test.cloudfoundry.password=${PASSWORD}"
    OSB_CMDB_PROPS="${OSB_CMDB_PROPS} -Dspring.cloud.appbroker.acceptance-test.cloudfoundry.client_id=${CLIENT_ID}"
    OSB_CMDB_PROPS="${OSB_CMDB_PROPS} -Dspring.cloud.appbroker.acceptance-test.cloudfoundry.client_secret=${CLIENT_SECRET}"
    OSB_CMDB_PROPS="${OSB_CMDB_PROPS} -Dspring.cloud.appbroker.acceptance-test.cloudfoundry.default-org=${DEFAULT_ORG}"
    OSB_CMDB_PROPS="${OSB_CMDB_PROPS} -Dspring.cloud.appbroker.acceptance-test.cloudfoundry.default-space=${DEFAULT_SPACE}"
    OSB_CMDB_PROPS="${OSB_CMDB_PROPS} -Dspring.cloud.appbroker.acceptance-test.cloudfoundry.skip-ssl-validation=${SKIP_SSL_VALIDATION}"
  fi

  # To check received env in gradle test, use a gradle --debug output which produces the following trace
  # > 10:56:56.188 [DEBUG] [org.gradle.process.internal.DefaultExecHandle] Environment for process 'Gradle Test Executor 1': { [...] }
  export SPRING_CLOUD_APPBROKER_DEPLOYER_CLOUDFOUNDRY_API_HOST="${API_HOST}"
  export SPRING_CLOUD_APPBROKER_DEPLOYER_CLOUDFOUNDRY_API_PORT="${API_PORT}"
  export SPRING_CLOUD_APPBROKER_DEPLOYER_CLOUDFOUNDRY_USERNAME="${USERNAME}"
  export SPRING_CLOUD_APPBROKER_DEPLOYER_CLOUDFOUNDRY_PASSWORD="${PASSWORD}"
  export SPRING_CLOUD_APPBROKER_DEPLOYER_CLOUDFOUNDRY_DEFAULT_ORG="${DEFAULT_ORG}"
  export SPRING_CLOUD_APPBROKER_DEPLOYER_CLOUDFOUNDRY_DEFAULT_SPACE="${DEFAULT_SPACE}"
  export SPRING_CLOUD_APPBROKER_DEPLOYER_CLOUDFOUNDRY_SKIP_SSL_VALIDATION="${SKIP_SSL_VALIDATION}"

  #Original is commented
  export GRADLE_ARGS="spring-cloud-app-broker-acceptance-tests:test -Dspring.security.user.name=${SPRING_SECURITY_USER_NAME} -Dspring.security.user.password=${SPRING_SECURITY_USER_PASSWORD} -Dosbcmdb.admin.user=${OSBCMDB_ADMIN_USER} -Dosbcmdb.admin.password=${OSBCMDB_ADMIN_PASSWORD} -Ddebug=true -Dtests.broker-app-path=$PWD/osb-cmdb/build/libs/osb-cmdb.jar -PacceptanceTests -PincludeTag=cmdb "

  ./gradlew ${gradle_proxy_config} ${OSB_CMDB_PROPS} ${GRADLE_ARGS}
}

build
rename_jar_file_to_be_predicable
run_tests