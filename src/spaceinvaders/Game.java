package spaceinvaders;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.media.AudioClip;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * The main hook of our game. This class with both act as a manager for the
 * display and central mediator for the game logic.
 *
 * Display management will consist of a loop that cycles round all entities in
 * the game asking them to move and then drawing them in the appropriate place.
 * With the help of an inner class it will also allow the player to control the
 * main ship.
 *
 * As a mediator it will be informed when entities within our game detect events
 * (e.g. alient killed, played died) and will take appropriate game actions.
 *
 * @author Kevin Glass
 */
@SuppressWarnings("serial")
public class Game extends Canvas {

    /**
     * The strategy that allows us to use accelerate page flipping
     */
    private BufferStrategy strategy;
    /**
     * True if the game has been paused
     */
    private boolean gamePaused = false;

    /**
     * True if the game is currently "running", i.e. the game loop is looping
     */
    private boolean gameRunning = true;
    /**
     * The list of all the entities that exist in our game
     */

    private ArrayList<Entity> entities = new ArrayList<Entity>();
    /**
     * The list of entities that need to be removed from the game this loop
     */
    private ArrayList<Entity> removeList = new ArrayList<Entity>();
    /**
     * The entity representing the player
     */
    private Entity ship;
    /**
     * The speed at which the player's ship should move (pixels/sec)
     */
    private double moveSpeed = 400;
    /**
     * The time at which last fired a shot
     */
    private long lastFire = 0;
    /**
     * The interval between our players shot (ms)
     */
    private long firingInterval = 300;
    /**
     * The number of aliens left on the screen
     */
    private int alienCount;

    /**
     * The message to display which waiting for a key press
     */
    private String message = "";
    /**
     * True if we're holding up game play until a key has been pressed
     */
    private boolean waitingForKeyPress = true;
    /**
     * True if the left cursor key is currently pressed
     */
    private boolean leftPressed = false;
    /**
     * True if the right cursor key is currently pressed
     */
    private boolean rightPressed = false;
    /**
     * True if we are firing
     */
    private boolean firePressed = false;
    /**
     * True if game logic needs to be applied this loop, normally as a result of
     * a game event
     */
    private boolean logicRequiredThisLoop = false;
    
    /**
     * The last time the screen was updated
     */
    private long lastLoopTime;

    // the current level that the player is on
    private int level = 1;

    private Map<Integer, String> levelsDesc = new HashMap();

    private Map<Integer, String> levelsAlienImages = new HashMap();

    private Map<Integer, String> levelsBackground = new HashMap();

    private Map<Integer, String> levelAlternateAlienImages = new HashMap();

    private long previousAlienShotTime;

    /**
     * Construct our game and set it running.
     */
    public Game() {
        // create a frame to contain our game
        JFrame container = new JFrame("SpaceInvaders");

        // get hold the content of the frame and set up the resolution of the game
        JPanel panel = (JPanel) container.getContentPane();
        panel.setPreferredSize(new Dimension(800, 600));
        panel.setLayout(null);

        // setup our canvas size and put it into the content of the frame
        setBounds(0, 0, 800, 600);
        panel.add(this);

        // Tell AWT not to bother repainting our canvas since we're
        // going to do that our self in accelerated mode
        setIgnoreRepaint(true);

        // finally make the window visible 
        container.pack();
        container.setResizable(false);
        container.setVisible(true);

        // add a listener to respond to the user closing the window. If they
        // do we'd like to exit the game
        container.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // add a key input system (defined below) to our canvas
        // so we can respond to key pressed
        addKeyListener(new KeyInputHandler());

        levelsDesc.put(1, "Defend");
        levelsDesc.put(2, "Attack");
        levelsDesc.put(3, "Annihilate");
        levelsDesc.put(4, "The Evasion");

        levelsAlienImages.put(1, "assets/drawables/alien_level_1.gif");
        levelsAlienImages.put(2, "assets/drawables/alien_level_2.gif");
        levelsAlienImages.put(3, "assets/drawables/alien_level_3.gif");

        levelAlternateAlienImages.put(1, "assets/drawables/alien_level_1_alt.gif");
        levelAlternateAlienImages.put(2, "assets/drawables/alien_level_2_alt.gif");
        levelAlternateAlienImages.put(3, "assets/drawables/alien_level_3_alt.gif");

        levelsBackground.put(1, "assets/drawables/battle_round_1.jpg");
        levelsBackground.put(2, "assets/drawables/battle_round_2.jpg");
        levelsBackground.put(3, "assets/drawables/battle_round_3.jpg");
        levelsBackground.put(4, "assets/drawables/battle_round_4.jpg");

        // request the focus so key events come to us
        requestFocus();

        // create the buffering strategy which will allow AWT
        // to manage our accelerated graphics
        createBufferStrategy(2);
        strategy = getBufferStrategy();

        // initialise the entities in our game so there's something
        // to see at startup
        initEntities();
    }

