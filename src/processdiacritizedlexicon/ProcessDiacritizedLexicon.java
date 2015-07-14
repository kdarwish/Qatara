/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package processdiacritizedlexicon;

import ArabicPOSTagger.POSTagger;
import static ArabicPOSTagger.testCase.getProperSegmentation;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import processdiacritizedlexicon.ArabicUtils;
import static processdiacritizedlexicon.ArabicUtils.prefixes;
import static processdiacritizedlexicon.ArabicUtils.suffixes;
import static processdiacritizedlexicon.DiacritizeText.diacritizedPrefixes;
import static processdiacritizedlexicon.DiacritizeText.diacritizedSuffixes;
import org.chasen.crfpp.Tagger;

/**
 *
 * @author kareemdarwish
 */
public class ProcessDiacritizedLexicon {

    public static HashMap<String, Integer> hPrefixes = new HashMap<String, Integer>();
    public static HashMap<String, Integer> hSuffixes = new HashMap<String, Integer>();

    public static HashMap<String, String> diacritizedPrefixes = new HashMap<String, String>();
    public static HashMap<String, String> diacritizedSuffixes = new HashMap<String, String>();

    public static ArabicPOSTagger.POSTagger stemmer = null;
    
    private static Tagger caseEndingTagger = null;

    private static String dataPath="";

    public static DiacritizeText dt = null;

    
    static {
//        try {
//            System.loadLibrary("CRFPP");
//        } catch (UnsatisfiedLinkError e) {
//            System.err.println(e);
//        }
    }

    public ProcessDiacritizedLexicon(String dpath) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException {
        dataPath= dpath;
        initVariables();
    }
    
