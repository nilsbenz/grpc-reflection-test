package grpcreflection.grpc;

import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link StreamObserver} which groups multiple observers and executes them all.
 */
public class CompositeStreamObserver<T> implements StreamObserver<T> {
   private final List<StreamObserver<T>> observers;

   public static <T> CompositeStreamObserver<T> of(StreamObserver<T>... observers) {
      return new CompositeStreamObserver<T>(Arrays.asList(observers));
   }

   private CompositeStreamObserver(List<StreamObserver<T>> observers) {
      this.observers = observers;
   }

   @Override
   public void onCompleted() {
      for (StreamObserver<T> observer : observers) {
         try {
            observer.onCompleted();
         } catch (Throwable t) {
            System.err.println("Exception in composite onComplete, moving on");
            t.printStackTrace();
         }
      }
   }

   @Override
   public void onError(Throwable t) {
      for (StreamObserver<T> observer : observers) {
         try {
            observer.onError(t);
         } catch (Throwable s) {
            System.err.println("Exception in composite onError, moving on");
            s.printStackTrace();
         }
      }
   }

   @Override
   public void onNext(T value) {
      for (StreamObserver<T> observer : observers) {
         try {
            observer.onNext(value);
         } catch (Throwable t) {
            System.err.println("Exception in composite onNext, moving on");
            t.printStackTrace();
         }
      }
   }
}
