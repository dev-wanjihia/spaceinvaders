/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spaceinvaders;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.media.AudioClip;

/**
 *
 * @author Samson
 */
public class ExplosionEntity extends Entity {

    public static AudioClip explosionClip;
    
    /**
     * 
     * @param x The x coordinate of where to create the explosion
     * @param y The y coordinate of where to create the explosion
     * 
     */
    public ExplosionEntity( int x, int y) {
        super("assets/drawables/explosion0.gif", x, y);
        
        if(explosionClip == null)
            explosionClip = new AudioClip(getClass().getResource("assets/audio/explosion.mp3").toString());
        
        new Thread(new Runnable() { 
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    ExplosionEntity.this.sprite = SpriteStore.get().getSprite("assets/drawables/explosion1.gif");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void collidedWith(Entity other) {}
    
    public void playSound(){
        String src = getClass()
                    .getResource("assets/audio/explosion.mp3")
                    .toString();
        
        new AudioClip(src).play();
    }
}
