package grpcreflection.reflection;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.reflection.v1alpha.FileDescriptorResponse;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc.ServerReflectionStub;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServiceResponse;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadFileDescriptorsFromServer {

   public static List<FileDescriptor> loadAll(ServerReflectionStub stub) throws DescriptorValidationException {
      ResponseObserver responseObserver = new ResponseObserver();
      responseObserver.requestStreamObserver = stub.serverReflectionInfo(responseObserver);

      ServerReflectionRequest initialRequest = ServerReflectionRequest.newBuilder().setListServices("").build();
      responseObserver.requestStreamObserver.onNext(initialRequest);
      responseObserver.waitForCompletion();

      List<FileDescriptor> fileDescriptors = createFileDescriptors(responseObserver.fileDescriptorProtos);
      return fileDescriptors;
   }

   private static List<FileDescriptor> createFileDescriptors(Map<String, FileDescriptorProto> protoMap) throws DescriptorValidationException {
      Map<String, FileDescriptor> descriptorCache = new HashMap<>();
      List<FileDescriptor> result = new ArrayList<>();
      for (FileDescriptorProto proto : protoMap.values()) {
         var descriptor = descriptorFromProto(proto, protoMap, descriptorCache);
         result.add(descriptor);
      }
      return result;
   }

   private static FileDescriptor descriptorFromProto(
         FileDescriptorProto descriptorProto,
         Map<String, FileDescriptorProto> protoMap,
         Map<String, FileDescriptor> descriptorCache) throws DescriptorValidationException {
      if (descriptorCache.containsKey(descriptorProto.getName())) {
         return descriptorCache.get(descriptorProto.getName());
      }

      // fetch all the required dependencies recursively.
      List<FileDescriptor> dependencies = new ArrayList<>();
      for (String dependencyName : descriptorProto.getDependencyList()) {
         FileDescriptorProto dependencyProto = protoMap.get(dependencyName);
         if (dependencyProto == null) {
            System.err.println("Missing dependency: " + dependencyName);
         }
         dependencies.add(descriptorFromProto(dependencyProto, protoMap, descriptorCache));
      }

      return FileDescriptor.buildFrom(descriptorProto, dependencies.toArray(new FileDescriptor[dependencies.size()]));
   }

   private static class ResponseObserver implements StreamObserver<ServerReflectionResponse> {

      private final CountDownLatch waitForCompletionLatch = new CountDownLatch(1);
      private final AtomicInteger expectedFileDescriptors = new AtomicInteger(0);
      private final Map<String, FileDescriptorProto> fileDescriptorProtos = new ConcurrentHashMap<>();
      StreamObserver<ServerReflectionRequest> requestStreamObserver;

      @Override
      public void onNext(ServerReflectionResponse value) {
         if (value.hasListServicesResponse()) {
            // for each service, call again to get proto
            value.getListServicesResponse().getServiceList().stream()
                  .map(ServiceResponse::getName)
                  .map((name) -> ServerReflectionRequest.newBuilder().setFileContainingSymbol(name).build())
                  .forEach((request) -> {
                     expectedFileDescriptors.addAndGet(1);
                     requestStreamObserver.onNext(request);
                  });
         } else if (value.hasFileDescriptorResponse()) {
            List<FileDescriptorProto> protoList = collectFileDescriptors(value.getFileDescriptorResponse());
            protoList.forEach((file) -> fileDescriptorProtos.put(file.getName(), file));

            // for each dependency, check if already present or else fetch
            // TODO: check if/when this happens
            for (FileDescriptorProto fileDescriptorProto : protoList) {
               for (String dependency : fileDescriptorProto.getDependencyList()) {
                  if (! fileDescriptorProtos.containsKey(dependency)) {
                     System.out.println("dependency missing: " + dependency);
                     var request = ServerReflectionRequest.newBuilder().setFileByFilename(dependency).build();
                     expectedFileDescriptors.addAndGet(1);
                     requestStreamObserver.onNext(request);
                  }
               }
            }

            // complete, if the last response arrived
            int remaining = expectedFileDescriptors.decrementAndGet();
            if (remaining == 0) {
               requestStreamObserver.onCompleted();
            }
         }
      }

      @Override
      public void onError(Throwable t) {
         t.printStackTrace();
      }

      @Override
      public void onCompleted() {
         waitForCompletionLatch.countDown();
      }

      private static List<FileDescriptorProto> collectFileDescriptors(FileDescriptorResponse response) {
         List<FileDescriptorProto> result = new ArrayList<>();
         for (ByteString bytes : response.getFileDescriptorProtoList()) {
            try {
               result.add(FileDescriptorProto.parseFrom(bytes.toByteArray()));
            } catch (InvalidProtocolBufferException e) {
               e.printStackTrace();
            }
         }
         return result;
      }

      void waitForCompletion() {
         try {
            waitForCompletionLatch.await();
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }
}
