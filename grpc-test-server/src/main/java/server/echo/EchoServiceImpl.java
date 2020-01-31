package server.echo;

import echo.EchoRequest;
import echo.EchoResponse;
import echo.EchoServiceGrpc;
import echo.Trace;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {

   private final ManagedChannel channelToDelegate;

   public EchoServiceImpl() {
      String delegate = System.getenv("DELEGATE_ADDRESS");
      if (delegate != null) {
         System.out.println("Using delegate: " + delegate);
         this.channelToDelegate = ManagedChannelBuilder.forTarget(delegate).usePlaintext().build();
      } else {
         System.out.println("No delegate configured...");
         this.channelToDelegate = null;
      }
   }

   @Override
   public void echo(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
      System.out.println("Received: " + request.getMessage());

      EchoResponse.Builder response = EchoResponse.newBuilder()
            .setMessage(request.getMessage())
            .addTraces(Trace.newBuilder().setHost(getHostname()).build())
      ;

      if (channelToDelegate != null) {
         EchoResponse delegateResponse = delegate(request);
         response.addAllTraces(delegateResponse.getTracesList());
      }

      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
   }

   private EchoResponse delegate(EchoRequest request) {
      EchoServiceGrpc.EchoServiceBlockingStub stub = EchoServiceGrpc.newBlockingStub(channelToDelegate);
      return stub.echo(request);
   }

   private String getHostname() {
      try {
         return InetAddress.getLocalHost().getHostName() + "(" + InetAddress.getLocalHost().getHostAddress() + ")";
      } catch (UnknownHostException e) {
         return "<Unknown hostname>";
      }
   }
}
