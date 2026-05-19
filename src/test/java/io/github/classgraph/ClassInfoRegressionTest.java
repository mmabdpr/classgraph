package io.github.classgraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Regression tests for package-private ClassInfo creation paths. */
public class ClassInfoRegressionTest {
    /** Class names sometimes arrive with descriptor terminators. */
    @Test
    public void getOrCreateClassInfoHandlesClassNameWithDescriptorTerminator() {
        final Map<String, ClassInfo> classNameToClassInfo = new HashMap<>();

        final ClassInfo classInfo = ClassInfo.getOrCreateClassInfo("java/lang/String;", classNameToClassInfo);

        assertThat(classInfo.getName()).isEqualTo("java.lang.String");
    }

    /** Array descriptors should create usable ArrayClassInfo instances. */
    @Test
    public void getOrCreateClassInfoHandlesArrayDescriptors() {
        final Map<String, ClassInfo> classNameToClassInfo = new HashMap<>();

        final ArrayClassInfo stringArrayClassInfo = (ArrayClassInfo) ClassInfo
                .getOrCreateClassInfo("[Ljava/lang/String;", classNameToClassInfo);
        assertThat(stringArrayClassInfo.getName()).isEqualTo("java.lang.String[]");
        assertThat(stringArrayClassInfo.getTypeSignatureStr()).isEqualTo("[Ljava/lang/String;");
        assertThat(stringArrayClassInfo.loadClass()).isEqualTo(String[].class);

        final ArrayClassInfo intArrayClassInfo = (ArrayClassInfo) ClassInfo.getOrCreateClassInfo("[[I",
                classNameToClassInfo);
        assertThat(intArrayClassInfo.getName()).isEqualTo("int[][]");
        assertThat(intArrayClassInfo.getTypeSignatureStr()).isEqualTo("[[I");
        assertThat(intArrayClassInfo.loadClass()).isEqualTo(int[][].class);
    }

    /** Detached ClassInfo instances should not require a ScanResult to answer dependencies. */
    @Test
    public void getClassDependenciesWorksWithoutScanResult() {
        final ClassInfo classInfo = new ClassInfo("example.Foo", /* classModifiers = */ 0,
                /* classfileResource = */ null);

        assertThatCode(classInfo::getClassDependencies).doesNotThrowAnyException();
        assertThat(classInfo.getClassDependencies()).isEmpty();
    }
}
