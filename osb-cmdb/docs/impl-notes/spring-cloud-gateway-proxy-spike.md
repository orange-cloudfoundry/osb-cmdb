      
```
2020-07-29 14:35:35.309 DEBUG 29598 --- [or-http-epoll-3] reactor.netty.http.client.HttpClient     : [id: 0x13b432c0, L:/10.0.2.15:37142 - R:shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/10.228.194.8:443] WRITE: 582B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 47 45 54 20 2f 76 32 2f 65 76 65 6e 74 73 20 48 |GET /v2/events H|
|00000010| 54 54 50 2f 31 2e 31 0d 0a 55 73 65 72 2d 41 67 |TTP/1.1..User-Ag|
|00000020| 65 6e 74 3a 20 4d 6f 7a 69 6c 6c 61 2f 35 2e 30 |ent: Mozilla/5.0|
|00000030| 20 28 58 31 31 3b 20 55 62 75 6e 74 75 3b 20 4c | (X11; Ubuntu; L|
|00000040| 69 6e 75 78 20 78 38 36 5f 36 34 3b 20 72 76 3a |inux x86_64; rv:|
|00000050| 37 38 2e 30 29 20 47 65 63 6b 6f 2f 32 30 31 30 |78.0) Gecko/2010|
|00000060| 30 31 30 31 20 46 69 72 65 66 6f 78 2f 37 38 2e |0101 Firefox/78.|
|00000070| 30 0d 0a 41 63 63 65 70 74 3a 20 2a 2f 2a 0d 0a |0..Accept: */*..|
|00000080| 41 63 63 65 70 74 2d 4c 61 6e 67 75 61 67 65 3a |Accept-Language:|
|00000090| 20 65 6e 2d 55 53 2c 65 6e 3b 71 3d 30 2e 35 0d | en-US,en;q=0.5.|
|000000a0| 0a 41 63 63 65 70 74 2d 45 6e 63 6f 64 69 6e 67 |.Accept-Encoding|
|000000b0| 3a 20 67 7a 69 70 2c 20 64 65 66 6c 61 74 65 0d |: gzip, deflate.|
|000000c0| 0a 4f 72 69 67 69 6e 3a 20 68 74 74 70 3a 2f 2f |.Origin: http://|
|000000d0| 6c 6f 63 61 6c 68 6f 73 74 3a 38 30 38 30 0d 0a |localhost:8080..|
|000000e0| 50 72 61 67 6d 61 3a 20 6e 6f 2d 63 61 63 68 65 |Pragma: no-cache|
|000000f0| 0d 0a 43 61 63 68 65 2d 43 6f 6e 74 72 6f 6c 3a |..Cache-Control:|
|00000100| 20 6e 6f 2d 63 61 63 68 65 0d 0a 46 6f 72 77 61 | no-cache..Forwa|
|00000110| 72 64 65 64 3a 20 70 72 6f 74 6f 3d 68 74 74 70 |rded: proto=http|
|00000120| 3b 68 6f 73 74 3d 22 6c 6f 63 61 6c 68 6f 73 74 |;host="localhost|
|00000130| 3a 38 30 38 30 22 3b 66 6f 72 3d 22 31 32 37 2e |:8080";for="127.|
|00000140| 30 2e 30 2e 31 3a 35 35 38 39 36 22 0d 0a 58 2d |0.0.1:55896"..X-|
|00000150| 46 6f 72 77 61 72 64 65 64 2d 46 6f 72 3a 20 31 |Forwarded-For: 1|
|00000160| 32 37 2e 30 2e 30 2e 31 0d 0a 58 2d 46 6f 72 77 |27.0.0.1..X-Forw|
|00000170| 61 72 64 65 64 2d 50 72 6f 74 6f 3a 20 68 74 74 |arded-Proto: htt|
|00000180| 70 0d 0a 58 2d 46 6f 72 77 61 72 64 65 64 2d 50 |p..X-Forwarded-P|
|00000190| 6f 72 74 3a 20 38 30 38 30 0d 0a 58 2d 46 6f 72 |ort: 8080..X-For|
|000001a0| 77 61 72 64 65 64 2d 48 6f 73 74 3a 20 6c 6f 63 |warded-Host: loc|
|000001b0| 61 6c 68 6f 73 74 3a 38 30 38 30 0d 0a 68 6f 73 |alhost:8080..hos|
|000001c0| 74 3a 20 6c 6f 63 61 6c 68 6f 73 74 3a 38 30 38 |t: localhost:808|
|000001d0| 30 0d 0a 75 70 67 72 61 64 65 3a 20 77 65 62 73 |0..upgrade: webs|
|000001e0| 6f 63 6b 65 74 0d 0a 63 6f 6e 6e 65 63 74 69 6f |ocket..connectio|
|000001f0| 6e 3a 20 75 70 67 72 61 64 65 0d 0a 73 65 63 2d |n: upgrade..sec-|
|00000200| 77 65 62 73 6f 63 6b 65 74 2d 6b 65 79 3a 20 6b |websocket-key: k|
|00000210| 39 48 4f 75 6e 4a 72 59 48 34 4c 41 59 6f 6c 71 |9HOunJrYH4LAYolq|
|00000220| 38 53 58 39 67 3d 3d 0d 0a 73 65 63 2d 77 65 62 |8SX9g==..sec-web|
|00000230| 73 6f 63 6b 65 74 2d 76 65 72 73 69 6f 6e 3a 20 |socket-version: |
|00000240| 31 33 0d 0a 0d 0a                               |13....          |
+--------+-------------------------------------------------+----------------+
2020-07-29 14:35:35.309 DEBUG 29598 --- [or-http-epoll-3] reactor.netty.http.client.HttpClient     : [id: 0x13b432c0, L:/10.0.2.15:37142 - R:shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/10.228.194.8:443] FLUSH
2020-07-29 14:35:35.321 DEBUG 29598 --- [or-http-epoll-3] reactor.netty.http.server.HttpServer     : [id: 0x08314f0e, L:/127.0.0.1:8080 - R:/127.0.0.1:55896] READ COMPLETE
2020-07-29 14:35:35.361 DEBUG 29598 --- [or-http-epoll-3] reactor.netty.http.server.HttpServer     : [id: 0x08314f0e, L:/127.0.0.1:8080 - R:/127.0.0.1:55896] READ COMPLETE
2020-07-29 14:35:35.361 DEBUG 29598 --- [or-http-epoll-3] reactor.netty.http.server.HttpServer     : [id: 0x08314f0e, L:/127.0.0.1:8080 ! R:/127.0.0.1:55896] INACTIVE
2020-07-29 14:35:35.361 DEBUG 29598 --- [or-http-epoll-3] r.n.http.server.HttpServerOperations     : [id: 0x08314f0e, L:/127.0.0.1:8080 ! R:/127.0.0.1:55896] Cancelling Websocket inbound. Closing Websocket
2020-07-29 14:35:35.362 DEBUG 29598 --- [or-http-epoll-3] reactor.netty.http.server.HttpServer     : [id: 0x08314f0e, L:/127.0.0.1:8080 ! R:/127.0.0.1:55896] WRITE: 2B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 88 00                                           |..              |
+--------+-------------------------------------------------+----------------+
2020-07-29 14:35:35.362 DEBUG 29598 --- [or-http-epoll-3] reactor.netty.http.server.HttpServer     : [id: 0x08314f0e, L:/127.0.0.1:8080 ! R:/127.0.0.1:55896] FLUSH
2020-07-29 14:35:35.362 DEBUG 29598 --- [or-http-epoll-3] reactor.netty.http.server.HttpServer     : [id: 0x08314f0e, L:/127.0.0.1:8080 ! R:/127.0.0.1:55896] UNREGISTERED
2020-07-29 14:35:35.371 DEBUG 29598 --- [or-http-epoll-3] reactor.netty.http.client.HttpClient     : [id: 0x13b432c0, L:/10.0.2.15:37142 - R:shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/10.228.194.8:443] READ: 291B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 48 54 54 50 2f 31 2e 31 20 34 30 34 20 4e 6f 74 |HTTP/1.1 404 Not|
|00000010| 20 46 6f 75 6e 64 0d 0a 43 61 63 68 65 2d 43 6f | Found..Cache-Co|
|00000020| 6e 74 72 6f 6c 3a 20 6e 6f 2d 63 61 63 68 65 2c |ntrol: no-cache,|
|00000030| 20 6e 6f 2d 73 74 6f 72 65 0d 0a 43 6f 6e 74 65 | no-store..Conte|
|00000040| 6e 74 2d 54 79 70 65 3a 20 74 65 78 74 2f 70 6c |nt-Type: text/pl|
|00000050| 61 69 6e 3b 20 63 68 61 72 73 65 74 3d 75 74 66 |ain; charset=utf|
|00000060| 2d 38 0d 0a 58 2d 43 66 2d 52 6f 75 74 65 72 65 |-8..X-Cf-Routere|
|00000070| 72 72 6f 72 3a 20 75 6e 6b 6e 6f 77 6e 5f 72 6f |rror: unknown_ro|
|00000080| 75 74 65 0d 0a 58 2d 43 6f 6e 74 65 6e 74 2d 54 |ute..X-Content-T|
|00000090| 79 70 65 2d 4f 70 74 69 6f 6e 73 3a 20 6e 6f 73 |ype-Options: nos|
|000000a0| 6e 69 66 66 0d 0a 44 61 74 65 3a 20 57 65 64 2c |niff..Date: Wed,|
|000000b0| 20 32 39 20 4a 75 6c 20 32 30 32 30 20 31 32 3a | 29 Jul 2020 12:|
|000000c0| 33 35 3a 33 35 20 47 4d 54 0d 0a 43 6f 6e 74 65 |35:35 GMT..Conte|
|000000d0| 6e 74 2d 4c 65 6e 67 74 68 3a 20 36 36 0d 0a 0d |nt-Length: 66...|
|000000e0| 0a 34 30 34 20 4e 6f 74 20 46 6f 75 6e 64 3a 20 |.404 Not Found: |
|000000f0| 52 65 71 75 65 73 74 65 64 20 72 6f 75 74 65 20 |Requested route |
|00000100| 28 27 6c 6f 63 61 6c 68 6f 73 74 3a 38 30 38 30 |('localhost:8080|
|00000110| 27 29 20 64 6f 65 73 20 6e 6f 74 20 65 78 69 73 |') does not exis|
|00000120| 74 2e 0a                                        |t..             |
+--------+-------------------------------------------------+----------------+
2020-07-29 14:35:35.372  WARN 29598 --- [or-http-epoll-3] r.netty.http.client.HttpClientConnect    : [id: 0x13b432c0, L:/10.0.2.15:37142 - R:shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/10.228.194.8:443] The connection observed an error

io.netty.handler.codec.http.websocketx.WebSocketHandshakeException: Invalid handshake response getStatus: 404 Not Found
	at io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13.verify(WebSocketClientHandshaker13.java:274) ~[netty-codec-http-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker.finishHandshake(WebSocketClientHandshaker.java:302) ~[netty-codec-http-4.1.51.Final.jar:4.1.51.Final]
	at reactor.netty.http.client.WebsocketClientOperations.onInboundNext(WebsocketClientOperations.java:118) ~[reactor-netty-0.9.10.RELEASE.jar:0.9.10.RELEASE]
	at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:96) [reactor-netty-0.9.10.RELEASE.jar:0.9.10.RELEASE]
```

