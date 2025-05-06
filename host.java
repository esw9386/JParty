import java.awt.*;
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
    static ArrayList<PrintStream> contestants;
    static JFrame jf;
    static JPanel category;
    static JPanel cluePanel;
    static JTextArea moneyValue;
    static JTextArea clueString;
    static JTextArea answer;
    static ArrayList<Team> teams;
    
    public static void main(String args[]) {
        // load the window
        jf = new JFrame("JParty Host");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container jp = jf.getContentPane();
        JTextArea text = new JTextArea("This is JParty!");

        // select game settings
        File userTemplate = null;

        // wait for teams to join
        try {
            ServerSocket ss = new ServerSocket(5190);
            while (teams.size() < 3) { // how to implement teams readying up?
                text.setText("Waiting for teams ("+teams.size()+"/3)...");
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
        // play();
    }

    static class ProcessConnection extends Thread {
        Socket s;
        ProcessConnection(Socket s) {this.s = s;}
        @Override
        public void run() {
            try {
                Scanner sin = new Scanner(s.getInputStream());
                try {sleep(10);} catch (InterruptedException ix) {System.out.println("InterruptedException: "+ix.toString());}
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
        public void play() {

        }
    }

    class File {}
}
