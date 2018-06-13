import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    // String which stores the result of the generateText method
    private static String word_i;
    // Integer which stores the result of the wrongWord method
    private static int wwi;

    // Declaring all the language models as global variables
    private static ArrayList<BuildModel.Ngram> unigram = new ArrayList<>();
    private static ArrayList<BuildModel.Ngram> bigram = new ArrayList<>();
    private static ArrayList<BuildModel.Ngram> trigram = new ArrayList<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // Loading the Language Model
        loadModels();

        // Asking the user which function he wishes to perform (Main Menu)
        Scanner sc = new Scanner(System.in);
        String choice = "0";
        do {
            System.out.println("Press \"1\". Generate Text");
            System.out.println("Press \"2\". Wrong Word");
            System.out.println("Press \"3\". Terminate Program");
            choice = sc.nextLine();
            switch (Integer.parseInt(choice)) {
                case 1: {
                    System.out.println("Please enter the text you would like the program to continue.");
                    String input = sc.nextLine();
                    if (input.length() == 0){
                        System.out.println("No input was received, please try again");
                        continue;
                    }
                    String lowerCaseInput = input.toLowerCase();
                    String[] text = lowerCaseInput.split(" ");

                    generateText(text);
                    System.out.println(input + " + " + word_i);

                    break;
                }
                case 2: {
                    System.out.println("Please enter the text you would like the program to detect the most likely error in.");
                    String input = sc.nextLine();
                    if (input.length() == 0){
                        System.out.println("No input was received, please try again");
                        continue;
                    }
                    String lowerCaseInput = input.toLowerCase();
                    String[] text = lowerCaseInput.split(" ");
                    if (text.length < 3){
                        System.out.println("Please enter 3 or more words for the program to detect the most likely misspelt word");
                        continue;
                    }

                    wrongWord(text);
                    System.out.println("Most likely misspelt word is: " + text[wwi]);

                    break;
                }
            }
        } while (Integer.parseInt(choice) != 3);
        System.out.println("Program is terminating");
    }



    private static void generateText(String text[]){
        // finding the penultimate (word_i-2) and last words(word_i-1) in the entered sentence (programm will generate word_i)
        String word_im2, word_im1;

        if (text.length == 1){   // If the user has only entered one word
            word_im1 = text[0];

            ArrayList<BuildModel.Ngram> bigramMatches = new ArrayList<>();
            bigramMatches = findBigramMatches(word_im1);

            // If no matching bigrams are found, back off to unigram model
            if (bigramMatches.size() == 0){
                System.out.println("Using the Unigram model");
                word_i = unigram.get( probabilisticChoice(unigram) ).n_gram[0];
            } else {
                // instead of choosing the matching bigram with the highets probability, it is chosen probabilistically
                // using the probabilisticChoice method
                /*
                int highestProbability = 0;
                for (int i = 1; i < bigramMatches.size(); i++) {
                    if (bigramMatches.get(i).smoothedProbability > bigramMatches.get(highestProbability).smoothedProbability){
                        highestProbability = i;
                    }
                }
                */
                System.out.println("Using the Bigram model");
                word_i = bigramMatches.get( probabilisticChoice(bigramMatches) ).n_gram[1];
            }
        } else {                        // If the user has entered 2 or more words
            word_im2 = text[text.length - 2];
            word_im1 = text[text.length - 1];

            // finding all trigrams which start with the last 2 words of the entered sentence
            ArrayList<BuildModel.Ngram> trigramMatches = new ArrayList<>();
            trigramMatches = findTrigramMatches(word_im2, word_im1);

            // if no matching trigrams are found (back off)
            ArrayList<BuildModel.Ngram> bigramMatches = new ArrayList<>();
            if (trigramMatches.size() == 0){
                bigramMatches = findBigramMatches(word_im1);
            } else {
                System.out.println("Using the Trigram model");
                word_i = trigramMatches.get( probabilisticChoice(trigramMatches) ).n_gram[2];
            }

            // if no matching trigrams or bigrams are found, back off to unigram model
            if (trigramMatches.size() == 0 && bigramMatches.size() == 0){
                System.out.println("Using the Unigram model");
                word_i = unigram.get( probabilisticChoice(unigram) ).n_gram[0];
            } else if (trigramMatches.size() == 0 && bigramMatches.size() != 0){
                System.out.println("Using the Bigram model");
                word_i = bigramMatches.get( probabilisticChoice(bigramMatches) ).n_gram[1];
            }
        }
    }

    // Returns an ArrayList of trigrams, for which words_im2 and word_im1 are the last words in the input sentence
    private static ArrayList<BuildModel.Ngram> findTrigramMatches(String word_im2, String word_im1){
        ArrayList<BuildModel.Ngram> trigramMatches = new ArrayList<>();

        for (int i = 0; i < trigram.size(); i++) {
            if (trigram.get(i).n_gram[0].equals(word_im2) && trigram.get(i).n_gram[1].equals(word_im1)){
                trigramMatches.add(trigram.get(i));
            }
        }

        return trigramMatches;
    }

    // Returns an ArrayList of bigrams, for which word_im1 is the last word in the input sentence
    private static ArrayList<BuildModel.Ngram> findBigramMatches(String word_im1){
        ArrayList<BuildModel.Ngram> bigramMatches = new ArrayList<>();

        for (int i = 0; i < bigram.size(); i++) {
            if (bigram.get(i).n_gram[0].equals(word_im1)){
                bigramMatches.add(bigram.get(i));
            }
        }

        return bigramMatches;
    }

    // Method which chooses an ngram based on its smoothed probability
    private static int probabilisticChoice(ArrayList<BuildModel.Ngram> ngram){
        // finding the total probability of all the elements in the given ngram
        double totalProbability = 0.0;
        for (int i = 0; i < ngram.size(); i++) {
            totalProbability += ngram.get(i).smoothedProbability;
        }

        // divide all the probabilities by the totalProbability so as to make them out of 1 (x% of 100%)
        for (int i = 0; i < ngram.size(); i++) {
            ngram.get(i).smoothedProbability /= totalProbability;
        }

        // choosing an ngram based on its probability
        double p = Math.random();
        double cumulativeProbability = 0.0;
        int i;
        for (i = 0; i < ngram.size(); i++) {
            cumulativeProbability += ngram.get(i).smoothedProbability;
            if (cumulativeProbability > p) {
                break;
            }
        }
        return i;
    }

    private static void wrongWord(String text[]){
        // array containing the probability of each individual word in the sentence
        double word_probability[] = new double[text.length];
        // array containing the number of times each word was matched with an n-gram
        int word_match_count[] = new int[text.length];

        // Spell Checking using Trigrams
        for (int i = 0; i < text.length - 2; i++) {
            for (int j = 0; j < trigram.size(); j++) {
                if (trigram.get(j).n_gram[0].equals(text[i]) && trigram.get(j).n_gram[1].equals(text[i+1]) && trigram.get(j).n_gram[2].equals(text[i+2])){
                    word_probability[i] += trigram.get(j).smoothedProbability;
                    word_probability[i+1] += trigram.get(j).smoothedProbability;
                    word_probability[i+2] += trigram.get(j).smoothedProbability;
                    break;
                }
            }
        }

        // Spell Checking using Bigrams
        for (int i = 0; i < text.length-1; i++) {
            for (int j = 0; j < bigram.size(); j++) {
                if (bigram.get(j).n_gram[0].equals(text[i]) && bigram.get(j).n_gram[1].equals(text[i+1])){
                    word_probability[i] += bigram.get(j).smoothedProbability;
                    word_probability[i+1] += bigram.get(j).smoothedProbability;
                    break;
                }
            }
        }

        // Spell Checking using Unigrams
        for (int i = 0; i < text.length; i++) {
            for (int j = 0; j < unigram.size(); j++) {
                if (text[i].equals(unigram.get(j).n_gram[0])){
                    word_probability[i] += unigram.get(j).smoothedProbability;
                    break;
                }
            }
        }

        // Initialising the values of the word_match_count array
        for (int i = 0; i < word_match_count.length; i++) {
            // the first and last word are matched 3 times
            if (i == 0 || i == word_match_count.length - 1) {
                word_match_count[i] = 3;
                continue;
            }
            // if the sentence is 3 words long
            if (word_match_count.length == 3) {
                // the second word is matched 4 times
                word_match_count[1] = 4;
            } else {    // if the sentence is of any other length
                // the second and penultimate word are matched 5 times
                if (i == 1 || i == word_match_count.length - 2) {
                    word_match_count[i] = 5;
                } else {    // all other words are matched 6 times
                    word_match_count[i] = 6;
                }
            }
        }

        // normalising the word probabilities, by dividing each word's probability by its word match count
        for (int i = 0; i < word_probability.length; i++) {
            word_probability[i] /= word_match_count[i];
        }

        // finding the word with the lowest probability
        wwi = 0;
        for (int i = 1; i < word_probability.length; i++) {
            if (word_probability[i] < word_probability[wwi]){
                wwi = i;
            }
        }
    }

    private static void loadModels() throws IOException, ClassNotFoundException {
        String fileName;
        FileInputStream fis;
        ObjectInputStream ois;

        fileName= "C:\\NLP\\Language Models\\unigram";
        fis = new FileInputStream(fileName);
        ois = new ObjectInputStream(fis);
        unigram = (ArrayList<BuildModel.Ngram>) ois.readObject();

        fileName= "C:\\NLP\\Language Models\\bigram";
        fis = new FileInputStream(fileName);
        ois = new ObjectInputStream(fis);
        bigram = (ArrayList<BuildModel.Ngram>) ois.readObject();

        fileName= "C:\\NLP\\Language Models\\trigram";
        fis = new FileInputStream(fileName);
        ois = new ObjectInputStream(fis);
        trigram = (ArrayList<BuildModel.Ngram>) ois.readObject();

        fis.close();
        ois.close();
    }
}