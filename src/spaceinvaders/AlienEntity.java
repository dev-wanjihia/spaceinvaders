package spaceinvaders;


/**
 * An entity which represents one of our space invader aliens.
 *
 * @author Kevin Glass
 */
public class AlienEntity extends Entity {

    /**
     * The speed at which the alient moves horizontally
     */
    private double moveSpeed = 75;
    /**
     * The game in which the entity exists
     */
    private Game game;

    private boolean alternate = true;
    
    private boolean shotAllowed  = false;
    
    public int row, column;
    

    /**
     * Create a new alien entity
     *
     * @param game The game in which this entity is being created
     * @param ref The sprite which should be displayed for this alien
     * @param x The intial x location of this alien
     * @param y The intial y location of this alient
     */
    public AlienEntity(Game game, String ref, int x, int y) {
        super(ref, x, y);

        this.game = game;
        dx = -moveSpeed;
        
        alternate = true;
    }

    /**
     * Request that this alien moved based on time elapsed
     *
     * @param delta The time that has elapsed since last move
     */
    public void move(long delta) {
        // if we have reached the left hand side of the screen and
        // are moving left then request a logic update 
        if ((dx < 0) && (x < 10)) {
            game.updateLogic();
        }
        // and vice vesa, if we have reached the right hand side of 
        // the screen and are moving right, request a logic update
        if ((dx > 0) && (x > 750)) {
            game.updateLogic();
        }

        // proceed with normal move
        super.move(delta);
    }

    /**
     * Update the game logic related to aliens
     */
    public void doLogic() {
        // swap over horizontal movement and move down the
        // screen a bit
        dx = -dx;
        y += 10;

        // if we've reached the bottom of the screen then the player
        // dies
        if (y > 570) {
            game.notifyDeath();
        }
    }

    /**
     * Notification that this alien has collided with another entity
     *
     * @param other The other entity
     */
    public void collidedWith(Entity other) {
        // collisions with aliens are handled elsewhere
        
        // This alien has just died and it needs to transfer its shooting powers
        // to the next alien in line
        // This is going to be done by notifying the other aliens that this 
        // alien has just died
        if(other instanceof ShotEntity)
        {
            if(shotAllowed){
                game.transferShotPower(this);
            }
        }
    }

    public AlienEntity withAlternateFrame(String ref, int altTime) {
        Sprite altSprite = 
              SpriteStore.get().getSprite(ref),
              currSprite = this.sprite;

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean stepper = false;
                
                try {
                    while (true && AlienEntity.this.alternate) {
                        Thread.sleep(altTime);
                        stepper = !stepper;
                        AlienEntity.this.sprite = stepper ? currSprite : altSprite;
                    }
                } catch (InterruptedException ignore) {}
            }
        }).start();
        
        return this;
    }
    
    public boolean shotAllowed(){
        return shotAllowed;
    }
    
    public void setShotAllowed(boolean shotAllowed){
        this.shotAllowed = shotAllowed;
    }
    
    
}