    public static void initVariables() throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException {
        String prefixes[] = {
            // "ال", "و", "ف", "ب", "ك", "ل", "لل", "س"
            "\u0627\u0644", "\u0648", "\u0641", "\u0628", "\u0643", "\u0644", "\u0644\u0644", "س"
        };

        String suffixes[] = {
            // "ه", "ها", "ك", "ي", "هما", "كما", "نا", "كم", "هم", "هن", "كن",
            // "ا", "ان", "ين", "ون", "وا", "ات", "ت", "ن", "ة"
            // "ية"
            "\u0647", "\u0647\u0627", "\u0643", "\u064a", "\u0647\u0645\u0627", "\u0643\u0645\u0627", "\u0646\u0627", "\u0643\u0645", "\u0647\u0645", "\u0647\u0646", "\u0643\u0646",
            "\u0627", "\u0627\u0646", "\u064a\u0646", "\u0648\u0646", "\u0648\u0627", "\u0627\u062a", "\u062a", "\u0646", "\u0629", "ية"
        };
        // init prefix and suffix list
        for (int i = 0; i < prefixes.length; i++) {
            hPrefixes.put(prefixes[i].toString(), 1);
        }
        for (int i = 0; i < suffixes.length; i++) {
            hSuffixes.put(suffixes[i].toString(), 1);
        }
        diacritizedPrefixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("w"), processdiacritizedlexicon.ArabicUtils.buck2utf8("wa"));
        diacritizedPrefixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("s"), processdiacritizedlexicon.ArabicUtils.buck2utf8("sa"));
        diacritizedPrefixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("f"), processdiacritizedlexicon.ArabicUtils.buck2utf8("fa"));
        diacritizedPrefixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("k"), processdiacritizedlexicon.ArabicUtils.buck2utf8("ka"));
        diacritizedPrefixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("b"), processdiacritizedlexicon.ArabicUtils.buck2utf8("bi"));
        diacritizedPrefixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("l"), processdiacritizedlexicon.ArabicUtils.buck2utf8("li"));
        diacritizedPrefixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("ll"), processdiacritizedlexicon.ArabicUtils.buck2utf8("lilo"));
        diacritizedPrefixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("Al"), processdiacritizedlexicon.ArabicUtils.buck2utf8("Aalo"));

        diacritizedSuffixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("hmA"), processdiacritizedlexicon.ArabicUtils.buck2utf8("humA"));
        diacritizedSuffixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("km"), processdiacritizedlexicon.ArabicUtils.buck2utf8("kum"));
        diacritizedSuffixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("hm"), processdiacritizedlexicon.ArabicUtils.buck2utf8("hum"));
        diacritizedSuffixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("hn"), processdiacritizedlexicon.ArabicUtils.buck2utf8("hun"));
        diacritizedSuffixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("wn"), processdiacritizedlexicon.ArabicUtils.buck2utf8("wna"));
        diacritizedSuffixes.put(processdiacritizedlexicon.ArabicUtils.buck2utf8("yn"), processdiacritizedlexicon.ArabicUtils.buck2utf8("yna"));
        
        // init the stemmer
        stemmer = new POSTagger(dataPath);
        
        // init case ending tagger
        // caseEndingTagger = new Tagger("-m /work/CLASSIC/DIACRITIZE/RDI/case.90-10.stem.gn.trigram-window.model");
        caseEndingTagger = new Tagger("-v1 -m "+dataPath+"case-train.90-10.wverb.txt.model");
        caseEndingTagger.clear();

        dt = new DiacritizeText("", "", dataPath+"rdi+ldc-text.txt.tok.nocase.arpa.blm",
                dataPath+"diacritizedWords.dictionary.GenAndSeen");
        
    }

    public static void diacritizeSTDIN() throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, Exception  {

        BufferedReader sr = new BufferedReader(new InputStreamReader(System.in));
        BufferedWriter sw = new BufferedWriter(new OutputStreamWriter(System.out));

        String line = "";
        while ((line = sr.readLine()) != null) {
            line = line.replace("/", "\\");
            String[] ws = line.split(" +");
            System.out.println("Line:"+line);
            if(line.length()<1)
                    continue;
            String newLine = fullyDiacritizeSentence(line, dt).replace("\\", "/").replace("§","");
            /*
            ArrayList<String> words = ArabicUtils.tokenize(line);// new ArrayList<String>();
            */
            /*
            for (String s : ws) {
                if (s.trim().length() > 0) {
                    words.add(s);
                }
            }
            */
            /*
            String newLine = dt.diacritize(words);//.replace("§","");
            */
            // bw.write(line + "\n");
            // bw.write(ArabicUtils.removeDiacritics(line) + "\n");
            //newLine = newLine.replace("§", "");
            // newLine = recoverDiacriticsLostDuringStandarization(newLine);
            // bw.write(processdiacritizedlexicon.ArabicUtils.utf82buck(newLine) + "\n");
            sw.write(newLine + "\n");
            // bw.write("===================================================================\n");
            sw.flush();
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, Exception {  

        String arg;
        int i=0;
        int args_flag = 0; // correct set of arguments

        while (i < args.length) {
            arg = args[i++];
            // 
            if (arg.equals("--help") || arg.equals("-h")) {
                System.out.println("Usage: ProcessDiacritizedLexicon <--help|-h> <inputfile>");
                System.exit(-1);
            }
            args_flag++;
        }
        System.err.print("Loading libraries ....");
        dataPath = System.getProperty("java.library.path");      
        initVariables();
        System.err.println("\rLibraries loaded.      ");
        System.err.print("Processing file ....");
        switch (args.length) {
            case 0: diacritizeSTDIN(); break;
            case 1: diacritizeFile(args[0]); break;
            default: System.out.println("Usage: ProcessDiacritizedLexicon <--help|-h> <inputfile>");
                System.exit(-1);
        }
        System.err.println("\rProcessing Completed.   ");

        //diacritizeFile("/work/CLASSIC/sample7.txt");
        //diacritizeFile("/work/CLASSIC/sample8.txt");
        
        // countBigrams();
        
//        getWordsWithSingleDiacrtizations ();
        
        // getSubjObjVerbs(dataPath+"/RDI/case-train.90-10.txt");
        
        // mergeDictionaries();
        // processProcessedFiles();
        // diacritizeTreebank();
        // addUnigramContextToWords();
        // removeCaseEndingFromCorpus(dataPath+"/RDI/rdi-text.txt.tok", dataPath+"/RDI/rdi-text.txt.tok.nocase");
        // convertARPAfileToDiacritizationDictionary(dataPath+"/RDI/rdi+ldc-text.txt.tok.nocase.uni", dataPath+"/RDI/rdi+ldc-text.txt.tok.nocase.uni.dictionary");
        
        // buildCaseEndingTrainingCorpus();
        // buildCaseEndingTrainingCorpusToCRF();
        // reformatCaseEndingTrainingCorpus();
        
        
        // simplifyReformatedCaseEndingTrainingCorpus();
        // simplifyReformatedCaseEndingTrainingCorpusAddVerbFeature();
        // diacritizeFile("/work/CLASSIC/all-text.sample.small.txt");
        
        
        // mergeAndPruneGeneratedDictionaryAndObservedDictionary ();
        
        // generateChoicesForLexiconFile();

        // diacritizeTreebankFullSystem();
        // diacritizeTreebankFullSystemSegmented();
        // pruneDictionaryOfPossibilities();
        // generateStemTemplateDiacritizations();
        // getTempaltes();
        // buildPrefixSuffixApplicabilityTable();
        // generateDiacritizedWordsAndCheckWithAffixes();
        // buildDictionaryFrom_generateDiacritizedWordsAndCheckWithAffixes_output();
        
        // generateDiacritizedWordsAndCheck(dataPath+"/templates-list.txt", 
        // dataPath+"/all.uni.count.3", dataPath+"/diacritizedWords.txt.");
        // buildDictionaryFrom_generateDiacritizedWordsAndCheck_output(dataPath+"/diacritizedWords.txt.1.0", dataPath+"/diacritizedWords.dictionary.1.0");
        
        // generateChoicesForLexiconFile();
        
        /*
        generateDiacritizedWordsAndCheck(dataPath+"/templates-list-dialect.txt", 
                "/work/ARABIZI/DIACRITIZE/tweets-unique-words.txt", 
                "/work/ARABIZI/DIACRITIZE/diacritizedWords.txt.");
        buildDictionaryFrom_generateDiacritizedWordsAndCheck_output("/work/ARABIZI/DIACRITIZE/diacritizedWords.txt.1.0", 
                "/work/ARABIZI/DIACRITIZE/diacritizedWords.dictionary.1.0");
        */
        // expandDialectalWordsWithNegation();
        
        // FindDialectalWordsWithLetterSubstitution();
        /*
        // generate MSA verbs
        generateDiacritizedWordsAndCheck("/work/ARABIZI/DIACRITIZE/msa-verb-templates.txt", 
                dataPath+"/all.uni.count.3", 
                "/work/ARABIZI/DIACRITIZE/msa-verbs.txt.");
        buildDictionaryFrom_generateDiacritizedWordsAndCheck_output("/work/ARABIZI/DIACRITIZE/msa-verbs.txt.1.0", 
                "/work/ARABIZI/DIACRITIZE/msa-verbs.dictionary.1.0");
        
        // generate dialectal Egyptian verbs
        generateDiacritizedWordsAndCheck("/work/ARABIZI/DIACRITIZE/egyptian-verb-templates.txt", 
                "/work/ARABIZI/DIACRITIZE/tweets-unique-words.txt", 
                "/work/ARABIZI/DIACRITIZE/egyptian-verbs.txt.");
        buildDictionaryFrom_generateDiacritizedWordsAndCheck_output("/work/ARABIZI/DIACRITIZE/egyptian-verbs.txt.1.0", 
                "/work/ARABIZI/DIACRITIZE/egyptian-verbs.dictionary.1.0");
        */
        
        // UseRegexToExpandDialectalVerbList();
        
        // removeCaseEnding();
        /*
        removeCaseEndingFromWordList(false);
        substituteWordWithOneWithoutCaseEnding(false);
        removeCaseEndingFromWordList(true);
        substituteWordWithOneWithoutCaseEnding(true);
        */
        // generateChoicesForLexiconFileWithBackoff();
        // getMostLikelyTemplateForEachWord();
        // processATBToGenerateTextWithNoCaseEnding();
        
        // getTopNMSAwordsNotCoveredByCorpus();
        // mergeAndPruneGeneratedDictionaryAndObservedDictionary_GetDiacritizationExamplesForCRF ();
        // generateTestDataForCRFDiacritization ();
    }

    public static void getWordsWithSingleDiacrtizations ()  throws FileNotFoundException, IOException
    {
        String filename = dataPath+"/RDI/rdi-text.txt.tok.count";
        BufferedReader br = openFileForReading(filename);
        BufferedWriter bw = openFileForWriting(filename + ".out");
        
        HashMap<String, HashMap<String, Integer>> diacritizationOptions = new HashMap<String, HashMap<String, Integer>>();
        HashMap<String, Integer> tokenCount = new HashMap<String, Integer>();
        
        String line = "";
        while ((line = br.readLine()) != null)
        {
            String[] parts = line.trim().split(" +");
            if (parts.length == 2)
            {
                int count = Integer.parseInt(parts[0]);
                String token = parts[1].trim();
                String tokenNoDiacritics = ArabicUtils.removeDiacritics(token);
                //if (tokenNoDiacritics.equals("أو"))
                //    System.err.println();
                
                if (!diacritizationOptions.containsKey(tokenNoDiacritics))
                    diacritizationOptions.put(tokenNoDiacritics, new HashMap<String, Integer>());
                HashMap<String, Integer> options = diacritizationOptions.get(tokenNoDiacritics);
                options.put(token, count);
                diacritizationOptions.put(tokenNoDiacritics, options);
                
                if (tokenCount.containsKey(tokenNoDiacritics))
                {
                    tokenCount.put(tokenNoDiacritics, tokenCount.get(tokenNoDiacritics) + count);
                }
                else
                {
                    tokenCount.put(tokenNoDiacritics, count);
                }
            }
        }
        
        for (String s : diacritizationOptions.keySet())
        {
            if (tokenCount.get(s) >= 15 && !s.matches("[0-9\\(\\)\\-\\+\\*\\&\\^\\%\\.,;\\:\\$\\#\\@\\!a-zA-Z=<>\\?\\[\\]\\{\\}\\/_]+"))
            {
                HashMap<String, Integer> options = diacritizationOptions.get(s);
                double maxPercentage = 0d;
                String maxDiacritization = "";
                
                for (String opt : options.keySet())
                {
                    int count = options.get(opt);
                    double percentage = (double) count/ (double) tokenCount.get(s);
                    if (percentage > maxPercentage)
                    {
                        maxPercentage = percentage;
                        maxDiacritization = opt;
                    }
                }
                if ((maxPercentage > 0.95d && tokenCount.get(s) >= 1000) || (maxPercentage > 0.98d && tokenCount.get(s) >= 20) || options.keySet().size() == 1)
                {
                    bw.write(s + "\t" + maxDiacritization + "\t" + maxPercentage + "\t" + tokenCount.get(s) + "\n");
//                    for (String opt : options.keySet())
//                    {
//                        bw.write(opt + " " + options.get(opt) + ";");
//                    }
//                    bw.write("\n");
                }
            }
        }
        
        bw.close();
    }
    
    public static void getSubjObjVerbs(String filename) throws FileNotFoundException, IOException
    {
        BufferedReader br = openFileForReading(filename);
        BufferedWriter bw = openFileForWriting(filename + ".out");
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        String line = "";
        int i = 0;
        int vPos = -1;
        String verb = "";
        String subj = "";
        String obj = "";
        while ((line = br.readLine()) != null)
        {
            if (line.trim().length() == 0 || line.contains("PUNC"))
            {
                if (vPos != -1)
                {
                    // print out
                    String key = "verb: " + verb + "\tsujb: " + subj + "\tobj: " + obj;
                    if (!counts.containsKey(key))
                        counts.put(key, 0);
                    counts.put(key, counts.get(key) + 1);
                }
                i = 0;
                vPos = -1; // position of verb
                verb = "";
                subj = "";
                obj = "";
            }
            else
            {
                i++;
                String[] parts = line.split("\t");
                String word = parts[1];
                String POS = parts[2];
                String diacritic = parts[12];

                if (POS.equals("V"))
                {
                    if (vPos != -1)
                    {
                        // print out
                        String key = "verb: " + verb + "\tsujb: " + subj + "\tobj: " + obj;
                        if (!counts.containsKey(key))
                            counts.put(key, 0);
                        counts.put(key, counts.get(key) + 1);
                    }
                    
                    vPos = i;
                    verb = "";
                    subj = "";
                    obj = "";
                    verb = word;
                }
                else if (POS.contains("NOUN") && (diacritic.replace("~", "").equals("u") || diacritic.replace("~", "").endsWith("N")) && vPos != -1 && i - vPos <= 5)
                {
                    // potential subj
                    if (subj.trim().length() == 0)
                        subj = word;
                }
                else if (POS.contains("NOUN") && (diacritic.replace("~", "").equals("a") || diacritic.replace("~", "").endsWith("F")) && vPos != -1 && i - vPos <= 10)
                {
                    // potential obj
                    if (obj.trim().length() == 0)
                        obj = word;
                }
            }
        }
        
        for(String s : counts.keySet())
        {
            bw.write(counts.get(s) + "\t" + s + "\n");
        }
        
        br.close();
        bw.close();
    }
    
    public static String printOutAggregateOutput(String word, String aggregator, BufferedWriter bw, String prevSuffix, String prevPrefix) 
            throws IOException, ClassNotFoundException, Exception 
    {
        String stem = getWordStem(word, false);
        String clitics = getWordStem(word, true);
        
        String currentSuffix = "#";
        String currentPrefix = "#";
        
        String lastCurrentPrefix = "#";
        String firstCurrentSuffix = "#";
        
        //if (aggregator.contains("."))
        //    System.err.println();

        String[] posTags = aggregator.split("\\+");
        ArrayList<String> partsArray = new ArrayList<String>();
        String[] parts = clitics.split(";");
        if (word.equals(";")) {
            partsArray.add(";");
        } else {
            for (int j = 0; j < parts.length; j++) {
                String p = parts[j];
                if (p.endsWith("+")) {
                    for (String c : p.split("\\+")) {
                        if (c.trim().length() > 0) {
                            partsArray.add(c + "+");
                        }
                    }
                    // get last prefix
                    lastCurrentPrefix = p.split("\\+")[p.split("\\+").length - 1] + "+";
                    currentPrefix = p;
                } else if (p.startsWith("+")) {
                    for (String c : p.split("\\+")) {
                        if (c.trim().length() > 0) {
                            partsArray.add("+" + c);
                        }
                    }
                    firstCurrentSuffix = "+" + p.split("\\+")[0];
                    currentSuffix = p;
                } else {
                    if (p.trim().length() > 0) {
                        partsArray.add(p);
                    }
                }
            }
            
        }
        if (posTags.length != partsArray.size()) {
            System.err.println("mismatch");
        }
        for (int j = 0; j < partsArray.size(); j++) {
            // the features of interest are:
            // 1. undiacritized token
            // 2. POS tag
            // 3. last current prefix (if stem, otherwise Y)
            // 4. first current suffix (if stem, otherwise Y)
            // 5. template (if stem, otherwise undiacritized token)
            // 6. last letter of word (if stem, otherwise Y)
            // 7. previous word prefix (if stem, otherwise Y)
            // 8. previous word suffix (if stem, otherwise Y)
            // 9. next letter (if prefix, otherwise Y)
            // 10. first letter in the word (if stem, otherwise Y) -- to catch verbs in present tense
            // 11. gender and number if NOUN, ADJ, or NUM
            // 11. diacritized form
            
            String p = partsArray.get(j);
            String[] posParts = posTags[j].split("\\/");
            String POS = "Y";
            String template = "Y";
            if (posParts.length == 2)
            {
                POS = posParts[0];
                template = posParts[1];
            }
            else if (posParts.length == 3)
            {
                POS = posParts[2];
                template = posParts[1];
            }
            else if (posParts.length > 3)
            {
                POS = posParts[posParts.length - 1];
                template = posParts[posParts.length - 2];
            }
            else
            {
                //System.err.println();
            }
            String toPrint = "";
            if (p.endsWith("+")) {
                String firstLetter = "#";
                if (stem.trim().length() > 0)
                    firstLetter = stem.substring(0, 1);
                // bw.write(ArabicUtils.removeDiacritics(p) + "\t" + posTags[j] + p + "\t" + "\n");
                toPrint = MessageFormat.format("{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}\t{7}\t{8}\t{9}\t{10}\n", 
                        ArabicUtils.removeDiacritics(p),    // 1
                        POS,                                // 2
                        "Y",                  // 3
                        "Y",                 // 4
                        ArabicUtils.removeDiacritics(p),                           // 5
                        "Y", //6
                        "Y",                                // 7
                        "Y",                                // 8
                        firstLetter,               // 9
                        "Y",                // 10
                        p                                   // 11
                );                
            } else if (p.startsWith("+")) {
                String firstLetter = "#";
                if (stem.trim().length() > 0)
                    firstLetter = stem.substring(0, 1);
                if (p.equals("+ة") || p.equals("+ات"))
                {
                    toPrint = MessageFormat.format("{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}\t{7}\t{8}\t{9}\t{10}\n", 
                        ArabicUtils.removeDiacritics(p),    // 1
                        POS,                                // 2
                        lastCurrentPrefix,                  // 3
                        firstCurrentSuffix,                 // 4
                        ArabicUtils.removeDiacritics(p),                           // 5
                        "Y", //6
                        prevPrefix,                                // 7
                        prevSuffix,                                // 8
                        firstLetter,               // 9
                        "Y",                // 10
                        p                                   // 11
                    );  
                }
                else
                {
                    toPrint = MessageFormat.format("{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}\t{7}\t{8}\t{9}\t{10}\n", 
                        ArabicUtils.removeDiacritics(p),    // 1
                        POS,                                // 2
                        "Y",                  // 3
                        "Y",                 // 4
                        ArabicUtils.removeDiacritics(p),                           // 5
                        "Y", //6
                        "Y",                                // 7
                        "Y",                                // 8
                        "Y",               // 9
                        "Y",                // 10
                        p                                   // 11
                    );  
                }
            } else {
                String diacritic = ArabicUtils.utf82buck(p.replaceFirst("^.*[" + ArabicUtils.AllArabicLetters + "]", ""));
                // remove last diacritic
                String stemNoCase = p.substring(0, p.length() - diacritic.length());
                if (diacritic.trim().length() > 0 && stemNoCase.trim().length() > 0) {
                    // bw.write(stemNoCase + "\t" + posTags[j] + "\t" + diacritic + "\n");
                } else {
                    if (stem.endsWith("ي") || stem.endsWith("ا") || stem.endsWith("و")) {
                        // bw.write(stemNoCase + "\t" + posTags[j] + "\tMaad\n");
                        diacritic = "Maad";
                    } else {
                        // bw.write(word + "\t" + posTags[j] + "\tnull\n");
                        diacritic = "null";
                    }
                }
                p = ArabicUtils.removeDiacritics(p);
                //if (p.trim().length() == 0)
                //    System.err.println();
                if (POS.equals("PREP") || POS.equals("PART"))
                {
                    template = p;
                }
                toPrint = MessageFormat.format("{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}\t{7}\t{8}\t{9}\t{10}\n", 
                        p,    // 1
                        POS,                                // 2
                        lastCurrentPrefix,                  // 3
                        firstCurrentSuffix,                 // 4
                        template,                           // 5
                        p.substring(p.length() - 1), //6
                        prevPrefix,                                // 7
                        prevSuffix,                                // 8
                        "Y",               // 9
                        p.substring(0, 1), // 10
                        diacritic                                   // 11
                    );  
            }
            bw.write(toPrint);
            bw.flush();
        }
        return currentPrefix + "\t" + currentSuffix;
    }
    
    public static void buildCaseEndingTrainingCorpus() throws FileNotFoundException, IOException, ClassNotFoundException, Exception
    {
        BufferedReader br = openFileForReading(dataPath+"/RDI/rdi-text.txt.tok");
        BufferedWriter bw = openFileForWriting(dataPath+"/RDI/rdi-text.txt.tok.case-info");
        
        String line = "";
        while ((line = br.readLine()) != null)
        {
            ArrayList<String> words = ArabicUtils.tokenizeWithoutProcessing(line);
            int position = 0;
            
            ArrayList<String> pos = stemmer.tag(line, false, false);
            String aggregator = "";
            String lastAffixes = "#\t#";
            for (String tag : pos)
            {
                if (tag.equals("-"))
                {
                    if (aggregator.trim().length() > 0)
                    {
                        // find diacritized form
                        String[] segments = aggregator.split("\\+");
                        String originalWord = "";
                        for (String s : segments)
                        {
                            if (originalWord.trim().length() > 0)
                                originalWord += "+";
                            if (s.startsWith("/"))
                                originalWord = s.substring(0, 1);
                            else
                                originalWord += s.replaceFirst("\\/.*", "");
                        }
//                        if (words.get(position).contains("1980"))
//                            {
//                                ArrayList<String> temp = ArabicUtils.tokenize(words.get(position));
//                                temp = ArabicUtils.tokenizeWithoutProcessing(words.get(position));
//                                System.err.println();
//                            }
                        if (ArabicUtils.removeDiacritics(words.get(position)).equals(originalWord.replaceFirst("^ل\\+ال", "لل").replaceFirst("^و\\+ل\\+ال", "ولل").replaceFirst("^ف\\+ل\\+ال", "فلل").replace("+", "")))
                        {
                            String word = words.get(position);
                            word = transferDiacriticsFromWordToSegmentedVersion(word, originalWord);
                            position++;
                            lastAffixes = printOutAggregateOutput(word, aggregator, bw, 
                                        lastAffixes.replaceFirst("\t.*", ""), lastAffixes.replaceFirst(".*\t", ""));
                            bw.flush();
                            aggregator = "";
                        }
                        else
                        {
                            if (!originalWord.contains("+"))
                            {
                                lastAffixes = printOutAggregateOutput(words.get(position), aggregator, bw, 
                                        lastAffixes.replaceFirst("\t.*", ""), lastAffixes.replaceFirst(".*\t", ""));
                                position++;
                                bw.flush();
                                aggregator = "";
                            }
                            else
                            {
                                System.err.println(words.get(position) + "\t" + aggregator);
                                position++;
                                aggregator = "";
                            }
                        }
                        String toPrint = MessageFormat.format("{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}\t{7}\t{8}\t{9}\t{10}\n",
                                "_", // 1
                                "O", // 2
                                "O", // 3
                                "O", // 4
                                "O", // 5
                                "O", //6
                                "O", // 7
                                "O", // 8
                                "O", // 9
                                "O", // 10
                                "O" // 11
                        );
                        bw.write(toPrint);
                    }
                }
                else
                {
                    if (aggregator.trim().length() > 0)
                    {
                        aggregator += "+";
                    }
                    aggregator += tag;
                }
            }
            if (aggregator.trim().length() > 0)
            {
                String word = words.get(position);
                position++;
                lastAffixes = printOutAggregateOutput(word, aggregator, bw,
                        lastAffixes.replaceFirst("\t.*", ""), lastAffixes.replaceFirst(".*\t", ""));
                bw.flush();
                aggregator = "";
            }
            bw.write("\n");
        }
        br.close();
        bw.close();
    }
    
    
    // build final training/test set
    public static void simplifyReformatedCaseEndingTrainingCorpus() throws FileNotFoundException, IOException, ClassNotFoundException, UnsupportedEncodingException, InterruptedException, Exception
    {
        genderNumberTags gnt = new genderNumberTags("/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/POSandNERData/");
        
        BufferedReader br = openFileForReading(dataPath+"/RDI/rdi-text.txt.tok.case-info.combine");
        BufferedWriter bw = openFileForWriting(dataPath+"/RDI/rdi-text.txt.tok.case-info.combine.simple");
        String line = "";
        ArrayList<String> lines = new ArrayList<String>();
        while ((line = br.readLine()) != null)
        {
            if (line.startsWith("_") || line.trim().length() == 0)
            {
                String prefix = "";
                String prefixPOS = "";
                String suffix = "";
                String suffixPOS = "";
                String stem = "";
                String stemPOS = "";
                String word = "";
                String diacritic = "";
                String template = "#";
                for (int i = 0; i < lines.size(); i++)
                {
                    if (lines.get(i).endsWith("+"))
                    {
                        String[] first = lines.get(i).split("\t");
                        prefix += first[0].trim();
                        prefixPOS += "+" + first[1].trim();
                        word += first[0];
                    }
                    else
                    {
                        if (lines.get(i).startsWith("+"))
                        {
                            String[] first = lines.get(i).split("\t");
                            suffix += first[0].trim();
                            suffixPOS += "+" + first[1].trim();
                            word += first[0];
                        }
                        else
                        {
                            if (lines.get(i).startsWith("+"))
                            {
                                String[] first = lines.get(i).split("\t");
                                suffix += first[0].trim();
                                suffixPOS += "+" + first[1].trim();
                                word += first[0];
                            }
                            else
                            {
                                String[] first = lines.get(i).split("\t");
                                stem += first[0].trim();
                                stemPOS += "+" + first[1].trim();
                                if (first[1].trim().equals("V"))
                                    template = first[4];
                                word += first[0];
                                diacritic = lines.get(i).replaceFirst(".*\t", "");
                            }
                        }
                    }
                }
                if (prefix.trim().length() == 0) 
                {
                    prefix = "#"; prefixPOS = "Y";
                }
                else
                {
                    prefixPOS = prefixPOS.substring(1);
                }
                if (suffix.trim().length() == 0) 
                {
                    suffix = "#"; suffixPOS = "Y";
                }
                else
                {
                    suffixPOS = suffixPOS.substring(1);
                }
                if (stem.trim().length() == 0) 
                {
                    stem = "#"; stemPOS = "Y";
                }
                else
                {
                    stemPOS = stemPOS.substring(1);
                }
                if (word.trim().length() > 0)
                {
                    word = word.replace("++", "+");
                    // extra features
                    // suffix
                    String Suff = "#";
                    if (stem.contains("+"))
                        Suff = stem.substring(stem.indexOf("+"));
                    // first letter
                    String firstLetter = stem.substring(0, 1);
                    String lastLetter = stem.substring(stem.length() - 1);
                    
                    if (diacritic.trim().length() == 0)
                        diacritic = "null";
                
                    String genderNumberTag = "O";
                    if (stemPOS.contains("NOUN") || stemPOS.contains("ADJ") || stemPOS.contains("NUM"))
                    {
                        genderNumberTag = gnt.tagWord(stem, stemPOS);
                    }
                    
                    bw.write(word + "\t" + stem + "\t" + stemPOS + "\t" + 
                            prefix + "\t" + prefixPOS + "\t" + 
                            suffix + "\t" + suffixPOS + "\t" + 
                            Suff + "\t" + firstLetter + "\t" + lastLetter + "\t" + 
                            template + "\t" + genderNumberTag + "\t" +
                            diacritic + "\n");
                }
                
                if (line.trim().length() == 0)
                    bw.write(line + "\n");
//                else
//                    bw.write("_\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\n");
                bw.flush();
                lines.clear();
            }
            else
            {
                lines.add(line);
            }
        }
        bw.close();
    }
    
    // build final training/test set with verb feature
    public static void simplifyReformatedCaseEndingTrainingCorpusAddVerbFeature() throws FileNotFoundException, IOException, ClassNotFoundException, UnsupportedEncodingException, InterruptedException, Exception
    {
        genderNumberTags gnt = new genderNumberTags("/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/POSandNERData/");
        
        BufferedReader br = openFileForReading(dataPath+"/RDI/rdi-text.txt.tok.case-info.combine");
        BufferedWriter bw = openFileForWriting(dataPath+"/RDI/rdi-text.txt.tok.case-info.combine.simple");
        String line = "";
        ArrayList<String> lines = new ArrayList<String>();
        String lastSeenVerb = "";
        int lastSeenVerbLoc = -1;
        int currentPosInSentence = 0;
        while ((line = br.readLine()) != null)
        {
            if (line.startsWith("_") || line.trim().length() == 0)
            {
                if (line.trim().length() == 0)
                {
                    currentPosInSentence = 0;
                    lastSeenVerbLoc = -1;
                    lastSeenVerb = "";
                }
                String prefix = "";
                String prefixPOS = "";
                String suffix = "";
                String suffixPOS = "";
                String stem = "";
                String stemPOS = "";
                String word = "";
                String diacritic = "";
                String template = "#";
                for (int i = 0; i < lines.size(); i++)
                {
                    if (lines.get(i).endsWith("+"))
                    {
                        String[] first = lines.get(i).split("\t");
                        prefix += first[0].trim();
                        prefixPOS += "+" + first[1].trim();
                        word += first[0];
                    }
                    else
                    {
                        if (lines.get(i).startsWith("+"))
                        {
                            String[] first = lines.get(i).split("\t");
                            suffix += first[0].trim();
                            suffixPOS += "+" + first[1].trim();
                            word += first[0];
                        }
                        else
                        {
                            if (lines.get(i).startsWith("+"))
                            {
                                String[] first = lines.get(i).split("\t");
                                suffix += first[0].trim();
                                suffixPOS += "+" + first[1].trim();
                                word += first[0];
                            }
                            else
                            {
                                String[] first = lines.get(i).split("\t");
                                stem += first[0].trim();
                                stemPOS += "+" + first[1].trim();
                                if (first[1].trim().equals("V"))
                                    template = first[4];
                                word += first[0];
                                diacritic = lines.get(i).replaceFirst(".*\t", "");
                            }
                        }
                    }
                }
                if (prefix.trim().length() == 0) 
                {
                    prefix = "#"; prefixPOS = "Y";
                }
                else
                {
                    prefixPOS = prefixPOS.substring(1);
                }
                if (suffix.trim().length() == 0) 
                {
                    suffix = "#"; suffixPOS = "Y";
                }
                else
                {
                    suffixPOS = suffixPOS.substring(1);
                }
                if (stem.trim().length() == 0) 
                {
                    stem = "#"; stemPOS = "Y";
                }
                else
                {
                    stemPOS = stemPOS.substring(1);
                }
                if (word.trim().length() > 0)
                {
                    word = word.replace("++", "+");
                    // extra features
                    
                    // set current position in sentence
                    currentPosInSentence++;
                    // check if verb
                    String lastVerb = "VerbNotSeen";
                    if (stemPOS.equals("V"))
                    {
                        // this is a verb
                        lastSeenVerb = stem;
                        lastSeenVerbLoc = currentPosInSentence;
                    }
                    else if (lastSeenVerbLoc != -1 && currentPosInSentence - lastSeenVerbLoc < 7)
                    {
                        lastVerb = lastSeenVerb;
                    }
                    // suffix
                    String Suff = "#";
                    if (stem.contains("+"))
                        Suff = stem.substring(stem.indexOf("+"));
                    // first letter
                    String firstLetter = stem.substring(0, 1);
                    String lastLetter = stem.substring(stem.length() - 1);
                    
                    if (diacritic.trim().length() == 0)
                        diacritic = "null";
                
                    String genderNumberTag = "O";
                    if (stemPOS.contains("NOUN") || stemPOS.contains("ADJ") || stemPOS.contains("NUM"))
                    {
                        genderNumberTag = gnt.tagWord(stem, stemPOS);
                    }
                    
                    bw.write(word + "\t" + stem + "\t" + stemPOS + "\t" + 
                            prefix + "\t" + prefixPOS + "\t" + 
                            suffix + "\t" + suffixPOS + "\t" + 
                            Suff + "\t" + firstLetter + "\t" + lastLetter + "\t" + 
                            template + "\t" + genderNumberTag + "\t" +
                            lastVerb + "\t" +
                            diacritic + "\n");
                }
                
                if (line.trim().length() == 0)
                    bw.write(line + "\n");
//                else
//                    bw.write("_\tO\tO\tO\tO\tO\tO\tO\tO\tO\tO\n");
                bw.flush();
                lines.clear();
            }
            else
            {
                lines.add(line);
            }
        }
        bw.close();
    }
    
    public static void reformatCaseEndingTrainingCorpus() throws FileNotFoundException, IOException
    {
        BufferedReader br = openFileForReading(dataPath+"/RDI/rdi-text.txt.tok.case-info");
        BufferedWriter bw = openFileForWriting(dataPath+"/RDI/rdi-text.txt.tok.case-info.combine");
        String line = "";
        ArrayList<String> lines = new ArrayList<String>();
        while ((line = br.readLine()) != null)
        {
            if (line.startsWith("_") || line.trim().length() == 0)
            {
                for (int i = 0; i < lines.size(); i++)
                {
                    if (lines.get(i).endsWith("+"))
                    {
                        bw.write(lines.get(i) + "\n");
                    }
                    else
                    {
                        if (i == lines.size() - 1 || lines.get(i).startsWith("+"))
                        {
                            bw.write(lines.get(i) + "\n");
                        }
                        else
                        {
                            if (i < lines.size() - 1 && lines.get(i + 1).contains("NSUFF"))
                            {
                                String[] first = lines.get(i).split("\t");
                                String[] next = lines.get(i+1).split("\t");
                                String newLine = "";
                                String diacritic = processdiacritizedlexicon.ArabicUtils.utf82buck(next[10]).replaceAll("[^aiouNKF~]+", "");
                                if (diacritic.trim().length() == 0)
                                {
                                    //System.err.println();
                                    if (next[10].substring(next[10].length() - 1).equals("ي") || next[10].substring(next[10].length() - 1).equals("ا") || next[10].substring(next[10].length() - 1).equals("و"))
                                        diacritic = "Maad";
                                    else
                                        diacritic = "null";
                                }
                                
                                newLine = first[0].trim() + next[0].trim() +
                                        "\t" + first[1].trim() + "+" + next[1].trim() +
                                        "\t" + first[2] +
                                        "\t" + first[3] +
                                        "\t" + first[4] + ArabicUtils.utf82buck(next[4]) +
                                        "\t" + first[5] +
                                        "\t" + first[6] +
                                        "\t" + first[7] +
                                        "\t" + first[8] +
                                        "\t" + first[9] +
                                        "\t" + diacritic
                                        ;
                                i++;
                                bw.write(newLine + "\n");
                            }
                            else
                            {
                                bw.write(lines.get(i) + "\n");
                            }
                        }
                    }
                }
                bw.write(line + "\n");
                bw.flush();
                lines.clear();
            }
            else
            {
                lines.add(line);
            }
        }
        bw.close();
    }
    
    public static void buildCaseEndingTrainingCorpusToCRF() throws FileNotFoundException, IOException
    {
        BufferedReader br = openFileForReading(dataPath+"/RDI/rdi-text.txt.tok.case-info");
        BufferedWriter bw = openFileForWriting(dataPath+"/RDI/rdi-text.txt.tok.case-info.dictionary");
        
        HashMap<String, HashMap<String, Integer>> diacritics = new HashMap<String, HashMap<String,Integer>>();
        HashMap<String, Long> wordCount = new HashMap<String, Long>();
        String line = "";
        ArrayList<String> eachWord = new ArrayList<String>();
        while ((line = br.readLine()) != null)
        {
            String[] parts = line.split("\t");
            if (parts.length == 11) // && (parts[0].startsWith("+") || parts[0].endsWith("+")))
            {
                String word = parts[0];
                String dia = parts[10];
                if (!diacritics.containsKey(word))
                {
                    diacritics.put(word, new HashMap<String, Integer>());
                }
                HashMap<String, Integer> tmp = diacritics.get(word);
                if (tmp.containsKey(dia))
                    tmp.put(dia, tmp.get(dia) + 1);
                else
                    tmp.put(dia, 1);
                diacritics.put(word, tmp);
                if (wordCount.containsKey(word))
                    wordCount.put(word, wordCount.get(word) + 1);
                else
                    wordCount.put(word, 1l);
            }
        }
        for (String s : diacritics.keySet())
        {
            if (wordCount.get(s) > 20)
            {
                bw.write(s + "\t");
                Map<String, Integer> tmp = MapUtil.sortByValue(diacritics.get(s));
                float count = 0f;
                for (String  ss : tmp.keySet())
                {
                    // get count
                    count += tmp.get(ss);
                }
                float agg = 0f;
                for (String  ss : tmp.keySet())
                {
                    // get count
                    if ((agg/count) < 0.95) // && tmp.size() == 1)
                        bw.write(ss + "," + tmp.get(ss) + ";");
                    agg += tmp.get(ss);
                }
                if (tmp.size() == 1)
                    bw.write("\t**");
                bw.write("\n");
                bw.flush();
            }
        }
        bw.close();
    }
    
    public static void getTopNMSAwordsNotCoveredByCorpus() throws FileNotFoundException, IOException, InterruptedException, ClassNotFoundException, Exception
    {
        ArabicPOSTagger.POSTagger tagger = new POSTagger("/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/POSandNERData/");
        // load seen word forms
        BufferedReader brSeen = openFileForReading("/work/ARABIZI/DIACRITIZE/diacritizedWords.dictionary.from-text");
        TMap<String, Integer> dictionary = new THashMap<String, Integer>();
        
        String line = "";

        while ((line = brSeen.readLine()) != null)
        {
            String[] parts = line.split("\t");
            if (parts.length == 2)
            {
                ArrayList<String> stem = getStemmedWords(tagger.tag(parts[0], true, false));
                String stemmedWord = "";
                for (String dia : stem)
                    stemmedWord += dia + " ";
                stemmedWord = stemmedWord.trim();
                dictionary.put(removeAffixes(stemmedWord), 1);
            }
        }
        
        // read corpus and get top n unigrams
        BufferedReader br = openFileForReading("/Users/kareemdarwish/RESEARCH/ALJAZEERA/articles-no-talk-shows.txt.tokenized.tok.lm.sort");
        BufferedWriter bw = openFileForWriting(dataPath+"/diacritizedWords.dictionary.missing");
        while ((line = br.readLine()) != null)
        {
            String[] parts = line.split("\t");
            if (parts.length == 2)
            {
                ArrayList<String> stem = getStemmedWords(tagger.tag(parts[0], true, false));
                String stemmedWord = "";
                for (String dia : stem)
                    stemmedWord += dia + " ";
                stemmedWord = removeAffixes(stemmedWord).trim();
                if (!dictionary.containsKey(stemmedWord) && stemmedWord.matches("[" + ArabicUtils.AllArabicLetters + "]+"))
                {
                    bw.write(stemmedWord + "\t" + line + "\n");
                    bw.flush();
                }
            }
        }
        
    }
    
    public static void processATBToGenerateTextWithNoCaseEnding() throws FileNotFoundException, IOException
    {
        BufferedReader br = openFileForReading("/work/CLASSIC/LDC/allText.txt");
        BufferedWriter bw = openFileForWriting("/work/CLASSIC/LDC/allText.txt.nocase");
        
        String line = "";
        while ((line = br.readLine()) != null)
        {
            line = line.replace("- -", "+");
            line = line.replace("`", "a");
            line = line.replace("aa", "a");
            line = line.replace("(null)", "");
            // line = processdiacritizedlexicon.ArabicUtils.buck2utf8(line);
            String[] words = line.split(" +");
            for (String word : words)
            {
                String stemmed = processdiacritizedlexicon.ArabicUtils.buck2utf8(word.replaceAll("[aiou~NKF]", ""));
                stemmed = stemmed.replace("++", "+");
                stemmed = stemmed.replaceFirst("^\\+", "").replaceFirst("\\+$", "");
                String diacritized = processdiacritizedlexicon.ArabicUtils.buck2utf8(word.replace("+", ""));
                String stemmedAndDiacritized = transferDiacriticsFromWordToSegmentedVersion(diacritized, stemmed);
                stemmedAndDiacritized = standardizeDiacritics(removeCaseEnding(stemmedAndDiacritized));
                bw.write(stemmedAndDiacritized.replace("+", "") + " ");
            }
            bw.write("\n");
        }
        bw.close();
    }
    
    public static String buck2utf8Partial(String input) {
        input = input.replace("A", "\u0627").replace("<", "\u0625").replace(">", "\u0623").replace("'", "\u0621");
        input = input.replace("b", "\u0628").replace("t", "\u062a").replace("v", "\u062b").replace("j", "\u062c").replace("H", "\u062d");
        input = input.replace("x", "\u062e").replace("d", "\u062f").replace("r", "\u0631").replace("z", "\u0632");
        input = input.replace("s", "\u0633").replace("$", "\u0634").replace("S", "\u0635").replace("D", "\u0636").replace("T", "\u0637");
        input = input.replace("Z", "\u0638").replace("E", "\u0639").replace("g", "\u063a").replace("f", "\u0641").replace("q", "\u0642");
        input = input.replace("k", "\u0643").replace("l", "\u0644").replace("m", "\u0645").replace("n", "\u0646").replace("h", "\u0647");
        input = input.replace("w", "\u0648").replace("y", "\u064a").replace("Y", "\u0649").replace("p", "\u0629").replace("&", "\u0624");
        input = input.replace("}", "\u0626").replace("{", "إ");
        input = input.replace("a", "\u064e").replace("F", "\u064b").replace("u", "\u064f").replace("N", "\u064c");
        input = input.replace("i", "\u0650").replace("K", "\u064d").replace("o", "\u0652").replace("~", "\u0651");
        return input;
    }
    
    public static void UseRegexToExpandDialectalVerbList() throws FileNotFoundException, IOException
    {
        BufferedReader brVerbs = openFileForReading("/work/ARABIZI/DIACRITIZE/egyptian-and-msa-verbs.dictionary.1.0");
        String line = "";
        HashMap<String, Integer> verbs = new HashMap<String, Integer>();
        while ((line = brVerbs.readLine()) != null)
        {
            String[] parts = line.trim().split("[\\s\t]+");
            if (parts.length == 2) {
                verbs.put(parts[0], 0);
            }
        }
        
        BufferedReader brMSA = openFileForReading("/work/ARABIZI/DIACRITIZE/normalized-msa-unigrams.lm");
        HashMap<String, Float> msaWords = new HashMap<String, Float>();
        while ((line = brMSA.readLine()) != null)
        {
            String[] parts = line.split("\t");
            
            if (parts.length == 2 && parts[0].matches("[0-9\\-\\.]+"))
            {
                String word = parts[1];
            float score = Float.parseFloat(parts[0]);
            if (score >= -8)
                msaWords.put(word, score);
            }
        }
        
        BufferedReader brWords = openFileForReading("/work/ARABIZI/DIACRITIZE/tweets-unique-words.txt");
        
        /*
        [cnj]: w, f
        [mA]: m, mA
        [$]: $, $y
        [sbj]: t, nA, ty, tm, tw, twA, w, wA
        [obj]: ny, nA, k, ky, km, kwm, kw, kwA, h, hw, hA, hm, hwm
        [prp]: ly, lnA, lk, lkw, lkm, lkwm, lh, lhw, lw, lhA, lhm, lhwm
        Past verb: [cnj] [mA] verb [sbj] [obj] [prep] [$]
        */
        
        Pattern regexVerbWithoutNegationPast = Pattern.compile(buck2utf8Partial("(?:[wf]*?)(.+)(?:t|nA|ty|tm|tw|twA|w|wA)*(?:ny|nA|k|ky|km|kwm|kw|kwA|h|hw|hA|hm|hwm)*"
                + "(ly|lnA|lk|lkw|lkm|lkwm|lh|lhw|lw|lhA|lhm|lhwm)+"));
        Pattern regexVerbWithNegationPast = Pattern.compile(buck2utf8Partial("(?:[wf]*?)(?:m|mA)*(.+)(?:t|nA|ty|tm|tw|twA|w|wA)*(?:ny|nA|k|ky|km|kwm|kw|kwA|h|hw|hA|hm|hwm)*"
                + "(ly|lnA|lk|lkw|lkm|lkwm|lh|lhw|lw|lhA|lhm|lhwm)*(?:$|$y)+"));
        /*
        [anit]: >, n, y, t  ([anit]verb: means a verb starts with any of them)
        [bh]: b, h, H, hA, HA
        [sbj2]: t, ty, y, w, wA
        Present verb: [cnj] [mA] [bh] [anit]verb [sbj2] [obj] [prep] [$]
        */
        Pattern regexVerbWithoutNegationPresent = 
                Pattern.compile(buck2utf8Partial("(?:[wf]*?)(?:b|h|H|hA|HA)+(?:>|n|y|t)*(.+)(?:t|nA|ty|tm|tw|twA|w|wA)*(?:ny|nA|k|ky|km|kwm|kw|kwA|h|hw|hA|hm|hwm)*"
                + "(ly|lnA|lk|lkw|lkm|lkwm|lh|lhw|lw|lhA|lhm|lhwm)*"));
        Pattern regexVerbWithNegationPresent = 
                Pattern.compile(buck2utf8Partial("(?:[wf]*?)(?:m|mA)*(?:b|h|H|hA|HA)*(?:>|n|y|t)*(.+)(?:t|nA|ty|tm|tw|twA|w|wA)*(?:ny|nA|k|ky|km|kwm|kw|kwA|h|hw|hA|hm|hwm)*"
                + "(ly|lnA|lk|lkw|lkm|lkwm|lh|lhw|lw|lhA|lhm|lhwm)*(?:$|$y)+"));
        
        BufferedWriter bw = openFileForWriting("/work/ARABIZI/DIACRITIZE/egyptian-and-msa-verbs.dictionary.1.0.expand");
        
        while ((line = brWords.readLine()) != null) {
            String[] parts = line.trim().split("[\\s\t]+");
            if (parts.length == 2) {
                // check if matches one of the above, then print it
                String word = parts[0];
                
                Matcher m = regexVerbWithoutNegationPast.matcher(word);
                /*
                if (m.matches() && m.group(1).length() > 1 && !msaWords.containsKey(word))
                {
                    if (verbs.containsKey(m.group(1)) || verbs.containsKey(m.group(1) + "ل"))
                        bw.write(word + "\n");
                        
                }
                */
                m = regexVerbWithNegationPast.matcher(word);
                if (m.matches() && m.group(1).length() > 1 && !msaWords.containsKey(word))
                {
                    if (verbs.containsKey(m.group(1)))
                        bw.write(word + "\n");
                    else if (m.group(2) != null && verbs.containsKey(m.group(0) + "ل"))
                        bw.write(word + "\n");
                }
                m = regexVerbWithNegationPresent.matcher(word);
                if (m.matches() && m.group(1).length() > 1 && !msaWords.containsKey(word))
                {
                    if (verbs.containsKey(m.group(1)))
                        bw.write(word + "\n");
                    else if (m.group(2) != null && verbs.containsKey(m.group(0) + "ل"))
                        bw.write(word + "\n");
                }
                /*
                m = regexVerbWithoutNegationPresent.matcher(word);
                if (m.matches() && m.group(1).length() > 1 && !msaWords.containsKey(word))
                {
                    if (verbs.containsKey(m.group(1)))
                        bw.write(word + "\n");
                    else if (m.group(2) != null && verbs.containsKey(m.group(0) + "ل"))
                        bw.write(word + "\n");
                }
                */
//                if (word.matches(regexVerbWithNegationPresent))
//                    bw.write(word + "\n");
//                if (word.matches(regexVerbWithoutNegationPresent))
//                    bw.write(word + "\n");
            }
        }
    }
    
    
    public static void expandDialectalWordsWithNegation() throws FileNotFoundException, IOException
    {
        BufferedReader brWords = openFileForReading("/work/ARABIZI/DIACRITIZE/tweets-unique-words.txt");
        String line = "";
        HashMap<String, Float> rawForm = new HashMap<String, Float>();
        while ((line = brWords.readLine()) != null) {
            String[] parts = line.trim().split("[\\s\t]+");
            if (parts.length == 2) {
                if (parts[1].trim().matches("[0-9\\.\\-]+") && parts[0].trim().length() > 0) {
                    rawForm.put(processdiacritizedlexicon.ArabicUtils.normalizeFull(parts[0]).trim(), Float.parseFloat(parts[1]));
                }
            }
        }
        
        BufferedReader brMSA = openFileForReading("/work/ARABIZI/DIACRITIZE/normalized-msa-unigrams.lm");
        HashMap<String, Float> msaWords = new HashMap<String, Float>();
        while ((line = brMSA.readLine()) != null)
        {
            String[] parts = line.split("\t");
            
            if (parts.length == 2 && parts[0].matches("[0-9\\-\\.]+"))
            {
                String word = parts[1];
            float score = Float.parseFloat(parts[0]);
            if (score >= -8)
                msaWords.put(word, score);
            }
        }
        
        String inputFile = "/work/ARABIZI/DIACRITIZE/diacritizedWords.dictionary.1.0";
        BufferedWriter bw = openFileForWriting(inputFile + ".expand");
        BufferedReader br = openFileForReading(inputFile);
        while ((line = br.readLine()) != null)
        {
            String[] parts = line.trim().split("\t");
            if (parts.length == 2)
            {
                String word = processdiacritizedlexicon.ArabicUtils.normalizeFull(parts[0]);
                if (!msaWords.containsKey(word))
                    bw.write(word + "\n");
                
                ArrayList<String> expansion = getNegationFormsDialectal(word, rawForm);
                for (String exp : expansion)
                    if (!msaWords.containsKey(exp))
                        bw.write(exp + "\n");
                
                expansion = getAttatchedPPFormsDialectal(word, rawForm);
                for (String exp : expansion)
                {
                    bw.write(exp + "\n");
                    ArrayList<String> expansion2 = getNegationFormsDialectal(exp, rawForm);
                    for (String exp2 : expansion2)
                        if (!msaWords.containsKey(exp2))
                            bw.write(exp2 + "\n");
                    expansion2 = getNegationFormsDialectal(exp + "ي", rawForm);
                    for (String exp2 : expansion2)
                        if (!msaWords.containsKey(exp2))
                            bw.write(exp2 + "\n");
                    expansion2 = getNegationFormsDialectal(exp + "و", rawForm);
                    for (String exp2 : expansion2)
                        if (!msaWords.containsKey(exp2))
                            bw.write(exp2 + "\n");
                }
                
                
            }
        }
        
        bw.close();
        br.close();
    }

    public static ArrayList<String> getLetterSubstitutionFormsDialectal(String word, HashMap<String, Float> rawForm)
    {
        ArrayList<String> output = new ArrayList<String>();
        if (word.contains("ث") && rawForm.containsKey(word.replace("ث", "ت")))
            output.add(word.replace("ث", "ت"));
        if (word.contains("ئ") && rawForm.containsKey(word.replace("ئ", "ي")))
            output.add(word.replace("ئ", "ي"));
        if (word.contains("ذ") && rawForm.containsKey(word.replace("ذ", "د")))
            output.add(word.replace("ذ", "د"));
        if (word.contains("ض") && rawForm.containsKey(word.replace("ض", "ظ")))
            output.add(word.replace("ض", "ظ"));
        return output;
    }
    
    public static ArrayList<String> getNegationFormsDialectal(String word, HashMap<String, Float> rawForm)
    {
        ArrayList<String> output = new ArrayList<String>();
        if (rawForm.containsKey("م" + word + "ش"))
            output.add("م" + word + "ش");
        if (rawForm.containsKey("ما" + word + "ش"))
            output.add("ما" + word + "ش");
        if (rawForm.containsKey(word + "ش"))
            output.add(word + "ش");
        return output;
    }
    
    public static ArrayList<String> getAttatchedPPFormsDialectal(String word, HashMap<String, Float> rawForm)
    {
        ArrayList<String> output = new ArrayList<String>();
        if (rawForm.containsKey(word + "لي"))
            output.add(word + "لي");
        if (rawForm.containsKey(word + "له"))
            output.add(word + "له");
        if (rawForm.containsKey(word + "لك"))
            output.add(word + "لك");
        if (rawForm.containsKey(word + "لكو"))
            output.add(word + "لكو");
        if (rawForm.containsKey(word + "لو"))
            output.add(word + "لو");
        
        
        if (word.endsWith("ل") && rawForm.containsKey(word + "ي"))
            output.add(word + "ي");
        if (word.endsWith("ل") && rawForm.containsKey(word + "ه"))
            output.add(word + "ه");
        if (word.endsWith("ل") && rawForm.containsKey(word + "ك"))
            output.add(word + "ك");
        if (word.endsWith("ل") && rawForm.containsKey(word + "كو"))
            output.add(word + "كو");
        if (word.endsWith("ل") && rawForm.containsKey(word + "و"))
            output.add(word + "و");
        
        if (rawForm.containsKey(word + "ني"))
            output.add(word + "ني");
        if (rawForm.containsKey(word + "كي"))
            output.add(word + "كي");
        if (rawForm.containsKey(word + "كم"))
            output.add(word + "كم");
        if (rawForm.containsKey(word + "كو"))
            output.add(word + "كو");
        if (rawForm.containsKey(word + "هم"))
            output.add(word + "هم");
        return output;
    }
    
    public static void getMostLikelyTemplateForEachWord() throws FileNotFoundException, IOException
    {
        // first step is to count all the templates generating a seen word
        // key is template without diacritics, value is the most commonly seen tempalate with diacritics
        
        // count diacritized templates
        HashMap<String, Integer> diacritizedTemplateCount = new HashMap<String, Integer>();
        HashMap<String, String> wordTemplate = new HashMap<String, String>();
        BufferedReader br = openFileForReading(dataPath+"/diacritizedWords.txt.1.0");
        String line = "";
        while ((line = br.readLine()) != null)
        {
            String[] parts = line.split("\t");
            if (!parts[2].contains("§"))
            {
                if (diacritizedTemplateCount.containsKey(parts[2]))
                {
                    diacritizedTemplateCount.put(parts[2], diacritizedTemplateCount.get(parts[2]) + 1);
                }
                else
                {
                    diacritizedTemplateCount.put(parts[2], 1);
                }
            }
            String bareWord = processdiacritizedlexicon.ArabicUtils.removeDiacritics(parts[0]);
            parts[2] = parts[2].replace("§", "");
            if (wordTemplate.containsKey(bareWord))
                wordTemplate.put(bareWord, wordTemplate.get(bareWord) + ";" + parts[2] + "\t" + parts[0]);
            else
                wordTemplate.put(bareWord, parts[2] + "\t" + parts[0]);
        }
        
        // make an inventory of non-diacritized to diacritized templates
//        HashMap<String, String> bareTemplateCount = new HashMap<String, String>();
//        
//        for (String s : diacritizedTemplateCount.keySet())
//        {
//            String bareTemplate = s.replaceAll("[aiouNKF~]+", "");
//            if (bareTemplateCount.containsKey(bareTemplate))
//                bareTemplateCount.put(bareTemplate, bareTemplateCount.get(bareTemplate) + ";" + s);
//            else
//                bareTemplateCount.put(bareTemplate, s);
//            // remove diacritic from word
//            
//        }
        
        // now read each word and assign the most likely diacritization based on the most likely template
        // dump into file
        BufferedWriter bw = openFileForWriting(dataPath+"/diacritizedWords.dictionary.template.1.0");
        for (String word : wordTemplate.keySet())
        {
            String[] templates = wordTemplate.get(word).split(";");
            int count = 0;
            String diacritizedForm = "";
            for (String template : templates)
            {
                String t = template.replaceFirst("\t.*", "");
                
                if (diacritizedTemplateCount.containsKey(t) && count < diacritizedTemplateCount.get(t))
                {
                    diacritizedForm = template.replaceFirst(".*\t", "");
                    count = diacritizedTemplateCount.get(t);
                }
            }
            if (diacritizedForm.trim().length() > 0)
                bw.write(word + "\t" + diacritizedForm + "\n");
        }
        bw.close();
    }
    
    public static void removeCaseEndingFromCorpus(String inputFile, String outputFile) throws FileNotFoundException, IOException, ClassNotFoundException, Exception
    {
        // BufferedReader brDict = openFileForReading(dataPath+"/all-text.txt.diacritized.filtered.uni.arpa.words.tok.stem.map");
        
//        TMap<String, String> dictionary = new THashMap<String, String>();
//        
        String line = "";
//        while ((line = brDict.readLine()) != null)
//        {
//            String[] parts = line.split("\t");
//            if (parts.length == 2)
//            {
//                dictionary.put(parts[0], standardizeDiacritics(parts[1]));
//            }
//        }
        
        BufferedReader br = openFileForReading(inputFile);
        // BufferedReader br = openFileForReading(dataPath+"/qtl.txt");
        String stemExtension = "";

        BufferedWriter bw = openFileForWriting(outputFile);
        
        while ((line = br.readLine()) != null)
        {
            String[] words = line.split(" +");
            for (String w : words)
            {
                ArrayList<String> parts = stemmer.tag(w, true, false);
                String stem = "";
                for (String s : parts)
                {
                    if (!s.equals("_"))
                    {
                        if (stem.trim().length() > 0)
                            stem += "+";
                        stem += s;
                    }
                }
                String diacritizedAndStemmed = transferDiacriticsFromWordToSegmentedVersion(w.trim(), stem.trim());
                bw.write(standardizeDiacritics(removeCaseEnding(diacritizedAndStemmed).replace("+", "")) + " ");
            }
            bw.write("\n");
            bw.flush();
        }
        bw.close();
    }
    
    public static void removeCaseEndingFromWordList(boolean stemWords) throws FileNotFoundException, IOException
    {
        String prefixes[] = {
            // "ال", "و", "ف", "ب", "ك", "ل", "لل"
            "\u0627\u0644", "\u0648", "\u0641", "\u0628", "\u0643", "\u0644", "\u0644\u0644", "س"
        };

        String suffixes[] = {
            // "ه", "ها", "ك", "ي", "هما", "كما", "نا", "كم", "هم", "هن", "كن",
            // "ا", "ان", "ين", "ون", "وا", "ات", "ت", "ن", "ة", "و"
            "\u0647", "\u0647\u0627", "\u0643", "\u064a", "\u0647\u0645\u0627", "\u0643\u0645\u0627", "\u0646\u0627", "\u0643\u0645", "\u0647\u0645", "\u0647\u0646", "\u0643\u0646",
            "\u0627", "\u0627\u0646", "\u064a\u0646", "\u0648\u0646", "\u0648\u0627", "\u0627\u062a", "\u062a", "\u0646", "\u0629", "و"
        };
        
        HashMap<String, Integer> hPrefixes = new HashMap<String, Integer>();
        HashMap<String, Integer> hSuffixes = new HashMap<String, Integer>();
        
        for (String p : prefixes)
        {
            hPrefixes.put(p, 1);
        }
        for (String s : suffixes)
        {
            hSuffixes.put(s, 1);
        }
        BufferedReader br = openFileForReading(dataPath+"/all-text.txt.diacritized.filtered.uni.arpa.words.tok");
        // BufferedReader br = openFileForReading(dataPath+"/temp.txt");
        // BufferedReader br = openFileForReading(dataPath+"/qtl.txt");
        String stemExtension = "";
        if (stemWords)
            stemExtension = ".stem";
        BufferedWriter bw = openFileForWriting(dataPath+"/all-text.txt.diacritized.filtered.uni.arpa.words.tok" 
                + stemExtension + ".map");
        
        String line = "";
        String lastDiacriticRegex = "[" + processdiacritizedlexicon.ArabicUtils.buck2utf8("aiou") + "]+$";
        while ((line = br.readLine()) != null)
        {
            // reattach word starting from suffixes
            // remove diacritic of the stem
            String [] parts = line.split("\t");
            if (parts.length == 2)
            {
                String diacritizedAndStemmed = transferDiacriticsFromWordToSegmentedVersion(parts[0].trim(), parts[1].trim());
                if (stemWords)
                    bw.write(parts[0] + "\t" + standardizeDiacritics(removeCaseEnding(removeAffixes(diacritizedAndStemmed))) + "\n");
                else
                    bw.write(parts[0] + "\t" + standardizeDiacritics(removeCaseEnding(diacritizedAndStemmed).replace("+", "")) + "\n");
            }
        }
        bw.close();
        
    }
    
    public static String removeCaseEnding(String diacritizedAndStemmed) 
    {
        String lastDiacriticRegex = "[" + processdiacritizedlexicon.ArabicUtils.buck2utf8("aiou") + "]+$";
        if (diacritizedAndStemmed.contains("+")) {
            
            // diacritize prefixes
            String finalDiacritizedForm = "";
            String finalDiacritizedFormPrefix = "";
            int pos = diacritizedAndStemmed.indexOf("+");
            while (pos > 0) {
                String prefix = ArabicUtils.removeDiacritics(diacritizedAndStemmed.substring(0, pos));
                String prefixOrg = diacritizedAndStemmed.substring(0, pos);
                if (hPrefixes.containsKey(prefix))
                {
                    diacritizedAndStemmed = diacritizedAndStemmed.substring(pos + 1);
                    // find next clitic -- this is to handle the case of having a prefix starting with l
                    String nextClitic = "";
                    if (diacritizedAndStemmed.indexOf("+") >= 0)
                        nextClitic = ArabicUtils.removeDiacritics(
                                diacritizedAndStemmed.substring(0, diacritizedAndStemmed.indexOf("+")));
                    else
                        nextClitic = ArabicUtils.removeDiacritics(diacritizedAndStemmed);
                    if (prefix.equals("ل") && diacritizedSuffixes.containsKey(nextClitic) && prefixOrg.length() == 1)
                    {
                        finalDiacritizedFormPrefix += "ل" + processdiacritizedlexicon.ArabicUtils.buck2utf8("a") + "+";
                    }
                    else if (diacritizedPrefixes.containsKey(prefix))
                    {
                        if (prefixOrg.length() > 1) // if l already has a diacritic
                            finalDiacritizedFormPrefix += prefixOrg + "+";
                        else
                        {
                            /*
                            if (prefixOrg.matches(".*[" + processdiacritizedlexicon.ArabicUtils.buck2utf8("aiou~NKF") + "]+.*"))
                                 finalDiacritizedFormPrefix += prefixOrg + "+";
                            else
                            */
                                finalDiacritizedFormPrefix += diacritizedPrefixes.get(prefix) + "+";
                        }
                    }
                    else
                    {
                        finalDiacritizedFormPrefix += prefix + "+";
                    }
                    pos = diacritizedAndStemmed.indexOf("+");
                }
                else
                    pos = -1;
            }

            
            // find last suffix
            String finalDiacritizedFormSuffix = "";
            String lastSuffix = "";
            pos = diacritizedAndStemmed.lastIndexOf("+");
            while (pos > 0) {
                String lastClitic = diacritizedAndStemmed.substring(pos).replace("+", "");
                if (ArabicUtils.removeDiacritics(lastClitic).equals("ت") || ArabicUtils.removeDiacritics(lastClitic).equals("ة")) {
                    diacritizedAndStemmed = diacritizedAndStemmed.substring(0, pos);
                    // this should be the end
                    lastClitic = ArabicUtils.removeDiacritics(lastClitic);
                    finalDiacritizedFormSuffix = "+" + lastClitic + finalDiacritizedFormSuffix;
                    lastSuffix = lastClitic;
                    pos = -1;
                } else if (hSuffixes.containsKey(ArabicUtils.removeDiacritics(lastClitic))) {
                    diacritizedAndStemmed = diacritizedAndStemmed.substring(0, pos);
                    // diacritize the suffix
                    // lastClitic = ArabicUtils.removeDiacritics(lastClitic);
                    lastSuffix = ArabicUtils.removeDiacritics(lastClitic); // lastClitic;
                    /* if (lastClitic.matches(".*[" + processdiacritizedlexicon.ArabicUtils.buck2utf8("aiou~NKF") + "]+.*"))
                    {
                        // do nothing
                    }
                    else */ 
                        if (diacritizedSuffixes.containsKey(lastClitic))
                            lastClitic = diacritizedSuffixes.get(lastClitic);
                        else
                            lastClitic = ArabicUtils.removeDiacritics(lastClitic);
                    // suffix into final diacritized form
                    finalDiacritizedFormSuffix = "+" + lastClitic + finalDiacritizedFormSuffix;
                    pos = diacritizedAndStemmed.lastIndexOf("+");
                }
                else
                    pos = -1;
            }
            
            diacritizedAndStemmed = diacritizedAndStemmed.replace("+", "");
            if (lastSuffix.equals("ة") || lastSuffix.equals("ت"))
                finalDiacritizedForm = finalDiacritizedFormPrefix + 
                        diacritizedAndStemmed + 
                        finalDiacritizedFormSuffix;
            else
                finalDiacritizedForm = finalDiacritizedFormPrefix + 
                        diacritizedAndStemmed.replaceFirst(lastDiacriticRegex, "") + 
                        finalDiacritizedFormSuffix;
            // finalDiacritizedForm = finalDiacritizedForm.replace("+", "");
            return finalDiacritizedForm;
        } else {
            // remove last diacritic
            diacritizedAndStemmed = diacritizedAndStemmed.replaceFirst(lastDiacriticRegex, "");
            return diacritizedAndStemmed;
        }

    }
    
    public static String transferDiacriticsFromWordToSegmentedVersion(String diacritizedWord, String stemmedWord)
    {
        // System.err.println(diacritizedWord + "\t" + stemmedWord);
        // if (stemmedWord.contains("رسول"))
        //    System.err.println(diacritizedWord + "\t" + stemmedWord);
        boolean startsWithLamLam = false;
        boolean startsWithWaLamLam = false;
        boolean startsWithFaLamLam = false;
        if (
                (stemmedWord.startsWith("ل+ال") && // !ArabicUtils.removeDiacritics(diacritizedWord).equals(stemmedWord.replace("+", "").replace(";", "")))
                ArabicUtils.removeDiacritics(diacritizedWord).startsWith("لل"))
                ||
                (stemmedWord.startsWith("و+ل+ال") && // !ArabicUtils.removeDiacritics(diacritizedWord).equals(stemmedWord.replace("+", "").replace(";", "")))
                ArabicUtils.removeDiacritics(diacritizedWord).startsWith("ولل"))
                ||
                (stemmedWord.startsWith("ف+ل+ال") && // !ArabicUtils.removeDiacritics(diacritizedWord).equals(stemmedWord.replace("+", "").replace(";", "")))
                ArabicUtils.removeDiacritics(diacritizedWord).startsWith("فلل"))
                )
        {
            // startsWithLamLam = true;
            int posFirstLam = diacritizedWord.indexOf("ل", 0);
            int posSecondLam = diacritizedWord.indexOf("ل", posFirstLam + 1);
            diacritizedWord = diacritizedWord.substring(0, posSecondLam) + "ا" + diacritizedWord.substring(posSecondLam);
        }
//        else if (stemmedWord.startsWith("و+ل+ال") && !ArabicUtils.removeDiacritics(diacritizedWord).equals(stemmedWord.replace("+", "").replace(";", "")))
//        {
//            startsWithWaLamLam = true;
//            stemmedWord = "ولل" + stemmedWord.substring(6);
//        }
//        else if (stemmedWord.startsWith("ف+ل+ال") && !ArabicUtils.removeDiacritics(diacritizedWord).equals(stemmedWord.replace("+", "").replace(";", "")))
//        {
//            startsWithFaLamLam = true;
//            stemmedWord = "فلل" + stemmedWord.substring(6);
//        }
        
        String output = "";
        // الْممَسّ  و+ال+ممس 
        // stemmedWord = stemmedWord.replaceFirst("^ل\\+ال", "لل");
        stemmedWord = stemmedWord.replace(" ", "");
        stemmedWord = stemmedWord.replaceFirst("\\+$", "");
        if (diacritizedWord.equals(stemmedWord) || !stemmedWord.contains("+"))
            return diacritizedWord;
        
        int pos = 0;
        for (int i = 0; i < stemmedWord.length(); i++)
        {
            if (stemmedWord.substring(i, i+1).equals("+") || stemmedWord.substring(i, i+1).equals(";"))
            {
                /*
                if (i + 1 < stemmedWord.length())
                {
                    if (stemmedWord.substring(i + 1, i + 1).equals("+"))
                    {
                        output += stemmedWord.substring(i, i+1);
                        i++;
                    }
                    int loc = diacritizedWord.indexOf(stemmedWord.substring(i+1, i+2), pos);
                    if (loc >= 0)
                    {
                        String diacritics = diacritizedWord.substring(pos, loc);
                        output += diacritics + stemmedWord.substring(i, i+1);
                        pos = loc;
                    }
                    else
                    {
                        output += stemmedWord.substring(i, i+1);
                    }
                }
                else */
                {
                    output += stemmedWord.substring(i, i+1);
                }
            }
            else
            {
                int loc = diacritizedWord.indexOf(stemmedWord.substring(i, i+1), pos);
                if (loc >= 0)
                {
                    String diacritics = diacritizedWord.substring(pos, loc);
                    output += diacritics + stemmedWord.substring(i, i+1);
                    // add trailing diacritics
                    loc++;
                    while (loc < diacritizedWord.length() && diacritizedWord.substring(loc, loc + 1).matches("[" + 
                           processdiacritizedlexicon.ArabicUtils.buck2utf8("aiouNKF~") + "]"))
                    {
                        output += diacritizedWord.substring(loc, loc + 1);
                        loc++;
                    }
                    pos = loc;
                }
                else
                {
                    // System.err.println(diacritizedWord + "\t" + stemmedWord);
                }
            }
        }
//        if (startsWithLamLam)
//        {
//            // split lam lam in the begining of words
//            // find the beginning of the word
//            int posLetter = output.indexOf("+", output.indexOf("ل", 1));
//            String outputWithoutLamLam = output.substring(posLetter + 1);
//            // remove head
//            outputWithoutLamLam = outputWithoutLamLam.replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
//            output = ArabicUtils.buck2utf8("li") + "+" + ArabicUtils.buck2utf8("Alo") + "+" + outputWithoutLamLam;
//        }
//        else if (startsWithWaLamLam)
//        {
//            int posLetter = output.indexOf("+");
//            String outputWithoutLamLam = output.substring(posLetter + 1);
//            // find the different letters
//            int posFirstLam = output.indexOf("ل", 1);
//            int posSecondLam = output.indexOf("ل", posFirstLam + 1);
//            int posPlus = output.indexOf("+");
//            outputWithoutLamLam = outputWithoutLamLam.replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
//            output = ArabicUtils.buck2utf8("w") + output.substring(1, posFirstLam) + "+" + ArabicUtils.buck2utf8("l") 
//                    + output.substring(posFirstLam + 1, posSecondLam) + "+" + ArabicUtils.buck2utf8("Al")  + output.substring(posSecondLam + 1, posPlus) + "+" + outputWithoutLamLam;
//        }
//        else if (startsWithFaLamLam)
//        {
//            int posLetter = output.indexOf("+");
//            String outputWithoutLamLam = output.substring(posLetter + 1);
//            // remove head
//            int posFirstLam = output.indexOf("ل", 1);
//            int posSecondLam = output.indexOf("ل", posFirstLam + 1);
//            int posPlus = output.indexOf("+");
//            outputWithoutLamLam = outputWithoutLamLam.replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
//            output = ArabicUtils.buck2utf8("f") + output.substring(1, posFirstLam) + "+" + ArabicUtils.buck2utf8("l") 
//                    + output.substring(posFirstLam + 1, posSecondLam) + "+" + ArabicUtils.buck2utf8("Al")  + output.substring(posSecondLam + 1, posPlus) + "+" + outputWithoutLamLam;
//        }
        return output;
    }
    
    public static void generateChoicesForLexiconFile() throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, Exception {
        DiacritizeText dt = new DiacritizeText("", "", "", dataPath+"/diacritizedWords.dictionary.1.0");
         // BufferedReader br = openFileForReading("/work/ARABIZI/DIACRITIZE/coda.uniq");
        // BufferedWriter bw = openFileForWriting("/work/ARABIZI/DIACRITIZE/coda.uniq.out.tmp");

        BufferedReader br = openFileForReading("/work/ARABIZI/DIACRITIZE/missing-words.txt");
        BufferedWriter bw = openFileForWriting("/work/ARABIZI/DIACRITIZE/missing-words.txt.out");

        String line = "";
        while ((line = br.readLine()) != null) {
            if (line.trim().length() > 0) {
                ArrayList<String> word = new ArrayList<String>();
                word.add(ArabicUtils.buck2utf8(line));
                HashMap<Integer, ArrayList<String>> latice = dt.buildLaticeStem(word);
                bw.write(line + "\t");
                for (String s : latice.get(1)) // bw.write(recoverDiacriticsLostDuringStandarization(ArabicUtils.utf82buck(s)) + ";");
                {
                    bw.write(recoverDiacriticsLostDuringStandarization(s) + ";");
                }
                bw.write("\n");
            }
        }

        bw.close();
    }

    public static void generateChoicesForLexiconFileWithBackoff() throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, Exception {
        HashMap<Integer, DiacritizeText> dts = new HashMap<Integer, DiacritizeText>();
        dts.put(0, new DiacritizeText("", "", "", dataPath+"/diacritizedWords.dictionary.1.0"));
        dts.put(1, new DiacritizeText("", "", "", "/work/ARABIZI/DIACRITIZE/diacritizedWords.dictionary.1.0"));
        dts.put(2, new DiacritizeText("", "", "", "/work/ARABIZI/DIACRITIZE/diacritizedWords.dictionary.from-text"));
        
        // DiacritizeText dt = new DiacritizeText("", "", "");
         // BufferedReader br = openFileForReading("/work/ARABIZI/DIACRITIZE/coda.uniq");
        // BufferedWriter bw = openFileForWriting("/work/ARABIZI/DIACRITIZE/coda.uniq.out.tmp");

        // BufferedReader br = openFileForReading("/work/ARABIZI/DIACRITIZE/missing-words.txt");
        // BufferedWriter bw = openFileForWriting("/work/ARABIZI/DIACRITIZE/missing-words.txt.out");
        
        BufferedReader br = openFileForReading("/work/ARABIZI/DIACRITIZE/mada_failed.txt");
        BufferedWriter bw = openFileForWriting("/work/ARABIZI/DIACRITIZE/mada_failed.txt.out");

        String line = "";
        while ((line = br.readLine()) != null) {
            if (line.trim().length() > 0) {
                ArrayList<String> word = new ArrayList<String>();
                word.add(ArabicUtils.buck2utf8(line));
                // run the lattice builder in succession, stop when results are found
                HashMap<Integer, HashMap<Integer, ArrayList<String>>> lattices = new HashMap<Integer, HashMap<Integer, ArrayList<String>>>();
                for (int i = 0; i < dts.size(); i++)
                {
                    DiacritizeText dt = dts.get(i);
                    HashMap<Integer, ArrayList<String>> lattice = dt.buildLaticeStem(word);
                    lattices.put(i, lattice);
                }
                
                // bw.write(line + "\t");
                int printedOut = 0;
                for (int i = 0; i < dts.size(); i++)
                {
                    if (printedOut == 0)
                    {
                        HashMap<Integer, ArrayList<String>> lattice = lattices.get(i);
                        String marker = "";
                        for (int j = 0; j < i; j++)
                            marker += "@";
                        for (String s : lattice.get(1)) // bw.write(recoverDiacriticsLostDuringStandarization(ArabicUtils.utf82buck(s)) + ";");
                        {
                            // bw.write(recoverDiacriticsLostDuringStandarizationUTF8(s) + marker + ";");
                            if (s.contains("§"))
                            {
                                bw.write(line + "\t" + recoverDiacriticsLostDuringStandarizationUTF8(s) + "\n");
                                printedOut = 1;
                            }
                        }
                    }
                }
                if (printedOut == 0)
                {
                    HashMap<Integer, ArrayList<String>> lattice = lattices.get(0);
                    for (String s : lattice.get(1)) // bw.write(recoverDiacriticsLostDuringStandarization(ArabicUtils.utf82buck(s)) + ";");
                        bw.write(line + "\t" + recoverDiacriticsLostDuringStandarizationUTF8(s.replace("§", "")) + "\n");
                }
                // bw.write("\n");
            }
        }

        bw.close();
    }

    public static String recoverDiacriticsLostDuringStandarization(String input) {
        String output = "";

        input = processdiacritizedlexicon.ArabicUtils.utf82buck(input);
        
        for (int i = 0; i < input.length(); i++) {
            if (input.substring(i, i + 1).equals("A")
                    || input.substring(i, i + 1).equals("w")
                    || input.substring(i, i + 1).equals("y")) {
                if (i > 0
                        && !input.substring(i - 1, i).equals("a")
                        && !input.substring(i - 1, i).equals("i")
                        && !input.substring(i - 1, i).equals("o")
                        && !input.substring(i - 1, i).equals("u")) {
                    if (input.substring(i, i + 1).equals("A") 
                            && !input.substring(i-1, i).equals("w") && !input.substring(i-1, i).equals("y")
                            ) {
                        output += "a";
                    } else if (input.substring(i, i + 1).equals("w")
                             && !input.substring(i-1, i).equals("A") && !input.substring(i-1, i).equals("y")
                            ) {
                        output += "u";
                    } else if (input.substring(i, i + 1).equals("y")
                             && !input.substring(i-1, i).equals("w") && !input.substring(i-1, i).equals("A")
                            ) {
                        output += "i";
                    }
                }
                output += input.substring(i, i + 1);
            } else {
                output += input.substring(i, i + 1);
            }
        }

        String regexToRemoveLeadingDiacritic = "^[aiouNKF~]+";
        output = output.replaceFirst(regexToRemoveLeadingDiacritic, "");
        output = processdiacritizedlexicon.ArabicUtils.buck2utf8(output);
        return output;
    }

    public static String recoverDiacriticsLostDuringStandarizationUTF8(String input) {
        String output = "";

        for (int i = 0; i < input.length(); i++) {
            if (input.substring(i, i + 1).equals("ا")
                    || input.substring(i, i + 1).equals("و")
                    || input.substring(i, i + 1).equals("ي")) {
                if (i > 0
                        && !input.substring(i - 1, i).equals(processdiacritizedlexicon.ArabicUtils.buck2utf8("a"))
                        && !input.substring(i - 1, i).equals(processdiacritizedlexicon.ArabicUtils.buck2utf8("i"))
                        && !input.substring(i - 1, i).equals(processdiacritizedlexicon.ArabicUtils.buck2utf8("o"))
                        && !input.substring(i - 1, i).equals(processdiacritizedlexicon.ArabicUtils.buck2utf8("u"))) {
                    if (input.substring(i, i + 1).equals("ا")
                            && !input.substring(i-1, i).equals("و") && !input.substring(i-1, i).equals("ي")
                            ) {
                        output += processdiacritizedlexicon.ArabicUtils.buck2utf8("a");
                    } else if (input.substring(i, i + 1).equals("و")
                            && !input.substring(i-1, i).equals("ا") && !input.substring(i-1, i).equals("ي")
                            ) {
                        output += processdiacritizedlexicon.ArabicUtils.buck2utf8("u");
                    } else if (input.substring(i, i + 1).equals("ي")
                            && !input.substring(i-1, i).equals("و") && !input.substring(i-1, i).equals("ا")
                            ) {
                        output += processdiacritizedlexicon.ArabicUtils.buck2utf8("i");
                    }
                }
                output += input.substring(i, i + 1);
            } else {
                output += input.substring(i, i + 1);
            }
        }

        String regexToRemoveLeadingDiacritic = "^[" + processdiacritizedlexicon.ArabicUtils.buck2utf8("aiouNKF~") + "]+";
        output = output.replaceFirst(regexToRemoveLeadingDiacritic, "");
        output = output.replace(processdiacritizedlexicon.ArabicUtils.buck2utf8("bia"), processdiacritizedlexicon.ArabicUtils.buck2utf8("bi"));
        return output;
    }
    
    public static void buildDictionaryFrom_generateDiacritizedWordsAndCheckWithAffixes_output() throws FileNotFoundException, IOException {
        BufferedReader br = openFileForReading(dataPath+"/diacritizedWords.affix.txt");
        // BufferedReader br = openFileForReading(dataPath+"/diacritizedWords.txt.1.0");
        TMap<String, ArrayList<String>> dictionary = new THashMap<String, ArrayList<String>>();
        String line = "";
        while ((line = br.readLine()) != null) {
            // line + "\t#" + "\t" + ss + "\t" + word + ArabicUtils.buck2utf8(sd) + ss + "\n"
            String[] parts = line.split("\t");
            if (parts.length == 7) {
                String word = parts[0];
                String prefix = parts[4];
                String suffix = parts[5];
                String surface = parts[6];
                String bareSurface = ArabicUtils.removeDiacritics(surface);
                String concatSurface = ArabicUtils.removeDiacritics(prefix + "+" + word + "+" + suffix);
                if (!dictionary.containsKey(bareSurface)) {
                    dictionary.put(bareSurface, new ArrayList<String>());
                }
                ArrayList<String> tmp = dictionary.get(bareSurface);
                String toInsert = standardizeDiacritics(surface);// + "," + concatSurface;
                if (!tmp.contains(toInsert)) {
                    tmp.add(toInsert);
                    dictionary.put(bareSurface, tmp);
                }
            }
        }

        BufferedWriter bw = openFileForWriting(dataPath+"/diacritizedWords.dictionary.1.0");
        for (String surface : dictionary.keySet()) {
            bw.write(surface + "\t");
            for (String choice : dictionary.get(surface)) {
                bw.write(";" + choice);
            }
            bw.write(";\n");
            bw.flush();
        }
        bw.close();
    }

    public static void buildDictionaryFrom_generateDiacritizedWordsAndCheck_output(String inputFile, String outputFile) throws FileNotFoundException, IOException {
        // BufferedReader br = openFileForReading(dataPath+"/diacritizedWords.affix.txt");
        BufferedReader br = openFileForReading(inputFile);
        TMap<String, ArrayList<String>> dictionary = new THashMap<String, ArrayList<String>>();
        String line = "";
        while ((line = br.readLine()) != null) {
            // line + "\t#" + "\t" + ss + "\t" + word + ArabicUtils.buck2utf8(sd) + ss + "\n"
            String[] parts = line.split("\t");
            if (parts.length > 1) {
                // String word = parts[0];
                // String prefix = parts[4];
                // String suffix = parts[5];
                String surface = parts[0];
                String bareSurface = ArabicUtils.removeDiacritics(surface);
                // String concatSurface = ArabicUtils.removeDiacritics(prefix + "+" + word + "+" + suffix);
                if (!dictionary.containsKey(bareSurface)) {
                    dictionary.put(bareSurface, new ArrayList<String>());
                }
                ArrayList<String> tmp = dictionary.get(bareSurface);
                String toInsert = standardizeDiacritics(surface);// + "," + concatSurface;
                if (!tmp.contains(toInsert)) {
                    tmp.add(toInsert);
                    dictionary.put(bareSurface, tmp);
                }
            }
        }

        BufferedWriter bw = openFileForWriting(outputFile);
        for (String surface : dictionary.keySet()) {
            bw.write(surface + "\t");
            for (String choice : dictionary.get(surface)) {
                bw.write(";" + choice);
            }
            bw.write(";\n");
            bw.flush();
        }
        bw.close();
    }

    public static void buildPrefixSuffixApplicabilityTable() throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException {
        BufferedReader br = openFileForReading(dataPath+"/all-text.txt.diacritized.full-tok.uni.count");
        BufferedWriter bw = openFileForWriting(dataPath+"/prefix-suffix-application.txt");

        HashMap<String, Integer> affix = new HashMap<String, Integer>();

        String line = "";
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\t");
            if (parts.length == 2 && parts[0].trim().length() > 0) {
                String w = ArabicUtils.removeDiacritics(parts[0]);
                int count = Integer.parseInt(parts[1]);
                // get prefixes
                ArrayList<String> prefixes = new ArrayList<String>();
                while (w.contains("+_")) {
                    String prefix = w.substring(0, w.indexOf("+_"));
                    w = w.substring(w.indexOf("+_") + 2);
                    prefixes.add(prefix);
                }
                if (prefixes.size() == 0) {
                    prefixes.add("#");
                }

                // get suffixes
                ArrayList<String> suffixes = new ArrayList<String>();
                while (w.contains("_+")) {
                    String suffix = w.substring(w.lastIndexOf("_+") + 2);
                    w = w.substring(0, w.lastIndexOf("_+"));
                    suffixes.add(suffix);
                }
                if (suffixes.size() == 0) {
                    suffixes.add("#");
                }

                for (String p : prefixes) {
                    for (String s : suffixes) {
                        if (hPrefixes.containsKey(p) && hSuffixes.containsKey(s)) {
                            String key = p + "_" + s;
                            if (affix.containsKey(key)) {
                                affix.put(key, affix.get(key) + count);
                            } else {
                                affix.put(key, count);
                            }
                        }
                    }
                }

            }
        }

        for (String ps : affix.keySet()) {
            bw.write(ps.replace("_", "\t") + "\t" + affix.get(ps) + "\n");
        }

        br.close();
        bw.close();
    }

    public static void generateDiacritizedWordsAndCheck(String templatesFile, String uniqueWordsFile, String outputFile) throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException {
        BufferedReader brRoots = openFileForReading("/Users/kareemdarwish/RESEARCH/SEBAWAI/sebawai/roots.txt");
        // BufferedReader brRoots = openFileForReading("/work/ARABIZI/DIACRITIZE/klm.txt");
        BufferedReader brTemplates = openFileForReading(templatesFile);
        // BufferedReader brWords = openFileForReading(dataPath+"/train.dictionary.super");
        BufferedReader brWords = openFileForReading(uniqueWordsFile);

        Float minScore = 1f;

        BufferedWriter bw = openFileForWriting(outputFile + Float.toString(minScore));

        ArrayList<String> roots = new ArrayList<String>();
        ArrayList<String> templates = new ArrayList<String>();
        // HashMap<String, Float> words = new HashMap<>();

        String line = "";
        while ((line = brRoots.readLine()) != null) {
            if (!line.endsWith("e-08"))
            {
                line = line.replaceFirst("\t.*", "").trim();
                if (line.length() == 3 || line.length() == 4) {
                    line = morph2buck(line);
                    if (!roots.contains(line)) {
                        roots.add(line);
                    }
                }
            }
        }

        line = "";
        while ((line = brTemplates.readLine()) != null) {
            // normalization is optional
            line = line.replace(">", "A").replace("<", "A").replace("|", "A").replace("Y", "y").replace("p", "h");
            //
            line = processdiacritizedlexicon.ArabicUtils.utf82buck(line.trim());
            line = line.replace("aA", "A").replace("uw", "w").replace("iy", "y");
            if (!templates.contains(line.trim()) && line.trim().length() > 0) {
                templates.add(line.trim());
                // System.out.println(line);
            }
        }

        HashMap<String, Float> rawForm = new HashMap<String, Float>();
        HashMap<String, Float> rawFormNoDiacritics = new HashMap<String, Float>();

        line = "";
        while ((line = brWords.readLine()) != null) {
            // normalization is optional
            line = processdiacritizedlexicon.ArabicUtils.normalizeFull(line);
            // 
            String[] parts = line.trim().split("[\\s\t]+");
            //if (line.contains("بيتكلم"))
            //    System.err.println();
            if (parts.length == 2) {
                if (parts[0].trim().equals("ابتزاز"))
                    System.err.println(line);
                if (parts[1].trim().matches("[0-9\\.\\-]+") && parts[0].trim().length() > 0) {
                    float score = Float.parseFloat(parts[1]);
                    if (score >= -2) {
                        String[] seen = parts[0].split(";");
                        for (String s : seen) {
                            if (s.trim().length() > 0) {
                                // words.put(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(s)).replaceFirst("[aiouNKF]+$", ""), score);
                                //if (rawForm.containsKey(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(s)).replaceFirst("[aiouNKF]+$", ""))) {
                                //    rawForm.put(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(s).replaceFirst("[aiouNKF]+$", "")),
                                //            rawForm.get(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(s)).replaceFirst("[aiouNKF]+$", "")) + score);
                                //} else {
                                    rawForm.put(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(s)).replaceFirst("[aiouNKF]+$", ""), score);
                                //}

                                //if (rawFormNoDiacritics.containsKey(processdiacritizedlexicon.ArabicUtils.utf82buck(ArabicUtils.removeDiacritics(s)))) {
                                //    rawFormNoDiacritics.put(processdiacritizedlexicon.ArabicUtils.utf82buck(ArabicUtils.removeDiacritics(s)), rawFormNoDiacritics.get(processdiacritizedlexicon.ArabicUtils.utf82buck(ArabicUtils.removeDiacritics(s))) + score);
                                //} else {
                                    rawFormNoDiacritics.put(processdiacritizedlexicon.ArabicUtils.utf82buck(ArabicUtils.removeDiacritics(s)), score);
                                //}

                            }
                        }
                    }
                }
            }
        }

        for (String root : roots) {
            for (String template : templates) {
                if (template.startsWith("bi"))
                    System.err.println(root + "\t" + template);

                if (root.length() == 3) {
                    // if (root.equals("ktb"))
                    //    System.err.println(root);
                    // hollow and repeated letter roots
                    // on going

                    // if (root.contains("y") || root.contains("w") || root.contains("A") || root.substring(1, 2).equals(root.substring(2)))
                    //{
                    // hollow letter in the middle
                    if (root.substring(1, 2).matches("[ywA]") && !template.contains("E") & template.matches(".*f.*l.*")) {
                        String w = template;
                        int pos = w.indexOf("f");
                        w = w.substring(0, pos) + root.substring(0, 1) + w.substring(pos + 1);
                        while (w.indexOf("l", pos + 1) >= 0) {
                            pos = w.indexOf("l", pos + 1);
                            w = w.substring(0, pos) + root.substring(2, 3) + w.substring(pos + 1);
                        }

                        if (rawForm.containsKey(w)) {
                            bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                            bw.flush();
                        } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                            bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                            bw.flush();
                        }
                    }
                    //}

                    // repeated letters
                    if (root.substring(1, 2).equals(root.substring(2, 3))) {
                        if (template.contains("L")) {
                            String w = template;
                            int pos = w.indexOf("f");
                            w = w.substring(0, pos) + root.substring(0, 1) + w.substring(pos + 1);
                            while (w.indexOf("L", pos + 1) >= 0) {
                                pos = w.indexOf("L", pos + 1);
                                w = w.substring(0, pos) + root.substring(1, 2) + w.substring(pos + 1);
                            }
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                        if (root.substring(2, 3).equals("y") && template.contains("f")) {
                            String w = template;
                            w = w.replace("f", root.substring(0, 1));
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                    }

                    // first letter is hamza/alef
                    if (root.substring(0, 1).equals("A")) {
                        if (template.matches(".*E.*l.*") && !template.contains("f")) {
                            String w = template;
                            int pos = w.indexOf("E");
                            w = w.substring(0, pos) + root.substring(1, 2) + w.substring(pos + 1);
                            while (w.indexOf("l", pos + 1) >= 0) {
                                pos = w.indexOf("l", pos + 1);
                                w = w.substring(0, pos) + root.substring(2, 3) + w.substring(pos + 1);
                            }
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                    }

                    // middle letter is hamza/alef
                    if (root.substring(1, 2).equals("A")) {
                        if (template.matches(".*f.*l.*") && !template.contains("E")) {
                            String w = template;
                            int pos = w.indexOf("f");
                            w = w.substring(0, pos) + root.substring(0, 1) + w.substring(pos + 1);
                            while (w.indexOf("l", pos + 1) >= 0) {
                                pos = w.indexOf("l", pos + 1);
                                w = w.substring(0, pos) + root.substring(2, 3) + w.substring(pos + 1);
                            }
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                    }

                    // first and last letters are ya or wa
                    if ((root.substring(0, 1).equals("y") || root.substring(0, 1).equals("w"))
                            && (root.substring(2, 3).equals("y") || root.substring(2, 3).equals("w"))) {
                        if (!template.contains("f") && !template.contains("l") && template.contains("E")) {
                            String w = template;
                            w = w.replace("E", root.substring(1, 2));
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                    }

                    // first letter is ya or wa
                    if (root.substring(0, 1).equals("y") || root.substring(0, 1).equals("w")) {
                        if (template.matches(".*E.*l.*") && !template.contains("f")) {
                            String w = template;
                            int pos = w.indexOf("E");
                            w = w.substring(0, pos) + root.substring(1, 2) + w.substring(pos + 1);
                            while (w.indexOf("l", pos + 1) >= 0) {
                                pos = w.indexOf("l", pos + 1);
                                w = w.substring(0, pos) + root.substring(2, 3) + w.substring(pos + 1);
                            }
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                    }

                    // last letter is a ya or wa
                    if (root.substring(2, 3).equals("y") || root.substring(2, 3).equals("w")) {
                        if (template.matches(".*f.*E.*") && !template.contains("l")) {
                            String w = template;
                            int pos = w.indexOf("f");
                            w = w.substring(0, pos) + root.substring(0, 1) + w.substring(pos + 1);
                            while (w.indexOf("E", pos + 1) >= 0) {
                                pos = w.indexOf("E", pos + 1);
                                w = w.substring(0, pos) + root.substring(1, 2) + w.substring(pos + 1);
                            }
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                    }

                    // last letter is an alef
                    if (root.substring(2, 3).equals("A")) {
                        if (template.matches(".*f.*E.*") && !template.contains("l")) {
                            String w = template;
                            int pos = w.indexOf("f");
                            w = w.substring(0, pos) + root.substring(0, 1) + w.substring(pos + 1);
                            while (w.indexOf("E", pos + 1) >= 0) {
                                pos = w.indexOf("E", pos + 1);
                                w = w.substring(0, pos) + root.substring(1, 2) + w.substring(pos + 1);
                            }
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                    }

                    // second and last letters are A, y, w ******
                    if (
                            (root.substring(1, 2).equals("w") || root.substring(1, 2).equals("y") || root.substring(1, 2).equals("A"))
                            &&
                            (root.substring(2, 3).equals("w") || root.substring(2, 3).equals("y") || root.substring(2, 3).equals("A"))
                            ) {
                        if (template.matches(".*f.*") && !template.contains("E") && !template.contains("l")) {
                            String w = template;
                            w = template.replace("f", root.substring(0, 1));
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                    }
                    
                    // first letter is hamza and last letter is w or y
                    if (root.substring(0, 1).equals("A") && (root.substring(2, 3).equals("w") || root.substring(2, 3).equals("y")))
                    {
                        if (template.matches(".*E.*") && !template.contains("f") && !template.contains("l")) {
                            String w = template;
                            w = template.replace("E", root.substring(1, 2));
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                    }
                    
                    // default case
                    {
                        if (template.matches(".*f.*E.*l.*")) {
                            String w = getWord(root, template);
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                            if (root.contains("A")) {
                                w = getWord(root.replace("A", "'"), template);
                                if (rawForm.containsKey(w)) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                }
                                w = getWord(root.replace("A", ">"), template);
                                if (rawForm.containsKey(w)) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                }
                                w = getWord(root.replace("A", "<"), template);
                                if (rawForm.containsKey(w)) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                }
                                w = getWord(root.replace("A", "&"), template);
                                if (rawForm.containsKey(w)) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                }
                                w = getWord(root.replace("A", "}"), template);
                                if (rawForm.containsKey(w)) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                }
                                w = getWord(root.replace("A", "|"), template);
                                if (rawForm.containsKey(w)) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                    bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                    bw.flush();
                                }
                            }
                        }
                        // handle special cases
                        if ((root.startsWith("D") || root.startsWith("S")) && template.matches(".*f[aiou~]+t.*")) {
                            String w = getWord(root, template.replaceFirst("t", "T"));
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                        else if ((root.startsWith("z") || root.startsWith("S")) && template.matches(".*f[aiou~]+t.*")) {
                            String w = getWord(root, template.replaceFirst("t", "d"));
                            if (rawForm.containsKey(w)) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                                bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                                bw.flush();
                            }
                        }
                    }
                }
                if (root.length() == 4) {
                    if (template.matches(".*f.*E.*l.*l.*")) {
                        String w = template;
                        int pos = w.indexOf("f");
                        w = w.substring(0, pos) + root.substring(0, 1) + w.substring(pos + 1);
                        while (w.indexOf("E", pos + 1) >= 0) {
                            pos = w.indexOf("E", pos + 1);
                            w = w.substring(0, pos) + root.substring(1, 2) + w.substring(pos + 1);
                        }
                        if (w.indexOf("l", pos + 1) >= 0) {
                            pos = w.indexOf("l", pos + 1);
                            w = w.substring(0, pos) + root.substring(2, 3) + w.substring(pos + 1);
                        }
                        if (w.indexOf("l", pos + 1) >= 0) {
                            pos = w.indexOf("l", pos + 1);
                            w = w.substring(0, pos) + root.substring(3, 4) + w.substring(pos + 1);
                        }
                        if (rawForm.containsKey(w)) {
                            // bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + "\n");
                            bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawForm.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                            bw.flush();
                        } else if (rawFormNoDiacritics.containsKey(w.replaceAll("[aiouNKF~]+", ""))) {
                            bw.write(processdiacritizedlexicon.ArabicUtils.buck2utf8(w) + "\t" + root + "\t" + template + "\t" + rawFormNoDiacritics.get(w) + getDifferentCaseEndings(rawForm, w) + "\n");
                            bw.flush();
                        }
                    }
                }
            }
        }

    }

    public static void generateDiacritizedWordsAndCheckWithAffixes() throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException {
        // this function checks if a particular prefix can be applied to a word
        BufferedReader brWords = openFileForReading(dataPath+"/all.uni.count.2");

        HashMap<String, Float> words = new HashMap<String, Float>();
        HashMap<String, Float> rawForm = new HashMap<String, Float>();
        Float minScore = 1.0f;
        String line = "";

        while ((line = brWords.readLine()) != null) {
            String[] parts = line.trim().split("[\\s\t]+");
            if (parts.length == 2) {
                // if (parts[0].trim().equals("مكتبة"))
                //    System.err.println(line);
                if (parts[1].trim().matches("[0-9\\.\\-]+") && parts[0].trim().length() > 0) {
                    float score = Float.parseFloat(parts[1]);
                    if (score >= minScore) {
                        String[] seen = parts[0].split(";");
                        for (String s : seen) {
                            if (s.trim().length() > 0) {
                                words.put(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(s)).replaceFirst("[aiouNKF]+$", ""), score);
                                if (rawForm.containsKey(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(s)).replaceFirst("[aiouNKF]+$", ""))) {
                                    rawForm.put(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(s)).replaceFirst("[aiouNKF]+$", ""), rawForm.get(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(s)).replaceFirst("[aiouNKF]+$", "")) + score);
                                } else {
                                    rawForm.put(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(s)).replaceFirst("[aiouNKF]+$", ""), score);
                                }
                            }
                        }
                    }
                }
            }
        }
        BufferedReader br = openFileForReading(dataPath+"/diacritizedWords.txt");
        BufferedWriter bw = openFileForWriting(dataPath+"/diacritizedWords.affix.txt");
        line = "";
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\t");
            if (parts.length == 4) {
                bw.write(line + "\t#\t#\t" + parts[0] + "\n");
                String word = parts[0];
                for (String s : hPrefixes.keySet()) {
                    String ss = "";
                    if (diacritizedPrefixes.containsKey(ArabicUtils.utf82buck(s))) {
                        ss = ArabicUtils.buck2utf8(diacritizedPrefixes.get(ArabicUtils.utf82buck(s)));
                    } else {
                        ss = s;
                    }
                    if (rawForm.containsKey(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(ss + word)))) {
                        bw.write(line + "\t" + ss + "\t#\t" + ss + word + "\n");
                    }
                }
                for (String s : hSuffixes.keySet()) {
                    String ss = "";
                    if (diacritizedSuffixes.containsKey(ArabicUtils.utf82buck(s))) {
                        ss = ArabicUtils.buck2utf8(diacritizedSuffixes.get(ArabicUtils.utf82buck(s)));
                    } else {
                        ss = s;
                    }
                    String[] diac = {"a", "i", "u", "o", ""};
                    for (String sd : diac) {
                        String key = "";
                        if (word.endsWith("ة")) // && sd.trim().length() > 0)
                        {
                            if (ss.equals("ات")) {
                                key = word.substring(0, word.length() - 1).replaceFirst("[ًٌٍَُِّ]+$", "") + ss;
                            } else {
                                key = word.substring(0, word.length() - 1) + "ت" + ArabicUtils.buck2utf8(sd) + ss;
                            }
                        } else if (word.endsWith("ى")) // && sd.trim().length() > 0)
                        {
                            key = word.substring(0, word.length() - 1) + "ي" + ArabicUtils.buck2utf8(sd) + ss;
                            key = ";" + word.substring(0, word.length() - 1) + "ا" + ArabicUtils.buck2utf8(sd) + ss;
                        } else {
                            key = word + ArabicUtils.buck2utf8(sd) + ss;
                        }
                        if (key.contains(";")) {
                            String[] keys = key.split(";");
                            for (String k : keys) {
                                if (rawForm.containsKey(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(k)))) {
                                    bw.write(line + "\t#" + "\t" + ss + "\t" + k + "\n");
                                }
                            }
                        } else {
                            if (rawForm.containsKey(processdiacritizedlexicon.ArabicUtils.utf82buck(standardizeDiacritics(key)))) {
                                bw.write(line + "\t#" + "\t" + ss + "\t" + key + "\n");
                            }
                        }
                    }
                }
            }
            bw.flush();
        }
        for (String p : hPrefixes.keySet()) {
            for (String s : hSuffixes.keySet()) {
                String prefix = p;
                if (diacritizedPrefixes.containsKey(ArabicUtils.utf82buck(p))) {
                    prefix = diacritizedPrefixes.get(ArabicUtils.utf82buck(p));
                }
                String suffix = s;
                if (diacritizedSuffixes.containsKey(ArabicUtils.utf82buck(s))) {
                    prefix = diacritizedSuffixes.get(ArabicUtils.utf82buck(s));
                }
                String key = ArabicUtils.buck2utf8(prefix + suffix);
                key = standardizeDiacritics(key);
                if (rawForm.containsKey(processdiacritizedlexicon.ArabicUtils.utf82buck(key))) {
                    bw.write(key + "\t" + ArabicUtils.buck2utf8(prefix) + "\t" + ArabicUtils.buck2utf8(suffix) + "\t" + key + "\n");
                }
            }
        }
        bw.flush();
        bw.close();
    }

    public static String getDifferentCaseEndings(HashMap<String, Float> rawForm, String w) {
        String choices = "";
        String[] diacritics = {"a", "i", "u", "o", "N", "K", "F"};
        for (String d : diacritics) {
            if (rawForm.containsKey(w + d)) {
                choices += "\t" + w + d + "\t" + rawForm.get(w + d);
            } else {
                choices += "\tx\tx";
            }
        }
        return choices;
    }

    public static String getWord(String root, String template) {
        String w = template;
        int pos = w.indexOf("f");
        w = w.substring(0, pos) + root.substring(0, 1) + w.substring(pos + 1);
        while (w.indexOf("E", pos + 1) >= 0) {
            pos = w.indexOf("E", pos + 1);
            w = w.substring(0, pos) + root.substring(1, 2) + w.substring(pos + 1);
        }
        while (w.indexOf("l", pos + 1) >= 0) {
            pos = w.indexOf("l", pos + 1);
            w = w.substring(0, pos) + root.substring(2, 3) + w.substring(pos + 1);
        }
        return w;
    }

    public static String morph2buck(String input) {
        input = input.replace('P', '$').replace('O', '*');
        return input;
    }

    public static void getTempaltes() throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException {
        BufferedReader brRef = openFileForReading("/Users/kareemdarwish/RESEARCH/AraComLex/Arabic-patterns1.2/Arabic-patterns-tabbed1.2.txt");
        BufferedReader brFree = openFileForReading(dataPath+"/templates.txt");
        BufferedWriter bw = openFileForWriting(dataPath+"/templates.temp.txt");

        String line = "";

        String[] nounDiacritics = {"a", "i", "u", "N", "K", "F"};
        String[] verbDiacritics = {"u", "a", "o"};

        ArrayList<String> templates = new ArrayList<String>();
        ArrayList<String> templatesNoCase = new ArrayList<String>();

        while ((line = brRef.readLine()) != null) {
            String[] parts = line.split("\t");
            for (String s : parts) {
                String p = "";
                if (s.startsWith("pattern:")) {
                    p = s.replace("pattern:", "");
                } else if (s.startsWith("singularPattern:") && !s.contains("unspec")) {
                    p = s.replace("singularPattern:", "");
                } else if (s.startsWith("hasFem:") && !s.contains("unspec")) {
                    p = s.replace("hasFem:", "");
                }
                if (!line.contains("nType:unspec")) {
                    // is noun or adject will likely take damma, fatha, kasra and tanween
                    for (String t : nounDiacritics) {
                        if (!templates.contains(p + t)) {
                            templates.add(p + t);
                        }
                    }
                }
                if (!line.contains("vType:unspec")) {
                    // is noun or adject will likely take damma, fatha, kasra and tanween
                    for (String t : verbDiacritics) {
                        if (!templates.contains(p + t)) {
                            templates.add(p + t);
                        }
                    }
                }
                if (!templatesNoCase.contains(p)) {
                    templatesNoCase.add(p.replaceFirst("[aiouNKF~]+$", ""));
                }
            }
        }

        while ((line = brFree.readLine()) != null) {
            line = ArabicUtils.utf82buck(line.trim());
            line = line.replace("a~", "~a").replace("u~", "~u").replace("i~", "~i");
            line = line.replaceFirst("[aiouNKF~]+$", "");
            if (!templatesNoCase.contains(line)) {
                templatesNoCase.add(line);
            }
//            line = line.replace("aA", "A").replace("iy", "y").replace("uw", "w"); // .replaceFirst("[aiou]+$", "");
//            if (!templatesNoCase.contains(line.replaceFirst("[aiouNKF~]+$", "")) && !templates.contains(line))
//                System.out.println(line.replaceAll("[aiouNKF~]+", "") + "\t" + line);
        }

        for (String s : templatesNoCase) {
            if (s.trim().length() > 0 && !s.contains(".")) {
                System.out.println(s);
            }
        }
    }

    
    
    public static void generateTestDataForCRFDiacritization ()
            throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, Exception
    {
        BufferedReader br = openFileForReading(dataPath+"/diacritizedWords.dictionary.missing.sort");
        BufferedWriter bw = openFileForWriting(dataPath+"/diacritizedWords.dictionary.missing.sort.dia");
        String line = "";
        while ((line = br.readLine()) != null)
        {
            line = processdiacritizedlexicon.ArabicUtils.utf82buck(line);
            String[] parts = line.split("\t");
            if (parts.length > 4)
            {
                for (int j = 3; j <= 4; j++)
                {
                    for (int i = 0; i < parts[j].length(); i++)
                    {
                        bw.write(parts[j].substring(i, i+1) + "\n");
                    }
                    bw.write("\n");
                }
            }
        }
        bw.flush();
        bw.close();
    }
    
    public static void mergeAndPruneGeneratedDictionaryAndObservedDictionary_GetDiacritizationExamplesForCRF ()
        throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, Exception {
        ArabicPOSTagger.POSTagger tagger = new POSTagger("/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/POSandNERData/");
        // read generated dictionary
        BufferedReader brGen = openFileForReading(dataPath+"/diacritizedWords.dictionary.1.0");
        BufferedReader brSeen = openFileForReading("/work/ARABIZI/DIACRITIZE/diacritizedWords.dictionary.from-text");
        
        BufferedWriter bw = openFileForWriting(dataPath+"/diacritizedWords.dictionary.CRF");
        
        TMap<String, String> dictionary = new THashMap<String, String>();
        
        String line = "";
        while ((line = brGen.readLine()) != null)
        {
            String[] parts = line.split("\t");
            if (parts.length == 2)
            {
                if (dictionary.containsKey(parts[0]))
                    dictionary.put(parts[0], dictionary.get(parts[0]) + parts[1]);
                else
                    dictionary.put(parts[0], parts[1]);
            }
        }
        
        TMap<String, String> dictionarySeen = new THashMap<String, String>();
        line = "";
        while ((line = brSeen.readLine()) != null)
        {
            String[] parts = line.split("\t");
            if (parts.length == 2)
            {
                ArrayList<String> stem = getStemmedWords(tagger.tag(parts[0], true, false));
                String stemmedWord = "";
                for (String dia : stem)
                    stemmedWord += dia + " ";
                stemmedWord = stemmedWord.trim();
                String[] diacritizedWords = parts[1].split(";");
                String diacritizedForms = ";";
                for (String dia : diacritizedWords)
                {
                    if (dia.trim().length() > 0)
                    {
                        String s = transferDiacriticsFromWordToSegmentedVersion(dia, stemmedWord);
                        s = removeAffixes(s);
                        s = removeCaseEnding(s);
                        if (IsFullydiacritized(s, true))
                            diacritizedForms += standardizeDiacritics(s) + ";";
                        parts[0] = ArabicUtils.removeDiacritics(s);
                    }
                }
                if (diacritizedForms.length() > 1)
                {
                    if (dictionary.containsKey(parts[0]))
                    {
                     //   dictionary.put(parts[0], dictionary.get(parts[0]) + diacritizedForms);
                        // do nothing
                    }
                    else
                        dictionarySeen.put(parts[0], diacritizedForms);
                }
            }
        }
        
        DiacritizeText dt = new DiacritizeText("", dataPath+"/all-text.txt.diacritized.filtered.nocase.3g.blm", "",  dataPath+"/empty");
        // prune dictionary using probability mass
        for (String key : dictionarySeen.keySet())
        {
            String[] choices = dictionarySeen.get(key).split(";");
            // get top score
            float topScore = 0;
            String topChoice = "";
            // Map<String, Float> Scores = new HashMap<String, Float>();
            for (String c : choices)
            {
                if (c.trim().length() > 0)
                {
                    float score = (float) dt.scoreUsingLM(c);
                    score = (float) Math.exp(score);
                    if (score > topScore)
                    {
                        topChoice = c;
                        topScore = score;
                    }
                    // Scores.put(c, score);
                }
            }
            String features = "";
            for (int i = 0; i < topChoice.length(); i++)
            {
                if (i == 0)
                    features += topChoice.substring(i, i + 1);
                else
                {
                    if (topChoice.substring(i, i + 1).matches("[" + ArabicUtils.AllArabicLetters + "]"))
                    {
                        features += ";" + topChoice.substring(i, i + 1);
                    }
                    else
                    {
                        if (topChoice.substring(i - 1, i).matches("[" + ArabicUtils.AllArabicLetters + "]"))
                        {
                            features += ",";
                        }
                        features += topChoice.substring(i, i + 1);
                    }
                }
            }
            
            String[] featParts = features.split(";");
            for (String fp : featParts)
            {
                if (fp.contains(","))
                {
                    String letter = fp.replaceFirst(",.*", "");
                    String diacritic = processdiacritizedlexicon.ArabicUtils.utf82buck(fp.replaceFirst(".*,", ""));
                    bw.write(letter + "\t" + diacritic + "\n");
                }
                else
                {
                    bw.write(fp + "\t" + "null" + "\n");
                }
                bw.flush();
            }
            bw.write("\n");
            // Scores = pruneBasedOnProbabilityMass(Scores, 0.9f);
            // bw.write(key + "\t;");
            // for (String c : Scores.keySet())
            //    bw.write(c + ";");
            // bw.write("\n");
            bw.flush();
        }
        
        bw.close();
    }
    
    public static Map<String, Float> pruneBasedOnProbabilityMass(Map<String, Float> count, float threshold) {
        HashMap<String, Float> output = new HashMap<String, Float>();
        float totalCount = 0f;
        for (String s : count.keySet()) {
            totalCount += count.get(s);
        }

        float incrementalCount = 0;
        for (String s : count.keySet()) {
            if ((float) incrementalCount / (float) totalCount < threshold && (float) count.get(s) / (float) totalCount > 0.02) {
                output.put(s, count.get(s));
            }
            incrementalCount += count.get(s);
        }
        return output;
    }
    
    public static String removeAffixes(String stemmedWord)
    {
        if (!stemmedWord.contains("+"))
            return stemmedWord;
        else
        {
            boolean ok = true;
            // remove prefixes
            while (ok)
            {
                int pos = stemmedWord.indexOf("+");
                if (pos >= 0)
                {
                    String clitic = stemmedWord.substring(0, pos);
                    clitic = ArabicUtils.removeDiacritics(clitic.replace("+", ""));
                    if (hPrefixes.containsKey(clitic))
                        stemmedWord = stemmedWord.substring(pos + 1);
                    else
                        ok = false;
                }
                else
                    ok = false;
            }
            
            // remove suffixes
            ok = true;
            while (ok)
            {
                int pos = stemmedWord.lastIndexOf("+");
                if (pos >= 0)
                {
                        String clitic = stemmedWord.substring(pos);
                    clitic = ArabicUtils.removeDiacritics(clitic.replace("+", "")).trim();
                    if (clitic.equals("ة"))
                        ok = false;
                    else if (clitic.equals("ت"))
                    {
                        ok = false;
                        stemmedWord = stemmedWord.substring(0, pos)  + "+ة";
                    }
                    else if (hSuffixes.containsKey(clitic))
                    {
                        stemmedWord = stemmedWord.substring(0, pos);
                    }
                    else
                    {
                        ok = false;
                    }
                }
                else
                    ok = false;
            }
            return stemmedWord.replace("+", "");
        }
    }
    
    public static boolean IsFullydiacritized (String word, boolean withCaseEnding)
    {
        String lastDiacriticRegex = "[" + processdiacritizedlexicon.ArabicUtils.buck2utf8("aiou~") + "]+";
        word = recoverDiacriticsLostDuringStandarization(word);
        for (int i = 0; i < word.length() - 1; i++)
        {
            String letter = word.substring(i, i + 1);
            if (letter.matches(lastDiacriticRegex))
            {
                // do nothing
            }
            else
            {
                if (!letter.matches("[اوي]"))
                {
                    if (!word.substring(i + 1, i + 2).matches(lastDiacriticRegex))
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    public static ArrayList<String> getStemmedWords (ArrayList<String> output)
    {
        ArrayList<String> words = new ArrayList<String>();
        String word = "";
        for (String s : output)
        {
            if (s.equals("_"))
            {
                if (word.trim().length() > 0)
                {
                    word = word.replaceFirst("^\\+", "");
                    words.add(getProperSegmentation(word, hPrefixes, hSuffixes));
                    word = "";
                }
            }
            else
            {
                word += "+" + s;
            }
        }
        return words;
    }
    
    private static ArrayList<String> getCaseEndingsCRF(ArrayList<String> input) throws IOException, InterruptedException // 0 = tokenizer, 1 = pos, 2 = phrase detection
    {
        ArrayList<String> output = new ArrayList<String>();
        String res = new String();
        String ins = new String();

        for (String elt : input) {
            ins += elt + "\n";
        }
        res = caseEndingTagger.parse(ins);
        
        String[] parts = res.split("\n");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].trim().startsWith("# 0") && !parts[i].trim().startsWith("# 1."))
            {
                output.add(parts[i]);
            }
        }

        return output;

    }
    
    public static void pruneDictionaryOfPossibilities() throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException {
        BufferedReader br = openFileForReading(dataPath+"/train.arpa.1g.dictionary");
        // BufferedReader br = openFileForReading(dataPath+"/tmp.txt");
        BufferedWriter bw = openFileForWriting(dataPath+"/train.arpa.1g.dictionary.out.0.90");
        // BufferedWriter bw = openFileForWriting(dataPath+"/tmp.txt.1");
        DiacritizeText dt = new DiacritizeText("", "", "", dataPath+"/diacritizedWords.dictionary.1.0");

        String line = "";
        while ((line = br.readLine()) != null) {
            String[] parts = line.trim().split("[\\s\t]+");
            if (parts.length == 2) {
                parts[0] = ArabicUtils.utf82buck(parts[0]);
                String[] seen = parts[1].split(";");
                Map<String, Float> count = new TreeMap<String, Float>();
                for (String s : seen) {
                    if (s.length() > 0) {
                        // String template = getDiacriticsTemplate(ArabicUtils.utf82buck(standardizeDiacritics(s)).replaceFirst("^[aiuoNKF~]+", ""));
                        String template = getDiacriticsTemplate(ArabicUtils.utf82buck(s).replaceFirst("^[aiuoNKF~]+", ""));
                        if (!template.matches(".*[aiouNKF][aiouNKF].*")
                                && !template.matches(".*[NKF]+M.*")) // make sure that it has no wacky diacritizations
                        {
                            float score = (float) dt.scoreUsingLM(s);
                            score = (float) Math.exp(score);
                            if (count.containsKey(template)) {
                                count.put(template, score + count.get(template));
                            } else {
                                count.put(template, score);
                            }
                        }
                    }
                }
                // perform templates merge
                while (count.size() > mergeTemplates(sortByValues(count)).size()) {
                    count = mergeTemplates(sortByValues(count));
                }

                // while (count.size() > mergeTemplatesExtraLiberal(sortByValues(count)).size())
                //    count = mergeTemplatesExtraLiberal(sortByValues(count));
                count = pruneBasedOnProbabilityMass(sortByValues(count), 0.90f);

                bw.write(ArabicUtils.buck2utf8(parts[0]) + "\t;");
                for (String s : sortByValues(count).keySet()) {
                    bw.write(ArabicUtils.buck2utf8(putDiacriticsBasedOnTemplate(parts[0], s)).replace(".", "") + ";");
                }
                bw.write("\n");
                bw.flush();
            }
        }
    }

    public static String fullyDiacritizeSentence (String inputSentence, DiacritizeText dt) throws ClassNotFoundException, Exception
    {
        String outputSentence = "";
        ArrayList<String> words = ArabicUtils.tokenize(inputSentence);
        String newLine = dt.diacritize(words);
        ArrayList<String> tokens = dt.tagWords(newLine);
        ArrayList<String> caseEndingFeatures = createCaseEndingCRFInput(tokens);
        ArrayList<String> caseEndingOutput = getCaseEndingsCRF(caseEndingFeatures);
        outputSentence = combineDiacritizedWordWithCaseEnding(dt, caseEndingOutput, newLine.trim().split(" +"));
        return outputSentence;
    }
    
    private static void countBigrams() throws FileNotFoundException, IOException
    {
        BufferedReader br = openFileForReading(dataPath+"/RDI/rdi-text.txt.tok");
        BufferedWriter bw = openFileForWriting(dataPath+"/RDI/rdi-text.txt.tok.bigrams");
        
        THashMap<String, Integer> bigrams = new THashMap<String, Integer>();
        
        String line = "";
        while ((line = br.readLine()) != null)
        {
            String[] tokens = line.split(" +");
            for (int i = 0; i < tokens.length - 1; i++)
            {
                String bigram = tokens[i] + " " + tokens[i+1];
                int count = 1;
                if (bigrams.contains(bigram))
                {
                    count = bigrams.get(bigram) + 1;
                }
                bigrams.put(bigram, count);
            }
        }
        
        THashMap<String, String> undiacritizedBigrams = new THashMap<String, String>();
        
        for (String s : bigrams.keySet())
        {
            if (!s.contains(";"))
            {
                String undiacritizedBigram = ArabicUtils.removeDiacritics(s);
                if (undiacritizedBigrams.contains(undiacritizedBigram))
                {
                    undiacritizedBigrams.put(undiacritizedBigram, undiacritizedBigrams.get(undiacritizedBigram) + ";" + s + " " + bigrams.get(s));
                }
                else
                {
                    undiacritizedBigrams.put(undiacritizedBigram, s + " " + bigrams.get(s));
                }
            }
        }
        
        for (String s : undiacritizedBigrams.keySet())
        {
            if (!undiacritizedBigrams.get(s).contains(";") && Integer.parseInt(undiacritizedBigrams.get(s).replaceFirst("^.* ", "")) >= 5)
                bw.write(s + "\t" + undiacritizedBigrams.get(s) + "\n");
        }
        
        bw.close();
    }
    
    private static String OLDcombineDiacritizedWordWithCaseEndingOLD(DiacritizeText dt, ArrayList<String> caseEndings, String[] diacritizedWords)
    {
        String output = "";
        //if (caseEndings.size() != diacritizedWords.length)
        //    System.err.println();
        for (int i = 0; i < caseEndings.size(); i++)
        {
            if (!caseEndings.get(i).trim().startsWith("# 0") && !caseEndings.get(i).trim().startsWith("# 1.")) {
                String[] parts = caseEndings.get(i).split("[ \t]+");
                String word = parts[0];
                String stem = parts[1];
                String ending = parts[parts.length - 1];
                String dWord = correctLamAlefLam(diacritizedWords[i], true);
                Double endingScore = 0d;
                if (ending.contains("/"))
                {
                    endingScore = Double.parseDouble(ending.substring(ending.indexOf("/") + 1));
                    ending = ending.substring(0, ending.indexOf("/")).trim();
                }
                else
                {
                    //System.err.println();
                }
                if (endingScore < 0.8 || ending.equals("null")) {
                    ending = "";
                }
                if (dt.getwordsWithSingleDiacritizations().containsKey(word.trim().replace("+", "")))
                {
                    output += "§" + dt.getwordsWithSingleDiacritizations().get(word.trim().replace("+", "")) + "§ ";
                }
                else if ((stem.trim().equals("#") && !word.endsWith("+ه")) || ending.trim().equals("null") || ending.trim().equals("Maad")) 
                {
                    output += dWord + " ";
                } else {
                    // get prefix
                    int pos = word.indexOf(stem);
                    if (pos == -1) {
                        if (stem.startsWith("#")) {
                            stem = stem.substring(1);
                            pos = word.indexOf(stem);
                        }
                    }
                // if (word.startsWith("ب"))
                    //     System.err.println();
                    System.err.println("w:"+word);
                    if(pos == -1)
                           pos = 0; //word.length();
                    String prefixPlusStem = correctLamAlefLam((word.substring(0, pos) + stem).replace("+", ""), false);
                    int j = 0;
                // if (ArabicUtils.removeDiacritics(dWord).contains("كما"))
                     System.err.println(prefixPlusStem+" :: "+dWord);
                    // get the position of the last letter in the diacritized word without diacritics
                    while (!(ArabicUtils.removeDiacritics(dWord.substring(0, j)).equals(prefixPlusStem))) {
                        j++;
                    }
                    String fullDiacritization = "";
                    if (ending.equals("oi"))
                    {
                        if (stem.endsWith("+ين"))
                        {
                        // find the last diacritic before yn, 
                        // if it is null, then put a at the end of the word
                        // else put a i
                        
                            String stemWithoutYn = stem.substring(0, stem.indexOf("+ين"));
                            int positionInWord = 0;
                            for (int k = 0; k < stemWithoutYn.length(); k++)
                            {
                                positionInWord = dWord.indexOf(stemWithoutYn.substring(k, k + 1), positionInWord);
                            }
                            if (dWord.substring(positionInWord + 1, positionInWord + 2).matches("[" + ArabicUtils.buck2utf8("aiouNKF~") + "]"))
                            {
                                ending = "i";
                            }
                            else
                            {
                                ending = "a";
                            }
                            fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8(ending) + dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                        }
                        else
                        {
                            fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8("i") + dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                        }
                    }
                    else
                    {
                        if (!ending.isEmpty())
                        {
                            fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8(ending);
                            if (diacritizedSuffixes.containsKey(dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "")))
                            {
                                fullDiacritization += diacritizedSuffixes.get(dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", ""));
                            }
                            else
                            {
                                fullDiacritization += dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                            }
                            if (word.endsWith("+ه"))
                            {
                                if (ending.contains("i"))
                                {
                                    fullDiacritization += ArabicUtils.buck2utf8("i");
                                }
                                else
                                {
                                    fullDiacritization += ArabicUtils.buck2utf8("u");
                                }
                            }
                        }
                        else
                        {
                            if (word.contains("+ه"))
                            {
                                String tmpdWord = dWord.replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                                if (tmpdWord.endsWith("ه"))
                                {
                                    tmpdWord = tmpdWord.substring(0, tmpdWord.length() - 1);
                                    // find last diacritic
                                    if (tmpdWord.matches(".*[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+$"))
                                    {
                                        if (tmpdWord.endsWith(ArabicUtils.buck2utf8("i")))
                                            // ends with i
                                            ending = "i";
                                        else
                                            ending = "u";
                                        fullDiacritization = tmpdWord + "ه" + ArabicUtils.buck2utf8(ending);
                                    }
                                    else if (tmpdWord.endsWith("ي"))
                                    {
                                        ending = "i";
                                        fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8(ending) + dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                                    }
                                    else
                                    {
                                        fullDiacritization = tmpdWord + ArabicUtils.buck2utf8("a") + "ه" + ArabicUtils.buck2utf8("u");
                                    }
                                }
                                else
                                {
                                    fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8(ending) + dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                                }
                            }
                            else
                            {
                                fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8(ending) + dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                            }
                        }
                    }
                    output += fullDiacritization + " ";
                }
            }
        }
        output = putBigramsWithSingleDiacritization(output, dt);
        return output.trim();
    }
    private static String combineDiacritizedWordWithCaseEnding(DiacritizeText dt, ArrayList<String> caseEndings, String[] diacritizedWords)
    {
        String output = "";
        if (caseEndings.size() != diacritizedWords.length)
            System.err.println();
        for (int i = 0; i < caseEndings.size(); i++)
        {
            if (!caseEndings.get(i).trim().startsWith("# 0") && !caseEndings.get(i).trim().startsWith("# 1.")) {
                String[] parts = caseEndings.get(i).split("[ \t]+");
                String word = parts[0];
                String stem = parts[1];
                String ending = parts[parts.length - 1];
                String dWord = correctLamAlefLam(diacritizedWords[i], true);
                Double endingScore = 0d;
                if (ending.contains("/"))
                {
                    endingScore = Double.parseDouble(ending.substring(ending.indexOf("/") + 1));
                    ending = ending.substring(0, ending.indexOf("/")).trim();
                }
                else
                {
                    System.err.println();
                }
                if (endingScore < 0.8 || ending.equals("null")) {
                    ending = "";
                }
                if (dt.getwordsWithSingleDiacritizations().containsKey(word.trim().replace("+", "")))
                {
                    output += "§" + dt.getwordsWithSingleDiacritizations().get(word.trim().replace("+", "")) + "§ ";
                }
                else if ((stem.trim().equals("#") && !word.endsWith("+ه")) || ending.trim().equals("null") || ending.trim().equals("Maad")) 
                {
                    output += dWord + " ";
                } else {
                    // get prefix
                    int pos = word.indexOf(stem);
                    if (pos == -1) {
                        if (stem.startsWith("#")) {
                            stem = stem.substring(1);
                            pos = word.indexOf(stem);
                        }
                    }
                // if (word.startsWith("ب"))
                    //     System.err.println();
                    String prefixPlusStem = "";
                    if (pos == -1)
                    {
                        if (stem.contains("+"))
                            stem = stem.substring(0, stem.indexOf("+"));
                        pos = word.indexOf(stem);
                        if (pos > -1){
                            prefixPlusStem = correctLamAlefLam((word.substring(0, pos) + stem).replace("+", ""), false);
                        }
                        else
                        {
            prefixPlusStem = stem;
                            System.err.println(word + "\t" + stem);
                        }
                            
                    }
                    else
                        prefixPlusStem = correctLamAlefLam((word.substring(0, pos) + stem).replace("+", ""), false);
                    int j = 0;
                // if (ArabicUtils.removeDiacritics(dWord).contains("كما"))
                        System.err.println(dWord);
                    // get the position of the last letter in the diacritized word without diacritics
                    while (!(ArabicUtils.removeDiacritics(dWord.substring(0, j)).equals(prefixPlusStem))) {
                        j++;
                    }
                    String fullDiacritization = "";
                    if (ending.equals("oi"))
                    {
                        if (stem.endsWith("+ين"))
                        {
                        // find the last diacritic before yn, 
                        // if it is null, then put a at the end of the word
                        // else put a i
                        
                            String stemWithoutYn = stem.substring(0, stem.indexOf("+ين"));
                            int positionInWord = 0;
                            for (int k = 0; k < stemWithoutYn.length(); k++)
                            {
                                positionInWord = dWord.indexOf(stemWithoutYn.substring(k, k + 1), positionInWord);
                            }
                            if (dWord.substring(positionInWord + 1, positionInWord + 2).matches("[" + ArabicUtils.buck2utf8("aiouNKF~") + "]"))
                            {
                                ending = "i";
                            }
                            else
                            {
                                ending = "a";
                            }
                            fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8(ending) + dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                        }
                        else
                        {
                            fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8("i") + dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                        }
                    }
                    else
                    {
                        if (!ending.isEmpty())
                        {
                            fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8(ending);
                            if (diacritizedSuffixes.containsKey(dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "")))
                            {
                                fullDiacritization += diacritizedSuffixes.get(dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", ""));
                            }
                            else
                            {
                                fullDiacritization += dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                            }
                            if (word.endsWith("+ه"))
                            {
                                if (ending.contains("i"))
                                {
                                    fullDiacritization += ArabicUtils.buck2utf8("i");
                                }
                                else
                                {
                                    fullDiacritization += ArabicUtils.buck2utf8("u");
                                }
                            }
                        }
                        else
                        {
                            if (word.contains("+ه"))
                            {
                                String tmpdWord = dWord.replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                                if (tmpdWord.endsWith("ه"))
                                {
                                    tmpdWord = tmpdWord.substring(0, tmpdWord.length() - 1);
                                    // find last diacritic
                                    if (tmpdWord.matches(".*[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+$"))
                                    {
                                        if (tmpdWord.endsWith(ArabicUtils.buck2utf8("i")))
                                            // ends with i
                                            ending = "i";
                                        else
                                            ending = "u";
                                        fullDiacritization = tmpdWord + "ه" + ArabicUtils.buck2utf8(ending);
                                    }
                                    else if (tmpdWord.endsWith("ي"))
                                    {
                                        ending = "i";
                                        fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8(ending) + dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                                    }
                                    else
                                    {
                                        fullDiacritization = tmpdWord + ArabicUtils.buck2utf8("a") + "ه" + ArabicUtils.buck2utf8("u");
                                    }
                                }
                                else
                                {
                                    fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8(ending) + dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                                }
                            }
                            else
                            {
                                fullDiacritization = dWord.substring(0, j) + ArabicUtils.buck2utf8(ending) + dWord.substring(j).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
                            }
                        }
                    }
                    output += fullDiacritization + " ";
                }
            }
        }
        output = putBigramsWithSingleDiacritization(output, dt);
        return output.trim();
    }
    private static String putBigramsWithSingleDiacritization(String input, DiacritizeText dt)
    {
        String output = "";
        String[] words = input.split(" +");
        int i = 0;
        while (i < words.length - 1)
        {
            String bigram = words[i] + " " + words[i+1];
            boolean hasFunnyCharacter = bigram.contains("§");
            String undiacritizedBigram = ArabicUtils.removeDiacritics(bigram);
            if (dt.getbigramsWithSingleDiacritizations().containsKey(undiacritizedBigram))
            {
                output += "§§" + dt.getbigramsWithSingleDiacritizations().get(undiacritizedBigram) + "§§ ";
                i++; i++;
            }
            else
            {
                output += words[i] + " ";
                i++;
            }
        }
        if (i == words.length - 1)
        {
            output += words[i] + " ";
            i++;
        }
        return output.trim();
    }
    
    private static String correctLamAlefLam (String input, boolean withDiacritics)
    {
        if (ArabicUtils.removeDiacritics(input).startsWith("لال"))
        {
            return correctLamAlefLamNoPrefixes(input, withDiacritics);
        }
        else if (ArabicUtils.removeDiacritics(input).startsWith("ولال") || ArabicUtils.removeDiacritics(input).startsWith("فلال"))
        {
            String firstLetter = input.substring(0, 1);
            input = input.substring(input.indexOf("ل"));
            if (withDiacritics)
            {
                firstLetter += ArabicUtils.buck2utf8("a");
            }
            return firstLetter + correctLamAlefLamNoPrefixes(input, withDiacritics);
        }
        else
        {
            return input;
        }
    }
    
    private static String correctLamAlefLamNoPrefixes (String input, boolean withDiacritics)
    {
        String output = "";
        if (ArabicUtils.removeDiacritics(input).startsWith("لال") || ArabicUtils.removeDiacritics(input).startsWith("ل+ال"))
        {
            int i = 0;
            while (!(ArabicUtils.removeDiacritics(input.substring(0, i)).replace("+", "").equals("لال")))
            {
                i++;
            }
            if (withDiacritics)
                output = ArabicUtils.buck2utf8("lilo") + input.substring(i).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
            else
                output = ArabicUtils.buck2utf8("ll") + input.substring(i).replaceFirst("^[" + ArabicUtils.buck2utf8("aiouNKF~") + "]+", "");
        }
        else
        {
            output = input;
        }
        return output;
    }
    
    public static ArrayList<String> createCaseEndingCRFInput(ArrayList<String> lines) throws ClassNotFoundException, Exception
    {
        ArrayList<String> output = new ArrayList<String>();
       
        /*
            word
            stem 
            stemPOS 
            prefix
            prefixPOS 
            suffix
            suffixPOS
            Suff
            firstLetter
            lastLetter 
            template
            genderNumberTag
            diacritic
        */
        String lastSeenVerb = "";
        int lastSeenVerbLoc = -1;
        int currentPosInSentence = 0;
        
        ArrayList<String> wordParts = new ArrayList<String>();
        for (int i = 0; i < lines.size(); i++)
        {
            if (lines.get(i).trim().equals("-"))
            {
                if (wordParts.size() > 0)
                {
                    currentPosInSentence++;
                    // get word
                    String word = "";
                    for (String s : wordParts)
                    {
                        if (word.length() > 0)
                        {
                            word += "+";
                        }
                        word += s.replaceFirst("\\/.*", "");
                    }
                    // get stem
                    // if (word.startsWith("ب"))
                    //    System.err.println();
                    String PrefixStemSuffix = getWordStem(word, true).trim();
                    if (!PrefixStemSuffix.contains(";"))
                        PrefixStemSuffix = "#;" + PrefixStemSuffix + ";#";
                    if (PrefixStemSuffix.endsWith(";"))
                        PrefixStemSuffix = PrefixStemSuffix + "#";
                    if (PrefixStemSuffix.startsWith(";"))
                        PrefixStemSuffix = "#" + PrefixStemSuffix;
                    if (PrefixStemSuffix.contains(";;"))
                        PrefixStemSuffix = PrefixStemSuffix.replace(";;", ";#;");

                    String stem = PrefixStemSuffix.substring(PrefixStemSuffix.indexOf(";") + 1, 
                            PrefixStemSuffix.lastIndexOf(";")).trim(); // .replaceFirst(".*?;", "").replaceFirst(";.*?", "");
                    String template = "Y"; // template of verbs only
                    // get stemPOS
                    String stemPOS = "";
                    // get genderNumber
                    String genderNumberTag = "O";
                    if (stem.equals("#"))
                    {
                        stemPOS = "Y";
                    }
                    else
                    {
                        for (String s : wordParts)
                        {
                            if (s.startsWith(stem + "/"))
                            {
                                stemPOS = s.replaceFirst(".*\\/", "").trim();
                                if (stemPOS.contains("-"))
                                {
                                    genderNumberTag = stemPOS.substring(stemPOS.indexOf("-") + 1).trim();
                                    stemPOS = stemPOS.substring(0, stemPOS.indexOf("-")).trim();
                                }
                                if (stemPOS.trim().equals("V") || stemPOS.trim().startsWith("V+"))
                                {
                                    template = s.substring(s.indexOf("/") + 1, s.lastIndexOf("/"));
                                }
                            }
                        }
                    }

                    // attach if NSUFF to stem and stemPOS
                    // get prefix and suffix POS
                    String prefixPOS = "";
                    String suffixPOS = "";
                    // get prefix & suffix
                    String prefix = ""; // PrefixStemSuffix.replaceFirst(";.*", "");
                    String suffix = ""; // PrefixStemSuffix.replaceFirst(".*;", "");
                    
                    // add verb feature
                    String lastVerb = "VerbNotSeen";
                    if (stemPOS.equals("V"))
                    {
                        // this is a verb
                        lastSeenVerb = stem;
                        lastSeenVerbLoc = currentPosInSentence;
                    }
                    else if (lastSeenVerbLoc != -1 && currentPosInSentence - lastSeenVerbLoc < 7)
                    {
                        lastVerb = lastSeenVerb;
                    }
                    
                    int positionInPrefixStemSuffix = 0;
                    for (String ss : wordParts)
                    {
                        if (ss.startsWith(stem + "/"))
                        {
                            positionInPrefixStemSuffix = 2;
                        } 
                        else if (ss.contains("NSUFF"))
                        {
                            stem += "+" + ss.replaceFirst("\\/.*", "").trim();
                            stemPOS += "+NSUFF";
                        }
                        else if (positionInPrefixStemSuffix == 0)
                        {
                            prefixPOS += ss.replaceFirst(".*\\/", "").trim() + "+";
                            prefix += ss.substring(0, ss.indexOf("/")).trim() + "+";
                        }
                        else if (positionInPrefixStemSuffix == 2)
                        {
                            suffixPOS += "+" + ss.replaceFirst(".*\\/", "").trim();
                            suffix += "+" + ss.substring(0, ss.indexOf("/")).trim();
                        }
                    }



                    // correct prefix and suffix if empty

                    if (prefix.contains("#") || prefix.isEmpty())
                    {
                        prefixPOS = "Y";
                        prefix = "#";
                    }
                    if (suffix.contains("#") || suffix.isEmpty())
                    {
                        suffixPOS = "Y";
                        suffix = "#";
                    }

                    String Suff = "#"; // if word has NSUFF -- show surface form
                    if (stem.contains("+"))
                        Suff = stem.substring(stem.indexOf("+"));

                    // first and last letter
                    String firstLetter = stem.substring(0, 1);
                    String lastLetter = stem.substring(stem.length() - 1);

                    output.add(word + "\t" + stem + "\t" + stemPOS + "\t" + 
                                prefix + "\t" + prefixPOS + "\t" + 
                                suffix + "\t" + suffixPOS + "\t" + 
                                Suff + "\t" + firstLetter + "\t" + lastLetter + "\t" + 
                                template + "\t" + genderNumberTag + "\t" + lastVerb);
                    wordParts.clear();
                }
            }
            else
            {
                wordParts.add(lines.get(i));
            }
        }
        
        return output;
    }
    
    public static void diacritizeFile(String inputFile) throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, Exception {
        // DiacritizeText dt = new DiacritizeText("", dataPath+"/all-text.txt.diacritized.filtered.nocase.3g.blm", dataPath+"/RDI/rdi+ldc-text.txt.tok.nocase.arpa.blm",
        //acritizeText dt = new DiacritizeText("", "", dataPath+"rdi+ldc-text.txt.tok.nocase.arpa.blm",dataPath+"diacritizedWords.dictionary.GenAndSeen");
        BufferedReader br = openFileForReading(inputFile);
        BufferedWriter bw = openFileForWriting(inputFile + ".out");
        String line = "";
        while ((line = br.readLine()) != null) {
            line = line.replace("/", "\\");
            String[] ws = line.split(" +");
            String newLine = fullyDiacritizeSentence(line, dt).replace("\\", "/");
            /*
            ArrayList<String> words = ArabicUtils.tokenize(line);// new ArrayList<String>();
            */
            /*
            for (String s : ws) {
                if (s.trim().length() > 0) {
                    words.add(s);
                }
            }
            */
            /*
            String newLine = dt.diacritize(words);//.replace("§","");
            */
            // bw.write(line + "\n");
            // bw.write(ArabicUtils.removeDiacritics(line) + "\n");
            //newLine = newLine.replace("§", "");
            // newLine = recoverDiacriticsLostDuringStandarization(newLine);
            // bw.write(processdiacritizedlexicon.ArabicUtils.utf82buck(newLine) + "\n");
            bw.write(newLine + "\n");
            // bw.write("===================================================================\n");
            bw.flush();
        }
    }

    public static void addUnigramContextToWords() throws FileNotFoundException, IOException {
        BufferedReader br = openFileForReading("/work/CLASSIC/all-text.txt.diacritized-words.filtered.tok.count");
        // BufferedReader br = openFileForReading("/work/CLASSIC/tmp.txt");
        String dictionaryFile = "/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort.out";
        BufferedReader brDict = openFileForReading(dictionaryFile);

        BufferedWriter bwUpdate = openFileForWriting("/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort.out.dictionary");
        BufferedWriter bwBigram = openFileForWriting("/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort.out.bigrams");

        HashMap<String, String> dictionary = new HashMap<String, String>();
        HashMap<String, Integer> diacritizedFormCount = new HashMap<String, Integer>();
        String line = "";
        while ((line = brDict.readLine()) != null) {
            String[] parts = line.trim().split("[\\s\t]+");
            parts[0] = ArabicUtils.utf82buck(parts[0]);
            if (dictionary.containsKey(parts[0])) {
                dictionary.put(parts[0], dictionary.get(parts[0]) + ";" + parts[1]);
            } else {
                dictionary.put(parts[0], parts[1]);
            }

            String dia = putDiacriticsBasedOnTemplate(parts[0], parts[1]);
            if (diacritizedFormCount.containsKey(dia)) {
                diacritizedFormCount.put(dia, diacritizedFormCount.get(dia) + Integer.parseInt(parts[2]));
            } else {
                diacritizedFormCount.put(dia, Integer.parseInt(parts[2]));
            }
        }

        HashMap<String, Integer> bigrams = new HashMap<String, Integer>();

        while ((line = br.readLine()) != null) {
            line = line.trim();
            int cnt = Integer.parseInt(line.trim().replaceFirst(" .*", ""));
            if (cnt >= 1) {
                line = line.replaceFirst("^[0-9]+ ", "").trim();
                String[] parts = line.trim().split("[\t]+");
                if (parts.length == 2) {
                    // match dicitized word to one in the dictionary
                    parts[0] = parts[0].replaceAll("[\\/\\.=\\(\\);\\+_\\-«»\\&\\%\\|]+", "").replace("\\", "").trim();
                    parts[1] = parts[1].replaceAll("[\\/\\.=\\(\\);\\+_\\-«»\\&\\%\\|]+", "").replace("\\", "").trim();
                    parts[0] = ArabicUtils.utf82buck(parts[0]);
                    String originalWord = parts[0];
                    parts[0] = parts[0].replaceFirst("^[aiuoNKF~]+", "");
                    parts[0] = parts[0].replaceFirst("[aiuoNKF~]+$", "");
                    if (parts[0].matches("^.*[aiuoNKF~]+.*$")) {
                        String template = getDiacriticsTemplate(parts[0]);
                        String[] stemParts = (" " + getDiacritizedStem(parts[1].replace("_", "").trim().replaceAll(" +", "+"), template) + " ").split("\\+");
                        String stem = stemParts[1].trim();
                        String prefix = stemParts[0].trim();
                        String suffix = stemParts[2].trim();
                        stem = ArabicUtils.utf82buck(stem.replaceFirst(".*\\+", ""));
                        template = getDiacriticsTemplate(ArabicUtils.utf82buck(stem.replace(".", "")));

                        if (dictionary.containsKey(stem.replaceAll("[aiuoNKF~\\.]+", "")) && stem.trim().length() > 0) {
                            String possibleTemplates = dictionary.get(stem.replaceAll("[aiuoNKF~\\.]+", ""));
                            String[] possibleTemplatesArray = possibleTemplates.split(";");
                            boolean merged = false;
                            for (int i = 0; i < possibleTemplatesArray.length && merged == false; i++) {
                                String s = possibleTemplatesArray[i];
                                Map<String, Float> count = new HashMap<String, Float>();
                                count.put(s, 1f);
                                count.put(template, 1f);
                                count = mergeTemplatesExtraLiberal(count);
                                if (count.size() == 1) {
                                    merged = true;
                                    // print dictionary entry
                                    // diacritized stem
                                    // full diacritized word

                                    bwUpdate.write(stem.replaceAll("[aiuoNKF~\\.]+", "") + "\t"
                                            + ArabicUtils.utf82buck(putDiacriticsBasedOnTemplate(stem.replaceAll("[aiuoNKF~\\.]+", ""), template)).replace(".", "") + "\t"
                                            + ArabicUtils.utf82buck(putDiacriticsBasedOnTemplate(stem.replaceAll("[aiuoNKF~\\.]+", ""), s)).replace(".", "") + "\t"
                                            + cnt + "\n"
                                    );

                                    if (diacritizedPrefixes.containsKey(prefix)) {
                                        prefix = diacritizedPrefixes.get(prefix);
                                    }
                                    if (diacritizedSuffixes.containsKey(suffix)) {
                                        suffix = diacritizedSuffixes.get(suffix);
                                    }

                                    bwUpdate.write(originalWord.replaceAll("[aiuoNKF~\\.]+", "") + "\t"
                                            + originalWord.replace(".", "") + "\t"
                                            + prefix + ArabicUtils.utf82buck(putDiacriticsBasedOnTemplate(stem.replaceAll("[aiuoNKF~\\.]+", ""), s)).replace(".", "") + suffix + "\t"
                                            + cnt + "\n"
                                    );

                                    bwUpdate.flush();

                                    bwBigram.write(stem.replaceAll("[aiuoNKF~\\.]+", "") + "\t"
                                            + ArabicUtils.utf82buck(prefix) + "+"
                                            + ArabicUtils.utf82buck(putDiacriticsBasedOnTemplate(stem.replaceAll("[aiuoNKF~\\.]+", ""), s))
                                            + "\t" + cnt
                                            + "\n");
                                    bwBigram.flush();
                                }
                            }
                            if (merged == false) {
                                bwBigram.write(stem.replace("[aiuoNKF~]+", "") + "\t" + ArabicUtils.utf82buck(prefix) + "+"
                                        + stem.replace(".", "")
                                        + "\t" + cnt
                                        + "\n");
                                bwBigram.flush();
                            }
                        }
                    }
                }
            }
        }

        bwBigram.close();
        bwUpdate.close();
    }

    public static void generateStemTemplateDiacritizations() throws FileNotFoundException, IOException {
        BufferedReader br = openFileForReading(dataPath+"/train.dictionary.tok");
        BufferedWriter bw = openFileForWriting(dataPath+"/train.dictionary.tok.templates.ref");

        String line = "";

        TMap<String, Integer> templates = new THashMap<String, Integer>();

        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\t");
            if (parts.length == 3) {
                String rawWord = parts[0];
                String diaWord = parts[1];
                String analysis = parts[2];

                ArrayList<String> temp = getDiacritizedTemplate(diaWord, analysis);
                for (String s : temp) {
                    if (templates.containsKey(s)) {
                        templates.put(s, templates.get(s) + 1);
                    } else {
                        templates.put(s, 1);
                        bw.write(s + "\t" + diaWord + "\n");
                    }
                }

            }
        }

//        for (String s : templates.keySet())
//            bw.write(s + "\t" + templates.get(s) + "\n");
        br.close();
        bw.close();
    }

    public static ArrayList<String> getDiacritizedTemplate(String diaWord, String analysis) {
        ArrayList<String> output = new ArrayList<String>();

        if (analysis.contains("E") && analysis.contains("f")) {
            analysis = analysis.replace(";/Y/PUNC", "");
            String[] anaOrg = analysis.trim().split(" +");
            // System.err.println(analysis);
            String[] dia = diaWord.split(";");

            for (String s : dia) {
                String[] ana = anaOrg;
                String candidate = s;
                if (s.trim().length() > 0) {
                    // remove leading Y's
                    int i = 0;
                    while (i < ana.length && ana[i].contains("/Y/")) {
                        String headLetters = ana[i].substring(0, ana[i].indexOf("/"));
                        while (headLetters.length() > 0) {
                            if (candidate.indexOf(headLetters.substring(0, 1)) == -1) {
                                // System.err.println();
                            } else {
                                candidate = candidate.substring(candidate.indexOf(headLetters.substring(0, 1)));
                            }

                            if (headLetters.length() > 1) {
                                headLetters = headLetters.substring(1);
                            } else {
                                headLetters = "";
                            }

                        }
                        // ana[i] = "";
                        i++;
                    }

                    // remove trailing Y's
                    i = ana.length - 1;
                    while (i >= 0 && ana[i].contains("/Y/")) {
                        String headLetters = ana[i].substring(0, ana[i].indexOf("/"));
                        while (headLetters.length() > 0) {
                            if (candidate.lastIndexOf(headLetters.substring(headLetters.length() - 1)) == -1) {
                                //System.err.println();
                            } else {
                                candidate = candidate.substring(0, candidate.lastIndexOf(headLetters.substring(headLetters.length() - 1)));
                            }
                            if (headLetters.length() > 1) {
                                headLetters = headLetters.substring(0, headLetters.length() - 1);
                            } else {
                                headLetters = "";
                            }
                        }
                        // ana[i] = "";
                        i--;
                    }

                    String template = "";
                    for (String a : ana) {
                        if (!a.contains("/Y/")) {
                            template += a;
                        }
                    }

                    String[] temp = template.split("\\/");
                    if (temp.length == 3) {
                        if (temp[1].length() == ArabicUtils.removeDiacritics(candidate).length()) {
                            String diaTemplate = getDiacriticsTemplate(ArabicUtils.utf82buck(standardizeDiacritics(candidate)));
                            output.add(temp[1] + "\t" + diaTemplate);
                        }
                    }
                }
            }
        }
        return output;
    }

    public static String standardizeDiacritics(String word) {
        String diacrtics = "[\u064e\u064b\u064f\u064c\u0650\u064d\u0652\u0651]";
        String sokun = "\u0652";
        String fatha = "\u064e";
        word = word.replaceFirst("^" + diacrtics + "+", "");
        word = word.replace("َا", "ا").replace("ُو", "و").replace("ِي", "ي").replace(".", "").replace("آَ", "آ");
        int pos = word.indexOf("و");
        while (pos > 0 && pos < word.length() - 1) {
            if (!word.substring(pos - 1, pos).matches(diacrtics) && word.substring(pos + 1, pos + 2).equals(sokun)) {
                word = word.substring(0, pos + 1) + word.substring(pos + 2);
            }
            pos = word.indexOf("و", pos + 1);
        }
        pos = word.indexOf("ي");
        while (pos > 0 && pos < word.length() - 1) {
            if (!word.substring(pos - 1, pos).matches(diacrtics) && word.substring(pos + 1, pos + 2).equals(sokun)) {
                word = word.substring(0, pos + 1) + word.substring(pos + 2);
            }
            pos = word.indexOf("ي", pos + 1);
        }
        pos = word.indexOf("ا");
        while (pos > 0 && pos < word.length() - 1) {
            if (!word.substring(pos - 1, pos).matches(diacrtics)
                    && (word.substring(pos + 1, pos + 2).equals(sokun) || word.substring(pos + 1, pos + 2).equals(fatha))) {
                word = word.substring(0, pos + 1) + word.substring(pos + 2);
            }
            pos = word.indexOf("ا", pos + 1);
        }
        if (word.startsWith("الْ")) {
            word = word.replaceFirst("الْ", "ال");
        }
        // word = word.replaceFirst(diacrtics + "+$", "");
        return word;
    }

    public static void diacritizeTreebank() throws FileNotFoundException, IOException {
        String AllArabicLetters = "\u0621-\u063a\u0641-\u0652";
        String inputFile = "/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/DATA/LDC-2.3.1.VOC.pos.voc.txt";
        BufferedReader br = openFileForReading(inputFile);

        String dictionaryFile = "/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort.out";
        BufferedReader brDict = openFileForReading(dictionaryFile);

        String Mistakes = "/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort.out.mistakes";
        BufferedWriter bw = openFileForWriting(Mistakes);

        HashMap<String, String> dictionary = new HashMap<String, String>();
        String line = "";
        String lastWord = "";
        while ((line = brDict.readLine()) != null) {
            String[] parts = line.trim().split("[\\s\t]+");
            if (!lastWord.equals(parts[0])) {
                dictionary.put(parts[0], putDiacriticsBasedOnTemplate(parts[0], parts[1]));
            }
            lastWord = parts[0];
        }

        String word = "";
        long correct = 0;
        long incorrect = 0;
        long partCorrect = 0;
        long unknown = 0;
        while ((line = br.readLine()) != null) {
            String[] parts = line.trim().split("[\\s\t]+");

            if (line.trim().length() == 0 || parts[0].contains("___")) {
                // end of word
                word = word.replaceFirst("[aiuoNKF~]+$", "");
                String guess = "";
                if (dictionary.containsKey(ArabicUtils.buck2utf8(word.replaceAll("[aiuoNKF~]+", "")))) {
                    guess = dictionary.get(ArabicUtils.buck2utf8(word.replaceAll("[aiuoNKF~]+", "")));
                }
                word = ArabicUtils.buck2utf8(word);
                if (word.matches("[" + AllArabicLetters + "]+")) {
                    String guessBackup = guess;
                    word = standardizeDiacritics(word);
                    guess = standardizeDiacritics(guess);

                    if (word.equals(guess)) {
                        correct++;
                    } else if (word.matches(guessBackup.replace(".", ".*"))) {
                        partCorrect++;
                    } else if (guess.length() == 0) {
                        unknown++;
                    } else {
                        incorrect++;
                        bw.write(ArabicUtils.utf82buck(word) + "\t" + ArabicUtils.utf82buck(guess) + "\n");
                        bw.flush();
                    }
                }
                word = "";
            } else if (parts.length == 2) {
                parts[0] = parts[0].replace("-", "");
                if (parts[1].contains("CASE") || parts[0].matches("[aiuoNKF~]+")
                        || parts[1].contains("DET") || parts[1].contains("PRON")
                        || parts[1].contains("CONJ") || (parts[1].contains("PREP") && parts[0].replaceAll("[iou]+", "").length() == 1)) {
                    // do nothing
                } else {
                    word += parts[0];
                }
            }

        }
        System.out.println(correct + "\t" + partCorrect + "\t" + incorrect + "\t" + unknown);
        bw.close();
    }

    public static void diacritizeTreebankFullSystem() throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, Exception {
        DiacritizeText dt = new DiacritizeText("", "", "", dataPath+"/diacritizedWords.dictionary.1.0");
        String AllArabicLetters = "\u0621-\u063a\u0641-\u0652";
        // String inputFile = "/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/DATA/LDC-2.3.1.VOC.pos.voc.txt";
        String inputFile = dataPath+"/tmp.txt";
        BufferedReader br = openFileForReading(inputFile);

        // String dictionaryFile = "/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort.out";
        // BufferedReader brDict = openFileForReading(dictionaryFile);
        String Mistakes = "/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort.out.mistakes";
        BufferedWriter bw = openFileForWriting(Mistakes);

        String line = "";
        String sentence = "";
        long correct = 0;
        long incorrect = 0;
        long partCorrect = 0;
        long unknown = 0;
        while ((line = br.readLine()) != null) {
            if (line.trim().length() == 0) {
                sentence = ArabicUtils.buck2utf8(sentence).replaceAll(" +", " ");
                String dtSentence = dt.diacritize(ArabicUtils.removeDiacritics(sentence));
                String[] org = sentence.split(" +");
                String[] output = dtSentence.split(" +");

                if (org.length == output.length) {
                    for (int i = 0; i < org.length; i++) {
                        String word = standardizeDiacritics(org[i]);
                        String guess = standardizeDiacritics(output[i]);
                        if (word.equals(guess)) {
                            correct++;
                            // } else if (word.matches(output[i].replace(".", ".*"))) {
                            //    partCorrect++;
                        } else if (guess.length() == 0) {
                            unknown++;
                        } else {
                            incorrect++;
                            bw.write(ArabicUtils.utf82buck(word) + "\t" + ArabicUtils.utf82buck(guess) + "\n");
                            bw.flush();
                        }
                    }
                }
                sentence = "";
            } else if (line.contains("PUNC")) {
                // do nothing
            } else if (line.endsWith("\tO")) {
                sentence += " ";
            } else {
                sentence += line.replaceFirst("\t.*", "").replace("-", "");
            }
        }
        System.out.println(correct + "\t" + partCorrect + "\t" + incorrect + "\t" + unknown);
        bw.close();
    }

    public static void diacritizeTreebankFullSystemSegmented() throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, Exception {
        DiacritizeText dt = new DiacritizeText("", "", "", dataPath+"/diacritizedWords.dictionary.1.0");
        String AllArabicLetters = "\u0621-\u063a\u0641-\u0652";
        String inputFile = "/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/DATA/LDC-2.3.1.VOC.pos.voc.txt";
        // String inputFile = dataPath+"/tmp.txt";
        BufferedReader br = openFileForReading(inputFile);

        // String dictionaryFile = "/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort.out";
        // BufferedReader brDict = openFileForReading(dictionaryFile);
        BufferedWriter bwOut = openFileForWriting("/work/CLASSIC/output.txt");

        String Mistakes = "/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort.out.mistakes";
        BufferedWriter bw = openFileForWriting(Mistakes);

        String line = "";
        String sentence = "";
        long correct = 0;
        long incorrect = 0;
        long partCorrect = 0;
        long unknown = 0;
        while ((line = br.readLine()) != null) {
            if (line.trim().length() == 0) {
                sentence = ArabicUtils.buck2utf8(sentence).replace("(نلل)", " ").replaceAll(" +", " ");
                ArrayList<String> tokens = new ArrayList<String>();
                for (String ss : ArabicUtils.removeDiacritics(sentence).split(" +")) {
                    tokens.add(ss);
                }
                String dtSentence = dt.diacritize(tokens);
                bwOut.write(dtSentence + "\n");
                bwOut.flush();
                String[] org = sentence.split(" +");
                String[] output = dtSentence.split(" +");

                if (org.length == output.length) {
                    for (int i = 0; i < org.length; i++) {
                        String word = standardizeDiacritics(org[i]);
                        String guess = standardizeDiacritics(output[i]);
                        if (word.equals(guess)) {
                            correct++;
                            // } else if (word.matches(output[i].replace(".", ".*"))) {
                            //    partCorrect++;
                        } else if (guess.length() == 0) {
                            unknown++;
                        } else {
                            incorrect++;
                            bw.write(ArabicUtils.utf82buck(word) + "\t" + ArabicUtils.utf82buck(guess) + "\n");
                            bw.flush();
                        }
                    }
                }
                sentence = "";
            } else if (line.contains("PUNC")) {
                sentence += " " + line.replaceFirst("\t.*", "") + " ";
            } else if (line.endsWith("\tO")) {
                sentence += " ";
            } else {
                String seg = line.replaceFirst("\t.*", "").replace("-", "");
                if (hPrefixes.containsKey(ArabicUtils.buck2utf8(seg.replaceAll("[aiouNKF~]+", "")))) {
                    seg = seg + "+ ";
                } else if (hSuffixes.containsKey(ArabicUtils.buck2utf8(seg.replaceAll("[aiouNKF~]+", "")))) {
                    seg = " +" + seg;
                }
                sentence += seg;
            }
        }
        System.out.println(correct + "\t" + partCorrect + "\t" + incorrect + "\t" + unknown);
        bw.close();
    }

    public static String putDiacriticsBasedOnTemplate(String word, String template) {
        // System.err.println(word + "\t" + template);
        String output = "";
        String[] di = template.split("M");
        for (int i = 0; i < Math.min(di.length, word.length()); i++) {
            output += word.substring(i, i + 1);
            if (di[i].equals(".")) {
                if (word.substring(i, i + 1).equals("<")) {
                    output += "i";
                } else {
                    output += ".";
                }
            } else {
                output += di[i];
            }
        }
        output += word.substring(Math.min(di.length, word.length()));
        output = ArabicUtils.buck2utf8(output);
        return output;
    }
    
    public static String getProperSegmentationForDiacritization(String input)
    {
        if (hPrefixes.isEmpty()) {
            for (int i = 0; i < prefixes.length; i++) {
                hPrefixes.put(prefixes[i].toString(), 1);
            }
        }
        if (hSuffixes.isEmpty()) {
            for (int i = 0; i < suffixes.length; i++) {
                hSuffixes.put(suffixes[i].toString(), 1);
            }
        }
        String output = "";
        String[] word = input.split("\\+");
        String currentPrefix = "";
        String currentSuffix = "";
        int iValidPrefix = -1;
        while (iValidPrefix + 1 < word.length && hPrefixes.containsKey(word[iValidPrefix + 1])) {
            iValidPrefix++;
        }

        int iValidSuffix = word.length;

        while (iValidSuffix > Math.max(iValidPrefix, 0) && (hSuffixes.containsKey(word[iValidSuffix - 1])
                || word[iValidSuffix - 1].equals("_"))) {
            iValidSuffix--;
        }

        for (int i = 0; i <= iValidPrefix; i++) {
            currentPrefix += word[i] + "+";
        }
        String stemPart = "";
        for (int i = iValidPrefix + 1; i < iValidSuffix; i++) {
            stemPart += word[i];
        }

        if (iValidSuffix == iValidPrefix) {
            iValidSuffix++;
        }

        for (int i = iValidSuffix; i < word.length && iValidSuffix != iValidPrefix; i++) {
            currentSuffix += "+" + word[i];
        }

        output = currentPrefix + ";" + stemPart + ";" + currentSuffix;
        return output.replace("++", "+");
    }

    
    private static String getWordStem (String w, boolean withAffixes) throws InterruptedException, ClassNotFoundException, Exception
    {
        //if (w.contains("لِ+الْ+مَسْأَلَ+ةِ"))
        //    System.err.println();
        
        if (w.equals(";"))
            return w;
        String lastDiacriticRegex = "[" + processdiacritizedlexicon.ArabicUtils.buck2utf8("aiou") + "]+$";
        // try stemming
        String cliticSplit = "";
        if (w.contains("+") && !w.endsWith("+") && !w.startsWith("+"))
        {
            cliticSplit = ArabicUtils.removeDiacritics(w);
        }    
        else
        {
            ArrayList<String> clitics = stemmer.tag(w, true, false);
             // clitics.get(0);
            for (int c = 0; c < clitics.size(); c++) {
                if (!clitics.get(c).equals("_")) {
                    if (cliticSplit.trim().length() > 0) {
                        cliticSplit += "+";
                    }
                    cliticSplit += clitics.get(c);
                }
            }
        }


        cliticSplit = getProperSegmentationForDiacritization(cliticSplit);
        cliticSplit = processdiacritizedlexicon.ProcessDiacritizedLexicon.transferDiacriticsFromWordToSegmentedVersion(w.replace("+",""), cliticSplit);
        // prefixes & stem & suffixes 
        if (withAffixes)
        {
            return cliticSplit;
        }
        else
        {
            if (cliticSplit.contains(";"))
            {
                String taMarbouta = "";
                if (cliticSplit.contains(";+ة") || cliticSplit.contains(";+ت"))
                {
                    if (!cliticSplit.endsWith(";+ت"))
                        taMarbouta = "ة";
                }
                String stem = cliticSplit.substring(cliticSplit.indexOf(";") + 1);// .replaceFirst(".*?;", "").replaceFirst(";.*?", "");
                if (!stem.contains(";"))
                    return stem;
                    // System.err.println(stem);
                stem = stem.substring(0, stem.indexOf(";"));
                stem = stem.replace(";", "");
                if (taMarbouta.length() > 0)
                    stem += taMarbouta;
                else
                    stem = stem.replaceFirst(lastDiacriticRegex, "");
                return stem;
            }
            else
                return cliticSplit;
        }
    }
    
    public static void convertARPAfileToDiacritizationDictionary(String inputFile, String outputFile) throws FileNotFoundException, IOException, ClassNotFoundException, Exception
    {
        // stemmer = new POSTagger("/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/POSandNERData/");
        BufferedReader br = openFileForReading(inputFile);
        BufferedWriter bw = openFileForWriting(outputFile);
        
        String line = "";
        
        HashMap<String, String> words = new HashMap<String, String>();
        HashMap<String, HashMap<String, Double>> stems = new HashMap<String, HashMap<String, Double>>();
        
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\t");
            if (parts.length == 2 && parts[0].startsWith("-"))
            {
                String word = parts[1];
                String nonDiacritizedWord = ArabicUtils.removeDiacritics(word);
                
                String stem = getWordStem(word, false);
                String nonDiacritizedStem = ArabicUtils.removeDiacritics(stem);
                
//                ArrayList<String> stem = getStemmedWords(stemmer.tag(word, true, false));
//                String stemmedWord = "";
//                for (String dia : stem)
//                {
//                    if (stemmedWord.trim().length() > 0)
//                        stemmedWord += "+";
//                    stemmedWord += dia;
//                }
//                stemmedWord = getProperSegmentation(stemmedWord, hPrefixes, hSuffixes);
//                stemmedWord = transferDiacriticsFromWordToSegmentedVersion(word, stemmedWord);
                
                
                
                if (words.containsKey(nonDiacritizedWord))
                {
                    if (!words.get(nonDiacritizedWord).contains(";" + word + ";"))
                        words.put(nonDiacritizedWord, words.get(nonDiacritizedWord) + word + ";");
                }
                else
                    words.put(nonDiacritizedWord, ";" + word + ";");
                
                if (words.containsKey(nonDiacritizedStem))
                {
                    if (!words.get(nonDiacritizedStem).contains(";" + stem + ";"))
                        words.put(nonDiacritizedStem, words.get(nonDiacritizedStem) + stem + ";");
                }
                else
                    words.put(nonDiacritizedStem, ";" + stem + ";");
            }
        }
        
        for (String s : words.keySet())
            bw.write(s + "\t" + words.get(s) + "\n");
        
        br.close();
        bw.close();
    }
    
    public static void processProcessedFiles() throws FileNotFoundException, IOException {
        BufferedReader br = openFileForReading("/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort");
        // BufferedWriter bw = openFileForWriting("/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out.sort.out");
        BufferedWriter bw = openFileForWriting("/work/ARABIZI/DIACRITIZE/diacritizedWords.dictionary.from-text");
        // BufferedReader br = openFileForReading("/work/CLASSIC/misr.txt");
        // BufferedWriter bw = openFileForWriting("/work/CLASSIC/misr.txt.out");

        Map<String, String> lines = new TreeMap<String, String>();

        String line = "";
        String lastWord = "";
        Map<String, Float> count = new HashMap<String, Float>();
        int lineCount = 0;
        while ((line = br.readLine()) != null) {
            String[] parts = line.trim().split("[\\s\t]+");
            if (parts.length == 3 && parts[0].matches("[" + ArabicUtils.AllArabicLetters + "]+")) {
                parts[0] = ArabicUtils.utf82buck(parts[0]).trim();
                parts[0] = parts[0].replaceAll("[aiuoNKF~]+", "");
                parts[1] = parts[1].replace("MNM", "MuM").replace("MFM", "MaM").replace("MKM", "MiM");
                if (!parts[1].matches("^[\\.M]+$")) {
                    if (lines.containsKey(parts[0] + "\t" + parts[1])) {
                        int c = Integer.parseInt(parts[2]) + Integer.parseInt(lines.get(parts[0] + "\t" + parts[1]));
                        lines.put(parts[0] + "\t" + parts[1], Integer.toString(c));
                    } else {
                        lines.put(parts[0] + "\t" + parts[1], parts[2]);
                    }
                }
            }
            lineCount++;
        }
        for (String sLine : lines.keySet()) {
            line = sLine + "\t" + lines.get(sLine);
            String[] parts = line.trim().split("[\\s\t]+");
            if (parts.length == 3) {

                // parts[1] = parts[1].replaceAll("[aiuoNKF~]+", "");
                // parts[2] = ArabicUtils.utf82buck(parts[2]);
                if (!lastWord.equals(parts[0])) {
                    if (lastWord.length() > 0) {
                        if (count.size() > 1) {
                            while (count.size() > 1 && count.size() > mergeTemplates(sortByValues(count)).size()) {
                                count = mergeTemplates(sortByValues(count));
                            }
                            count = pruneBasedOnProbabilityMass(sortByValues(count), 0.95f);
                            while (count.size() > 1 && count.size() > mergeTemplatesLiberal(sortByValues(count)).size()) {
                                count = mergeTemplatesLiberal(sortByValues(count));
                            }
                            /*
                            while (count.size() > 1 && count.size() > mergeTemplatesExtraLiberal(sortByValues(count)).size()) {
                                count = mergeTemplatesExtraLiberal(sortByValues(count));
                            }
                                    */
                        }
                        Map<String, Float> sorted = sortByValues(count);
                        // get total count
                        bw.write(lastWord + "\t;");
                        for (String s : sorted.keySet()) {
                            bw.write(putDiacriticsBasedOnTemplate(lastWord, s).replace(".", "") + ";");
                            // bw.write(ArabicUtils.buck2utf8(lastWord) + "\t" + s + "\t" + sorted.get(s) + "\n");
                            // bw.flush();
                        }
                        bw.write("\n");
                        bw.flush();
                    }
                    // clear count;
                    count.clear();
                    lastWord = parts[0];
                }
                if (Integer.parseInt(parts[2]) > 1) {
                    // remove the case ending
                    String diacritics = parts[1];
                    if (count.containsKey(diacritics)) {
                        count.put(diacritics, count.get(diacritics) + Integer.parseInt(parts[2]));
                    } else {
                        count.put(diacritics, Float.parseFloat(parts[2]));
                    }
                }
            }
        }
        bw.close();
    }

    public static void processRawFiles() throws FileNotFoundException, IOException {
        BufferedReader br = openFileForReading("/work/CLASSIC/all-text.txt.diacritized-words.filtered.count");
        BufferedWriter bw = openFileForWriting("/work/CLASSIC/all-text.txt.diacritized-words.filtered.count.out");
        // BufferedReader br = openFileForReading("/work/CLASSIC/ahmed.txt");
        // BufferedWriter bw = openFileForWriting("/work/CLASSIC/ahmed.txt.out");
        String line = "";
        String lastWord = "";
        Map<String, Float> count = new HashMap<String, Float>();
        while ((line = br.readLine()) != null) {
            String[] parts = line.trim().split("[\\s\t]+");
            if (parts.length == 3 && parts[1].matches("[" + ArabicUtils.AllArabicLetters + "]+")) {
                parts[1] = ArabicUtils.utf82buck(parts[1]);
                parts[1] = parts[1].replaceAll("[aiuoNKF~]+", "");
                parts[2] = ArabicUtils.utf82buck(parts[2]);
                if (!lastWord.equals(parts[1])) {
                    if (lastWord.length() > 0) {
                        if (count.size() > 1) {
                            while (count.size() > mergeTemplates(count).size()) {
                                count = mergeTemplates(count);
                            }
                        }
                        Map<String, Float> sorted = sortByValues(count);
                        for (String s : sorted.keySet()) {
                            bw.write(ArabicUtils.buck2utf8(lastWord) + "\t" + s + "\t" + sorted.get(s) + "\n");
                            bw.flush();
                        }
                    }
                    // clear count;
                    count.clear();
                    lastWord = parts[1];
                }
                if (Integer.parseInt(parts[0]) > 1) {
                    // remove the case ending
                    parts[2] = parts[2].replaceAll("[aiuoNKF~]+$", "");
                    parts[2] = parts[2].replaceAll("^[aiuoNKF~]+", "");
                    String diacritics = getDiacriticsTemplate(parts[2]);
                    if (count.containsKey(diacritics)) {
                        count.put(diacritics, count.get(diacritics) + Integer.parseInt(parts[0]));
                    } else {
                        count.put(diacritics, Float.parseFloat(parts[0]));
                    }
                }

            }
        }
        bw.close();
    }

    public static Map<String, Float> mergeTemplates(Map<String, Float> count) {
        Map<String, Float> output = new TreeMap<String, Float>(count);
        for (String s : count.keySet()) {
            for (String ss : count.keySet()) {
                if (getNumberOfOccurancesofCharacterInString(ss, ".") <= 1
                        && getNumberOfOccurancesofCharacterInString(s, ".") <= 1
                        && editDistance(ss, s) <= 1) {
                    if (ss.matches(s) && count.get(ss) > count.get(s)) {
                        if (output.containsKey(ss) && output.containsKey(s)) {
                            output.put(ss, output.get(ss) + output.get(s));
                            output.remove(s);
                        }
                    } else if (s.matches(ss) && count.get(s) > count.get(ss)) {
                        if (output.containsKey(ss) && output.containsKey(s)) {
                            output.put(s, output.get(ss) + output.get(s));
                            output.remove(ss);
                        }
                    } else {
                        // do nothing
                    }
                }
            }
        }
        return output;
    }

    public static Map<String, Float> mergeTemplatesLiberal(Map<String, Float> count) {
        Map<String, Float> output = new TreeMap<String, Float>(count);
        for (String s : count.keySet()) {
            for (String ss : count.keySet()) {
                if (!s.equals(ss)
                        && getNumberOfOccurancesofCharacterInString(ss, ".") <= 1
                        && getNumberOfOccurancesofCharacterInString(s, ".") <= 1
                        && editDistance(ss, s) <= 1) {
                    if (ss.matches(s))// && count.get(ss) > 10 * count.get(s))
                    {
                        if (output.containsKey(ss) && output.containsKey(s)) {
                            output.put(ss, output.get(ss) + output.get(s));
                            output.remove(s);
                        }
                    } else if (s.matches(ss))// && count.get(s) > 10 * count.get(ss))
                    {
                        if (output.containsKey(ss) && output.containsKey(s)) {
                            output.put(s, output.get(ss) + output.get(s));
                            output.remove(ss);
                        }
                    } else {
                        // do nothing
                    }
                }
            }
        }
        return output;
    }

    public static boolean templatesMatch(String s, String ss) {
        boolean output = true;

        s = s.replace("~", "~.*");
        ss = ss.replace("~", "~.*");

        String[] ta = s.split("M");
        String[] tb = ss.split("M");

        if (ta.length == tb.length) {
            for (int i = 0; i < ta.length && output == true; i++) {
                if (ta[i].matches(tb[i]) || tb[i].matches(ta[i])) {
                    // we are good
                } else {
                    output = false;
                }
            }
        } else {
            output = false;
        }
        return output;
    }

    public static Map<String, Float> mergeTemplatesExtraLiberal(Map<String, Float> count) {
        Map<String, Float> output = new TreeMap<String, Float>(count);
        for (String s : count.keySet()) {
            for (String ss : count.keySet()) {
                if (!s.equals(ss)
                        && templatesMatch(s, ss)
                        && output.size() > 1) {
                    String[] ta = s.split("M");
                    String[] tb = ss.split("M");
                    String template = "";
                    for (int i = 0; i < ta.length; i++) {
                        ta[i] = ta[i].replace(".", "");
                        tb[i] = tb[i].replace(".", "");
                        if (i > 0) {
                            template += "M";
                        }
                        if (ta[i].length() > tb[i].length()) {
                            template += ta[i];
                        } else if (tb[i].length() > 0) {
                            template += tb[i];
                        } else {
                            template += ".";
                        }
                    }
                    int sum = 0;
                    boolean update = false;
                    if (output.containsKey(s)) {
                        sum += output.get(s);
                        output.remove(s);
                        update = true;
                    }
                    if (output.containsKey(ss)) {
                        sum += output.get(ss);
                        output.remove(ss);
                        update = true;
                    }
                    if (update) {
                        output.put(template, (float) sum);
                    }
                }
            }
        }
        return output;
    }

    public static int getNumberOfOccurancesofCharacterInString(String word, String Character) {
        int count = 0;
        for (int i = 0; i < word.length(); i++) {
            if (word.substring(i, i + 1).equals(Character)) {
                count++;
            }
        }
        return count;
    }

    public static String getDiacriticsTemplate(String input) {
        if (input.trim().length() == 0) {
            return "";
        }
        String output = "";
        String diacriticsRegex = "[aiuoNKF~\\.]+";
        if (!input.substring(input.length() - 1).matches(diacriticsRegex)) {
            input = input + ".";
        }
        ArrayList<String> positions = new ArrayList<String>();
        for (int i = 1; i < input.length(); i++) {
            if (!input.substring(i - 1, i).matches(diacriticsRegex)) {
                // previous character is NOT a diacritic
                if (input.substring(i, i + 1).matches(diacriticsRegex)) {
                    // if the character is a diacritic
                    positions.add(input.substring(i, i + 1));
                } else {
                    // if the character is not a diacritics, then a diacritic is missing
                    positions.add(".");
                }
            } else {
                // previous character is a diacritic
                // if current is a diacritic
                if (input.substring(i, i + 1).matches(diacriticsRegex)) {
                    // System.err.println(input);
                    positions.set(positions.size() - 1, positions.get(positions.size() - 1) + input.substring(i, i + 1));
                }
            }
        }
        if (positions.size() > 0) {
            for (int j = 0; j < positions.size() - 1; j++) {
                output += positions.get(j) + "M";
            }
            output += positions.get(positions.size() - 1);
        }
        return output;
    }

    public static <K extends Comparable, V extends Comparable> Map<K, V> sortByValues(Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new LinkedList<Map.Entry<K, V>>(map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {

            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<K, V> sortedMap = new LinkedHashMap<K, V>();

        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static BufferedReader openFileForReading(String filename) throws FileNotFoundException {
        BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename))));
        return sr;
    }

    public static BufferedWriter openFileForWriting(String filename) throws FileNotFoundException {
        BufferedWriter sw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename))));
        return sw;
    }

    public static int editDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i < dp.length; i++) {
            for (int j = 0; j < dp[i].length; j++) {
                dp[i][j] = i == 0 ? j : j == 0 ? i : 0;
                if (i > 0 && j > 0) {
                    if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                        dp[i][j] = dp[i - 1][j - 1];
                    } else {
                        dp[i][j] = Math.min(dp[i][j - 1] + 1, Math.min(
                                dp[i - 1][j - 1] + 1, dp[i - 1][j] + 1));
                    }
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    public static String getDiacritizedStem(String input, String template) {
        String output = "";
        String[] word = input.split("\\+");
        String currentPrefix = "";
        String currentSuffix = "";
        int iValidPrefix = -1;
        while (iValidPrefix + 1 < word.length && hPrefixes.containsKey(word[iValidPrefix + 1])) {
            iValidPrefix++;
        }

        int iValidSuffix = word.length;

        while (iValidSuffix > Math.max(iValidPrefix, 0) && (hSuffixes.containsKey(word[iValidSuffix - 1])
                || word[iValidSuffix - 1].equals("_"))) {
            iValidSuffix--;
        }

        for (int i = 0; i <= iValidPrefix; i++) {
            currentPrefix += word[i] + "+";
        }
        String stemPart = "";
        for (int i = iValidPrefix + 1; i < iValidSuffix; i++) {
            stemPart += word[i];
        }

        if (iValidSuffix == iValidPrefix) {
            iValidSuffix++;
        }

        for (int i = iValidSuffix; i < word.length && iValidSuffix != iValidPrefix; i++) {
            currentSuffix += "+" + word[i];
        }
        currentPrefix = currentPrefix.replace("+", "");
        currentSuffix = currentSuffix.replace("+", "");
        // output = currentPrefix + stemPart + currentSuffix;
        for (int i = 0; i < currentPrefix.length(); i++) {
            template = template.replaceFirst("^[aiuoKNF~\\.]+M", "");
        }
        for (int i = 0; i < currentSuffix.length(); i++) {
            template = template.replaceFirst("M[aiuoKNF~\\.]+$", "");
        }
        stemPart = stemPart.replace("+", "");

        if (stemPart.trim().length() > 0) {
            stemPart = putDiacriticsBasedOnTemplate(stemPart, template);
        }

        return ArabicUtils.utf82buck(currentPrefix) + "+" + stemPart + "+" + ArabicUtils.utf82buck(currentSuffix);
    }
}
