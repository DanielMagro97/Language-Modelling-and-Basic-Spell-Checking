import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class BuildModel {

    public static void main(String[] args) throws IOException {
        /* Declaring the Data Structures which will store the N-Grams.
         * Each N-gram is stored as an Arraylist of 'Ngram' objects. The 'Ngram' object contains an array of strings
         * (size 1 for unigram, 2 for bigram, 3 for trigram) and an integer storing count, i.e. the number of times it occurs
         * inside the corpus. It also contains a field which stores the smoothed probability.
         */
        ArrayList<Ngram> unigram = new ArrayList<>();
        ArrayList<Ngram> bigram = new ArrayList<>();
        ArrayList<Ngram> trigram = new ArrayList<>();

        // Constructing the unigram, bigram and trigram for the given corpus and storing in 'unigram', 'bigram' and 'trigram'
        System.out.println("Generating Models...");
        generateModels(unigram, bigram, trigram);

        // Performing Laplace Smoothing on each of the n-gram models
        smoothUnigram(unigram);
        System.out.println("Unigram Laplace Smoothed Probability has been calculated");
        smoothBigram(unigram, bigram);
        System.out.println("Bigram Laplace Smoothed Probability has been calculated");
        smoothTrigram(unigram, bigram, trigram);
        System.out.println("Trigram Laplace Smoothed Probability has been calculated");

        // saving unigram, bigram and trigram as objects
        saveModels(unigram, bigram, trigram);
        System.out.println("Models have been saved as objects to disk");
    }


    // File IO method, retrieves all the lines in the text file and stores each line
    // as an element in an array of Strings
    private static String[] readInput(String inputFile) throws IOException {
        String[] data = Files.readAllLines(Paths.get(inputFile)).toArray(new String[]{});
        return data;
    }

    // index is a global variable that will store in which element an already exisitng N-gram's count is stored (used by checkExistence method)
    private static int index;
    // this variable tracks the progress of the model generation (files completed)
    private static int file_count = 0;

    private static void generateModels(ArrayList<Ngram> unigram, ArrayList<Ngram> bigram, ArrayList<Ngram> trigram) throws IOException {
        File dir = new File("C:\\NLP\\Full MLRS Corpus");
        File[] directoryListing = dir.listFiles();

        // iterating through all the text files that make up the corpus
        for (File child : directoryListing) {
            String textFile[] = readInput(child.getAbsolutePath());

            // starting from i=3 since first 3 lines are not part of the corpus
            for (int i = 3; i < textFile.length; i++) {

                String word_i, word_ip1, word_ip2;

                // checking if the first symbol is a '<', and if so, skipping that line
                if (textFile[i].charAt(0) == '<'){
                    continue;
                } else {
                    // the current word (word_i) is the first word of the current line of the text file
                    // First the current line is considered (textFile[i])
                    // Then the line is split by tabulations "\t" and stored in an array ( .split("\t") )
                    // Next the first element of that array is considered ( [0] )
                    // Finally the first word is converted into lowercase and stored inside word_i ( .toLowerCase() ) in lowercase
                    word_i = textFile[i].split("\t")[0].toLowerCase();

                    // if the word is already in our model, we simply increment its count
                    if (checkExistence(unigram, new String []{word_i})){
                        unigram.get(index).count++;
                    } else {    // if it is not already in our model, it is created
                        Ngram ug = new Ngram(1);
                        ug.n_gram[0] = word_i;
                        ug.count = 1;
                        unigram.add(ug);
                    }
                }

                if (textFile[i+1].charAt(0) == '<'){
                    // do nothing (no words follow the current so a bigram or trigram cannot be constructed)
                    continue;
                } else {
                    // Same as word_i but textFile[i+1] is used to consider the word on the next line
                    word_ip1 = textFile[i + 1].split("\t")[0].toLowerCase();

                    if (checkExistence(bigram, new String []{word_i, word_ip1})){
                        bigram.get(index).count++;
                    } else {
                        Ngram bg = new Ngram(2);
                        bg.n_gram[0] = word_i;
                        bg.n_gram[1] = word_ip1;
                        bg.count = 1;

                        bigram.add(bg);
                    }
                }

                if (textFile[i+2].charAt(0) == '<'){
                    // do nothing (no words follow the current so a trigram cannot be built)
                    continue;
                } else {
                    // Same as word_i but textFile[i+2] is used to consider the word 2 lines ahead
                    word_ip2 = textFile[i+2].split("\t")[0].toLowerCase();

                    if (checkExistence(trigram, new String []{word_i, word_ip1, word_ip2})){
                        trigram.get(index).count++;
                    } else {
                        Ngram tg = new Ngram(3);
                        tg.n_gram[0] = word_i;
                        tg.n_gram[1] = word_ip1;
                        tg.n_gram[2] = word_ip2;
                        tg.count = 1;

                        trigram.add(tg);
                    }
                }
            }
            file_count++;
            System.out.println("file " + file_count + "/" + directoryListing.length + " done");
        }
    }

    // Method to check if a word is already in the language model, and if so returning its index
    private static boolean checkExistence(ArrayList<Ngram> model, String[] words){
        for (int i = 0; i < model.size(); i++) {
            // comparing 2 arrays to check if their size and contents are identical
            if (Arrays.equals(model.get(i).n_gram, words)){
                index = i;
                return true;
            }
        }
        return false;
    }

    // Class which will hold the n_grams (n=1 unigram, n=2 bigram, n=3 trigram)
    public static class Ngram implements Serializable{
        final String[] n_gram;
        int count;
        double smoothedProbability;

        public Ngram(final int n) {
            n_gram = new String[n];
        }
    }

    private static void smoothUnigram(ArrayList<Ngram> unigram){
        // looping through all unigrams to find the total count
        int totalCount = 0;
        for (int i = 0; i < unigram.size(); i++) {
            totalCount += unigram.get(i).count;
        }

        int vocabSize = unigram.size();

        for (int i = 0; i < unigram.size(); i++) {
            // (count(Wn) + 1) / (totalcount(all Wn) + V)
            unigram.get(i).smoothedProbability = (double) (unigram.get(i).count + 1)/(totalCount + vocabSize);
        }
    }

    private static void smoothBigram(ArrayList<Ngram> unigram, ArrayList<Ngram> bigram){
        int vocabSize = unigram.size();

        for (int i = 0; i < bigram.size(); i++) {
            // Finding the count of w_n-1
            int count_wordNminus1 = 0;
            for (int j = 0; j < unigram.size(); j++) {
                if (bigram.get(i).n_gram[0].equals(unigram.get(j).n_gram[0])){
                    count_wordNminus1 = unigram.get(j).count;
                }
            }
            // (count(Wn-1,Wn) + 1) / (count(Wn-1) + V)
            bigram.get(i).smoothedProbability = (double) (bigram.get(i).count + 1)/(count_wordNminus1 + vocabSize);
        }
    }

    private static void smoothTrigram(ArrayList<Ngram> unigram, ArrayList<Ngram> bigram, ArrayList<Ngram> trigram){
        int vocabSize = unigram.size();

        for (int i = 0; i < trigram.size(); i++) {
            // Finding the count of w_n-2,w_n-1
            int count_wordNminus2and1 = 0;
            for (int j = 0; j < bigram.size(); j++) {
                if (Arrays.equals(new String[]{trigram.get(i).n_gram[0], trigram.get(i).n_gram[1]}, bigram.get(j).n_gram)){
                    count_wordNminus2and1 = bigram.get(j).count;
                }
            }
            // (count(Wn-2,Wn-1,Wn) + 1) / (count(Wn-2,Wn-1) + V)
            trigram.get(i).smoothedProbability = (double) (trigram.get(i).count + 1)/(count_wordNminus2and1 + vocabSize);
        }
    }


    // Method to Save objects to Disk
    private static void saveModels(ArrayList<Ngram> unigram, ArrayList<Ngram> bigram, ArrayList<Ngram> trigram) throws IOException {
        String fileName;
        FileOutputStream fos;
        ObjectOutputStream oos;

        fileName = "C:\\NLP\\Language Models\\unigram";
        fos = new FileOutputStream(fileName);
        oos = new ObjectOutputStream(fos);
        oos.writeObject(unigram);

        fileName= "C:\\NLP\\Language Models\\bigram";
        fos = new FileOutputStream(fileName);
        oos = new ObjectOutputStream(fos);
        oos.writeObject(bigram);

        fileName= "C:\\NLP\\Language Models\\trigram";
        fos = new FileOutputStream(fileName);
        oos = new ObjectOutputStream(fos);
        oos.writeObject(trigram);

        fos.close();
        oos.close();
    }
}
