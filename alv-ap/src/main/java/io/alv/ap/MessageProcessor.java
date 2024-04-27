package io.alv.ap;

import io.alv.core.Encoding;
import io.alv.core.Message;
import io.alv.core.handler.Messages;
import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSetMultimap;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@AutoService({Processor.class})
public class MessageProcessor extends BasicAnnotationProcessor {
  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_17;
  }

  @Override
  protected Iterable<? extends Step> steps() {
    return Set.of(new MessageProcessorStep(processingEnv));
  }

  public static class MessageProcessorStep implements BasicAnnotationProcessor.Step {

    private final ProcessingEnvironment processingEnv;

    public MessageProcessorStep(ProcessingEnvironment processingEnv) {
      this.processingEnv = processingEnv;
    }

    @Override
    public Set<String> annotations() {
      return Set.of(Message.class.getCanonicalName());
    }

    @Override
    public Set<? extends Element> process(ImmutableSetMultimap<String, Element> immutableSetMultimap) {
      Map<String, Encoding> registerMap = new HashMap<>();

      for (Element element : immutableSetMultimap.get(Message.class.getCanonicalName())) {
        if (element.getKind() == ElementKind.RECORD) {
          TypeElement typeElement = (TypeElement) element;
          Message model = typeElement.getAnnotation(Message.class);
          registerMap.put(typeElement.getQualifiedName().toString(), model.value());
        }
      }

      generateRegisterModelsImplementation(registerMap);

      return Set.of();
    }

    private void generateRegisterModelsImplementation(Map<String, Encoding> registerMap) {
      // Create the 'models' method
      MethodSpec.Builder modelsMethodBuilder = MethodSpec.methodBuilder("messages")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(
          ParameterizedTypeName.get(
            ClassName.get(Map.class),
            ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)),
            ClassName.get(Encoding.class)
          )
        )
        .addStatement("$T<$T<?>, $T> map = new $T<>()",
          Map.class,
          ClassName.get(Class.class),
          Encoding.class,
          HashMap.class);

      for (Map.Entry<String, Encoding> entry : registerMap.entrySet()) {
        modelsMethodBuilder.addStatement("map.put($T.class, $T.$L)", ClassName.bestGuess(entry.getKey()), Encoding.class, entry.getValue().name());
      }

      modelsMethodBuilder.addStatement("return $T.unmodifiableMap(map)", Collections.class);
      MethodSpec modelsMethod = modelsMethodBuilder.build();

      // Create the class
      TypeSpec registerModelsImpl = TypeSpec.classBuilder("MessagesImpl")
        .addAnnotation(AnnotationSpec.builder(AutoService.class)
          .addMember("value", "$T.class", Messages.class)
          .build()
        )
        .addSuperinterface(Messages.class)
        .addModifiers(Modifier.PUBLIC)
        .addMethod(modelsMethod)
        .build();

      // Write the file
      JavaFile javaFile = JavaFile.builder("io.alv.core.handler", registerModelsImpl)
        .build();

      try {
        javaFile.writeTo(processingEnv.getFiler());
      } catch (IOException e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate RegisterModels implementation: " + e.getMessage());
      }
    }
  }
}
