package grpcreflection.grpc;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class UnaryResultObserver<T extends Message> implements StreamObserver<T> {

   private final CompletableFuture<T> future = new CompletableFuture<>();

   @Override
   public synchronized void onNext(T value) {
      future.complete(value);
   }

   @Override
   public synchronized void onError(Throwable t) {
      future.completeExceptionally(t);
   }

   @Override
   public void onCompleted() {
   }

   public T getResult() {
      try {
         return future.get();
      } catch (InterruptedException | ExecutionException e) {
         throw new RuntimeException(e);
      }
   }
}
