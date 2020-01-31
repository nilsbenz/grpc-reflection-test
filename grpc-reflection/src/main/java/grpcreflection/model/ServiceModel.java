package grpcreflection.model;

import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;

public class ServiceModel {

   private final Map<String, ServiceDescriptor> services;

   public ServiceModel(Map<String, ServiceDescriptor> services) {
      this.services = services;
   }

   public static ServiceModel of(List<FileDescriptor> fileDescriptors) {
      Map<String, ServiceDescriptor> services = new HashMap<>();
      for (FileDescriptor fileDescriptor : fileDescriptors) {
         for (ServiceDescriptor service : fileDescriptor.getServices()) {
            services.put(service.getFullName(), service);
         }
      }
      return new ServiceModel(unmodifiableMap(services));
   }

   public Collection<ServiceDescriptor> getServices() {
      return services.values();
   }

   public ServiceDescriptor getService(String fullyQualifiedName) {
      return services.get(fullyQualifiedName);
   }

   public Collection<MethodDescriptor> getAllMethods() {
      return services.values().stream()
            .flatMap((service) -> service.getMethods().stream())
            .collect(Collectors.toList());
   }

   public MethodDescriptor getMethod(String fullyQualifiedMethodName) {
      String fullyQualifiedServiceName = fullyQualifiedMethodName.substring(0, fullyQualifiedMethodName.lastIndexOf('.'));
      String methodName = fullyQualifiedMethodName.substring(fullyQualifiedMethodName.lastIndexOf('.') + 1);
      return getMethod(fullyQualifiedServiceName, methodName);
   }

   public MethodDescriptor getMethod(String fullyQualifiedServiceName, String methodName) {
      ServiceDescriptor service = getService(fullyQualifiedServiceName);
      if (service == null) {
         return null;
      }
      return service.findMethodByName(methodName);
   }
}
