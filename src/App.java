import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import java.util.ArrayList;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Flappy Bird");
            CardLayout cards = new CardLayout();
            JPanel root = new JPanel(cards);

            Logic logic = new Logic();
            View view = new View(logic);
            logic.setView(view);

            MainMenu menu = new MainMenu(() -> {
                cards.show(root, "game");
                logic.startGame();
                view.requestFocusInWindow();
            });

            root.add(menu, "menu");
            root.add(view, "game");

            frame.setContentPane(root);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}

class MainMenu extends JPanel {
    MainMenu(Runnable onStart) {
        setPreferredSize(new Dimension(360, 640));
        setBackground(new Color(0x70c5ce));
        setLayout(new GridBagLayout());

        JLabel title = new JLabel("FLAPPY BIRD");
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.WHITE);

        JButton start = new JButton("Mulai");
        JButton exit = new JButton("Keluar");
        Dimension size = new Dimension(160, 45);
        start.setPreferredSize(size);
        exit.setPreferredSize(size);
        start.setFocusable(false);
        exit.setFocusable(false);

        start.addActionListener(e -> onStart.run());
        exit.addActionListener(e -> System.exit(0));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(15, 0, 15, 0);
        c.gridy = 0;
        add(title, c);
        c.gridy = 1;
        add(start, c);
        c.gridy = 2;
        add(exit, c);
    }
}

class Player {
    private int posX, posY, width, height, velocityY;
    private Image image;

    Player(int posX, int posY, int width, int height, Image image) {
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.image = image;
        this.velocityY = 0;
    }

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }
    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public Image getImage() { return image; }
    public void setImage(Image image) { this.image = image; }
    public int getVelocityY() { return velocityY; }
    public void setVelocityY(int velocityY) { this.velocityY = velocityY; }
}

class Pipe {
    private int posX, posY, width, height, velocityX;
    private Image image;
    boolean passed;

    Pipe(int posX, int posY, int width, int height, Image image) {
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.image = image;
        this.velocityX = 0;
        this.passed = false;
    }

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }
    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public Image getImage() { return image; }
    public void setImage(Image image) { this.image = image; }
    public int getVelocityX() { return velocityX; }
    public void setVelocityX(int velocityX) { this.velocityX = velocityX; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
}

class Sound {
    static Clip load(String name) {
        try {
            URL url = Sound.class.getResource("assets/" + name);
            AudioInputStream in = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(in);
            return clip;
        } catch (Exception e) {
            return null; // file audio tidak ada -> game tetap jalan tanpa suara
        }
    }

