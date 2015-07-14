/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package processdiacritizedlexicon;

import static ArabicPOSTagger.ArabicUtils.prefixes;
import static ArabicPOSTagger.ArabicUtils.suffixes;
import ArabicPOSTagger.POSTagger;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import static processdiacritizedlexicon.ArabicUtils.normalizeFull;
import static processdiacritizedlexicon.ArabicUtils.removeDiacritics;
import static processdiacritizedlexicon.ProcessDiacritizedLexicon.transferDiacriticsFromWordToSegmentedVersion;

/**
 *
 * @author kareemdarwish
 */
public class DiacritizeText {

    private static String baseDir = "";
    private static String kenlmDir = "";
    private static String dataDirectory = "";
    private static Process process = null;
    private static Process process2ndLM = null;
    private static BufferedReader brLM = null;
    private static BufferedWriter bwLM = null;
    private static BufferedReader brLM2ndLM = null;
    private static BufferedWriter bwLM2ndLM = null;
    private static TMap<String, String> candidatesUnigram = new THashMap<String, String>();
    private static final HashMap<String, Integer> hPrefixes = new HashMap<String, Integer>();
    private static final HashMap<String, Integer> hSuffixes = new HashMap<String, Integer>();
    public static HashMap<String, String> diacritizedPrefixes = new HashMap<String, String>();
    public static HashMap<String, String> diacritizedSuffixes = new HashMap<String, String>();
    public static TMap<String, Integer> seenWordsMap = new THashMap<String, Integer>();
    public static TMap<String, String> wordsWithSingleDiacritizations = new THashMap<String, String>();
    public static TMap<String, String> bigramsWithSingleDiacritizations = new THashMap<String, String>();
    
    
    public static TMap<String, String> defaultDiacritizationBasedOnTemplateProbability = new THashMap<String, String>();
    
    public static POSTagger tagger = null; 
    // new POSTagger("/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/POSandNERData/");
    
