package dev.themeinerlp.faweschematiccloud

import com.intellectualsites.arkitektonika.Arkitektonika
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutionException

class ArkitektonikaContractTest {
    @Test
    fun `client uses schematic multipart field and parses API keys`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val executor = Executors.newSingleThreadExecutor()
        val file = Files.createTempFile("contract-", ".schem")
        val requests = java.util.concurrent.LinkedBlockingQueue<String>()
        val status = java.util.concurrent.atomic.AtomicInteger(200)
        server.createContext("/upload") { exchange ->
            requests.add(exchange.requestMethod + "\n" + exchange.requestBody.readBytes().toString(Charsets.ISO_8859_1))
            val body = if (status.get() == 200) """{"download_key":"0123456789abcdef0123456789abcdef","delete_key":"abcdef0123456789abcdef0123456789"}""" else """{"error":"too large"}"""
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status.get(), body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.start()
        try {
            Files.writeString(file, "test-payload")
            val client = Arkitektonika.builder().withUrl("http://127.0.0.1:${server.address.port}")
                .withExecutorService(executor).build()
            val keys = client.upload(file.toFile()).get(10, TimeUnit.SECONDS)
            assertEquals("0123456789abcdef0123456789abcdef", keys.accessKey)
            assertEquals("abcdef0123456789abcdef0123456789", keys.deletionKey)
            val request = requests.poll(1, TimeUnit.SECONDS)
            assertTrue(request.startsWith("POST\n"))
            assertTrue(request.contains("name=\"schematic\""))
            assertTrue(request.contains("test-payload"))
            status.set(413)
            assertThrows(ExecutionException::class.java) { client.upload(file.toFile()).get(10, TimeUnit.SECONDS) }
        } finally {
            server.stop(0)
            executor.shutdownNow()
            Files.deleteIfExists(file)
        }
    }
}