    /**
     * Start a fresh game, this should clear out any old data and create a new
     * set.
     */
    private void startGame() {
        // clear out any existing entities and intialise a new set
        entities.clear();
        initEntities();

        // blank out any keyboard settings we might currently have
        leftPressed = false;
        rightPressed = false;
        firePressed = false;

        previousAlienShotTime = System.currentTimeMillis();
    }

    /**
     * Initialise the starting state of the entities (ship and aliens). Each
     * entitiy will be added to the overall list of entities in the game.
     */
    private void initEntities() {
        // create the player ship and place it roughly in the center of the screen
        ship = new ShipEntity(this, "sprites/ship.gif", 370, 550);
        entities.add(ship);

        alienCount = 0;
        if (level != 4) {
            // create a block of aliens (5 rows, by 12 aliens, spaced evenly)
            for (int row = 0; row < 3; row++) {
                for (int x = 0; x < 10; x++) {
                    AlienEntity alien = new AlienEntity(this, levelsAlienImages.get(level), 100 + (x * 70), (50) + row * 50)
                            .withAlternateFrame(levelAlternateAlienImages.get(level), 100);

                    if (row == 2) {
                        alien.setShotAllowed(true);
                    }

                    alien.row = row;
                    alien.column = x;

                    entities.add(alien);
                    alienCount++;
                }
            }
        }
    }

    /**
     * This method will redraw all the game frames
     */
    private void refreshFrames() {
        // work out how long its been since the last update, this
        // will be used to calculate how far the entities should
        // move this loop
        long delta = System.currentTimeMillis() - lastLoopTime;
        lastLoopTime = System.currentTimeMillis();

        // Get hold of a graphics context for the accelerated 
        // surface and blank it out
        Graphics2D g = (Graphics2D) strategy.getDrawGraphics();

        g.setColor(Color.black);
        g.fillRect(0, 0, 800, 600);

        // cycle round asking each entity to move itself
        if (!waitingForKeyPress) {
            for (int i = 0; i < entities.size(); i++) {
                Entity entity = (Entity) entities.get(i);

                entity.move(delta);
            }
        }

        new SpriteStore()
                .getSprite(levelsBackground.get(level))
                .draw(g, 0, 0);

        // cycle round drawing all the entities we have in the game
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = (Entity) entities.get(i);

            entity.draw(g);
        }

        // brute force collisions, compare every entity against
        // every other entity. If any of them collide notify 
        // both entities that the collision has occured
        for (int p = 0; p < entities.size(); p++) {
            for (int s = p + 1; s < entities.size(); s++) {
                Entity me = (Entity) entities.get(p);
                Entity him = (Entity) entities.get(s);

                if (me.collidesWith(him)) {
                    me.collidedWith(him);
                    him.collidedWith(me);
                }
            }
        }

        if (level != 4) {
            // Check if the aliens can target the ship and shoot if they have a shot
            ShotEntity shot = null;

            for (Entity current : entities) {
                if (current instanceof AlienEntity
                        && current.hasTarget(ship)
                        && ((AlienEntity) current).shotAllowed()
                        && System.currentTimeMillis() - previousAlienShotTime >= (Math.random() > 0.5 ? 2000 : 1000)) {
                    shot = new ShotEntity(this, "assets/drawables/alien_shot.gif", current.getX(), current.getY() + 60);
                    shot.dy = 350;
                    previousAlienShotTime = System.currentTimeMillis() - level * 200;
                }
            }

            if (shot != null) {
                entities.add(shot);
            }
        }

        // remove any entity that has been marked for clear up
        entities.removeAll(removeList);
        removeList.clear();

        // if a game event has indicated that game logic should
        // be resolved, cycle round every entity requesting that
        // their personal logic should be considered.
        if (logicRequiredThisLoop) {
            for (int i = 0; i < entities.size(); i++) {
                Entity entity = (Entity) entities.get(i);
                entity.doLogic();
            }

            logicRequiredThisLoop = false;
        }

