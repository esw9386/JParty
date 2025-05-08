package JParty;
import java.io.File;
import java.util.ArrayList;

public class game {
    static int BACKGROUND = 0x314bcc;
    static class Game {
        ArrayList<host.Team> teams;
        ArrayList<Board> boards;
        File template;
        Game() {}
        Game(File template) {};
        public void setTeams(ArrayList<host.Team> teams) {this.teams=teams;}
        public void play() {

        }

        class Board {
            String category;
            ArrayList<Clue> clues;

            class Clue {
                String prompt, answer;
            }
        }
    }
}
