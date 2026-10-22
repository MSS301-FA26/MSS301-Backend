package com.cinemaai.catalog.security;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

final class GatewayHttpHarness implements AutoCloseable {
    private final Process process;
    private final Path workDir;
    private final int port;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    GatewayHttpHarness(int upstreamPort) throws Exception {
        try (ServerSocket socket = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) { port = socket.getLocalPort(); }
        workDir = Files.createTempDirectory("gateway-http-test-");
        Path root = Path.of("..").toAbsolutePath().normalize();
        Path buildDir = workDir.resolve("gateway-build");
        boolean windows = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win");
        var command = new java.util.ArrayList<String>();
        if (windows) { command.add("cmd.exe"); command.add("/c"); }
        command.add(root.resolve(windows ? "mvnw.cmd" : "mvnw").toString());
        command.add("-f");
        command.add(root.resolve("api-gateway/pom.xml").toString());
        command.add("package");
        command.add("-Dmaven.test.skip=true");
        command.add("-Dgateway.test.build.directory=" + buildDir);
        Path buildLog = workDir.resolve("build.log");
        Process build = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true)
                .redirectOutput(buildLog.toFile()).start();
        if (!build.waitFor(180, java.util.concurrent.TimeUnit.SECONDS)) {
            build.destroyForcibly().waitFor();
            throw new IllegalStateException("Gateway build timed out; log: " + buildLog);
        }
        if (build.exitValue() != 0) throw new IllegalStateException("Gateway build failed: " + Files.readString(buildLog));
        Path jar = buildDir.resolve("api-gateway-1.0.0-SNAPSHOT.jar");
        Path config = workDir.resolve("gateway.properties");
        Files.writeString(config, "spring.application.name=gateway-http-test\nserver.address=127.0.0.1\nserver.port=" + port + "\n" +
                "spring.cloud.gateway.routes[0].id=downstream\nspring.cloud.gateway.routes[0].uri=http://127.0.0.1:" + upstreamPort + "\n" +
                "spring.cloud.gateway.routes[0].predicates[0]=Path=/**\n" +
                "INTERNAL_GATEWAY_SECRET=gateway-test\nAI_GATEWAY_SECRET=ai-test\nmanagement.endpoints.web.exposure.include=health\n");
        process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar", jar.toString(),
                "--spring.config.location=" + config.toUri()).directory(workDir.toFile()).redirectErrorStream(true)
                .redirectOutput(workDir.resolve("gateway.log").toFile()).start();
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) throw new IllegalStateException("Gateway exited: " + Files.readString(workDir.resolve("gateway.log")));
            try { if (send("/actuator/health", Map.of()).statusCode() == 200) return; } catch (Exception ignored) { }
            Thread.sleep(250);
        }
        process.destroyForcibly();
        throw new IllegalStateException("Gateway startup timed out: " + Files.readString(workDir.resolve("gateway.log")));
    }
    HttpResponse<String> send(String path, Map<String,String> headers) throws Exception { return sendTo(port, path, headers); }
    HttpResponse<String> sendTo(int target, String path, Map<String,String> headers) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + target + path)).timeout(Duration.ofSeconds(10)).GET();
        headers.forEach(request::header);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Override public void close() throws Exception {
        process.destroy(); if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly().waitFor();
        try {
            try (var paths = Files.walk(workDir)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        } catch (java.io.IOException locked) {
            workDir.toFile().deleteOnExit();
        }
    }
}
