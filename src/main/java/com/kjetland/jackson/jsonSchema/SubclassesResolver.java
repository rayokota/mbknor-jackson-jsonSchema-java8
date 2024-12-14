package com.kjetland.jackson.jsonSchema;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubclassesResolver {

    private ClassGraph classGraph;
    private ScanResult scanResult;
    
    public SubclassesResolver() {
        this(null);
    }
    
    public SubclassesResolver (List<String> packagesToScan, List<String> classesToScan) {
        this(buildClassGraph(packagesToScan, classesToScan));
    }
    
    public SubclassesResolver (ClassGraph classGraph) {
        if (classGraph == null) {
            log.debug("Entire classpath will be scanned because SubclassesResolver is not configured. See "
                    + "https://github.com/mbknor/mbknor-jackson-jsonSchema#subclass-resolving-using-reflection");
            this.classGraph = new ClassGraph();
        } else {
            this.classGraph = classGraph;
        }
    }

    public ScanResult getScanResult() {
        if (scanResult == null) {
            scanResult = classGraph.enableClassInfo().scan();
        }
        return scanResult;
    }

    public List<Class<?>> getSubclasses(Class<?> clazz) {
        if (clazz.isInterface())
            return getScanResult().getClassesImplementing(clazz.getName()).loadClasses();
        else
            return getScanResult().getSubclasses(clazz.getName()).loadClasses();
    }
    
    static private ClassGraph buildClassGraph(List<String> packagesToScan, List<String> classesToScan) {
        if (packagesToScan == null && classesToScan == null)
            return null;

        ClassGraph classGraph = new ClassGraph();

        if (packagesToScan != null)
            classGraph.whitelistPackages(packagesToScan.toArray(new String[0]));

        if (classesToScan != null)
            classGraph.whitelistClasses(classesToScan.toArray(new String[0]));

        return classGraph;
    }
}
