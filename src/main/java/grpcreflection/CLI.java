package grpcreflection;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import grpcreflection.grpc.DynamicGrpcClient;
import grpcreflection.model.ServiceModel;
import grpcreflection.reflection.LoadFileDescriptorsFromServer;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc.ServerReflectionStub;

import java.util.List;

public class CLI {

   private final ManagedChannel channel;
   private final ServiceModel serviceModel;

   public static void main(String[] args) {
      final ManagedChannel channel = ManagedChannelBuilder
            .forTarget("localhost:7070")
            .usePlaintext()
            .build();

      CLI cli = CLI.initialize(channel);

      System.out.println("===================================");
      cli.listAll();
      System.out.println("===================================");
      cli.healthCheck();
      System.out.println("===================================");
      cli.getPossibleProducts();

      channel.shutdown();
   }

   private void healthCheck() {
      MethodDescriptor method = serviceModel.getMethod("grpc.health.v1.Health.Check");
      String result = invoke(method, "{}");
      System.out.println(result);
   }

   private void getPossibleProducts() {
      MethodDescriptor method = serviceModel.getMethod("product.productmgmt.product.v1.ProductService.GetPossibleProducts");
      String jsonRequest = "{ \"dueDate\": { \"year\": \"2000\", \"month\": \"01\", \"day\": \"01\" } }";
      String result = invoke(method, jsonRequest);
      System.out.println(result);
   }

   private String invoke(MethodDescriptor method, String jsonRequest) {
      try {
         DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(method.getInputType());
         JsonFormat.parser().merge(jsonRequest, messageBuilder);
         DynamicMessage message = messageBuilder.build();
         DynamicGrpcClient client = DynamicGrpcClient.create(method, channel);
         DynamicMessage result = client.callUnaryBlocking(message, CallOptions.DEFAULT);
         return JsonFormat.printer().print(result);
      } catch (InvalidProtocolBufferException e) {
         throw new RuntimeException(e);
      }
   }

   private void listAll() {
      serviceModel.getAllMethods().forEach((m) -> {
         System.out.println(m.getFullName() + " (" + m.getInputType().getFullName() + ") : " + m.getOutputType().getFullName());
      });
   }

   private static CLI initialize(ManagedChannel channel) {
      try {
         ServerReflectionStub stub = ServerReflectionGrpc.newStub(channel);
         List<FileDescriptor> fileDescriptors = LoadFileDescriptorsFromServer.loadAll(stub);
         ServiceModel serviceModel = ServiceModel.of(fileDescriptors);
         return new CLI(channel, serviceModel);
      } catch (Descriptors.DescriptorValidationException e) {
         throw new RuntimeException(e);
      }
   }

   private CLI(ManagedChannel channel, ServiceModel serviceModel) {
      this.channel = channel;
      this.serviceModel = serviceModel;
   }
}
