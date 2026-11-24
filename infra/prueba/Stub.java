import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class Stub {
    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 8080;
        for (String a : args) {
            if (a.startsWith("--server.address=")) host = a.substring(17);
            if (a.startsWith("--server.port=")) port = Integer.parseInt(a.substring(14));
        }
        HttpServer s = HttpServer.create(new InetSocketAddress(host, port), 0);
        s.createContext("/", ex -> {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            boolean salud = ex.getRequestURI().getPath().equals("/api/salud");
            byte[] body = (salud ? "" : "{\"error\":\"backend de prueba\"}").getBytes();
            if (!salud) ex.getResponseHeaders().add("Content-Type", "application/json");
            boolean head = ex.getRequestMethod().equals("HEAD");
            ex.sendResponseHeaders(salud ? 200 : 404, head || body.length == 0 ? -1 : body.length);
            if (!head && body.length > 0) ex.getResponseBody().write(body);
            ex.close();
        });
        s.start();
        System.out.println("Backend de prueba escuchando en " + host + ":" + port);
    }
}