Root cause: the websocket request isn't rewritten by springcloud getway, and hence lands with the "Host: localhost" to gorouter which rejects it with an unknown route
      
In initial request, spring cloud gateway had the right host:

```
2020-07-29 15:14:37.325 DEBUG 32266 --- [or-http-epoll-3] reactor.netty.http.client.HttpClient     : [id: 0x58781123, L:/10.0.2.15:52044 - R:shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/10.228.194.8:443] WRITE: 315B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 47 45 54 20 2f 74 65 73 74 20 48 54 54 50 2f 31 |GET /test HTTP/1|
|00000010| 2e 31 0d 0a 55 73 65 72 2d 41 67 65 6e 74 3a 20 |.1..User-Agent: |
|00000020| 63 75 72 6c 2f 37 2e 35 38 2e 30 0d 0a 41 63 63 |curl/7.58.0..Acc|
|00000030| 65 70 74 3a 20 2a 2f 2a 0d 0a 46 6f 72 77 61 72 |ept: */*..Forwar|
|00000040| 64 65 64 3a 20 70 72 6f 74 6f 3d 68 74 74 70 3b |ded: proto=http;|
|00000050| 68 6f 73 74 3d 22 6c 6f 63 61 6c 68 6f 73 74 3a |host="localhost:|
|00000060| 38 30 38 30 22 3b 66 6f 72 3d 22 31 32 37 2e 30 |8080";for="127.0|
|00000070| 2e 30 2e 31 3a 34 32 35 37 38 22 0d 0a 58 2d 46 |.0.1:42578"..X-F|
|00000080| 6f 72 77 61 72 64 65 64 2d 46 6f 72 3a 20 31 32 |orwarded-For: 12|
|00000090| 37 2e 30 2e 30 2e 31 0d 0a 58 2d 46 6f 72 77 61 |7.0.0.1..X-Forwa|
|000000a0| 72 64 65 64 2d 50 72 6f 74 6f 3a 20 68 74 74 70 |rded-Proto: http|
|000000b0| 0d 0a 58 2d 46 6f 72 77 61 72 64 65 64 2d 50 6f |..X-Forwarded-Po|
|000000c0| 72 74 3a 20 38 30 38 30 0d 0a 58 2d 46 6f 72 77 |rt: 8080..X-Forw|
|000000d0| 61 72 64 65 64 2d 48 6f 73 74 3a 20 6c 6f 63 61 |arded-Host: loca|
|000000e0| 6c 68 6f 73 74 3a 38 30 38 30 0d 0a 68 6f 73 74 |lhost:8080..host|
|000000f0| 3a 20 73 68 69 65 6c 64 2d 77 65 62 75 69 2d 63 |: shield-webui-c|
|00000100| 66 2d 6d 79 73 71 6c 2e 6e 64 2d 69 6e 74 2d 63 |f-mysql.nd-int-c|
|00000110| 66 61 70 69 2e 69 74 6e 2e 69 6e 74 72 61 6f 72 |fapi.itn.intraor|
|00000120| 61 6e 67 65 0d 0a 63 6f 6e 74 65 6e 74 2d 6c 65 |ange..content-le|
|00000130| 6e 67 74 68 3a 20 30 0d 0a 0d 0a                |ngth: 0....     |
+--------+-------------------------------------------------+----------------+

```      
   
Trying with websocket support did not help yet, Host header is still invalid

```yaml
      routes:
        - id: websocket_route
          uri: wss://shield-webui-cf-mysql.nd-int-cfapi.redacted.domain
          predicates:
            - Path=/v2/events
        - id: route_all
          uri: https://shield-webui-cf-mysql.nd-int-cfapi.redacted.domain
          predicates:
            - Host=**
```

```
2020-07-29 15:35:39.516 DEBUG 1487 --- [or-http-epoll-4] r.n.http.client.HttpClientOperations     : [id: 0xe6712b08, L:/10.0.2.15:52952 - R:shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/10.228.194.8:443] Attempting to perform websocket handshake with wss://localhost:8080/v2/events
2020-07-29 15:35:39.520 DEBUG 1487 --- [or-http-epoll-4] reactor.netty.http.client.HttpClient     : [id: 0xe6712b08, L:/10.0.2.15:52952 - R:shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/10.228.194.8:443] WRITE: 582B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 47 45 54 20 2f 76 32 2f 65 76 65 6e 74 73 20 48 |GET /v2/events H|
|00000010| 54 54 50 2f 31 2e 31 0d 0a 55 73 65 72 2d 41 67 |TTP/1.1..User-Ag|
|00000020| 65 6e 74 3a 20 4d 6f 7a 69 6c 6c 61 2f 35 2e 30 |ent: Mozilla/5.0|
|00000030| 20 28 58 31 31 3b 20 55 62 75 6e 74 75 3b 20 4c | (X11; Ubuntu; L|
|00000040| 69 6e 75 78 20 78 38 36 5f 36 34 3b 20 72 76 3a |inux x86_64; rv:|
|00000050| 37 38 2e 30 29 20 47 65 63 6b 6f 2f 32 30 31 30 |78.0) Gecko/2010|
|00000060| 30 31 30 31 20 46 69 72 65 66 6f 78 2f 37 38 2e |0101 Firefox/78.|
|00000070| 30 0d 0a 41 63 63 65 70 74 3a 20 2a 2f 2a 0d 0a |0..Accept: */*..|
|00000080| 41 63 63 65 70 74 2d 4c 61 6e 67 75 61 67 65 3a |Accept-Language:|
|00000090| 20 65 6e 2d 55 53 2c 65 6e 3b 71 3d 30 2e 35 0d | en-US,en;q=0.5.|
|000000a0| 0a 41 63 63 65 70 74 2d 45 6e 63 6f 64 69 6e 67 |.Accept-Encoding|
|000000b0| 3a 20 67 7a 69 70 2c 20 64 65 66 6c 61 74 65 0d |: gzip, deflate.|
|000000c0| 0a 4f 72 69 67 69 6e 3a 20 68 74 74 70 3a 2f 2f |.Origin: http://|
|000000d0| 6c 6f 63 61 6c 68 6f 73 74 3a 38 30 38 30 0d 0a |localhost:8080..|
|000000e0| 50 72 61 67 6d 61 3a 20 6e 6f 2d 63 61 63 68 65 |Pragma: no-cache|
|000000f0| 0d 0a 43 61 63 68 65 2d 43 6f 6e 74 72 6f 6c 3a |..Cache-Control:|
|00000100| 20 6e 6f 2d 63 61 63 68 65 0d 0a 46 6f 72 77 61 | no-cache..Forwa|
|00000110| 72 64 65 64 3a 20 70 72 6f 74 6f 3d 68 74 74 70 |rded: proto=http|
|00000120| 3b 68 6f 73 74 3d 22 6c 6f 63 61 6c 68 6f 73 74 |;host="localhost|
|00000130| 3a 38 30 38 30 22 3b 66 6f 72 3d 22 31 32 37 2e |:8080";for="127.|
|00000140| 30 2e 30 2e 31 3a 34 33 34 37 30 22 0d 0a 58 2d |0.0.1:43470"..X-|
|00000150| 46 6f 72 77 61 72 64 65 64 2d 46 6f 72 3a 20 31 |Forwarded-For: 1|
|00000160| 32 37 2e 30 2e 30 2e 31 0d 0a 58 2d 46 6f 72 77 |27.0.0.1..X-Forw|
|00000170| 61 72 64 65 64 2d 50 72 6f 74 6f 3a 20 68 74 74 |arded-Proto: htt|
|00000180| 70 0d 0a 58 2d 46 6f 72 77 61 72 64 65 64 2d 50 |p..X-Forwarded-P|
|00000190| 6f 72 74 3a 20 38 30 38 30 0d 0a 58 2d 46 6f 72 |ort: 8080..X-For|
|000001a0| 77 61 72 64 65 64 2d 48 6f 73 74 3a 20 6c 6f 63 |warded-Host: loc|
|000001b0| 61 6c 68 6f 73 74 3a 38 30 38 30 0d 0a 68 6f 73 |alhost:8080..hos|
|000001c0| 74 3a 20 6c 6f 63 61 6c 68 6f 73 74 3a 38 30 38 |t: localhost:808|
|000001d0| 30 0d 0a 75 70 67 72 61 64 65 3a 20 77 65 62 73 |0..upgrade: webs|
|000001e0| 6f 63 6b 65 74 0d 0a 63 6f 6e 6e 65 63 74 69 6f |ocket..connectio|
|000001f0| 6e 3a 20 75 70 67 72 61 64 65 0d 0a 73 65 63 2d |n: upgrade..sec-|
|00000200| 77 65 62 73 6f 63 6b 65 74 2d 6b 65 79 3a 20 41 |websocket-key: A|
|00000210| 48 71 63 46 6e 66 63 55 58 67 79 61 4f 5a 61 56 |HqcFnfcUXgyaOZaV|
|00000220| 64 78 39 6f 51 3d 3d 0d 0a 73 65 63 2d 77 65 62 |dx9oQ==..sec-web|
|00000230| 73 6f 63 6b 65 74 2d 76 65 72 73 69 6f 6e 3a 20 |socket-version: |
|00000240| 31 33 0d 0a 0d 0a                               |13....          |
+--------+-------------------------------------------------+----------------+
```    
   



