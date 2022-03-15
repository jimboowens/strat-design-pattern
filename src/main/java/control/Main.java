package control;

import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.Quarkus;

import control.controllers.ScoreBoard;
import control.model.Balloon;
import control.model.Clown;
import control.model.SquareBalloon;

import java.util.logging.Logger;

import javax.inject.Inject;

public class Main {

    @Inject
    Logger log;

    public void main(String[] args) {
        ScoreBoard sb = new ScoreBoard();
        ScoreBoard.algorithmBase = new Balloon();
        
        System.out.println("Balloon Tap Score: [" + sb.showScore(10, 5) + "]");
        
        ScoreBoard.algorithmBase = new Clown();
        System.out.println("Clown Tap Score: [" + sb.showScore(10, 5) + "]");

        ScoreBoard.algorithmBase = new SquareBalloon();
        System.out.println("SquareBalloon Tap Score: [" + sb.showScore(10, 5) + "]");

    }

}
