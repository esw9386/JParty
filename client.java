import java.awt.*;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;

public class client {
    static String name;
    static final String SERVER_PROMPT = "Enter a server name.";
    static final String SERVER_FAIL = "Connection failed; try again";
    static final String NAME_PROMPT = "Enter a team name.";
    static final String CLOSED = "LOCKED";
    static final String OPEN = "BUZZ IN";
    static final String BUZZ = "BUZZED";
    static Scanner sin;
    static PrintStream sout;
    static boolean over;
    static Buzzer buzzer;
    static JTextField jtf;

    public static void main(String[] args) {
        // Initial GUI setup
        JFrame jf = new JFrame("This is JParty!");
        jf.setSize(300, 150);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel main = new JPanel(new BorderLayout());
        jtf = new JTextField(); // Server name, team name, final jeopardy answer
        buzzer = new Buzzer(SERVER_PROMPT);
        buzzer.addActionListener((e) -> {
            try {
                String msg = jtf.getText();
                if (!(msg.isEmpty()|buzzer.getText().equals(CLOSED))) {
                    jtf.setText("");
                    if (sout==null) {
                        buzzer.server=msg;
                        try {buzzer.setup();}
                        catch (IOException iox) {buzzer.setText(SERVER_FAIL);}
                    } else if (name==null) {
                        sout.println(msg);
                        client.name = msg;
                        buzzer.setText(CLOSED);
                    } else {sout.println(msg);}
                } else if (buzzer.getText().equals(OPEN)) {
                        sout.println("B");
                        buzzer.setText("Buzz sent!");
                }
            } catch (NullPointerException npx) {System.out.println("NullPointerException: "+npx.toString());}
        });
        
        main.add(buzzer, BorderLayout.CENTER);
        main.add(jtf, BorderLayout.SOUTH);
        jf.add(main);
        jf.setVisible(true);
    }

    static class Buzzer extends JButton implements Runnable { 
        String server;
        Socket s;

        Buzzer(String text) { super(text); }
        
        public void setup() throws IOException {
            if (sout==null) {
                s = new Socket(server, 5190);
                client.sin = new Scanner(s.getInputStream());
                (new Thread(this)).start(); // begins to listen for messages from the server
                client.sout = new PrintStream(s.getOutputStream(), true);
                setText(NAME_PROMPT);
            }
        }

        @Override
        public void run() {
            setText("Welcome to JParty");
            while (!over) {
                if (sin.hasNextLine()) {
                    switch (sin.nextLine()) {
                        case signals.WAITING:
                            setText("Waiting for teams to join...");
                            break;
                        case signals.CLOSED: 
                            setText(CLOSED);
                            break;
                        case signals.OPEN: // clue has been read and clients are prompted to buzz
                            setText(OPEN);
                            break;
                        case signals.BUZZ:
                            setText(BUZZ);
                            break;
                    }
                    }
                }
            }

    public class signals {
        static final String WAITING = "0"; // Not every contestant has joined yet
        static final String CLOSED = "1"; // Buzzer is locked as a question has not yet been fully read
        static final String OPEN = "2"; // Question has been read and anyone can buzz in
        static final String BUZZ = "B";
    }
    }
}