    public DiacritizeText(String dir, String lmFile1, String lmFile2, String dictionaryFile) throws IOException, FileNotFoundException, ClassNotFoundException, InterruptedException {
        baseDir = dir;
        kenlmDir = System.getProperty("java.library.path");
        dataDirectory = System.getProperty("java.library.path");
        //System.err.println("Args: "+dir+" lmf1:"+lmFile1+" lmf2:"+lmFile2+" dictf:"+dictionaryFile);
        if (!dir.endsWith("/")) {
            baseDir += "/";
        }
        if (!kenlmDir.endsWith("/")) {
            kenlmDir += "/";
        }

        if (lmFile1.trim().length() > 0)
        {
            String[] args = {
                kenlmDir + "query",
                // baseDir + "train.blm.3g"
                lmFile1
            };

            process = new ProcessBuilder(args).start();

            brLM = new BufferedReader(new InputStreamReader(process.getInputStream()));
            bwLM = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        }
        
        
        if (lmFile2.trim().length() > 0)
        {
            String[] args2ndLM = {
                kenlmDir + "query",
                // "/work/CLASSIC/LDC/allText.txt.nocase.blm"
                lmFile2
            };

            process2ndLM = new ProcessBuilder(args2ndLM).start();

            brLM2ndLM = new BufferedReader(new InputStreamReader(process2ndLM.getInputStream()));
            bwLM2ndLM = new BufferedWriter(new OutputStreamWriter(process2ndLM.getOutputStream()));
        }
        
        // candidatesUnigram = loadCandidates(baseDir + "diacritizedWords.dictionary.1.0"); // "train.arpa.1g.dictionary.out.0.90");
        candidatesUnigram = loadCandidates(dictionaryFile);
        // loadSeenWords();

        tagger = new POSTagger(dataDirectory);
        
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
        
        for (int i = 0; i < prefixes.length; i++) {
            hPrefixes.put(prefixes[i].toString(), 1);
        }
        for (int i = 0; i < suffixes.length; i++) {
            hSuffixes.put(suffixes[i].toString(), 1);
        }
        
        diacritizedPrefixes.put(ArabicUtils.buck2utf8("w"), ArabicUtils.buck2utf8("wa"));
        diacritizedPrefixes.put(ArabicUtils.buck2utf8("s"), ArabicUtils.buck2utf8("sa"));
        diacritizedPrefixes.put(ArabicUtils.buck2utf8("f"), ArabicUtils.buck2utf8("fa"));
        diacritizedPrefixes.put(ArabicUtils.buck2utf8("k"), ArabicUtils.buck2utf8("ka"));
        diacritizedPrefixes.put(ArabicUtils.buck2utf8("b"), ArabicUtils.buck2utf8("bi"));
        diacritizedPrefixes.put(ArabicUtils.buck2utf8("l"), ArabicUtils.buck2utf8("li"));
        diacritizedPrefixes.put(ArabicUtils.buck2utf8("ll"), ArabicUtils.buck2utf8("lilo"));
        diacritizedPrefixes.put(ArabicUtils.buck2utf8("Al"), ArabicUtils.buck2utf8("Aalo"));

        diacritizedSuffixes.put(ArabicUtils.buck2utf8("hmA"), ArabicUtils.buck2utf8("humA"));
        diacritizedSuffixes.put(ArabicUtils.buck2utf8("km"), ArabicUtils.buck2utf8("kum"));
        diacritizedSuffixes.put(ArabicUtils.buck2utf8("hm"), ArabicUtils.buck2utf8("hum"));
        diacritizedSuffixes.put(ArabicUtils.buck2utf8("hn"), ArabicUtils.buck2utf8("hun"));
        
        BufferedReader brDiacritizationBasedOnTemplateProb = ProcessDiacritizedLexicon.openFileForReading(dataDirectory+"/diacritizedWords.dictionary.template.1.0");
        String line = "";
        while ((line = brDiacritizationBasedOnTemplateProb.readLine()) != null)
        {
            String[] parts = line.split("\t");
            if (parts.length == 2)
            {
                defaultDiacritizationBasedOnTemplateProbability.put(parts[0], parts[1]);
            }
        }
        
        BufferedReader brWordsWithOneSolution = ProcessDiacritizedLexicon.openFileForReading(dataDirectory+"/rdi-text.txt.tok.count.out");
        line = "";
        while ((line = brWordsWithOneSolution.readLine()) != null)
        {
            String[] parts = line.split("\t");
            if (parts.length == 4)
            {
                wordsWithSingleDiacritizations.put(parts[0], parts[1]);
            }
        }
        
        brWordsWithOneSolution = ProcessDiacritizedLexicon.openFileForReading(dataDirectory+"/rdi-text.txt.tok.bigrams");
        line = "";
        while ((line = brWordsWithOneSolution.readLine()) != null)
        {
            String[] parts = line.split("\t");
            if (parts.length == 2)
            {
                String diacritizedBigram = parts[1].trim().substring(0, parts[1].trim().lastIndexOf(" "));
                int diacritizedBigramCount = Integer.parseInt(parts[1].trim().substring(parts[1].trim().lastIndexOf(" ") + 1));
                bigramsWithSingleDiacritizations.put(parts[0], diacritizedBigram);
            }
        }
        
    }

    public TMap<String, String> getwordsWithSingleDiacritizations()
    {
        return wordsWithSingleDiacritizations;
    }
    
    public TMap<String, String> getbigramsWithSingleDiacritizations()
    {
        return bigramsWithSingleDiacritizations;
    }
    
    public ArrayList<String> tagWords(String inputText) throws InterruptedException, ClassNotFoundException, Exception
    {
        ArrayList<String> output = tagger.tag(inputText, false, true);
        return output;
    }
    
    public void loadSeenWords() throws FileNotFoundException, IOException
    {
        BufferedReader br = ProcessDiacritizedLexicon.openFileForReading(dataDirectory+"/all-text.txt.diacritized.full-tok.uni.count");
        String line = "";
        while ((line = br.readLine()) != null)
        {
            String[] parts = line.split("\t");
            if (parts[0].trim().length() > 0 && parts[1].matches("[0-9]+"))
            {
                parts[0] = ProcessDiacritizedLexicon.standardizeDiacritics(parts[0].replace("+", "").replace("_", ""));
                seenWordsMap.put(parts[0], Integer.parseInt(parts[1]));
            }
        }
    }
    
    public String diacritize(String input) throws IOException, ClassNotFoundException, Exception {
        HashMap<Integer, ArrayList<String>> latice = buildLaticeStem(processdiacritizedlexicon.ArabicUtils.tokenize(input));
        return findBestPath(latice);
    }
    
