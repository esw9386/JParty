package JParty;
// import org.json.simple.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.*;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author isabe
 */
public class host {
    static JFrame jf0, jf1; // startup window and fullscreen game
    static JPanel category;
    static JPanel cluePanel;
    static File preset1 = new File("preset1.json");
    static File preset2 = new File("preset2.json");
    static File blank = new File("blank.json");
    static JTextArea moneyValue;
    static JTextArea clueString;
    static JTextArea answer;
    static ArrayList<Team> teams;
    static Game game;
    
    public static void main(String args[]) {
        /* 1. Open window
         * 2. Display startup card with welcome message and template selection
         * 3. Wait for teams to join on a new card
         * 4. Start game in a new window
         */

        // load the window
        jf0 = new JFrame("JParty Host");
        jf0.setSize(1000,1000);
        jf0.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel cards = new JPanel(new CardLayout());

        JPanel card1 = new JPanel(new BorderLayout());
        JPanel card1lower = new JPanel(new FlowLayout());
        JButton custom = new JButton("Select .json File");
        JButton ps1 = new JButton("Preset 1");
        JButton ps2 = new JButton("Preset 2");

        JTextArea text = new JTextArea("This is JParty!");
        card1.add(text, BorderLayout.CENTER);
        card1.add(card1lower, BorderLayout.SOUTH);

        JPanel card2 = new JPanel();
        jf0.add(cards);
        jf0.setVisible(true);

        // select game settings
        File template = new File("");
        game = new Game(template);

        // wait for teams to join
        try {
            ServerSocket ss = new ServerSocket(5190);
            while (teams.size() < 3) { // how to implement teams readying up?
                text.setText(String.format(prompts.WAITING_HOST, teams.size()));
                text.setText("Waiting for teams to join ("+teams.size()+"/3)...");
                Socket s = ss.accept();
                teams.add(new Team(s));
                new ProcessConnection(s).start();
            }
        } catch (IOException ex) {System.out.println("IOException: "+ex.toString());}

        // wait for teams to select names and ready up
        text.setText("Waiting for teams to be ready...");
        boolean ready = false;
        while (!ready) {
            ready = true;
            for (Team team : teams) {if (!team.ready) {ready = false;}}
        }

        // start the game
        // game = new Game();
        game.play();
    }

    static class ProcessConnection extends Thread {
        Socket s;
        ProcessConnection(Socket s) {this.s = s;}
        @Override
        public void run() {
            try {
                Scanner sin = new Scanner(s.getInputStream());
                while (!sin.hasNextLine()) {}
                System.out.println("New connection from "+sin.nextLine()+":");
                String line;
                while (true) {
                    if (sin.hasNextLine()) {
                        line = sin.nextLine();
                        // for (PrintStream stream : streams) {stream.print(line+'\n');}
                    }
                }
            } catch (IOException iox) {System.out.println("IOException: "+iox.toString());}
        }
    }

    static class Team {
        Socket s;
        PrintStream out;
        Scanner in;
        String name;
        boolean ready;
        int score;
        JButton right, wrong;
        Team(Socket s) {
            this.s=s; 
            try {
                out = new PrintStream(s.getOutputStream());
                in = new Scanner(s.getInputStream());
            } catch (IOException iox) {System.out.println("InterruptedException: "+iox.toString());}
            name = ""; score = 0;
        }
        Team(String name) {this.name = name; score=0; ready=false;}
    }

    static class Game {
        ArrayList<Team> teams;
        File template;
        Game(File template) {this.template=template;}
        public void play() {

        }
    }

    static class File {
        String path;
        File(String path) {this.path = path;}
    }

    static class ButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            //(e.getSource()).pause();
        }
    }
}