With websocket config

```yaml
      routes:
        - id: websocket_route
          uri: wss://shield-webui-cf-mysql.nd-int-cfapi.redacted.domain
          predicates:
            - Path=/v2/events
          filters:
#            - PreserveHostHeader
            - SetRequestHostHeader=shield-webui-cf-mysql.nd-int-cfapi.redacted.domain

        - id: route_all
          uri: https://shield-webui-cf-mysql.nd-int-cfapi.redacted.domain
          predicates:
            - Host=** 
```

still fails
 
```
==> access.log <==
192.168.35.77 - - [29/Jul/2020:16:14:38 +0000] "GET /v2/events HTTP/1.1" 401 35 "-" "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:78.0) Gecko/20100101 Firefox/78.0"
192.168.35.77 - - [29/Jul/2020:16:14:38 +0000] "GET /v2/bearings HTTP/1.1" 200 154 "http://localhost:8080/" "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:78.0) Gecko/20100101 Firefox/78.0"

==> shieldd.log <==
2020-07-29 16:14:38.553495148 +000 UTC /var/vcap/packages/shield/bin/shieldd: ERROR: failed to retrieve session [] from database: (no such session)
```

```
2020-07-29 18:18:32.754 DEBUG 2616 --- [or-http-epoll-4] r.n.http.client.HttpClientOperations     : [id: 0x6a7b21f4, L:/10.0.2.15:59238 - R:shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/10.228.194.8:443] Attempting to perform websocket handshake with wss://shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/v2/events
2020-07-29 18:18:32.756 DEBUG 2616 --- [or-http-epoll-4] reactor.netty.http.client.HttpClient     : [id: 0x6a7b21f4, L:/10.0.2.15:59238 - R:shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/10.228.194.8:443] WRITE: 652B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 47 45 54 20 2f 76 32 2f 65 76 65 6e 74 73 20 48 |GET /v2/events H|
|00000010| 54 54 50 2f 31 2e 31 0d 0a 55 73 65 72 2d 41 67 |TTP/1.1..User-Ag|
|00000020| 65 6e 74 3a 20 4d 6f 7a 69 6c 6c 61 2f 35 2e 30 |ent: Mozilla/5.0|
|00000030| 20 28 58 31 31 3b 20 55 62 75 6e 74 75 3b 20 4c | (X11; Ubuntu; L|
|00000040| 69 6e 75 78 20 78 38 36 5f 36 34 3b 20 72 76 3a |inux x86_64; rv:|
|00000050| 37 38 2e 30 29 20 47 65 63 6b 6f 2f 32 30 31 30 |78.0) Gecko/2010|
|00000060| 30 31 30 31 20 46 69 72 65 66 6f 78 2f 37 38 2e |0101 Firefox/78.|
|00000070| 30 0d 0a 41 63 63 65 70 74 3a 20 2a 2f 2a 0d 0a |0..Accept: */*..|
|00000080| 41 63 63 65 70 74 2d 4c 61 6e 67 75 61 67 65 3a |Accept-Language:|
|00000090| 20 65 6e 2d 55 53 2c 65 6e 3b 71 3d 30 2e 35 0d | en-US,en;q=0.5.|
|000000a0| 0a 41 63 63 65 70 74 2d 45 6e 63 6f 64 69 6e 67 |.Accept-Encoding|
|000000b0| 3a 20 67 7a 69 70 2c 20 64 65 66 6c 61 74 65 0d |: gzip, deflate.|
|000000c0| 0a 4f 72 69 67 69 6e 3a 20 68 74 74 70 3a 2f 2f |.Origin: http://|
|000000d0| 6c 6f 63 61 6c 68 6f 73 74 3a 38 30 38 30 0d 0a |localhost:8080..|
|000000e0| 50 72 61 67 6d 61 3a 20 6e 6f 2d 63 61 63 68 65 |Pragma: no-cache|
|000000f0| 0d 0a 43 61 63 68 65 2d 43 6f 6e 74 72 6f 6c 3a |..Cache-Control:|
|00000100| 20 6e 6f 2d 63 61 63 68 65 0d 0a 46 6f 72 77 61 | no-cache..Forwa|
|00000110| 72 64 65 64 3a 20 70 72 6f 74 6f 3d 68 74 74 70 |rded: proto=http|
|00000120| 3b 68 6f 73 74 3d 73 68 69 65 6c 64 2d 77 65 62 |;host=shield-web|
|00000130| 75 69 2d 63 66 2d 6d 79 73 71 6c 2e 6e 64 2d 69 |ui-cf-mysql.nd-i|
|00000140| 6e 74 2d 63 66 61 70 69 2e 69 74 6e 2e 69 6e 74 |nt-cfapi.was.red|
|00000150| 72 61 6f 72 61 6e 67 65 3b 66 6f 72 3d 22 31 32 |raredact;for="12|
|00000160| 37 2e 30 2e 30 2e 31 3a 34 39 37 36 30 22 0d 0a |7.0.0.1:49760"..|
|00000170| 58 2d 46 6f 72 77 61 72 64 65 64 2d 46 6f 72 3a |X-Forwarded-For:|
|00000180| 20 31 32 37 2e 30 2e 30 2e 31 0d 0a 58 2d 46 6f | 127.0.0.1..X-Fo|
|00000190| 72 77 61 72 64 65 64 2d 50 72 6f 74 6f 3a 20 68 |rwarded-Proto: h|
|000001a0| 74 74 70 0d 0a 58 2d 46 6f 72 77 61 72 64 65 64 |ttp..X-Forwarded|
|000001b0| 2d 50 6f 72 74 3a 20 38 30 38 30 0d 0a 58 2d 46 |-Port: 8080..X-F|
|000001c0| 6f 72 77 61 72 64 65 64 2d 48 6f 73 74 3a 20 6c |orwarded-Host: l|
|000001d0| 6f 63 61 6c 68 6f 73 74 3a 38 30 38 30 0d 0a 68 |ocalhost:8080..h|
|000001e0| 6f 73 74 3a 20 73 68 69 65 6c 64 2d 77 65 62 75 |ost: shield-webu|
|000001f0| 69 2d 63 66 2d 6d 79 73 71 6c 2e 6e 64 2d 69 6e |i-cf-mysql.nd-in|
|00000200| 74 2d 63 66 61 70 69 2e 69 74 6e 2e 69 6e 74 72 |t-cfapi.itn.intr|
|00000210| 61 6f 72 61 6e 67 65 0d 0a 75 70 67 72 61 64 65 |aredact..upgrade|
|00000220| 3a 20 77 65 62 73 6f 63 6b 65 74 0d 0a 63 6f 6e |: websocket..con|
|00000230| 6e 65 63 74 69 6f 6e 3a 20 75 70 67 72 61 64 65 |nection: upgrade|
|00000240| 0d 0a 73 65 63 2d 77 65 62 73 6f 63 6b 65 74 2d |..sec-websocket-|
|00000250| 6b 65 79 3a 20 53 5a 36 58 42 4c 63 6a 4b 2b 61 |key: SZ6XBLcjK+a|
|00000260| 58 34 52 67 7a 63 39 4a 64 67 77 3d 3d 0d 0a 73 |X4Rgzc9Jdgw==..s|
|00000270| 65 63 2d 77 65 62 73 6f 63 6b 65 74 2d 76 65 72 |ec-websocket-ver|
|00000280| 73 69 6f 6e 3a 20 31 33 0d 0a 0d 0a             |sion: 13....    |
+--------+-------------------------------------------------+----------------+

2020-07-29 18:18:32.827 DEBUG 2616 --- [or-http-epoll-4] reactor.netty.http.client.HttpClient     : [id: 0x6a7b21f4, L:/10.0.2.15:59238 - R:shield-webui-cf-mysql.nd-int-cfapi.redacted.domain/10.228.194.8:443] READ: 216B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 48 54 54 50 2f 31 2e 31 20 34 30 31 20 55 6e 61 |HTTP/1.1 401 Una|
|00000010| 75 74 68 6f 72 69 7a 65 64 0d 0a 53 65 72 76 65 |uthorized..Serve|
|00000020| 72 3a 20 6e 67 69 6e 78 0d 0a 44 61 74 65 3a 20 |r: nginx..Date: |
|00000030| 57 65 64 2c 20 32 39 20 4a 75 6c 20 32 30 32 30 |Wed, 29 Jul 2020|
|00000040| 20 31 36 3a 31 38 3a 33 32 20 47 4d 54 0d 0a 43 | 16:18:32 GMT..C|
|00000050| 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 70 |ontent-Type: app|
|00000060| 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0d 0a 43 |lication/json..C|
|00000070| 6f 6e 74 65 6e 74 2d 4c 65 6e 67 74 68 3a 20 33 |ontent-Length: 3|
|00000080| 35 0d 0a 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 6b |5..Connection: k|
|00000090| 65 65 70 2d 61 6c 69 76 65 0d 0a 4b 65 65 70 2d |eep-alive..Keep-|
|000000a0| 41 6c 69 76 65 3a 20 74 69 6d 65 6f 75 74 3d 32 |Alive: timeout=2|
|000000b0| 30 0d 0a 0d 0a 7b 22 65 72 72 6f 72 22 3a 22 41 |0....{"error":"A|
|000000c0| 75 74 68 6f 72 69 7a 61 74 69 6f 6e 20 72 65 71 |uthorization req|
|000000d0| 75 69 72 65 64 22 7d 0a                         |uired"}.        |
+--------+-------------------------------------------------+----------------+

```