    public String diacritize(ArrayList<String> input) throws IOException, ClassNotFoundException, Exception {
        HashMap<Integer, ArrayList<String>> latice = buildLaticeStem(input);
        return findBestPath(latice);
    }

    public String limitNumberOfChoices(String input, int max)
    {
        String output = ";";
        String[] parts = input.split(";");
        for (int i = 0; i <= Math.min(parts.length - 1, max); i++)
        {
            output += parts[i] + ";";
        }
        return output;
    }
    
    private TMap<String, String> loadCandidates(String filePath) throws FileNotFoundException, IOException, ClassNotFoundException {
        TMap<String, String> candidates = new THashMap<String, String>();
        // char[] tab = {'\t'};

        String line = "";
        // if (wikipediaArEn.isEmpty())
        File file = new File(filePath + ".ser");
//        if (file.exists()) {

            // System.out.println(df.format(cal.getTime()));
//            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
//            candidates = (THashMap) ios.readObject();
            // System.out.println(df.format(cal.getTime()));
//        } else {
            BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))));
            while ((line = sr.readLine()) != null) {
                if (line.length() > 0) {
                    String[] lineParts = line.split("\t");
                    if (line.length() > 0 && lineParts.length > 0) // && Regex.IsMatch("^[0-9\\.\\-]$"))
                    {
                        candidates.put(lineParts[0], limitNumberOfChoices(lineParts[1], 100));
                        /*
                         String norm = normalizeFull(line);
                         if (!candidates.containsKey(norm)) {
                         candidates.put(norm, line);
                         }
                         else
                         {
                         String temp = candidates.get(norm) + " " + line;
                         candidates.put(norm, temp);
                         }
                         */
                    }
                }
            }
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(candidates);
            oos.close();
