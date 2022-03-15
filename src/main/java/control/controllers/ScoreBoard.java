package control.controllers;

public class ScoreBoard {
    public static ScoreAlgorithmBase algorithmBase;

    public int showScore(int taps,int multiplier){
        return algorithmBase.calculateScore(taps, multiplier);
    }
    
}
