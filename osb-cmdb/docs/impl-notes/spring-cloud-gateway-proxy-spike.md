      
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
|00000140| 6e 74 2d 63 66 61 70 69 2e 69 74 6e 2e 69 6e 74 |nt-cfapi.itn.int|
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