//        }
        return candidates;
    }

    private boolean checkIfAllChoicesHaveTheSameScore (ArrayList<String> unigramChoices) throws IOException
    {
        double bestScore = -1000;
        boolean sameScore = true;
        for (int i = 0; i < unigramChoices.size(); i++) { // s : paths) {
            // double finalScore = scoreUsingLM(unigramChoices.get(i));
            double finalScore = scoreUsingTwoLMs(unigramChoices.get(i));
            if (bestScore != finalScore && i > 0)
                sameScore = false;
            if (bestScore < finalScore)
                bestScore = finalScore;
        }
        return sameScore;
    }
    
    private String GetBestChoiceUnigram (ArrayList<String> unigramChoices) throws IOException
    {
        double bestScore = -1000;
        String bestChoice = "";
        for (int i = 0; i < unigramChoices.size(); i++) { // s : paths) {
            // double finalScore = scoreUsingLM(unigramChoices.get(i));
            double finalScore = scoreUsingTwoLMs(unigramChoices.get(i));
            if (bestScore < finalScore)
            {
                bestScore = finalScore;
                bestChoice = unigramChoices.get(i);
            }
        }
        return bestChoice;
    }
    
    private String getWordStem (String w, boolean withAffixes) throws InterruptedException, ClassNotFoundException, Exception
    {
        String lastDiacriticRegex = "[" + processdiacritizedlexicon.ArabicUtils.buck2utf8("aiou") + "]+$";
        // try stemming
        ArrayList<String> clitics = tagger.tag(w, true, false);
        String cliticSplit = "";
                    for (int c = 0; c < clitics.size(); c++) {
                        if (!clitics.get(c).equals("_"))
                        {
                            if (cliticSplit.trim().length() > 0)
                                cliticSplit += "+";
                            cliticSplit += clitics.get(c);
                        }
                    }

        cliticSplit = getProperSegmentation(cliticSplit);
        cliticSplit = processdiacritizedlexicon.ProcessDiacritizedLexicon.transferDiacriticsFromWordToSegmentedVersion(w, cliticSplit);
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
    
    private String checkIfOOVandGetMostLikelyUnigramSolution(ArrayList<String> unigramChoices) throws IOException, FileNotFoundException, ClassNotFoundException, Exception
    {
        
        String output = "***";
        if (unigramChoices.size() == 1)
            return unigramChoices.get(0);
        else
        {
            // check if they have same score
            boolean sameScore = checkIfAllChoicesHaveTheSameScore(unigramChoices);
            if (sameScore)
            {
                // System.err.println(unigramChoices.get(0));

                // attempt to stem and find the best solution
                ArrayList<String> stems = new ArrayList<String>();
                HashMap<String, String> stemToWordMap = new HashMap<String, String>();
                for (String s : unigramChoices)
                {
                    String stem = getWordStem(s, false);
                    stems.add(stem.replaceFirst("[" + processdiacritizedlexicon.ArabicUtils.buck2utf8("aiou") + "]+$", ""));
                    stemToWordMap.put(stem, s);
                }
//                if (ArabicUtils.removeDiacritics(stems.get(0)).equals("حياة"))
//                    System.err.println();
                if (checkIfAllChoicesHaveTheSameScore(stems))
                {
                    // System.err.println("**" + unigramChoices.get(0) + "\t" + stems.get(0));
                    // revert to the most commonly used template
                    if (defaultDiacritizationBasedOnTemplateProbability.containsKey(ArabicUtils.removeDiacritics(stems.get(0))))
                    {
                        output = defaultDiacritizationBasedOnTemplateProbability.get(ArabicUtils.removeDiacritics(stems.get(0)));
                        if (stemToWordMap.containsKey(output))
                            output = stemToWordMap.get(output);
                        else
                        {
                            // get original prefixes and suffixes
                            String stemWithPrefixesAndSuffixes = getWordStem(ArabicUtils.removeDiacritics(unigramChoices.get(0)), true);
                            if (stemWithPrefixesAndSuffixes.contains(";"))
                            {
                                String Prefixes = stemWithPrefixesAndSuffixes.replaceFirst(";.*", "");
                                String Suffixes = stemWithPrefixesAndSuffixes.replaceFirst(".*;", "");
                                if (Suffixes.startsWith("+ة"))
                                    Suffixes = Suffixes.substring(2);
                                else if (Suffixes.startsWith("+ت") && !Suffixes.endsWith("+ت"))
                                {
                                    Suffixes = Suffixes.substring(2);
                                    if (output.endsWith("ة"))
                                        output = output.replace("ة", "ت");
                                }
                                output = diacritizePrefixes(Prefixes) + output + diacritizeSuffixes(Suffixes, "");
                            }
                        }
                    }
                    //System.err.println("**" + unigramChoices.get(0) + "\t" + stems.get(0) + "\tusing: " + output);
                }
                else
                {
                    String bestChoice = GetBestChoiceUnigram(stems);
                    output = stemToWordMap.get(bestChoice);
                }
            }
            return output;
        }
    }
    
    private String findBestPath(HashMap<Integer, ArrayList<String>> latice) throws IOException, Exception {
        String space = " +";
        HashMap<Integer, String> finalAnswer = new HashMap<Integer, String>();
        // finalAnswer.put(0, "<s>");
        // finalAnswer.put(0, " ");

        for (int i = 1; i <= latice.keySet().size() - 1; i++) {
            String sBase = ""; finalAnswer.get(0);
            for (int j = 1; j < i; j++) {
                sBase += " " + finalAnswer.get(j);
            }

            ArrayList<String> paths = new ArrayList<String>();
            if (checkIfOOVandGetMostLikelyUnigramSolution(latice.get(i)).equals("***"))
            {
                // add options for current node
                for (String sol : latice.get(i)) {
                    paths.add(sBase + " " + sol);
                }
            }
            else
            {
                paths.add(sBase + " " + checkIfOOVandGetMostLikelyUnigramSolution(latice.get(i)));
            }
            
            ArrayList<String> pathsNext = new ArrayList<String>();
            // add options for next node
            for (String s : paths) {
                // System.err.println(i);
                for (String sol : latice.get(i + 1)) {
                    pathsNext.add(s + " " + sol);
                }
            }

            // determine best option for current word
            // this would be done using the language model
            String bestPathOutput = findBestPathLM(pathsNext).trim();
            
            String[] bestPath = bestPathOutput.split(" +");
            
            if (bestPath.length == i + 1 || bestPath.length == i) { // + 2) {
                finalAnswer.put(i, bestPath[i - 1]);
            } else {
                System.err.println("ERROR");
            }
        }
        String sBest = ""; // finalAnswer.get(1);
        for (int k = 1; k <= finalAnswer.keySet().size(); k++) {
            sBest += " " + finalAnswer.get(k);
        }
        return sBest.replaceAll(" +", " ").trim();
    }

    private String correctLeadingLamAlefLam(String s) {
        if (s.startsWith("لال")) {
            s = "لل" + s.substring(3);
        }
        return s;
    }

    public double scoreUsingLM(String s) throws IOException {
        bwLM.write(s + "\n");
        bwLM.flush();
        String stemp = brLM.readLine();
        if (stemp.contains("Total:")) {
            stemp = stemp.replaceFirst(".*Total\\:", "").trim();
            stemp = stemp.replaceFirst("OOV.*", "").trim();
        } else {
            stemp = "-1000";
        }
        if (stemp.contains("inf"))
            return -1000f;

        double finalScore = Double.parseDouble(stemp);
        return finalScore;
    }
    
    public double scoreUsingTwoLMs(String s) throws IOException {
        String stemp = "0";
        //System.err.println("Score: "+s);
        
        if (bwLM != null && brLM != null)
        {
            bwLM.write(s + "\n");
            bwLM.flush();

            stemp = brLM.readLine();
            if (stemp.contains("Total:")) {
                stemp = stemp.replaceFirst(".*Total\\:", "").trim();
                stemp = stemp.replaceFirst("OOV.*", "").trim();
            } else {
                stemp = "-100";
            }
        }
        double firstScore = 0d;
        if (stemp.contains("inf"))
            firstScore = -100f;
        else
            firstScore = Double.parseDouble(stemp);
        
        String stemp2ndLM = "0";
        if (bwLM2ndLM != null && brLM2ndLM != null)
        {
            bwLM2ndLM.write(s + "\n");
            bwLM2ndLM.flush();
            stemp2ndLM = brLM2ndLM.readLine();
            if (stemp2ndLM.contains("Total:")) {
                stemp2ndLM = stemp2ndLM.replaceFirst(".*Total\\:", "").trim();
                stemp2ndLM = stemp2ndLM.replaceFirst("OOV.*", "").trim();
            } else {
                stemp2ndLM = "-100";
            }
        }
        double secondScore = 0d;
        if (stemp2ndLM.contains("inf"))
            secondScore = -100f;
        else
            secondScore = Double.parseDouble(stemp2ndLM);

        double finalScore = 0d;
        if (firstScore > -100 && secondScore > -100)
            finalScore = 0.1 * firstScore + 0.9 * secondScore;
        else
            finalScore = Math.min(firstScore, secondScore);

        return finalScore;
    }
    
    private String findBestPathLM(ArrayList<String> paths) throws IOException {
        if (paths.size() == 1)
        {
            return paths.get(0);
        }
        else
        {
            double bestScore = -1000;
            boolean sameScore = true;
            String bestPath = "";
            for (int i = 0; i < paths.size(); i++) { // s : paths) {
                // only score the last n words
                String s = paths.get(i);
                String ss = getTheTrailingNWords(s, 5);
                // double finalScore = scoreUsingLM(ss);
                double finalScore = scoreUsingTwoLMs(ss);
                if (bestScore != finalScore && i > 0)
                    sameScore = false;
                if (bestScore < finalScore) {
                    bestScore = finalScore;
                    bestPath = s;
                }
            }
            // if (sameScore)
            //    bestPath += "***";
            return bestPath;
        }
    }

    private String getTheTrailingNWords(String s, int n)
    {
        String ss = "";
        String[] parts = s.split(" +");
        if (parts.length <= n)
            return s;
        else
        {
            for (int i = parts.length - n; i < parts.length; i++)
                ss += parts[i] + " ";
        }
        return ss.trim();
    }
    
    private HashMap<Integer, ArrayList<String>> buildLatice(ArrayList<String> words) {
        HashMap<Integer, ArrayList<String>> latice = new HashMap<Integer, ArrayList<String>>();
        int i = 0;

        ArrayList<String> temp = new ArrayList<String>();
        // temp.add("<s>");
        temp.add(" ");
        i++;
        latice.put(i, temp);

        for (String w : words) {
            // if (bStem == false) {
            String norm = ArabicUtils.removeDiacritics(w); // correctLeadingLamAlefLam(normalizeFull(w));
            if (candidatesUnigram.containsKey(norm) && candidatesUnigram.get(norm).split(";").length > 0) {
                temp = new ArrayList<String>();
                for (String s : candidatesUnigram.get(norm).split(";")) {
                    if (s.length() > 0) {
                        if (!temp.contains(s)) {
                            temp.add(s);
                        }
                    }
                }

                // if multiple candidates exist and one does not have diacritics, then remove it
                if (temp.size() > 1) {
                    ArrayList<String> ttemp = new ArrayList<String>(temp);
                    for (String t : temp) {
                        //                    if (!t.matches(".*[ًٌٍُِّْ].*"))
                        {
                            // put phoney dicritics and see if they get removed
                            String tt = "";
                            for (int k = 0; k < t.length(); k++) {
                                if (t.substring(k, k + 1).matches("[ايو]")) {
                                    tt += t.substring(k, k + 1);
                                } else {
                                    if (k < t.length() - 1) {
                                        if (t.substring(k + 1, k + 2).matches("[ايو]")) {
                                            tt += t.substring(k, k + 1);
                                        } else {
                                            tt += t.substring(k, k + 1) + processdiacritizedlexicon.ArabicUtils.buck2utf8("a");
                                        }
                                    } else {
                                        tt += t.substring(k, k + 1) + processdiacritizedlexicon.ArabicUtils.buck2utf8("a");
                                    }
                                }
                            }
                            if (tt.matches(".*[ًٌٍَُِّْ].*")) {
                                ttemp.remove(t);
                            }
                        }
                    }
                    if (ttemp.size() > 0 && temp.size() != ttemp.size()) {
                        temp = new ArrayList<String>(ttemp);
                    }
                }

                if (temp.size() > 0) {
                    latice.put(i, temp);
                }
            } else {
                temp = new ArrayList<String>();
                temp.add(w);
                latice.put(i, temp);
            }
            i++;
        }
        temp = new ArrayList<String>();
        // temp.add("</s>");
        temp.add(" ");
        latice.put(i, temp);

        return latice;
    }

    public String diacritizePrefixes(String prefixString)
    {
        String[] tmpP = prefixString.split("\\+");
        ArrayList<String> prefixes = new ArrayList<String>();
        for (String p : tmpP)
            if (p.length() > 0)
                prefixes.add(p);
        return diacritizePrefixes(prefixes);
    }
    
    public String diacritizePrefixes(ArrayList<String> prefixes)
    {
        String diacritizedWord = "";
        for (String p : prefixes) {
            if (p.length() > 0) {
                diacritizedWord += diacritizedPrefixes.get(p);
            }
        }
        return diacritizedWord;
    }
    
    public String diacritizeSuffixes(String suffixString, String caseEnding)
    {
        String[] tmpS = suffixString.split("\\+");
        ArrayList<String> suffixes = new ArrayList<String>();
        for (String s : tmpS)
            if (s.length() > 0)
                suffixes.add(s);
        return diacritizeSuffixes(suffixes, caseEnding);
    }
    
    public String diacritizeSuffixes(ArrayList<String> suffixes, String caseEnding)
    {
        String diacritizedWord = "";
        for (String p : suffixes) {
            if (p.length() > 0) {
                if (p.equals("ة") || p.equals("ت"))
                {
                    diacritizedWord += p + caseEnding;
                }
                else if (diacritizedSuffixes.containsKey(p)) {
                    diacritizedWord += diacritizedSuffixes.get(p);
                } else {
                    diacritizedWord += p;
                }
            }
        }
        return diacritizedWord;
    }
    
    public HashMap<Integer, ArrayList<String>> buildLaticeStem(ArrayList<String> words) throws InterruptedException, ClassNotFoundException, Exception {
        HashMap<Integer, ArrayList<String>> latice = new HashMap<Integer, ArrayList<String>>();
        int i = 0;

        String[] diacritics = {""}; // a", "i", "o", "u", "N", "K", "F", ""};
        
        ArrayList<String> temp = new ArrayList<String>();
        // temp.add("<s>");
        temp.add(" ");
        i++;
        latice.put(i, temp);

        for (String w : words) {
            //if (w.trim().equals("محصن"))
                //System.err.println();
            if (w.length() > 0) {
                String norm = ArabicUtils.removeDiacritics(w); // correctLeadingLamAlefLam(normalizeFull(w));
                if (candidatesUnigram.containsKey(norm) && candidatesUnigram.get(norm).split(";").length > 0) {
                    temp = new ArrayList<String>();
                    for (String s : candidatesUnigram.get(norm).split(";")) {
                        if (s.length() > 0) {
                            if (!temp.contains(s)) {
                                for (String di : diacritics)
                                {
                                    if (!(di.matches("[NKF]") && s.endsWith("ّ")) && !temp.contains(s + ArabicUtils.buck2utf8(di))) // && seenWordsMap.containsKey(s)) // don't put tanween with shadda
                                        temp.add(s + ArabicUtils.buck2utf8(di));
                                }
                            }
                        }
                    }
                    if (temp.size() > 0) {
                        latice.put(i, temp);
                    }
                } 
                else 
                {
                    // try stemming
                    ArrayList<String> clitics = tagger.tag(w, true, false);
                    String cliticSplit = "";
                    for (int c = 0; c < clitics.size(); c++) {
                        if (!clitics.get(c).equals("_"))
                        {
                            if (cliticSplit.trim().length() > 0)
                                cliticSplit += "+";
                            cliticSplit += clitics.get(c);
                        }
                    }

                    cliticSplit = getProperSegmentation(cliticSplit);

                    // prefixes & stem & suffixes 
                    cliticSplit = cliticSplit.replace("ل+ال", "لل");
                    
                    String[] tprefix = cliticSplit.replaceFirst(";.*", "").split("\\+");
                    ArrayList<String> prefixes = new ArrayList<String>();
                    for (String p : tprefix)
                        if (p.trim().length() > 0)
                            prefixes.add(p);
                    
                    String[] tsuffix = cliticSplit.replaceFirst(".*;", "").split("\\+");
                    ArrayList<String> suffixes = new ArrayList<String>();
                    for (String p : tsuffix)
                        if (p.trim().length() > 0)
                            suffixes.add(p);
                    
                    String stem = cliticSplit.substring(cliticSplit.indexOf(";") + 1);// .replaceFirst(".*?;", "").replaceFirst(";.*?", "");
                    stem = stem.substring(0, stem.indexOf(";"));
                    stem = stem.replace(";", "");

                    if (candidatesUnigram.containsKey(stem) && candidatesUnigram.get(stem).split(";").length > 0) {
                        temp = new ArrayList<String>();
                        for (String s : candidatesUnigram.get(stem).split(";")) {
                            for (String di : diacritics)
                            {
                                if (s.length() > 0) {
                                    String diacritizedWord = diacritizePrefixes(prefixes);
                                    /*
                                    String diacritizedWord = "";
                                    for (String p : prefixes) {
                                        if (p.length() > 0) {
                                            diacritizedWord += diacritizedPrefixes.get(p);
                                        }
                                    }
                                    */
                                    if (
                                            // suffixes.size() > 0 && suffixes.get(0).equals("ة")
                                            (suffixes.size() > 0 && suffixes.get(0).equals("ة")) 
                                            || 
                                            (suffixes.size() > 1 && suffixes.get(0).equals("ت"))
                                            ) // if the first suffix is ta marbouta, put fatha
                                        if (!stem.endsWith("ا"))
                                            diacritizedWord += s + ArabicUtils.buck2utf8("a");
                                        else 
                                            diacritizedWord += s;
                                    else
                                    {
                                        if (!(di.matches("[NKF]") && s.endsWith("ّ"))) // don't put tanween with shadda
                                            diacritizedWord += s + ArabicUtils.buck2utf8(di);
                                    }
                                    
                                    diacritizedWord += diacritizeSuffixes(suffixes, ArabicUtils.buck2utf8(di));
                                    /*
                                    for (String p : suffixes) {
                                        if (p.length() > 0) {
                                            if (p.equals("ة") || p.equals("ت"))
                                            {
                                                diacritizedWord += p + ArabicUtils.buck2utf8(di);
                                            }
                                            else if (diacritizedSuffixes.containsKey(p)) {
                                                diacritizedWord += diacritizedSuffixes.get(p);
                                            } else {
                                                diacritizedWord += p;
                                            }
                                        }
                                    }
                                    */
                                    if (!temp.contains(diacritizedWord)) // && seenWordsMap.containsKey(diacritizedWord)) {
                                    {
                                        temp.add(diacritizedWord);
                                    }
                                }
                            }
                        }
                        latice.put(i, temp);
                    } 
                    // handle the case of having a ya at the end of the stem
                    else if (stem.endsWith("ي") && candidatesUnigram.containsKey(stem.substring(0, stem.length() - 1)) && candidatesUnigram.get(stem.substring(0, stem.length() - 1)).split(";").length > 0)
                    {
                        temp = new ArrayList<String>();
                        for (String s : candidatesUnigram.get(stem.substring(0, stem.length() - 1)).split(";")) {
                            for (String di : diacritics)
                            {
                                if (s.length() > 0) {
                                    String diacritizedWord = "";
                                    for (String p : prefixes) {
                                        if (p.length() > 0) {
                                            diacritizedWord += diacritizedPrefixes.get(p);
                                        }
                                    }

                                    // remove trailing diacritics and put ya at the end
                                    
                                    diacritizedWord += s + "ي";
                                    if (
                                            (suffixes.size() > 0 && suffixes.get(0).equals("ة")) 
                                            || 
                                            (suffixes.size() > 1 && suffixes.get(0).equals("ت"))
                                            ) // if the first suffix is ta marbouta, put fatha
                                        diacritizedWord += ArabicUtils.buck2utf8("~a");
                                    
                                    for (String p : suffixes) {
                                        if (p.length() > 0) {
                                            if (p.equals("ة") || p.equals("ت"))
                                            {
                                                diacritizedWord += p + ArabicUtils.buck2utf8(di);
                                            }
                                            else if (diacritizedSuffixes.containsKey(p)) {
                                                diacritizedWord += diacritizedSuffixes.get(p);
                                            } else {
                                                diacritizedWord += p;
                                            }
                                        }
                                    }

                                    if (!temp.contains(diacritizedWord)) // && seenWordsMap.containsKey(diacritizedWord)) {
                                    {
                                        temp.add(diacritizedWord);
                                    }
                                }
                            }
                        }
                        latice.put(i, temp);
                    }
                    // handle the case of having a At as a suffix which often means that ta marbouta was removed
                    else if (suffixes.size() > 0 && suffixes.get(0).equals("ات") 
                            && candidatesUnigram.containsKey(stem + "ة") 
                            && candidatesUnigram.get(stem + "ة").split(";").length > 0)
                    {
                        temp = new ArrayList<String>();
                        for (String s : candidatesUnigram.get(stem + "ة").split(";")) {
                            for (String di : diacritics)
                            {
                                if (s.length() > 0) {
                                    String diacritizedWord = "";
                                    for (String p : prefixes) {
                                        if (p.length() > 0) {
                                            diacritizedWord += diacritizedPrefixes.get(p);
                                        }
                                    }

                                    // remove trailing ta marbouta
                                    String fatha = processdiacritizedlexicon.ArabicUtils.buck2utf8("a");
                                    diacritizedWord += s.replace("ة", "").replaceFirst(fatha + "$", ""); // remove last fatha if it exists
                                    
                                    if (
                                            (suffixes.size() > 0 && suffixes.get(0).equals("ة")) 
                                            || 
                                            (suffixes.size() > 1 && suffixes.get(0).equals("ت"))
                                            ) // if the first suffix is ta marbouta, put fatha
                                        diacritizedWord += ArabicUtils.buck2utf8("~a");
                                    
                                    for (String p : suffixes) {
                                        if (p.length() > 0) {
                                            if (p.equals("ة") || p.equals("ت"))
                                            {
                                                diacritizedWord += p + ArabicUtils.buck2utf8(di);
                                            }
                                            else if (diacritizedSuffixes.containsKey(p)) {
                                                diacritizedWord += diacritizedSuffixes.get(p);
                                            } else {
                                                diacritizedWord += p;
                                            }
                                        }
                                    }

                                    if (!temp.contains(diacritizedWord)) // && seenWordsMap.containsKey(diacritizedWord)) {
                                    {
                                        temp.add(diacritizedWord);
                                    }
                                }
                            }
                        }
                        latice.put(i, temp);
                    }
                    else {
                        temp = new ArrayList<String>();
                        temp.add(w);
                        latice.put(i, temp);
                    }
                }
            }
            else
            {
                temp = new ArrayList<String>();
                temp.add("");
                latice.put(i, temp);
            }
            i++;
        }
        temp = new ArrayList<String>();
        // temp.add("</s>");
        temp.add(" ");
        latice.put(i, temp);

        return latice;
    }

    public String getProperSegmentation(String input)
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

}
