package uk.ac.ebi.rdf2json;

import org.obolibrary.robot.IOHelper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author Erhun Giray TUNCAY
 * @email giray.tuncay@tib.eu
 * TIB-Leibniz Information Center for Science and Technology
 */
public class OntologyConversion {
    private static final Logger logger = LoggerFactory.getLogger(OntologyConversion.class);

    private OWLOntology ontology;

    private String extOriginal;

    private String extConverted;

    public OntologyConversion(String url, String id, OWLDocumentFormat convertedFormat) throws IOException {
        convertOntologyToRDF(url,id,convertedFormat);
    }

    public OWLOntology getOntology() {
        return ontology;
    }

    public String getExtOriginal() {
        return extOriginal;
    }

    public String getExtConverted() {
        return extConverted;
    }

    private void convertOntologyToRDF(String url, String outputFile, OWLDocumentFormat convertedFormat) throws IOException {
        FileOutputStream fos = null;
        OWLOntology ont = loadOntology(url);
        try {
            OWLDocumentFormat format = ont.getOWLOntologyManager().getOntologyFormat(ont);
            extOriginal = getExtension(format);
            extConverted = getExtension(convertedFormat);
            if (extOriginal.equals(extConverted)){
                extOriginal = extOriginal+"1";
                extConverted = extConverted+"2";
            }
            if (format instanceof OBODocumentFormat){
                Path resourceDirectory = Paths.get(OntologyGraph.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
                logger.info("Saving the original "+format.getKey()+" format ontology to "+outputFile+extOriginal);
                fos = getFileOutPutStreamForExecutionPath(outputFile+extOriginal);
                ont.saveOntology(format, fos);
                logger.info("Saving the converted RDF/XML Syntax format ontology to "+outputFile+extConverted);
                String filePath = resourceDirectory.resolve(outputFile+extConverted).toString();
                IOHelper iohelper = new IOHelper();
                iohelper.saveOntology(ont,convertedFormat, IRI.create(new File(filePath)),true);
                ont = loadOntology("file:"+filePath);
            } else {
                logger.info("Saving the original "+format.getKey()+" format ontology to "+outputFile+extOriginal);
                fos = getFileOutPutStreamForExecutionPath(outputFile+extOriginal);
                ont.saveOntology(format, fos);
                logger.info("Saving the converted RDF/XML Syntax format ontology to "+outputFile+extConverted);
                fos = getFileOutPutStreamForExecutionPath(outputFile+extConverted);
                ont.saveOntology(new RDFXMLDocumentFormat(), fos);
                Path resourceDirectory = Paths.get(OntologyGraph.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
                String filePath = resourceDirectory.resolve(outputFile+extConverted).toString();
                ont = loadOntology("file:"+filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (OWLOntologyStorageException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fos != null)
                fos.close();
        }
        ontology = ont;
    }

    private OWLOntology loadOntology(String url) throws IOException {
        OWLOntologyManager ontManager = OWLManager.createOWLOntologyManager();
        OWLOntology ont;
        InputStream is = null;
        URLConnection con = null;
        String originalUrl = url;

        try {
            try {
                URL tempURL = new URL(url);
                con = tempURL.openConnection();
                is = tempURL.openStream();
            } catch (IOException e) {
                url = replaceURLByProtocol(con, url);
                try {
                    is = new URL(url).openStream();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            try {
                ont = ontManager.loadOntologyFromOntologyDocument(is);
            } catch (Exception e) {
                url = replaceURLByProtocol(con, url);
                try {
                    is = new URL(url).openStream();
                    ont = ontManager.loadOntologyFromOntologyDocument(is);
                } catch (Exception e2) {
                    ont = ontManager.loadOntologyFromOntologyDocument(IRI.create(url));
                }
            }
        } catch (OWLOntologyCreationException e) {
            ont = importsPrefixesAndNonUnicodeCharactersCorrectingLoader(originalUrl);
        } finally {
            if (is != null)
                is.close();
        }
        return ont;
    }

    public static OWLOntology importsPrefixesAndNonUnicodeCharactersCorrectingLoader(String url) throws IOException {
        url = urlConverter(url).toExternalForm();
        OWLOntologyManager ontManager = OWLManager.createOWLOntologyManager();
        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        config = config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        ontManager.setOntologyLoaderConfiguration(config);
        OWLOntology ontology;
        try {
            try {
                IRI documentIRI = IRI.create(url);
                ontology = ontManager.loadOntology(documentIRI);
            } catch (Exception e) {
                try (InputStream inputStream = urlConverter(url).openStream()) {
                    String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    content = content.replaceAll("[^\\u0009\\u000A\\u000D\\u0020-\\uFFFF]", "");
                    Path cleanedPath = Files.writeString(Paths.get("ontology_cleaned.owl"), content, StandardCharsets.UTF_8);
                    try (InputStream is = Files.newInputStream(cleanedPath)){
                        ontology = ontManager.loadOntologyFromOntologyDocument(is);
                    }
                }
            }

            OWLDataFactory df = ontManager.getOWLDataFactory();
            for (OWLImportsDeclaration declaration : ontology.getImportsDeclarations()){
                IRI modifiedIri = IRI.create(urlConverter(declaration.getIRI().toString()));
                OWLImportsDeclaration modifiedDeclaration = df.getOWLImportsDeclaration(modifiedIri);
                logger.info("original imports iri {} being replaced with {}", declaration.getIRI(), modifiedIri);
                ontManager.applyChange(new RemoveImport(ontology, declaration));
                ontManager.applyChange(new AddImport(ontology, modifiedDeclaration));
            }

            OWLDocumentFormat format = ontManager.getOntologyFormat(ontology);

            if (format != null && format.isPrefixOWLOntologyFormat()) {
                PrefixDocumentFormat pdf = format.asPrefixOWLOntologyFormat();
                Map<String, String> map = pdf.getPrefixName2PrefixMap();
                map.forEach((p, iri) -> logger.info("original {} = {}", p, iri));
                for (Map.Entry<String,String> entry : map.entrySet()){
                    pdf.setPrefix(entry.getKey(), urlConverter(entry.getValue()).toExternalForm());
                }
                map.forEach((p, iri) -> logger.info("corrected {} = {}", p, iri));
            } else {
                logger.info("Ontology format has no prefixes to be redirected.");
            }

            return ontology;
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static URL urlConverter(String url) throws IOException {
        HttpURLConnection con =
                (HttpURLConnection) new URL(url).openConnection();
        con.setInstanceFollowRedirects(true);
        con.setRequestMethod("HEAD");

        while (con.getHeaderField("Location") != null) {
            con = (HttpURLConnection) new URL(con.getHeaderField("Location")).openConnection();
            con.setInstanceFollowRedirects(true);
            con.setRequestMethod("HEAD");
        }
        logger.info("redirected url: {}", con.getURL().toExternalForm());
        return con.getURL();
    }

    private FileOutputStream getFileOutPutStreamForExecutionPath(String outputFile) {
        FileOutputStream fos;
        try {
            Path resourceDirectory = Paths.get(OntologyGraph.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            String filePath = resourceDirectory.resolve(outputFile).toString();
            fos = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return fos;
    }

    private String replaceURLByProtocol(URLConnection con, String url) {
        if (con instanceof HttpsURLConnection) {
            url = url.replace("https:", "http:");
        } else if (con instanceof HttpURLConnection) {
            url = url.replace("http:", "https:");
        }
        return url;
    }

    private String getExtension(OWLDocumentFormat format) throws IllegalArgumentException {
        String ext = ".txt";
        if (format instanceof OBODocumentFormat)
            ext = ".obo";
        else if (format instanceof RDFXMLDocumentFormat)
            ext = ".owl";
        else if (format instanceof TurtleDocumentFormat)
            ext = ".ttl";
        else if (format instanceof OWLXMLDocumentFormat)
            ext = ".owx";
        else if (format instanceof ManchesterSyntaxDocumentFormat)
            ext = ".omn";
        else if (format instanceof FunctionalSyntaxDocumentFormat)
            ext = ".ofn";
        return ext;
    }
}
