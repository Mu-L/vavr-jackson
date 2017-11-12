package generator;

import com.squareup.javapoet.*;
import io.vavr.Tuple;
import io.vavr.collection.PriorityQueue;
import org.junit.Assert;
import org.junit.Test;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;

import static generator.utils.Initializer.initMapper;
import static generator.utils.Initializer.initValue;
import static generator.utils.Serializer.expectedJson;

/**
 * @author <a href="mailto:ruslan.sennov@gmail.com">Ruslan Sennov</a>
 */
public class SimplePojo {

    public static void main(String[] args) throws IOException {
        java.util.Map<String, Object> cases = new java.util.HashMap<>();
        cases.put("PriorityQueueOfString", PriorityQueue.of("A", "B"));
        generate(cases);
    }

    static void generate(java.util.Map<String, Object> cases) throws IOException {

        TypeSpec.Builder pojoTest = TypeSpec.classBuilder("SimplePojoTest")
                .addJavadoc("generated\n")
                .addModifiers(Modifier.PUBLIC);
        initMapper(pojoTest, "MAPPER");

        cases.forEach((k, v) -> addCase(pojoTest, k, v));

        JavaFile javaFile = JavaFile.builder("io.vavr.jackson.generated", pojoTest.build())
                .indent("    ")
                .build();

        javaFile.writeTo(new File("src/test/java"));
    }

    private static void addCase(TypeSpec.Builder builder, String pojoName, Object value) {
        addCase(builder, pojoName, value, 0);
    }

    private static void addCase(TypeSpec.Builder builder, String pojoName, Object value, int opts) {

        MethodSpec.Builder testBuilder = MethodSpec.methodBuilder("test" + pojoName)
                .addAnnotation(Test.class)
                .addModifiers(Modifier.PUBLIC)
                .addException(ClassName.get(Exception.class));
        TypeName valueTypeName = initValue(testBuilder, "src", value);
        MethodSpec testSpec = testBuilder
                .addStatement("$T json = MAPPER.writeValueAsString(new $L().setValue(src))", ClassName.get(String.class), pojoName)
                .addStatement("$T.assertEquals(json, $S)", ClassName.get(Assert.class), "{\"value\":"+ expectedJson(value, opts) + "}")
                .addStatement("$L restored = MAPPER.readValue(json, $L.class)", pojoName, pojoName)
                .addStatement("$T.assertEquals(src, restored.getValue())", ClassName.get(Assert.class))
                .build();
        builder.addMethod(testSpec);

        TypeSpec pojoSpec = TypeSpec.classBuilder(pojoName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addField(FieldSpec.builder(valueTypeName, "v", Modifier.PRIVATE).build())
                .addMethod(MethodSpec.methodBuilder("getValue")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(valueTypeName)
                        .addStatement("return v")
                        .build())
                .addMethod(MethodSpec.methodBuilder("setValue")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ClassName.get("", pojoName))
                        .addParameter(ParameterSpec.builder(valueTypeName, "v").build())
                        .addStatement("this.v = v")
                        .addStatement("return this")
                        .build())
                .build();

        builder.addType(pojoSpec);
    }
}
