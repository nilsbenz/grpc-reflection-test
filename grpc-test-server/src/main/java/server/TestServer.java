package server;

import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import server.echo.EchoServiceImpl;

public class TestServer {

   static public void main(String[] args) throws Exception {
      HealthStatusManager healthStatusManager = new HealthStatusManager();

      io.grpc.Server server = ServerBuilder
            .forPort(8080)
            .addService(new EchoServiceImpl())
            .addService(healthStatusManager.getHealthService())
            .addService(ProtoReflectionService.newInstance())
            .build();

      System.out.println("Starting server...");
      server.start();
      System.out.println("Server started!");
      server.awaitTermination();
   }

}
