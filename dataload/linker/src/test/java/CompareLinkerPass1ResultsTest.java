
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CompareLinkerPass1ResultsTest {

    static LinkerPass1.LinkerPass1Result pass1Result;

    static {
        try {
            System.out.println("filePath: "+resolveConfigFile(System.getProperty("filePath")));
            pass1Result = LinkerPass1.run(resolveConfigFile(System.getProperty("filePath")).toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static LinkerPass1FromService.LinkerPass1Result pass1ResultFromService;

    static {
        try {
            System.out.println("serviceUrl: "+System.getProperty("serviceUrl"));
            pass1ResultFromService = LinkerPass1FromService.run(System.getProperty("serviceUrl"),20);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void compareLinkerPass1ResultsDefinitions() throws IOException {
        Set<Map.Entry<String, EntityDefinitionSet>> defs1 = pass1Result.iriToDefinitions.entrySet();
        Set<Map.Entry<String, EntityDefinitionSet>> defs2 = pass1ResultFromService.iriToDefinitions.entrySet();
        int count=0;
        for (Map.Entry<String, EntityDefinitionSet> def1 : defs1){
            for (Map.Entry<String, EntityDefinitionSet> def2 : defs2){
                if(def1.getKey().equals(def2.getKey())){
                    EntityDefinitionSet i1 = def1.getValue();
                    EntityDefinitionSet i2 = def2.getValue();
                    count+=1;
                    System.out.println("Different definition fields: "+ReflectionDiffUtil.diffObjects(i1.definitions,i2.definitions));
                    System.out.println("count: "+count);
                    assertEquals(i1.definitions,i2.definitions,"definitions1: "+i1.definitions+" and definitions2: "+i2.definitions);
                    assertEquals(i1.definingDefinitions,i2.definingDefinitions,"definingDefinitions1: "+i1.definingDefinitions+" and definingDefinitions2: "+i2.definingDefinitions);
                    assertEquals(i1.definingOntologyIris,i2.definingOntologyIris,"definingOntologyIris1: "+i1.definingOntologyIris+" and definingOntologyIris2: "+i2.definingOntologyIris);
                    assertEquals(i1.definingOntologyIds,i2.definingOntologyIds,"definingOntologyIds1: "+i1.definingOntologyIds+" and definingOntologyIds2: "+i2.definingOntologyIds);
                    assertEquals(i1.ontologyIdToDefinitions,i2.ontologyIdToDefinitions,"ontologyIdToDefinitions1: "+i1.ontologyIdToDefinitions+" and ontologyIdToDefinitions2: "+i2.ontologyIdToDefinitions);
                }
            }
        }
        //assertEquals(defs1, defs2, "Expected iriToDefinitions to differ");
    }

    @Test
    void compareLinkerPass1ResultsBaseUris() throws IOException {
        Set<Map.Entry<String, Set<String>>> baseUris1 = pass1Result.ontologyIdToBaseUris.entrySet();
        Set<Map.Entry<String, Set<String>>> baseUris2 = pass1ResultFromService.ontologyIdToBaseUris.entrySet();
        int count=0;
        for (Map.Entry<String, Set<String>> baseUri1 : baseUris1){
            for (Map.Entry<String, Set<String>> baseUri2 : baseUris2){
                if(baseUri1.getKey().equals(baseUri2.getKey())){
                    Set<String> i1 = baseUri1.getValue();
                    Set<String> i2 = baseUri2.getValue();
                    count+=1;
                    System.out.println("Different base uri fields: "+ReflectionDiffUtil.diffObjects(i1,i2));
                    System.out.println("count: "+count);
                    assertEquals(i1,i2,"baseUris1: "+i1+" and baseUris2: "+i2);
                }
            }
        }
        assertEquals(baseUris1, baseUris2, "Expected ontologyIdToBaseUris to be equal");
    }

    @Test
    void compareLinkerPass1ResultsIriToOids() throws IOException {
        Set<Map.Entry<String, Set<String>>> oitoids1 = pass1Result.ontologyIriToOntologyIds.entrySet();
        Set<Map.Entry<String, Set<String>>> oitoids2 = pass1ResultFromService.ontologyIriToOntologyIds.entrySet();
        int count=0;
        for (Map.Entry<String, Set<String>> oitoid1 : oitoids1){
            for (Map.Entry<String, Set<String>> oitoid2 : oitoids2){
                if(oitoid1.getKey().equals(oitoid2.getKey())){
                    Set<String> i1 = oitoid1.getValue();
                    Set<String> i2 = oitoid2.getValue();
                    count+=1;
                    System.out.println("Different ontologyIriToOntologyIds fields: "+ReflectionDiffUtil.diffObjects(i1,i2));
                    System.out.println("count: "+count);
                    assertEquals(i1,i2,"oioids1: "+i1+" and oioids2: "+i2);
                }
            }
        }
        //assertEquals(oitoids1, oitoids2, "Expected ontologyIriToOntologyIds to be equal");
    }

    @Test
    void compareLinkerPass1ResultsPrefixToOids() throws IOException {
        Set<Map.Entry<String, Set<String>>> pptoids1 = pass1Result.preferredPrefixToOntologyIds.entrySet();
        Set<Map.Entry<String, Set<String>>> pptoids2 = pass1ResultFromService.preferredPrefixToOntologyIds.entrySet();
        int count=0;
        for (Map.Entry<String, Set<String>> pptoid1 : pptoids1){
            for (Map.Entry<String, Set<String>> pptoid2 : pptoids2){
                if(pptoid1.getKey().equals(pptoid2.getKey())){
                    Set<String> i1 = pptoid1.getValue();
                    Set<String> i2 = pptoid2.getValue();
                    count+=1;
                    System.out.println("Different preferredPrefixToOntologyIds fields: "+ReflectionDiffUtil.diffObjects(i1,i2));
                    System.out.println("count: "+count);
                    assertEquals(i1,i2,"pptoids1: "+i1+" and pptoids2: "+i2);
                }
            }
        }
        assertEquals(pptoids1, pptoids2, "Expected preferredPrefixToOntologyIds to be equal");
    }

    @Test
    void compareLinkerPass1ResultsImportingOntologyIds() throws IOException {
        Collection<Map.Entry<String, String>> oids1 = pass1Result.ontologyIdToImportingOntologyIds.entries();
        Collection<Map.Entry<String, String>> oids2 = pass1ResultFromService.ontologyIdToImportingOntologyIds.entries();
        int count=0;
        for (Map.Entry<String, String> oid1 : oids1){
            for (Map.Entry<String, String> oid2 : oids2){
                if(oid1.equals(oid2)){
                    count+=1;
                    System.out.println("Different ontologyIdToImportingOntologyIds fields: "+ReflectionDiffUtil.diffObjects(oid1,oid2));
                    System.out.println("count: "+count);
                    assertEquals(oid1,oid2,"oids1: "+oid1+" and oids2: "+oid2);
                }
            }
        }
        assertEquals(oids1, oids2, "Expected ontologyIdToImportingOntologyIds to be equal");
    }
    @Test
    void compareLinkerPass1ResultsImportedOntologyIds() throws IOException {
        Collection<Map.Entry<String, String>> oids1 = pass1Result.ontologyIdToImportedOntologyIds.entries();
        Collection<Map.Entry<String, String>> oids2 = pass1ResultFromService.ontologyIdToImportedOntologyIds.entries();
        int count=0;
        for (Map.Entry<String, String> oid1 : oids1){
            for (Map.Entry<String, String> oid2 : oids2){
                if(oid1.equals(oid2)){
                    count+=1;
                    System.out.println("Different ontologyIdToImportedOntologyIds fields: "+ReflectionDiffUtil.diffObjects(oid1,oid2));
                    System.out.println("count: "+count);
                    assertEquals(oid1,oid2,"oids1: "+oid1+" and oids2: "+oid2);
                }
            }
        }
        assertEquals(oids1, oids2, "Expected ontologyIdToImportedOntologyIds to be equal");
    }

    public static Path resolveConfigFile(String relativePath) {
        // Start from current working directory (module's base)
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path configPath = currentDir.resolve(relativePath);

        // If it doesn't exist, walk up to find "dataload/configs"
        while (currentDir != null && !Files.exists(configPath)) {
            currentDir = currentDir.getParent();
            if (currentDir == null) break;
            configPath = currentDir.resolve("dataload").resolve(relativePath);
        }

        if (!Files.exists(configPath)) {
            throw new RuntimeException("Config file not found: " + configPath.toAbsolutePath());
        }

        return configPath.toAbsolutePath();
    }

}
