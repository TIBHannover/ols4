package uk.ac.ebi.rdf2json;

import org.obolibrary.robot.IOHelper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;

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
                } catch (IOException ioe) {
                    ont = ontManager.loadOntologyFromOntologyDocument(IRI.create(url));
                }
            }
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null)
                is.close();
        }
        return ont;
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
