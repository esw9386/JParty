package JParty;
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
        jtf = new JTextField();
        main.add(buzzer, BorderLayout.CENTER);
        main.add(jtf, BorderLayout.SOUTH);
        jf.add(main);
        jf.setVisible(true);
    }

    static class Buzzer extends JButton implements Runnable {
        Scanner sin;
        String text;
        boolean over;
        Buzzer(String text) {setText(text); addActionListener(new BuzzerListener()); over=false;}
        Buzzer(String text, Scanner sin) {this(text); this.sin=sin;}

        @Override
        public void run() {
            setText("Type the host server and tap this button to connect.");
            while (!over) {
                if (sin.hasNextLine()) {
                    switch (sin.nextLine()) {
                        case signals.WAITING:
                            setText("Waiting for teams to join...");
                        case signals.CLOSED:
                        case signals.OPEN:
                            setText("BUZZ IN");
                        case signals.NEXT:
                        case signals.OVER:
                            over = true;
                    }
                }
            }
        }

        void setup() {
            if (s==null)
                try {
                    s = new Socket(server, 5190);
                    sin = new Scanner(s.getInputStream());
                    sout = new PrintStream(s.getOutputStream());
                    buzzer.setText(prompts.WAITING_CLIENT);
                } catch (IOException iox) {System.out.println("Exception thrown: "+iox.toString());}
        }
        
        class BuzzerListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                if (!over) {
                    if (buzzer.text.equals(prompts.CONNECT_INIT)) {
                        server = jtf.getText();
                        setup();
                    }
                    switch (signal) {
                        case signals.CLOSED:
                        case signals.OPEN:
                        case signals.NEXT:
                        case signals.WAITING:
                    }
                }
            }
        }
    }
}