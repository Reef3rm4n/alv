package io.alv.ap;

import io.alv.core.Handler;
import io.alv.core.MessageHandler;
import io.alv.core.handler.Handlers;
import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSetMultimap;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@AutoService(Processor.class)
public class HandlerProcessor extends BasicAnnotationProcessor {
  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  protected Iterable<? extends Step> steps() {
    return Set.of(new HandlerProcessorStep(processingEnv));
  }

  private static class HandlerProcessorStep implements BasicAnnotationProcessor.Step {
    private final ProcessingEnvironment processingEnv;

    HandlerProcessorStep(ProcessingEnvironment processingEnv) {
      this.processingEnv = processingEnv;
    }

    @Override
    public Set<String> annotations() {
      return Set.of(Handler.class.getCanonicalName());
    }

    @Override
    public Set<? extends Element> process(ImmutableSetMultimap<String, Element> immutableSetMultimap) {
      Set<Element> elements = immutableSetMultimap.get(Handler.class.getCanonicalName());

      // Create the 'handlers' method
      MethodSpec.Builder handlersMethodBuilder = MethodSpec.methodBuilder("handlers")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(
            ClassName.get(Set.class),
          ParameterizedTypeName.get(ClassName.get(MessageHandler.class), WildcardTypeName.subtypeOf(Object.class))
          )
        )
        .addStatement("$T<$T<?>> set = new $T<>()", Set.class, MessageHandler.class, HashSet.class);

      for (Element element : elements) {
        handlersMethodBuilder.addStatement("set.add(new $T())", ClassName.get((TypeElement) element));
      }

      handlersMethodBuilder.addStatement("return $T.unmodifiableSet(set)", Collections.class);
      MethodSpec handlersMethod = handlersMethodBuilder.build();

      // Create the class
      TypeSpec handlerModelsImpl = TypeSpec.classBuilder("HandlersImpl")
        .addSuperinterface(Handlers.class)
        .addAnnotation(AnnotationSpec.builder(AutoService.class)
          .addMember("value", "$T.class", Handlers.class)
          .build()
        )
        .addModifiers(Modifier.PUBLIC)
        .addMethod(handlersMethod)
        .build();

      // Write the file
      JavaFile javaFile = JavaFile.builder("io.alv.core.handler", handlerModelsImpl)
        .build();

      try {
        javaFile.writeTo(processingEnv.getFiler());
      } catch (IOException e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate HandlerModels implementation: " + e.getMessage());
      }

      return Set.of();
    }
  }
}