* [ ] Try with proxying directly shield backend without the gorouter
   * [ ] Push the SCG to CF to get access to 192.168.211.40:443 internal IP with an HTTP route
   * [ ] Configure browser to use an HTTP proxy for testing
  
   
```
$ curl -vvv http://shield-webui-cf-mysql.nd-int-cfapi.was.redacted/ --proxy http://osbcmdbproxy0.nd-int-paas.was.redacted:80
*   Trying 10.228.194.2...
* TCP_NODELAY set
* Connected to osbcmdbproxy0.nd-int-paas.was.redacted (10.228.194.2) port 80 (#0)
> GET http://shield-webui-cf-mysql.nd-int-cfapi.was.redacted/ HTTP/1.1
> Host: shield-webui-cf-mysql.nd-int-cfapi.was.redacted
> User-Agent: curl/7.58.0
> Accept: */*
> Proxy-Connection: Keep-Alive
> 
< HTTP/1.1 503 Service Unavailable
< Content-Type: text/plain; charset=utf-8
< X-Cf-Routererror: endpoint_failure
< X-Content-Type-Options: nosniff
< Date: Thu, 30 Jul 2020 16:05:51 GMT
< Content-Length: 24
< Strict-Transport-Security: max-age=15768000
< 
503 Service Unavailable
* Connection #0 to host osbcmdbproxy0.nd-int-paas.was.redacted left intact

```   

https://docs.cloudfoundry.org/adminguide/troubleshooting-router-error-responses.html
> endpoint_failure 	The registered endpoint for the desired route failed to handle the request.

Problem is that gorouter does not handle this HTTP PROXY and returns a 503

   * [x] Trying instead with a distinct route than the one registered in shield
   
```
java.security.cert.CertificateException: No subject alternative names matching IP address 192.168.211.40 found 
```   

   * [x] Ignore client invalid certs
   * [x] observed once to login in windows and then login request hanging in both windows and ubuntu. https://osbcmdbproxy0.nd-int-cfapi.was.redacted/
   * [x] observed once more to login in ubuntu in a fresh browser window. https://osbcmdbproxy0.nd-int-cfapi.was.redacted/
      * can't see login request in firefox webdev view
      * can't see login request in firefox webdev view
      * nginx logs
   
```
192.168.35.81 - - [30/Jul/2020:16:41:33 +0000] "POST /v2/auth/login HTTP/1.1" 499 0 "https://osbcmdbproxy0.nd-int-cfapi.was.redacted/" "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0"
192.168.35.81 - - [30/Jul/2020:16:43:48 +0000] "POST /v2/auth/login HTTP/1.1" 400 58 "https://osbcmdbproxy0.nd-int-cfapi.was.redacted/" "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0"
192.168.35.81 - - [30/Jul/2020:16:44:24 +0000] "POST /v2/auth/login HTTP/1.1" 200 650 "https://osbcmdbproxy0.nd-int-cfapi.was.redacted/" "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0"

192.168.35.81 - - [30/Jul/2020:16:47:50 +0000] "POST /v2/auth/login HTTP/1.1" 499 0 "https://osbcmdbproxy0.nd-int-cfapi.was.redacted/" "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:78.0) Gecko/20100101 Firefox/78.0"
192.168.35.81 - - [30/Jul/2020:16:48:11 +0000] "POST /v2/auth/login HTTP/1.1" 200 650 "-" "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:78.0) Gecko/20100101 Firefox/78.0"
192.168.35.81 - - [30/Jul/2020:16:50:00 +0000] "POST /v2/auth/login HTTP/1.1" 200 650 "http://osbcmdbproxy0.nd-int-paas.was.redacted/" "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:78.0) Gecko/20100101 Firefox/78.0"
192.168.35.81 - - [30/Jul/2020:17:08:01 +0000] "POST /v2/auth/login HTTP/1.1" 200 650 "https://osbcmdbproxy0.nd-int-cfapi.was.redacted/" "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:78.0) Gecko/20100101 Firefox/78.0"
```

https://en.wikipedia.org/wiki/List_of_HTTP_status_codes

> nginx
> 
> The nginx web server software expands the 4xx error space to signal issues with the client's request.[87][88] 
> 
> 499 Client Closed Request
>     Used when the client has closed the request before the server could send a response.
>
    * [ ] configure nginx wiretrace to troubleshoot
    * [ ] experiment with IP address discovery by bosh director DNS server query in Java ?
    * [ ] better understand websocket protocol and CF routing
       * https://tools.ietf.org/html/rfc6455 WebSocket protocol RFC
       * https://www.w3.org/TR/websockets/ websocket JS api
       * https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API Firefox doc on WebSocket W3C API
          * https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent details on close event
          * https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/error_event 
    * [ ] study anynes sso proxy app https://github.com/anynines/sso-proxy-app (commercially licensed)
    

In chrome with gorouter to nginx routing
        
```
data.js:414 WebSocket connection to 'wss://shield-webui-cf-mysql.nd-int-cfapi.was.redacted/v2/events' failed: HTTP Authentication failed; no valid credentials available
data.js:417 websocket failed:  
Event {isTrusted: true, type: "error", target: WebSocket, currentTarget: WebSocket, eventPhase: 2, …}
bubbles: false
cancelBubble: false
cancelable: false
composed: false
currentTarget: WebSocket {url: "wss://shield-webui-cf-mysql.nd-int-cfapi.was.redacted/v2/events", readyState: 3, bufferedAmount: 0, onopen: ƒ, onerror: ƒ, …}
defaultPrevented: false
eventPhase: 0
isTrusted: true
path: []
returnValue: true
srcElement: WebSocket {url: "wss://shield-webui-cf-mysql.nd-int-cfapi.was.redacted/v2/events", readyState: 3, bufferedAmount: 0, onopen: ƒ, onerror: ƒ, …}
target: WebSocket {url: "wss://shield-webui-cf-mysql.nd-int-cfapi.was.redacted/v2/events", readyState: 3, bufferedAmount: 0, onopen: ƒ, onerror: ƒ, …}
timeStamp: 860.5549999920186
type: "error"
__proto__: Event
```


upon login, 1st successfull ws

https://w3c.github.io/web-performance/specs/HAR/Overview.html

```

```

* [ ] Compare HAR when OK (direct gorouter routing) and KO (SCG routing)
   * Before login WSS `events` requests return WS handshake 101 instead of 401 from nginx response.
   
