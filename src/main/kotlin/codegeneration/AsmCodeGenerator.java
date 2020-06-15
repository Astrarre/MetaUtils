package codegeneration;

import api.JavaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import signature.GenericReturnType;
import signature.GenericTypeOrPrimitive;
import signature.TypeArgumentDeclaration;
import util.PackageName;

import java.nio.file.Path;
import java.util.List;

public class AsmCodeGenerator implements CodeGenerator{
    @Override
    public void writeClass(@NotNull ClassInfo info, @Nullable PackageName packageName, @NotNull Path writeTo) {
        info.getBody().invoke(new Class());
    }

    private static class Class implements GeneratedClass {

        @Override
        public void addMethod(@NotNull MethodInfo methodInfo, boolean isStatic, boolean isFinal, boolean isAbstract, @NotNull List<TypeArgumentDeclaration> typeArguments, @NotNull String name, @Nullable JavaType<? extends GenericReturnType> returnType) {
            methodInfo.getBody().invoke(new Method());
        }

        @Override
        public void addConstructor(@NotNull MethodInfo info) {
            info.getBody().invoke(new Method());
        }

        @Override
        public void addInnerClass(@NotNull ClassInfo info, boolean isStatic) {

        }

        @Override
        public void addField(@NotNull String name, @NotNull JavaType<? extends GenericTypeOrPrimitive> type, @NotNull Visibility visibility, boolean b, boolean b2, @Nullable Expression initializer) {

        }
    }

    private static class Method implements GeneratedMethod {

        @Override
        public void addStatement(@NotNull Statement statement) {

        }

        @Override
        public void addComment(@NotNull String comment) {

        }
    }
}


