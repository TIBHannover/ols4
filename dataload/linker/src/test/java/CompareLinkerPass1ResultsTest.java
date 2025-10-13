
import org.checkerframework.checker.units.qual.K;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CompareLinkerPass1ResultsTest {

    @Test
    void compareLinkerPass1Results() throws IOException {
        LinkerPass1.LinkerPass1Result pass1Result = LinkerPass1.run("/home/xxx/Documents/git/TIBHannover/ols4/dataload/configs/ontologies_out.json");;
        LinkerPass1FromService.LinkerPass1Result pass1ResultFromService = LinkerPass1FromService.run("http://localhost:8080",20);
        Set<Map.Entry<String, EntityDefinitionSet>> defs1 = pass1Result.iriToDefinitions.entrySet();
        Set<Map.Entry<String, EntityDefinitionSet>> defs2 = pass1ResultFromService.iriToDefinitions.entrySet();
        assertNotEquals(defs1, defs2, "Expected iriToDefinitions to differ");
        int count=0;
        for (Map.Entry<String, EntityDefinitionSet> def1 : defs1){
            for (Map.Entry<String, EntityDefinitionSet> def2 : defs2){
                if(def1.getKey().equals(def2.getKey())){
                    EntityDefinitionSet i1 = def1.getValue();
                    EntityDefinitionSet i2 = def2.getValue();
                    count+=1;
                    assertEquals(i1,i2,"entity definition set "+count+" "+def1.getKey()+" - "+def2.getKey());
                }
            }
        }



    }

}