```
2020-07-31 17:01:14.452 DEBUG 23802 --- [or-http-epoll-2] r.netty.http.client.HttpClientConnect    : [id: 0xb1d7b232, L:/10.0.2.15:57952 - R:shield-webui-cf-mysql.nd-int-cfapi.was.redacted/10.228.194.8:443] Handler is being applied: {uri=wss://shield-webui-cf-mysql.nd-int-cfapi.was.redacted/v2/events, method=GET}
2020-07-31 17:01:14.452 DEBUG 23802 --- [or-http-epoll-2] r.n.resources.PooledConnectionProvider   : [id: 0xb1d7b232, L:/10.0.2.15:57952 - R:shield-webui-cf-mysql.nd-int-cfapi.was.redacted/10.228.194.8:443] onStateChange(GET{uri=/v2/events, connection=PooledConnection{channel=[id: 0xb1d7b232, L:/10.0.2.15:57952 - R:shield-webui-cf-mysql.nd-int-cfapi.was.redacted/10.228.194.8:443]}}, [request_prepared])
2020-07-31 17:01:14.452 DEBUG 23802 --- [or-http-epoll-2] reactor.netty.ReactorNetty               : [id: 0xb1d7b232, L:/10.0.2.15:57952 - R:shield-webui-cf-mysql.nd-int-cfapi.was.redacted/10.228.194.8:443] Added encoder [reactor.left.httpAggregator] at the beginning of the user pipeline, full pipeline: [reactor.left.sslHandler, reactor.left.loggingHandler, reactor.left.httpCodec, reactor.left.httpAggregator, reactor.right.reactiveBridge, DefaultChannelPipeline$TailContext#0]
2020-07-31 17:01:14.452 DEBUG 23802 --- [or-http-epoll-2] r.n.http.client.HttpClientOperations     : [id: 0xb1d7b232, L:/10.0.2.15:57952 - R:shield-webui-cf-mysql.nd-int-cfapi.was.redacted/10.228.194.8:443] Attempting to perform websocket handshake with wss://shield-webui-cf-mysql.nd-int-cfapi.was.redacted/v2/events
2020-07-31 17:01:14.454 DEBUG 23802 --- [or-http-epoll-2] reactor.netty.http.client.HttpClient     : [id: 0xb1d7b232, L:/10.0.2.15:57952 - R:shield-webui-cf-mysql.nd-int-cfapi.was.redacted/10.228.194.8:443] WRITE: 652B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 47 45 54 20 2f 76 32 2f 65 76 65 6e 74 73 20 48 |GET /v2/events H|
|00000010| 54 54 50 2f 31 2e 31 0d 0a 55 73 65 72 2d 41 67 |TTP/1.1..User-Ag|
|00000020| 65 6e 74 3a 20 4d 6f 7a 69 6c 6c 61 2f 35 2e 30 |ent: Mozilla/5.0|
|00000030| 20 28 58 31 31 3b 20 55 62 75 6e 74 75 3b 20 4c | (X11; Ubuntu; L|
|00000040| 69 6e 75 78 20 78 38 36 5f 36 34 3b 20 72 76 3a |inux x86_64; rv:|
|00000050| 37 39 2e 30 29 20 47 65 63 6b 6f 2f 32 30 31 30 |79.0) Gecko/2010|
|00000060| 30 31 30 31 20 46 69 72 65 66 6f 78 2f 37 39 2e |0101 Firefox/79.|
|00000070| 30 0d 0a 41 63 63 65 70 74 3a 20 2a 2f 2a 0d 0a |0..Accept: */*..|
|00000080| 41 63 63 65 70 74 2d 4c 61 6e 67 75 61 67 65 3a |Accept-Language:|
|00000090| 20 65 6e 2d 55 53 2c 65 6e 3b 71 3d 30 2e 35 0d | en-US,en;q=0.5.|
|000000a0| 0a 41 63 63 65 70 74 2d 45 6e 63 6f 64 69 6e 67 |.Accept-Encoding|
|000000b0| 3a 20 67 7a 69 70 2c 20 64 65 66 6c 61 74 65 0d |: gzip, deflate.|
|000000c0| 0a 4f 72 69 67 69 6e 3a 20 68 74 74 70 3a 2f 2f |.Origin: http://|
|000000d0| 6c 6f 63 61 6c 68 6f 73 74 3a 38 30 38 30 0d 0a |localhost:8080..|
|000000e0| 50 72 61 67 6d 61 3a 20 6e 6f 2d 63 61 63 68 65 |Pragma: no-cache|
|000000f0| 0d 0a 43 61 63 68 65 2d 43 6f 6e 74 72 6f 6c 3a |..Cache-Control:|
|00000100| 20 6e 6f 2d 63 61 63 68 65 0d 0a 46 6f 72 77 61 | no-cache..Forwa|
|00000110| 72 64 65 64 3a 20 70 72 6f 74 6f 3d 68 74 74 70 |rded: proto=http|
|00000120| 3b 68 6f 73 74 3d 73 68 69 65 6c 64 2d 77 65 62 |;host=shield-web|
|00000130| 75 69 2d 63 66 2d 6d 79 73 71 6c 2e 6e 64 2d 69 |ui-cf-mysql.nd-i|
|00000140| 6e 74 2d 63 66 61 70 69 2e 69 74 6e 2e 69 6e 74 |nt-cfapi.was.red|
|00000150| 72 61 6f 72 61 6e 67 65 3b 66 6f 72 3d 22 31 32 |acted...;for="12|
|00000160| 37 2e 30 2e 30 2e 31 3a 33 33 32 31 30 22 0d 0a |7.0.0.1:33210"..|
|00000170| 58 2d 46 6f 72 77 61 72 64 65 64 2d 46 6f 72 3a |X-Forwarded-For:|
|00000180| 20 31 32 37 2e 30 2e 30 2e 31 0d 0a 58 2d 46 6f | 127.0.0.1..X-Fo|
|00000190| 72 77 61 72 64 65 64 2d 50 72 6f 74 6f 3a 20 68 |rwarded-Proto: h|
|000001a0| 74 74 70 0d 0a 58 2d 46 6f 72 77 61 72 64 65 64 |ttp..X-Forwarded|
|000001b0| 2d 50 6f 72 74 3a 20 38 30 38 30 0d 0a 58 2d 46 |-Port: 8080..X-F|
|000001c0| 6f 72 77 61 72 64 65 64 2d 48 6f 73 74 3a 20 6c |orwarded-Host: l|
|000001d0| 6f 63 61 6c 68 6f 73 74 3a 38 30 38 30 0d 0a 68 |ocalhost:8080..h|
|000001e0| 6f 73 74 3a 20 73 68 69 65 6c 64 2d 77 65 62 75 |ost: shield-webu|
|000001f0| 69 2d 63 66 2d 6d 79 73 71 6c 2e 6e 64 2d 69 6e |i-cf-mysql.nd-in|
|00000200| 74 2d 63 66 61 70 69 2e 69 74 6e 2e 69 6e 74 72 |t-cfapi.was.reda|
|00000210| 61 6f 72 61 6e 67 65 0d 0a 75 70 67 72 61 64 65 |cted.....upgrade|
|00000220| 3a 20 77 65 62 73 6f 63 6b 65 74 0d 0a 63 6f 6e |: websocket..con|
|00000230| 6e 65 63 74 69 6f 6e 3a 20 75 70 67 72 61 64 65 |nection: upgrade|
|00000240| 0d 0a 73 65 63 2d 77 65 62 73 6f 63 6b 65 74 2d |..sec-websocket-|
|00000250| 6b 65 79 3a 20 58 46 6d 59 58 58 7a 56 54 39 39 |key: XFmYXXzVT99|
|00000260| 6b 4d 50 71 42 6c 76 69 4f 76 41 3d 3d 0d 0a 73 |kMPqBlviOvA==..s|
|00000270| 65 63 2d 77 65 62 73 6f 63 6b 65 74 2d 76 65 72 |ec-websocket-ver|
|00000280| 73 69 6f 6e 3a 20 31 33 0d 0a 0d 0a             |sion: 13....    |
+--------+-------------------------------------------------+----------------+
2020-07-31 17:01:14.454 DEBUG 23802 --- [or-http-epoll-2] reactor.netty.http.client.HttpClient     : [id: 0xb1d7b232, L:/10.0.2.15:57952 - R:shield-webui-cf-mysql.nd-int-cfapi.was.redacted/10.228.194.8:443] FLUSH
2020-07-31 17:01:14.459 DEBUG 23802 --- [or-http-epoll-4] reactor.netty.http.server.HttpServer     : [id: 0x565db29f, L:/127.0.0.1:8080 - R:/127.0.0.1:33210] READ COMPLETE
2020-07-31 17:01:14.535 DEBUG 23802 --- [or-http-epoll-2] reactor.netty.http.client.HttpClient     : [id: 0xb1d7b232, L:/10.0.2.15:57952 - R:shield-webui-cf-mysql.nd-int-cfapi.was.redacted/10.228.194.8:443] READ: 216B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 48 54 54 50 2f 31 2e 31 20 34 30 31 20 55 6e 61 |HTTP/1.1 401 Una|
|00000010| 75 74 68 6f 72 69 7a 65 64 0d 0a 53 65 72 76 65 |uthorized..Serve|
|00000020| 72 3a 20 6e 67 69 6e 78 0d 0a 44 61 74 65 3a 20 |r: nginx..Date: |
|00000030| 46 72 69 2c 20 33 31 20 4a 75 6c 20 32 30 32 30 |Fri, 31 Jul 2020|
|00000040| 20 31 35 3a 30 31 3a 31 34 20 47 4d 54 0d 0a 43 | 15:01:14 GMT..C|
|00000050| 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 70 |ontent-Type: app|
|00000060| 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0d 0a 43 |lication/json..C|
|00000070| 6f 6e 74 65 6e 74 2d 4c 65 6e 67 74 68 3a 20 33 |ontent-Length: 3|
|00000080| 35 0d 0a 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 6b |5..Connection: k|
|00000090| 65 65 70 2d 61 6c 69 76 65 0d 0a 4b 65 65 70 2d |eep-alive..Keep-|
|000000a0| 41 6c 69 76 65 3a 20 74 69 6d 65 6f 75 74 3d 32 |Alive: timeout=2|
|000000b0| 30 0d 0a 0d 0a 7b 22 65 72 72 6f 72 22 3a 22 41 |0....{"error":"A|
|000000c0| 75 74 68 6f 72 69 7a 61 74 69 6f 6e 20 72 65 71 |uthorization req|
|000000d0| 75 69 72 65 64 22 7d 0a                         |uired"}.        |
+--------+-------------------------------------------------+----------------+
2020-07-31 17:01:14.537  WARN 23802 --- [or-http-epoll-2] r.netty.http.client.HttpClientConnect    : [id: 0xb1d7b232, L:/10.0.2.15:57952 - R:shield-webui-cf-mysql.nd-int-cfapi.was.redacted/10.228.194.8:443] The connection observed an error

io.netty.handler.codec.http.websocketx.WebSocketHandshakeException: Invalid handshake response getStatus: 401 Unauthorized
	at io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13.verify(WebSocketClientHandshaker13.java:274) ~[netty-codec-http-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker.finishHandshake(WebSocketClientHandshaker.java:302) ~[netty-codec-http-4.1.51.Final.jar:4.1.51.Final]
	at reactor.netty.http.client.WebsocketClientOperations.onInboundNext(WebsocketClientOperations.java:118) ~[reactor-netty-0.9.10.RELEASE.jar:0.9.10.RELEASE]
	at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:96) [reactor-netty-0.9.10.RELEASE.jar:0.9.10.RELEASE]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.codec.MessageToMessageDecoder.channelRead(MessageToMessageDecoder.java:103) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:436) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:324) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:296) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:251) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.logging.LoggingHandler.channelRead(LoggingHandler.java:271) [netty-handler-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.ssl.SslHandler.unwrap(SslHandler.java:1526) [netty-handler-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.ssl.SslHandler.decodeJdkCompatible(SslHandler.java:1275) [netty-handler-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.ssl.SslHandler.decode(SslHandler.java:1322) [netty-handler-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.codec.ByteToMessageDecoder.decodeRemovalReentryProtection(ByteToMessageDecoder.java:501) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:440) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:276) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1410) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:919) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:792) [netty-transport-native-epoll-4.1.51.Final-linux-x86_64.jar:4.1.51.Final]
	at io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:475) [netty-transport-native-epoll-4.1.51.Final-linux-x86_64.jar:4.1.51.Final]
	at io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:378) [netty-transport-native-epoll-4.1.51.Final-linux-x86_64.jar:4.1.51.Final]
	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:989) [netty-common-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74) [netty-common-4.1.51.Final.jar:4.1.51.Final]
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30) [netty-common-4.1.51.Final.jar:4.1.51.Final]
	at java.lang.Thread.run(Thread.java:748) [na:1.8.0_252]

2020-07-31 17:01:14.543 DEBUG 23802 --- [or-http-epoll-2] r.n.http.server.HttpServerOperations     : [id: 0x565db29f, L:/127.0.0.1:8080 - R:/127.0.0.1:33210] Outbound error happened

io.netty.handler.codec.http.websocketx.WebSocketHandshakeException: Invalid handshake response getStatus: 401 Unauthorized
	at io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13.verify(WebSocketClientHandshaker13.java:274) ~[netty-codec-http-4.1.51.Final.jar:4.1.51.Final]
	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Error has been observed at the following site(s):
	|_ checkpoint ⇢ http://localhost:8080/v2/events [ReactorNettyRequestUpgradeStrategy]
Stack trace:
		at io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13.verify(WebSocketClientHandshaker13.java:274) ~[netty-codec-http-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker.finishHandshake(WebSocketClientHandshaker.java:302) ~[netty-codec-http-4.1.51.Final.jar:4.1.51.Final]
		at reactor.netty.http.client.WebsocketClientOperations.onInboundNext(WebsocketClientOperations.java:118) ~[reactor-netty-0.9.10.RELEASE.jar:0.9.10.RELEASE]
		at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:96) [reactor-netty-0.9.10.RELEASE.jar:0.9.10.RELEASE]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.codec.MessageToMessageDecoder.channelRead(MessageToMessageDecoder.java:103) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:436) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:324) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:296) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:251) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.logging.LoggingHandler.channelRead(LoggingHandler.java:271) [netty-handler-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.ssl.SslHandler.unwrap(SslHandler.java:1526) [netty-handler-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.ssl.SslHandler.decodeJdkCompatible(SslHandler.java:1275) [netty-handler-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.ssl.SslHandler.decode(SslHandler.java:1322) [netty-handler-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.codec.ByteToMessageDecoder.decodeRemovalReentryProtection(ByteToMessageDecoder.java:501) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:440) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:276) [netty-codec-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1410) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:919) [netty-transport-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:792) [netty-transport-native-epoll-4.1.51.Final-linux-x86_64.jar:4.1.51.Final]
		at io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:475) [netty-transport-native-epoll-4.1.51.Final-linux-x86_64.jar:4.1.51.Final]
		at io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:378) [netty-transport-native-epoll-4.1.51.Final-linux-x86_64.jar:4.1.51.Final]
		at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:989) [netty-common-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74) [netty-common-4.1.51.Final.jar:4.1.51.Final]
		at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30) [netty-common-4.1.51.Final.jar:4.1.51.Final]
		at java.lang.Thread.run(Thread.java:748) [na:1.8.0_252]

2020-07-31 17:01:14.544 DEBUG 23802 --- [or-http-epoll-4] reactor.netty.http.server.HttpServer     : [id: 0x565db29f, L:/127.0.0.1:8080 - R:/127.0.0.1:33210] WRITE: 25B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 88 17 03 ea 53 65 72 76 65 72 20 69 6e 74 65 72 |....Server inter|
|00000010| 6e 61 6c 20 65 72 72 6f 72                      |nal error       |
+--------+-------------------------------------------------+----------------+
2020-07-31 17:01:14.544 DEBUG 23802 --- [or-http-epoll-4] reactor.netty.http.server.HttpServer     : [id: 0x565db29f, L:/127.0.0.1:8080 - R:/127.0.0.1:33210] FLUSH
2020-07-31 17:01:14.547 DEBUG 23802 --- [or-http-epoll-4] reactor.netty.http.server.HttpServer     : [id: 0x565db29f, L:/127.0.0.1:8080 - R:/127.0.0.1:33210] CLOSE
2020-07-31 17:01:14.547 DEBUG 23802 --- [or-http-epoll-4] reactor.netty.http.server.HttpServer     : [id: 0x565db29f, L:/127.0.0.1:8080 ! R:/127.0.0.1:33210] INACTIVE
2020-07-31 17:01:14.548 DEBUG 23802 --- [or-http-epoll-4] reactor.netty.http.server.HttpServer     : [id: 0x565db29f, L:/127.0.0.1:8080 ! R:/127.0.0.1:33210] UNREGISTERED

```
   * [x] search how to recover from WebSocketHandshakeException by returning the original exception
      * Likely would need to catch the exception and modify the response. 
      * [x] search github issue for WebSocketHandshakeException 
      * [ ] Look for error handling in documentation https://cloud.spring.io/spring-cloud-gateway/reference/html/
         * https://cloud.spring.io/spring-cloud-gateway/reference/html/#fallback-headers 
      * [x] Look for filters API https://cloud.spring.io/spring-cloud-gateway/reference/html/#writing-custom-gatewayfilter-factories
          * [ ] Clarify how to register the factory
          * [ ] Look for example in getting started
             * provides fallback with static response
             * [ ] Look for syntax to error return in fallback handler
      * [ ] Increase log level to understand where is taken decision to complete de handshake in the response.
      * [ ] Inspect how RetryFilter handles error
      * [ ] Submit an issue
         * [ ] Check WSS specs for any compliance statement about intermediates 
            * https://tools.ietf.org/html/rfc6455#section-4.2.2
            
               >  2.  The server can perform additional client authentication, for
               >        example, by returning a 401 status code with the corresponding
               >        |WWW-Authenticate| header field as described in [RFC2616]. 
      * [x] Submit a stackoverflow question: https://stackoverflow.com/questions/63196638/spring-cloud-gateway-hides-server-websocket-handshake-401-failures-to-clients
      * [ ] Try fixing ReactorNettyRequestUpgradeStrategy (webflux): assuming it receives the error but does not map it into handle handshake response error 
         * [ ] Look for related issues
            * [websocket+handshake](https://github.com/spring-projects/spring-framework/issues?q=is%3Aissue+websocket+handshake)
               * https://github.com/spring-projects/spring-framework/issues/18953 Better documentation on WebSockets and support for token based authentication 
         * [ ] Look for source code and associated unit tests
            * Not much in unit test https://github.com/spring-projects/spring-framework/blob/913eca9e141b9c58c2c175dc822ceab624a96f0b/spring-webflux/src/test/java/org/springframework/web/reactive/socket/server/upgrade/ReactorNettyRequestUpgradeStrategyTests.java#L27
            * WebSocket session close is covered into integration test https://github.com/spring-projects/spring-framework/blob/22bf62def11c990335b2d604c147b524105bf32e/spring-webflux/src/test/java/org/springframework/web/reactive/socket/WebSocketIntegrationTests.java#L147-L164
            * Not clear about websocket handshake error 
            * [ ] Check how to register an error handler
            * [x] Search for `spring reactive websocket 401 authorization failure` 
               * https://github.com/spring-cloud/spring-cloud-gateway/issues/857 Gateway fail to close websocket when server return 401 when fail to check token during handshake
                  * Exact same symptom as my problem
                  * Marked as duplicate of https://github.com/spring-cloud/spring-cloud-gateway/issues/845 Gateway don't close websocket connection when client close websocket.
         * [ ] try locating where is the exception handled
            * [ ] set log level to trace
            * reactor.netty.http.server.WebsocketServerOperations.onOutboundError() is where the error is caught, with the exception thrown indicating the http status
               * [ ] read reactor netty documentation to better understand the API https://projectreactor.io/docs/netty/release/reference/index.html
      * [ ] Try fixing WebsocketRoutingFilter (gateway), assuming it can influence the handskake error
         * [ ] Read documentation about error handling
            * https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-dispatcher-exceptions
            * https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-exception-handler seems relevant
         * [ ] Check response status handler is indeed registered from logs: did not see one
         * [x] Search source code for registered WebExceptionHandler 
            * WebFluxConfigurationSupport indeed configures a WebFluxResponseStatusExceptionHandler
             ```
           Common WebFlux exception handler that detects instances of org.springframework.web.server.ResponseStatusException (inherited from the base class) as well as exceptions annotated with @ResponseStatus by determining the HTTP status for them and updating the status of the response accordingly. If the response is already committed, the error remains unresolved and is propagated. 
             ```
         * [x] Increase log level on reactor netty server side: reactor.netty.http.server
         * [x] Reproduce outside of webbrowser with curl to get raw byte output and be faster to reproduce. Need to edit ws:// protocol to http:// otherwise curl fails with `curl: (1) Protocol "ws" not supported or disabled in libcurl`
         ```
        curl -vvv 'http://localhost:8080/v2/events' -H 'User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:79.0) G0' -H 'Accept: */*' -H 'Accept-Language: en-US,en;q=0.5' --compressed -H 'Sec-WebSocket-Version: 13' -H 'Origin: http://localhost:8080' -H 'Sec-WebSocket-Extensions: permessage-deflate' -H 'Sec-WebSocket-Key: RVzxNmmNHdZQPNOCIWbomA==' -H 'Connection: keep-alive, Upgrade' -H 'Pragma: no-cache' -H 'Cache-Control: no-cache' -H 'Upgrade: websocket'
          ```
         * [x] Add debugger breakpoint in the error handler
            * [x] during handling
               * triggers for premature webpage closed
               * does not trigger for 401 response
                 
```
2020-08-03 12:28:48.678 DEBUG 27758 --- [or-http-epoll-2] reactor.netty.channel.ChannelOperations  : [id: 0x73ad1898, L:/127.0.0.1:8080 ! R:/127.0.0.1:41288] An outbound error could not be processed

io.netty.handler.codec.http.websocketx.WebSocketHandshakeException: Invalid handshake response getStatus: 401 Unauthorized
	at io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13.verify(WebSocketClientHandshaker13.java:274) ~[netty-codec-http-4.1.51.Final.jar:4.1.51.Final]
	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Assembly trace from producer [reactor.core.publisher.MonoCreate] :
	reactor.core.publisher.Mono.create(Mono.java:191)
	reactor.netty.http.client.HttpClientConnect$MonoHttpConnect.subscribe(HttpClientConnect.java:291)
Error has been observed at the following site(s):
	|_      Mono.create ⇢ at reactor.netty.http.client.HttpClientConnect$MonoHttpConnect.subscribe(HttpClientConnect.java:291)
	|_   Flux.concatMap ⇢ at reactor.util.retry.RetrySpec.generateCompanion(RetrySpec.java:324)
	|_   Mono.retryWhen ⇢ at reactor.netty.http.client.HttpClientConnect$MonoHttpConnect.subscribe(HttpClientConnect.java:328)
	|_ Mono.flatMapMany ⇢ at reactor.netty.http.client.WebsocketFinalizer.handle(WebsocketFinalizer.java:87)
	|_ Flux.doOnRequest ⇢ at org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient.execute(ReactorNettyWebSocketClient.java:150)
	|_        Flux.next ⇢ at org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient.execute(ReactorNettyWebSocketClient.java:155)
	|_       checkpoint ⇢ http://localhost:8080/v2/events [ReactorNettyRequestUpgradeStrategy]
```


```
curl --trace - 'http://localhost:8080/v2/events' -H 'User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:79.0) Gecko/20100101 Firefox/79.0' -H 'Accept: */*' -H 'Accept-Language: en-US,en;q=0.5' --compressed -H 'Sec-WebSocket-Version: 13' -H 'Origin: http://localhost:8080' -H 'Sec-WebSocket-Extensions: permessage-deflate' -H 'Sec-WebSocket-Key: RVzxNmmNHdZQPNOCIWbomA==' -H 'Connection: keep-alive, Upgrade' -H 'Pragma: no-cache' -H 'Cache-Control: no-cache' -H 'Upgrade: websocket'

<= Recv header, 21 bytes (0x15)
0000: 63 6f 6e 6e 65 63 74 69 6f 6e 3a 20 75 70 67 72 connection: upgr
0010: 61 64 65 0d 0a                                  ade..
<= Recv header, 52 bytes (0x34)
0000: 73 65 63 2d 77 65 62 73 6f 63 6b 65 74 2d 61 63 sec-websocket-ac
0010: 63 65 70 74 3a 20 76 73 46 36 34 32 64 69 44 62 cept: vsF642diDb
0020: 74 6b 57 54 5a 77 76 50 58 52 6f 37 67 65 76 47 tkWTZwvPXRo7gevG
0030: 49 3d 0d 0a                                     I=..
<= Recv header, 2 bytes (0x2)
0000: 0d 0a                                           ..
<= Recv data, 25 bytes (0x19)
0000: 88 17 03 ea 53 65 72 76 65 72 20 69 6e 74 65 72 ....Server inter
0010: 6e 61 6c 20 65 72 72 6f 72                      nal error
��Server internal error== Info: Connection #0 to host localhost left intact

```

```
strace  -e trace=network curl  'http://localhost:8080/v2/events' -H 'User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:79.0) Gecko/20100101 Firefox/79.0' -H 'Accept: */*' -H 'Accept-Language: en-US,en;q=0.5' --compressed -H 'Sec-WebSocket-Version: 13' -H 'Origin: http://localhost:8080' -H 'Sec-WebSocket-Extensions: permessage-deflate' -H 'Sec-WebSocket-Key: RVzxNmmNHdZQPNOCIWbomA==' -H 'Connection: keep-alive, Upgrade' -H 'Pragma: no-cache' -H 'Cache-Control: no-cache' -H 'Upgrade: websocket'
socket(AF_INET6, SOCK_DGRAM, IPPROTO_IP) = 3
socket(AF_INET, SOCK_STREAM, IPPROTO_TCP) = 3
setsockopt(3, SOL_TCP, TCP_NODELAY, [1], 4) = 0
setsockopt(3, SOL_SOCKET, SO_KEEPALIVE, [1], 4) = 0
setsockopt(3, SOL_TCP, TCP_KEEPIDLE, [60], 4) = 0
setsockopt(3, SOL_TCP, TCP_KEEPINTVL, [60], 4) = 0
connect(3, {sa_family=AF_INET, sin_port=htons(8080), sin_addr=inet_addr("127.0.0.1")}, 16) = -1 EINPROGRESS (Operation now in progress)
getsockopt(3, SOL_SOCKET, SO_ERROR, [0], [4]) = 0
getpeername(3, {sa_family=AF_INET, sin_port=htons(8080), sin_addr=inet_addr("127.0.0.1")}, [128->16]) = 0
getsockname(3, {sa_family=AF_INET, sin_port=htons(42764), sin_addr=inet_addr("127.0.0.1")}, [128->16]) = 0
sendto(3, "GET /v2/events HTTP/1.1\r\nHost: l"..., 462, MSG_NOSIGNAL, NULL, 0) = 462
recvfrom(3, "HTTP/1.1 101 Switching Protocols"..., 102400, 0, NULL, NULL) = 129
recvfrom(3, "\210\27\3\352Server internal error", 102400, 0, NULL, NULL) = 25
recvfrom(3, "", 102400, 0, NULL, NULL)  = 0
��Server internal error+++ exited with 0 +++

```



```
	@Override
	protected void onOutboundError(Throwable err) {
		if (channel().isActive()) {
			if (log.isDebugEnabled()) {
				log.debug(format(channel(), "Outbound error happened"), err);
			}
			sendCloseNow(new CloseWebSocketFrame(1002, "Server internal error"), f ->
					terminate());
		}
	}

 at stack trace:
onOutboundError:172, WebsocketServerOperations (reactor.netty.http.server)
onError:219, ChannelOperations (reactor.netty.channel)
onError:670, HttpServerOperations$WebsocketSubscriber (reactor.netty.http.server)
onError:390, FluxOnAssembly$OnAssemblySubscriber (reactor.core.publisher)
onError:390, FluxOnAssembly$OnAssemblySubscriber (reactor.core.publisher)
onError:87, MonoNext$NextSubscriber (reactor.core.publisher)
onError:390, FluxOnAssembly$OnAssemblySubscriber (reactor.core.publisher)
onError:227, FluxPeekFuseable$PeekFuseableSubscriber (reactor.core.publisher)
onError:390, FluxOnAssembly$OnAssemblySubscriber (reactor.core.publisher)
onError:197, MonoFlatMapMany$FlatMapManyMain (reactor.core.publisher)
onError:390, FluxOnAssembly$OnAssemblySubscriber (reactor.core.publisher)
onError:124, SerializedSubscriber (reactor.core.publisher)
whenError:213, FluxRetryWhen$RetryWhenMainSubscriber (reactor.core.publisher)
onError:255, FluxRetryWhen$RetryWhenOtherSubscriber (reactor.core.publisher)
onError:390, FluxOnAssembly$OnAssemblySubscriber (reactor.core.publisher)
drain:406, FluxConcatMap$ConcatMapImmediate (reactor.core.publisher)
onNext:243, FluxConcatMap$ConcatMapImmediate (reactor.core.publisher)
onNext:333, DirectProcessor$DirectInner (reactor.core.publisher)
onNext:142, DirectProcessor (reactor.core.publisher)
onNext:99, SerializedSubscriber (reactor.core.publisher)
onError:180, FluxRetryWhen$RetryWhenMainSubscriber (reactor.core.publisher)
onError:390, FluxOnAssembly$OnAssemblySubscriber (reactor.core.publisher)
error:185, MonoCreate$DefaultMonoSink (reactor.core.publisher)
onUncaughtException:402, HttpClientConnect$HttpObserver (reactor.netty.http.client)
onUncaughtException:507, ReactorNetty$CompositeConnectionObserver (reactor.netty)
onUncaughtException:528, PooledConnectionProvider$DisposableAcquire (reactor.netty.resources)
onUncaughtException:364, PooledConnectionProvider$PooledConnection (reactor.netty.resources)
drainReceiver:225, FluxReceive (reactor.netty.channel)
onInboundError:430, FluxReceive (reactor.netty.channel)
onInboundError:442, ChannelOperations (reactor.netty.channel)
onInboundNext:122, WebsocketClientOperations (reactor.netty.http.client)
channelRead:96, ChannelOperationsHandler (reactor.netty.channel)
invokeChannelRead:379, AbstractChannelHandlerContext (io.netty.channel)
invokeChannelRead:365, AbstractChannelHandlerContext (io.netty.channel)
fireChannelRead:357, AbstractChannelHandlerContext (io.netty.channel)
channelRead:103, MessageToMessageDecoder (io.netty.handler.codec)
invokeChannelRead:379, AbstractChannelHandlerContext (io.netty.channel)
invokeChannelRead:365, AbstractChannelHandlerContext (io.netty.channel)
fireChannelRead:357, AbstractChannelHandlerContext (io.netty.channel)
fireChannelRead:436, CombinedChannelDuplexHandler$DelegatingChannelHandlerContext (io.netty.channel)
fireChannelRead:324, ByteToMessageDecoder (io.netty.handler.codec)
channelRead:296, ByteToMessageDecoder (io.netty.handler.codec)
channelRead:251, CombinedChannelDuplexHandler (io.netty.channel)
invokeChannelRead:379, AbstractChannelHandlerContext (io.netty.channel)
invokeChannelRead:365, AbstractChannelHandlerContext (io.netty.channel)
fireChannelRead:357, AbstractChannelHandlerContext (io.netty.channel)
channelRead:271, LoggingHandler (io.netty.handler.logging)
invokeChannelRead:379, AbstractChannelHandlerContext (io.netty.channel)
invokeChannelRead:365, AbstractChannelHandlerContext (io.netty.channel)
fireChannelRead:357, AbstractChannelHandlerContext (io.netty.channel)
unwrap:1526, SslHandler (io.netty.handler.ssl)
decodeJdkCompatible:1275, SslHandler (io.netty.handler.ssl)
decode:1322, SslHandler (io.netty.handler.ssl)
decodeRemovalReentryProtection:501, ByteToMessageDecoder (io.netty.handler.codec)
callDecode:440, ByteToMessageDecoder (io.netty.handler.codec)
channelRead:276, ByteToMessageDecoder (io.netty.handler.codec)
invokeChannelRead:379, AbstractChannelHandlerContext (io.netty.channel)
invokeChannelRead:365, AbstractChannelHandlerContext (io.netty.channel)
fireChannelRead:357, AbstractChannelHandlerContext (io.netty.channel)
channelRead:1410, DefaultChannelPipeline$HeadContext (io.netty.channel)
invokeChannelRead:379, AbstractChannelHandlerContext (io.netty.channel)
invokeChannelRead:365, AbstractChannelHandlerContext (io.netty.channel)
fireChannelRead:919, DefaultChannelPipeline (io.netty.channel)
epollInReady:792, AbstractEpollStreamChannel$EpollStreamUnsafe (io.netty.channel.epoll)
processReady:475, EpollEventLoop (io.netty.channel.epoll)
run:378, EpollEventLoop (io.netty.channel.epoll)
run:989, SingleThreadEventExecutor$4 (io.netty.util.concurrent)
run:74, ThreadExecutorMap$2 (io.netty.util.internal)
run:30, FastThreadLocalRunnable (io.netty.util.concurrent)
run:748, Thread (java.lang) 
```


* Aug 3rd, 2020 3pm
   * status:
       * Very likely bug in SCG/reactor-netty 
       * shield loops on unexpected response (which was modified by SCG proxification)
   * Fixes
       * [x] Submit a GH issue as a complement to stack overflow question https://github.com/spring-cloud/spring-cloud-gateway/issues/1884
       * client code (Netty client?) to expose the observed HTTP response code during handshake. Currently exception does not provide status code programmatically (only present in the exception message)
       * server code (WebsocketRoutingFilter) to catch/handle the error instead of letting it flow up to reactor-netty which currently applies default behavior
          * mark WS connection for closing
          * return hardcoded internal error message 
        
   * Workaround
      * Modify shield nginx config to return 401 on the WSS message ?
      * Modify SCG with a shield-specific rule to workaround
         * make an HTTP request before websocket upgrading to check shield authentication status
      * [ ] study shield authentication backends and possible basic auth support 
      * [ ] fix shield JS client code to avoid looping on empty responses and triggering a race condition with `login`
         * submit issue to shield
         * [ ] study how 401 status is currently handled to avoid retry. Try to apply same handling on empty/invalid response. 
      
Server WebSocket handshake errors should result in handshake error returned to client
Handle failures during WebSocket handshake (currently returns 101)
Server WebSocket handshake errors should return client handshake error
Return original status code during server WebSocket handshake errors

* As a spring cloud gateway user
* in order to route traffic to websocket servers that perform authentication at the websocket handshake time (which is a valid behavior according to [WSS specs section-4.2.2](https://tools.ietf.org/html/rfc6455#section-4.2.2) ) and return a 401 status code to clients
* I need to be able handle server WS handshake failure and to return the original (e.g. 401) status to websocket client handshake


Currently, as previously reported in #857 (which was marked as a duplicate for #845), the gateway is first returning the WSS handshake response (`HTTP/1.1 101 Switching Protocols` triggered in ReactorNettyRequestUpgradeStrategy) prior to contacting the server and receiving the 401 status, and finally returns a default `Server internal error` to the client

Traces and steps to reproduce are detailed into https://stackoverflow.com/questions/63196638/spring-cloud-gateway-hides-server-websocket-handshake-401-failures-to-clients

I'm suspecting the following problems/dependencies to fix this issue:
* client code (Netty client?) needs to expose the observed HTTP response code during handshake. Currently, the exception does not provide access to the status code programmatically (it is only present in the exception message).
```
io.netty.handler.codec.http.websocketx.WebSocketHandshakeException: Invalid handshake response getStatus: 401 Unauthorized
	at io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13.verify(WebSocketClientHandshaker13.java:274) ~[netty-codec-http-4.1.51.Final.jar:4.1.51.Final]
``` 
* server code (ReactorNettyRequestUpgradeStrategy in spring-webflux) needs to receive and collect server errors during client handshake and return them to the client prior to committing the websocket handshake 