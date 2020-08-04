* [x] inspect the routing table

```bash
#log into gorouter
head /var/vcap/jobs/gorouter/config/gorouter.yml

curl -u router-status:redacted-password http://127.0.0.1:8080/routes | jq
```

* [x] troubleshoot failed websocket shield requests with route services
   * gorouter does not support WS for route bound to route services, see https://github.com/cloudfoundry/gorouter/commit/1bf9a13a98ef2230760b572186d52acc0fad9bad and https://github.com/cloudfoundry/docs-cf-admin/pull/183
   * this is clearly visible in firefox version 70+ in webdevelopper (previous version do not make this clear)
   * shield v9 is a UI rewrite using vue.js https://shieldproject.io/community/call/#20200528
