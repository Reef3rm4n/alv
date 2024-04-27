package io.alv.ap;

import io.alv.core.Broadcast;
import io.alv.core.MessageHandler;
import io.alv.core.Reply;
import io.alv.core.handler.messages.handles.BroadcastSubscription;
import io.alv.core.handler.messages.handles.MessageOffer;
import io.alv.core.handler.messages.output.Event;
import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSetMultimap;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

@AutoService({Processor.class})
public class ClusterClientGen extends BasicAnnotationProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_17;
  }

  @Override
  protected Iterable<? extends BasicAnnotationProcessor.Step> steps() {
    return Set.of(new ClientGentStep(processingEnv));
  }

  public static class ClientGentStep implements BasicAnnotationProcessor.Step {

    private final ProcessingEnvironment processingEnv;

    public ClientGentStep(ProcessingEnvironment processingEnv) {
      this.processingEnv = processingEnv;
    }

    @Override
    public Set<String> annotations() {
      return Set.of(Reply.class.getCanonicalName(), Broadcast.class.getCanonicalName());
    }

    @Override
    public Set<? extends Element> process(ImmutableSetMultimap<String, Element> immutableSetMultimap) {
      generateClients(immutableSetMultimap);
      generateBroadcastSubscription(immutableSetMultimap);
      return Set.of();
    }

    private void addBroadcastMethod(TypeElement typeElement, TypeSpec.Builder classBuilder) {
      // Get the @Broadcast annotation from the typeElement
      Broadcast broadcastAnnotation = typeElement.getAnnotation(Broadcast.class);
      if (broadcastAnnotation != null) {
        // For each event in the @Broadcast annotation, generate an abstract method
        for (AnnotationMirror mirror : typeElement.getAnnotationMirrors()) {
          if (mirror.getAnnotationType().toString().equals(Broadcast.class.getCanonicalName())) {
            Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = mirror.getElementValues();
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
              if (entry.getKey().getSimpleName().toString().equals("value")) {
                @SuppressWarnings("unchecked")
                List<AnnotationValue> values = (List<AnnotationValue>) entry.getValue().getValue();
                for (AnnotationValue value : values) {
                  DeclaredType declaredType = (DeclaredType) value.getValue();
                  TypeElement eventClass = (TypeElement) declaredType.asElement();
                  MethodSpec onEventTypeMethod = MethodSpec.methodBuilder("on" + eventClass.getSimpleName())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(ClassName.get(eventClass.asType()), "event")
                    .build();
                  classBuilder.addMethod(onEventTypeMethod);
                }
              }
            }
          }
        }
      }
    }

    private void generateBroadcastSubscription(ImmutableSetMultimap<String, Element> immutableSetMultimap) {
      if (immutableSetMultimap.containsKey(Broadcast.class.getCanonicalName())) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("BroadcastSubscriptionImpl")
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .addSuperinterface(ClassName.get(BroadcastSubscription.class));
        String packageName = null;
        for (Element element : immutableSetMultimap.get(Broadcast.class.getCanonicalName())) {
          if (element instanceof TypeElement typeElement) {
            if (isSubtypeOfMessageHandler(typeElement)) {
              try {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating message offer for " + typeElement);
                packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
                addBroadcastMethod(typeElement, classBuilder);
              } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), typeElement);
              }
            } else {
              processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation @Broadcast can only be used on implementations of MessageHandler", typeElement);
            }
          }
        }

        // Generate the switch statement for the onEvent method
        StringBuilder switchStatement = new StringBuilder("switch (message.getClass().getSimpleName()) {\n");
        for (Element element : immutableSetMultimap.get(Broadcast.class.getCanonicalName())) {
          if (element instanceof TypeElement typeElement) {
            for (AnnotationMirror mirror : typeElement.getAnnotationMirrors()) {
              if (mirror.getAnnotationType().toString().equals(Broadcast.class.getCanonicalName())) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = mirror.getElementValues();
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                  if (entry.getKey().getSimpleName().toString().equals("value")) {
                    @SuppressWarnings("unchecked")
                    List<AnnotationValue> values = (List<AnnotationValue>) entry.getValue().getValue();
                    for (AnnotationValue value : values) {
                      DeclaredType declaredType = (DeclaredType) value.getValue();
                      TypeElement eventClass = (TypeElement) declaredType.asElement();
                      switchStatement.append("  case \"")
                        .append(eventClass.getSimpleName())
                        .append("\":\n    on")
                        .append(eventClass.getSimpleName())
                        .append("((")
                        .append(eventClass.getQualifiedName())
                        .append(") message);\n    break;\n");
                    }
                  }
                }
              }
            }
          }
        }
        switchStatement.append("}");

        // Implement the onEvent() method with the generated switch statement
        MethodSpec onEventMethod = MethodSpec.methodBuilder("onEvent")
          .addModifiers(Modifier.PUBLIC)
          .addParameter(Event.class, "event")
          .addStatement("Object message = io.alv.core.handler.messages.encoding.MessageEnvelopeCodec.deserialize(event.payload());\n" +
            switchStatement)
          .build();
        classBuilder.addMethod(onEventMethod);

        TypeSpec newClass = classBuilder.build();

        JavaFile javaFile = JavaFile.builder(packageName, newClass)
          .build();
        try {
          javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    private void generateClients(ImmutableSetMultimap<String, Element> immutableSetMultimap) {
      for (Element element : immutableSetMultimap.get(Reply.class.getCanonicalName())) {
        if (element instanceof TypeElement typeElement) {
          if (isSubtypeOfMessageHandler(typeElement)) {
            try {
              processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating message offer for " + typeElement);
              generateClass(typeElement);
            } catch (IOException e) {
              processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), typeElement);
            }
          } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation @Reply can only be used on implementations of MessageHandler", typeElement);
          }
        }
      }
    }

    private boolean isSubtypeOfMessageHandler(TypeElement typeElement) {
      Types typeUtils = processingEnv.getTypeUtils();
      TypeMirror messageHandlerType = processingEnv.getElementUtils().getTypeElement(MessageHandler.class.getName()).asType();

      // Get the raw types of MessageHandler and AddIdHandler
      TypeMirror rawMessageHandlerType = typeUtils.erasure(messageHandlerType);
      TypeMirror rawTypeElementType = typeUtils.erasure(typeElement.asType());

      boolean isSubtype = typeUtils.isAssignable(rawTypeElementType, rawMessageHandlerType);

      // Add debug output
      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Checking if " + typeElement + " is a subtype of MessageHandler: " + isSubtype);

      return isSubtype;
    }

    private void generateClass(TypeElement typeElement) throws IOException {
      String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
      final var messageClass = getFirstGenericType(typeElement);
      ClassName messageClassName = ClassName.bestGuess(messageClass.toString());
      TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getSimpleName(messageClass) + "Offer")
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(MessageOffer.class), messageClassName));

