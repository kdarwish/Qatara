QATARA
=============

ABOUT
--------------------------
QCRI Advanced Tools for ARAbic (QATARA) is a Library of a statistical Tokenizer, Part of Speech, 
Named Entities, Gender and Number Tagger, and a Diacritizer  that was trained using Conditional 
Random Fields (CRF++). CRF have been used for segmenting/labeling data among other NLP tasks.


Download
---------

QATARA Library. Latest release

You may check-out also the latest version from the githup repository: https://github.com/Qatar-Computing-Research-Institute/Qatara
Data directory is not included. Download the data from http://alt.qcri.org/tools/qatara/data.tgz 

Demonstration
--------------

Check the QATARA demo online from http://qatsdemo.cloudapp.net/qatara/.


CONTENTS
--------------------------
Package content, files, folders

 
How to run the software
------------------------
1 - Command line:


	java -Xmx2048m -Djava.library.path=../data/ -cp ./lib/ArabicPOSTaggerLib.jar:./lib/CRFPP.jar:./lib/trove-3.0.3.jar:./lib/weka.jar:./dist/ProcessDiacritizedLexicon.jar:. QataraLib <options> < [file to parse]

Parameters:

	QataraLib <--help|-h> [--task|-t pos|tok|ner|diac] [--klm|-k kenlmDir] <  filename
	* options: 
 	*  --help		display help information
 	*  --task		tok :  Parse file using tokenization model
 	*               pos :  Parse file using both tokenization and pos models
 	*               ner :  Parse file using tokenization, pos and named entities models   
 	*               diac:  Diacritize text
    *
 	*  --klm		kenlmdir :  Directory with kenlm binary
 	* 

Example:

	java -Xmx2048m -Djava.library.path=../data/ -cp ./lib/ArabicPOSTaggerLib.jar:./lib/CRFPP.jar:./lib/trove-3.0.3.jar:./lib/weka.jar:./dist/ProcessDiacritizedLexicon.jar:. QataraLib -t diac < testfile.txt

For Windows Environment: You may require to explicitly specify the library path:

	java -Xmx1024m -Djava.library.path=./data/ -cp ./lib/ArabicPOSTaggerLib.jar:./lib/CRFPP.jar:./lib/trove-3.0.3.jar:./lib/weka.jar:./dist/ProcessDiacritizedLexicon.jar:. QataraLib -t diac < testfile.txt


How to compile the software
----------------------------
Build the jar:
 
	ant jar
	
Deploy the package to other direcotory:

	ant deploy -Do=<Dest Dir>

Dependencies
---------------
Arabic Text Analyzer used Java Native Interface (JNI) wrapper to access CRF++ functionalilies:
Two files needed from the CRF++ which are:

- ArabicPOSTaggerLib.jar http://alt.qcri.org/tools/apost/
- CRFPP.jar
- and a platform depandent library
	- 	libCRFPP.jnilib -for Mac OS-
	-	libCRFPP.so -for Linux 86_64-
	-	CRFPP.dll -for Windows-
  
	You can download the source code for CRF++ from http://code.google.com/p/crfpp/ 
	To build CRFPP and kenlm, See ArabicPOSTaggerLib documentation.

- kenlm langauage model
KenLM langauge model binary for querying. This is used used for denormalization using the inpur text as a query; denormalized text is generated. 
The source code could be downloaded from
http://kheafield.com/code/kenlm/

===



CONTACT
--------------------------
If you have any problem, question please contact kdarwish@qf.org.qa or aabdelali@qf.org.qa.

WEB SITE
---------------------------
URL for the project  and the latest news  and downloads
	http://alt.qcri.org/tools/qatara

GITHUB
---------------------------
Where to download the latest version, 
	https://github.com/Qatar-Computing-Research-Institute/Qatara


LICENSE
------------
QATARA Library code is made public for RESEARCH purpose only, except the binaries and libraries in the depadencies which 
have their own licenses, listed below.  See the references in these files for more details.  
 - CRF++
 - KenLM

For the rest:

    QATARA Library is being made public for research purpose only. 
    For non-research use, please contact:
        Kareem M. Darwish <kdarwish@qf.org.qa>
        Ahmed Abdelali <aabdelali@qf.org.qa>
    
    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  


COPYRIGHT
----------------------------
Copyright 2015 QCRI
