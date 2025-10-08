* [ ] Generate new gradle build from spring initializer
  * [x] fix build for compilation
  * [x] fix build for unit tests
  * [ ] compile acceptance tests
* [ ] use openrewrite to perform sequential stack migration ?
* [x] diff jar with latest
* [ ] set up github action
* [ ] publish as github release
* [ ] test E2E

### Check tests pass

Fix missing display of unit tests report in gradle output

### Diff jars with 1.8.0

```shell
cd ./osb-cmdb/build/libs/
curl -LO https://github.com/orange-cloudfoundry/osb-cmdb/releases/download/v1.8.0%2Btestcircle/osb-cmdb-1.8.0.jar
```

compare using intellij jar diff: mostly OK
* some small (unreleased) bumps
* missing git.properties file