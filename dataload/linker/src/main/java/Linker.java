import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.cli.*;

import java.io.*;

public class Linker {

    static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException {

        Options options = new Options();

        Option input = new Option(null, "input", true, "unlinked ontologies JSON input filename or service url to get the entities from. Service mode is activated by the service parameter");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option(null, "output", true, "linked ontologies JSON output filename");
        output.setRequired(true);
        options.addOption(output);

        Option leveldbPath = new Option(null, "leveldbPath", true, "optional path of leveldb containing extra mappings (for ORCID etc.)");
        leveldbPath.setRequired(false);
        options.addOption(leveldbPath);

        Option service = new Option(null, "service", false, "Activates the service input mode. If not activated, the default mode is file input mode.");
        leveldbPath.setRequired(false);
        options.addOption(service);

        Option pageSize = new Option(null, "pageSize", true, "Page size for each call of the Service url");
        pageSize.setRequired(false);
        options.addOption(pageSize);

        Option json = new Option(null, "json", false, "fully linked json calls are used instead of the native v2 calls");
        json.setRequired(false);
        options.addOption(json);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("linker", options);

            System.exit(1);
            return;
        }

        String inputPath = cmd.getOptionValue("input");
        String outputFilePath = cmd.getOptionValue("output");
        String leveldb_path = cmd.getOptionValue("leveldbPath");
        boolean serviceMode = cmd.hasOption("service");
        boolean JSON = cmd.hasOption("json");
        int pSize = cmd.hasOption("pageSize") ? Integer.parseInt(cmd.getOptionValue("pageSize")) : 20;

        LevelDB leveldb = leveldb_path != null ? new LevelDB(leveldb_path) : null;

        try {
            LinkerPass1.LinkerPass1Result pass1Result;
            LinkerPass1FromServiceJSON.LinkerPass1Result pass1ResultFromServiceJSON;
            LinkerPass1FromService.LinkerPass1Result pass1ResultFromService;
    //        LinkerPass1.LinkerPass1Result pass1Result = gson.fromJson(new InputStreamReader(new FileInputStream("/Users/james/ols4/linked.json")), LinkerPass1.LinkerPass1Result.class);
            if (serviceMode && JSON) {
                pass1ResultFromServiceJSON = LinkerPass1FromServiceJSON.run(inputPath,pSize);
                LinkerPass2FromServiceJSON.run(inputPath, pSize, outputFilePath, leveldb, pass1ResultFromServiceJSON);
                ServiceBase.httpclient.close();
            } else if (serviceMode) {
                pass1ResultFromService = LinkerPass1FromService.run(inputPath,pSize);
                LinkerPass2FromService.run(inputPath, pSize, outputFilePath, leveldb, pass1ResultFromService);
                ServiceBase.httpclient.close();
            } else {
                pass1Result = LinkerPass1.run(inputPath);
                LinkerPass2.run(inputPath, outputFilePath, leveldb, pass1Result);
            }


    //        gson.toJson(pass1Result, new FileWriter(outputFilePath));
    //        Files.write(Path.of(outputFilePath), gson.toJson(pass1Result).getBytes(StandardCharsets.UTF_8));



        } finally {
            if(leveldb != null)
                leveldb.close();
        }
    }
}