    static void play(Clip clip) {
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    static void loop(Clip clip) {
        if (clip == null) return;
        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    static void stop(Clip clip) {
        if (clip != null) clip.stop();
    }
}

class Logic implements ActionListener, KeyListener {
    int frameWidth = 360;
    int frameHeight = 640;

    int playerStartPosX = frameWidth / 2;
    int playerStartPosY = frameHeight / 2;
    int playerWidth = 34;
    int playerHeight = 24;

    int pipeStartPosX = frameWidth;
    int pipeStartPosY = 0;
    int pipeWidth = 64;
    int pipeHeight = 512;

    Timer gameLoop;
    Timer pipesCooldown;
    int gravity = 1;
    int jumpPower = -9;
    int pipeVelocityX = -2;

    View view;
    Image birdImage;
    Player player;

    Image lowerPipeImage;
    Image upperPipeImage;
    ArrayList<Pipe> pipes;

    int score = 0;
    boolean gameOver = false;

    Clip bgm, jumpSfx, pointSfx, hitSfx;

    public Logic() {
        birdImage = new ImageIcon(getClass().getResource("assets/bird.png")).getImage();
        player = new Player(playerStartPosX, playerStartPosY, playerWidth, playerHeight, birdImage);

        lowerPipeImage = new ImageIcon(getClass().getResource("assets/lowerPipe.png")).getImage();
        upperPipeImage = new ImageIcon(getClass().getResource("assets/upperPipe.png")).getImage();
        pipes = new ArrayList<>();

        bgm = Sound.load("bgm.wav");
        jumpSfx = Sound.load("jump.wav");
        pointSfx = Sound.load("point.wav");
        hitSfx = Sound.load("hit.wav");

        pipesCooldown = new Timer(1500, e -> placePipes());
        gameLoop = new Timer(1000 / 60, this);
    }

    public void setView(View view) { this.view = view; }
    public Player getPlayer() { return player; }
    public ArrayList<Pipe> getPipes() { return pipes; }
    public int getScore() { return score; }
    public boolean isGameOver() { return gameOver; }

    public void startGame() {
        player.setPosX(playerStartPosX);
        player.setPosY(playerStartPosY);
        player.setVelocityY(0);
        pipes.clear();
        score = 0;
        gameOver = false;
        if (view != null) {
            view.setScore(0);
            view.repaint();
        }
        gameLoop.start();
        pipesCooldown.restart();
        Sound.loop(bgm);
    }

    public void placePipes() {
        int randomPosY = (int) (pipeStartPosY - (pipeHeight / 4) - Math.random() * (pipeHeight / 2));
        int openingSpace = frameHeight / 4;

        Pipe upperPipe = new Pipe(pipeStartPosX, randomPosY, pipeWidth, pipeHeight, upperPipeImage);
        pipes.add(upperPipe);

        Pipe lowerPipe = new Pipe(pipeStartPosX, randomPosY + openingSpace + pipeHeight,
                pipeWidth, pipeHeight, lowerPipeImage);
        pipes.add(lowerPipe);
    }

    public void move() {
        player.setVelocityY(player.getVelocityY() + gravity);
        player.setPosY(player.getPosY() + player.getVelocityY());
        player.setPosY(Math.max(player.getPosY(), 0));

        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.setPosX(pipe.getPosX() + pipeVelocityX);
        }
    }

    private boolean hitsPipe(Pipe p) {
        Rectangle a = new Rectangle(player.getPosX(), player.getPosY(), player.getWidth(), player.getHeight());
        Rectangle b = new Rectangle(p.getPosX(), p.getPosY(), p.getWidth(), p.getHeight());
        return a.intersects(b);
    }

    private void endGame() {
        gameOver = true;
        gameLoop.stop();
        pipesCooldown.stop();
        Sound.stop(bgm);
        Sound.play(hitSfx);
        view.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();

        for (Pipe p : pipes) {
            if (hitsPipe(p)) {
                endGame();
                return;
            }
        }
        if (player.getPosY() + player.getHeight() >= frameHeight) {
            endGame();
            return;
        }

        // pipa atas & bawah selalu berpasangan (indeks genap = atas)
        for (int i = 0; i + 1 < pipes.size(); i += 2) {
            Pipe upper = pipes.get(i);
            if (!upper.passed && player.getPosX() > upper.getPosX() + upper.getWidth()) {
                upper.passed = true;
                pipes.get(i + 1).passed = true;
                score++;
                view.setScore(score);
                Sound.play(pointSfx);
            }
        }

        while (pipes.size() >= 2 && pipes.get(0).getPosX() + pipes.get(0).getWidth() < 0) {
            pipes.remove(0);
            pipes.remove(0);
        }

        view.repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_SPACE && !gameOver) {
            player.setVelocityY(jumpPower);
            Sound.play(jumpSfx);
        } else if (key == KeyEvent.VK_R && gameOver) {
            startGame();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}

class View extends JPanel {
    private final Logic logic;
    private final JLabel scoreLabel;

    View(Logic logic) {
        this.logic = logic;
        setPreferredSize(new Dimension(logic.frameWidth, logic.frameHeight));
        setBackground(new Color(0x70c5ce));
        setLayout(null);
        setFocusable(true);
        addKeyListener(logic);

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 24));
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setBounds(10, 10, 200, 30);
        add(scoreLabel);
    }

    public void setScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        ArrayList<Pipe> pipes = logic.getPipes();
        if (pipes != null) {
            for (int i = 0; i < pipes.size(); i++) {
                Pipe pipe = pipes.get(i);
                g.drawImage(pipe.getImage(), pipe.getPosX(), pipe.getPosY(),
                        pipe.getWidth(), pipe.getHeight(), null);
            }
        }

        Player player = logic.getPlayer();
        if (player != null) {
            g.drawImage(player.getImage(), player.getPosX(), player.getPosY(),
                    player.getWidth(), player.getHeight(), null);
        }

        if (logic.isGameOver()) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("GAME OVER", 60, 280);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Skor: " + logic.getScore(), 135, 320);
            g.drawString("Tekan R untuk restart", 85, 355);
        }
    }
}