// Implement the message() method
      MethodSpec messageMethod = MethodSpec.methodBuilder("message")
        .addModifiers(Modifier.PUBLIC)
        .returns(messageClassName)
        .addStatement("return this.message")
        .build();
      classBuilder.addMethod(messageMethod);

      MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(messageClassName, "message")
        .addStatement("this.message = message")
        .build();
      classBuilder.addField(messageClassName, "message", Modifier.PRIVATE);
      classBuilder.addMethod(constructor);
      generateOnEvent(typeElement, classBuilder);
      TypeSpec newClass = classBuilder.build();

      JavaFile javaFile = JavaFile.builder(packageName, newClass)
        .build();

      javaFile.writeTo(processingEnv.getFiler());
    }

    private void generateOnEvent(TypeElement typeElement, TypeSpec.Builder classBuilder) {
      // Generate onEventType methods and add cases to the switch statement in onEvent()
      StringBuilder switchStatement = new StringBuilder("switch (message.getClass().getSimpleName()) {\n");
      List<? extends AnnotationMirror> annotationMirrors = typeElement.getAnnotationMirrors();
      for (AnnotationMirror annotationMirror : annotationMirrors) {
        if (annotationMirror.getAnnotationType().toString().equals(Reply.class.getCanonicalName())) {
          Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
          for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals("value")) {
              @SuppressWarnings("unchecked")
              List<AnnotationValue> values = (List<AnnotationValue>) entry.getValue().getValue();
              for (AnnotationValue value : values) {
                DeclaredType classTypeMirror = (DeclaredType) value.getValue();
                TypeElement outputTypeElement = (TypeElement) classTypeMirror.asElement();

                // Generate onEventType methods
                MethodSpec onEventTypeMethod = MethodSpec.methodBuilder("on" + outputTypeElement.getSimpleName())
                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                  .addParameter(ClassName.get(outputTypeElement.asType()), "event")
                  .build();
                classBuilder.addMethod(onEventTypeMethod);

                // Add case to the switch statement
                switchStatement.append("  case \"")
                  .append(outputTypeElement.getSimpleName())
                  .append("\":\n    on")
                  .append(outputTypeElement.getSimpleName())
                  .append("((")
                  .append(outputTypeElement.getQualifiedName())
                  .append(") message);\n    break;\n");
              }
            }
          }
        }
      }
      switchStatement.append("}");

      // Implement the onEvent() method with the generated switch statement
      MethodSpec onEventMethod = MethodSpec.methodBuilder("onEvent")
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(ClassName.get(Consumer.class), ClassName.get(Event.class)))
        .addStatement("return event -> {\n" +
          "  Object message = io.alv.core.handler.messages.encoding.MessageEnvelopeCodec.deserialize(event.payload());\n" +
          switchStatement +
          "}")
        .build();
      classBuilder.addMethod(onEventMethod);
    }

    private TypeMirror getFirstGenericType(TypeElement typeElement) {
      Types typeUtils = processingEnv.getTypeUtils();
      TypeMirror messageHandlerType = processingEnv.getElementUtils().getTypeElement(MessageHandler.class.getCanonicalName()).asType();

      for (TypeMirror superType : typeUtils.directSupertypes(typeElement.asType())) {
        if (typeUtils.isSameType(typeUtils.erasure(superType), typeUtils.erasure(messageHandlerType)) && superType instanceof DeclaredType) {
          DeclaredType declaredType = (DeclaredType) superType;
          List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
          if (!typeArguments.isEmpty()) {
            return typeArguments.get(0);
          }
        }
      }
      throw new IllegalArgumentException("Could not find a generic type for " + typeElement);
    }

    private String getSimpleName(TypeMirror typeMirror) {
      String fullName = typeMirror.toString();
      int lastDotIndex = fullName.lastIndexOf('.');
      return (lastDotIndex != -1) ? fullName.substring(lastDotIndex + 1) : fullName;
    }
  }
}
