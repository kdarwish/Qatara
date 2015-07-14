 
import ArabicPOSTagger.*;
import processdiacritizedlexicon.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import ArabicPOSTagger.POSTagger;
import processdiacritizedlexicon.ArabicUtils;
import static processdiacritizedlexicon.ArabicUtils.prefixes;
import static processdiacritizedlexicon.ArabicUtils.suffixes;
import static processdiacritizedlexicon.DiacritizeText.diacritizedPrefixes;
import static processdiacritizedlexicon.DiacritizeText.diacritizedSuffixes;



import java.io.BufferedReader;
import java.io.InputStreamReader;


// here we fill data 
public class QataraLib
{
    

    public static DenormalizeText dnt;
    public static POSTagger tagger;
    public static ArabicNER ner;
    //public static DiacritizeText dt;
    public static ProcessDiacritizedLexicon dlex;
    public static DiacritizeText dt = null;
    public static testCase tc ;
    /**
     * We want to use Java from PHP to pass text to  APOSTLib
     *
     * @param intext
     * @return
     */
    public QataraLib(DenormalizeText demo_dnt, POSTagger demo_tagger, ArabicNER demo_ner, ProcessDiacritizedLexicon demo_dlex) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException {

        dnt = demo_dnt;
        tagger = demo_tagger;
        ner = demo_ner;
        //dt = demo_dt; 
        dlex = demo_dlex;
        tc = new testCase();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException, InterruptedException, Exception {  

        
        int i=0;
        int mode = 1;
        boolean klm = false;
        String kenlmDir="";
        String arg;
        int args_flag = 0; // correct set of arguments
                
        while (i < args.length) {
            arg = args[i++];
            // 
            if (arg.equals("--help") || arg.equals("-h")) {
                System.out.println("Usage: QataraLib <--help|-h> [task|-t pos|tok|ner|diac] <[--klm|-k][kenlmDir]>");
                System.exit(-1);
            } 
            
            if (arg.equals("--task") || arg.equals("-t")) {
                                args_flag++;
                if(args[i].equals("pos")) {
                    mode = 2;
                }
                if(args[i].equals("tok")) {
                    mode = 1;
                } 
                if(args[i].equals("ner")) {
                    mode = 3;
                }
                if(args[i].equals("diac")) {
                    mode = 4;
                } 
                
                args_flag++;
            }

            if ((arg.equals("--klm") || arg.equals("-k"))&& args.length>=i) {
                args_flag++;
                kenlmDir = args[i];
                klm = true;
                            
            } 

        } 
                //System.out.println("Args len "+args.length+" flag:"+args_flag+" taks:"+mode+" klmdir="+kenlmDir);
                
        if(args_flag==0 || args.length<2) {
            System.out.println("Usage: QataraLib <--help|-h> [task|-t pos|tok|ner] <[--klm|-k][kenlmDir]>");
            System.exit(-1);
        }

        String dataDirectory = System.getProperty("java.library.path");

        System.out.print("Initializing the system ....");

        kenlmDir = dataDirectory;
        dnt = new DenormalizeText(dataDirectory, kenlmDir) ;
        tagger = new POSTagger(dataDirectory);
        ner = new ArabicNER(dataDirectory, tagger);
        //demo_dt = new DiacritizeText(dataDirectory, dataDirectory+"/all-text.txt.diacritized.filtered.nocase.3g.blm", dataDirectory ,  dataDirectory+"/empty");
        dlex = new ProcessDiacritizedLexicon(dataDirectory);
        //apostdemo = new ApostDemoWrap(demo_dnt, demo_tagger, demo_ner, demo_dlex);
        System.out.print("\r");
        System.out.println("System ready!               ");
        

        if(mode == 4) 
            diacritizeSTDIN();
        else 
            tc.processSTDIN(dataDirectory, kenlmDir, mode, klm);
    }   

    public static void processSTDIN(String dataDir, String kenlmDir, int mode, boolean bDenormalizeText) throws Exception, IOException, InterruptedException, FileNotFoundException, ClassNotFoundException
    {
        tc.processSTDIN(dataDir, kenlmDir, mode, bDenormalizeText);
    }
    
    public static void diacritizeSTDIN()  throws Exception, IOException, InterruptedException, FileNotFoundException, ClassNotFoundException
    {
        String outtext = new String("");
        String line = "";
        String newline = "";

        BufferedReader sr = new BufferedReader(new InputStreamReader(System.in));
        BufferedWriter sw = new BufferedWriter(new OutputStreamWriter(System.out));
        
        while ((line = sr.readLine()) != null) {
                //System.err.println("Process: "+line);
            line = line.replace("/", "\\");
            newline = dlex.fullyDiacritizeSentence(line, dlex.dt).replace("\\", "/");

            System.err.println(newline);

            outtext += " "+newline;
        }

    }


}