        // if we're waiting for an "any key" press then draw the 
        // current message 
        if (waitingForKeyPress) {
            g.setColor(Color.white);
            g.drawString(message, (800 - g.getFontMetrics().stringWidth(message)) / 2, 250);
            g.drawString("Press any key", (800 - g.getFontMetrics().stringWidth("Press any key")) / 2, 300);
        }

        g.setFont(new Font("Calibri bold", 24, 23));
        g.setColor(level == 1 ? Color.red : level == 2 ? Color.orange : Color.green);
        g.drawString("Level : " + levelsDesc.get(level), 600, 30);

        // finally, we've completed drawing so clear up the graphics
        // and flip the buffer over
        g.dispose();
        strategy.show();

        // resolve the movement of the ship. First assume the ship 
        // isn't moving. If either cursor key is pressed then
        // update the movement appropraitely
        ship.setHorizontalMovement(0);

        if ((leftPressed) && (!rightPressed)) {
            ship.setHorizontalMovement(-moveSpeed);
        } else if ((rightPressed) && (!leftPressed)) {
            ship.setHorizontalMovement(moveSpeed);
        }

        // if we're pressing fire, attempt to fire
        if (firePressed) {
            tryToFire();
        }
    }

    /**
     * Creates a new meteor entity and adds it to the entities to be rendered.
     */
    private void generateMeteor() {
        int x = (int) (Math.random() * 800), y = -10;
        
        MeteorEntity meteor = new MeteorEntity(this, x, y);
        meteor.dy = 300;
        
        if(Math.random() > 0.8)
            meteor.dx = 200;
        
        entities.add(meteor);
    }

    // Creating an explosion at a given point in the screen
    public void createExplosionAt(double x, double y) {
        ExplosionEntity explosion = new ExplosionEntity((int) x, (int) y);
        entities.add(explosion);
        explosion.playSound();

        Thread animationWait = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(150);
                    entities.remove(explosion);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        animationWait.start();
    }

    /**
     * Notification from a game entity that the logic of the game should be run
     * at the next opportunity (normally as a result of some game event)
     */
    public void updateLogic() {
        logicRequiredThisLoop = true;
    }

    /**
     * Remove an entity from the game. The entity removed will no longer move or
     * be drawn.
     *
     * @param entity The entity that should be removed
     */
    public void removeEntity(Entity entity) {
        removeList.add(entity);

        if (entity instanceof AlienEntity) {
            createExplosionAt(entity.getX(), entity.getY());
        }

    }

    /**
     * Notification that the player has died.
     */
    public void notifyDeath() {
        if (!waitingForKeyPress) {
            createExplosionAt(ship.x, ship.y);
        }

        message = "Oh no! They got you, try again?";
        waitingForKeyPress = true;
    }

    /**
     * Notification that the player has won since all the aliens are dead.
     */
    public void notifyWin() {
        message = "Well done! You Win!";
        waitingForKeyPress = true;

        level++;

        if (level == 5) {
            level = 1;
        }
    }

    /**
     * Notification that an alien has been killed
     */
    public void notifyAlienKilled() {
        // reduce the alient count, if there are none left, the player has won!
        alienCount--;

        if (alienCount == 0) {
            notifyWin();
        }

        // if there are still some aliens left then they all need to get faster, so
        // speed up all the existing aliens
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = (Entity) entities.get(i);

            if (entity instanceof AlienEntity) {
                // speed up by 2%
                entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.02);
            }
        }
    }

    /**
     * @param from is the shooter alien that has just died and would like to
     * leave its shooting capabilities to the next alien in the column.
     */
    public void transferShotPower(AlienEntity from) {
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity entity = entities.get(i);
            if (entity instanceof AlienEntity) {
                AlienEntity alien = (AlienEntity) entity;
                if (alien.column == from.column && (alien.row == from.row - 1 || alien.row == from.row - 2)) {
                    alien.setShotAllowed(true);
                    break;
                }
            }
        }
    }

    /**
     * Attempt to fire a shot from the player. Its called "try" since we must
     * first check that the player can fire at this point, i.e. has he/she
     * waited long enough between shots
     */
    public void tryToFire() {
        // check that we have waiting long enough to fire
        if (System.currentTimeMillis() - lastFire < firingInterval) {
            return;
        }

        // if we waited long enough, create the shot entity, and record the time.
        lastFire = System.currentTimeMillis();
        ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", ship.getX() + 10, ship.getY() - 30);
        entities.add(shot);
    }

    /**
     * The main game loop. This loop is running during all game play as is
     * responsible for the following activities:
     * <p>
     * - Working out the speed of the game loop to update moves - Moving the
     * game entities - Drawing the screen contents (entities, text) - Updating
     * game events - Checking Input
     * <p>
     */
    public void gameLoop() {
        lastLoopTime = System.currentTimeMillis();

        int i = 0;

        // keep looping round till the game ends
        while (gameRunning) {
            if (!gamePaused) {
                refreshFrames();
            }

            if (level == 4 && i++ % 10 == 0 && !waitingForKeyPress) {
                generateMeteor();
            }

            // finally pause for a bit. Note: this should run us at about
            // 100 fps but on windows this might vary each loop due to
            // a bad implementation of timer
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
        }
    }

    /**
     * A class to handle keyboard input from the user. The class handles both
     * dynamic input during game play, i.e. left/right and shoot, and more
     * static type input (i.e. press any key to continue)
     *
     * This has been implemented as an inner class more through habbit then
     * anything else. Its perfectly normal to implement this as seperate class
     * if slight less convienient.
     *
     * @author Kevin Glass
     */
    private class KeyInputHandler extends KeyAdapter {

        /**
         * The number of key presses we've had while waiting for an "any key"
         * press
         */
        private int pressCount = 1;

        /**
         * Notification from AWT that a key has been pressed. Note that a key
         * being pressed is equal to being pushed down but *NOT* released. Thats
         * where keyTyped() comes in.
         *
         * @param e The details of the key that was pressed
         */
        public void keyPressed(KeyEvent e) {

            // Code for pausing the game
            if (e.getKeyCode() == KeyEvent.VK_P) {
                gamePaused = !gamePaused;

                if (!gamePaused) {
                    lastLoopTime = System.currentTimeMillis();
                } else {
                    waitingForKeyPress = true;
                    message = "PAUSED";
                }
            }

            // if we're waiting for an "any key" typed then we don't 
            // want to do anything with just a "press"
            if (waitingForKeyPress) {
                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                leftPressed = true;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                rightPressed = true;
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                firePressed = true;
            }
        }

        /**
         * Notification from AWT that a key has been released.
         *
         * @param e The details of the key that was released
         */
        public void keyReleased(KeyEvent e) {
            // if we're waiting for an "any key" typed then we don't 
            // want to do anything with just a "released"
            if (waitingForKeyPress) {
                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                leftPressed = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                rightPressed = false;
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                firePressed = false;
            }
        }

        /**
         * Notification from AWT that a key has been typed. Note that typing a
         * key means to both press and then release it.
         *
         * @param e The details of the key that was typed.
         */
        public void keyTyped(KeyEvent e) {
            // if we're waiting for a "any key" type then
            // check if we've recieved any recently. We may
            // have had a keyType() event from the user releasing
            // the shoot or move keys, hence the use of the "pressCount"
            // counter.
            if (waitingForKeyPress) {
                if (pressCount == 1) {
                    // since we've now recieved our key typed
                    // event we can mark it as such and start 
                    // our new game
                    waitingForKeyPress = false;
                    startGame();
                    pressCount = 0;
                } else {
                    pressCount++;
                }
            }

            // if we hit escape, then quit the game
            if (e.getKeyChar() == 27) {
                System.exit(0);
            }
        }
    }

    /**
     * The entry point into the game. We'll simply create an instance of class
     * which will start the display and game loop.
     *
     * @param argv The arguments that are passed into our game
     */
    public static void main(String argv[]) {
        Game g = new Game();

        // The path of the games music
        String musicSrc = g.getClass().getResource("assets/audio/game_music.mp3").toString();

        // Create a clip and start
        new AudioClip(musicSrc).play();

        // Wait for 3900 ms for when the clip is about to end 
        // Start playing another the same clip
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(3900);
                        AudioClip gameMusic = new AudioClip(musicSrc);
                        gameMusic.play();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }).start();

        // Start the main game loop, note: this method will not
        // return until the game has finished running. Hence we are
        // using the actual main thread to run the game.
        g.gameLoop();
    }
    
}
