import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;


/**
 *
 * @author esw9386
 */
public class client {
    static String server, signal="";
    static Socket s;
    static Scanner sin;
    static PrintStream sout;
    static Buzzer buzzer;
    static JTextField jtf;


    public static void main(String[] args) {
        JFrame jf = new JFrame("This is JParty!");
        JPanel main = new JPanel(new BorderLayout());
        buzzer = new Buzzer(prompts.CONNECT_INIT);
        jtf = new JTextField(); // choosing team name + final jeopardy
        main.add(buzzer, BorderLayout.CENTER);
        main.add(jtf, BorderLayout.SOUTH);
        jf.add(main);
        jf.setVisible(true);
        Scanner sin; // host will continually be sending messages to each client
    }

    static class Buzzer extends JButton implements Runnable { // thread
        String text;
        boolean over;
        host.Game game; 
        int id; // has to be first thing sent by host (0, 1, 2)
        Buzzer(String text) {setText(text); addActionListener(new BuzzerListener()); over=false;} 
        // Buzzer(String text, Scanner sin) {this(text); this.sin=sin;} // probs won't be used

        @Override
        public void run() {
            setText("Type the host server and tap this button to connect.");
            while (!over) { // game has not yet ended
                if (sin.hasNextLine()) {
                    switch (sin.nextLine()) { 
                        case signals.WAITING: // so if sin.nextLine() is equal to WAITING, which means when user input is "0"? 
                            setText("Waiting for teams to join...");
                        case signals.CLOSED: 
                        case signals.OPEN: // clue has been read and clients are prompted to buzz
                            setText("BUZZ IN");
                        // case signals.NEXT: // stays open
                        case signals.OVER: // switches to CLOSED signal too?
                            over = true;
                    }
                }
            }
        }

        void setup() {
            if (s == null)
                try {
                    s = new Socket(server, 5190);
                    sin = new Scanner(s.getInputStream());
                    sout = new PrintStream(s.getOutputStream());
                    buzzer.setText(prompts.WAITING_CLIENT); // waits for teams to join
                } catch (IOException iox) {System.out.println("Exception thrown: "+iox.toString());}
        }
        
        class BuzzerListener implements ActionListener {
            // game window will display who presses the button first (change team name color) and if they are correct or incorrect when answering
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!over) {
                    if (buzzer.text.equals(prompts.CONNECT_INIT)) {
                        server = jtf.getText();
                        setup();
                    }
                    switch (signal) {
                        case signals.CLOSED: // buzzer should do nothing
                        case signals.OPEN: // if pressed first, buzzer should send msg to host that contestant has an answer; synchronized
                            synchronized(game) {
                                game.guess(id);
                            }
                        // case signals.NEXT: // 
                        case signals.WAITING: // buzzer should do nothing
                        case signals.OVER: // buzzer should do nothing
                    }
                }
            }
        }
    }
    public class prompts {
        static final String CONNECT_INIT = "Type the host server and tap this button to connect."; // first msg that appears when a client opens the window
        static final String CONNECT_FAIL = "Connection to %s failed; try again"; // typed invalid server name
        static final String WAITING_HOST = "Waiting for teams to join (%d/3)..."; // msg for host while contestants are still joining (WAITING signal)
        static final String WAITING_CLIENT = "Waiting for teams to join..."; // msg for client while others are still able to join (WAITING signal)
    }
    
    public class signals {
        static final String WAITING = "0"; // not every contestant has joined yet // Ready button for host 
        static final String CLOSED = "1"; // buzzer is locked as a question has not yet been fully read // Lock buzzer button for host
        static final String OPEN = "2"; // question has been read and anyone can buzz in // Open buzzer button for host
    //    static final String NEXT = "3"; // moving onto next clue; host can just close the buzzers
        static final String OVER = "4"; // when single jeopardy ends // End game button for host
    }
}